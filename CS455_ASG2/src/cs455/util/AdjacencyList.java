/**
 * @author Shaun Parkison (shaunpa)
 * Colorado State University
 * CS455 - Dist. Systems
 */

package cs455.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdjacencyList {
	
	private final Map<String, List<String>> ADJACENCY;
	private final String ROOT_URL;
	
	public AdjacencyList(String rootUrl){
		ADJACENCY = new HashMap<String, List<String>>();
		ROOT_URL = rootUrl;
	}
	
	/**
	 * Adds an edge to the vertex. If vertex not present, it will be added.
	 * Vertex would be the parent URL, edge would be the crawl URL (which is the outgoing link)
	 * @param String vertex
	 * @param String edge
	 */
	public void addEdge(String vertex, String edge){
		if(!(ADJACENCY.containsKey(vertex))){
			List<String> edgeList = new ArrayList<String>();
			edgeList.add(edge);
			ADJACENCY.put(vertex, edgeList);
		}else{
			ADJACENCY.get(vertex).add(edge);
		}
	}
	
	/**
	 * If valid vertex, returns List<String> of edges
	 * else, returns null if vertex not found
	 * @param String vertex
	 * @return List<String> outEdges
	 */
	public List<String> outEdges(String vertex){
		List<String> edgeList = null;
		if(ADJACENCY.containsKey(vertex)){
			edgeList = new ArrayList<String>(ADJACENCY.get(vertex));
		}
		return edgeList;
	}
	
	/**
	 * Returns list of incoming links to given vertex,
	 * returns empty List if no incoming edges found
	 * @param String vertex
	 * @return List<String> inEdges
	 */
	public List<String> inEdges(String vertex){
		List<String> edgeList = new ArrayList<String>();
		for (String key : ADJACENCY.keySet()) {
		    int index = ADJACENCY.get(key).indexOf(vertex);
		    if(index != -1)
		    	edgeList.add(ADJACENCY.get(key).get(index));
		}
		return edgeList;
	}
	
	/**
	 * Returns a formatted string for creating directory
	 * @param String directoryUrl
	 * @return String directoryName
	 * @throws MalformedURLException
	 */
	public String getDirectoryName(String directoryUrl) throws MalformedURLException{
		URL url = new URL(directoryUrl);
		String directoryName = url.getPath();
		String[] temp = directoryName.split("/");
		directoryName = "";
		for(int i = 1; i<temp.length; i++){
			directoryName += temp[i].replaceAll("[^a-zA-Z0-9._-]", "-");
			if(i != temp.length-1)
				directoryName += "-";
		}
		return directoryName;
	}
	
}//END AdjacencyList
