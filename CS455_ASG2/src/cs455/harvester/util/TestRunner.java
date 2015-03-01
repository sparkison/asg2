/**
 * @author Shaun Parkison (shaunpa)
 * Colorado State University
 * CS455 - Dist. Systems
 */

package cs455.harvester.util;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

import cs455.harvester.Crawler;

public class TestRunner {

	public static void main(String args[]){

		TestRunner runner = new TestRunner();
		int poolSize = 10;

		Path path = Paths.get(args[0]);

//		try {
//			String [] command = {
//					System.getProperty("java.home") + 
//					System.getProperty("file.separator") + "bin" + 
//		            System.getProperty("file.separator") + "java", "-cp", 
//		            System.getProperty("java.class.path"), "cs455.harvester.Crawler"};
//			ProcessBuilder p = new ProcessBuilder(command);
//			p.redirectErrorStream(false);
//		    Process process = p.start();
//		    System.out.println("Fin");
//		} catch (IOException e1) {
//			e1.printStackTrace();
//		}
		
		try (Scanner scanner =  new Scanner(path)){
			while (scanner.hasNextLine()){
				String[] temp = runner.delimitConfig(scanner.nextLine());
				runner.executeCrawler(poolSize, temp, args[0]);
			}      
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public String[] delimitConfig(String config){
		return config.split(",");		
	}

	public void executeCrawler(int poolSize, String[] config, String configPath){
		int port = Integer.parseInt(config[0].split(":")[1]);
		String rootUrl = config[1];
		//System.out.println("Starting Crawler with: portnum: ["+ port +"], poolsize: [" + poolSize + "], rooturl: [" + rootUrl + "], config: [" + configPath + "]");
		try {
			new Crawler(port, poolSize, rootUrl, configPath);
		} catch (IOException e) {
			e.printStackTrace();
		}			
	}

	
}//END TestRunner