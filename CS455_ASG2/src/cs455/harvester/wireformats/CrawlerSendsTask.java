package cs455.harvester.wireformats;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class CrawlerSendsTask implements Event{

	private int type;
	private String urlToCrawl;
	private String originatingCrawlerUrl;

	public CrawlerSendsTask(int type, String urlToCrawl, String originatingCrawlerUrl){
		this.type = type;
		this.urlToCrawl = urlToCrawl;
		this.originatingCrawlerUrl = originatingCrawlerUrl;
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
			
			byte[] urlToCrawlBytes = urlToCrawl.getBytes();
			int elementLength = urlToCrawlBytes.length;
			dout.writeInt(elementLength);
			dout.write(urlToCrawlBytes);
			
			byte[] originatingCrawlerUrlBytes = originatingCrawlerUrl.getBytes();
			int elementLength2 = originatingCrawlerUrlBytes.length;
			dout.writeInt(elementLength2);
			dout.write(originatingCrawlerUrlBytes);

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
	public CrawlerSendsTask(byte[] marshalledBytes) throws IOException {
		ByteArrayInputStream baInputStream = new ByteArrayInputStream(
				marshalledBytes);
		DataInputStream din = new DataInputStream(new BufferedInputStream(
				baInputStream));

		type = din.readInt();
		
		int identifierLength = din.readInt();
		byte[] identifierBytes = new byte[identifierLength];
		din.readFully(identifierBytes);
		urlToCrawl = new String(identifierBytes);

		int identifierLength2 = din.readInt();
		byte[] identifierBytes2 = new byte[identifierLength2];
		din.readFully(identifierBytes2);
		originatingCrawlerUrl = new String(identifierBytes2);
		
		baInputStream.close();
		din.close();
	}

	@Override
	public int getType() {
		return type;
	}

	/**
	 * @return the urlToCrawl
	 */
	public String getUrlToCrawl() {
		return urlToCrawl;
	}

	/**
	 * @return the originatingCrawlerUrl
	 */
	public String getOriginatingCrawlerUrl() {
		return originatingCrawlerUrl;
	}

}
