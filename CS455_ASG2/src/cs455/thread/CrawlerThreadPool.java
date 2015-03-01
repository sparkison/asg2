/**
 * @author Shaun Parkison (shaunpa)
 * Colorado State University
 * CS455 - Dist. Systems
 */

package cs455.thread;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import cs455.harvester.Crawler;
import cs455.task.CrawlerTask;


public class CrawlerThreadPool{

	// Instance variables **************
	private volatile boolean shutDown;
	private volatile boolean complete;
	private final LinkedList<CrawlerThread> threads;
	private final LinkedList<CrawlerTask> tasks;
	private final Crawler crawler;
	private final List<String> crawlerConnections;
	private HashSet<CrawlerTask> crawled = new HashSet<CrawlerTask>();
	private Object waitLock = new Object();

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
		// Volatile boolean for shut down
		shutDown = false;

		// Loop to start crawler threads up
		for(int i = 0; i<size; i++) {
			CrawlerThread crawlThread = new CrawlerThread(this);
			threads.add(crawlThread);
			crawlThread.start();
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
		return new Boolean(shutDown);
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
	public void forward(String forwards){
		synchronized(crawlerConnections){
			if(crawlerConnections.contains(forwards)){
				// Forward connection
				crawler.sendTaskToCrawler(forwards);
			}
		}
		//TODO need to mark this as an outgoing link
	}
	
	/**
	 * 
	 * @param task
	 */
	public void confirmCrawled(CrawlerTask task){
		synchronized(crawled){
			//System.out.println("Task confirmed crawled: " + task);
			crawled.add(task);
		}
	}
	
	/**
	 * Remove item from head of LinkedList for processing
	 * @return CrawlTask
	 */
	public CrawlerTask removeFromQueue() {
		synchronized(tasks){
			return tasks.poll();
		}
	}

	/**
	 * Add item to tail of LinkedList for processing
	 * @param CrawlerTask
	 */
	public void submit(CrawlerTask task) {
		if(!shutDown) {
			
			/*
			 * If submitted task crawlUrl.equals(rootUrl) originated from this pools node,
			 * else originated from other node, need to mark as incoming link 
			 */
			
			// Add task to queue, if we haven't already crawled it
			synchronized(tasks){
				if(!(crawled.contains(task))){
					tasks.add(task);
					//System.out.println("Task added to queue: " + task);
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