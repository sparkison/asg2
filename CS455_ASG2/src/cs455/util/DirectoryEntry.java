package cs455.util;

public class DirectoryEntry {

	private final String directory;
	private final String link;

	public DirectoryEntry(String directory, String link){
		this.directory = directory;
		this.link = link;
	}

	public String getDirectory(){
		return new String(directory);
	}

	public String getLink(){
		return new String(link);
	}

}
