package cs455.harvester.transport;

import java.io.IOException;
import java.net.Socket;

import cs455.harvester.Node;

public class TCPConnection{

	private TCPSender sender;
	private TCPReceiverThread receiver;
	private Socket socket;
	private int connectionId;
	
	/**
	 * TCPConncetion constructor
	 * Node is the node that created the receiver
	 * id is the id assigned to this connection
	 * @param id
	 * @param socket
	 * @param node
	 */
	public TCPConnection(int id, Socket socket, Node node){
		
		this.connectionId = id;
		this.socket = socket;
		
		try {
			
			/*
			 * Passing the connection id onto the receiver
			 * Whenever we receive data from this connection, we
			 * can determine where it came from
			 */
			this.sender = new TCPSender(socket);
			this.receiver = new TCPReceiverThread(id, socket, node);
			Thread receive = new Thread(receiver);
			receive.start();
			
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	/**
	 * Return the socket associated with this TCPConnection
	 * @return
	 */
	public Socket getSocket(){
		return socket;
	}
	
	/**
	 * Send data to this connections node
	 * @param data
	 * @throws IOException
	 */
	public void sendData(byte[] data) throws IOException{
		sender.sendData(data);
	}
	
	/**
	 * Get the id assigned to this connection
	 * @return int
	 */
	public int getConnectionId(){
		return connectionId;
	}

}
