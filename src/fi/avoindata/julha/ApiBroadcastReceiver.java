package fi.avoindata.julha;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.zxing.client.android.AndroidHttpClient;

import fi.avoindata.julha.history.CallHistoryManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class ApiBroadcastReceiver extends BroadcastReceiver {

	private static final String TAG = "ApiBroadcastReceiver";
	String org = "";
	String name = "";

	Context context;
	
	private static final String USER_AGENT = "Puhelu (Android)";
	private NetworkThread networkThread;
	
	private final Handler handler = new Handler() {
	    @Override
	    public void handleMessage(Message message) {
	      switch (message.what) {
	        case R.id.search_succeeded:
	        	Log.i(TAG,"search_succeeded");
	        	handleSearchResults((JSONObject) message.obj);
	        	resetForNewQuery();
	        	break;
	        case R.id.search_failed:
	        	Log.i(TAG,"search_failed");
	        	resetForNewQuery();
	            break;
	      }
	    }
	  };
	@Override
	public void onReceive(Context context, Intent intent) {
		Bundle bundle = intent.getExtras();
		this.context = context;
		
        if(null == bundle)
                return;
        
        Log.i(TAG,bundle.toString());
        
        String state = bundle.getString(TelephonyManager.EXTRA_STATE);
                        
        Log.i(TAG,"State: "+ state);
        
        if(state.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_RINGING))
        {
                String phonenumber = bundle.getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
                        
                Log.i(TAG,"Incoming Number: " + phonenumber);
                Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phonenumber));
                Cursor cursor = context.getContentResolver().query(uri, new String[]{PhoneLookup.DISPLAY_NAME},null,null,null);
                try {
                	while(cursor.moveToNext()){
                	    String contactName = cursor.getString(cursor.getColumnIndexOrThrow(PhoneLookup.DISPLAY_NAME));
                	    Log.d(TAG, "contactMatch name: " + contactName);
                	    return;
                	    
                	}

                } finally {
                		cursor.close();
                }
                startNetworkSearch(phonenumber);
        	}

	}
	  private void handleSearchResults(JSONObject json) {
		  try {
		      String name = json.getString("name");
		      String org = json.getString("org");
		      String number = json.getString("number");
		      String givenName = json.getString("givenName");
		      String sn = json.getString("sn");

		      if (name.equals(""))Toast.makeText(context,R.string.toast_no_callinfo, Toast.LENGTH_LONG).show();
              else Toast.makeText(context,name + context.getResources().getString(R.string.toast_callinfo) + "\n" + org, Toast.LENGTH_LONG).show();
		      
        	  CallItem callItem = new CallItem();
        	  callItem.setFullname(name);
        	  callItem.setGivenName(givenName);
        	  callItem.setSn(sn);
        	  callItem.setOrg(org);
        	  callItem.setNumber(number);
        	  CallHistoryManager.addCallItem(context, callItem);
		} catch (JSONException e) {	    	
	        Log.w(TAG, "Bad JSON", e);
	    }
	  }
	  private void resetForNewQuery() {
		    networkThread = null;
	  }
	 private void startNetworkSearch(String query) {
		    if (networkThread == null) {
		      if (query != null && query.length() > 0) {
		        networkThread = new NetworkThread(query, handler);
		        networkThread.start();
		      }
		    }
		  }
		  
		  private static final class NetworkThread extends Thread {
			    private final String query;
			    private final Handler handler;

			    NetworkThread(String query, Handler handler) {
			      this.query = query;
			      this.handler = handler;
			    }

			    @Override
			    public void run() {
			      AndroidHttpClient client = null;
			      Log.i(TAG,"NetworkThread: " + query);
			      try {
			    	  Locale l = Locale.getDefault();
			  	      String lang = l.getLanguage();
			    	  URI uri= new URI("http", null, "www.botsbot.com", -1, "/puhelu/ci.php", "lang=" + lang + "&out=json&pn=" + query, null);
			        
			          HttpUriRequest get = new HttpGet(uri);
			          //get.setHeader("cookie", getCookie(uri.toString()));
			          client = AndroidHttpClient.newInstance(USER_AGENT);
			          HttpResponse response = client.execute(get);
			          if (response.getStatusLine().getStatusCode() == 200) {
				          HttpEntity entity = response.getEntity();
				          ByteArrayOutputStream jsonHolder = new ByteArrayOutputStream();
				          entity.writeTo(jsonHolder);
				          jsonHolder.flush();
				          JSONObject json = new JSONObject(jsonHolder.toString());
				          jsonHolder.close();
			
				          Message message = Message.obtain(handler, R.id.search_succeeded);
				          message.obj = json;
				          message.sendToTarget();
			          } else {
				          Log.w(TAG, "HTTP returned " + response.getStatusLine().getStatusCode() + " for " + uri);
				          Message message = Message.obtain(handler, R.id.search_failed);
				          message.sendToTarget();
			        }
			      } catch (Exception e) {
			        Log.w(TAG, "Error accessing search", e);
			        Message message = Message.obtain(handler, R.id.search_failed);
			        message.sendToTarget();
			      } finally {
			        if (client != null) {
			          client.close();
			        }
			      }
			    }
		  }

}
