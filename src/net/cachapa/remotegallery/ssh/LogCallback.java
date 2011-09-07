/**
 * 
 */
package net.cachapa.remotegallery.ssh;

public interface LogCallback {
	public void log(Ssh ssh, String log);
	public void logError(Ssh ssh, String log);
}