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

		System.out.println("Starting Crawler with: portnum: ["+ port +"], poolsize: [" + poolSize + "], rooturl: [" + rootUrl + "], config: [" + configPath + "]");

		new Crawler(port, poolSize, rootUrl, configPath);			
	}

	
}//END TestRunner