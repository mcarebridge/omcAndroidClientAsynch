package com.adviteya.mobile.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class AppStatus
{
	
	private static AppStatus    instance  = new AppStatus();
	private ConnectivityManager connectivityManager;
	private NetworkInfo         wifiInfo, mobileInfo;
	private static Context      context;
	private boolean             connected = false;
	
	/**
	 * Get instance
	 * 
	 * @param ctx
	 * @return
	 */
	public static AppStatus getInstance(Context ctx)
	{
		
		context = ctx;
		return instance;
	}
	
	/**
	 * 
	 * @param con
	 * @return
	 */
	public boolean isOnline()
	{
		
		connectivityManager = (ConnectivityManager) context
		        .getSystemService(Context.CONNECTIVITY_SERVICE);
		
		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
		connected = networkInfo != null && networkInfo.isAvailable()
		        && networkInfo.isConnected();
		return connected;
	}
}
