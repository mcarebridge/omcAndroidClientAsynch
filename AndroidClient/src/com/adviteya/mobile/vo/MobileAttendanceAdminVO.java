package com.adviteya.mobile.vo;

import java.io.Serializable;

public class MobileAttendanceAdminVO implements Serializable
{
	
	private String registeredSIMSerial;
	private Long   phoneNumber;
	// Store lat,long and accuracy in mts. in the format
	// LAT:XXX@LONG:XXXX@ACU:XXXXX
	private String latLong;
	private String empKey;
	private String empPassword;
	private String imeiNumber;
	// this is same as userName in MA_User
	private String userName;
	
	public String getRegisteredSIMSerial()
	{
		return registeredSIMSerial;
	}
	
	public void setRegisteredSIMSerial(String registeredSIMSerial)
	{
		this.registeredSIMSerial = registeredSIMSerial;
	}
	
	public Long getPhoneNumber()
	{
		return phoneNumber;
	}
	
	public void setPhoneNumber(Long phoneNumber)
	{
		this.phoneNumber = phoneNumber;
	}
	
	public String getLatLong()
	{
		return latLong;
	}
	
	public void setLatLong(String latLong)
	{
		this.latLong = latLong;
	}
	
	public String getEmpKey()
	{
		return empKey;
	}
	
	public void setEmpKey(String empKey)
	{
		this.empKey = empKey;
	}
	
	public String getEmpPassword()
	{
		return empPassword;
	}
	
	public void setEmpPassword(String empPassword)
	{
		this.empPassword = empPassword;
	}
	
	public String getImeiNumber()
	{
		return imeiNumber;
	}
	
	public void setImeiNumber(String imeiNumber)
	{
		this.imeiNumber = imeiNumber;
	}
	
	public String getUserName()
	{
		return userName;
	}
	
	public void setUserName(String userName)
	{
		this.userName = userName;
	}
	
}
