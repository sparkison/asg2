/**
 * @author Shaun Parkison (shaunpa)
 * Colorado State University
 * CS455 - Dist. Systems
 */

package cs455.harvester.thread;

import java.util.LinkedList;

import cs455.harvester.task.CrawlerTask;


public class CrawlerThreadPool{

	// Instance variables **************
	private volatile boolean shutDown;
	private final LinkedList<CrawlerThread> threads;
	private final LinkedList<CrawlerTask> tasks;
	private Object waitLock = new Object();

	/**
	 * Main constructor for thread pool class
	 * @param size
	 */
	public CrawlerThreadPool(int size) {
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
	 * Get shutdown status
	 * @return
	 */
	public boolean isShutDown() {
		return new Boolean(shutDown);
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
			// Add task to queue
			synchronized(tasks){
				tasks.add(task);
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