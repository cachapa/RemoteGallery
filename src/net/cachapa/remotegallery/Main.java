package net.cachapa.remotegallery;

import java.util.ArrayList;

import net.cachapa.remotegallery.util.AppPreferences;
import net.cachapa.remotegallery.util.CacheDeleter;
import net.cachapa.remotegallery.util.Database;
import net.cachapa.remotegallery.util.Dialogs;
import net.cachapa.remotegallery.util.ServerConf;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class Main extends ListActivity implements OnClickListener {
	/*** Activity lifecycle ***/
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		// Register a menu for the long press
		registerForContextMenu(getListView());
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		ArrayList<ServerConf> serverConfs = Database.getInstance(this).getAllServerConfs();
		setListAdapter(new ServerListAdapter(serverConfs));
	}
	
	/*** OnClick Listeners ***/
	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.iconButton:
			startActivity(new Intent(this, net.cachapa.remotegallery.Preferences.class));
			break;
			
		case R.id.addButton:
			startActivity(new Intent(this, net.cachapa.remotegallery.ConfigureServer.class));
			break;
		}
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		ServerConf serverConf = (ServerConf) getListAdapter().getItem(position);
		Intent remoteBrowserIntent = new Intent(this, net.cachapa.remotegallery.RemoteBrowser.class);
		remoteBrowserIntent.putExtra("id", serverConf.id);
		remoteBrowserIntent.putExtra("path", "");
		startActivity(remoteBrowserIntent);
	}
	
	/*** Menus ***/
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		ServerConf serverConf = (ServerConf) getListAdapter().getItem(((AdapterContextMenuInfo)menuInfo).position);
		menu.setHeaderTitle(serverConf.name);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_context, menu);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		ServerConf serverConf = (ServerConf) getListAdapter().getItem(info.position);
		Intent intent;
		
		switch (item.getItemId()) {
		case R.id.menuTest:
			intent = new Intent(this, net.cachapa.remotegallery.ConnectionTest.class);
			intent.putExtra("id", serverConf.id);
			startActivity(intent);
			return true;
			
		case R.id.menuEdit:
			intent = new Intent(this, net.cachapa.remotegallery.ConfigureServer.class);
			intent.putExtra("id", serverConf.id);
			startActivity(intent);
			return true;
			
		case R.id.menuCacheSize:
			Dialogs.showCacheSizeDialog(this, AppPreferences.getCacheDir(serverConf));
			return true;
			
		case R.id.menuClearCache:
			new CacheDeleter(this).execute(AppPreferences.getServerDir(serverConf));
			return true;
			
		case R.id.menuDelete:
			Database database = Database.getInstance(this);
			database.deleteServerConf(serverConf);
			ArrayList<ServerConf> serverConfs = database.getAllServerConfs();
			setListAdapter(new ServerListAdapter(serverConfs));
			return true;
		}
		return false;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuAddServer:
			startActivity(new Intent(this, net.cachapa.remotegallery.ConfigureServer.class));
			return true;
			
		case R.id.menuPreferences:
			startActivity(new Intent(this, net.cachapa.remotegallery.Preferences.class));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	
	/*** Adapter ***/
	public class ServerListAdapter extends BaseAdapter implements ListAdapter {
		ArrayList<ServerConf> serverConfs;
		
		public ServerListAdapter(ArrayList<ServerConf> serverConfs) {
			this.serverConfs = serverConfs;
		}
		
		@Override
		public int getCount() {
			return serverConfs.size();
		}

		@Override
		public Object getItem(int position) {
			return serverConfs.get(position);
		}

		@Override
		public long getItemId(int position) {
			return serverConfs.get(position).id;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ServerConf serverConf = serverConfs.get(position);
			View view = LayoutInflater.from(Main.this).inflate(android.R.layout.simple_list_item_2, null);
			((TextView)view.findViewById(android.R.id.text1)).setText(serverConf.name);
			((TextView)view.findViewById(android.R.id.text2)).setText(serverConf.fullAddress());
			return view;
		}
	}
}