/**
 * @author Shaun Parkison (shaunpa)
 * Colorado State University
 * CS455 - Dist. Systems
 */

package cs455.harvester.task;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.htmlparser.jericho.Config;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.LoggerProvider;
import net.htmlparser.jericho.Source;
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
	
	public void URLExtractor(){
		/**
		 * This class demonstrates how to use the Jericho HTML parser
		 * to extract URLs of a given web pages.
		 */
		// disable verbose log statements
		Config.LoggerProvider = LoggerProvider.DISABLED;
		try {
			// web page that needs to be parsed
			final String pageUrl = "http://www.cs.colostate.edu/~cs455";
			Source source = new Source(new URL(pageUrl));
			// get all 'a' tags
			List<Element> aTags = source.getAllElements(HTMLElementName.A);
			// get the URL ("href" attribute) in each 'a' tag
			for (Element aTag : aTags) {
				// print the url
				System.out.println(aTag.getAttributeValue("href"));
			}
		} catch (IOException e) { // in case of malformed url
			System.err.println(e.getMessage());
		}
	}

}//************** END CrawlTask **************
