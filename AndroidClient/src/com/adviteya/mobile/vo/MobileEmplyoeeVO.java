package com.adviteya.mobile.vo;

import java.io.Serializable;

public class MobileEmplyoeeVO implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4589249665664369675L;
	private Long keyRowId;
	private String timeSheetDate;
	private Long timeSheetId;
	private Long assignmentId;
	private Long shiftId;
	private String location;
	private Long empId;
	private String empCompanyId;
	private String empName;
	private String passKey;
	private String inTime;
	private String outTime;
	private String createdDateTime;
	private String createdBy;
	private String marker;
	/**
	 * @return the passKey
	 */
	public String getPassKey() {
		return passKey;
	}

	/**
	 * @param passKey
	 *            the passKey to set
	 */
	public void setPassKey(String passKey) {
		this.passKey = passKey;
	}

	/**
	 * @return the createdBy
	 */
	public String getCreatedBy() {
		return createdBy;
	}

	/**
	 * @param createdBy
	 *            the createdBy to set
	 */
	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	/**
	 * @return the shiftId
	 */
	public Long getShiftId() {
		return shiftId;
	}

	/**
	 * @param shiftId
	 *            the shiftId to set
	 */
	public void setShiftId(Long shiftId) {
		this.shiftId = shiftId;
	}

	/**
	 * @return the empId
	 */
	public Long getEmpId() {
		return empId;
	}

	/**
	 * @param empId
	 *            the empId to set
	 */
	public void setEmpId(Long empId) {
		this.empId = empId;
	}

	/**
	 * @return the keyRowId
	 */
	public Long getKeyRowId() {
		return keyRowId;
	}

	/**
	 * @param keyRowId
	 *            the keyRowId to set
	 */
	public void setKeyRowId(Long keyRowId) {
		this.keyRowId = keyRowId;
	}

	/**
	 * @return the timeSheetDate
	 */
	public String getTimeSheetDate() {
		return timeSheetDate;
	}

	/**
	 * @param timeSheetDate
	 *            the timeSheetDate to set
	 */
	public void setTimeSheetDate(String timeSheetDate) {
		this.timeSheetDate = timeSheetDate;
	}

	/**
	 * @return the timeSheetId
	 */
	public Long getTimeSheetId() {
		return timeSheetId;
	}

	/**
	 * @param timeSheetId
	 *            the timeSheetId to set
	 */
	public void setTimeSheetId(Long timeSheetId) {
		this.timeSheetId = timeSheetId;
	}

	/**
	 * @return the assignmentId
	 */
	public Long getAssignmentId() {
		return assignmentId;
	}

	/**
	 * @param assignmentId
	 *            the assignmentId to set
	 */
	public void setAssignmentId(Long assignmentId) {
		this.assignmentId = assignmentId;
	}

	/**
	 * @return the location
	 */
	public String getLocation() {
		return location;
	}

	/**
	 * @param location
	 *            the location to set
	 */
	public void setLocation(String location) {
		this.location = location;
	}

	/**
	 * @return the empCompanyId
	 */
	public String getEmpCompanyId() {
		return empCompanyId;
	}

	/**
	 * @param empCompanyId
	 *            the empCompanyId to set
	 */
	public void setEmpCompanyId(String empCompanyId) {
		this.empCompanyId = empCompanyId;
	}

	/**
	 * @return the empName
	 */
	public String getEmpName() {
		return empName;
	}

	/**
	 * @param empName
	 *            the empName to set
	 */
	public void setEmpName(String empName) {
		this.empName = empName;
	}

	/**
	 * @return the inTime
	 */
	public String getInTime() {
		return inTime;
	}

	/**
	 * @param inTime
	 *            the inTime to set
	 */
	public void setInTime(String inTime) {
		this.inTime = inTime;
	}

	/**
	 * @return the outTime
	 */
	public String getOutTime() {
		return outTime;
	}

	/**
	 * @param outTime
	 *            the outTime to set
	 */
	public void setOutTime(String outTime) {
		this.outTime = outTime;
	}

	/**
	 * @return the createdDateTime
	 */
	public String getCreatedDateTime() {
		return createdDateTime;
	}

	/**
	 * @param createdDateTime
	 *            the createdDateTime to set
	 */
	public void setCreatedDateTime(String createdDateTime) {
		this.createdDateTime = createdDateTime;
	}

	public String getMarker()
	{
		return marker;
	}

	public void setMarker(String marker)
	{
		this.marker = marker;
	}

}
