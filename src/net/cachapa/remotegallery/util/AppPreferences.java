package net.cachapa.remotegallery.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Environment;
import android.preference.PreferenceManager;

public class AppPreferences {
	public static final String REVERSE_DIR_ORDER = "reverseDirOrderPreference";
	public static final String NUMBER_OF_THREADS = "numberOfThreadsPreference";
	
	private static final String APP_DIR = "RemoteGallery";
	private static final String THUMBNAIL_DIR = "thumbnails";
	private static final String CACHE_DIR = "cache";
	
	public boolean reverseDirOrder = true;
	public int numberOfThreads = 6;
	
	private static AppPreferences instance;
//	private Context context;
	private SharedPreferences prefs;
	
	protected AppPreferences(Context context) {
//		this.context = context;
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
		reloadPreferences();
	}
	
	public static AppPreferences getInstance(Context context) {
		if (instance == null) {
			instance = new AppPreferences(context);
		}
		return instance;
	}
	
	public void reloadPreferences() {
		reverseDirOrder = prefs.getBoolean(REVERSE_DIR_ORDER, true);
		numberOfThreads = Integer.valueOf(prefs.getString(NUMBER_OF_THREADS, "6")).intValue();
	}
	
	public static String getServerDir(ServerConf serverConf) {
		return Environment.getExternalStorageDirectory() + "/" + APP_DIR + "/" + serverConf.id;
	}
	
	public static String getCacheDir(ServerConf serverConf) {
		return getServerDir(serverConf) + "/" + AppPreferences.CACHE_DIR;
	}
	
	public static String getThumbnailDir(ServerConf serverConf) {
		return getServerDir(serverConf) + "/" + AppPreferences.THUMBNAIL_DIR;
	}
	
	public void updateValue(String preference, String value) {
		Editor editor = prefs.edit();
		editor.putString(preference, value);
		editor.commit();
	}

	public void updateValue(String preference, int value) {
		Editor editor = prefs.edit();
		editor.putInt(preference, value);
		editor.commit();
	}
	
	public void updateValue(String preference, float value) {
		Editor editor = prefs.edit();
		editor.putFloat(preference, value);
		editor.commit();
	}
	
	public void updateValue(String preference, boolean value) {
		Editor editor = prefs.edit();
		editor.putBoolean(preference, value);
		editor.commit();
	}
}
