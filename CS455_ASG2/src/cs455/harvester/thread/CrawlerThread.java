package cs455.harvester.thread;

import java.util.concurrent.Callable;

import cs455.harvester.util.ListenerInterface;
/**
 * Run submitted task of {@link CrawlerThreadPool} After running the task , It calls
 * on {@link ListenerInterface}object with {@link Output}which contains returned
 * result of {@link Callable}task. Waits if the pool is empty.
 *
 * @author abhishek
 *
 * @param <T>
 */
public class CrawlerThread<T> extends Thread {
	/**
	 * MyThreadPool object, from which the task to be run
	 */
	private CrawlerThreadPool<T> pool;
	private boolean active = true;
	public boolean isActive() {
		return active;
	}
	public void setPool(CrawlerThreadPool<T> p) {
		pool = p;
	}
	/**
	 * Checks if there are any unfinished tasks left. if there are , then runs
	 * the task and call back with output on resultListner Waits if there are no
	 * tasks available to run If shutDown is called on MyThreadPool, all waiting
	 * threads will exit and all running threads will exit after finishing the
	 * task
	 */
	public void run() {
		ListenerInterface<T> result = pool.getResultListener();
		Callable<T> task;
		while (true) {
			task = pool.removeFromQueue();
			if (task != null) {
				try {
					T output = task.call();
					result.finish(output);
				} catch (Exception e) {
					result.error(e);
				}
			} else {
				if (!isActive())
					break;
				else{
					synchronized (pool.getWaitLock()) {
						try {
							pool.getWaitLock().wait();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
	void shutdown() {
		active = false;
	}
}
