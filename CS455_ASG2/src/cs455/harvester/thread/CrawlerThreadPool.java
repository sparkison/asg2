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
	 * @param myResultListener
	 */
	public CrawlerThreadPool(int size) {

		// Instance variables to keep track of things
		// List of tasks to be performed
		tasks = new LinkedList<CrawlTask>();
		// List of crawler threads
		threads = new LinkedList<CrawlerThread>();
		// Volatile boolean for shut down
		shutDown = false;

		// Loop to start crawler threads up
		for (int i = 0; i < size; i++) {
			CrawlerThread hThread = new CrawlerThread();
			hThread.setPool(this);
			threads.add(hThread);
			hThread.start();
		}
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
	 * Status methods
	 */

	public boolean isShutDown() {
		return shutDown;
	}

	/**
	 * Pop item from queue for processing
	 * @return callable
	 */
	public synchronized CrawlTask removeFromQueue() {
		return tasks.poll();
	}

	/**
	 * Add item to queue for processing
	 * @param callable
	 */
	public void submit(CrawlTask task) {
		if(!shutDown) {
			synchronized(this){
				tasks.add(task);
			}
			synchronized(this.waitLock){
				waitLock.notify();
			}
		} else {
			System.out.println("task is rejected.. Pool shutDown executed");
		}
	}

	/**
	 * Stop tasks, and shutdown
	 */
	public void stop() {
		for (CrawlerThread hThread : threads) {
			hThread.shutdown();
		}
		synchronized (this.waitLock) {
			waitLock.notifyAll();
		}
		for (CrawlerThread hThread : threads) {
			try {
				hThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}