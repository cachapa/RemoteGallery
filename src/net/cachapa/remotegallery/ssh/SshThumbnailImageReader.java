package net.cachapa.remotegallery.ssh;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import net.cachapa.remotegallery.util.AppPreferences;
import net.cachapa.remotegallery.util.ServerConf;
import net.cachapa.remotegallery.util.Util;
import android.content.Context;
import android.util.DisplayMetrics;

public class SshThumbnailImageReader extends Ssh {
	private String path;

	public SshThumbnailImageReader(Context context, ServerConf serverConf) {
		super(context, serverConf);
	}
	
	public void getImage(String path, LogCallback logCallback) {
		this.path = path;
		String remoteCommand = "jhead -st - '" + serverConf.remotePath + path + "'";
		execute(remoteCommand, logCallback);
	}
	
	@Override
	public void getDataStream(InputStream inputStream) throws IOException {
		String imagePath = AppPreferences.getCacheDir(serverConf) + path;
		File outputFile = new File(imagePath + ".tn.temp");
		// Create the directory
		File parent = new File(outputFile.getParent());
		parent.mkdirs();
		
		int read;
		byte[] buffer = new byte[32 * 1024];
		BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(outputFile));
		while ((read = inputStream.read(buffer)) != -1) {
			output.write(buffer, 0, read);
		}
		inputStream.close();
		output.close();
		
		if (outputFile.length() > 0) {
			// Generate the thumbnail
			String thumbnailPath = AppPreferences.getThumbnailDir(serverConf) + path;
			int size = (int) (48 * context.getResources().getDisplayMetrics().density);
			
			// Create the thumbnails directory, and the .nomedia file, if necessary
			if (new File(new File(thumbnailPath).getParent()).mkdirs()) {
				File noMediaFile = new File(AppPreferences.getThumbnailDir(serverConf) + "/.nomedia");
				if (!noMediaFile.exists()) {
					noMediaFile.createNewFile();
				}
			}
			
			Util.generateThumbnail(imagePath + ".tn.temp", thumbnailPath, size);
			
			// Delete temp file
			outputFile.delete();
		}
	}
}
