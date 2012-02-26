package net.cachapa.remotegallery.util;

public class DirEntry {
	public String name;
	public boolean isDirectory;
	public boolean isDownloading = false;
	public boolean isThumbDownloading = false;
	public boolean alreadyDownloadedOnce = false;
	public boolean alreadyDownloadedThumbOnce = false;
	
	public DirEntry(String name, boolean isDirectory) {
		this.name = name;
		this.isDirectory = isDirectory;
	}
	
	@Override
	public String toString() {
		return name;
	}
}