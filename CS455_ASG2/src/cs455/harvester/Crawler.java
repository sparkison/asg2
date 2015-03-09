/**
 * @author Shaun Parkison (shaunpa)
 * Colorado State University
 * CS455 - Dist. Systems
 */

package cs455.harvester;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import cs455.task.CrawlerTask;
import cs455.thread.CrawlerThreadPool;
import cs455.transport.TCPReceiverThread;
import cs455.transport.TCPSender;
import cs455.util.CommandParser;
import cs455.wireformats.CrawlerSendsFinished;
import cs455.wireformats.CrawlerSendsIncomplete;
import cs455.wireformats.CrawlerSendsTask;
import cs455.wireformats.CrawlerSendsTaskComplete;
import cs455.wireformats.Event;
import cs455.wireformats.EventFactory;
import cs455.wireformats.Protocol;


public class Crawler implements Node{

	// Instance variables **************
	private final int RECURSION_DEPTH = 5;
	private final ServerSocket SERVER_SOCKET;
	private final String MY_URL;
	private final String FULL_URL;
	private final long startTime = System.nanoTime();

	private Map<String, String[]> connections;
	private Map<String, TCPSender> myConnections;
	private Map<String, Integer> forwardTaskCount;
	private Map<String, Integer> receiveTaskCount;
	private Map<String, Boolean> crawlersComplete;
	private CrawlerThreadPool myPool;
	private EventFactory ef = EventFactory.getInstance();
	// Used for debug print statements
	private boolean debug = false;
	// Used to determine program runtime
	private boolean timer = true;

