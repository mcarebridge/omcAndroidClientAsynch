package com.adviteya.mobile.activity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.TimeZone;

import org.apache.http.HttpResponse;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.adviteya.mobile.service.AttendanceDatabaseAdaptor;
import com.adviteya.mobile.util.AppStatus;
import com.adviteya.mobile.util.AttendanceUtil;
import com.adviteya.mobile.util.MobileConstants;
import com.adviteya.mobile.vo.MobileEmplyoeeVO;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class ScanAttendance extends Activity implements MobileConstants
{
	
	private static final String       TAG            = "ScanAttendance";
	private LinearLayout              root;
	// private TextView textEmpDetails, errMsgText;
	private TextView                  errMsgText;
	private TableRow                  row1, row2;
	private Button                    buttonVerify;
	private Button                    buttonAttendanceAction;
	private Button                    buttonKickScan;
	private Button                    buttonOmitAttendance;
	private Button                    buttonLogout;
	private AttendanceDatabaseAdaptor dbAdaptor;
	public static long                empId;
	// @deprecated
	public static long                companyId;
	public static String              empCompanyId;
	public static String              inTime;
	public static String              outTime;
	public static long                timeSheetId;
	public static String              attendanceAction;
	
	private String                    _mobileDbSynch = null;
	private boolean                   isFirstRun     = false;
	
	private static final String       IN_CONT        = "IN - Next Scan";
	private static final String       OUT_CONT       = "OUT - Next Scan";
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		Log.i(TAG, "-------------->onCreate<-------------------");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.scanattendance);
		buttonAttendanceAction = (Button) findViewById(R.id.attendanceAction);
		buttonOmitAttendance = (Button) findViewById(R.id.omitAttendance);
		// buttonAttendanceAction.setVisibility(View.INVISIBLE);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
		
		AppStatus appStatus = AppStatus.getInstance(this);
		if (!appStatus.isOnline())
		{
			Toast.makeText(
			        this,
			        "This application need Data network. Please enable Data connectivity.",
			        Toast.LENGTH_LONG).show();
		}
	}
	
	/**
	 * 
	 * @param view
	 * @throws ParseException
	 */
	public void verifyEmployee(String companyId, String scannedEmpId)
	        throws ParseException
	{
		boolean _noError = false;
		String _errMsgString = "";
		empCompanyId = null;
		attendanceAction = null;
		CharSequence _empId = scannedEmpId;
		CharSequence _pwd = "NOT_NEEDED";
		errMsgText = (TextView) findViewById(R.id.errMsgText);
		
		errMsgText.setBackgroundColor(Color.BLACK);
		errMsgText.setTextColor(Color.RED);
		errMsgText.setTextSize((float) 15.0);
		errMsgText.setVisibility(View.VISIBLE);
		
		if (_empId != null)
		{
			if (!(_empId.length() == 0))
			{
				_noError = true;
			} else
			{
				_noError = false;
				_errMsgString += "emp Id is blank" + "\n";
			}
		} else
		{
			_errMsgString += "emp Id is blank" + "\n";
		}
		
		if (_pwd != null)
		{
			if (!(_pwd.length() == 0))
			{
				_noError = true;
			} else
			{
				_noError = false;
				_errMsgString += "emp pin is blank" + "\n";
			}
		} else
		{
			_noError = false;
			_errMsgString += "emp pin is blank";
		}
		
		if (_noError)
		{
			
			// Verify the Employee against the local db
			AttendanceDatabaseAdaptor dbAdaptor = new AttendanceDatabaseAdaptor(
			        this);
			dbAdaptor.open();
			// @todo : need to populate company id
			MobileEmplyoeeVO mobileEmp = dbAdaptor.authenticateEmplyoee(
			        _empId.toString(), _pwd.toString());
			
			// row1 = (TableRow) findViewById(R.id.tableRow1);
			// row2 = (TableRow) findViewById(R.id.tableRow2);
			
			if (mobileEmp != null)
			{
				
				empId = mobileEmp.getEmpId();
				empCompanyId = mobileEmp.getEmpCompanyId();
				inTime = mobileEmp.getInTime();
				outTime = mobileEmp.getOutTime();
				timeSheetId = mobileEmp.getTimeSheetId().longValue();
				
				errMsgText.setClickable(false);
				
				errMsgText.setText(mobileEmp.getEmpName() + " verified");
				errMsgText.setTextColor(Color.GREEN);
				errMsgText.setEnabled(false);
				errMsgText.setGravity(0x01);
				errMsgText.setTextSize(20f);
				
				buttonAttendanceAction.setVisibility(View.VISIBLE);
				buttonAttendanceAction.setClickable(true);
				buttonOmitAttendance.setClickable(true);
				buttonOmitAttendance.setTextColor(Color.RED);
				
				buttonLogout = (Button) findViewById(R.id.logout);
				buttonLogout.setClickable(true);
				buttonLogout.setTextColor(Color.parseColor("#3ADF00"));
				
				if (mobileEmp.getInTime().equals("-"))
				{
					buttonAttendanceAction.setText(IN_CONT);
					buttonAttendanceAction.setBackgroundColor(Color.GREEN);
					buttonAttendanceAction.setTextColor(Color.BLACK);
					attendanceAction = "IN";
				} else if (!mobileEmp.getInTime().equals("-"))
				// i.e If IN-TIME is a DATE
				{
					if (mobileEmp.getOutTime().equals("-"))
					{
						buttonAttendanceAction.setText(OUT_CONT);
						buttonAttendanceAction.setBackgroundColor(Color.YELLOW);
						buttonAttendanceAction.setTextColor(Color.BLACK);
						attendanceAction = "OUT";
					} else if (!mobileEmp.getOutTime().equals("-"))
					{
						// i.e If OUT-TIME is a DATE
						
						SimpleDateFormat sdf = new SimpleDateFormat(
						        "MM/dd/yyyy kk:mm:ss");
						Date _inDate = sdf.parse(mobileEmp.getInTime());
						Date _outDate = sdf.parse(mobileEmp.getOutTime());
						
						Log.i(TAG, "-- IN DATE --" + mobileEmp.getInTime());
						Log.i(TAG, "-- OUT DATE --" + mobileEmp.getOutTime());
						
						buttonAttendanceAction.setTextColor(Color.BLACK);
						if (_inDate.after(_outDate))
						{
							buttonAttendanceAction
							        .setBackgroundColor(Color.YELLOW);
							buttonAttendanceAction.setText(OUT_CONT);
							attendanceAction = "OUT";
						} else if (_outDate.after(_inDate))
						{
							buttonAttendanceAction
							        .setBackgroundColor(Color.GREEN);
							buttonAttendanceAction.setText(IN_CONT);
							attendanceAction = "IN";
						}
					}
				}
				
			} else
			{
				errMsgText.setText(_empId + " Not Verified");
				errMsgText.setTextColor(Color.RED);
				errMsgText.setTextSize((float) 20.0);
				buttonOmitAttendance.setClickable(true);
				buttonAttendanceAction.setText("IN/OUT");
				buttonAttendanceAction.setBackgroundColor(Color.GRAY);
				buttonAttendanceAction.setClickable(false);
			}
			dbAdaptor.close();
		} else
		{
			buttonAttendanceAction.setText("IN/OUT");
			buttonAttendanceAction.setBackgroundColor(Color.GRAY);
			buttonAttendanceAction.setClickable(false);
			buttonOmitAttendance.setClickable(false);
			errMsgText.setBackgroundColor(Color.BLACK);
			errMsgText.setText(_errMsgString);
			errMsgText.setTextColor(Color.RED);
			errMsgText.setTextSize((float) 20.0);
		}
		
	}
	
	/**
	 * 
	 * @param view
	 */
	public void markAttendance(View view)
	{
		
		// service and add a record -
		
		// AttendanceService service = new AttendanceService();
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy kk:mm:ss");
		TimeZone _t = TimeZone.getDefault();
		Log.i(TAG, _t.getDisplayName());
		Calendar _c = Calendar.getInstance(_t);
		
		if (attendanceAction.equalsIgnoreCase("IN"))
		{
			
			inTime = sdf.format(_c.getTime());
			
		} else if (attendanceAction.equalsIgnoreCase("OUT"))
		{
			outTime = sdf.format(_c.getTime());
		}
		
		if (MobileAttendanceLogin.empAssignmentsAvailable)
		{
			updateRecord(timeSheetId, inTime, outTime);
		} else
		{
			updateRecordForNoAssignment(timeSheetId, inTime, outTime,
			        SynchronizeActivity.selectedLocationKey,
			        SynchronizeActivity.selectedShiftKey);
		}
		
		if (errMsgText != null)
		{
			errMsgText.setTextColor(Color.WHITE);
			errMsgText.setText("Data inserted. Pl. scan next record.");
		}
		
		buttonAttendanceAction = (Button) findViewById(R.id.attendanceAction);
		buttonAttendanceAction.setClickable(false);
		
		buttonLogout = (Button) findViewById(R.id.logout);
		buttonLogout.setClickable(true);
		
		// Go back and Scan
		Log.i(TAG, "-- About to call ScanAttendance --");
		IntentIntegrator _iInteg = new IntentIntegrator(ScanAttendance.this);
		_iInteg.initiateScan();
		
	}
	
	/**
	 * <pre>
	 * This method does -
	 * 1. Mark the last attendance
	 * 2. Synch and Logout
	 * </pre>
	 * 
	 * @param view
	 */
	public void logout(View view)
	{
		if (attendanceAction != null)
		{
			markAttendance(view);
		}
		
		ScanAttendance.empId = 0;
		ScanAttendance.companyId = 0;
		MobileAttendanceLogin.companyId = 0;
		MobileAttendanceLogin.longitude = 0D;
		MobileAttendanceLogin.latitue = 0D;
		MobileAttendanceLogin.accuracy = 0D;
		MobileAttendanceLogin.userVerificationMsg = "";
		
		/**
		 * Invoke server using HTTPClient to get the latest team snapshot For
		 * test add test records
		 */
		errMsgText = (TextView) findViewById(R.id.errMsgText);
		try
		{
			AttendanceUtil _attendanceUtil = new AttendanceUtil();
			_mobileDbSynch = _attendanceUtil.getAllMobileSynch(this);
			errMsgText.setTextColor(Color.WHITE);
			errMsgText.setText("Sending " + AttendanceUtil.getSentRecs()
			        + " Records");
			Toast.makeText(this,
			        "Sending " + AttendanceUtil.getSentRecs() + " Records",
			        Toast.LENGTH_LONG).show();
			
		} catch (Exception e1)
		{
			_mobileDbSynch = FIRST_RUN;
			isFirstRun = true;
			Log.i(TAG, e1.getMessage(), e1);
			// @todo : need to send this message to scr
		}
		
		HttpResponse response = null;
		
		// Get Timesheet snapshot from SqlLite
		String synchServerDataString = null;
		try
		{
			synchServerDataString = AttendanceUtil.synchMobile(
			        MobileAttendanceLogin._empId.toString().trim(),
			        MobileAttendanceLogin._pwd.toString().trim(),
			        MobileAttendanceLogin._latlang.toString().trim(),
			        _mobileDbSynch.toString().trim());
			
		} catch (Exception e)
		{
			// @todo : need to send this message to scr
			Log.i(TAG, e.getMessage(), e);
		}
		
		Intent mainIntent = new Intent(view.getContext(),
		        com.adviteya.mobile.activity.MobileAttendanceLogin.class);
		startActivity(mainIntent);
		finish();
		
	}
	
	private void updateRecord(long timeSheetId, String inTime, String outTime)
	{
		dbAdaptor = new AttendanceDatabaseAdaptor(this);
		dbAdaptor.open();
		dbAdaptor.updateMobileSync(timeSheetId, inTime, outTime, empId);
		dbAdaptor.close();
	}
	
	/**
	 * 
	 * @param view
	 */
	public void kickScan(View view)
	{
		Log.i(TAG, "-- kickScan --");
		buttonKickScan = (Button) findViewById(R.id.kickScan);
		buttonKickScan.setClickable(false);
		buttonKickScan.setTextColor(Color.parseColor("#3ADF00"));
		buttonLogout = (Button) findViewById(R.id.logout);
		buttonLogout.setClickable(true);
		buttonLogout.setTextColor(Color.parseColor("#3ADF00"));
		buttonLogout.setText("Last Attendance Record");
		
		Toast.makeText(this, "Ready to scan attendance", Toast.LENGTH_LONG)
		        .show();
		IntentIntegrator _iInteg = new IntentIntegrator(ScanAttendance.this);
		_iInteg.initiateScan();
	}
	
	/**
	 * 
	 * @param view
	 */
	public void omitAttendance(View view)
	{
		
		Toast.makeText(this, "User omitted this attendance record",
		        Toast.LENGTH_LONG).show();
		IntentIntegrator _iInteg = new IntentIntegrator(ScanAttendance.this);
		_iInteg.initiateScan();
	}
	
	/**
	 * 
	 * 
	 */
	protected void onActivityResult(int requestCode, int resultCode,
	        Intent intent)
	{
		Log.i(TAG, "-- onActivityResult --");
		IntentResult scanResult = IntentIntegrator.parseActivityResult(
		        requestCode, resultCode, intent);
		if (scanResult != null)
		{
			Log.i(TAG, "-- onActivityResult.STEP-1");
			String _scannedContents = scanResult.getContents();
			if (_scannedContents != null)
			{
				Log.i(TAG, "-- onActivityResult.STEP-2");
				StringTokenizer _st = new StringTokenizer(_scannedContents, "@");
				String _companyId = (String) _st.nextElement();
				String _scannedEmpId = (String) _st.nextElement();
				
				try
				{
					verifyEmployee(_companyId, _scannedEmpId);
				} catch (ParseException e)
				{
					Log.i(TAG, "-- onActivityResult.STEP-2");
					e.printStackTrace();
				}
			} else
			{
				Log.i(TAG, "-- onActivityResult.STEP-3");
				buttonAttendanceAction.setText("IN/OUT");
				buttonAttendanceAction.setBackgroundColor(Color.GRAY);
				buttonAttendanceAction.setClickable(false);
				Toast.makeText(this, " -- Unable to parse contents --",
				        Toast.LENGTH_LONG).show();
				buttonKickScan.setClickable(true);
				buttonKickScan.setTextColor(Color.parseColor("#3ADF00"));
			}
			
		} else
		{
			Log.i(TAG, "-- onActivityResult.STEP-4");
			buttonAttendanceAction.setText("IN/OUT");
			buttonAttendanceAction.setBackgroundColor(Color.GRAY);
			buttonAttendanceAction.setClickable(false);
			Toast.makeText(this, " -- Unable to parse contents --",
			        Toast.LENGTH_LONG).show();
			buttonKickScan.setClickable(true);
			buttonKickScan.setTextColor(Color.parseColor("#3ADF00"));
			
		}
		// else continue with any other code you need in the method
	}
	
	/**
	 * 
	 */
	private void updateAttendance()
	{
		// service and add a record -
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy kk:mm");
		
		if (attendanceAction != null)
		{
			
			if (attendanceAction.equalsIgnoreCase("IN"))
			{
				
				inTime = sdf.format(new Date());
				
			} else if (attendanceAction.equalsIgnoreCase("OUT"))
			{
				outTime = sdf.format(new Date());
			}
			
			updateRecord(timeSheetId, inTime, outTime);
			
		}
	}
	
	/**
	 * 
	 * @param timeSheetId
	 * @param inTime
	 * @param outTime
	 * @param locationKey
	 * @param shiftKey
	 */
	private void updateRecordForNoAssignment(long timeSheetId, String inTime,
	        String outTime, String locationKey, String shiftKey)
	{
		dbAdaptor = new AttendanceDatabaseAdaptor(this);
		dbAdaptor.open();
		dbAdaptor.updateMobileSyncForNoAssignment(timeSheetId, inTime, outTime,
		        empId, locationKey, shiftKey);
		dbAdaptor.close();
	}
	
	/**
	 * If back button is pressed log out of system
	 */
	@Override
	public void onBackPressed()
	{
		MobileAttendanceLogin._empId = null;
		MobileAttendanceLogin._pwd = null;
		MobileAttendanceLogin.userVerificationMsg = null;
		Intent mainIntent = new Intent(this,
		        com.adviteya.mobile.activity.MobileAttendanceLogin.class);
		startActivity(mainIntent);
		finish();
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
	}
	
	@Override
	protected void onResume()
	{
		Log.i(TAG, "-- Invoked onResume -- ");
		super.onResume();
	}
}
