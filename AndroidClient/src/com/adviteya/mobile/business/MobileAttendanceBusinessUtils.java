package com.adviteya.mobile.business;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;

import android.content.Context;
import android.util.Log;

import com.adviteya.mobile.service.AttendanceDatabaseAdaptor;
import com.adviteya.mobile.util.AttendanceUtil;
import com.adviteya.mobile.util.MobileConstants;
import com.adviteya.mobile.vo.MobileAttendanceAdminVO;
import com.adviteya.mobile.vo.MobileEmplyoeeVO;

public class MobileAttendanceBusinessUtils implements MobileConstants
{
	
	private static final String TAG = "MobileAttendanceBusinessUtils";
	
	public static int validateAttendanceAdmin(
	        MobileAttendanceAdminVO mobileAttendance, Context context)
	        throws Exception
	{
		
		boolean _userAuthenticated = false;
		int userVerificationCode = OMC_GEN_FAIL;
		
		Log.i(TAG, "Supervisor name : " + mobileAttendance.getUserName());
		
		try
		{
			String userVerificationMsg = "";
			String omcAuthMsgCode = "";
			
			HttpClient client = new DefaultHttpClient();
			// dj : 07/09/2012 Added new Parameters
			client.getParams()
			        .setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT,
			                new Integer(30000));
			client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT,
			        new Integer(30000));
			
			String _validMobileSupervisor = IMEI_VALIDATE_MOBILE_SUPERVISOR;
			
			String _webAppURL = BASE_URL + _validMobileSupervisor;
			// HttpGet request = new HttpGet(_webAppURL);
			HttpPost httppost = new HttpPost(_webAppURL);
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
			nameValuePairs.add(new BasicNameValuePair("imeiCode",
			        mobileAttendance.getImeiNumber()));
			nameValuePairs.add(new BasicNameValuePair("supervisorUserName",
			        mobileAttendance.getUserName()));
			nameValuePairs.add(new BasicNameValuePair("latLang",
			        mobileAttendance.getLatLong()));
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			
			HttpResponse response = client.execute(httppost);
			Header[] authMsg = response.getHeaders("AUTH_MSG");
			Header[] omcAuthMsg = response.getHeaders("OMC_AUTH_MSG");
			Header[] hasPlannedAssignments = response
			        .getHeaders("PLANNED_ASSIGNMENTS");
			
			Log.i("Header data ", authMsg[0].getValue());
			Log.i("omcAuthMsgCode ", omcAuthMsg[0].getValue());
			
			userVerificationMsg = authMsg[0].getValue();
			omcAuthMsgCode = omcAuthMsg[0].getValue();
			
