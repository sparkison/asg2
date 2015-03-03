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
	private final LinkedList<CrawlerThread> THREADS;
	private final LinkedList<CrawlerTask> TASKS;
	private final AdjacencyList ADJACENCY;
	private final Crawler CRAWLER;
	private final Object TASK_LOCK = new Object();
	private final Object WAIT_LOCK = new Object();

	private List<String> crawled = new ArrayList<String>();
	private boolean debug = true;

	/**
	 * Main constructor for thread pool class
	 * @param size
	 */
	public CrawlerThreadPool(int size, Crawler CRAWLER) {
		// Crawler associated with this pool
		this.CRAWLER = CRAWLER;
		// List of TASKS to be performed
		TASKS = new LinkedList<CrawlerTask>();
		// List of CRAWLER THREADS
		THREADS = new LinkedList<CrawlerThread>();
		// Create our ADJACENCY list to build out the graph
		ADJACENCY = new AdjacencyList(CRAWLER.getRootUrl());
		// Volatile boolean for shut down
		shutDown = false;

		// Loop to start CRAWLER THREADS up
		for(int i = 0; i<size; i++) {
			CrawlerThread crawlThread = new CrawlerThread(this);
			THREADS.add(crawlThread);
			crawlThread.start();
		}

		// Create the root folder for this Crawler
		File file = new File(DIRECTORY_ROOT + CRAWLER.getRootUrl().replaceAll("[^a-zA-Z0-9._-]", "-") + "/nodes");
		if (!file.exists()) {
			if (file.mkdirs()) {
				//System.out.println("Directory is created!");
			} else {
				//System.out.println("Failed to create directory!");
			}
		}

	}//END CrawlerThreadPool

	/**
	 * This thread will sleep for 5 seconds to allow other Threads
	 * to continue polling the queue. If queue is still empty after 5 seconds
	 * report complete.
	 */
	public void checkCompletionStatus(){

		Thread completionChecker = new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(5000);
					synchronized(TASK_LOCK){
						if(TASKS.isEmpty() && !complete){
							taskComplete();
							if(debug)
								System.out.println("\n\n*************************\n CRAWLER COMPLETED ALL TASKS \n*************************\n\n");
						}
					}	
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});  
		completionChecker.start();
	}

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
	 * Forward crawl task to other CRAWLERs
	 * @param String
	 */
	public void forward(CrawlerTask task, String forwards){
		CRAWLER.sendTaskToCrawler(forwards);
	}

	/**
	 * Remove item from head of LinkedList for processing
	 * @return CrawlTask
	 */
	public CrawlerTask removeFromQueue() {
		synchronized(TASK_LOCK){
			CrawlerTask task = TASKS.poll();
			if(TASKS.isEmpty())
				checkCompletionStatus();
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
					if(debug)
						System.out.println("Task added: " + task);
					TASKS.add(task);
					ADJACENCY.addEdge(task.getParentUrl(), task.getCrawlUrl());
					crawled.add(task.getCrawlUrl());
				}
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