package com.adviteya.mobile.activity;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.TimeZone;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.adviteya.mobile.business.MobileAttendanceBusinessUtils;
import com.adviteya.mobile.service.AttendanceDatabaseAdaptor;
import com.adviteya.mobile.util.AppStatus;
import com.adviteya.mobile.util.AttendanceUtil;
import com.adviteya.mobile.util.MobileConstants;
import com.adviteya.mobile.vo.MobileAttendanceAdminVO;
import com.adviteya.mobile.vo.MobileEmplyoeeVO;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class HeadlessClient extends Activity implements OnClickListener,
        LocationListener, MobileConstants
{
	
	private boolean                   gps_enabled              = false;
	private Location                  gps_loc                  = null;
	private LocationManager           lm                       = null;
	public static double              longitude                = 0D;
	public static double              latitue                  = 0D;
	public static double              accuracy                 = 0D;
	private static final String       TAG                      = "HeadlessClient";
	private String                    latLong                  = "";
	// private String networkStateString = "";
	TelephonyManager                  telephonyManager;
	PhoneStateListener                listener;
	private ImageView                 actionIcon;
	private TextView                  datetime, coordinates;
	// private TableRow tableRow5;
	private MobileAttendanceAdminVO   _mobileAttendance;
	private AttendanceDatabaseAdaptor dbAdaptor;
	private SimpleDateFormat          sdf1                     = new SimpleDateFormat(
	                                                                   "EEE, d MMM yyyy HH:mm:ss aaa");
	private final static String       LOGON                    = "logon";
	private final static String       LOGOFF                   = "logoff";
	private final static String       EMPALERT                 = "emp_alert";
	private final static String       SYSTEMREADY              = "systemready";
	private final static String       SYNCH                    = "android_synch";
	private final static String       BROKEN                   = "package_broken";
	private boolean                   isSupervisorVerified     = false;
	private int                       omcState                 = OMC_OPEN;
	private int                       supervisorVerifyErr      = OMC_GEN_FAIL;
	private String                    supervisorVerifyMsg      = OMC_GEN_FAIL_MSG;
	
	private String                    supervisorUserId         = "-";
	private boolean                   adminUserLocallyVerified = false;
	private boolean                   instanceStarted          = false;
	private boolean                   isNotified               = false;
	private int                       notificationId           = 0;
	private AlarmManager              alarmManager             = null;
	
	private BroadcastReceiver         br                       = null;
	private PendingIntent             pi                       = null;
	private String                    managerCellnumber        = "9561093652";
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		Log.i(TAG, ">>>>> Starting onCreate <<<<<<");
		Log.i(TAG, "--------> OMC STATE ------> " + omcState);
		
		super.onCreate(savedInstanceState);
		
		if (savedInstanceState != null)
		{
			instanceStarted = savedInstanceState.getBoolean("instanceStarted");
			_mobileAttendance = (MobileAttendanceAdminVO) savedInstanceState
			        .getSerializable("_mobileAttendance");
			isSupervisorVerified = savedInstanceState
			        .getBoolean("isSupervisorVerified");
		}
		
		Log.i(TAG, ">>>>> Value of instanceStarted <<<<<<" + instanceStarted);
		
		setContentView(R.layout.main);
		
		// tableRow5 = (TableRow) findViewById(R.id.tableRow5);
		
		datetime = (TextView) findViewById(R.id.datetime);
		datetime.setBackgroundColor(Color.WHITE);
		coordinates = (TextView) findViewById(R.id.coordinates);
		coordinates.setText("Loading geo coordinates...");
		coordinates.setBackgroundColor(Color.WHITE);
		try
		{
			
			ImageView logo = (ImageView) findViewById(R.id.logo);
			logo.setVisibility(View.VISIBLE);
			
			actionIcon = (ImageView) findViewById(R.id.logon);
			actionIcon.setVisibility(View.VISIBLE);
			actionIcon.setEnabled(true);
			actionIcon.setClickable(true);
			
			LinearLayout l_layout1 = (LinearLayout) findViewById(R.id.tableLayout1);
			l_layout1.setBackgroundColor(Color.GRAY);
			
			TableLayout t_layout = (TableLayout) findViewById(R.id.tableLayout1);
			t_layout.setBackgroundColor(Color.WHITE);
			
			// start :: check NTP time
			
			int _providerAutoTime = android.provider.Settings.System.getInt(
			        getContentResolver(),
			        android.provider.Settings.System.AUTO_TIME);
			Log.i(TAG, "Provider auto time : " + _providerAutoTime);
			
			boolean _dateTimeAvl = false;
			
			// Register AlarmListener
			setUp();
			
			if (_providerAutoTime == 1)
			{
				_dateTimeAvl = true;
				// try
				// {
				// Date _currentSystemDate = new Date();
				// Date _ntpDate = new Date(AttendanceUtil.getNTPTime());
				//
				// long _diff = _currentSystemDate.getTime()
				// - _ntpDate.getTime();
				//
				// double _diffInMins = _diff / ((double) 1000 * 60);
				//
				// if (Math.abs(_diffInMins) < 5)
				// {
				// datetime.setText(sdf1.format(_ntpDate));
				// _dateTimeAvl = true;
				// } else
				// {
				// datetime.setText("Inaccurate Phone time :: Expected time : "
				// + sdf1.format(_ntpDate));
				// }
				//
				// } catch (NTPTimeoutException e)
				// {
				// Toast.makeText(
				// this,
				// "NTP Connection Timeout. Please check data settings",
				// Toast.LENGTH_LONG).show();
				// Log.i(TAG,
				// "Conncetion Time out. Please visit your Company office");
				// }
			} else
			{
				Toast.makeText(
				        this,
				        "OneMasterControl Android application uses Network provided values. Please change phone settings",
				        Toast.LENGTH_LONG).show();
			}
			// end :: check NTP time
			
			// get location
			boolean isLocationAvl = checkLocation();
			
			Log.i(TAG, ">>>>> Location Available <<<<<<" + isLocationAvl);
			
			// get network
			if (isLocationAvl && _dateTimeAvl)
			{
				// read IMEI code
				String _imeiCode = readIMEICode();
				if (_mobileAttendance == null)
				{
					_mobileAttendance = new MobileAttendanceAdminVO();
				}
				
				_mobileAttendance.setImeiNumber(_imeiCode);
				_mobileAttendance.setLatLong(latLong);
				
				/**
				 * Authenticate the user locally. If verified then do not invoke
				 * scanner.
				 * 
				 */
				dbAdaptor = new AttendanceDatabaseAdaptor(this);
				dbAdaptor.open();
				adminUserLocallyVerified = dbAdaptor
				        .isAdminAuthenticated(_mobileAttendance);
				
				Log.i(TAG, "onCreate >>>>>> adminUserLocallyVerified : "
				        + adminUserLocallyVerified);
				
				dbAdaptor.close();
				// checkNetwork();
				
				_mobileAttendance = new MobileAttendanceAdminVO();
				_mobileAttendance.setImeiNumber(_imeiCode);
				_mobileAttendance.setLatLong(latLong);
				
				// if the admin user is locally verified then supervisor is
				// verified
				
				if (adminUserLocallyVerified)
				{
					isSupervisorVerified = true;
				}
				
				if (!instanceStarted)
				{
					// isSupervisorVerified = false;
					invokeScanner();
				}
			} else
			{
				final Context context = this;
				int systemalert = getResources().getIdentifier(SYSTEMREADY,
				        "drawable", getPackageName());
				actionIcon.setImageResource(systemalert);
				actionIcon.setOnClickListener(new ImageView.OnClickListener() {
					public void onClick(View v)
					{
						finish();
					}
				});
			}
		} catch (Exception e)
		{
			e.printStackTrace();
			Toast.makeText(
			        this,
			        "Unexpected error caused. Please visit your Company office",
			        Toast.LENGTH_LONG).show();
			Log.i(TAG,
			        "Unexpected error caused. Please visit your Company office");
		}
	}
	
	/**
	 * 
	 */
	private void setUp()
	{
		br = new BroadcastReceiver() {
			@Override
			public void onReceive(Context c, Intent i)
			{
				Log.i(TAG, " -- About to Trigger Alarm  --");
				Toast.makeText(
				        c,
				        "Timeout :: OMC did not detect any activity for 15 mins",
				        Toast.LENGTH_LONG).show();
				timeOut(c);
			}
		};
		registerReceiver(br, new IntentFilter("com.adviteya.mobile.activity"));
		pi = PendingIntent.getBroadcast(this, 0, new Intent(
		        "com.adviteya.mobile.activity"), 0);
		alarmManager = (AlarmManager) (this
		        .getSystemService(Context.ALARM_SERVICE));
		
	}
	
	/**
	 * 
	 * @param savedInstanceState
	 */
	@Override
	public void onDestroy()
	{
		Log.i(TAG, " -- Calling Destroy --");
		super.onDestroy();
	}
	
	/**
	 * 
	 */
	@Override
	public void onResume()
	{
		Log.i(TAG, " -- Calling Resume --");
		super.onResume();
		/**
		 * onResume is is always called after onStart, even if the app hasn't
		 * been paused
		 * 
		 * add location listener and request updates every 1000ms or 10m
		 */
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10f, this);
	}
	
	/**
	 * 
	 */
	@Override
	public void onRestart()
	{
		Log.i(TAG, " -- Calling Restart --");
		super.onRestart();
		/**
		 * onResume is is always called after onStart, even if the app hasn't
		 * been paused
		 * 
		 * add location listener and request updates every 1000ms or 10m
		 */
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10f, this);
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState)
	{
		Log.i(TAG, " -- Calling onSaveInstanceState --");
		savedInstanceState.putBoolean("instanceStarted", true);
		Log.i(TAG, " -- onSaveInstanceState  _mobileAttendance --"
		        + _mobileAttendance);
		savedInstanceState.putSerializable("mobileAttendance",
		        _mobileAttendance);
		savedInstanceState.putBoolean("isSupervisorVerified",
		        isSupervisorVerified);
		savedInstanceState.putBoolean("isNotified", isNotified);
		savedInstanceState.putInt("notificationId", notificationId);
		savedInstanceState.putInt("omcState", omcState);
		
		super.onSaveInstanceState(savedInstanceState);
	}
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState)
	{
		Log.i(TAG, " -- Calling onRestoreInstanceState --");
		super.onRestoreInstanceState(savedInstanceState);
		instanceStarted = savedInstanceState.getBoolean("instanceStarted");
		isSupervisorVerified = savedInstanceState
		        .getBoolean("isSupervisorVerified");
		isNotified = savedInstanceState.getBoolean("isNotified");
		notificationId = savedInstanceState.getInt("savedInstanceState");
		omcState = savedInstanceState.getInt("omcState");
		
		_mobileAttendance = (MobileAttendanceAdminVO) savedInstanceState
		        .getSerializable("mobileAttendance");
		Log.i(TAG, " -- onRestoreInstanceState _mobileAttendance --"
		        + _mobileAttendance);
	}
	
	/**
	 * 
	 */
	private void invokeScanner()
	{
		// Toast.makeText(this, "Ready to scan attendance", Toast.LENGTH_LONG)
		// .show();
		omcState = OMC_READYTOSCAN;
		Log.i(TAG, "--------> OMC STATE ------> " + omcState);
		
		// Register Alarm
		alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
		        SystemClock.elapsedRealtime() + OMC_TIMEOUT_15MINS, pi);
		
		IntentIntegrator _iInteg = new IntentIntegrator(HeadlessClient.this);
		_iInteg.initiateScan();
		
	}
	
	/**
	 * 
	 */
	protected void onActivityResult(int requestCode, int resultCode,
	        Intent intent)
	{
		Log.i(TAG,
		        "------> START onActivityResult  --" + sdf1.format(new Date()));
		Log.i(TAG, "------> supervisorUserId   --" + supervisorUserId);
		omcState = OMC_SCANNING;
		Log.i(TAG, "--------> OMC STATE ------> " + omcState);
		
		// Log.i(TAG, "-- onActivityResult --");
		int _systemReadyId = getResources().getIdentifier(SYSTEMREADY,
		        "drawable", getPackageName());
		actionIcon.setImageResource(_systemReadyId);
		
		IntentResult scanResult = IntentIntegrator.parseActivityResult(
		        requestCode, resultCode, intent);
		if (scanResult != null)
		{
			// Log.i(TAG, "-- onActivityResult.STEP-1");
			String _scannedContents = scanResult.getContents();
			Log.i(TAG, "-- _scannedContents -- " + _scannedContents);
			if (_scannedContents != null)
			{
				// Log.i(TAG, "-- onActivityResult.STEP-2");
				StringTokenizer _st = new StringTokenizer(_scannedContents, "@");
				String _companyId = (String) _st.nextElement();
				String _scannedEmpId = (String) _st.nextElement();
				
				supervisorUserId = _scannedEmpId;
				// _mobileAttendance.setUserName(supervisorUserId);
				
				Log.i(TAG, "-- SupervisorId  --" + supervisorUserId);
				Log.i(TAG, "-- isSupervisorVerified  --" + isSupervisorVerified);
				
				// This block will executed only after supervisor has been
				// verified
				if (isSupervisorVerified
				        && _mobileAttendance.getUserName() != null)
				{
					omcState = OMC_AUTHENTICATED;
					Log.i(TAG, "--------> OMC STATE ------> " + omcState);
					
					int _bizOpenId = getResources().getIdentifier(LOGON,
					        "drawable", getPackageName());
					
					sendSupervisorLoggedInNotification();
					
					actionIcon.setImageResource(_bizOpenId);
					/**
					 * Verify the employee and update the record if verified
					 */
					Log.i(TAG,
					        "-- BEFORE EMP VERIFY & UPDATE  --"
					                + sdf1.format(new Date()));
					boolean _empVerified = verifyEmployee(_companyId,
					        _scannedEmpId);
					Log.i(TAG,
					        "-- AFTER EMP VERIFY & UPDATE --"
					                + sdf1.format(new Date()));
					
					if (_empVerified)
					{
						omcState = OMC_READYTOSCAN;
						Log.i(TAG, "--------> OMC STATE ------> " + omcState);
						
						invokeScanner();
					} else
					{
						
						try
						{
							playSound(this);
						} catch (IllegalArgumentException e)
						{
							e.printStackTrace();
						} catch (SecurityException e)
						{
							e.printStackTrace();
						} catch (IllegalStateException e)
						{
							e.printStackTrace();
						} catch (IOException e)
						{
							e.printStackTrace();
						}
						
						Toast.makeText(this,
						        "Employee not verified. Inform HR",
						        Toast.LENGTH_LONG).show();
						int _emp_alert = getResources().getIdentifier(EMPALERT,
						        "drawable", getPackageName());
						
						actionIcon.setImageResource(_emp_alert);
						actionIcon
						        .setOnClickListener(new ImageView.OnClickListener() {
							        public void onClick(View v)
							        {
								        invokeScanner();
							        }
						        });
						
					}
				} else
				{
					omcState = OMC_AUTHENTICATING;
					Log.i(TAG, "--------> OMC STATE ------> " + omcState);
					
					authenticateSupervisor(_scannedEmpId);
				}
			} else
			{
				Log.i(TAG, "-- onActivityResult.STEP-3");
				Toast.makeText(this, " -- Unrecognized Format --",
				        Toast.LENGTH_LONG).show();
				
				if (isSupervisorVerified)
				{
					logon();
				} else
				{
					logout();
				}
				
			}
		} else
		{
			Log.i(TAG, "-- onActivityResult.STEP-4");
			Toast.makeText(this, " -- No scan results --", Toast.LENGTH_LONG)
			        .show();
			if (isSupervisorVerified)
			{
				logon();
			} else
			{
				logout();
			}
		}
		
		Log.i(TAG, "<------ END onActivityResult  --" + sdf1.format(new Date()));
	}
	
	/**
	 * Authenticate supervisor based on userName and IMEI code
	 */
	private void authenticateSupervisor(String _scannedEmpId)
	{
		Log.i(TAG, "-- authenticateSupervisor --");
		
		final Context context = this;
		
		int _bizOpenId = getResources().getIdentifier(SYNCH, "drawable",
		        getPackageName());
		actionIcon.setImageResource(_bizOpenId);
		_mobileAttendance.setUserName(supervisorUserId);
		
		// Authenticate user
		Log.i(TAG, "-- Before validating User-- " + isSupervisorVerified);
		try
		{
			supervisorVerifyErr = MobileAttendanceBusinessUtils
			        .validateAttendanceAdmin(_mobileAttendance, this);
		} catch (Exception e1)
		{
			// TODO Auto-generated catch block
			Log.e(TAG, "-- Exception in Authentication-- " + e1.getMessage(),
			        e1);
		}
		Log.i(TAG, "-- After validating User-- " + isSupervisorVerified);
		
		if (supervisorVerifyErr != OMC_AUTH_SUCCESS)
		{
			omcState = OMC_AUTH_FAILED;
			Log.i(TAG, "--------> OMC STATE ------> " + omcState);
			
			switch (supervisorVerifyErr)
			{
				case OMC_CONN_TIMEOUT:
					supervisorVerifyMsg = OMC_CONN_TIMEOUT_MSG;
					break;
				case OMC_AUTH_FAIL:
					supervisorVerifyMsg = OMC_AUTH_FAIL_MSG;
					break;
				
				case OMC_ASG_FAIL:
					supervisorVerifyMsg = OMC_ASG_FAIL_MSG;
					break;
				default:
				case OMC_GEN_FAIL:
					supervisorVerifyMsg = OMC_GEN_FAIL_MSG;
					break;
			}
			
			AttendanceUtil.sendSMS(
			        managerCellnumber,
			        "OMC Message for User Id : "
			                + (_mobileAttendance != null ? _mobileAttendance
			                        .getUserName() : " No logged in User ")
			                + " OMC Authentication Error :"
			                + supervisorVerifyMsg);
			
			Toast.makeText(this, supervisorVerifyMsg, Toast.LENGTH_LONG).show();
			Log.i(TAG, supervisorVerifyMsg);
			cancelSupervisorLoggedInNotification(supervisorVerifyMsg);
			
			// Just to close any open connections.
			if (dbAdaptor != null)
			{
				dbAdaptor.close();
			}
			
			int _logoff = getResources().getIdentifier(LOGOFF, "drawable",
			        getPackageName());
			actionIcon.setImageResource(_logoff);
			actionIcon.setOnClickListener(new ImageView.OnClickListener() {
				public void onClick(View v)
				{
					finish();
				}
			});
		}
		// Invoke scanner
		else
		{
			omcState = OMC_AUTHENTICATED;
			Log.i(TAG, "--------> OMC STATE ------> " + omcState);
			
			// Temp Alarm for 2 mins
			// alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
			// SystemClock.elapsedRealtime() + 120000, pi);
			
			sendSupervisorLoggedInNotification();
			_mobileAttendance.setUserName(_scannedEmpId);
			dbAdaptor = new AttendanceDatabaseAdaptor(this);
			dbAdaptor.open();
			
			// Update the admin user locally
			
			dbAdaptor.authenicateAdminUserLocal(_mobileAttendance, "Y");
			
			Log.i(TAG, "-- updated local db . Local verification = 'Y'--");
			
			Log.i(TAG, "-- Before setting Synch Button--");
			_bizOpenId = getResources().getIdentifier(SYNCH, "drawable",
			        getPackageName());
			actionIcon.setImageResource(_bizOpenId);
			Log.i(TAG, "-- Before After Synch Button--");
			
			Toast.makeText(
			        this,
			        "Press to Synch Data. Please do not switch off or press back button during data Synch",
			        Toast.LENGTH_LONG).show();
			
			actionIcon.setOnClickListener(new ImageView.OnClickListener() {
				public void onClick(View v)
				{
					try
					{
						if (_mobileAttendance != null)
						{
							Log.i(TAG, "--- Synching the DB----");
							omcState = OMC_SYNCHING_DATA;
							Log.i(TAG, "--------> OMC STATE ------> "
							        + omcState);
							
							String _processedMessage = MobileAttendanceBusinessUtils
							        .syncLocalDb(context,
							                _mobileAttendance.getImeiNumber(),
							                _mobileAttendance.getUserName(),
							                _mobileAttendance.getLatLong());
							
							Log.i(TAG,
							        "--- _processedMessage message received : ----"
							                + _processedMessage);
							
							if (_processedMessage.equals("DATA_PROCESSED"))
							{
								isSupervisorVerified = true;
								invokeScanner();
							} else if (_processedMessage
							        .contains("DATA_SUBMITTED"))
							{
								String _timeToSynch = _processedMessage.substring(
								        _processedMessage.indexOf("^") + 1,
								        _processedMessage.length());
								Toast.makeText(
								        context,
								        "Your request has been submitted. Please Synch again in next "
								                + _timeToSynch + " mins",
								        Toast.LENGTH_LONG).show();
							}
						}
					} catch (Exception e)
					{
						Log.e(TAG, "---------------->" + e.getMessage());
						
						AttendanceUtil
						        .sendSMS(
						                managerCellnumber,
						                "OMC Message : "
						                        + (_mobileAttendance != null ? _mobileAttendance
						                                .getUserName()
						                                : " No logged in User ")
						                        + " Facing Data connectivity outage");
						
						e.printStackTrace();
						errorRouter(SERVER_ERR);
						
						// finish();
					}
					
				}
			});
		}
	}
	
	/**
	 * 
	 */
	private void logon()
	{
		Log.i(TAG, "-- Logging IN --");
		omcState = OMC_STANDBY;
		Log.i(TAG, "--------> OMC STATE ------> " + omcState);
		
		// Register Alarm
		alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
		        SystemClock.elapsedRealtime() + OMC_TIMEOUT_15MINS, pi);
		
		int _bizOpenId = getResources().getIdentifier(LOGON, "drawable",
		        getPackageName());
		actionIcon.setImageResource(_bizOpenId);
		
		actionIcon.setOnClickListener(new ImageView.OnClickListener() {
			public void onClick(View v)
			{
				invokeScanner();
			}
		});
	}
	
	/**
	 * Read IMEI code
	 */
	private String readIMEICode()
	{
		TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		String _deviceId = telephonyManager.getDeviceId();
		Log.i(TAG, _deviceId);
		return _deviceId;
	}
	
	/**
	 * Read mobile number
	 * 
	 * @return
	 */
	private String readLineNumber()
	{
		TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		String _deviceId = telephonyManager.getLine1Number();
		Log.i(TAG, _deviceId);
		return _deviceId;
	}
	
	/**
	 * 
	 */
	private void checkNetwork()
	{
		
		// Get the telephony manager
		telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		
		// Create a new PhoneStateListener
		listener = new PhoneStateListener() {
			@Override
			public void onCallStateChanged(int state, String incomingNumber)
			{
				String stateString = "N/A";
				switch (state)
				{
					case TelephonyManager.DATA_DISCONNECTED:
						stateString = "data disconnected";
						break;
					case TelephonyManager.DATA_CONNECTED:
						stateString = "data connected";
						break;
				}
				// networkStateString = stateString;
			}
		};
		
		// Toast.makeText(this, networkStateString, Toast.LENGTH_LONG).show();
		
	}
	
	/**
	 * This method checks location as well as availability of network
	 */
	private boolean checkLocation() throws Exception
	{
		// exceptions will be thrown if provider is not permitted.
		boolean gpsEnabled = false;
		
		try
		{
			lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			AppStatus appStatus = AppStatus.getInstance(this);
			StringBuilder sb = new StringBuilder(512);
			
			if (appStatus.isOnline())
			{
				
				gps_enabled = lm
				        .isProviderEnabled(LocationManager.GPS_PROVIDER);
				if (gps_enabled)
				{
					gpsEnabled = true;
					lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,
					        1000, 10f, this);
					gps_loc = lm
					        .getLastKnownLocation(LocationManager.GPS_PROVIDER);
					if (gps_loc != null)
					{
						
						longitude = gps_loc.getLongitude();
						latitue = gps_loc.getLatitude();
						accuracy = gps_loc.getAccuracy();
						
						String strLongitude = Location.convert(longitude,
						        Location.FORMAT_SECONDS);
						String strLatitude = Location.convert(latitue,
						        Location.FORMAT_SECONDS);
						
						coordinates.setText("Long : " + strLongitude
						        + " Lat : " + strLatitude + " Accuracy : "
						        + Math.round(accuracy) + " mts");
						
						sb.append("LONG:" + longitude);
						sb.append("@");
						sb.append("LAT:" + latitue);
						sb.append("@");
						sb.append("ACU:" + Math.round(accuracy));
					} else
					{
						sb.append("LOCATION_NOT_FOUND");
					}
					
				} else
				{
					Toast.makeText(this,
					        "This application need GPS. Please enable GPS.",
					        Toast.LENGTH_LONG).show();
					
				}
			} else
			{
				AttendanceUtil
				        .sendSMS(
				                managerCellnumber,
				                "OMC Message : "
				                        + (_mobileAttendance != null ? _mobileAttendance
				                                .getUserName()
				                                : " No logged in User ")
				                        + " Facing Data connectivity outage");
				Toast.makeText(
				        this,
				        "This application need Data network. Please enable Data connectivity.",
				        Toast.LENGTH_LONG).show();
			}
			
			latLong = sb.toString();
			
			Log.i(TAG, "LatLang " + latLong);
			return gpsEnabled;
		} catch (Exception ex)
		{
			Log.e(TAG, "-- Err in GPS --", ex);
			throw new Exception(ex);
		}
		
	}
	
	@Override
	public void onLocationChanged(Location arg0)
	{
		Log.i(TAG, "-- Location Changed --");
		try
		{
			checkLocation();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public void onProviderDisabled(String arg0)
	{
		Log.i(TAG, "-- onProviderDisabled --");
		Intent intent = new Intent(
		        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		startActivity(intent);
	}
	
	/**
	 * 
	 * @param companyId
	 * @param scannedEmpId
	 */
	private boolean verifyEmployee(String companyId, String scannedEmpId)
	{
		boolean _empVerified = false;
		
		CharSequence _empId = scannedEmpId;
		CharSequence _pwd = "NOT_NEEDED";
		// Verify the Employee against the local db
		AttendanceDatabaseAdaptor dbAdaptor = new AttendanceDatabaseAdaptor(
		        this);
		dbAdaptor.open();
		// Code to check the fulldatabase
		// try
		// {
		// List _mobiledbList = dbAdaptor.getAllMobileSynch();
		// for (Iterator iterator = _mobiledbList.iterator(); iterator
		// .hasNext();)
		// {
		// MobileEmplyoeeVO _mobileEmp = (MobileEmplyoeeVO) iterator.next();
		// Log.i(TAG, " -- Emp Company Id" + _mobileEmp.getEmpCompanyId());
		//
		//
		// }
		//
		// } catch (Exception e)
		// {
		//
		// e.printStackTrace();
		// }
		
		// @todo : need to populate company id
		MobileEmplyoeeVO mobileEmp = dbAdaptor.authenticateEmplyoee(
		        _empId.toString(), _pwd.toString());
		
		if (mobileEmp != null)
		{
			_empVerified = true;
			Log.i(TAG, "-- BEFORE EMP UPDATE EMP  --" + sdf1.format(new Date()));
			
			markAttendance(mobileEmp);
			
			Log.i(TAG, "-- AFTER EMP UPDATE EMP  --" + sdf1.format(new Date()));
			
		} else
		{
			// Employee Not-verified
		}
		
		dbAdaptor.close();
		
		return _empVerified;
	}
	
	/**
	 * Update the attendance record for planned assignments. It doesn't work if
	 * the rule has been choosen as "NO PLANNED ASSIGNMENT"
	 * 
	 * @param view
	 */
	public void markAttendance(MobileEmplyoeeVO mobileEmp)
	{
		
		// service and add a record -
		
		// AttendanceService service = new AttendanceService();
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy kk:mm:ss");
		TimeZone _t = TimeZone.getDefault();
		// Log.i(TAG, _t.getDisplayName());
		Calendar _c = Calendar.getInstance(_t);
		
		if (mobileEmp.getInTime().equals("-"))
		{
			// set in time
			mobileEmp.setInTime(sdf.format(_c.getTime()));
		} else if (!mobileEmp.getInTime().equals("-"))
		// i.e If IN-TIME is a DATE
		{
			if (mobileEmp.getOutTime().equals("-"))
			{
				
				// set out time
				mobileEmp.setOutTime(sdf.format(_c.getTime()));
			} else if (!mobileEmp.getOutTime().equals("-"))
			{
				// i.e If OUT-TIME is a DATE
				// Do nothing. Keeping it open for a Logic
			}
		}
		
		updateRecord(mobileEmp.getTimeSheetId().longValue(),
		        mobileEmp.getInTime(), mobileEmp.getOutTime(), mobileEmp
		                .getEmpId().longValue());
		
		if (!isNotified)
		{
			sendUpdateNotification();
		}
		
		// if (MobileAttendanceLogin.empAssignmentsAvailable)
		// {
		// updateRecord(timeSheetId, inTime, outTime);
		// } else
		// {
		// updateRecordForNoAssignment(timeSheetId, inTime, outTime,
		// SynchronizeActivity.selectedLocationKey,
		// SynchronizeActivity.selectedShiftKey);
		// }
	}
	
	private void updateRecord(long timeSheetId, String inTime, String outTime,
	        long empId)
	{
		AttendanceDatabaseAdaptor dbAdaptor = new AttendanceDatabaseAdaptor(
		        this);
		dbAdaptor.open();
		dbAdaptor.updateMobileSync(timeSheetId, inTime, outTime, empId);
		dbAdaptor.close();
	}
	
	@Override
	public void onProviderEnabled(String provider)
	{
		Log.i(TAG, "-- onProviderEnabled --");
		
	}
	
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras)
	{
		Log.i(TAG, "-- onStatusChanged --");
		
	}
	
	@Override
	public void onClick(View arg0)
	{
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * 
	 * @param context
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public void playSound(Context context) throws IllegalArgumentException,
	        SecurityException, IllegalStateException, IOException
	{
		Uri soundUri = RingtoneManager
		        .getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		MediaPlayer mMediaPlayer = new MediaPlayer();
		mMediaPlayer.setDataSource(context, soundUri);
		final AudioManager audioManager = (AudioManager) context
		        .getSystemService(Context.AUDIO_SERVICE);
		if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0)
		{
			mMediaPlayer.setAudioStreamType(AudioManager.STREAM_RING);
			mMediaPlayer.setLooping(false);
			mMediaPlayer.prepare();
			mMediaPlayer.start();
		}
	}
	
	/**
	 * If back button is pressed log out of system
	 */
	@Override
	public void onBackPressed()
	{
		logout();
	}
	
	private void logout()
	{
		Log.i(TAG, "--- Logging out --");
		
		final Context context = this;
		// Intent mainIntent = new Intent(this,
		// com.adviteya.mobile.activity.HeadlessClient.class);
		// startActivity(mainIntent);
		
		// isSupervisorVerified = false;
		omcState = OMC_READY_TO_LOGOUT;
		Log.i(TAG, "--------> OMC STATE ------> " + omcState);
		
		Toast.makeText(
		        this,
		        "Press to Logout. Please do not switch off or press back button till action is complete",
		        Toast.LENGTH_LONG).show();
		
		int _okId = getResources().getIdentifier(LOGOFF, "drawable",
		        getPackageName());
		actionIcon.setImageResource(_okId);
		
		Log.i(TAG, "--------> OMC STATE ------> " + omcState);
		
		// If the logout button is not pressed
		// alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
		// SystemClock.elapsedRealtime() + OMC_TIMEOUT_30MINS, pi);
		
		actionIcon.setOnClickListener(new ImageView.OnClickListener() {
			
			public void onClick(View v)
			{
				timeOut(context);
				// if (isSupervisorVerified)
				// {
				// if (_mobileAttendance != null)
				// {
				// Log.i(TAG, "--- Logging out and Synching the DB----");
				// Log.i(TAG, "--- _mobileAttendance.getImeiNumber()----"
				// + _mobileAttendance.getImeiNumber());
				// Log.i(TAG, "--- _mobileAttendance.getUserName()----"
				// + _mobileAttendance.getUserName());
				//
				// try
				// {
				// MobileAttendanceBusinessUtils.syncLocalDb(context,
				// _mobileAttendance.getImeiNumber(),
				// _mobileAttendance.getUserName(),
				// _mobileAttendance.getLatLong());
				// cancelNotification();
				// cancelSupervisorLoggedInNotification("OMC Supervisor Logged out");
				// omcState = OMC_OPEN;
				// Log.i(TAG, "--------> OMC STATE ------> "
				// + omcState);
				//
				// } catch (Exception e)
				// {
				// Log.e(TAG, "---------------->" + e.getMessage());
				// e.printStackTrace();
				// errorRouter("Error : Please check the network connectivity");
				// }
				// }
				// }
				//
				// if (dbAdaptor != null)
				// {
				// Log.i(TAG, "--- Adaptor is not Null ----");
				// if (isSupervisorVerified)
				// {
				// Log.i(TAG,
				// "--- Supervisor is verfied- about to clean Local Auth table--");
				//
				// dbAdaptor.open();
				// dbAdaptor.cleanLocalAuth();
				// dbAdaptor.close();
				//
				// Log.i(TAG, "--- Closing db----");
				// }
				//
				// dbAdaptor.close();
				// }
				// isSupervisorVerified = false;
				// finish();
				
			}
		});
		
		// isSupervisorVerified = false;
		// _mobileAttendance = null;
	}
	
	/**
	 * Execute on Timeout
	 * 
	 * @param context
	 */
	private void timeOut(Context context)
	{
		if (isSupervisorVerified)
		{
			if (_mobileAttendance != null)
			{
				Log.i(TAG, "--- Logging out and Synching the DB----");
				Log.i(TAG, "--- _mobileAttendance.getImeiNumber()----"
				        + _mobileAttendance.getImeiNumber());
				Log.i(TAG, "--- _mobileAttendance.getUserName()----"
				        + _mobileAttendance.getUserName());
				
				try
				{
					MobileAttendanceBusinessUtils.syncLocalDb(context,
					        _mobileAttendance.getImeiNumber(),
					        _mobileAttendance.getUserName(),
					        _mobileAttendance.getLatLong());
					cancelNotification();
					cancelSupervisorLoggedInNotification("OMC Supervisor Logged out");
					omcState = OMC_OPEN;
					Log.i(TAG, "--------> OMC STATE ------> " + omcState);
					
				} catch (Exception e)
				{
					Log.e(TAG, "---------------->" + e.getMessage());
					e.printStackTrace();
					errorRouter("Error : Please check the network connectivity");
				}
			}
		}
		
		if (dbAdaptor != null)
		{
			Log.i(TAG, "--- Adaptor is not Null ----");
			if (isSupervisorVerified)
			{
				Log.i(TAG,
				        "--- Supervisor is verfied- about to clean Local Auth table--");
				
				dbAdaptor.open();
				dbAdaptor.cleanLocalAuth();
				dbAdaptor.close();
				
				Log.i(TAG, "--- Closing db----");
			}
			
			dbAdaptor.close();
		}
		isSupervisorVerified = false;
		finish();
	}
	
	private void errorRouter(String errMsg)
	{
		Log.i(TAG, "--- Logging out --");
		
		final Context context = this;
		// 1
		Toast.makeText(this, errMsg, Toast.LENGTH_LONG).show();
		// 2
		int _okId = getResources().getIdentifier(BROKEN, "drawable",
		        getPackageName());
		actionIcon.setImageResource(_okId);
		
		actionIcon.setOnClickListener(new ImageView.OnClickListener() {
			
			public void onClick(View v)
			{
				if (dbAdaptor != null)
				{
					Log.i(TAG, "--- Adaptor is not Null ----");
					if (isSupervisorVerified)
					{
						Log.i(TAG,
						        "--- Supervisor is verfied- about to clean Local Auth table--");
						
						dbAdaptor.open();
						dbAdaptor.cleanLocalAuth();
						dbAdaptor.close();
						
						Log.i(TAG, "--- Closing db----");
					}
					dbAdaptor.close();
				}
				finish();
			}
		});
	}
	
	/**
	 * 
	 */
	private void sendUpdateNotification()
	{
		// dj start
		isNotified = true;
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
		int icon = R.drawable.notify;
		CharSequence tickerText = "Synch OMC";
		long when = System.currentTimeMillis();
		
		Notification notification = new Notification(icon, tickerText, when);
		notification.defaults = Notification.DEFAULT_SOUND;
		
		Context kontext = getApplicationContext();
		CharSequence contentTitle = "My notification";
		CharSequence contentText = "OMC needs Synch";
		Intent notificationIntent = new Intent(this, HeadlessClient.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
		        notificationIntent, 0);
		
		notification.setLatestEventInfo(kontext, contentTitle, contentText,
		        contentIntent);
		
		notificationId = (int) Math.random();
		
		mNotificationManager.notify(notificationId, notification);
		// dj end
	}
	
	/**
	 * 
	 */
	private void sendSupervisorLoggedInNotification()
	{
		// dj start
		isNotified = true;
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
		int icon = R.drawable.loggedin;
		CharSequence tickerText = "OMC Supervisor authenticated";
		long when = System.currentTimeMillis();
		
		Notification notification = new Notification(icon, tickerText, when);
		notification.defaults = Notification.DEFAULT_SOUND;
		
		Context kontext = getApplicationContext();
		CharSequence contentTitle = "My notification";
		CharSequence contentText = "OMC Supervisor authenticated";
		Intent notificationIntent = new Intent(this, HeadlessClient.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
		        notificationIntent, 0);
		
		notification.setLatestEventInfo(kontext, contentTitle, contentText,
		        contentIntent);
		
		notificationId = (int) Math.random();
		
		mNotificationManager.notify(notificationId, notification);
		// dj end
	}
	
	/**
	 * 
	 */
	private void cancelSupervisorLoggedInNotification(String tickerMessage)
	{
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
		int icon = R.drawable.loggedout;
		CharSequence tickerText = tickerMessage;
		long when = System.currentTimeMillis();
		
		Notification notification = new Notification(icon, tickerText, when);
		notification.defaults = Notification.DEFAULT_SOUND;
		
		Context kontext = getApplicationContext();
		CharSequence contentTitle = "My notifications";
		CharSequence contentText = tickerMessage;
		Intent notificationIntent = new Intent(this, HeadlessClient.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
		        notificationIntent, 0);
		
		notification.setLatestEventInfo(kontext, contentTitle, contentText,
		        contentIntent);
		
		notificationId = (int) Math.random();
		
		mNotificationManager.notify(notificationId, notification);
		
		isNotified = false;
		
	}
	
	/**
	 * 
	 */
	private void cancelNotification()
	{
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
		int icon = R.drawable.notify;
		CharSequence tickerText = "Synch OMC";
		long when = System.currentTimeMillis();
		
		Notification notification = new Notification(icon, tickerText, when);
		notification.defaults = Notification.DEFAULT_SOUND;
		
		Context kontext = getApplicationContext();
		CharSequence contentTitle = "My notification";
		CharSequence contentText = "OMC needs Synch";
		Intent notificationIntent = new Intent(this, HeadlessClient.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
		        notificationIntent, 0);
		
		notification.setLatestEventInfo(kontext, contentTitle, contentText,
		        contentIntent);
		
		notificationId = (int) Math.random();
		
		mNotificationManager.cancel(notificationId);
		
		isNotified = false;
		
	}
	
}