			/**
			 * if (userVerificationMsg
			 * .equalsIgnoreCase(MOBILE_SUPERVISOR_AUTHENTICATION_SUCCESS)) {
			 * _userAuthenticated = true; userVerificationCode =
			 * OMC_AUTH_SUCCESS; }
			 **/
			userVerificationCode = new Integer(omcAuthMsgCode).intValue();
			
		} catch (ClientProtocolException e)
		{
			Log.e(TAG, e.getMessage());
			throw new Exception(e);
		} catch (IllegalStateException e)
		{
			Log.e(TAG, e.getMessage());
			throw new Exception(e);
			
		} catch (IOException e)
		{
			Log.e(TAG, e.getMessage());
			userVerificationCode = OMC_CONN_TIMEOUT;
			throw new Exception(e);
		}
		return userVerificationCode;
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
	public static String syncLocalDb(Context context, String imeiCode,
	        String supervisorUserName, String latLang) throws Exception
	{
		
		Log.i(TAG, "-- syncLocalDb --");
		Log.i(TAG, "-- latLang --" + latLang);
		String _mobileDbSynch = null;
		boolean isFirstRun = false;
		int _recSent = 0;
		String _processedMessage = null;
		
		List<MobileEmplyoeeVO> mobileEmpList = null;
		
		/**
		 * Invoke server using HTTPClient to get the latest team snapshot For
		 * test add test records
		 */
		// if (supervisorUserName != null)
		if (imeiCode != null)
		{
			AttendanceUtil _attendanceUtil = new AttendanceUtil();
			try
			{
				// _mobileDbSynch = _attendanceUtil.getAllMobileSynch(context);
				mobileEmpList = _attendanceUtil.getAllMobileSynchList(context);
				// _recSent = _attendanceUtil.getSentRecs();
				
				Log.i(TAG, "-- _mobileDbSynch --" + _mobileDbSynch);
				Log.i(TAG, "-- _recSent --" + _recSent);
				
			} catch (Exception e1)
			{
				_mobileDbSynch = FIRST_RUN;
				isFirstRun = true;
			}
			
			_mobileDbSynch = _attendanceUtil
			        .getMobileDataSynchString(mobileEmpList);
			_recSent = _attendanceUtil.getSentRecs();
			_processedMessage = exchangeEmployeeData(context, imeiCode,
			        supervisorUserName, latLang, _mobileDbSynch, _recSent,
			        isFirstRun);
		}
		
		return _processedMessage;
	}
	
	/**
	 * 
	 * @param context
	 * @param imeiCode
	 * @param supervisorUserName
	 * @param latLang
	 * @param _mobileDbSynch
	 * @param _recSent
	 * @throws Exception
	 */
	private static String exchangeEmployeeData(Context context,
	        String imeiCode, String supervisorUserName, String latLang,
	        String _mobileDbSynch, int _recSent, boolean isFirstRun)
	        throws Exception
	{
		HttpResponse response = null;
		
		// Get Timesheet snapshot from SqlLite
		String synchServerDataString = null;
		try
		{
			synchServerDataString = AttendanceUtil.synchMobileUsingIMEI(
			        imeiCode, _mobileDbSynch.toString().trim(),
			        supervisorUserName, latLang, _recSent);
			
			// Log.i(TAG, "-- synchServerDataString --" +
			// synchServerDataString);
			
			/**
			 * The data received from the server contains - Location and Shift
			 * meta data and Timesheet. It is in the following format
			 * DATASTRING=(LOCATIONANDSHIFT=XXXXXXXXX)(TIMESHEET=XXXXXXXXX)
			 */
			// Note : 04/13/2013 - If the msg is DATA_SUBMITTED - then - string
			// is format DATA_SUBMITTE^<TIME_IN_MINS_TO_SYNCH>
			
			if (!synchServerDataString.contains("DATA_SUBMITTED"))
			{
				if (!synchServerDataString.equals(""))
				{
					// Insert Location and Shift
					
					// Insert Timesheet String
					String _timeSheetString = AttendanceUtil
					        .extractTimeSheet(synchServerDataString);
					insertSynchedRow(_timeSheetString, context, isFirstRun);
					String _locationShiftString = AttendanceUtil
					        .extractLocationAndShift(synchServerDataString);
					
					synchServerDataString = "DATA_PROCESSED";
					
					/**
					 * Need to execute Locations and Shift data logic if a
					 * Company doesn't provide planned assignments
					 */
					// if (!MobileAttendanceLogin.empAssignmentsAvailable)
					// {
					// insertLocationShiftData(_locationShiftString);
					// // showLocations();
					// }
				}
			}
			
			return synchServerDataString;
			
		} catch (Exception e)
		{
			// @todo : need to send this message to scr
			e.printStackTrace();
			Log.i(TAG, e.getMessage(), e);
			throw new Exception(e.getMessage());
		}
	}
	
	/**
	 * Parse the delimited data and insert in to mobile db
	 * 
	 * @param dbAdaptor
	 * @param synchServerDataString
	 */
	private static void insertSynchedRow(String synchServerDataString,
	        Context context, boolean isFirstRun)
	{
		AttendanceDatabaseAdaptor dbAdaptor = new AttendanceDatabaseAdaptor(
		        context);
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
		int receivedRecs = 0;
		
		while (st.hasMoreElements())
		{
			String _line = (String) st.nextElement();
			// Log.i(TAG, "Data Line : " + _line);
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
				
				// Log.i(TAG, " dbAdaptor " + dbAdaptor);
				
				dbAdaptor.insertSynchedRow(timeSheetDate, timesheetId,
				        assignmentId, shiftId, location, empId, empCompanyId,
				        empName, empPwd, inTime, outTime, marker);
				
				i++;
				receivedRecs = i;
			}
		}
		dbAdaptor.close();
		
	}
	
}
