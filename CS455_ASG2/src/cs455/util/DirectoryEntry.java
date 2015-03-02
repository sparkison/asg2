package cs455.util;

public class DirectoryEntry {

	private final String directory;
	private final String link;
	private final String linkType;

	public DirectoryEntry(String directory, String link, String linkType){
		this.directory = directory;
		this.link = link;
		this.linkType = linkType;
	}

	public String getDirectory(){
		return new String(directory);
	}

	public String getLink(){
		return new String(link);
	}
	
	public String getLinkType(){
		return new String(linkType);
	}

	@Override
	public String toString() {
		return "DirectoryEntry ["
				+ (directory != null ? "directory=" + directory + ", " : "")
				+ (link != null ? "link=" + link + ", " : "")
				+ (linkType != null ? "linkType=" + linkType : "") + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((directory == null) ? 0 : directory.hashCode());
		result = prime * result + ((link == null) ? 0 : link.hashCode());
		result = prime * result
				+ ((linkType == null) ? 0 : linkType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof DirectoryEntry)) {
			return false;
		}
		DirectoryEntry other = (DirectoryEntry) obj;
		if (directory == null) {
			if (other.directory != null) {
				return false;
			}
		} else if (!directory.equals(other.directory)) {
			return false;
		}
		if (link == null) {
			if (other.link != null) {
				return false;
			}
		} else if (!link.equals(other.link)) {
			return false;
		}
		if (linkType == null) {
			if (other.linkType != null) {
				return false;
			}
		} else if (!linkType.equals(other.linkType)) {
			return false;
		}
		return true;
	}

}
