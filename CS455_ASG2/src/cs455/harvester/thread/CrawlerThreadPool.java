/**
 * @author Shaun Parkison (shaunpa)
 * Colorado State University
 * CS455 - Dist. Systems
 */

package cs455.harvester.thread;

import java.util.LinkedList;

import cs455.harvester.task.CrawlTask;


public class CrawlerThreadPool {

	// Instance variables **************
	private volatile boolean shutDown;
	private final LinkedList<CrawlerThread> threads;
	private final LinkedList<CrawlTask> tasks;
	private Object waitLock = new Object();

	/**
	 * Main constructor for thread pool class
	 * @param size
	 */
	public CrawlerThreadPool(int size) {
		// List of tasks to be performed
		tasks = new LinkedList<CrawlTask>();
		// List of crawler threads
		threads = new LinkedList<CrawlerThread>();
		// Volatile boolean for shut down
		shutDown = false;

		// Loop to start crawler threads up
		for(int i = 0; i<size; i++) {
			CrawlerThread crawlThread = new CrawlerThread();
			crawlThread.setPool(this);
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
	 * Get shutdown status
	 * @return
	 */
	public boolean isShutDown() {
		return shutDown;
	}

	/**
	 * Remove item from head of LinkedList for processing
	 * @return CrawlTask
	 */
	public CrawlTask removeFromQueue() {
		synchronized(tasks){
			return tasks.poll();
		}
	}

	/**
	 * Add item to tail of LinkedList for processing
	 * @param CrawlTask
	 */
	public void submit(CrawlTask task) {
		if(!shutDown) {
			synchronized(tasks){
				tasks.add(task);
			}
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
		for (CrawlerThread crawlThread : threads) {
			crawlThread.shutdown();
		}
		synchronized (this.waitLock) {
			waitLock.notifyAll();
		}
		for (CrawlerThread crawlThread : threads) {
			try {
				crawlThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}//************** END CrawlerThreadPool **************