package fi.avoindata.julha;

import android.os.Bundle;

public class CallItem {
	public static final String ID = "id";
	public static final String FULLNAME = "fullname";
	public static final String NUMBER = "number";
	public static final String GIVENNAME = "givenName";
	public static final String SN = "sn";
	public static final String ORG = "org";
	public static final String STATUS = "status";
	public static final String TIMESTAMP = "timestamp";
	
	public static final int STATUS_CI_NORMAL = 0;
	public static final int STATUS_CI_REPORTED = 1;
	
	private int id;
	private String number;
	private String fullname;
	private String sn;
	private String givenName;
	private String org;
	private int status;
	private int timestamp;
	
	public void setId(int id) {
		this.id = id;
	}
	public int getId() {
		return id;
	}
	public void setFullname(String fullname) {
		this.fullname = fullname;
	}
	public String getFullname() {
		return fullname;
	}
	public void setSn(String sn) {
		this.sn = sn;
	}
	public String getSn() {
		return sn;
	}
	public void setGivenName(String givenName) {
		this.givenName = givenName;
	}
	public String getGivenName() {
		return givenName;
	}
	public void setOrg(String org) {
		this.org = org;
	}
	public String getOrg() {
		return org;
	}
	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}
	public int getTimestamp() {
		return timestamp;
	}

	public Bundle getBundle(){
		Bundle bundle = new Bundle();
		bundle.putInt(CallItem.ID, this.id);
		bundle.putCharSequence(CallItem.FULLNAME, this.fullname);
		bundle.putCharSequence(CallItem.NUMBER, this.number);
		bundle.putCharSequence(CallItem.ORG, this.org);
		bundle.putInt(CallItem.TIMESTAMP, this.timestamp);
		return bundle;
	}
	public void setNumber(String number) {
		this.number = number;
	}
	public String getNumber() {
		return number;
	}
	public String toString() {
		return "fullname:" +this.fullname + ", number:" +this.number + ", org:" + this.org + ", givenName" + this.givenName + ", sn:" + this.sn;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public int getStatus() {
		return status;
	}
}
