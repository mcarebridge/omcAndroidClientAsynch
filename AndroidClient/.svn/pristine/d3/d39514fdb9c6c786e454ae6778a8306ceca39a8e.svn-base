package com.adviteya.mobile.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.adviteya.mobile.db.MobileAttendanceDBHelper;

public class AttendanceService extends Service {

	private static final String TAG = "AttendanceService";
	private MobileAttendanceDBHelper dbHelper;
	private AttendanceDatabaseAdaptor dbAdaptor;

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(TAG, "-- onCreated --");
		dbAdaptor = new AttendanceDatabaseAdaptor(this);
		dbAdaptor = dbAdaptor.open();
//		long rowId = dbAdaptor.insertSynchedRow("Kids", "Folk story",
//				"This is a story of a crocadile and a Monkey");
//		Log.d(TAG, "RowId : " + rowId);

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.d(TAG, "onStartCommand");
		return 0;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy");
	}

	public void createRecord() {
		dbAdaptor = new AttendanceDatabaseAdaptor(this);
		dbAdaptor.open();
		System.out.println("dbAdaptor Object is " + dbAdaptor);
//		long rowId = dbAdaptor.createsampleTableMobile("Kids", "Folk story",
//				"This is a story of a crocadile and a Monkey");
//		Log.d(TAG, "RowId : " + rowId);
		// dbAdaptor.close();

	}

}
