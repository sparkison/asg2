/**
 * @author Shaun Parkison (shaunpa)
 * Colorado State University
 * CS455 - Dist. Systems
 */

package cs455.harvester;

import cs455.harvester.wireformats.Event;

public class Crawler implements Node{

	// Instance variables **************
	private int port;
	private int poolSize;
	private String rootUrl;
	private String configPath;
	
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
		this.port = port;
		this.poolSize = poolSize;
		this.rootUrl = rootUrl;
		this.configPath = configPath;
		// Do some stuff!!
	}

	@Override
	public void onEvent(Event e) {
		// TODO Auto-generated method stub
	}


}//************** END Crawler **************
