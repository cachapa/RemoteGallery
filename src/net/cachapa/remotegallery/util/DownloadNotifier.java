package net.cachapa.remotegallery.util;

public class DownloadNotifier {
	private static DownloadNotifier instance;
	private onDownloadListener listener;
	
	protected DownloadNotifier() {
	}
	
	public static DownloadNotifier getInstance() {
		if (instance == null) {
			instance = new DownloadNotifier();
		}
		return instance;
	}
	
	public void setOnDownloadListener(onDownloadListener listener) {
		this.listener = listener;
	}
	
	public void notifyDownloadComplete() {
		if (listener != null) {
			listener.onDownloadComplete();
		}
	}
	
	
	public interface onDownloadListener {
		public void onDownloadComplete();
	}
}
