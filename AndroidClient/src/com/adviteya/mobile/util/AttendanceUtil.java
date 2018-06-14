package com.adviteya.mobile.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.content.Context;
import android.os.SystemClock;
import android.telephony.SmsManager;
import android.util.Log;

import com.adviteya.mobile.activity.MobileAttendanceLogin;
import com.adviteya.mobile.exception.NTPTimeoutException;
import com.adviteya.mobile.service.AttendanceDatabaseAdaptor;
import com.adviteya.mobile.vo.MobileEmplyoeeVO;

public class AttendanceUtil implements MobileConstants
{
	
	private static int          sentRecs = 0;
	private static final String TAG      = "AttendanceUtil";
	
	/**
	 * 
	 * @param textLoginId
	 * @param passKey
	 * @param latLang
	 * @param mobileData
	 * @return
	 * @throws Exception
	 */
	public static String synchMobile(String textLoginId, String passKey,
	        String latLang, String mobileData) throws Exception
	{
		HttpResponse response = null;
		
		// Get Timesheet snapshot from SqlLite
		String synchServerDataString = null;
		
		HttpClient client = new DefaultHttpClient();
		String _synchServerData = SYNCH_SERVER_DATA;
		String _webAppURL = BASE_URL + SYNCH_SERVER_DATA;
		HttpPost httppost = new HttpPost(_webAppURL);
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
		nameValuePairs.add(new BasicNameValuePair("textLoginId",
		        MobileAttendanceLogin._empId.toString().trim()));
		nameValuePairs.add(new BasicNameValuePair("pwdPasscode",
		        MobileAttendanceLogin._pwd.toString().trim()));
		nameValuePairs.add(new BasicNameValuePair("latLang",
		        MobileAttendanceLogin._latlang.toString().trim()));
		nameValuePairs.add(new BasicNameValuePair("mobileData", mobileData
		        .toString().trim()));
		httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		
		response = client.execute(httppost);
		Header[] headers = response.getHeaders("AUTH_MSG");
		synchServerDataString = readHTTPResponse(client, httppost, response);
		
		return synchServerDataString;
	}
	
	/**
	 * Use this method to Synch using IMEI code
	 * 
	 * @param imeiCode
	 * @return
	 * @throws Exception
	 */
	public static String synchMobileUsingIMEI(String imeiCode,
	        String mobileData, String supervisorUserName, String latLang,
	        int recsSent) throws Exception
	{
		HttpResponse response = null;
		
		// Get Timesheet snapshot from SqlLite
		String synchServerDataString = null;
		final HttpParams httpParams = new BasicHttpParams();
		// added timeout of 60 sec
		HttpConnectionParams.setConnectionTimeout(httpParams, 60000);
		HttpClient client = new DefaultHttpClient();
		String _processedMessage = "DATA_SUBMITTED";
		String _timeToSynch = "0";
		
		// Throw exception
		
		// client = null;
		
		String _synchServerData = IMEI_SYNCH_SERVER_DATA;
		String _webAppURL = BASE_URL + IMEI_SYNCH_SERVER_DATA;
		HttpPost httppost = new HttpPost(_webAppURL);
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
		nameValuePairs.add(new BasicNameValuePair("imeiCode", imeiCode.trim()));
		nameValuePairs.add(new BasicNameValuePair("supervisorUserName",
		        supervisorUserName));
		nameValuePairs.add(new BasicNameValuePair("mobileData", mobileData
		        .toString().trim()));
		nameValuePairs.add(new BasicNameValuePair("latLang", latLang));
		nameValuePairs.add(new BasicNameValuePair("mobileRecsSent", Integer
		        .toString(recsSent)));
		httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		
		response = client.execute(httppost);
		Header[] headers = response.getHeaders("AUTH_MSG");
		Header[] headers1 = response.getHeaders("TIME_TO_SYNCH");
		
		Log.i(TAG,
		        "synchMobileUsingIMEI ---------------------->"
		                + headers[0].getValue());
		
		_processedMessage = headers[0].getValue();
		_timeToSynch = (headers1 == null | headers1.length == 0) ? "0"
		        : headers1[0].getValue();
		
		if (_processedMessage.equalsIgnoreCase("MOBILE_SERVER_SYSTEM_ERR"))
		{
			throw new Exception("SYNCH_ERR");
		} else if (!_processedMessage.equalsIgnoreCase("02-PROCESSED"))
		{
			
			synchServerDataString = "DATA_SUBMITTED";
			synchServerDataString += "^" + _timeToSynch;
			
		} else if (_processedMessage.equalsIgnoreCase("02-PROCESSED"))
		{
			synchServerDataString = readHTTPResponse(client, httppost, response);
		}
		
		return synchServerDataString;
	}
	
	/**
	 * 
	 * @param client
	 * @param httppost
	 * @param response
	 * @throws IOException
	 */
	private static String readHTTPResponse(HttpClient client,
	        HttpPost httppost, HttpResponse response) throws IOException
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
				
				GZIPInputStream zis = new GZIPInputStream(
				        new BufferedInputStream(instream));
				
