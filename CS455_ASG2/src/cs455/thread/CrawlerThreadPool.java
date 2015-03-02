/**
 * @author Shaun Parkison (shaunpa)
 * Colorado State University
 * CS455 - Dist. Systems
 */

package cs455.thread;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import cs455.harvester.Crawler;
import cs455.task.CrawlerTask;
import cs455.util.DirectoryEntry;


public class CrawlerThreadPool{

	// Instance variables **************
	private volatile boolean shutDown;
	private volatile boolean complete;

	private final String DIRECTORY_ROOT = "/tmp/shaunpa/";
	private final LinkedList<CrawlerThread> threads;
	private final LinkedList<CrawlerTask> tasks;
	private final LinkedList<DirectoryEntry> directory;
	private final Crawler crawler;
	private final List<String> crawlerConnections;
	private final ReentrantLock taskLock = new ReentrantLock();

	private List<String> crawled = new ArrayList<String>();
	private Object waitLock = new Object();
	private Object directoryLock = new Object();

	/**
	 * Main constructor for thread pool class
	 * @param size
	 */
	public CrawlerThreadPool(int size, List<String> crawlerConnections, Crawler crawler) {
		// Crawler associated with this pool
		this.crawler = crawler;
		// List of connections
		this.crawlerConnections = crawlerConnections;
		// List of tasks to be performed
		tasks = new LinkedList<CrawlerTask>();
		// List of crawler threads
		threads = new LinkedList<CrawlerThread>();
		// create directory list, used for creating the directory structure
		directory = new LinkedList<DirectoryEntry>();
		// Volatile boolean for shut down
		shutDown = false;

		// Loop to start crawler threads up
		for(int i = 0; i<size; i++) {
			CrawlerThread crawlThread = new CrawlerThread(this);
			threads.add(crawlThread);
			crawlThread.start();
		}

		// Create the root folder for this Crawler
		File file = new File(DIRECTORY_ROOT + crawler.getRootUrl() + "/nodes");
		if (!file.exists()) {
			if (file.mkdirs()) {
				//System.out.println("Directory is created!");
			} else {
				//System.out.println("Failed to create directory!");
			}
		}

		// A separate thread for Thread creation
		startDirectoryCreator();

	}//END CrawlerThreadPool

	/**
	 * Thread for creating directories and files
	 */
	private void startDirectoryCreator(){
		Thread directoryCreator = new Thread(new Runnable() {
			public void run() {
				DirectoryEntry entry;
				while(true) {
					// Attempt to get a task from the queue
					entry = directory.poll();
					if(entry != null) {
						try {
							
							//TODO create directory and files as needed, add link to in/out files
							
						} catch (Exception e) {}
					} else {
						if (!shutDown)
							// If no longer active, break out of while
							break;
						else{
							// Else, queue was empty, wait until notified items have been added
							// and try again
							try {
								directoryLock.wait();
							} catch (InterruptedException e) {}
						}
					}
				}//END while
			}
		});  
		directoryCreator.start();
	}

	/**
	 * Getters
	 */
	public int getThreadPoolSize() {
		return threads.size();
	}
	public Object getWaitLock() {
		return waitLock;
	}

	/**
	 * Return shutdown status
	 * @return boolean
	 */
	public boolean isShutDown() {
		return shutDown ? true : false;
	}

	/**
	 * Tells whether this ThreadPool has finished all tasks
	 * @return
	 */
	public boolean isComplete() {
		return new Boolean(complete);
	}

	/**
	 * Used to set the completion status once
	 * recursive depth has been reached
	 */
	public void taskComplete(){
		complete = true;
	}

	/**
	 * If new task comes in, need to reset recursion
	 * by marking task as incomplete
	 */
	public void resetComplete(){
		complete = false;
	}

	/**
	 * Forward crawl task to other crawlers
	 * @param String
	 */
	public void forward(CrawlerTask task, String forwards){
		synchronized(crawlerConnections){
			if(crawlerConnections.contains(forwards)){
				crawler.sendTaskToCrawler(forwards);
			}
		}
	}

	/**
	 * 
	 * @param task
	 */
	public void confirmCrawled(CrawlerTask task){
		if(task.getCrawlUrl() != ""){
			taskLock.lock();
			try{
				crawled.add(task.getCrawlUrl());
			} finally {
				taskLock.unlock();
			}
			try {
					
				synchronized(directory){
					URL url = new URL(task.getParentUrl());
					String directoryUrl = url.getPath();
					String[] temp = directoryUrl.split("/");
					directoryUrl = "";
					for(int i = 1; i<temp.length; i++){
						directoryUrl += temp[i].replaceAll("[^a-zA-Z0-9.]", "-");
						if(i != temp.length-1)
							directoryUrl += "-";
					}

					// Add info to directory list. Use new thread to poll 
					// for additions and create directory with in/out file and associated links
					DirectoryEntry directoryEntry = new DirectoryEntry(directoryUrl, task.getCrawlUrl(), "out");
					directory.add(directoryEntry);

				}
				
				synchronized(directoryLock){
					directoryLock.notify();
				}

			} catch (MalformedURLException e) {}
		}
	}

	/**
	 * Remove item from head of LinkedList for processing
	 * @return CrawlTask
	 */
	public CrawlerTask removeFromQueue() {
		taskLock.lock();
		try{
			return tasks.poll();
		} finally {
			taskLock.unlock();
		}
	}

	/**
	 * Add item to tail of LinkedList for processing
	 * @param CrawlerTask
	 */
	public void submit(CrawlerTask task) {
		if(!shutDown) {
			taskLock.lock();
			// Add task to queue, if we haven't already crawled it
			try{
				if(!(crawled.contains(task.getCrawlUrl()))){
					tasks.add(task);
				}
			} finally {
				taskLock.unlock();
			}
			// If any threads waiting, notify task added to queue
			synchronized(waitLock){
				waitLock.notify();
			}
		} else {
			System.out.println("Unable to add task to queue, CrawlerThreadPool has shutdown...");
		}
	}

	/**
	 * Stop all threads, and shutdown
	 */
	public void stop() {
		// Call shutdown on all threads in pool
		for(CrawlerThread crawlThread : threads) {
			crawlThread.shutdown();
		}
		// Notify all threads still waiting on lock to finish
		synchronized (this.waitLock) {
			waitLock.notifyAll();
		}
		// Finally, wait for all threads to complete tasks
		for(CrawlerThread crawlThread : threads) {
			try {
				crawlThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}//************** END CrawlerThreadPool **************