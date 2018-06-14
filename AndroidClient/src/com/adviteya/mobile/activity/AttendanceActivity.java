package com.adviteya.mobile.activity;

import java.text.SimpleDateFormat;
import java.util.Date;

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
import com.adviteya.mobile.util.AttendanceUtil;
import com.adviteya.mobile.util.MobileConstants;
import com.adviteya.mobile.vo.MobileEmplyoeeVO;

public class AttendanceActivity extends Activity implements MobileConstants
{
	
	private static final String       TAG            = "AttendanceActivity";
	private LinearLayout              root;
	private TextView                  textEmpId;
	private TextView                  pwdEmpKey;
	// private TextView textEmpDetails, errMsgText;
	private TextView                  errMsgText;
	private TableRow                  row1, row2;
	private Button                    buttonVerify;
	private Button                    buttonAttendanceAction;
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
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);

		setContentView(R.layout.attendanceregister);
		
		buttonAttendanceAction = (Button) findViewById(R.id.attendanceAction);
		buttonAttendanceAction.setVisibility(View.GONE);
		
	}
	
	/**
	 * 
	 * @param view
	 */
	public void verifyEmployee(View view)
	{
		boolean _noError = false;
		String _errMsgString = "";
		empCompanyId = null;
		attendanceAction = null;
		
		textEmpId = (TextView) findViewById(R.id.textEmpId);
		CharSequence _empId = textEmpId.getText();
		
		pwdEmpKey = (TextView) findViewById(R.id.pwdEmpKey);
		CharSequence _pwd = pwdEmpKey.getText();
		
		// textEmpDetails = (TextView) findViewById(R.id.textEmpDetails);
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
				// _noError = false;
				// dev env fix
				_noError = true;
				_errMsgString += "emp pin is blank" + "\n";
			}
		} else
		{
			// _noError = false;
			// dev env fix
			_noError = true;
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
			
			row1 = (TableRow) findViewById(R.id.tableRow1);
			row2 = (TableRow) findViewById(R.id.tableRow2);
			
			buttonVerify = (Button) findViewById(R.id.buttonVerify);
			
			if (mobileEmp != null)
			{
				
				empId = mobileEmp.getEmpId();
				empCompanyId = mobileEmp.getEmpCompanyId();
				inTime = mobileEmp.getInTime();
				outTime = mobileEmp.getOutTime();
				timeSheetId = mobileEmp.getTimeSheetId().longValue();
				
				textEmpId.setClickable(false);
				textEmpId.setBackgroundColor(Color.GRAY);
				textEmpId.setEnabled(false);
				
				pwdEmpKey.setClickable(false);
				pwdEmpKey.setBackgroundColor(Color.GRAY);
				pwdEmpKey.setEnabled(false);
				
				errMsgText.setClickable(false);
				
				buttonVerify.setClickable(false);
				
				errMsgText.setText(mobileEmp.getEmpCompanyId() + " verified");
				errMsgText.setTextColor(Color.GREEN);
				errMsgText.setEnabled(false);
				errMsgText.setGravity(0x01);
				
				buttonAttendanceAction.setVisibility(View.VISIBLE);
				
				if (mobileEmp.getInTime().equals("-"))
				{
					buttonAttendanceAction.setText("IN");
					buttonAttendanceAction.setBackgroundColor(Color.GREEN);
					attendanceAction = "IN";
				} else if (!mobileEmp.getInTime().equals("-"))
				{
					if (mobileEmp.getOutTime().equals("-"))
					{
						buttonAttendanceAction.setText("OUT");
						buttonAttendanceAction.setBackgroundColor(Color.RED);
						attendanceAction = "OUT";
					}
				}
				
			} else
			{
				errMsgText.setText(_empId + " Not Verified");
				errMsgText.setTextColor(Color.RED);
				textEmpId.setText("");
				pwdEmpKey.setText("");
			}
			dbAdaptor.close();
		} else
		{
			errMsgText.setBackgroundColor(Color.BLACK);
			errMsgText.setText(_errMsgString);
			errMsgText.setTextColor(Color.RED);
			errMsgText.setTextSize((float) 15.0);
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
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy kk:mm");
		
		if (attendanceAction.equalsIgnoreCase("IN"))
		{
			
			inTime = sdf.format(new Date());
			
		} else if (attendanceAction.equalsIgnoreCase("OUT"))
		{
			outTime = sdf.format(new Date());
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
		
		textEmpId = (TextView) findViewById(R.id.textEmpId);
		textEmpId.setText("");
		textEmpId.setEnabled(true);
		textEmpId.setBackgroundColor(Color.WHITE);
		pwdEmpKey = (TextView) findViewById(R.id.pwdEmpKey);
		pwdEmpKey.setText("");
		pwdEmpKey.setEnabled(true);
		pwdEmpKey.setBackgroundColor(Color.WHITE);
		
		// textEmpDetails = (TextView) findViewById(R.id.textEmpDetails);
		// textEmpDetails.setText("");
		// textEmpDetails.setTextColor(Color.BLACK);
		errMsgText.setTextColor(Color.WHITE);
		errMsgText.setText("Data inserted. Pl. enter next record.");
		
		row1 = (TableRow) findViewById(R.id.tableRow1);
		row2 = (TableRow) findViewById(R.id.tableRow2);
		
		row1.setVisibility(View.VISIBLE);
		row2.setVisibility(View.VISIBLE);
		
		buttonVerify = (Button) findViewById(R.id.buttonVerify);
		buttonVerify.setVisibility(View.VISIBLE);
		buttonVerify.setClickable(true);
		
		buttonAttendanceAction = (Button) findViewById(R.id.attendanceAction);
		buttonAttendanceAction.setVisibility(View.GONE);
		
	}
	
	/**
	 * 
	 * @param view
	 */
	public void logout(View view)
	{
		AttendanceActivity.empId = 0;
		AttendanceActivity.companyId = 0;
		MobileAttendanceLogin.companyId = 0;
		MobileAttendanceLogin.longitude = 0D;
		MobileAttendanceLogin.latitue = 0D;
		MobileAttendanceLogin.accuracy = 0D;
		MobileAttendanceLogin.userVerificationMsg = "";
		
		/**
		 * Invoke server using HTTPClient to get the latest team snapshot For
		 * test add test records
		 */
		
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
		startActivityForResult(mainIntent, 0);
		setResult(RESULT_OK, mainIntent);
		finish();
		
	}
	
	/**
	 * 
	 * @param timeSheetId
	 * @param inTime
	 * @param outTime
	 */
	private void updateRecord(long timeSheetId, String inTime, String outTime)
	{
		dbAdaptor = new AttendanceDatabaseAdaptor(this);
		dbAdaptor.open();
		dbAdaptor.updateMobileSync(timeSheetId, inTime, outTime, empId);
		dbAdaptor.close();
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
	
	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
	}
	
}
