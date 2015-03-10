package cs455.wireformats;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class EventFactory {
	// Singleton instance
	private static EventFactory instance = null;

	// Exists only to defeat instantiation
	protected EventFactory() {
	}

	// Get instance of TaskFactory
	public static EventFactory getInstance() {
		if (instance == null) {
			instance = new EventFactory();
		}
		return instance;
	}

	public Event buildEvent(int type, String message) {
		// Get the message components
		String[] eventMessage = getMessage(message);

		switch (type) {

		case Protocol.CRAWLER_SENDS_TASK:
			return new CrawlerSendsTask(type, eventMessage[0], eventMessage[1]);

		case Protocol.CRAWLER_SENDS_TASK_COMPLETE:
			return new CrawlerSendsTaskComplete(type, message);

		case Protocol.CRAWLER_SENDS_FINISHED:
			return new CrawlerSendsFinished(type, message);
			
		case Protocol.CRAWLER_SENDS_INCOMPLETE:
			return new CrawlerSendsIncomplete(type, message);
			
		default:
			System.out.println("Event type not recognized");
			
		}

		return null;
	}

	public Event getEvent(byte[] data) {
		int type = getType(data);

		switch (type) {

		case Protocol.CRAWLER_SENDS_TASK:
			try {
				return new CrawlerSendsTask(data);
			} catch (IOException e) {
				System.out.println("Error creating CrawlerSendsTask event: ");
				e.printStackTrace();
			}

		case Protocol.CRAWLER_SENDS_TASK_COMPLETE:
			try {
				return new CrawlerSendsTaskComplete(data);
			} catch (IOException e) {
				System.out.println("Error creating CrawlerSendsTaskComplete event: ");
				e.printStackTrace();
			}

		case Protocol.CRAWLER_SENDS_FINISHED:
			try {
				return new CrawlerSendsFinished(data);
			} catch (IOException e) {
				System.out.println("Error creating CrawlerSendsFinished event: ");
				e.printStackTrace();
			}
			
		case Protocol.CRAWLER_SENDS_INCOMPLETE:
			try {
				return new CrawlerSendsIncomplete(data);
			} catch (IOException e) {
				System.out.println("Error creating CrawlerSendsIncomplete event: ");
				e.printStackTrace();
			}
			
		default:
			System.out.println("Event type not recognized");

		}

		return null;
	}

	/********************************************
	 ****************** HELPERS *****************
	 ********************************************/

	/**
	 * Get the type based on byte[] passed
	 * @param data
	 * @return int
	 */
	private int getType(byte[] data){
		ByteArrayInputStream baInputStream = new ByteArrayInputStream(data);
		DataInputStream din = new DataInputStream(baInputStream);
		int type = -1;
		try {
			type = din.readInt();
		} catch (IOException e) {
			System.out.println("EventFactory - error getting data type: ");
			e.printStackTrace();
		}
		try {
			baInputStream.close();
			din.close();
		} catch (IOException e) {
			System.out.println("EventFactory - error closing streams: ");
			e.printStackTrace();
		}
		return type;
	}// END getType

	/**
	 * Gets the string from the getEvent method, parameters delimited by ';'
	 * @return String
	 */
	private String[] getMessage(String message) {
		String delimit = ";";
		String[] temp = message.split(delimit);
		return temp;
	}// END getMessage	

}//************** END EventFactory **************
