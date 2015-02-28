package cs455.harvester.thread;

public interface ListenerInterface<T>  {

	public void finish(T obj);
	public void error(Exception ex);

}