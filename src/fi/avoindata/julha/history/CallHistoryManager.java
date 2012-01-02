package fi.avoindata.julha.history;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import fi.avoindata.julha.CallItem;
import fi.avoindata.julha.history.DBHelper;


public class CallHistoryManager {
	private static final String TAG = CallHistoryManager.class.getSimpleName();

	private static final int MAX_ITEMS = 40;
	private static final String[] GET_ITEM_COL_PROJECTION = {
		DBHelper.ID_COL,
		DBHelper.FULLNAME_COL,
		DBHelper.GIVENNAME_COL,
		DBHelper.SN_COL,
		DBHelper.NUMBER_COL,
		DBHelper.ORG_COL,
		DBHelper.TIMESTAMP_COL
  	};
  	
  	private static final String[] EXPORT_COL_PROJECTION = {
  		DBHelper.ID_COL,
  		DBHelper.FULLNAME_COL,
  		DBHelper.GIVENNAME_COL,
  		DBHelper.SN_COL,
  		DBHelper.NUMBER_COL,
  		DBHelper.ORG_COL,
  		DBHelper.TIMESTAMP_COL
  	};
  
  	private static final String[] ID_COL_PROJECTION = { 
	  DBHelper.ID_COL  
  	};
  
  	private static final DateFormat EXPORT_DATE_TIME_FORMAT = DateFormat.getDateTimeInstance();

	private Context context;
	  
	public CallHistoryManager(Context context) {
		this.context = context;
	}
	  
	  public static List<CallItem> getCallItems(Context context) {
		    SQLiteOpenHelper helper = new DBHelper(context);
		    List<CallItem> items = new ArrayList<CallItem>();
		    SQLiteDatabase db = helper.getReadableDatabase();
		    //db.delete(DBHelper.TABLE_NAME, null, null);
		    Cursor cursor = null;
		    try {
		      cursor = db.query(DBHelper.TABLE_NAME,
		                        GET_ITEM_COL_PROJECTION,
		                        null, null, null, null,
		                        DBHelper.TIMESTAMP_COL + " DESC");
		      int count = 0;
		      while (cursor.moveToNext()) {
		    	  CallItem result = new CallItem();
		    	  result.setId(cursor.getInt(0));
		    	  result.setFullname(cursor.getString(1));
		    	  result.setGivenName(cursor.getString(2));
		    	  result.setSn(cursor.getString(3));
		    	  result.setNumber(cursor.getString(4));
		    	  result.setOrg(cursor.getString(5));
		    	  result.setTimestamp(cursor.getInt(6));
		    	  Log.i(TAG, result.toString());
		    	  count++;
		    	  items.add(result);
		      }
		    } finally {
		      if (cursor != null) {
		        cursor.close();
		      }
		      db.close();
		    }
		    return items;
		  }
	  
	  public static void addCallItem(Context context,CallItem callItem) {
		  SQLiteOpenHelper helper = new DBHelper(context);
	      SQLiteDatabase db = helper.getWritableDatabase();
	      try {
	    	  String where = DBHelper.NUMBER_COL + "='" + callItem.getNumber() + "'";
			  Cursor cursor = db.query(DBHelper.TABLE_NAME, new String[] { DBHelper.ID_COL }, where, null, null, null, null, "1");
			  Integer id = null;
			  if (cursor.moveToFirst()) {
			         do {
			            id = cursor.getInt(0);
			         } while (cursor.moveToNext());
			  }
		   	  if (cursor != null && !cursor.isClosed()) {
		   			cursor.close();
		   	  }
		   	  if (id != null) db.delete(DBHelper.TABLE_NAME, DBHelper.ID_COL + "=?", new String[] { id.toString() });
	   		  // Insert
	          ContentValues values = new ContentValues();
	          values.put(DBHelper.FULLNAME_COL, callItem.getFullname());
	          values.put(DBHelper.NUMBER_COL, callItem.getNumber());
	          values.put(DBHelper.GIVENNAME_COL, callItem.getGivenName());
	          values.put(DBHelper.SN_COL, callItem.getSn());
	          values.put(DBHelper.ORG_COL, callItem.getOrg());
	          values.put(DBHelper.STATUS_COL, CallItem.STATUS_CI_NORMAL);
	          int millis = (int) (System.currentTimeMillis() / 1000);
	          values.put(DBHelper.TIMESTAMP_COL, millis);
	          long rowId = db.insert(DBHelper.TABLE_NAME, DBHelper.TIMESTAMP_COL, values);
	      	} finally {
	          db.close();
	        }   	  
	  }

		  public static void trimHistory(Context context) {
		    SQLiteOpenHelper helper = new DBHelper(context);
		    SQLiteDatabase db = helper.getWritableDatabase();
		    Cursor cursor = null;
		    try {
		    	cursor = db.query(DBHelper.TABLE_NAME,
		                        ID_COL_PROJECTION,
		                        null, null, null, null,
		                        DBHelper.TIMESTAMP_COL + " DESC");
		    	int count = 0;
		    	while (count < MAX_ITEMS && cursor.moveToNext()) {
		    		count++;
		    	}
		    	//List<String> items = new ArrayList<String>();
		    	while (cursor.moveToNext()) {
		  		  	db.delete(DBHelper.TABLE_NAME, DBHelper.ID_COL + '=' + cursor.getInt(0), null);
		  		  	count++;
		    	}
		    } finally {
		    	if (cursor != null) {
		    		cursor.close();
		    	}
		    	db.close();
		    }
		  }
		  
		  void clearHistory() {
			  SQLiteOpenHelper helper = new DBHelper(context);
			  SQLiteDatabase db = helper.getWritableDatabase();
			  try {
			      db.delete(DBHelper.TABLE_NAME, null, null);
			  } finally {
			      db.close();
			  }
		  }
}
