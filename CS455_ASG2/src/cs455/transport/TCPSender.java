package cs455.harvester.transport;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class TCPSender {
	@SuppressWarnings("unused")
	private Socket socket;
	private DataOutputStream dout;

	public TCPSender(Socket socket) throws IOException {
		this.socket = socket;
		dout = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
	}

	public void sendData(byte[] dataToSend) throws IOException {

		int dataLength = dataToSend.length;
		dout.writeInt(dataLength);
		dout.write(dataToSend, 0, dataLength);
		dout.flush();

	}
}