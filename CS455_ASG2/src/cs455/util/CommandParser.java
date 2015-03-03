package cs455.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import cs455.harvester.Crawler;

public class CommandParser extends Thread {

	private final Crawler crawler;

	public CommandParser(Crawler crawler){
		this.crawler = crawler;
	}

	public void run(){
		try
		{
			BufferedReader fromConsole = new BufferedReader(new InputStreamReader(System.in));
			String message;

			while (true) 
			{
				//System.out.print("Enter a command: ");
				message = fromConsole.readLine();
				handleMessage(message);
			}

		} 
		catch (Exception ex) 
		{
			System.out.println("Unexpected error while reading from console!");
			ex.printStackTrace();
		}
	}

	public void handleMessage(String message){

		if(message.startsWith("status"))
			crawler.printCompletionReport();
		else
			System.out.println("Command not recognized");

	}

}
