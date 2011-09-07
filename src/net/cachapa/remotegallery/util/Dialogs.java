package net.cachapa.remotegallery.util;

import java.io.File;

import net.cachapa.remotegallery.R;

import android.app.AlertDialog;
import android.content.Context;

public class Dialogs {
	public static void showCacheSizeDialog(Context context, String path) {
		long[] dirInfo = Util.dirInfo(new File(path));
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.cache_size)
		.setMessage(String.format(context.getString(R.string.cache_size_body), dirInfo[0], dirInfo[1], Util.bytesToHuman(dirInfo[2])))
		.setCancelable(true)
		.setPositiveButton(android.R.string.ok, null);
		builder.create().show();
	}
}
