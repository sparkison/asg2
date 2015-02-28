/**
 * @author Shaun Parkison (shaunpa)
 * Colorado State University
 * CS455 - Dist. Systems
 */

package cs455.harvester.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cs455.harvester.Crawler;

public class CrawlTask implements Task {

	// Instance variables **************
	private int recursionDepth;
	private String crawlUrl;
	private String rootUrl;
	private Crawler crawler;
	private Map<Integer, ArrayList<String>> recursionLevels = new HashMap<Integer, ArrayList<String>>();
	
	public CrawlTask(int recursionDepth, String crawlUrl, String rootUrl, Crawler crawler){
		this.recursionDepth = recursionDepth;
		this.crawlUrl = crawlUrl;
		this.rootUrl = rootUrl;
		this.crawler = crawler;
	}
	
	@Override
	public void start() {
		
		// TODO Auto-generated method stub
		
	}

}//************** END CrawlTask **************
