/**
 * 
 */
package com.adviteya.mobile.activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.adviteya.mobile.business.MobileAttendanceBusinessUtils;
import com.adviteya.mobile.service.AttendanceService;
import com.adviteya.mobile.util.AppStatus;
import com.adviteya.mobile.util.MobileConstants;

/**
 * @author Dheeraj
 * 
 */
public class MobileAttendanceLogin extends Activity implements OnClickListener,
		LocationListener, MobileConstants {
	// LocationListener, MobileConstants, OnSharedPreferenceChangeListener {

	private SharedPreferences prefs;
	private TextView textLoginId;
	private TextView pwdPasscode;
	private TextView textEmpDetails, errMsgText, textLocation;
	private Button buttonSubmit, refreshLocation, buttonForgetPassword,
			continueAtten, logoutExit;
	private static final String TAG = "MobileAttendanceLogin";
	public static long companyId = 0L;
	private boolean gps_enabled = false;
	private Location gps_loc = null;
	private LocationManager lm = null;
	public static double longitude = 0D;
	public static double latitue = 0D;
	public static double accuracy = 0D;
	public static String userVerificationMsg = "";
	public static CharSequence _empId, _pwd, _latlang;
	public static boolean empAssignmentsAvailable;
	TelephonyManager telephonyManager;
	PhoneStateListener listener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "-- onCreate --");
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main1);
		TextView textViewMsgWindow = (TextView) findViewById(R.id.textViewMsgWindow);
		textViewMsgWindow.setTextColor(Color.GREEN);
		textLocation = (TextView) findViewById(R.id.textLocation);

		// @todo : get Cell number

		// TelephonyManager tm = (TelephonyManager)
		// getSystemService(Context.TELEPHONY_SERVICE);
		// String getSimSerialNumber = tm.getSimSerialNumber();
		// Log.i("SIM Serial Number ", getSimSerialNumber);

		lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		buttonSubmit = (Button) findViewById(R.id.buttonSubmit);
		buttonSubmit.setEnabled(false);

		buttonForgetPassword = (Button) findViewById(R.id.buttonForgetPassword);
		buttonForgetPassword.setEnabled(false);

		continueAtten = (Button) findViewById(R.id.contiAttend);
		continueAtten.setVisibility(View.INVISIBLE);

		logoutExit = (Button) findViewById(R.id.logoutExit);
		logoutExit.setVisibility(View.INVISIBLE);

		textLoginId = (TextView) findViewById(R.id.textLoginId);
		pwdPasscode = (TextView) findViewById(R.id.pwdPasscode);
		textLoginId.setText("LoginId");
		pwdPasscode.setText("****");

		// Get the telephony manager
		telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

		// Create a new PhoneStateListener
		listener = new PhoneStateListener() {
			@Override
			public void onCallStateChanged(int state, String incomingNumber) {
				String stateString = "N/A";
				switch (state) {
				case TelephonyManager.DATA_DISCONNECTED:
					stateString = "data disconnected";
					buttonSubmit.setEnabled(false);
					buttonForgetPassword.setEnabled(false);
					break;
				case TelephonyManager.DATA_CONNECTED:
					stateString = "data connected";
					buttonSubmit.setEnabled(true);
					buttonForgetPassword.setEnabled(true);
					break;
				}
			}
		};

		// exceptions will be thrown if provider is not permitted.
		try {

			AppStatus appStatus = AppStatus.getInstance(this);
			if (appStatus.isOnline()) {

				gps_enabled = lm
						.isProviderEnabled(LocationManager.GPS_PROVIDER);
				if (gps_enabled) {
					gps_loc = lm
							.getLastKnownLocation(LocationManager.GPS_PROVIDER);
					buttonSubmit.setEnabled(true);
					buttonForgetPassword.setEnabled(true);
				} else {
					Toast.makeText(this,
							"This application need GPS. Please enable GPS.",
							Toast.LENGTH_LONG).show();
					textViewMsgWindow.setTextColor(Color.RED);
					textViewMsgWindow.setText("--GPS not enabled --");

				}
			} else {
				Toast.makeText(
						this,
						"This application need Data network. Please enable Data connectivity.",
						Toast.LENGTH_LONG).show();
				textViewMsgWindow.setTextColor(Color.RED);
				textViewMsgWindow.setText("--Data connectivity not enabled --");
			}
		} catch (Exception ex) {
			Log.e(TAG, "-- Err in GPS --", ex);
		}

		// prefs = PreferenceManager.getDefaultSharedPreferences(this);
		// prefs.registerOnSharedPreferenceChangeListener((OnSharedPreferenceChangeListener)
		// this);
	}

	/**
	 * The meth0d does the following :
	 * 
	 * <pre>
	 * 1. Authenticate the MobileAttendanceUser
	 * by using registered Cell and SIM card #, user's current long and lat,
	 * userId, and passkey 
	 * 2. The client will make a WS call to the server for
	 * authentication. 
	 * 3. The server will return - 
	 * a. session key valid for xxxx
	 * min (@todo - need define the session time) 
	 * b. Company name, registered
	 * location. If the user is registered for multiple location, then show it
	 * for the current registered location 
	 * c. last Synch datetime and location.
	 * </pre>
	 * 
	 * @param view
	 */
	public void loadAttendanceRegister(View view) {

		Log.i(TAG, "-- loadAttendanceRegister --");

		// Starting sequence of the usecase

		// 1. get SIM card number and cell number
		// 2. get location lang and lat
		// 3.

		boolean _noError = false;
		String _errMsgString = "";

		textLoginId = (TextView) findViewById(R.id.textLoginId);
		_empId = textLoginId.getText();

		pwdPasscode = (TextView) findViewById(R.id.pwdPasscode);
		_pwd = pwdPasscode.getText();

		errMsgText = (TextView) findViewById(R.id.textViewMsgWindow);

		textLocation = (TextView) findViewById(R.id.textLocation);
		_latlang = textLocation.getText();

		if (_empId != null) {
			if (!(_empId.length() == 0)) {
				_noError = true;
			} else {
				_noError = false;
				_errMsgString += LOGIN_ID_VALIDATION + "\n";
			}
		} else {
			_errMsgString += LOGIN_ID_VALIDATION + "\n";
		}

		if (_pwd != null) {
			if (!(_pwd.length() == 0)) {
				_noError = true;
			} else {
				// fix for dev env only
				_noError = false;
				_errMsgString += PASSCODE_VALIDATION + "\n";
			}
		} else {
			// fix for dev env only
			_noError = false;
			_errMsgString += PASSCODE_VALIDATION;
		}

		if (_noError) {

			// Invoke Authentication
			// boolean mobAttenAdmin = MobileAttendanceBusinessUtils
			// .validateAttendanceAdmin(null);
			// Set companyId.
			// @todo : to be replaced by WS call

			try {
				HttpClient client = new DefaultHttpClient();
				String _validMobileSupervisor = VALIDATE_MOBILE_SUPERVISOR;

				String _webAppURL = BASE_URL + _validMobileSupervisor;
				// HttpGet request = new HttpGet(_webAppURL);
				HttpPost httppost = new HttpPost(_webAppURL);
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(
						3);
				nameValuePairs.add(new BasicNameValuePair("textLoginId", _empId
						.toString().trim()));
				nameValuePairs.add(new BasicNameValuePair("pwdPasscode", _pwd
						.toString().trim()));
				nameValuePairs.add(new BasicNameValuePair("latLang", _latlang
						.toString().trim()));
				httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

				HttpResponse response = client.execute(httppost);
				Header[] authMsg = response.getHeaders("AUTH_MSG");
				Header[] hasPlannedAssignments = response
						.getHeaders("PLANNED_ASSIGNMENTS");

				userVerificationMsg = authMsg[0].getValue();
				if (userVerificationMsg
						.equalsIgnoreCase(MOBILE_SUPERVISOR_AUTHENTICATION_SUCCESS)) {
					empAssignmentsAvailable = Boolean
							.parseBoolean(hasPlannedAssignments[0].getValue());
				}

				Log.i("Header data ", authMsg[0].getValue());

			} catch (ClientProtocolException e) {
				Log.e(TAG, e.getMessage());
			} catch (IllegalStateException e) {
				Log.e(TAG, e.getMessage());
			} catch (IOException e) {
				Log.e(TAG, e.getMessage());
			}

			textLoginId.setText(null);
			pwdPasscode.setText(null);
			errMsgText.setText(null);

			if (userVerificationMsg
					.equalsIgnoreCase(MOBILE_SUPERVISOR_AUTHENTICATION_SUCCESS)) {
				Intent synchIntent = new Intent(view.getContext(),
						com.adviteya.mobile.activity.SynchronizeActivity.class);
				startActivityForResult(synchIntent, 0);
			} else {
				errMsgText.setBackgroundColor(Color.BLACK);
				errMsgText.setText(ADMIN_USER_VERIFICATION_FAILED);
				errMsgText.setTextColor(Color.RED);
				errMsgText.setTextSize((float) 15.0);
			}

		} else {
			errMsgText.setBackgroundColor(Color.BLACK);
			errMsgText.setText(_errMsgString);
			errMsgText.setTextColor(Color.RED);
			errMsgText.setTextSize((float) 15.0);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.i(TAG, "-- onCreateOptionsMenu --");

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(TAG, "-- onOptionsItemSelected --");
		/**
		 * switch (item.getItemId()) { case R.id.itemPref: startActivity(new
		 * Intent(this, PrefsActivity.class)); break; case R.id.startServ:
		 * startService(new Intent(this, AttendanceService.class)); break; case
		 * R.id.stopServ: stopService(new Intent(this,
		 * AttendanceService.class)); break; }
		 **/
		return true;
	}

	/**
	 * 
	 */
	public void onClick(View v) {
		Log.i(TAG, "--------- invoked onClick --------");
	}

	/**
	 * 
	 * @param v
	 */
	public void refreshLocation(View v) {
		Log.i(TAG, "-- Clicked on Refresh Location --");

		TextView textViewMsgWindow = (TextView) findViewById(R.id.textViewMsgWindow);
		textViewMsgWindow.setTextColor(Color.GREEN);

		LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		// exceptions will be thrown if provider is not permitted.
		try {

			AppStatus appStatus = AppStatus.getInstance(this);
			if (appStatus.isOnline()) {
				buttonSubmit.setEnabled(true);
				buttonForgetPassword.setEnabled(true);
			} else {
				buttonSubmit.setEnabled(false);
				buttonForgetPassword.setEnabled(false);
			}

			gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

			if (gps_enabled) {
				gps_loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			} else {
				textViewMsgWindow.setTextColor(Color.RED);
				textViewMsgWindow.setText("--GPS not enabled --");

			}
		} catch (Exception ex) {
			Log.e(TAG, "-- Err in GPS --", ex);
		}

		if (gps_loc != null) {

			StringBuilder sb = new StringBuilder(512);
			longitude = gps_loc.getLongitude();
			latitue = gps_loc.getLatitude();
			accuracy = gps_loc.getAccuracy();

			String strLongitude = Location.convert(longitude,
					Location.FORMAT_SECONDS);
			String strLatitude = Location.convert(latitue,
					Location.FORMAT_SECONDS);

			sb.append(" Long :" + strLongitude);
			sb.append(" Lat  :" + strLatitude);
			sb.append(" Acu  :" + Math.round(accuracy) + " mts");

			Toast.makeText(this, "Accuracy is " + Math.round(accuracy)
					+ " meters. ", 10);

			textLocation.setText(sb.toString());
			textViewMsgWindow.setTextColor(Color.GREEN);
			textViewMsgWindow.setText("Location found");

		} else {
			textViewMsgWindow.setTextColor(Color.RED);
			textViewMsgWindow.setText("--Unable to get Location --");
		}
	}

	@Override
	protected void onResume() {
		Log.i(TAG, "-- Invoked onResume -- ");
		String phoneModel = android.os.Build.MODEL;
		Log.i(TAG, "-- PhoneModel --" + phoneModel + "-"
				+ android.os.Build.MANUFACTURER + "-" + android.os.Build.DEVICE
				+ "-" + android.os.Build.DISPLAY);
		/**
		 * onResume is is always called after onStart, even if the app hasn't
		 * been paused
		 * 
		 * add location listener and request updates every 1000ms or 10m
		 */
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10f, this);

		Log.i(TAG, "-- Still Logged In -- " + _empId + " -- " + _pwd);

		textLoginId = (TextView) findViewById(R.id.textLoginId);
		pwdPasscode = (TextView) findViewById(R.id.pwdPasscode);

		if (_empId != null) {
			if (userVerificationMsg
					.equalsIgnoreCase(MOBILE_SUPERVISOR_AUTHENTICATION_SUCCESS)) {

				textLoginId.setText(_empId);
				pwdPasscode.setText(_pwd);
				textLoginId.setEnabled(false);
				pwdPasscode.setEnabled(false);

				buttonSubmit = (Button) findViewById(R.id.buttonSubmit);
				buttonSubmit.setVisibility(View.GONE);
				buttonForgetPassword = (Button) findViewById(R.id.buttonForgetPassword);
				buttonForgetPassword.setVisibility(View.GONE);

				refreshLocation = (Button) findViewById(R.id.refreshLocation);
				refreshLocation.setVisibility(View.GONE);

				continueAtten = (Button) findViewById(R.id.contiAttend);
				continueAtten.setVisibility(View.VISIBLE);

				logoutExit = (Button) findViewById(R.id.logoutExit);
				logoutExit.setVisibility(View.VISIBLE);
			}
		}

		super.onResume();
	}

	@Override
	protected void onPause() {
		/* GPS, as it turns out, consumes battery like crazy */
		lm.removeUpdates(this);
		super.onResume();
	}

	/**
	 * 	
	 */
	public void onSharedPreferenceChanged(SharedPreferences arg0, String arg1) {
		Log.i(TAG, "--------- invoked onSharedPreferenceChanged -------- "
				+ arg1);
		Log.i(TAG, arg0.getString("username", ""));
	}

	@Override
	public void onLocationChanged(Location location) {
		Log.i(TAG, "-- onLocationChanged --");
		TextView textViewMsgWindow = (TextView) findViewById(R.id.textViewMsgWindow);
		LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		try {
			gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

			if (gps_enabled) {
				gps_loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			} else {
				textViewMsgWindow.setTextColor(Color.RED);
				textViewMsgWindow.setText("--GPS not enabled --");

			}
		} catch (Exception ex) {
			Log.e(TAG, "-- Err in GPS --", ex);
		}

		StringBuilder sb = new StringBuilder(512);

		if (gps_enabled) {
			longitude = gps_loc.getLongitude();
			latitue = gps_loc.getLatitude();
			accuracy = gps_loc.getAccuracy();

			String strLongitude = Location.convert(longitude,
					Location.FORMAT_SECONDS);
			String strLatitude = Location.convert(latitue,
					Location.FORMAT_SECONDS);

			sb.append(" Long :" + strLongitude);
			sb.append(" Lat  :" + strLatitude);
			sb.append(" Acu  :" + Math.round(accuracy) + " mts");

		} else {
			textViewMsgWindow.setTextColor(Color.RED);
			textViewMsgWindow.setText("--GPS not enabled --");
		}

		// sb.append("Timestamp: ");
		// sb.append(location.getTime());
		// sb.append('\n');

		textLocation.setText(sb.toString());
	}

	/**
	 * 
	 */
	@Override
	public void onProviderDisabled(String provider) {
		Log.i(TAG, "-- onProviderDisabled --");
		Toast.makeText(this, "This application need GPS. Please enable GPS.",
				Toast.LENGTH_LONG).show();
		/* bring up the GPS settings */
		Intent intent = new Intent(
				android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		startActivity(intent);
	}

	/**
	 * 
	 */
	@Override
	public void onProviderEnabled(String provider) {
		Log.i(TAG, "-- onProviderEnabled --");
		Toast.makeText(this, "GPS Enabled", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		Log.i(TAG, "-- onStatusChanged --");

		/* This is called when the GPS status alters */
		switch (status) {
		case LocationProvider.OUT_OF_SERVICE:
			Log.i(TAG, "GPS Status Changed: Out of Service");
			Toast.makeText(this, "Status Changed: Out of Service",
					Toast.LENGTH_SHORT).show();
			break;
		case LocationProvider.TEMPORARILY_UNAVAILABLE:
			Log.i(TAG, "GPS Status Changed: Temporarily Unavailable");
			Toast.makeText(this, "Status Changed: Temporarily Unavailable",
					Toast.LENGTH_SHORT).show();
			break;
		case LocationProvider.AVAILABLE:
			Log.i(TAG, "GPS Status Changed: Available");
			Toast.makeText(this, "Status Changed: Available",
					Toast.LENGTH_SHORT).show();
			buttonSubmit.setEnabled(true);
			buttonForgetPassword.setEnabled(true);
			break;
		}

	}

	@Override
	protected void onStop() {
		/*
		 * may as well just finish since saving the state is not important for
		 * this toy app
		 */
		// Do not call this. This is leading to exit out of the system.
		// finish();
		super.onStop();
	}

	/**
	 * Close all threads and exit from the Application
	 * 
	 * @param v
	 */
	public void exitApplication(View v) {

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
	}

	/**
	 * 
	 * @param view
	 */
	public void continueAttendance(View view) {
		Intent synchIntent = new Intent(view.getContext(),
				com.adviteya.mobile.activity.SynchronizeActivity.class);
		startActivityForResult(synchIntent, 0);
	}

	/**
	 * 
	 * @param view
	 */
	public void logoutAndExit(View view) {
		_empId = null;
		_pwd = null;
		System.runFinalizersOnExit(true);
		System.exit(0);
	}

	/**
	 * 
	 */
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		if (intent != null)
			Log.i(TAG, "-- onActivityResult --" + intent.getType() + "--"
					+ resultCode);
	}

}
