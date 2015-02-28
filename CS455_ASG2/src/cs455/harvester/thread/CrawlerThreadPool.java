package cs455.harvester.thread;

import java.util.LinkedList;
import java.util.concurrent.Callable;

import cs455.harvester.util.ListenerInterface;
/**
 * This class is used to execute submitted {@link Callable} tasks. this class
 * creates and manages fixed number of threads User will provide a
 * {@link ListenerInterface}object in order to get the Result of submitted task
 *
 * @author abhishek
 *
 *
 */
public class CrawlerThreadPool<T> {
	private Object waitLock = new Object();
	public Object getWaitLock() {
		return waitLock;
	}
	/**
	 * list of threads for completing submitted tasks
	 */
	private final LinkedList<CrawlerThread<T>> threads;
	/**
	 * submitted task will be kept in this list until they run by one of
	 * threads in pool
	 */
	private final LinkedList<Callable<T>> tasks;
	/**
	 * shutDown flag to shut Down service
	 */
	private volatile boolean shutDown;
	/**
	 * ResultListener to get back the result of submitted tasks
	 */
	private ListenerInterface<T> resultListener;
	/**
	 * initializes the threadPool by starting the threads threads will wait till
	 * tasks are not submitted
	 *
	 * @param size
	 * Number of threads to be created and maintained in pool
	 * @param myResultListener
	 * ResultListener to get back result
	 */
	public CrawlerThreadPool(int size, ListenerInterface<T> myResultListener) {
		tasks = new LinkedList<Callable<T>>();
		threads = new LinkedList<CrawlerThread<T>>();
		shutDown = false;
		resultListener = myResultListener;
		for (int i = 0; i < size; i++) {
			CrawlerThread<T> hThread = new CrawlerThread<T>();
			hThread.setPool(this);
			threads.add(hThread);
			hThread.start();
		}
	}
	public ListenerInterface<T> getResultListener() {
		return resultListener;
	}
	public void setResultListener(ListenerInterface<T> resultListener) {
		this.resultListener = resultListener;
	}
	public boolean isShutDown() {
		return shutDown;
	}
	public int getThreadPoolSize() {
		return threads.size();
	}
	public synchronized Callable<T> removeFromQueue() {
		return tasks.poll();
	}
	public synchronized void addToTasks(Callable<T> callable) {
		tasks.add(callable);
	}
	/**
	 * submits the task to threadPool. will not accept any new task if shutDown
	 * is called Adds the task to the list and notify any waiting threads
	 *
	 * @param callable
	 */
	public void submit(Callable<T> callable) {
		if (!shutDown) {
			addToTasks(callable);
			synchronized (this.waitLock) {
				waitLock.notify();
			}
		} else {
			System.out.println("task is rejected.. Pool shutDown executed");
		}
	}
	/**
	 * Initiates a shutdown in which previously submitted tasks are executed,
	 * but no new tasks will be accepted. Waits if there are unfinished tasks
	 * remaining
	 *
	 */
	public void stop() {
		for (CrawlerThread<T> hThread : threads) {
			hThread.shutdown();
		}
		synchronized (this.waitLock) {
			waitLock.notifyAll();
		}
		for (CrawlerThread<T> hThread : threads) {
			try {
				hThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}