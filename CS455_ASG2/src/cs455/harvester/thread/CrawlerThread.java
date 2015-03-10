/**
 * @author Shaun Parkison (shaunpa)
 * Colorado State University
 * CS455 - Dist. Systems
 */

package cs455.harvester.thread;

import cs455.harvester.task.CrawlerTask;


public class CrawlerThread extends Thread{

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
					try {
						task.start();
					} finally {
						if (!(task.getOriginator().equals("internal")))
							pool.sendComplete(task.getOriginator());
						// Notify ThreadPool we've completed our task
						pool.threadCompletedTask();
					}
					// "Niceness", sleep for 1 second after each task completion
					Thread.sleep(1000);
				} catch (Exception e) {
					//System.err.println(e.getMessage());
				}
			} else {
				if (!active)
					// If no longer active, break out of while
					break;
				else{
					// Else, queue was empty, wait until notified items have been added
					// and try again
					synchronized(pool.getWaitLock()) {
						try {
							pool.getWaitLock().wait();
						} catch (InterruptedException e) {
							//System.err.println(e.getMessage());
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
			active = false;
		}finally{
			this.interrupt();
		}
	}

}//************** END CrawlerThread **************
