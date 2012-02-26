package net.cachapa.remotegallery.util;

import java.util.ArrayList;
import java.util.List;

public class DownloadNotifier {
	private static DownloadNotifier instance;
	private List<onDownloadListener> listeners = new ArrayList<onDownloadListener>();

	protected DownloadNotifier() {
	}
	
	public static DownloadNotifier getInstance() {
		if (instance == null) {
			instance = new DownloadNotifier();
		}
		return instance;
	}

	public void notifyDownloadComplete() {
		for (onDownloadListener listener : listeners) {
			listener.onDownloadComplete();
		}
	}
	
	public interface onDownloadListener {
		public void onDownloadComplete();
	}

	public void addDownloadListener(onDownloadListener listener) {
		if (!listeners.contains(listener))
			listeners.add(listener);
	}

	public void removeDownloadListener(onDownloadListener listener) {
		listeners.remove(listener);
	}
}
