package net.cachapa.remotegallery;

import net.cachapa.remotegallery.util.DownloadNotifier;
import net.cachapa.remotegallery.util.ImageSliderAdapter;
import net.cachapa.remotegallery.util.Util;
import net.cachapa.remotegallery.util.DownloadNotifier.onDownloadListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;

public class ImageSlider extends FragmentActivity implements OnPageChangeListener, onDownloadListener {
	private String[] imagePaths;
	private ViewPager viewPager;
	private DownloadNotifier downloadNotifier;
	private int currentPage = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.image_slider);
		
		Intent intent = getIntent();
		int index = intent.getIntExtra("index", 0);
		imagePaths = intent.getStringArrayExtra("paths");
		
		ImageSliderAdapter sliderAdapter = new ImageSliderAdapter(getSupportFragmentManager());
		sliderAdapter.setImagePaths(imagePaths);
		
		viewPager = (ViewPager) findViewById(R.id.viewPager);
		viewPager.setAdapter(sliderAdapter);
		viewPager.setOnPageChangeListener(this);
		viewPager.setCurrentItem(index);
		
		// Register a menu for the long press
		registerForContextMenu(viewPager);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		downloadNotifier = DownloadNotifier.getInstance();
		downloadNotifier.setOnDownloadListener(this);
	}
	
	/*** OnPageChangeListener ***/
	@Override
	public void onPageScrollStateChanged(int arg0) {
	}
	
	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
	}
	
	@Override
	public void onPageSelected(int page) {
		currentPage = page;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.image_slider, menu);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.getItem(0).setEnabled(Util.isCached(imagePaths[currentPage]));
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuShareImage:
			Intent shareChartIntent = new Intent(Intent.ACTION_SEND);
			shareChartIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + imagePaths[currentPage]));
			shareChartIntent.setType("image/jpg");
			startActivity(Intent.createChooser(shareChartIntent, getString(R.string.share_image)));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if (Util.isCached(imagePaths[currentPage])) {
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.image_slider, menu);
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		String path = imagePaths[currentPage];
		switch (item.getItemId()) {
		case R.id.menuShareImage:
			Intent shareChartIntent = new Intent(Intent.ACTION_SEND);
			shareChartIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + path));
			shareChartIntent.setType("image/jpg");
			startActivity(Intent.createChooser(shareChartIntent, getString(R.string.share_image)));
			return true;
		}
		
		return false;
	}
	
	@Override
	public void onDownloadComplete() {
		viewPager.getAdapter().notifyDataSetChanged();
	}
}
