package com.adviteya.mobile.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.adviteya.mobile.db.MobileAttendanceDBHelper;
import com.adviteya.mobile.util.MobileConstants;
import com.adviteya.mobile.vo.MobileAttendanceAdminVO;
import com.adviteya.mobile.vo.MobileEmplyoeeVO;

public class AttendanceDatabaseAdaptor implements MobileConstants
{
	
	private static final String      TAG = "AttendanceDatabaseAdaptor";
	
	private Context                  context;
	private SQLiteDatabase           database;
	private MobileAttendanceDBHelper dbHelper;
	
	public AttendanceDatabaseAdaptor(Context context)
	{
		this.context = context;
	}
	
	public AttendanceDatabaseAdaptor open() throws SQLException
	{
		dbHelper = new MobileAttendanceDBHelper(context);
		database = dbHelper.getWritableDatabase();
		return this;
	}
	
	public void close()
	{
		dbHelper.close();
	}
	
	/**
	 * Insert the server snapshot in the sqllite
	 * 
	 * @param timeSheetDate
	 * @param timesheetId
	 * @param assignmentId
	 * @param shiftId
	 * @param location
	 * @param empId
	 * @param empCompanyId
	 * @param empName
	 * @param empPwd
	 * @param inTime
	 * @param outTime
	 * @param marker
	 * @return
	 */
	
	public long insertSynchedRow(String timeSheetDate, long timesheetId,
	        long assignmentId, long shiftId, String location, String empId,
	        String empCompanyId, String empName, String empPwd, String inTime,
	        String outTime, String marker)
	{
		
		long i = 9999;
		try
		{
			ContentValues initialValues = createContentValues(timeSheetDate,
			        timesheetId, assignmentId, shiftId, location, empId,
			        empCompanyId, empName, empPwd, inTime, outTime, marker);
			i = database.insert(TABLE_MOBILE_SYNC, null, initialValues);
			
		} catch (Exception e)
		{
			e.printStackTrace();
		} finally
		{
			return i;
		}
		
	}
	
	/**
	 * 
	 * @param shiftId
	 * @param shiftName
	 * @param locationId
	 * @param location
	 * @param timeZone
	 * @return
	 */
	public long insertLocationShift(long shiftId, String shiftName,
	        long locationId, String location, String timeZone)
	{
		
		long i = 9999;
		try
		{
			ContentValues initialValues = createLocationShiftsContentValues(
			        shiftId, shiftName, locationId, location, timeZone);
			i = database.insert(TABLE_LOC_SHIFT_SYNC, null, initialValues);
			
		} catch (Exception e)
		{
			e.printStackTrace();
		} finally
		{
			return i;
		}
		
	}
	
	/**
	 * 
	 * @param timeSheetId
	 *            TODO
	 * @param inTime
	 *            TODO
	 * @param outTime
	 *            TODO
	 */
	public void updateMobileSync(long timeSheetId, String inTime,
	        String outTime, long empId)
	{
		
		// Log.i(TAG, "-- " + empId);
		
		ContentValues updateValues = new ContentValues();
		updateValues.put(TIMESHEET_ID, timeSheetId);
		updateValues.put(INTIME, inTime);
		updateValues.put(OUTTIME, outTime);
		
		// empCompanyId = "'" + empCompanyId + "'";
		
		int i = database.update(TABLE_MOBILE_SYNC, updateValues, " empId ="
		        + empId, null);
		
		// Log.i(TAG, "Updated recs " + i);
	}
	
	/**
	 * Update the Table Mobile Sync for No Asiignemnts
	 * 
	 * @param timeSheetId
	 * @param inTime
	 * @param outTime
	 * @param empId
	 */
	public void updateMobileSyncForNoAssignment(long timeSheetId,
	        String inTime, String outTime, long empId, String locationKey,
	        String shiftKey)
	{
		
		Log.i(TAG, "-- " + empId);
		
		ContentValues updateValues = new ContentValues();
		updateValues.put(TIMESHEET_ID, timeSheetId);
		updateValues.put(INTIME, inTime);
		updateValues.put(OUTTIME, outTime);
		updateValues.put(SHIFT_ID, shiftKey);
		updateValues.put(LOCATION, locationKey);
		
		// empCompanyId = "'" + empCompanyId + "'";
		
		int i = database.update(TABLE_MOBILE_SYNC, updateValues, " empId ="
		        + empId, null);
		
		Log.i(TAG, "Updated recs " + i);
	}
	
