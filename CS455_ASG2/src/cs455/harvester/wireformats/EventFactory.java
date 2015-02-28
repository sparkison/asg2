package cs455.harvester.wireformats;

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
