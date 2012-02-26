package net.cachapa.remotegallery;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import net.cachapa.remotegallery.ssh.LogCallback;
import net.cachapa.remotegallery.ssh.Ssh;
import net.cachapa.remotegallery.ssh.SshImageReader;
import net.cachapa.remotegallery.ssh.SshOutputReader;
import net.cachapa.remotegallery.ssh.SshThumbnailImageReader;
import net.cachapa.remotegallery.util.AppPreferences;
import net.cachapa.remotegallery.util.CacheDeleter;
import net.cachapa.remotegallery.util.Database;
import net.cachapa.remotegallery.util.Dialogs;
import net.cachapa.remotegallery.util.DirEntry;
import net.cachapa.remotegallery.util.DirEntryComparator;
import net.cachapa.remotegallery.util.DownloadNotifier;
import net.cachapa.remotegallery.util.ServerConf;
import net.cachapa.remotegallery.util.Util;
import net.cachapa.remotegallery.util.DownloadNotifier.onDownloadListener;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class RemoteBrowser extends ListActivity implements OnClickListener, OnGlobalLayoutListener, onDownloadListener {
	private DownloadNotifier downloadNotifier;
	private SshOutputReader sshOutputReader;
	private ServerConf serverConf;
	private String currentPath;
	private RotateAnimation refreshAnimation;
	private boolean isDownloadActive;
	
	/*** Activity lifecycle ***/
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.remote_browser);
		
		Intent intent = getIntent();
		serverConf = Database.getInstance(this).getServerConf(intent.getLongExtra("id", -1));
		if (serverConf == null) {
			// Means that this activity has been cleared from the memory stack
			// Instead of crashing, we panic and go back to the main activity, where it's safe
			Intent homeIntent = new Intent(this, Main.class);
			homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(homeIntent);
            return;
		}
		currentPath = intent.getStringExtra("path");
		setTitle((currentPath.length() > 0) ? Util.lastFolderName(currentPath) : serverConf.name);
		sshOutputReader = new SshOutputReader(this, serverConf);
		
		// Detect when the the refresh button has finished its layout
		View refreshButton = findViewById(R.id.refreshButton);
		if (refreshButton != null) {
			refreshButton.getViewTreeObserver().addOnGlobalLayoutListener(this);
		}
		
		// Try to load from cache, in case we're here because the screen was rotated
		ArrayList<DirEntry> dirEntries = (ArrayList<DirEntry>) getLastNonConfigurationInstance();
		if (dirEntries != null) {
			DirListAdapter dirListAdapter = new DirListAdapter(dirEntries);
			setListAdapter(dirListAdapter);
		}
		
		// Register a menu for the long press
		registerForContextMenu(getListView());

		// We want to register for notifications when either:
		//   (a) we're in the foreground (see onResume)
		//   (b) there is an ImageSlider directly on top of us, but we're the topmost
		//       RemoteBrowser underneath it (see onListItemClick).
		downloadNotifier = DownloadNotifier.getInstance();
	}

	@Override
	protected void onDestroy() {
		downloadNotifier.removeDownloadListener(this);
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		super.onResume();
		downloadNotifier.addDownloadListener(this);
		isDownloadActive = true;
		if (getListAdapter() != null) {
			getListAdapter().notifyDataSetChanged();
			getImages();
		}
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return getListAdapter().getDirEntries();
	}
	
	@Override
	public void onGlobalLayout() {
		// We need to create the animation here, otherwise the pivot isn't properly calculated
		if (refreshAnimation == null) {
			float pivot = (float)findViewById(R.id.refreshButton).getHeight() / 2;
			refreshAnimation = new RotateAnimation(0, 360, pivot, pivot);
			refreshAnimation.setDuration(1000);
			refreshAnimation.setRepeatCount(Animation.INFINITE);
			refreshAnimation.setInterpolator(new LinearInterpolator());
			
			// If we don't have a list already, load the cache and refresh from the server
			if (getListAdapter() == null) {
				DirListAdapter dirListAdapter = new DirListAdapter(loadCachedDirEntries());
				setListAdapter(dirListAdapter);
				new DirListLoader().execute(currentPath);
			}
		}
	}
	
	@Override
	public void setTitle(CharSequence title) {
		TextView titleTextView = (TextView) findViewById(R.id.titleTextView);
		if (titleTextView != null) {
			titleTextView.setText(title);
		}
		super.setTitle(title);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.remote_browser, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuFilter:
			InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			mgr.showSoftInput(findViewById(android.R.id.list), InputMethodManager.SHOW_IMPLICIT);
			return true;
			
		case R.id.menuRefresh:
			new DirListLoader().execute(currentPath);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		DirEntry dirEntry = (DirEntry)getListAdapter().getItem(((AdapterContextMenuInfo)menuInfo).position);
		if (isCached(dirEntry)) {
			menu.setHeaderTitle(dirEntry.name);
			if (dirEntry.isDirectory) {
				MenuInflater inflater = getMenuInflater();
				inflater.inflate(R.menu.remote_browser_context_dir, menu);
			}
			else {
				MenuInflater inflater = getMenuInflater();
				inflater.inflate(R.menu.remote_browser_context_image, menu);
			}
		}
		super.onCreateContextMenu(menu, v, menuInfo);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		DirEntry dirEntry = (DirEntry) getListAdapter().getItem(info.position);
		String cachePath = AppPreferences.getCacheDir(serverConf) + currentPath + "/" + dirEntry.name;
		
		switch (item.getItemId()) {
		case R.id.menuCacheSize:
			Dialogs.showCacheSizeDialog(this, cachePath);
			return true;
			
		case R.id.menuClearCache:
			String thumbnailPath = AppPreferences.getThumbnailDir(serverConf) + currentPath + "/" + dirEntry.name;
			new CacheDeleter(this).execute(cachePath, thumbnailPath);
			Toast.makeText(this, R.string.cache_cleared, Toast.LENGTH_SHORT).show();
			getListAdapter().notifyDataSetChanged();
			return true;
			
		case R.id.menuShareImage:
			Intent shareChartIntent = new Intent(Intent.ACTION_SEND);
			shareChartIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + cachePath));
			shareChartIntent.setType("image/jpg");
			startActivity(Intent.createChooser(shareChartIntent, getString(R.string.share_image)));
			return true;
		}
		return false;
	}
	
	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.homeButton:
			// Home icon in Action Bar clicked; go home without passing go
            Intent intent = new Intent(this, Main.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
			break;
			
		case R.id.filterButton:
			InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			mgr.showSoftInput(findViewById(android.R.id.list), InputMethodManager.SHOW_IMPLICIT);
			break;
			
		case R.id.refreshButton:
			new DirListLoader().execute(currentPath);
			break;
		}
	}
	
	private ArrayList<DirEntry> loadCachedDirEntries() {
		String path = AppPreferences.getCacheDir(serverConf) + currentPath;
		ArrayList<DirEntry> dirEntries = new ArrayList<DirEntry>();
		File dir = new File(path);
		final String[] children = dir.list();
		if (children != null) {
			for (String name : children) {
				if (new File(path + "/" + name).isDirectory()) {
					dirEntries.add(new DirEntry(name, true));
				}
				else if (name.toLowerCase().endsWith(".jpg")) {
					dirEntries.add(new DirEntry(name, false));
				}
			}
			Collections.sort(dirEntries, new DirEntryComparator(AppPreferences.getInstance(this).reverseDirOrder));
		}
		return dirEntries;
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		DirEntry entry = (DirEntry) getListAdapter().getItem(position);
		String path = currentPath + "/" + entry.name;
		if (entry.isDirectory) {
			// We're no longer the top most directory viewer - remove our listener
			downloadNotifier.removeDownloadListener(this);
			isDownloadActive = false;
			Intent remoteBrowserIntent = new Intent(this, net.cachapa.remotegallery.RemoteBrowser.class);
			remoteBrowserIntent.putExtra("path", path);
			startActivity(remoteBrowserIntent);
		}
		else {
			showImage(position, path);
		}
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			isDownloadActive = false;
		}
		return super.onKeyUp(keyCode, event);
	}
	
	@Override
	public DirListAdapter getListAdapter() {
		return (DirListAdapter) super.getListAdapter();
	}
	
	private boolean isCached(DirEntry entry) {
		String path = AppPreferences.getCacheDir(serverConf) + currentPath + "/" + entry.name;
		File file = new File(path);
		return file.exists();
	}

	private boolean isThumbCached(DirEntry entry) {
		String thumbnailPath = AppPreferences.getThumbnailDir(serverConf) + currentPath + "/" + entry.name;
		File file = new File(thumbnailPath);
		return file.exists();
	}

	private void showImage(int position, String path) {
		// Build a list of images
		DirListAdapter dirListAdapter = getListAdapter();
		ArrayList<String> entries = new ArrayList<String>();
		ArrayList<String> thumbEntries = new ArrayList<String>();
		for (DirEntry entry : dirListAdapter.getDirEntries()) {
			if (!entry.isDirectory) {
				entries.add(AppPreferences.getCacheDir(serverConf) + currentPath + "/" + entry.name);
				thumbEntries.add(AppPreferences.getThumbnailDir(serverConf) + currentPath + "/" + entry.name);				
			}
			else {
				position--;
			}
		}
		
		Intent imageViewerIntent = new Intent(RemoteBrowser.this, net.cachapa.remotegallery.ImageSlider.class);
		String[] entriesString = new String[entries.size()];
		String[] thumbEntriesString = new String[entries.size()];
		entriesString = entries.toArray(entriesString);
		thumbEntriesString = thumbEntries.toArray(thumbEntriesString);
		imageViewerIntent.putExtra("index", position);
		imageViewerIntent.putExtra("paths", entriesString);
		imageViewerIntent.putExtra("thumbPaths", thumbEntriesString);		
		startActivity(imageViewerIntent);
	}
	
	private void getImages() {
		// First we check how many threads are running on this directory
		int runningThreads = 0;
		ArrayList<DirEntry> entries = getListAdapter().getDirEntries();
		for (DirEntry entry : entries) {
			if (entry.isDownloading) {
				runningThreads++;
			}
			if (entry.isThumbDownloading) {
				runningThreads++;
			}
		}
		
		// We just start the necessary ammount of threads to get to the maximum
		int threadsMissing = AppPreferences.getInstance(RemoteBrowser.this).numberOfThreads - runningThreads;
		for (int i = 0; i < threadsMissing; i++) {
			getNextImage();
		}
	}
	
	private void getNextImage() {
		if (!isDownloadActive) {
			// If this window is running on the background, we stop loading further images
			// unless we're viewing images from this list, in which case, carry on
			return;
		}
		int firstPosition = getListView().getFirstVisiblePosition();
		DirEntry entry;
		String path;
		int size = getListAdapter().getCount();
		
		// First try and get thumbnails
		for (int i = 0; i < size; i++) {
			entry = (DirEntry) getListAdapter().getItem((i + firstPosition) % size);
			if (entry.isDirectory || entry.isDownloading || entry.isThumbDownloading ||
					isThumbCached(entry))
				continue;

			path = currentPath + "/" + entry.name;			
			if (!entry.alreadyDownloadedThumbOnce) {
				new ImageThumbDownloader(entry).execute(path);
				return;
			}
			else if (!entry.alreadyDownloadedOnce) {
				// Thumbnail fetch must have failed; prioritise a full fetch
				// of this file
				new ImageDownloader(entry).execute(path);
				return;
			}
		}
		
		// Then try and get the full quality image
		for (int i = 0; i < size; i++) {
			entry = (DirEntry) getListAdapter().getItem((i + firstPosition) % size);
			if (entry.isDirectory || entry.isDownloading || entry.isThumbDownloading ||
					isCached(entry))
				continue;
			
			path = currentPath + "/" + entry.name;
			if (!entry.alreadyDownloadedOnce) {
				new ImageDownloader(entry).execute(path);
				return;
			}
		}
	}
	
	private void activateProgressBar() {
		findViewById(R.id.refreshButton).setVisibility(View.GONE);
		findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
		findViewById(R.id.progressBar).startAnimation(refreshAnimation);
	}
	
	private void deactivateProgressBar() {
		findViewById(R.id.progressBar).setAnimation(null);
		findViewById(R.id.refreshButton).setVisibility(View.VISIBLE);
		findViewById(R.id.progressBar).setVisibility(View.GONE);
	}
	
	@Override
	public void onDownloadComplete() {
		getListAdapter().notifyDataSetChanged();
		getNextImage();
	}
	
	private class DirListLoader extends AsyncTask<String, String, ArrayList<DirEntry>> implements LogCallback {
		@Override
		protected void onPreExecute() {
			activateProgressBar();
		}
		
		@Override
		protected ArrayList<DirEntry> doInBackground(String... params) {
			try {
				return sshOutputReader.listDir(params[0], this);
			} catch (Exception e) {
				publishProgress(e.getLocalizedMessage());
				return null;
			}
		}
		
		@Override
		protected void onProgressUpdate(String... values) {
			if (hasWindowFocus()) {
				Toast.makeText(RemoteBrowser.this, "Error: " + values[0], Toast.LENGTH_LONG).show();
			}
		}
		
		@Override
		protected void onPostExecute(ArrayList<DirEntry> dirEntries) {
			deactivateProgressBar();
			if (dirEntries != null) {
				getListAdapter().setDirEntries(dirEntries);
				if (hasWindowFocus()) {
					Toast.makeText(RemoteBrowser.this, R.string.directory_refreshed, Toast.LENGTH_SHORT).show();
				}
				// We start a few threads to download the images from the server
				getImages();
			}
		}
		
		@Override
		public void log(Ssh ssh, String log) {
			// Do nothing
		}

		@Override
		public void logError(Ssh ssh, String log) {
			// There are two messages sent to the error stream at every succesful connection.
			// We ignore those.
			if (
					!log.toLowerCase().contains("key accepted unconditionally") &&
					!log.toLowerCase().contains("(fingerprint md5")
			) {
				publishProgress(log);
			}
		}
	}

	private abstract class GenericImageDownloader extends AsyncTask<String, String, String> implements LogCallback {
		protected DirEntry entry;

		public GenericImageDownloader(DirEntry entry) {
			this.entry = entry;
		}

		@Override
		protected void onProgressUpdate(String... values) {
			if (hasWindowFocus()) {
				Toast.makeText(RemoteBrowser.this, "Error: " + values[0], Toast.LENGTH_LONG).show();
			}
		}

		@Override
		public void log(Ssh ssh, String log) {
			// Do nothing
		}

		@Override
		public void logError(Ssh ssh, String log) {
			// There are two messages sent to the error stream at every
			// succesful connection.
			// We ignore those.
			if (!log.toLowerCase().contains("key accepted unconditionally") && !log.toLowerCase().contains("(fingerprint md5")) {
				publishProgress(log);
			}
		}
	}

	private class ImageDownloader extends GenericImageDownloader {
		public ImageDownloader(DirEntry entry) {
			super(entry);
		}

		@Override
		protected void onPreExecute() {
			entry.isDownloading = true;
		}
		
		@Override
		protected String doInBackground(String... params) {
			new SshImageReader(RemoteBrowser.this, serverConf).getImage(params[0], this);
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			entry.isDownloading = false;
			entry.alreadyDownloadedOnce = true;			
			downloadNotifier.notifyDownloadComplete();
		}
	}

	private class ImageThumbDownloader extends GenericImageDownloader {
		public ImageThumbDownloader(DirEntry entry) {
			super(entry);
		}

		@Override
		protected void onPreExecute() {
			entry.isThumbDownloading = true;
		}

		@Override
		protected String doInBackground(String... params) {
			new SshThumbnailImageReader(RemoteBrowser.this, serverConf).getImage(params[0], this);
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			entry.isThumbDownloading = false;
			entry.alreadyDownloadedThumbOnce = true;			
			downloadNotifier.notifyDownloadComplete();
		}
		
		@Override
		public void logError(Ssh ssh, String log) {
			// Ignore no thumbnail errors
			if (!log.toLowerCase().contains("image contains no thumbnail")) {
				super.logError(ssh, log);
			}
	}

	private class DirListAdapter extends BaseAdapter implements ListAdapter, Filterable {
		private ArrayList<DirEntry> dirEntries, preFilteredEntries;
		
		public DirListAdapter(ArrayList<DirEntry> dirEntries) {
			this.preFilteredEntries = this.dirEntries = dirEntries;
		}
		
		public void setDirEntries(ArrayList<DirEntry> dirEntries) {
			this.preFilteredEntries = this.dirEntries = dirEntries;
			notifyDataSetChanged();
		}
		
		public ArrayList<DirEntry> getDirEntries() {
			return dirEntries;
		}
		
		@Override
		public int getCount() {
			return dirEntries.size();
		}

		@Override
		public Object getItem(int position) {
			return dirEntries.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ItemViewHolder holder;
			
			if (convertView == null || convertView.getTag() == null) {
				convertView = LayoutInflater.from(RemoteBrowser.this).inflate(R.layout.browser_item, null);
				holder = new ItemViewHolder();
				holder.filenameView = (TextView) convertView.findViewById(R.id.filename);
				holder.thumbnailView = (ImageView) convertView.findViewById(R.id.thumbnail);
				holder.progressBar = convertView.findViewById(R.id.progressBar);
				
				convertView.setTag(holder);
			}
			else {
				holder = (ItemViewHolder) convertView.getTag();
			}
			
			DirEntry entry = dirEntries.get(position);
			String path = currentPath + "/" + entry.name;
			
			holder.filenameView.setText(entry.name);
			holder.thumbnailView.setVisibility(View.VISIBLE);
			holder.progressBar.setVisibility(View.INVISIBLE);
			if (entry.isDirectory) {
				if (isCached(entry)) {
					holder.thumbnailView.setImageResource(R.drawable.folder_cached);
				}
				else {
					holder.thumbnailView.setImageResource(R.drawable.folder);
				}
			}
			else {
				if (isThumbCached(entry)) {
					holder.thumbnailView.setImageBitmap(BitmapFactory.decodeFile(AppPreferences.getThumbnailDir(serverConf) + path));
				} else if (entry.isDownloading || entry.isThumbDownloading) {
					holder.thumbnailView.setVisibility(View.INVISIBLE);
					holder.progressBar.setVisibility(View.VISIBLE);
				} else {
					holder.thumbnailView.setImageResource(android.R.drawable.ic_menu_gallery);
				}
			}
			
			return convertView;
		}

		@Override
		public Filter getFilter() {
			return new Filter() {
				@SuppressWarnings("unchecked")
				@Override
				protected void publishResults(CharSequence constraint, FilterResults results) {
					dirEntries = (ArrayList<DirEntry>) results.values;
					notifyDataSetChanged();
				}
				
				@Override
				protected FilterResults performFiltering(CharSequence constraint) {
					ArrayList<DirEntry> filteredDirEntries = new ArrayList<DirEntry>();
					for (DirEntry dirEntry : preFilteredEntries) {
						if (dirEntry.name.toLowerCase().contains(constraint.toString().toLowerCase())) {
							filteredDirEntries.add(dirEntry);
						}
					}
					
					FilterResults results = new FilterResults();
					results.values = filteredDirEntries;
					results.count = filteredDirEntries.size();
					return results;
				}
			};
		}
	}
	
	
	static class ItemViewHolder {
		TextView filenameView;
		ImageView thumbnailView;
		View progressBar;
	}
}
