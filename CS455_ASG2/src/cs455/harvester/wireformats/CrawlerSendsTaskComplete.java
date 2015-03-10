package cs455.harvester.wireformats;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class CrawlerSendsTaskComplete implements Event {

	private int type;
	private String originatingUrl;

	public CrawlerSendsTaskComplete(int type, String originatingUrl){
		this.type = type;
		this.originatingUrl = originatingUrl;
	}

	// Marshalling (packing the bytes)
	@Override
	public byte[] getBytes() {
		byte[] marshalledBytes = null;
		ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
		DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(
				baOutputStream));

		try {
			dout.writeInt(type);

			byte[] originatingUrlBytes = originatingUrl.getBytes();
			int elementLength = originatingUrlBytes.length;
			dout.writeInt(elementLength);
			dout.write(originatingUrlBytes);

			dout.flush();
			marshalledBytes = baOutputStream.toByteArray();
			baOutputStream.close();
			dout.close();

		} catch (IOException e) {
			System.out.println("Error marshalling the bytes for OverlayNodeReportsTaskFinished.");
			e.printStackTrace();
		}

		return marshalledBytes;
	}

	// Unmarshalling (unpack the bytes)
	public CrawlerSendsTaskComplete(byte[] marshalledBytes) throws IOException {
		ByteArrayInputStream baInputStream = new ByteArrayInputStream(
				marshalledBytes);
		DataInputStream din = new DataInputStream(new BufferedInputStream(
				baInputStream));

		type = din.readInt();

		int identifierLength = din.readInt();
		byte[] identifierBytes = new byte[identifierLength];
		din.readFully(identifierBytes);
		originatingUrl = new String(identifierBytes);

		baInputStream.close();
		din.close();
	}

	@Override
	public int getType() {
		return type;
	}

	public String getOriginatingUrl() {
		return originatingUrl;
	}
	
	@Override
	public String toString() {
		return "CrawlerSendsTaskComplete [type=" + type + ", originatingUrl="
				+ originatingUrl + "]";
	}

}
