package cs455.harvester.transport;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

import cs455.harvester.Node;
import cs455.harvester.wireformats.Event;
import cs455.harvester.wireformats.EventFactory;


public class TCPReceiverThread extends Thread{

	// Instance variables **************
	private Socket socket;
	private DataInputStream din;
	private Node node;
	private EventFactory ef = EventFactory.getInstance();

	/**
	 * Main constructor
	 * @param id
	 * @param socket
	 * @param node
	 * @throws IOException
	 */
	public TCPReceiverThread(Socket socket, Node node) throws IOException {
		this.socket = socket;
		this.node = node;
		din = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
	}

	/**
	 * Runs when thread is initialized (.start() is called on it).
	 */
	public void run() {

		while (socket != null) {
			try {

				// Get data, and send to node for processing
				int dataLength = din.readInt();
				byte[] data = new byte[dataLength];
				din.readFully(data, 0, dataLength);
				/*
				 * Build Event to send to receiver
				 * this will be destined for the end
				 * that initiated the ServerSocket.
				 * Passing id with message so we know
				 * where the message originated from
				 */

				Event e = ef.getEvent(data);
				node.onEvent(e);

			} catch (SocketException se) {
				System.out.println(se.getMessage());
				break;
			} catch (IOException ioe) {
				System.out.println(ioe.getMessage()) ;
				break;
			}

		}

	}

}