/**
 * @author Shaun Parkison (shaunpa)
 * Colorado State University
 * CS455 - Dist. Systems
 */

package cs455.thread;

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

	private final LinkedList<CrawlerThread> THREADS;
	private final LinkedList<CrawlerTask> TASKS;
	private final AdjacencyList ADJACENCY;
	private final Crawler CRAWLER;
	private final Object TASK_LOCK = new Object();
	private final Object WAIT_LOCK = new Object();

	private List<String> crawled = new ArrayList<String>();
	private boolean debug = false;

	/**
	 * Main constructor for thread pool class
	 * @param size
	 */
	public CrawlerThreadPool(int size, Crawler crawler) {
		// Crawler associated with this pool
		CRAWLER = crawler;
		// List of TASKS to be performed
		TASKS = new LinkedList<CrawlerTask>();
		// List of CRAWLER THREADS
		THREADS = new LinkedList<CrawlerThread>();
		// Create our ADJACENCY list to build out the graph
		ADJACENCY = new AdjacencyList(CRAWLER.getRootUrl(), CRAWLER);
		// Volatile boolean for shut down
		shutDown = false;

		// Loop to start CRAWLER THREADS up
		for(int i = 0; i<size; i++) {
			CrawlerThread crawlThread = new CrawlerThread(this);
			THREADS.add(crawlThread);
			crawlThread.start();
		}

	}//END CrawlerThreadPool

	/**
	 * Getters
	 */
	public int getThreadPoolSize() {
		return THREADS.size();
	}
	public Object getWaitLock() {
		return WAIT_LOCK;
	}

	/**
	 * Return shutdown status
	 * @return boolean
	 */
	public boolean isShutDown() {
		return shutDown ? true : false;
	}

	/**
	 * Tells whether this ThreadPool has finished all TASKS
	 * @return
	 */
	public boolean isComplete() {
		return complete ? true : false;
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
	 * Forward crawl complete task to originating Crawler
	 * @param String
	 */
	public void sendComplete(String originatingUrl){
		CRAWLER.crawlerSendsTaskComplete(originatingUrl);
	}	

	/**
	 * Forward crawl task to other Crawler
	 * @param String
	 */
	public void forwardTask(String forwards){
		CRAWLER.sendTaskToCrawler(forwards);
	}

	/**
	 * Calls on the AdjacencyList to create the
	 * directory structure associated with this Crawler
	 */
	public void createDirectory(){
		ADJACENCY.startDirectoryCreation();
	}

	/**
	 * Adds broken link to adjacency list Object
	 * @param url
	 */
	public void reportBrokenLink(String url){
		ADJACENCY.addBrokenLink(url);
	}

	/**
	 * Remove item from head of LinkedList for processing
	 * Check if queue is empty, if yes set
	 * ThreadPool to complete
	 * @return CrawlTask
	 */
	public CrawlerTask removeFromQueue() {
		synchronized(TASK_LOCK){
			CrawlerTask task = TASKS.poll();
			if(TASKS.isEmpty())
				taskComplete();
			else
				resetComplete();
			return task;
		}
	}

	/**
	 * Add item to tail of LinkedList for processing
	 * @param CrawlerTask
	 */
	public void submit(CrawlerTask task) {
		if(!shutDown) {
			// Add task to queue, if we haven't already crawled it
			synchronized(TASK_LOCK){
				if(!(crawled.contains(task.getCrawlUrl()))){
					// Reset completion status, if previously set to complete
					resetComplete();
					// Mark as crawled to prevent duplicate crawling
					crawled.add(task.getCrawlUrl());

					if(debug)
						System.out.println("Task added: " + task);

					// Add the task, and add it to our adjacency list
					TASKS.add(task);
				} else {
					/*
					 * Already crawled
					 * If originated from outside this Crawler, send
					 * task complete message to originator
					 */
					if (!(task.getOriginator().equals("internal"))) {
						sendComplete(task.getOriginator());
					}
				}
				
				if (!(task.getOriginator().equals("internal")))
					ADJACENCY.addEdge(task.getOriginator(), task.getCrawlUrl());
				else
					ADJACENCY.addEdge(task.getParentUrl(), task.getCrawlUrl());
				
			}
			// If any THREADS waiting, notify task added to queue
			synchronized(WAIT_LOCK){
				WAIT_LOCK.notify();
			}
		} else {
			System.out.println("Unable to add task to queue, CrawlerThreadPool has shutdown...");
		}
	}

	/**
	 * Stop all THREADS, and shutdown
	 */
	public void stop() {
		// Call shutdown on all THREADS in pool
		for(CrawlerThread crawlThread : THREADS) {
			crawlThread.shutdown();
		}
		// Notify all THREADS still waiting on lock to finish
		synchronized (this.WAIT_LOCK) {
			WAIT_LOCK.notifyAll();
		}
		// Finally, wait for all THREADS to complete TASKS
		for(CrawlerThread crawlThread : THREADS) {
			try {
				crawlThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}//************** END CrawlerThreadPool **************