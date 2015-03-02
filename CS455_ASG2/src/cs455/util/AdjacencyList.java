package cs455.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class AdjacencyList {
	
	private final Map<String, List<String>> adjacency;

	public AdjacencyList(){
		adjacency = new TreeMap<String, List<String>>();
	}
	
	/**
	 * Adds an edge to the vertex. If vertex not present, it will be added.
	 * Vertex would be the parent URL, edge would be the crawl URL (which is the outgoing link)
	 * @param String vertex
	 * @param String edge
	 */
	public void addEdge(String vertex, String edge){
		if(!(adjacency.containsKey(vertex))){
			List<String> edgeList = new ArrayList<String>();
			edgeList.add(edge);
			adjacency.put(vertex, edgeList);
		}else{
			adjacency.get(vertex).add(edge);
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
		if(adjacency.containsKey(vertex)){
			edgeList = new ArrayList<String>(adjacency.get(vertex));
		}
		return edgeList;
	}
	
	/**
	 * Returns list of incoming links to given vertex,
	 * returns null if no incoming edges found
	 * @param String vertex
	 * @return List<String> inEdges
	 */
	public List<String> inEdges(String vertex){
		List<String> edgeList = null;
		for (String key : adjacency.keySet()) {
		    int index = adjacency.get(key).indexOf(vertex);
		    if(index != -1)
		    	edgeList.add(adjacency.get(key).get(index));
		}
		return edgeList;
	}
	
}