				BufferedReader reader = new BufferedReader(
				        new InputStreamReader(zis));
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
	 * Get the all records from Mobile and Send the Data a csv
	 * 
	 * @param dbAdaptor
	 * @return
	 * @throws Exception
	 */
	public String getAllMobileSynch(Context context) throws Exception
	{
		AttendanceDatabaseAdaptor dbAdaptor = new AttendanceDatabaseAdaptor(
		        context);
		dbAdaptor.open();
		List<MobileEmplyoeeVO> mobileEmpList = dbAdaptor.getAllMobileSynch();
		dbAdaptor.close();
		
		setSentRecs(mobileEmpList.size());
		
		StringBuffer _sb = new StringBuffer();
		_sb.append("timeSheetId,assignmentId,inTime,outTime,locationKey,ShiftKey,$");
		
		for (Iterator iterator = mobileEmpList.iterator(); iterator.hasNext();)
		{
			
			MobileEmplyoeeVO mobileEmplyoeeVO = (MobileEmplyoeeVO) iterator
			        .next();
			_sb.append(mobileEmplyoeeVO.getTimeSheetId());
			_sb.append(",");
			_sb.append(mobileEmplyoeeVO.getAssignmentId());
			_sb.append(",");
			_sb.append(mobileEmplyoeeVO.getInTime());
			_sb.append(",");
			_sb.append(mobileEmplyoeeVO.getOutTime());
			_sb.append(",");
			_sb.append(mobileEmplyoeeVO.getLocation());
			_sb.append(",");
			_sb.append(mobileEmplyoeeVO.getShiftId());
			_sb.append(",");
			_sb.append("$");
			setSentRecs(getSentRecs() + 1);
		}
		
		// Log.i(TAG, "Data sent -> " + _sb.toString());
		return _sb.toString();
		
	}
	
	/**
	 * This methods retuns the list of the total numbers records found to be
	 * synched
	 * 
	 * @param context
	 * @return
	 * @throws Exception
	 */
	public List<MobileEmplyoeeVO> getAllMobileSynchList(Context context)
	        throws Exception
	{
		AttendanceDatabaseAdaptor dbAdaptor = new AttendanceDatabaseAdaptor(
		        context);
		dbAdaptor.open();
		List<MobileEmplyoeeVO> mobileEmpList = dbAdaptor.getAllMobileSynch();
		dbAdaptor.close();
		
		Log.i(TAG, "Employee to be Synched -> " + mobileEmpList.size());
		return mobileEmpList;
		
	}
	
	/**
	 * Build the Data string of the Mobile data to be sent
	 * 
	 * @param mobileEmpList
	 * @return
	 * @throws Exception
	 */
	public String getMobileDataSynchString(List<MobileEmplyoeeVO> mobileEmpList)
	        throws Exception
	{
		setSentRecs(mobileEmpList.size());
		
		StringBuffer _sb = new StringBuffer();
		_sb.append("timeSheetId,assignmentId,inTime,outTime,locationKey,ShiftKey,$");
		
		for (Iterator iterator = mobileEmpList.iterator(); iterator.hasNext();)
		{
			
			MobileEmplyoeeVO mobileEmplyoeeVO = (MobileEmplyoeeVO) iterator
			        .next();
			_sb.append(mobileEmplyoeeVO.getTimeSheetId());
			_sb.append(",");
			_sb.append(mobileEmplyoeeVO.getAssignmentId());
			_sb.append(",");
			_sb.append(mobileEmplyoeeVO.getInTime());
			_sb.append(",");
			_sb.append(mobileEmplyoeeVO.getOutTime());
			_sb.append(",");
			_sb.append(mobileEmplyoeeVO.getLocation());
			_sb.append(",");
			_sb.append(mobileEmplyoeeVO.getShiftId());
			_sb.append(",");
			_sb.append("$");
			setSentRecs(getSentRecs() + 1);
		}
		
		// Log.i(TAG, "Data sent -> " + _sb.toString());
		return _sb.toString();
		
	}
	
	/**
	 * @return the sentRecs
	 */
	public static int getSentRecs()
	{
		return sentRecs;
	}
	
	/**
	 * @param sentRecs
	 *            the sentRecs to set
	 */
	private static void setSentRecs(int sentRecs)
	{
		AttendanceUtil.sentRecs = sentRecs;
	}
	
	/**
	 * Extract Location and Shift data from the server data
	 * 
	 * @param serverData
	 * @return
	 */
	public static String extractLocationAndShift(String serverData)
	{
		StringTokenizer _st = new StringTokenizer(serverData, "()");
		String _locationAndShiftRaw = (String) _st.nextElement();
		String _locationAndShift = _locationAndShiftRaw.substring(
		        _locationAndShiftRaw.indexOf("="),
		        _locationAndShiftRaw.length());
		
		return _locationAndShift;
	}
	
	/**
	 * Extract Location and Shift data from the server data
	 * 
	 * @param serverData
	 * @return
	 */
	public static String extractTimeSheet(String serverData)
	{
		StringTokenizer _st = new StringTokenizer(serverData, "()");
		String _locationAndShiftRaw = (String) _st.nextElement();
		String _timeSheetRaw = (String) _st.nextElement();
		String _timeSheet = _timeSheetRaw.substring(_timeSheetRaw.indexOf("="),
		        _timeSheetRaw.length());
		
		return _timeSheet;
	}
	
	/**
	 * 
	 * @return
	 * @throws NTPTimeoutException
	 */
	public static long getNTPTime() throws NTPTimeoutException
	{
		SntpClient _sntp = new SntpClient();
		_sntp.requestTime("time.nist.gov", 10000);
		
		long _now = _sntp.getNtpTime() + SystemClock.elapsedRealtime()
		        - _sntp.getNtpTimeReference();
		return _now;
	}
	
	
	/**
	 * Send short Messages
	 * @param phoneNumber
	 * @param message
	 */
	public static void sendSMS(String phoneNumber, String message)
	{
		
		SmsManager smsManager = SmsManager.getDefault();
		smsManager.sendTextMessage(phoneNumber, null, message, null, null);
		
	}
	
	/**
	 * Send long messages
	 * @param phoneNumber
	 * @param message
	 */
	public static void sendLongSMS(String phoneNumber, String message)
	{
		
		SmsManager smsManager = SmsManager.getDefault();
		ArrayList<String> parts = smsManager.divideMessage(message);
		smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null,
		        null);
		
	}
}
