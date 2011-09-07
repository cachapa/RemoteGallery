package net.cachapa.remotegallery.util;

public class ServerConf {
	public long id;
	public String name;
	public String address;
	public int port;
	public String username;
	public String keyPath;
	public String remotePath;
	
	public ServerConf(long id, String name, String address, int port, String username, String keyPath, String remotePath) {
		this.id = id;
		this.name = name.length() > 0 ? name : address;
		this.address = address;
		this.port = port;
		this.username = username;
		this.keyPath = keyPath;
		this.remotePath = remotePath;
	}
	
	public ServerConf(String name, String address, int port, String username, String keyPath, String remotePath) {
		this(-1, name, address, port, username, keyPath, remotePath);
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	public String fullAddress() {
		return username + "@" + address + ":" + port;
	}
}
