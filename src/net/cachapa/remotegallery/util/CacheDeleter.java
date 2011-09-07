package net.cachapa.remotegallery.util;

import java.io.File;

import net.cachapa.remotegallery.R;
import net.cachapa.remotegallery.util.Util.OnFileDeletedListener;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.BaseAdapter;
import android.widget.Toast;

public class CacheDeleter extends AsyncTask<String, Integer, Void> implements OnFileDeletedListener {
	private Context context;
	private ProgressDialog progressDialog;
	private int totalFiles = 0, filesDeleted = 0;
	
	public CacheDeleter(Context context) {
		this.context = context;
	}
	
	@Override
	protected void onPreExecute() {
		progressDialog = new ProgressDialog(context);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setTitle(context.getString(R.string.clearing_cache));
		progressDialog.setCancelable(false);
	}
	
	@Override
	protected Void doInBackground(String... paths) {
		for (int i = 0; i < paths.length; i++) {
			File file = new File(paths[i]);
			totalFiles += Util.countFiles(file);
		}
		
		for (int i = 0; i < paths.length; i++) {
			File file = new File(paths[i]);
			Util.deleteDirectory(file, this);
		}
		return null;
	}
	
	@Override
	protected void onProgressUpdate(Integer... values) {
		if (!progressDialog.isShowing()) {
			progressDialog.setMax(totalFiles);
			progressDialog.show();
		}
		progressDialog.setProgress(values[0]);
	}
	
	@Override
	protected void onPostExecute(Void result) {
		Toast.makeText(context, R.string.cache_cleared, Toast.LENGTH_SHORT).show();
		progressDialog.dismiss();
		
		// Try to refresh the list's icons
		if (context instanceof ListActivity) {
			ListActivity la = (ListActivity) context;
			if (la.getListAdapter() instanceof BaseAdapter) {
				((BaseAdapter) la.getListAdapter()).notifyDataSetChanged();
			}
		}
	}
	
	@Override
	public void onFileDeleted() {
		filesDeleted++;
		publishProgress(filesDeleted);
	}
}