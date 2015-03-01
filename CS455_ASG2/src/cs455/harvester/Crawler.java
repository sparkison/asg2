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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import cs455.harvester.task.CrawlerTask;
import cs455.harvester.thread.CrawlerThreadPool;
import cs455.harvester.transport.TCPReceiverThread;
import cs455.harvester.transport.TCPSender;
import cs455.harvester.wireformats.CrawlerSendsTask;
import cs455.harvester.wireformats.Event;
import cs455.harvester.wireformats.EventFactory;
import cs455.harvester.wireformats.Protocol;


public class Crawler implements Node{

	// Instance variables **************
	private final int RECURSION_DEPTH = 5;
	private final Map<String, String[]> CONNECTIONS = new HashMap<String, String[]>();
	private final ServerSocket svSocket;
	private final String MYURL;
	private Map<String, TCPSender> myConnections = new HashMap<String, TCPSender>();
	private CrawlerThreadPool myPool;
	private EventFactory ef = EventFactory.getInstance();

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
	public Crawler(int port, int poolSize, String rootUrl, String configPath) throws IOException{
		
		// Send only the www.rooturl.com portion of URL for easier checking
		MYURL = rootUrl.split("/")[2];
		
		// List of other crawler URLs to send to pool
		List<String> crawlers = new ArrayList<String>();
		
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
				 */
				//System.out.println(connectionRootUrl.split("/")[2]);
				if(!(connectionRootUrl.equals(rootUrl))){
					String cleanUrl = connectionRootUrl.split("/")[2];
					crawlers.add(cleanUrl);
					CONNECTIONS.put(cleanUrl, connection);
				}			
			}catch(ArrayIndexOutOfBoundsException e){} // Catch out of bounds error to prevent program termination

		}

		// Open ServerSocket to accept data from other Messaging Nodes
		this.svSocket = new ServerSocket(port);
		listen();

		// Display success message
		System.out.println("Crawler listening for connections on port: " + port);

		// instantiate the ThreadPool
		myPool = new CrawlerThreadPool(poolSize, crawlers, this);

		// Need to sleep for 10 seconds before starting up
		try {
			Thread.sleep(2000);	//TODO Need to change this to 10 seconds instead of 2
		} catch (InterruptedException e) {
			System.err.println(e.getMessage());
		}

		CrawlerTask t1 = new CrawlerTask(RECURSION_DEPTH, rootUrl, rootUrl, myPool);
		//myPool.submit(t1);

		this.stop();
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
						Socket client = svSocket.accept();
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
		synchronized(CONNECTIONS){
			for (Map.Entry<String, String[]> entry : CONNECTIONS.entrySet()) {
				String rootUrl = entry.getKey();
				String[] connection = entry.getValue();
				Socket socket;
				try {
					socket = new Socket(connection[0], Integer.parseInt(connection[1]));
					myConnections.put(rootUrl, new TCPSender(socket));
				} catch (UnknownHostException e) {
					success = false;
					System.out.println("Error connecting to client, unknown host error occurred: ");
					System.err.println(e.getMessage());
				} catch (IOException e) {
					success = false;
					System.out.println("Error connecting to client: ");
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
	 * Send URL to connected clients
	 * @param String
	 */
	public void sendTaskToCrawler(String crawlUrl){
		synchronized(myConnections){
			Event CrawlerSendsTask = ef.buildEvent(cs455.harvester.wireformats.Protocol.CRAWLER_SENDS_TASK, crawlUrl + ";" + MYURL);
			try {
				myConnections.get(crawlUrl).sendData(CrawlerSendsTask.getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
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
			break;
			
		case Protocol.CRAWLER_SENDS_FINISHED:
			break;
			
		default:
			System.out.println("Unrecognized event type sent.");
			
		}
	}
	
	/**
	 * Receive task from other crawler
	 * @param String
	 */
	private void receiveTaskFromCrawler(Event e){
		CrawlerSendsTask task = (CrawlerSendsTask) e;
		String urlToCrawl = task.getUrlToCrawl();
		String originatingUrl = task.getOriginatingCrawlerUrl();
		
		//TODO need to keep track of originating URL so we can send confirmation when task complete
		
		CrawlerTask newTask = new CrawlerTask(RECURSION_DEPTH, urlToCrawl, MYURL, myPool);
		myPool.submit(newTask);
	}

	/**
	 * Stop listening for other Crawlers, kill threads in pool, and close down
	 */
	public void stop(){
		try {
			myPool.stop();
			svSocket.close();
			System.exit(0);
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}

}//************** END Crawler **************
