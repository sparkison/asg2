/**
 * @author Shaun Parkison (shaunpa)
 * Colorado State University
 * CS455 - Dist. Systems
 */

package cs455.harvester.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cs455.harvester.Crawler;

public class AdjacencyList {

	private final String DIRECTORY_ROOT = "/tmp/cs455-shaunpa/";
	private final String NODE_ROOT;
	private final Map<String, Set<String>> ADJACENCY;
	private final Set<String> BROKEN_LINKS;
	private final String ROOT_URL;
	private final Crawler CRAWLER;
	private final File FILE;
	private final String PATTERN;
	private final boolean debug = true;

	public AdjacencyList(String rootUrl, Crawler crawler){
		ADJACENCY = new HashMap<String, Set<String>>();
		BROKEN_LINKS = new HashSet<String>();
		ROOT_URL = rootUrl;
		CRAWLER = crawler;
		NODE_ROOT = DIRECTORY_ROOT + rootUrl.replaceAll("[^a-zA-Z0-9._-]", "-");
		PATTERN = "(?i).*"+ rootUrl.trim();
		FILE = new File(NODE_ROOT + "/nodes");
	}

	/**
	 * Initiates the creation of the directory structure for this Crawler
	 */
	public void startDirectoryCreation(){
		if(debug)
			System.out.println("Creating directory for Crawler: " + ROOT_URL + "...");

		// Create the initial directory for the Crawler associated with this list
		if (!FILE.exists()) {
			if (FILE.mkdirs()) {
				//System.out.println("Directory is created!");
			} else {
				//System.out.println("Failed to create directory!");
			}
		}

		String nodePath = FILE.getAbsolutePath();
		for(String vertex : ADJACENCY.keySet()){
			//if(vertex.matches(PATTERN)){
				try {

					/*
					 * Build upper level folder for the vertex
					 */
					String nodeFolder = vertex.replaceAll(PATTERN, "");
					if(nodeFolder.equals(""))
						nodeFolder = ROOT_URL.replaceAll("[^a-zA-Z0-9._-]", "-");
					else
						nodeFolder = getDirectoryName(nodeFolder);

					File file = new File(nodePath + "/" + nodeFolder);
					if (!file.exists()) {
						file.mkdirs();
					}

					/*
					 * Build the out file
					 */
					File outFile = new File(file.getAbsolutePath() + "/out");
					List<String> out = outEdges(vertex);
					try {
						PrintWriter outWriter = new PrintWriter(outFile);
						for(String edge : out){
							outWriter.println(edge);
						}
						outWriter.flush();
						outWriter.close();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}

					/*
					 * Build the in file
					 */
					File inFile = new File(file.getAbsolutePath() + "/in");
					List<String> in = inEdges(vertex);
					try {
						PrintWriter inWriter = new PrintWriter(inFile);
						for(String edge : in){
							inWriter.println(edge);
						}
						inWriter.flush();
						inWriter.close();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}

				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			//}

			/*
			 * Write the broken links file
			 */
			File brokenLinks = new File(NODE_ROOT + "/broken-links");
			try {
				PrintWriter brokenLink = new PrintWriter(brokenLinks);
				for(String broken : BROKEN_LINKS){
					brokenLink.println(broken);
				}
				brokenLink.flush();
				brokenLink.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

			/*
			 * Done with main file creation, now we need to build
			 * our disjoint graph files
			 */
			getConnectedComponents();

		}

		if(debug)
			System.out.println("Finished creating directories.");

	}

	/**
	 * Determine the Connected components of the graph, using Dijkstra's BFS
	 * of the adjacency list
	 */
	private void getConnectedComponents(){

		File disjointSubGraphs = new File(NODE_ROOT + "/disjoint-subgraphs");
		if (!disjointSubGraphs.exists()) {
			disjointSubGraphs.mkdirs();
		}

		// Use a map to hold our disjoint graphs
		Map<Integer, LinkedList<String>> disjointGraphs = new HashMap<Integer, LinkedList<String>>();		
		// Get the list of vertices
		LinkedList<String> vertexList = new LinkedList<String>(ADJACENCY.keySet());
		// Build a working queue
		LinkedList<String> bfsQueue = new LinkedList<String>();
		// List to keep track of visited vertices
		List<String> visited = new ArrayList<String>();

		int index = 0;
		while(!(vertexList.isEmpty())){

			// Counter to keep track of each disjoint graph
			index++;
			disjointGraphs.put(index, new LinkedList<String>());
			// Add element to list to start
			bfsQueue.add(vertexList.pop());

			while(!(bfsQueue.isEmpty())){

				String vertex = bfsQueue.pop();

				List<String> neighbors = null;
				try{
					neighbors = new ArrayList<String>(ADJACENCY.get(vertex));
				}catch(NullPointerException e){}

				if(neighbors != null){
					for(String neighbor : neighbors){
						if(neighbor != null){
							// If neighbor not visited...
							if(!(visited.contains(neighbor))){
								// Mark it as visited
								visited.add(neighbor);
								// Add it to our disjoint graphs container
								disjointGraphs.get(index).add(neighbor);
								// Add it to our queue to be visited
								bfsQueue.add(neighbor);
								// Remove it from our vertex list (if present)
								vertexList.remove(neighbor);						
							}
						}
					}
				}

			}//END while

			/*
			 * Found first disjoint graph, if vertexList is not empty
			 * at this point, then still have more disjoint graphs, do it again...
			 */

		}//END while

		/*
		 * Loop through our disjoint graph Map container and add graph file, with links, as needed...
		 */
		for(Integer graphIndex : disjointGraphs.keySet()){

			int graphFileCount = 1;
			if(!(disjointGraphs.get(graphIndex).isEmpty())){
				File file = new File(disjointSubGraphs.getAbsolutePath() + "/graph" + graphFileCount);
				graphFileCount++;
				try {
					PrintWriter disjointGraph = new PrintWriter(file);
					for(String graphEdge : disjointGraphs.get(graphIndex)){
						disjointGraph.println(graphEdge);
					}
					disjointGraph.flush();
					disjointGraph.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
			
		}

	}

	/**
	 * Adds an edge to the vertex. If vertex not present, it will be added.
	 * Vertex would be the parent URL, edge would be the crawl URL (which is the outgoing link)
	 * @param String vertex
	 * @param String edge
	 */
	public void addEdge(String vertex, String edge){
		synchronized(ADJACENCY){
			if(!(ADJACENCY.containsKey(vertex))){
				Set<String> edgeList = new HashSet<String>();
				edgeList.add(edge);
				ADJACENCY.put(vertex, edgeList);
			}else{
				ADJACENCY.get(vertex).add(edge);
			}
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
			if(ADJACENCY.get(key).contains(vertex))
				edgeList.add(key);
		}
		return edgeList;
	}

	/**
	 * A list of broken links.
	 * Does not allow duplicates.
	 * @param url
	 */
	public void addBrokenLink(String url){
		synchronized(BROKEN_LINKS){
			BROKEN_LINKS.add(url);
		}
	}

	/**
	 * Returns a formatted string for creating directory
	 * @param String directoryUrl
	 * @return String directoryName
	 * @throws MalformedURLException
	 */
	public String getDirectoryName(String directoryUrl) throws MalformedURLException{
		// URL url = new URL(directoryUrl);
		// String directoryName = url.getPath();
		
		String[] temp = directoryUrl.split("/");
		String directoryName = "";

		for(int i = 0; i<temp.length; i++){
			directoryName += temp[i].replaceAll("[^a-zA-Z0-9._-]", "-");
			if(i != temp.length-1)
				directoryName += "-";
		}
		
		if(directoryName.charAt(0) == '-')
			directoryName = directoryName.substring(1);
		
		return directoryName.trim();
	}

}//END AdjacencyList
