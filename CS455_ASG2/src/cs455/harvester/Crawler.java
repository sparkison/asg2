/**
 * @author Shaun Parkison (shaunpa)
 * Colorado State University
 * CS455 - Dist. Systems
 */

package cs455.harvester;

import cs455.harvester.task.CrawlerTask;
import cs455.harvester.thread.CrawlerThreadPool;
import cs455.harvester.wireformats.Event;

public class Crawler implements Node{

	// Instance variables **************
	private static final int RECURSION_DEPTH = 5;
	//	private int port;
	//	private int poolSize;
	//	private String rootUrl;
	//	private String configPath;
	private CrawlerThreadPool myPool;

	public static void main(String[] args) throws InterruptedException {
		if(args.length < 3){
			System.out.println("Incorrect format used to initate Crawler\nPlease use: cs455.harvester.Crawler [portNum] [poolSize] [rootUrl] [configPath]");
		}else{
			int port = Integer.parseInt(args[0]);
			int poolSize = Integer.parseInt(args[1]);
			String rootUrl = args[2];
			String configPath = args[3];
			new Crawler(port, poolSize, rootUrl, configPath);
		}		
	}

	public Crawler(int port, int poolSize, String rootUrl, String configPath){
		//		this.port = port;
		//		this.poolSize = poolSize;
		//		this.rootUrl = rootUrl;
		//		this.configPath = configPath;

		myPool = new CrawlerThreadPool(poolSize);
		CrawlerTask t1 = new CrawlerTask(RECURSION_DEPTH, rootUrl, rootUrl, myPool);
		myPool.submit(t1);
	}

	@Override
	public void onEvent(Event e) {
		// TODO Auto-generated method stub
	}


}//************** END Crawler **************
