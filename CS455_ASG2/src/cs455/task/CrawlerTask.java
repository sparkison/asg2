/**
 * @author Shaun Parkison (shaunpa)
 * Colorado State University
 * CS455 - Dist. Systems
 */

package cs455.task;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import net.htmlparser.jericho.Config;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.LoggerProvider;
import net.htmlparser.jericho.Source;
import cs455.thread.CrawlerThreadPool;

public class CrawlerTask implements Task {

	// Instance variables **************
	private final int recursionDepth;
	private String parentUrl;
	private String crawlUrl;
	private String rootUrl;
	private CrawlerThreadPool crawlerPool;

	public CrawlerTask(int recursionDepth, String crawlUrl, String parentUrl, String rootUrl, CrawlerThreadPool crawlerPool){
		// Setting recursion depth to negative so we can increment up to 0
		// to make things more intuitive
		this.recursionDepth = recursionDepth;
		this.crawlUrl = crawlUrl;
		this.parentUrl = parentUrl;
		this.rootUrl = rootUrl;
		this.crawlerPool = crawlerPool;
	}

	@Override
	public void start() {
		// Check to make sure we haven't reached max depth
		if(recursionDepth > 0){
			// Not yet complete, make sure pool knows we're not done yet
			crawlerPool.resetComplete();
			int newDepth = recursionDepth - 1;
			// Crawl URL
			URLExtractor(crawlUrl, newDepth);
		}else{
			// We've reached the recursion depth, mark task as complete
			System.out.println("\n\n*********************\nCrawler reported task complete!!\n*********************");
			crawlerPool.taskComplete();
		}
	}

	public void URLExtractor(String url, int depth){

		Config.LoggerProvider = LoggerProvider.DISABLED;
		try {
			// Web page that needs to be parsed,
			// check for redirect before processing
			final String pageUrl = resolveRedirects(url);
			Source source = new Source(new URL(pageUrl));

			// get all 'a' tags
			List<Element> aTags = source.getAllElements(HTMLElementName.A);

			// get the URL ("href" attribute) in each 'a' tag
			for (Element aTag : aTags) {
				if(aTag != null){
					String pageLink = aTag.getAttributeValue("href").toString();
					if(pageLink.contains(rootUrl)) {
						// URL is a local URL, parse it
						CrawlerTask task = new CrawlerTask(depth, pageLink, pageUrl, rootUrl, crawlerPool);
						crawlerPool.submit(task);
					} else if(pageLink.charAt(0) == '#' || pageLink.charAt(0) == '/' || pageLink.charAt(0) == '.'){
						// URL is a relative URL, parse it
						CrawlerTask task = new CrawlerTask(depth, relativeToAbs(pageUrl, pageLink), pageUrl, rootUrl, crawlerPool);
						crawlerPool.submit(task);
					} else {
						// Need to forward it on...
						crawlerPool.forward(this, pageLink);
					}
				}				
			}

		} catch (IOException e) {} // in case of malformed url
	}

	/**
	 * Returns an absolute URL based on root and realtive URL passed
	 * @param String root
	 * @param String relative
	 * @return String absolute
	 */
	private String relativeToAbs(String root, String relative){
		String absolute = "";
		try {
			if(!new URI(relative).isAbsolute()){
				URI resolvedUrl = new URI(root).resolve(relative);
				absolute = resolvedUrl.toString();
			}
		} catch (URISyntaxException e1) {}
		return absolute;
	}

	/**
	 * Handle redirects
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public String resolveRedirects(String url) throws IOException {
		HttpURLConnection con = (HttpURLConnection)(new URL(url).openConnection());
		con.setInstanceFollowRedirects(false);
		con.connect();
		int responseCode = con.getResponseCode();
		if(responseCode == 301){
			return con.getHeaderField( "Location" );
		} else {
			return url;
		}
	}

	/**
	 * @return the crawlUrl
	 */
	public String getCrawlUrl() {
		return new String(crawlUrl);
	}

	/**
	 * @return the rootUrl
	 */
	public String getRootUrl() {
		return new String(rootUrl);
	}

	/**
	 * @return the parentUrl
	 */
	public String getParentUrl() {
		return new String(parentUrl);
	}

	@Override
	public String toString() {
		return "CrawlerTask [recursionDepth=" + recursionDepth + ", "
				+ (crawlUrl != null ? "crawlUrl=" + crawlUrl + ", " : "")
				+ (parentUrl != null ? "parentUrl=" + parentUrl + ", " : "")
				+ (rootUrl != null ? "rootUrl=" + rootUrl : "") + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((crawlUrl == null) ? 0 : crawlUrl.hashCode());
		result = prime * result + ((rootUrl == null) ? 0 : rootUrl.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof CrawlerTask)) {
			return false;
		}
		CrawlerTask other = (CrawlerTask) obj;
		if (crawlUrl == null) {
			if (other.crawlUrl != null) {
				return false;
			}
		} else if (!crawlUrl.equals(other.crawlUrl)) {
			return false;
		}
		if (rootUrl == null) {
			if (other.rootUrl != null) {
				return false;
			}
		} else if (!rootUrl.equals(other.rootUrl)) {
			return false;
		}
		return true;
	}

}//************** END CrawlTask **************
