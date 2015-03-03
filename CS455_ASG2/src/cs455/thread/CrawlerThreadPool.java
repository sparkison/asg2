/**
 * @author Shaun Parkison (shaunpa)
 * Colorado State University
 * CS455 - Dist. Systems
 */

package cs455.thread;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import cs455.harvester.Crawler;
import cs455.task.CrawlerTask;
import cs455.util.AdjacencyList;


public class CrawlerThreadPool{

	// Instance variables **************
	private volatile boolean shutDown;
	private volatile boolean complete;

	private final String DIRECTORY_ROOT = "/tmp/cs455-shaunpa/";
	private final LinkedList<CrawlerThread> threads;
	private final LinkedList<CrawlerTask> tasks;
	private final AdjacencyList adjacency;
	private final Crawler crawler;
	private final Object taskLock = new Object();

	private List<String> crawled = new ArrayList<String>();
	private Object waitLock = new Object();
	private boolean debug = true;

	/**
	 * Main constructor for thread pool class
	 * @param size
	 */
	public CrawlerThreadPool(int size, Crawler crawler) {
		// Crawler associated with this pool
		this.crawler = crawler;
		// List of tasks to be performed
		tasks = new LinkedList<CrawlerTask>();
		// List of crawler threads
		threads = new LinkedList<CrawlerThread>();
		// Create our adjacency list to build out the graph
		adjacency = new AdjacencyList(crawler.getRootUrl());
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

	}//END CrawlerThreadPool

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
		crawler.sendTaskToCrawler(forwards);
	}

	/**
	 * Remove item from head of LinkedList for processing
	 * @return CrawlTask
	 */
	public CrawlerTask removeFromQueue() {
		synchronized(taskLock){
			return tasks.poll();
		}		
	}

	/**
	 * Add item to tail of LinkedList for processing
	 * @param CrawlerTask
	 */
	public void submit(CrawlerTask task) {
		if(!shutDown) {
			// Add task to queue, if we haven't already crawled it
			synchronized(taskLock){
				if(!(crawled.contains(task.getCrawlUrl()))){
					if(debug)
						System.out.println("Task added: " + task);
					tasks.add(task);
					crawled.add(task.getCrawlUrl());
					adjacency.addEdge(task.getParentUrl(), task.getCrawlUrl());
				}
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