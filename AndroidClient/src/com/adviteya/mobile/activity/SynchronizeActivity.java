/**
 * 
 */
package com.adviteya.mobile.activity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.Intent;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.adviteya.mobile.service.AttendanceDatabaseAdaptor;
import com.adviteya.mobile.util.AppStatus;
import com.adviteya.mobile.util.AttendanceUtil;
import com.adviteya.mobile.util.MobileConstants;
import com.adviteya.mobile.vo.MobileEmplyoeeVO;

/**
 * @author deejay
 * 
 */
public class SynchronizeActivity extends Activity implements OnClickListener,
        MobileConstants
{
	
	private static final String TAG                 = "SynchronizeActivity";
	private TextView            syncmsgbox;
	private Button              synchronize, proceedforatndce;
	private String              _mobileDbSynch      = null;
	private boolean             isFirstRun          = false;
	private int                 receivedRecs        = 0;
	private Spinner             locationsList       = null;
	private Spinner             shiftList           = null;
	public static String        selectedLocationKey = null;
	public static String        selectedShiftKey    = null;
	private int                 q                   = 0;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		Log.i(TAG, "-- onCreate --");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.synchronize);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
		
		syncmsgbox = (TextView) findViewById(R.id.syncmsgbox);
		locationsList = (Spinner) findViewById(R.id.locationlist);
		locationsList.setVisibility(View.INVISIBLE);
		shiftList = (Spinner) findViewById(R.id.shiftlist);
		shiftList.setVisibility(View.INVISIBLE);
		
		AppStatus appStatus = AppStatus.getInstance(this);
		if (!appStatus.isOnline())
		{
			Toast.makeText(
			        this,
			        "This application need Data network. Please enable Data connectivity.",
			        Toast.LENGTH_LONG).show();
		}
		
		if (!MobileAttendanceLogin.empAssignmentsAvailable)
		{
			locationsList.setVisibility(View.VISIBLE);
			shiftList.setVisibility(View.VISIBLE);
		}
		
		if (MobileAttendanceLogin.userVerificationMsg
		        .equalsIgnoreCase(MOBILE_SUPERVISOR_AUTHENTICATION_SUCCESS))
		{
			syncmsgbox.setText(ADMIN_USER_VERIFICATION_SUCCESS);
		} else if (MobileAttendanceLogin.userVerificationMsg
		        .equalsIgnoreCase(MOBILE_SUPERVISOR_AUTHENTICATION_FAILED))
		{
			syncmsgbox.setText(ADMIN_USER_VERIFICATION_FAILED);
		}
		
		// syncmsgbox.setText(MobileAttendanceLogin.userVerificationMsg);
		
		proceedforatndce = (Button) findViewById(R.id.proceedforatndce);
		proceedforatndce.setVisibility(View.INVISIBLE);
	}
	
	/**
	 * <pre>
	 * This method is starts the Synchronisation logic 
	 * 1. Invoke web and send the client version 
	 * 2. Send the Unsynched records to the server 
	 * 3. Receive the latest server copy for that location and shift 
	 * 4. Record error
	 * </pre>
	 * 
	 * @param view
	 */
	public void syncLocal(View view)
	{
		Log.i(TAG, "-- synchLocal --");
		Log.i(TAG, "EmpId Value : "
		        + MobileAttendanceLogin._empId.toString().trim());
		
		synchronize = (Button) findViewById(R.id.synchronize);
		synchronize.setEnabled(false);
		isFirstRun = false;
		
		/**
		 * Invoke server using HTTPClient to get the latest team snapshot For
		 * test add test records
		 */
		
		try
		{
			AttendanceUtil _attendanceUtil = new AttendanceUtil();
			_mobileDbSynch = _attendanceUtil.getAllMobileSynch(this);
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
		/**
		 * The data received from the server contains - Location and Shift meta
		 * data and Timesheet. It is in the following format
		 * DATASTRING=(LOCATIONANDSHIFT=XXXXXXXXX)(TIMESHEET=XXXXXXXXX)
		 */
		
		// Insert Location and Shift
		
		// Insert Timesheet String
		String _timeSheetString = AttendanceUtil
		        .extractTimeSheet(synchServerDataString);
		insertSynchedRow(_timeSheetString);
		String _locationShiftString = AttendanceUtil
		        .extractLocationAndShift(synchServerDataString);
		
		/**
		 * Need to execute Locations and Shift data logic if a Company doesn't
		 * provide planned assignments
		 */
		if (!MobileAttendanceLogin.empAssignmentsAvailable)
		{
			insertLocationShiftData(_locationShiftString);
			syncmsgbox.setText("Sent " + AttendanceUtil.getSentRecs()
			        + " Records" + "\t" + "Received " + receivedRecs
			        + " Records");
			showLocations();
		}
		
		proceedforatndce.setVisibility(View.VISIBLE);
	}
	
	/**
	 * Pick all the unique Locations and Show to User
	 */
	private void showLocations()
	{
		AttendanceDatabaseAdaptor dbAdaptor = new AttendanceDatabaseAdaptor(
		        this);
		dbAdaptor.open();
		Map<String, String> locations = dbAdaptor.getAllTheLocations();
		
		Collection<String> locationValues = locations.values();
		Collection<String> locationKeys = locations.keySet();
		
		final ArrayList<String> _al = new ArrayList<String>(locationValues);
		final ArrayList<String> _keyList = new ArrayList<String>(locationKeys);
		
		Spinner locationsList = (Spinner) findViewById(R.id.locationlist);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
		        android.R.layout.simple_spinner_item, _al);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		locationsList.setAdapter(adapter);
		
		locationsList.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
			        int position, long id)
			{
				String _selectedLocationKey = (String) _keyList.get(position);
				selectedLocationKey = _selectedLocationKey;
				showShiftsForSelectedLocation(_selectedLocationKey);
				
			}
			
			@Override
			public void onNothingSelected(AdapterView<?> arg0)
			{
				
			}
		});
		
		dbAdaptor.close();
	}
	
	/**
	 * 
	 * @param locationKey
	 */
	private void showShiftsForSelectedLocation(String locationKey)
	{
		AttendanceDatabaseAdaptor dbAdaptor = new AttendanceDatabaseAdaptor(
		        this);
		dbAdaptor.open();
		Map<String, String> shifts = dbAdaptor
		        .getAllShiftsForALocation(locationKey);
		
		Collection<String> shiftValues = shifts.values();
		Collection<String> shiftKeys = shifts.keySet();
		
		final ArrayList<String> _al = new ArrayList<String>(shiftValues);
		final ArrayList<String> _keyList = new ArrayList<String>(shiftKeys);
		
		Spinner shiftList = (Spinner) findViewById(R.id.shiftlist);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
		        android.R.layout.simple_spinner_item, _al);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		shiftList.setAdapter(adapter);
		
		shiftList.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
			        int position, long id)
			{
				String _selectedKey = (String) _keyList.get(position);
				selectedShiftKey = _selectedKey;
			}
			
			@Override
			public void onNothingSelected(AdapterView<?> arg0)
			{
				
			}
		});
		
		dbAdaptor.close();
	}
	
	/**
	 * 
	 * @param client
	 * @param httppost
	 * @param response
	 * @throws IOException
	 */
	private String readHTTPResponse(HttpClient client, HttpPost httppost,
	        HttpResponse response) throws IOException
	{
		// Get hold of the response entity
		HttpEntity entity = response.getEntity();
		String responseString = null;
		
		// If the response does not enclose an entity, there is no need
		// to worry about connection release
		if (entity != null)
		{
			
			InputStream instream = null;
			
			try
			{
				instream = entity.getContent();
				
				BufferedReader reader = new BufferedReader(
				        new InputStreamReader(instream));
				// do something useful with the response
				responseString = reader.readLine();
			} catch (IOException ex)
			{
				
				// In case of an IOException the connection will be released
				// back to the connection manager automatically
				throw ex;
				
			} catch (RuntimeException ex)
			{
				
				// In case of an unexpected exception you may want to abort
				// the HTTP request in order to shut down the underlying
				// connection and release it back to the connection manager.
				httppost.abort();
				throw ex;
				
			} finally
			{
				
				// Closing the input stream will trigger connection release
				instream.close();
				// When HttpClient instance is no longer needed,
				// shut down the connection manager to ensure
				// immediate deallocation of all system resources
				client.getConnectionManager().shutdown();
			}
		}
		
		return responseString;
	}
	
	/**
	 * Parse the delimited data and insert in to mobile db
	 * 
	 * @param dbAdaptor
	 * @param synchServerDataString
	 */
	private void insertSynchedRow(String synchServerDataString)
	{
		AttendanceDatabaseAdaptor dbAdaptor = new AttendanceDatabaseAdaptor(
		        this);
		dbAdaptor.open();
		if (!isFirstRun)
		{
			dbAdaptor.cleanMobileSync();
		}
		
		StringTokenizer st = new StringTokenizer(synchServerDataString, "$");
		// header
		st.nextElement();
		
		// for testing only
		int i = 0;
		receivedRecs = 0;
		
		while (st.hasMoreElements())
		{
			String _line = (String) st.nextElement();
			
			StringTokenizer _st = new StringTokenizer(_line, ",");
			
			while (_st.hasMoreElements())
			{
				String timeSheetDate = (String) _st.nextElement();
				if (timeSheetDate == null)
					timeSheetDate = "";
				
				String _tSheetId = (String) _st.nextElement();
				
				Long timesheetId = Long
				        .parseLong(_tSheetId.trim().equals("-") ? "0"
				                : _tSheetId);
				if (timesheetId == null)
					timesheetId = new Long(0);
				
				String _asgId = (String) _st.nextElement();
				
				Long assignmentId = Long
				        .parseLong(_asgId.trim().equals("-") ? "0" : _asgId);
				if (assignmentId == null)
					assignmentId = new Long(0);
				
				String _shftId = (String) _st.nextElement();
				Long shiftId = Long.parseLong(_shftId.trim().equals("-") ? "0"
				        : _shftId);
				if (shiftId == null)
					shiftId = new Long(0);
				
				String location = (String) _st.nextElement();
				if (location == null)
					location = "";
				
				String empId = (String) _st.nextElement();
				if (empId == null)
					empId = "";
				
				String empCompanyId = (String) _st.nextElement();
				if (empCompanyId == null)
					empCompanyId = "";
				
				String empName = (String) _st.nextElement();
				if (empName == null)
					empName = "";
				
				String empPwd = (String) _st.nextElement();
				if (empPwd == null)
					empPwd = "";
				
				String inTime = (String) _st.nextElement();
				if (inTime == null)
					inTime = "";
				
				String outTime = (String) _st.nextElement();
				if (outTime == null)
					outTime = "";
				
				String marker = (String) _st.nextElement();
				if (marker == null)
					marker = "";
				
				Log.i(TAG, " dbAdaptor " + dbAdaptor);
				
				dbAdaptor.insertSynchedRow(timeSheetDate, timesheetId,
				        assignmentId, shiftId, location, empId, empCompanyId,
				        empName, empPwd, inTime, outTime, marker);
				
				i++;
				receivedRecs = i;
			}
		}
		dbAdaptor.close();
		
	}
	
	/**
	 * Extract and insert Location and Shift Data
	 * 
	 * @param locationShiftString
	 */
	private void insertLocationShiftData(String locationShiftString)
	{
		AttendanceDatabaseAdaptor dbAdaptor = new AttendanceDatabaseAdaptor(
		        this);
		dbAdaptor.open();
		if (!isFirstRun)
		{
			dbAdaptor.cleanLocationShiftSync();
		}
		
		StringTokenizer st = new StringTokenizer(locationShiftString, "|");
		// header
		st.nextElement();
		
		// for testing only
		int i = 0;
		receivedRecs = 0;
		
		while (st.hasMoreElements())
		{
			String _line = (String) st.nextElement();
			
			StringTokenizer _st = new StringTokenizer(_line, ",");
			
			while (_st.hasMoreElements())
			{
				String _shiftKey = (String) _st.nextElement();
				Long _shiftKeyId = Long
				        .parseLong(_shiftKey.trim().equals("-") ? "0"
				                : _shiftKey);
				if (_shiftKeyId == null)
					_shiftKeyId = new Long(0);
				
				String _shift = (String) _st.nextElement();
				if (_shift == null)
					_shift = "";
				
				String _locationKey = (String) _st.nextElement();
				Long locationKeyId = Long.parseLong(_locationKey.trim().equals(
				        "-") ? "0" : _locationKey);
				if (locationKeyId == null)
					locationKeyId = new Long(0);
				
				String _location = (String) _st.nextElement();
				if (_location == null)
					_location = "";
				
				String _timeZone = (String) _st.nextElement();
				if (_timeZone == null)
					_timeZone = "";
				
				Log.i(TAG, " dbAdaptor " + dbAdaptor);
				
				dbAdaptor.insertLocationShift(_shiftKeyId, _shift,
				        locationKeyId, _location, _timeZone);
				
				i++;
				receivedRecs = i;
			}
		}
		dbAdaptor.close();
		
	}
	
	/**
	 * Get the all records from Mobile and Send the Data a csv
	 * 
	 * @param dbAdaptor
	 * @return
	 * @throws Exception
	 */
	private String getAllMobileSynch() throws Exception
	{
		AttendanceDatabaseAdaptor dbAdaptor = new AttendanceDatabaseAdaptor(
		        this);
		dbAdaptor.open();
		List<MobileEmplyoeeVO> mobileEmpList = dbAdaptor.getAllMobileSynch();
		dbAdaptor.close();
		
		StringBuffer _sb = new StringBuffer();
		_sb.append("timeSheetId,assignmentId,inTime,outTime,$");
		
		for (Iterator iterator = mobileEmpList.iterator(); iterator.hasNext();)
		{
			
			MobileEmplyoeeVO mobileEmplyoeeVO = (MobileEmplyoeeVO) iterator
			        .next();
			// _sb.append(mobileEmplyoeeVO.getKeyRowId());
			// _sb.append(",");
			_sb.append(mobileEmplyoeeVO.getTimeSheetId());
			_sb.append(",");
			_sb.append(mobileEmplyoeeVO.getAssignmentId());
			_sb.append(",");
			// _sb.append(mobileEmplyoeeVO.getTimeSheetDate());
			// _sb.append(",");
			// _sb.append(mobileEmplyoeeVO.getShiftId());
			// _sb.append(",");
			// _sb.append(mobileEmplyoeeVO.getLocation());
			// _sb.append(",");
			// _sb.append(mobileEmplyoeeVO.getEmpId());
			// _sb.append(",");
			// _sb.append(mobileEmplyoeeVO.getEmpCompanyId());
			// _sb.append(",");
			// _sb.append(mobileEmplyoeeVO.getEmpName());
			// _sb.append(",");
			// _sb.append(mobileEmplyoeeVO.getPassKey());
			// _sb.append(",");
			_sb.append(mobileEmplyoeeVO.getInTime());
			_sb.append(",");
			_sb.append(mobileEmplyoeeVO.getOutTime());
			_sb.append(",");
			// _sb.append(mobileEmplyoeeVO.getMarker());
			_sb.append("$");
		}
		
		Log.i("--Data sent to Server --", _sb.toString());
		
		return _sb.toString();
		
	}
	
	/**
	 * 
	 * @param view
	 */
	public void proceedToAttendance(View view)
	{
		
		Log.i(TAG, "-- proceedToAttendance --");
		
		// Enable this for testing in dev env
		// Intent attendIntent = new Intent(view.getContext(),
		// com.adviteya.mobile.activity.AttendanceActivity.class);
		
		// Enable this for actual handset testing
		Intent attendIntent = new Intent(view.getContext(),
		        com.adviteya.mobile.activity.ScanAttendance.class);
		// startActivity(attendIntent);
		startActivityForResult(attendIntent, 0);
	}
	
	@Override
	public void onClick(DialogInterface arg0, int arg1)
	{
		
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
		q++;
		Log.i(TAG, "-- Invoked onResume -- ");
		Log.i(TAG, "-- q = " + q);
		super.onResume();
	}
	
	/**
	 * 
	 */
	public void finsh()
	{
		super.finish();
	}
	
	protected void onActivityResult(int requestCode, int resultCode,
	        Intent intent)
	{
		if (intent != null)
			Log.i(TAG, "-- onActivityResult --" + intent.getType() + "--"
			        + resultCode);
	}
}
