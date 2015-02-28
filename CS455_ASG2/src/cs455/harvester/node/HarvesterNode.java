package cs455.harvester.node;

import java.util.concurrent.Callable;
import net.htmlparser.jericho.*;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import cs455.harvester.thread.ResultListener;
import cs455.harvester.thread.HarvesterThreadPool;

public class HarvesterNode {

	public static void main(String[] args) throws InterruptedException {

		ResultListener result = new ResultListener();
		HarvesterThreadPool newPool = new HarvesterThreadPool(10, result);
		for(int i =0;i<5000;i++){
			newPool.submit(new MyRunable (i));
		}
		newPool.stop();
		
		HarvesterNode hv = new HarvesterNode();
		
		hv.URLExtractor();
		
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

}

class MyRunable implements Callable{
	int index = -1;
	public MyRunable(int index){
		this.index = index;
	}
	@Override
	public Integer call() throws Exception {
		return index;
	}

}