	/**
	 * 
	 * @param companyId
	 * @param empNum
	 * @param passKey
	 * @return
	 */
	public MobileEmplyoeeVO authenticateEmplyoee(String empCompanyId,
	        String empPwd)
	{
		
		Cursor _cursor = null;
		
		// The 'if' will get executed if it is coming from Barcode
		if (empPwd.equalsIgnoreCase("NOT_NEEDED"))
		{
			_cursor = database.query(TABLE_MOBILE_SYNC, null, EMPCOMPANY_ID
			        + " = '" + empCompanyId + "'", null, null, null, null);
			
		} else
		{
			_cursor = database.query(TABLE_MOBILE_SYNC, null, EMPCOMPANY_ID
			        + " = '" + empCompanyId + "' and " + EMPPWD + " = '"
			        + empPwd + "'", null, null, null, null);
			
		}
		
		MobileEmplyoeeVO attendance = null;
		
		if (_cursor != null)
		{
			
			_cursor.moveToFirst();
			
			while (_cursor.isAfterLast() == false)
			{
				
				attendance = new MobileEmplyoeeVO();
				
				attendance.setKeyRowId(_cursor.getLong(0));
				attendance.setTimeSheetId(_cursor.getLong(2));
				attendance.setEmpId(_cursor.getLong(6));
				attendance.setEmpCompanyId(_cursor.getString(7));
				attendance.setEmpName(_cursor.getString(8));
				attendance.setInTime(_cursor.getString(10));
				attendance.setOutTime(_cursor.getString(11));
				attendance.setMarker(_cursor.getString(12));
				_cursor.moveToNext();
			}
			
			_cursor.close();
		}
		
		return attendance;
	}
	
	/**
	 * 
	 * @param category
	 * @param summary
	 * @param description
	 * @return
	 */
	private ContentValues createContentValues(String timeSheetDate,
	        long timesheetId, long assignmentId, long shiftId, String location,
	        String empId, String empCompanyId, String empName, String empPwd,
	        String inTime, String outTime, String marker)
	{
		ContentValues values = new ContentValues();
		values.put("TIMESHEETDATE", timeSheetDate);
		values.put("TIMESHEETID", timesheetId);
		values.put("ASSIGNMENTID", assignmentId);
		values.put("SHIFTID", shiftId);
		values.put("LOCATION", location);
		values.put("EMPID", empId);
		values.put("EMPCOMPANYID", empCompanyId);
		values.put("EMPNAME", empName);
		values.put("EMPPWD", empPwd);
		values.put("INTIME", inTime);
		values.put("OUTTIME", outTime);
		values.put("MARKER", marker);
		return values;
	}
	
	/**
	 * 
	 * @param shiftId
	 * @param shiftName
	 * @param locationId
	 * @param location
	 * @param timeZone
	 * @return
	 */
	private ContentValues createLocationShiftsContentValues(long shiftId,
	        String shiftName, long locationId, String location, String timeZone)
	{
		ContentValues values = new ContentValues();
		values.put("SHIFTKEY", shiftId);
		values.put("SHIFT", shiftName);
		values.put("LOCATIONKEY", locationId);
		values.put("LOCATION", location);
		values.put("TIMEZONE", timeZone);
		return values;
	}
	
	/**
	 * Set values for Authentication Table
	 * 
	 * @param mobileAttendanceAdmin
	 * @param verify
	 * @param currentDate
	 * @return
	 */
	private ContentValues createAdminAuthContentValues(
	        MobileAttendanceAdminVO mobileAttendanceAdmin, String verify,
	        String currentDate)
	{
		ContentValues values = new ContentValues();
		values.put("IMEICODE", mobileAttendanceAdmin.getImeiNumber());
		values.put("USERNAME", mobileAttendanceAdmin.getUserName());
		values.put("VERIFY", verify);
		values.put("CURRENTDATETIME", currentDate);
		return values;
	}
	
	/**
	 * Clean the complete mobileSynch Table
	 * 
	 * @return
	 */
	public void cleanMobileSync()
	{
		
		database.delete(TABLE_MOBILE_SYNC, null, null);
	}
	
	public void cleanLocationShiftSync()
	{
		
		database.delete(TABLE_LOC_SHIFT_SYNC, null, null);
	}
	
	public void cleanLocalAuth()
	{
		
		database.delete(TABLE_LOCAL_AUTH, null, null);
	}
	
	/**
	 * Get all the records from the Mobile table
	 */
	public List<MobileEmplyoeeVO> getAllMobileSynch() throws Exception
	{
		
		// @todo : Check if the table exists
		Cursor _cursor = null;
//		 _cursor = database.query(TABLE_MOBILE_SYNC, null, null, null, null,
//		 null, null);
		
		_cursor = database.query(TABLE_MOBILE_SYNC, null, " inTime != '-' ", null, null,
		        null, null);
		
		ArrayList<MobileEmplyoeeVO> empList = new ArrayList();
		MobileEmplyoeeVO attendance = null;
		
		if (_cursor != null)
		{
			
			_cursor.moveToFirst();
			
			while (_cursor.isAfterLast() == false)
			{
				
				attendance = new MobileEmplyoeeVO();
				
				attendance.setKeyRowId(_cursor.getLong(0));
				attendance.setTimeSheetDate(_cursor.getString(1));
				attendance.setTimeSheetId(_cursor.getLong(2));
				attendance.setAssignmentId(_cursor.getLong(3));
				attendance.setShiftId(_cursor.getLong(4));
				attendance.setLocation(_cursor.getString(5));
				attendance.setEmpId(_cursor.getLong(6));
				attendance.setEmpCompanyId(_cursor.getString(7));
				attendance.setEmpName(_cursor.getString(8));
				attendance.setPassKey(_cursor.getString(9));
				attendance.setInTime(_cursor.getString(10));
				attendance.setOutTime(_cursor.getString(11));
				attendance.setMarker(_cursor.getString(12));
				_cursor.moveToNext();
				
				empList.add(attendance);
			}
			
			_cursor.close();
		}
		
		return empList;
		
	}
	
