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
	private final String MYURL;

	private Map<String, String[]> connections;
	private Map<String, TCPSender> myConnections;
	private Map<String, Boolean> forwardedTasks;
	private Map<String, Boolean> crawlersComplete;
	private CrawlerThreadPool myPool;
	private EventFactory ef = EventFactory.getInstance();
	private boolean debug = true;

	public static void main(String[] args) throws InterruptedException {
		if(args.length < 3){
			System.out.println("Incorrect format used to initate Crawler\nPlease use: cs455.harvester.Crawler [portNum] [poolSize] [rootUrl] [configPath]");
		}else{
			int port = Integer.parseInt(args[0]);
			int poolSize = Integer.parseInt(args[1]);
			String rootUrl = args[2];
			String configPath = args[3];
			try {
				new Crawler(port, poolSize, rootUrl, configPath);
			} catch (IOException e) {
				System.err.println(e.getMessage());
			}
		}		
	}

	/**
	 * Crawler constructor
	 * @param port
	 * @param poolSize
	 * @param rootUrl
	 * @param configPath
	 * @throws IOException 
	 */
	public Crawler(int port, int poolSize, String myUrl, String configPath) throws IOException{
		// Initialize our containers for other Crawler connections
		connections = new HashMap<String, String[]>();
		myConnections = new HashMap<String, TCPSender>();
		forwardedTasks = new HashMap<String, Boolean>();
		crawlersComplete = new HashMap<String, Boolean>();
		// Send only the www.root_url.com portion of URL for easier checking
		// Checking for special case for Psych dept.
		String rootUrl = myUrl.split("/")[2];
		if(rootUrl.equals("www.colostate.edu"))
			MYURL = "www.colostate.edu/Depts/Psychology";
		else
			MYURL = rootUrl;

		// Path to configuration file
		Path path = Paths.get(configPath);
		@SuppressWarnings("resource")
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
					crawlersComplete.put(cleanUrl, false);
				}			

			}catch(ArrayIndexOutOfBoundsException e){} // Catch out of bounds error to prevent program termination

		}

		// Open ServerSocket to accept data from other Messaging Nodes
		this.SERVER_SOCKET = new ServerSocket(port);
		listen();

		// Display success message
		System.out.println("Crawler listening for connections on port: " + port);

		// Instantiate the ThreadPool
		myPool = new CrawlerThreadPool(poolSize, this);

		// Need to sleep for 10 seconds before starting up
		try {
			Thread.sleep(5000);	//TODO Need to change this to 10 seconds instead of 2
		} catch (InterruptedException e) {
			System.err.println(e.getMessage());
		}

		// Setup connections to other Crawlers
		setupConnections();

		String type = "internal";
		CrawlerTask task1 = new CrawlerTask(RECURSION_DEPTH, myUrl, myUrl, MYURL, myPool, type);
		myPool.submit(task1);
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
					myConnections.put(rootUrl, new TCPSender(socket));
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
		return new String(MYURL);
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

		case Protocol.CRAWLER_SENDS_TASK_COMPLETE:
			crawlerReceivesTaskComplete(event);

		case Protocol.CRAWLER_SENDS_FINISHED:
			crawlerReceivesFinished(event);

		case Protocol.CRAWLER_SENDS_INCOMPLETE:
			crawlerReceivesIncomplete(event);

		default:
			System.out.println("Unrecognized event type sent.");

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
		Event crawlerSendsIncomplete = ef.buildEvent(Protocol.CRAWLER_SENDS_INCOMPLETE, MYURL);
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
			crawlerSendsFinished();
		}
	}

	/**
	 * Send finished status to all Crawlers
	 */
	public void crawlerSendsFinished(){
		if(completionCheck()){
			Event crawlerSendsFinished = ef.buildEvent(Protocol.CRAWLER_SENDS_FINISHED, MYURL);
			sendToAll(crawlerSendsFinished.getBytes());
		}
	}

	/**
	 * Receive task complete notification from Crawler
	 * @param Event e
	 */
	private void crawlerReceivesTaskComplete(Event e){
		synchronized(connections){
			CrawlerSendsTaskComplete taskComplete = (CrawlerSendsTaskComplete)e;
			forwardedTasks.put(taskComplete.getOriginatingUrl(), true);
			crawlerSendsFinished();
		}
	}

	/**
	 * Send completion Event to originating Crawler
	 * @param String destUrl
	 */
	public void crawlerSendsTaskComplete(String destUrl){
		Event crawlerSendsTaskComplete = ef.buildEvent(Protocol.CRAWLER_SENDS_TASK_COMPLETE, MYURL);
		synchronized(connections){
			if (myConnections.containsKey(destUrl)) {
				try {
					myConnections.get(destUrl).sendData(crawlerSendsTaskComplete.getBytes());
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				System.out.println("Unable to send notification to client. Not in connections list.");
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
				System.out.println("\n\n**************************************************\n "
						+ "Received task from crawler ["+ originatingUrl+"] "
						+ "\n**************************************************\n\n");

			CrawlerTask newTask = new CrawlerTask(RECURSION_DEPTH, urlToCrawl, parentUrl, MYURL, myPool, originatingUrl);
			myPool.submit(newTask);
		}
	}

	/**
	 * Send URL to connected clients
	 * @param String crawlUrl
	 */
	public void sendTaskToCrawler(String crawlUrl){
		Event crawlerSendsTask = ef.buildEvent(Protocol.CRAWLER_SENDS_TASK, crawlUrl + ";" + MYURL);
		synchronized(connections){
			try {
				for (String key : myConnections.keySet()) {
					if (crawlUrl.contains(key)) {
						if(debug)
							System.out.println("\n\n**************************************************\n"
									+ "Sending task to Crawler " + key
									+ "\n**************************************************\n\n");
						/*
						 * Need to keep track of forwarded tasks.
						 * Check to see if we've already added this Crawler, if not add it and
						 * set the forwards boolean to false.
						 */
						forwardedTasks.put(key, false);
						myConnections.get(key).sendData(crawlerSendsTask.getBytes());
						break;
					}
				}
			} catch (IOException e) {
				// e.printStackTrace();
				System.out.println("Connection to Crawler lost, removing from list...");
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
	 * ANY of the tasks forwarded haven't reported finished, or
	 * ANY of this Crawlers tasks aren't complete, then
	 * return FALSE
	 */
	public boolean completionCheck(){
		synchronized(connections){
			if(crawlersComplete.containsValue(false))
				return false;
			if(forwardedTasks.containsValue(false))
				return false;
			if(!(myPool.isComplete()))
				return false;
		}
		if(debug)
			System.out.println("\n\n******************************\n CRAWLER COMPLETED ALL TASKS \n******************************\n\n");
		return true;
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
		}
	}

}//************** END Crawler **************