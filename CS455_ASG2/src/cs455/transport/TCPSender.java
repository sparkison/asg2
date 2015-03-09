package cs455.transport;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;

public class TCPSender extends Thread {
	@SuppressWarnings("unused")
	private Socket socket;
	private DataOutputStream dout;
	private LinkedList<byte[]> queue;
	private final Object WAIT_LOCK = new Object();

	public TCPSender(Socket socket) throws IOException {
		this.socket = socket;
		queue = new LinkedList<byte[]>();
		dout = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
	}

	public void sendData(byte[] dataToSend) throws IOException {
		int dataLength = dataToSend.length;
		dout.writeInt(dataLength);
		dout.write(dataToSend, 0, dataLength);
		dout.flush();
//		queue.add(dataToSend);
//		synchronized(WAIT_LOCK){
//			WAIT_LOCK.notify();
//		}
	}

	public void run() {

		byte[] dataToSend;

		while(true) {
			dataToSend = queue.poll();
			if(dataToSend != null) {
				try {
					int dataLength = dataToSend.length;
					dout.writeInt(dataLength);
					dout.write(dataToSend, 0, dataLength);
					dout.flush();
				} catch (Exception e) {}
			} else {
				synchronized(WAIT_LOCK) {
					try {
						WAIT_LOCK.wait();
					} catch (InterruptedException e) {}
				}
			}
		}
	}
}