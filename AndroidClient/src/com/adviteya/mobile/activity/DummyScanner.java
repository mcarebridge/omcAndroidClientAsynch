/**
 * 
 */
package com.adviteya.mobile.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

/**
 * @author deejay
 * 
 */
public class DummyScanner extends Activity
{
	private static final String TAG = "DummyScanner";
	
	/**
	 * 
	 * @param view
	 */
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		Log.i(TAG, "-- onCreate --");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dummyscanner);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);

	}
	
	public void scanBarcode(View view)
	{
		
		Log.i(TAG, "-- scanBarcode --");
		Intent resultData = new Intent();
		resultData.putExtra("name", "dheeraj");
		setResult(Activity.RESULT_OK, resultData);
		finish();
		
	}
	
	@Override
	public void finish()
	{
		Log.i(TAG, "-- finish --");
		super.finish();
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
	}
	
}
