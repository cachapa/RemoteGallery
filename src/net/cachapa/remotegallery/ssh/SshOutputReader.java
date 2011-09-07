package net.cachapa.remotegallery.ssh;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;

import net.cachapa.remotegallery.util.AppPreferences;
import net.cachapa.remotegallery.util.DirEntry;
import net.cachapa.remotegallery.util.DirEntryComparator;
import net.cachapa.remotegallery.util.ServerConf;
import android.content.Context;

public class SshOutputReader extends Ssh {
	public SshOutputReader(Context context, ServerConf serverConf) {
		super(context, serverConf);
	}
	
	public ArrayList<DirEntry> listDir(String path, final LogCallback logCallback) {
		String remoteCommand = "ls" +
			" '" + serverConf.remotePath + path + "'" +
			" --indicator-style=slash";
		
		final ArrayList<DirEntry> dirEntries = new ArrayList<DirEntry>();
		LogCallback dirListCallback = new LogCallback() {
			@Override
			public void logError(Ssh ssh, String log) {
				logCallback.logError(ssh, log);
			}
			@Override
			public void log(Ssh ssh, String log) {
				String[] result = log.split("\n");
				
				for (String name : result) {
					if (name.endsWith("/") || name.toLowerCase().endsWith(".jpg")) {
						dirEntries.add(new DirEntry(name.replace("/", ""), name.endsWith("/")));
					}
				}
				Collections.sort(dirEntries, new DirEntryComparator(AppPreferences.getInstance(context).reverseDirOrder));
			}
		};
		
		execute(remoteCommand, dirListCallback);
		
		return dirEntries;
	}

	@Override
	public void getDataStream(InputStream inputStream) throws IOException {
		BufferedReader outputReader = new BufferedReader(new InputStreamReader(inputStream));
		int read;
		char[] buffer = new char[4096];
		StringBuffer output = new StringBuffer();
		while ((read = outputReader.read(buffer)) > 0) {
			output.append(buffer, 0, read);
		}
		outputReader.close();
		String result = output.toString();
		logCallback.log(this, result);
	}
}
