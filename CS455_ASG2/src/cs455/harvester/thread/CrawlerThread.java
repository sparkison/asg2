/**
 * @author Shaun Parkison (shaunpa)
 * Colorado State University
 * CS455 - Dist. Systems
 */

package cs455.harvester.thread;

import cs455.harvester.task.CrawlTask;

public class CrawlerThread extends Thread {

	// Instance variables **************
	private CrawlerThreadPool pool;
	private boolean active = true;

	public boolean isActive() {
		return active;
	}
	public void setPool(CrawlerThreadPool p) {
		pool = p;
	}

	/**
	 * Main run method for CralwerThread
	 * Will continue to poll the queue for tasks
	 * once task received, will run, then return results to listener
	 * and return itself to the pool for next task
	 */
	public void run() {

		CrawlTask task;

		while (true) {
			task = pool.removeFromQueue();
			if (task != null) {
				try {
					task.start();
				} catch (Exception e) {}
			} else {
				if (!isActive())
					break;
				else{
					synchronized (pool.getWaitLock()) {
						try {
							pool.getWaitLock().wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	/**
	 * Tell thread to stop executing
	 */
	void shutdown() {
		try{
			this.interrupt();
		}finally{
			active = false;
		}
	}
}