	public static void main(String[] args) throws InterruptedException {
		
		Crawler crawler = null;
		
		if(args.length < 3){
			System.out.println("Incorrect format used to initate Crawler\nPlease use: cs455.harvester.Crawler [portNum] [poolSize] [rootUrl] [configPath]");
		}else{
			int port = Integer.parseInt(args[0]);
			int poolSize = Integer.parseInt(args[1]);
			String rootUrl = args[2];
			String configPath = args[3];
			try {
				// Initialize the Crawler
				crawler = new Crawler(port, poolSize, rootUrl, configPath);
			} catch (IOException e) {
				System.err.println(e.getMessage());
				System.exit(-1);
			}
			
			// Start listening for connections
			crawler.listen();		

			// Used for debugging and determining state of Crawler
			CommandParser parser = new CommandParser(crawler);
			parser.start();

			/*
			 * Need to pause for 10 seconds before starting tasks
			 */
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				System.err.println(e.getMessage());
			}

			// Setup connections to other Crawlers, then start initial task
			if(!(crawler.setupConnections()))
				System.out.println("There were some errors setting up connections with other Crawlers");
			else{
				/*
				 * Start task, and initiate heartbeat
				 * Heartbeat used to determine if all Crawlers complete or not
				 */
				crawler.startTask();
				crawler.heartBeat();
			}
			
		}		
	}

	/**
	 * Crawler constructor, constructs a Crawler Object
	 * @param int port
	 * @param int poolSize
	 * @param String rootUrl
	 * @param String configPath
	 * @throws IOException 
	 */
	public Crawler(int port, int poolSize, String myUrl, String configPath) throws IOException{
		// Initialize our containers for other Crawler connections
		connections = new HashMap<String, String[]>();
		myConnections = new HashMap<String, TCPSender>();

		// Record keepers
		forwardTaskCount = new HashMap<String, Integer>();
		receiveTaskCount = new HashMap<String, Integer>();
		crawlersComplete = new HashMap<String, Boolean>();

		// Store only the www.root_url.com portion of URL for easier checking
		// Checking for special case for Psych dept.
		FULL_URL = myUrl;
		String rootUrl = myUrl.split("/")[2];
		if(rootUrl.equals("www.colostate.edu"))
			MY_URL = "www.colostate.edu/Depts/Psychology";
		else
			MY_URL = rootUrl;

		// Path to configuration file
		Path path = Paths.get(configPath);
		Scanner scanner = new Scanner(path);
		while (scanner.hasNextLine()){
			try{
				String[] temp = delimitConfig(scanner.nextLine());
				String[] connection = temp[0].split(":");
				String connectionRootUrl = temp[1].trim();
				/*
				 * Add each connection from configuration file to connections list
				 * Using HasMap, key is the RootURL of the connection, value is a String[]
				 * where String[0] = host and String[1] = port
				 * Extra check to make sure we don't add ourself to the list
				 * 
				 * Also need extra step for Psych dept.
				 */
				if(!(connectionRootUrl.equals(myUrl))){
					String cleanUrl = connectionRootUrl.split("/")[2];
					if(cleanUrl.equals("www.colostate.edu"))
						cleanUrl = "www.colostate.edu/Depts/Psychology";
					connections.put(cleanUrl, connection);
				}			

			}catch(ArrayIndexOutOfBoundsException e){} // Catch out of bounds error to prevent program termination

		}
		scanner.close();
		
		// Instantiate the ThreadPool
		myPool = new CrawlerThreadPool(poolSize, this);

		// Open ServerSocket to accept data from other Messaging Nodes
		SERVER_SOCKET = new ServerSocket(port);
	}
	
	/**
	 * Start initial crawl task
	 */
	public void startTask(){
		/*
		 * Crawler task format is: Recursion depth, URL to crawl, parent URL, MyURL, ThreaPool, originator 
		 * (if not internal, then originator is URL of Crawler that sent the task, otherwise default of "internal")
		 */
		String originator = "internal";
		myPool.submit(new CrawlerTask(RECURSION_DEPTH, FULL_URL, FULL_URL, MY_URL, myPool, originator));	
	}

	/**
	 * Listen method with embedded Thread class to
	 * start listening for Crawler connections
	 */
	public void listen(){
		// "this" reference to use for spawning the listening Thread
		final Crawler crawler = this;
		// "listener" Thread to accept incoming connections
		Thread listener = new Thread(new Runnable() {
			public void run() {
				while(true){
					try {
						Socket client = SERVER_SOCKET.accept();
						/*
						 * The messaging node doesn't need to assign id's
						 * for record tracking, so defaulting to 0 for connectionID
						 */
						synchronized(this){
							Thread receive = new Thread(new TCPReceiverThread(client, crawler));
							receive.start();
						}							
					} catch (IOException e) {}
				}
			}
		});
		listener.start();

		// Display success message
		System.out.println("Crawler listening for connections on port: " + SERVER_SOCKET.getLocalPort());
	}

	/**
	 * This method runs a "heartbeat" to determine
	 * when all tasks are complete.
	 * It waits for this Crawlers task to complete,
	 * then checks to see of all other Crawlers have
	 * reported finished.
	 */
	public void heartBeat(){
		Thread heartBeat = new Thread(new Runnable() {
			public void run() {
				while(!allCrawlerCompleted()){
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});  
		/*
		 * Wait a few of seconds to start heartbeat
		 * This allows time for tasks to start
		 * since the task queue will be empty initially
		 * need to give it time to queue up some tasks
		 */
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// Start listening
		heartBeat.start();
	}

	/**
	 * Setup connections to other Crawlers
	 * @return boolean
	 */
	public boolean setupConnections(){
		boolean success = true;
		synchronized(connections){
			for (Map.Entry<String, String[]> entry : connections.entrySet()) {
				String rootUrl = entry.getKey();
				String[] connection = entry.getValue();
				Socket socket;
				try {
					socket = new Socket(connection[0], Integer.parseInt(connection[1]));
					TCPSender sender = new TCPSender(socket);
					// sender.start();
					/*
					 * If here, setup successful, add connection to connections list,
					 * and set completion tracker list to false for this entry
					 */
					myConnections.put(rootUrl, sender);
					crawlersComplete.put(rootUrl, false);
				} catch (UnknownHostException e) {
					success = false;
					System.out.println("Error connecting to crawler "+ connection[0] +", unknown host error occurred: ");
					System.err.println(e.getMessage());
				} catch (IOException e) {
					success = false;
					System.out.println("Error connecting to crawler " + connection[0] +": ");
					System.err.println(e.getMessage());
				}
			}
		}
		return success;
	}

	/**
	 * Simple helper class
	 * @param String
	 * @return
	 */
	private String[] delimitConfig(String config){
		return config.split(",");		
	}

	/**
	 * Returns the Crawlers rootUrl
	 * @return String
	 */
	public String getRootUrl(){
		return new String(MY_URL);
	}

	/**
	 * Receive events from other Crawlers
	 * @param Event
	 */
	@Override
	public void onEvent(Event event) {
		switch (event.getType()){

		case Protocol.CRAWLER_SENDS_TASK:
			receiveTaskFromCrawler(event);
			break;

		case Protocol.CRAWLER_SENDS_TASK_COMPLETE:
			crawlerReceivesTaskComplete(event);
			break;

		case Protocol.CRAWLER_SENDS_FINISHED:
			crawlerReceivesFinished(event);
			break;

		case Protocol.CRAWLER_SENDS_INCOMPLETE:
			crawlerReceivesIncomplete(event);
			break;

		default:
			System.out.println("Unrecognized event type received.");

		}
	}

	/**
	 * Called when a Crawler resets its status
	 * @param Event e
	 */
	private void crawlerReceivesIncomplete(Event e){
		synchronized(connections){
			CrawlerSendsIncomplete crawlerIncomplete = (CrawlerSendsIncomplete)e;
			crawlersComplete.put(crawlerIncomplete.getOriginatingUrl(), false);
		}
	}

	/**
	 * Send incomplete status to all Crawlers
	 */
	private void crawlerSendsIncomplete(){
		Event crawlerSendsIncomplete = ef.buildEvent(Protocol.CRAWLER_SENDS_INCOMPLETE, MY_URL);
		sendToAll(crawlerSendsIncomplete.getBytes());
	}

	/**
	 * Called when a Crawler has set its status to finished
	 * @param Event e
	 */
	private void crawlerReceivesFinished(Event e){
		synchronized(connections){
			CrawlerSendsFinished crawlerFinished = (CrawlerSendsFinished)e;
			crawlersComplete.put(crawlerFinished.getOriginatingUrl(), true);
		}
	}

	/**
	 * Send finished status to all Crawlers
	 */
	private void crawlerSendsFinished(){
		Event crawlerSendsFinished = ef.buildEvent(Protocol.CRAWLER_SENDS_FINISHED, MY_URL);
		sendToAll(crawlerSendsFinished.getBytes());
	}

	/**
	 * Receive task complete notification from Crawler
	 * @param Event e
	 */
	private void crawlerReceivesTaskComplete(Event e){
		synchronized(connections){
			CrawlerSendsTaskComplete taskComplete = (CrawlerSendsTaskComplete)e;
			/*
			 * Received finished task from Crawler, change status
			 * in forwarded task to true so we know it's completed
			 */
			String originator = taskComplete.getOriginatingUrl();
			Integer count = receiveTaskCount.get(originator);
			if (count == null) {
				receiveTaskCount.put(originator, 1);
			}
			else {
				receiveTaskCount.put(originator, count + 1);
			}
		}
	}

	/**
	 * Send completion Event to originating Crawler
	 * @param String destUrl
	 */
	public void crawlerSendsTaskComplete(String destUrl){
		synchronized(connections){
			Event crawlerSendsTaskComplete = ef.buildEvent(Protocol.CRAWLER_SENDS_TASK_COMPLETE, MY_URL);
			try {
				myConnections.get(destUrl).sendData(crawlerSendsTaskComplete.getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Receive task from other crawler
	 * @param Event e
	 */
	private void receiveTaskFromCrawler(Event e){
		synchronized(connections){
			// If receiving a new task, we're not finished, report it
			crawlerSendsIncomplete();
			// Create the new task and start it up
			CrawlerSendsTask task = (CrawlerSendsTask)e;
			String urlToCrawl = task.getUrlToCrawl();
			String originatingUrl = task.getOriginatingCrawlerUrl();
			String parentUrl = urlToCrawl;

			if(debug)
				System.out.println(""
						+ "\n\n************************************************************\n"
						+ " Received task from crawler ["+ originatingUrl+"]\n"
						+ " Requested crawl of URL: [" + urlToCrawl + "]\n"
						+ "************************************************************\n\n");

			CrawlerTask newTask = new CrawlerTask(RECURSION_DEPTH, urlToCrawl, parentUrl, MY_URL, myPool, originatingUrl);
			myPool.submit(newTask);
		}
	}

	/**
	 * Send URL to connected clients
	 * @param String crawlUrl
	 */
	public void sendTaskToCrawler(String crawlUrl){
		synchronized(connections){
			Event crawlerSendsTask = ef.buildEvent(Protocol.CRAWLER_SENDS_TASK, crawlUrl + ";" + MY_URL);
			try {
				for (String key : myConnections.keySet()) {
					if (crawlUrl.toLowerCase().contains(key.toLowerCase())) {
						if(debug)
							System.out.println(""
									+ "\n\n************************************************************\n"
									+ " Sending task to Crawler [" + key + "]\n"
									+ " Requesting crawl of URL: [" + crawlUrl + "]\n"
									+ "************************************************************\n\n");
						/*
						 * Need to keep track of forwarded tasks.
						 * Place dest. URL in forwarded tasks, with status false
						 */
						Integer count = forwardTaskCount.get(key);
						if (count == null) {
							forwardTaskCount.put(key, 1);
						}
						else {
							forwardTaskCount.put(key, count + 1);
						}
						myConnections.get(key).sendData(crawlerSendsTask.getBytes());
						break;
					}
				}
			} catch (IOException e) {
				// e.printStackTrace();
				System.out.println("Connection to Crawler lost, unable to send task to Crawler.");
			}
		}
	}

	/**
	 * Send message to all clients
	 * @param bytes
	 */
	private void sendToAll(byte[] bytes){
		synchronized(connections){
			for (String key : myConnections.keySet()) {
				try {
					myConnections.get(key).sendData(bytes);
				} catch (IOException e) {
					System.out.println("Error sending data to Crawlers");
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Check that all conditions for completion have been met
	 * If ANY of the crawlers haven't reported complete, or
	 * ANY of the tasks forwarded haven't reported finished
	 * return false.
	 * 
	 * The third condition needs to be checked separately.
	 * @return boolean status
	 */
	public boolean completionCheck(){
		synchronized(connections){
			/*
			 * This caused a hold forever, b/c each Crawler
			 * was waiting on other one to finish.
			 * 
			 * Need to do another check after I sent completion.
			 */
			for (String key : forwardTaskCount.keySet()) {
				if(!(forwardTaskCount.get(key).equals(receiveTaskCount.get(key))))
					return false;
			}
			if(!(myPool.isComplete()))
				return false;

			// Send finished message if done with all tasks
			crawlerSendsFinished();
		}
		return true;
	}

	/**
	 * This is the final check. If the above conditions hold,
	 * and all Crawlers report finished, then harvesting is complete.
	 * @return boolean status
	 */
	private boolean allCrawlerCompleted(){
		synchronized(connections){
			if(!completionCheck())
				return false;
			if(crawlersComplete.containsValue(false))
				return false;

			if(debug)
				System.out.println("\n\n******************************\n CRAWLER COMPLETED ALL TASKS \n******************************\n\n");
			/*
			 * If here, everything has successfully completed!!
			 * All my tasks are done, and all other Crawlers have reported
			 * to me they're complete.
			 * 
			 * create the directory structure for this Crawler
			 */
			
			if(timer){
				long endTime = System.nanoTime();
				long duration = (endTime - startTime)/1000000000; // duration in seconds
				System.out.println("\n\n*******************************************\n CRAWLER COMPLETED IN "+duration/60+"mins / "+duration%60+"secs \n*******************************************\n\n");
			}
			
			myPool.createDirectory();
			
			return true;
		}
	}

	/**
	 * Helper method, will print out statuses
	 */
	public void printCompletionReport(){
		synchronized(connections){
			System.out.println("\n\n******************************\n");
			System.out.println("Completion status of other Crawlers:");
			for (String key : crawlersComplete.keySet()) {
				System.out.println(key + " " + crawlersComplete.get(key));
			}
			System.out.println("\nForwarded tasks count:");
			for (String key : forwardTaskCount.keySet()) {
				System.out.println(key + " " + forwardTaskCount.get(key));
			}
			System.out.println("\nForwarded tasks completion count:");
			for (String key : receiveTaskCount.keySet()) {
				boolean complete = receiveTaskCount.get(key).equals(forwardTaskCount.get(key));
				System.out.println(key + " " + receiveTaskCount.get(key) + " (" + complete + ")");
			}
			System.out.println("\nThreadPool status: " + myPool.isComplete());
			System.out.println("\n******************************\n\n");
		}
	}

	/**
	 * Helper method to determine which Crawler this is
	 */
	public void whoAmI(){
		System.out.println("\n\n******************************\n");
		System.out.println(" I am Crawler: " + MY_URL);
		System.out.println("\n******************************\n\n");
	}

	/**
	 * Stop listening for other Crawlers, kill threads in pool, and close down
	 */
	public void stop(){
		try {
			myPool.stop();
			SERVER_SOCKET.close();
			System.exit(0);
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}finally{
			System.exit(-1);
		}
	}

}//************** END Crawler **************