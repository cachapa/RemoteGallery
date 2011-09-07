package net.cachapa.remotegallery.util;

import java.util.ArrayList;

import net.cachapa.remotegallery.R;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

public class ImageSliderAdapter extends FragmentStatePagerAdapter {
	private String[] imagePaths;
	public static ArrayList<ImageFragment> pages;

	public ImageSliderAdapter(FragmentManager fm) {
		super(fm);
		pages = new ArrayList<ImageFragment>();
	}

	@Override
	public Fragment getItem(int position) {
		return ImageFragment.newInstance(imagePaths[position]);
	}

	@Override
	public int getCount() {
		return imagePaths.length;
	}
	
	public void setImagePaths(String[] imagePaths) {
		this.imagePaths = imagePaths;
	}
	
	@Override
	public void notifyDataSetChanged() {
		int size = pages.size();
		for (int i = 0; i < size; i++) {
			pages.get(i).loadImage();
		}
		super.notifyDataSetChanged();
	}
	
	
	public static class ImageFragment extends Fragment {
		String imagePath;
		ImageView imageView;
		ProgressBar progressBar;
		
		static ImageFragment newInstance(String imagePath) {
			ImageFragment fragment = new ImageFragment();
			
//			// Pass the image path as an argument
			Bundle args = new Bundle();
			args.putString("imagePath", imagePath);
			fragment.setArguments(args);
			
			return fragment;
		}
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			imagePath = getArguments().getString("imagePath");
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View page = inflater.inflate(R.layout.image_viewer, container, false);
			
			imageView = (ImageView) page.findViewById(R.id.imageView);
			progressBar = (ProgressBar) page.findViewById(R.id.progressBar);
			loadImage();
			
			pages.add(this);
			
			return page;
		}
		
		public void loadImage() {
			if (imageView.getDrawable() == null && Util.isCached(imagePath)) {
				imageView.setImageBitmap(BitmapFactory.decodeFile(imagePath));
				progressBar.setVisibility(View.GONE);
			}
		}
		
		@Override
		public void onDestroy() {
			// This method is called automatically by the FragmentStatePageAdapter
			// for any existing page 2 pages away from the current one.
			// We use this opportunity and recycle the bitmap to avoid exceeding the VM budget
			
			pages.remove(this);
			
			BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
			if (drawable != null) {
				Bitmap image = drawable.getBitmap();
				if (image != null) {
					image.recycle();
					image = null;
				}
				imageView.setImageDrawable(null);
				drawable = null;
			}
			super.onDestroy();
		}
	}
}
