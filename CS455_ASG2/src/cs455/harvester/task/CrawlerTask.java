/**
 * @author Shaun Parkison (shaunpa)
 * Colorado State University
 * CS455 - Dist. Systems
 */

package cs455.harvester.task;

import java.io.IOException;
import java.io.InputStream;
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
import cs455.harvester.thread.CrawlerThreadPool;

public class CrawlerTask implements Task {

	// Instance variables **************
	private final int RECURSION_DEPTH;
	private final String PARENT_URL;
	private final String CRAWL_URL;
	private final String ROOT_URL;
	private final String ORIGINATOR;
	private final CrawlerThreadPool CRAWLER_POOL;

	public CrawlerTask(int recursionDepth, String crawlUrl, String parentUrl, String rootUrl, CrawlerThreadPool crawlerPool, String originator){
		RECURSION_DEPTH = recursionDepth;
		CRAWL_URL = relativeToAbs(parentUrl, crawlUrl);
		PARENT_URL = normalize(parentUrl);
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
	}

	public void URLExtractor(String url, int depth){
		Config.LoggerProvider = LoggerProvider.DISABLED;
		try {
			// Web page that needs to be parsed,
			final String pageUrl = url;
			// Check for redirect before processing
			HttpURLConnection con = (HttpURLConnection)(new URL(pageUrl).openConnection());
			con.connect();
			InputStream istream = con.getInputStream();
			// this is the actual URL, the page is redirected to (if there is a redirect).
			con.getURL();
			// instead of passing the URL, pass the input stream.
			Source source = new Source(istream);

			// Don't parse if document, only if page			
			if(!(pageUrl.endsWith(".pdf") || pageUrl.endsWith(".doc"))){
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
							CRAWLER_POOL.forwardTask(pageLink, this);
						}
					}				
				}
			}

		} catch (IOException e) {
			/*
			 * If here, could be malformed URL, or 404 or 500 error.
			 * Need to check, and if 403, 404 or 500 add to broken-links
			 */
			try {
				if(checkDeadLink(url)){
					CRAWLER_POOL.reportBrokenLink(url);
				}
			} catch (IOException e1) {}
		}
	}

	/**
	 * Determines if link is dead or not.
	 * @param url
	 * @return
	 * @throws IOException
	 */
	private boolean checkDeadLink(String url) throws IOException {
		HttpURLConnection con = (HttpURLConnection)(new URL(url).openConnection());
		con.setInstanceFollowRedirects(false);
		con.connect();
		int responseCode = con.getResponseCode();
		if(responseCode == 403)
			return true;
		if(responseCode == 404)
			return true;
		if(responseCode == 500)
			return true;
		return false;
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
		
		return normalize(absolute);
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

	/**
	 * Normalize URL
	 * Licensed under http://www.apache.org/licenses/LICENSE-2.0
	 */
	public static String normalize(String normalized) {

		if (normalized == null) {
			return null;
		}

		// If the buffer begins with "./" or "../", the "." or ".." is removed.
		if (normalized.startsWith("./")) {
			normalized = normalized.substring(1);
		} else if (normalized.startsWith("../")) {
			normalized = normalized.substring(2);
		} else if (normalized.startsWith("..")) {
			normalized = normalized.substring(2);
		}

		// All occurrences of "/./" in the buffer are replaced with "/"
		int index = -1;
		while ((index = normalized.indexOf("/./")) != -1) {
			normalized = normalized.substring(0, index) + normalized.substring(index + 2);
		}

		// If the buffer ends with "/.", the "." is removed.
		if (normalized.endsWith("/.")) {
			normalized = normalized.substring(0, normalized.length() - 1);
		}

		int startIndex = 0;

		// All occurrences of "/<segment>/../" in the buffer, where ".."
		// and <segment> are complete path segments, are iteratively replaced
		// with "/" in order from left to right until no matching pattern remains.
		// If the buffer ends with "/<segment>/..", that is also replaced
		// with "/".  Note that <segment> may be empty.
		while ((index = normalized.indexOf("/../", startIndex)) != -1) {
			int slashIndex = normalized.lastIndexOf('/', index - 1);
			if (slashIndex >= 0) {
				normalized = normalized.substring(0, slashIndex) + normalized.substring(index + 3);
			} else {
				startIndex = index + 3;
			}
		}
		if (normalized.endsWith("/..")) {
			int slashIndex = normalized.lastIndexOf('/', normalized.length() - 4);
			if (slashIndex >= 0) {
				normalized = normalized.substring(0, slashIndex + 1);
			}
		}

		// All prefixes of "<segment>/../" in the buffer, where ".."
		// and <segment> are complete path segments, are iteratively replaced
		// with "/" in order from left to right until no matching pattern remains.
		// If the buffer ends with "<segment>/..", that is also replaced
		// with "/".  Note that <segment> may be empty.
		while ((index = normalized.indexOf("/../")) != -1) {
			int slashIndex = normalized.lastIndexOf('/', index - 1);
			if (slashIndex >= 0) {
				break;
			} else {
				normalized = normalized.substring(index + 3);
			}
		}
		if (normalized.endsWith("/..")) {
			int slashIndex = normalized.lastIndexOf('/', normalized.length() - 4);
			if (slashIndex < 0) {
				normalized = "/";
			}
		}

		return normalized;
	}

	@Override
	public String toString() {
		return "CrawlerTask [RECURSION_DEPTH=" + RECURSION_DEPTH + ", "
				+ (CRAWL_URL != null ? "CRAWL_URL=" + CRAWL_URL + ", " : "")
				+ (PARENT_URL != null ? "PARENT_URL=" + PARENT_URL + ", " : "")
				+ (ROOT_URL != null ? "ROOT_URL=" + ROOT_URL : "") + "]";
	}

}//************** END CrawlTask **************