	/**
	 * Retrieve all the locations for the Supervisor
	 * 
	 * @return
	 */
	public Map<String, String> getAllTheLocations()
	{
		Cursor _cursor = null;
		String _cols[] = new String[] { "LOCATIONKEY", "LOCATION" };
		_cursor = database.query(TABLE_LOC_SHIFT_SYNC, _cols, null, null, null,
		        null, null);
		
		HashMap<String, String> locations = new HashMap<String, String>();
		
		if (_cursor != null)
		{
			
			_cursor.moveToFirst();
			
			while (_cursor.isAfterLast() == false)
			{
				
				locations.put(_cursor.getString(0), _cursor.getString(1));
				_cursor.moveToNext();
			}
			
			_cursor.close();
		}
		
		return locations;
	}
	
	/**
	 * Get all the Shifts for a Location
	 * 
	 * @param locationKey
	 * @return
	 */
	public Map<String, String> getAllShiftsForALocation(String locationKey)
	{
		Cursor _cursor = null;
		String _cols[] = new String[] { "SHIFTKEY", "SHIFT" };
		_cursor = database.query(TABLE_LOC_SHIFT_SYNC, _cols, "LOCATIONKEY = '"
		        + locationKey + "'", null, null, null, null);
		
		HashMap<String, String> locations = new HashMap<String, String>();
		
		if (_cursor != null)
		{
			
			_cursor.moveToFirst();
			
			while (_cursor.isAfterLast() == false)
			{
				
				locations.put(_cursor.getString(0), _cursor.getString(1));
				_cursor.moveToNext();
			}
			
			_cursor.close();
		}
		
		return locations;
	}
	
	/**
	 * After server side authentication store the auth to local db, so that it
	 * doesn't go to server again
	 * 
	 * @param mobileAttendancAdmin
	 */
	public void authenicateAdminUserLocal(
	        MobileAttendanceAdminVO mobileAttendancAdmin, String verify)
	{
		Log.i(TAG, "-- " + "-- authenicateAdminUserLocal -- ");
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy kk:mm");
		String _currentDate = sdf.format(new Date());
		
		ContentValues initialValues = createAdminAuthContentValues(
		        mobileAttendancAdmin, verify, _currentDate);
		long _rowId = database.insert(TABLE_LOCAL_AUTH, null, initialValues);
		
		Log.i(TAG, "-- " + "-- inserted auth user -- " + _rowId);
	}
	
	/**
	 * Check the local db - if the admin user is already authenticated
	 * 
	 * @param mobileAttendancAdmin
	 * @return
	 */
	public boolean isAdminAuthenticated(
	        MobileAttendanceAdminVO mobileAttendancAdmin)
	{
		
		Log.i(TAG, "-- " + "-- isAdminAuthenticated -- ");
		/**
		 * Read the record for given IMEIcode and user. If user if found return
		 * 'true' for verified and 'false' for not verified
		 * 
		 * If User is not found return false
		 */
		
		boolean _userVerified = false;
		// String _subString = "imeiCode = '"
		// + mobileAttendancAdmin.getImeiNumber() + "'" + " and "
		// + "userName = '" + mobileAttendancAdmin.getUserName() + "'";
		
		String _subString = "imeiCode = '"
		        + mobileAttendancAdmin.getImeiNumber() + "'";
		
		Cursor _cursor = null;
		String _cols[] = new String[] { "IMEICODE", "USERNAME", "VERIFY",
		        "CURRENTDATETIME" };
		_cursor = database.query(TABLE_LOCAL_AUTH, _cols, _subString, null,
		        null, null, null);
		
		if (_cursor != null)
		{
			_cursor.moveToFirst();
			
			Log.i(TAG, "-- " + _cursor.getCount());
			
			while (_cursor.isAfterLast() == false)
			{
				if (_cursor.getCount() > 0)
				{
					if (_cursor.getString(2).equalsIgnoreCase("Y"))
					{
						_userVerified = true;
					} else if (_cursor.getString(2).equalsIgnoreCase("N"))
					{
						_userVerified = false;
					}
				}
				_cursor.moveToNext();
			}
			
			_cursor.close();
		}
		
		return _userVerified;
	}
}
