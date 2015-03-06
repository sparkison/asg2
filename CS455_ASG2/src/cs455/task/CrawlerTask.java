/**
 * @author Shaun Parkison (shaunpa)
 * Colorado State University
 * CS455 - Dist. Systems
 */

package cs455.task;

import java.io.IOException;
import java.net.HttpURLConnection;
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
	private final int RECURSION_DEPTH;
	private final String PARENT_URL;
	private final String CRAWL_URL;
	private final String ROOT_URL;
	private final String ORIGINATOR;
	private final CrawlerThreadPool CRAWLER_POOL;

	public CrawlerTask(int recursionDepth, String crawlUrl, String parentUrl, String rootUrl, CrawlerThreadPool crawlerPool, String originator){
		// Setting recursion depth to negative so we can increment up to 0
		// to make things more intuitive
		RECURSION_DEPTH = recursionDepth;
		CRAWL_URL = relativeToAbs(parentUrl, crawlUrl);
		PARENT_URL = parentUrl;
		ROOT_URL = rootUrl;
		ORIGINATOR = originator;
		CRAWLER_POOL = crawlerPool;
	}

	@Override
	public void start() {
		// Check to make sure we haven't reached max depth
		int newDepth = RECURSION_DEPTH - 1;
		if(newDepth > 0){
			// Crawl URL
			URLExtractor(CRAWL_URL, newDepth);
		}
		
		/*
		 * Type is set to "internal" if set by this Crawler
		 * if this was a forwarded task, Type will be set to the
		 * URL this task originated from
		 */
		if (!(ORIGINATOR.equals("internal"))) {
			CRAWLER_POOL.sendComplete(ORIGINATOR);
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
				if (aTag != null){
					String pageLink = aTag.getAttributeValue("href").toString();
					if (pageLink.contains(ROOT_URL) || pageLink.charAt(0) == '/' || pageLink.charAt(0) == '.' || pageLink.charAt(0) == '#') {
						CrawlerTask task = new CrawlerTask(depth, pageLink, pageUrl, ROOT_URL, CRAWLER_POOL, "internal");
						CRAWLER_POOL.submit(task);
					} else {
						// Need to forward it on...
						CRAWLER_POOL.forwardTask(pageLink);
					}
				}				
			}
			
		} catch (IOException e) {} // in case of malformed url
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
	 * Returns an absolute URL based on root and relative URL passed
	 * @param String root
	 * @param String relative
	 * @return String absolute
	 */
	private String relativeToAbs(String parent, String relative){
		String absolute = null;
		try {
			if(!new URI(relative).isAbsolute()){
				URI resolvedUrl = new URI(parent).resolve(relative);
				absolute = resolvedUrl.toString();
			} else {
				absolute = relative;
			}
		} catch (URISyntaxException e1) {}
		return absolute;
	}

	/**
	 * @return the CRAWL_URL
	 */
	public String getCrawlUrl() {
		return new String(CRAWL_URL);
	}

	/**
	 * @return the ROOT_URL
	 */
	public String getRootUrl() {
		return new String(ROOT_URL);
	}

	/**
	 * @return the PARENT_URL
	 */
	public String getParentUrl() {
		return new String(PARENT_URL);
	}
	
	/**
	 * @return the ORIGINATOR
	 */
	public String getOriginator() {
		return new String(ORIGINATOR);
	}

	@Override
	public String toString() {
		return "CrawlerTask [RECURSION_DEPTH=" + RECURSION_DEPTH + ", "
				+ (CRAWL_URL != null ? "CRAWL_URL=" + CRAWL_URL + ", " : "")
				+ (PARENT_URL != null ? "PARENT_URL=" + PARENT_URL + ", " : "")
				+ (ROOT_URL != null ? "ROOT_URL=" + ROOT_URL : "") + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((CRAWL_URL == null) ? 0 : CRAWL_URL.hashCode());
		result = prime * result + ((ROOT_URL == null) ? 0 : ROOT_URL.hashCode());
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
		if (CRAWL_URL == null) {
			if (other.CRAWL_URL != null) {
				return false;
			}
		} else if (!CRAWL_URL.equals(other.CRAWL_URL)) {
			return false;
		}
		if (ROOT_URL == null) {
			if (other.ROOT_URL != null) {
				return false;
			}
		} else if (!ROOT_URL.equals(other.ROOT_URL)) {
			return false;
		}
		return true;
	}

}//************** END CrawlTask **************
