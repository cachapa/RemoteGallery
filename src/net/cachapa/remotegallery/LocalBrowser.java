package net.cachapa.remotegallery;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import net.cachapa.remotegallery.util.DirEntry;
import net.cachapa.remotegallery.util.DirEntryComparator;
import net.cachapa.remotegallery.util.Util;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class LocalBrowser extends ListActivity implements OnClickListener {
	private static final int RESULT_CANCEL_PICKER = 10;
	
	private String currentPath;
	private DirListAdapter dirListAdapter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.local_browser);
		
		Intent intent = getIntent();
		currentPath = intent.getStringExtra("path");
		if (currentPath.length() == 0) {
			currentPath = Environment.getExternalStorageDirectory().getAbsolutePath();
		}
		setTitle(Util.lastFolderName(currentPath));
		
		dirListAdapter = new DirListAdapter(loadDirEntries());
		setListAdapter(dirListAdapter);
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
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.homeButton:
			// Home icon in Action Bar clicked; go home without passing go
            Intent intent = new Intent(this, Main.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
			break;
			
		case R.id.CancelButton:
			setResult(RESULT_CANCEL_PICKER);
			finish();
		}
	}
	
	private ArrayList<DirEntry> loadDirEntries() {
		String path = currentPath;
		ArrayList<DirEntry> dirEntries = new ArrayList<DirEntry>();
		File dir = new File(path);
		final String[] children = dir.list();
		if (children != null) {
			for (String name : children) {
				if (new File(path + "/" + name).isDirectory()) {
					dirEntries.add(new DirEntry(name, true));
				}
				else {
					dirEntries.add(new DirEntry(name, false));
				}
			}
			Collections.sort(dirEntries, new DirEntryComparator(false));
		}
		return dirEntries;
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		DirEntry entry = (DirEntry) getListAdapter().getItem(position);
		String path = currentPath + "/" + entry.name;
		if (entry.isDirectory) {
			Intent localBrowserIntent = new Intent(this, net.cachapa.remotegallery.LocalBrowser.class);
			localBrowserIntent.putExtra("path", path);
			startActivityForResult(localBrowserIntent, 1);
		}
		else {
			Intent intent = new Intent();
			intent.putExtra("path", path);
			setResult(RESULT_OK, intent);
			finish();
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK && requestCode == 1) {
			setResult(RESULT_OK, data);
			finish();
		}
		if (resultCode == RESULT_CANCEL_PICKER) {
			finish();
		}
	}
	
	
	public class DirListAdapter extends BaseAdapter implements ListAdapter {
		ArrayList<DirEntry> dirEntries;
		
		public DirListAdapter(ArrayList<DirEntry> dirEntries) {
			this.dirEntries = dirEntries;
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
				convertView = LayoutInflater.from(LocalBrowser.this).inflate(R.layout.browser_item, null);
				holder = new ItemViewHolder();
				holder.filenameView = (TextView) convertView.findViewById(R.id.filename);
				holder.thumbnailView = (ImageView) convertView.findViewById(R.id.thumbnail);
				
				convertView.setTag(holder);
			}
			else {
				holder = (ItemViewHolder) convertView.getTag();
			}
			
			DirEntry entry = dirEntries.get(position);
			
			holder.filenameView.setText(entry.name);
			if (entry.isDirectory) {
				holder.thumbnailView.setImageResource(R.drawable.folder);
			}
			else {
				holder.thumbnailView.setImageResource(R.drawable.generic_file);
			}
			
			return convertView;
		}
	}
	
	
	static class ItemViewHolder {
		TextView filenameView;
		ImageView thumbnailView;
	}
}
