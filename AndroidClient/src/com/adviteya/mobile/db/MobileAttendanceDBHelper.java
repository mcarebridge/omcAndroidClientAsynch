package com.adviteya.mobile.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.adviteya.mobile.util.MobileConstants;

public class MobileAttendanceDBHelper extends SQLiteOpenHelper implements
        MobileConstants
{
	
	/**
	 * 
	 * @param context
	 */
	public MobileAttendanceDBHelper(Context context)
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	/**
	 * 
	 */
	@Override
	public void onCreate(SQLiteDatabase database)
	{
		Log.i(TAG,
		        "------------------------------------------------------------------- onCreate --");
		String _a = database.findEditTable(TABLE_MOBILE_SYNC);
		database.execSQL(MOBILE_SYNCH_TABLE);
		database.execSQL(LOC_SHIFT_SYNC_TABLE);
		database.execSQL(LOCAL_AUTH_TABLE);
	}
	
	/**
	 * 
	 */
	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion,
	        int newVersion)
	{
		Log.i(TAG, "-- onUpgrade --" + "oldVersion=" + oldVersion
		        + " newVersion=" + newVersion);
		database.execSQL("DROP TABLE IF EXISTS " + TABLE_MOBILE_SYNC);
		database.execSQL("DROP TABLE IF EXISTS " + TABLE_LOC_SHIFT_SYNC);
		database.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCAL_AUTH);
		onCreate(database);
	}
	
}
