package com.adviteya.mobile.activity;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class PrefsActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("-- Loading PrefActivity --");
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.pref);
	}

	

}
