package cs455.harvester.task;

public interface Task {

	public void setCrawlUrl(String url);
	public void setRecursionDepth(int n);
	public void start();
	
}
