/*
 * Copyright 2008-2010 Daniel Cachapa <cachapa@gmail.com>
 * 
 * This program is distributed under the terms of the GNU General Public License Version 3
 * The license can be read in its entirety in the LICENSE.txt file accompanying this source code,
 * or at: http://www.gnu.org/copyleft/gpl.html
 * 
 * This file is part of Libra.
 *
 * WeightWatch is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * WeightWatch is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the WeightWatch source code. If not, see: http://www.gnu.org/licenses
 */

package net.cachapa.remotegallery;

import net.cachapa.remotegallery.util.AppPreferences;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class Preferences extends PreferenceActivity implements OnPreferenceChangeListener, OnClickListener {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.preferences);
		addPreferencesFromResource(R.xml.preferences);
		
		TextView titleTextView = (TextView) findViewById(R.id.titleTextView);
		if (titleTextView != null) {
			titleTextView.setText(R.string.preferences);
		}
		
		findPreference(AppPreferences.REVERSE_DIR_ORDER).setOnPreferenceChangeListener(this);
		findPreference(AppPreferences.NUMBER_OF_THREADS).setOnPreferenceChangeListener(this);
		findPreference("serverConfigurationPreference").setIntent(new Intent(Intent.ACTION_VIEW, Uri.parse("http://cachapa.net/?page_id=429")));
		
		updateSummaries();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// App icon in Action Bar clicked; go home
			Intent intent = new Intent(this, Main.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void updateSummaries() {
		AppPreferences appPreferences = AppPreferences.getInstance(this);
		Preference pref;
		
		pref = findPreference(AppPreferences.NUMBER_OF_THREADS);
		pref.setSummary(String.valueOf(appPreferences.numberOfThreads));
	}

	@Override
	public boolean onPreferenceChange(Preference pref, Object newValue) {
		if (pref.getKey().equals(AppPreferences.REVERSE_DIR_ORDER)) {
			AppPreferences.getInstance(this).reverseDirOrder = ((Boolean)newValue).booleanValue();
		}
		
		if (pref.getKey().equals(AppPreferences.NUMBER_OF_THREADS)) {
			AppPreferences.getInstance(this).numberOfThreads = Integer.valueOf((String)newValue).intValue();
		}
		updateSummaries();
		return true;
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
		}
	}
}