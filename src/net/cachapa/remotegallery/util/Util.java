package net.cachapa.remotegallery.util;

import java.io.File;
import java.io.FileOutputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Bitmap.CompressFormat;

public class Util {
	public static float Round(float rawValue, int decimalPlaces) {
		float p = (float) Math.pow(10, decimalPlaces);
		rawValue = rawValue * p;
		float tmp = Math.round(rawValue);
		return (float) tmp / p;
	}
	
	/**
	 * Calculates the number of files, directories and the total file size in the specified path
	 * @param path Full path to a given directory
	 * @return An array of 3 longs in the following format: {files, directories, total size (in bytes)}
	 */
	static public long[] dirInfo(File path) {
		long info[] = {0, 0, 0};
		if (path.exists()) {
			File[] files = path.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					info[1]++;
					long[] newInfo = dirInfo(files[i]);
					info[0] += newInfo[0];
					info[1] += newInfo[1];
					info[2] += newInfo[2];
				} else {
					info[0]++;
					info[2] += files[i].length();
				}
			}
		}
		return info;
	}
	
	static public int countFiles(File path) {
		int count = 0;
		if (path.exists()) {
			File[] files = path.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					count += countFiles(files[i]);
				} else {
					count++;
				}
			}
		}
		return count;
	}
	
	static public boolean deleteDirectory(File path, OnFileDeletedListener listener) {
		if (path.exists()) {
			File[] files = path.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteDirectory(files[i], listener);
				} else {
					files[i].delete();
					listener.onFileDeleted();
				}
			}
		}
		return (path.delete());
	}
	
	// Source: http://en.wikipedia.org/wiki/Byte
	private static final String[] suffixes = {"B", "KB", "MB", "GB", "TB", "PB", "ZB", "YB"};
	static public String bytesToHuman(long bytes) {
		float totalSize = bytes;
		int suffixIndex = 0;
		while (totalSize > 1024) {
			totalSize /= 1024;
			suffixIndex++;
		}
		return Round(totalSize, 1) + suffixes[suffixIndex];
	}
	
	public static void generateThumbnail(String originalPath, String thumbnailPath, int size) {
		Bitmap original = BitmapFactory.decodeFile(originalPath);
		
		float scale = (float)size / Math.max(original.getWidth(), original.getHeight());
		Matrix matrix = new Matrix();
		matrix.postScale(scale, scale);
		Bitmap thumbnailBitmap = Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(), matrix, false);
		try {
			FileOutputStream out = new FileOutputStream(thumbnailPath);
			thumbnailBitmap.compress(CompressFormat.JPEG, 90, out);
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// Release the memory
		original.recycle();
		thumbnailBitmap.recycle();
	}
	
	public static String lastFolderName(String path) {
		String[] folders = path.split("/");
		return folders[folders.length-1];
	}
	
	public static boolean isCached(String path) {
		return new File(path).exists();
	}
	
	
	public interface OnFileDeletedListener {
		public void onFileDeleted();
	}
}
