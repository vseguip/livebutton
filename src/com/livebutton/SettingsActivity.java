package com.livebutton;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;

public class SettingsActivity extends PreferenceActivity {

	private static final String LOG_TAG = "SettingsActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(LOG_TAG, "Creating SettingsActivity");
		addPreferencesFromResource(R.xml.prefs);
		// Get the custom preference
	}
}
