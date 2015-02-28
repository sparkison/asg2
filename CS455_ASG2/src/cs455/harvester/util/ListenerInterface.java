package cs455.harvester.util;

public interface ListenerInterface<T>  {

	public void finish(T obj);
	public void error(Exception ex);

}