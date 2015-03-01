/**
 * @author Shaun Parkison (shaunpa)
 * Colorado State University
 * CS455 - Dist. Systems
 */

package cs455.harvester.thread;

import cs455.harvester.task.CrawlerTask;


public class CrawlerThread extends Thread {

	// Instance variables **************
	private CrawlerThreadPool pool;
	private boolean active = true;

	public CrawlerThread(CrawlerThreadPool pool){
		this.pool = pool;
	}
	
	/**
	 * Main run method for CralwerThread
	 * Will continue to poll the queue for tasks
	 * once task received, will run, then return results to listener
	 * and return itself to the pool for next task
	 */
	public void run() {
		
		CrawlerTask task;

		while(true) {
			// Attempt to get a task from the queue
			task = pool.removeFromQueue();
			if(task != null) {
				try {
					task.start();
					// Niceness
					Thread.sleep(1000);
				} catch (Exception e) {}
			} else {
				if (!active)
					// If no longer active, break out of while
					break;
				else{
					// Else, try to get lock. This is a blocking call.
					synchronized(pool.getWaitLock()) {
						try {
							pool.getWaitLock().wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}//END while
		
	}//END run

	/**
	 * Tell thread to stop executing
	 */
	void shutdown() {
		// Set active to false, then interrupt the thread to ensure it dies
		try{
			this.interrupt();
		}finally{
			active = false;
		}
	}
	
}//************** END CrawlerThread **************
