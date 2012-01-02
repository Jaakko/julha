package fi.avoindata.julha;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.zxing.client.android.AndroidHttpClient;

import fi.avoindata.julha.history.CallHistoryManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class JulhaActivity extends ListActivity {
	CallListAdapter cla;
    ListView listView;
    List<CallItem> callList;
    
    private NetworkThread networkThread;
	private static final String TAG = JulhaActivity.class.getSimpleName();
	private static final String USER_AGENT = "Puhelu (Android)";
	private final Handler handler = new Handler() {
	    @Override
	    public void handleMessage(Message message) {
	      switch (message.what) {
	        case R.id.post_succeeded:
	        	Log.i(TAG,"post_succeeded");
	        	handlePostResult((JSONObject) message.obj);
	        	resetForNewQuery();
	        	break;
	        case R.id.post_failed:
	        	Log.i(TAG,"post_failed");
	        	resetForNewQuery();
	            break;
	      }
	    }
	  };
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calllist);
        listView = getListView(); 
        
        callList = CallHistoryManager.getCallItems(this);
        cla = new CallListAdapter(this, callList);

        listView.setAdapter(cla);
        
        OnItemClickListener oicl = new OnItemClickListener() {
        	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        		
        		if (callList.size() == 0) return;
                final Bundle callItem = ((CallItem) callList.get(position)).getBundle();
                AlertDialog.Builder builder = new AlertDialog.Builder(JulhaActivity.this);
                builder.setMessage(getResources().getString(R.string.dialog_spam_text))
                       .setCancelable(false)
                       .setPositiveButton(getResources().getString(R.string.dialog_yes), new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface dialog, int id) {
                        	   Log.i(TAG, "Spam: " + callItem.getString(CallItem.FULLNAME));
                        	   startNetworkPost(callItem.getString(CallItem.NUMBER), "spam");
                        	   dialog.cancel();
                           }
                       })
                       .setNegativeButton(getResources().getString(R.string.dialog_no), new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                           }
                       });
                AlertDialog alert = builder.create();
                alert.show();
        	}
        };
        listView.setOnItemClickListener(oicl);
    }
    @Override
    protected void onResume() {
        super.onResume();
        // The activity has become visible (it is now "resumed").
        callList = CallHistoryManager.getCallItems(this);
        cla = new CallListAdapter(this, callList);
    	listView.setAdapter(cla);
    	cla.notifyDataSetChanged();
    }
    private void handlePostResult(JSONObject json) {
		  try {
		      String response = json.getString("response");
		      Toast.makeText(this,response, Toast.LENGTH_LONG).show();
		} catch (JSONException e) {	    	
	        Log.w(TAG, "Bad JSON", e);
	    }
	  }
	  private void resetForNewQuery() {
		    networkThread = null;
	  }
	 private void startNetworkPost(String number, String reason) {
		    if (networkThread == null) {
		      if (number != null && number.length() > 0) {
		        networkThread = new NetworkThread(number, reason, handler);
		        networkThread.start();
		      }
		    }
		  }
		  
		  private static final class NetworkThread extends Thread {
			    private final String number;
			    private final String reason;
			    private final Handler handler;

			    NetworkThread(String number, String reason, Handler handler) {
			      this.number = number;
			      this.reason = reason;
			      this.handler = handler;
			    }

			    @Override
			    public void run() {
			      AndroidHttpClient client = null;
			      Log.i(TAG,"NetworkThread: " + number);
			      try {
			    	  Locale l = Locale.getDefault();
			  	      String lang = l.getLanguage();
			    	  URI uri= new URI("http", null, "www.botsbot.com", -1, "/puhelu/ci.php", "lang=" + lang + "&out=json&pn=" + number, null);
			        
			    	  HttpPost post = new HttpPost(uri);
			       // Add your data
			          List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			          nameValuePairs.add(new BasicNameValuePair("number", number));
			          nameValuePairs.add(new BasicNameValuePair("reason", reason));
			          post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			          //get.setHeader("cookie", getCookie(uri.toString()));
			          client = AndroidHttpClient.newInstance(USER_AGENT);
			          HttpResponse response = client.execute(post);
			          if (response.getStatusLine().getStatusCode() == 200) {
				          HttpEntity entity = response.getEntity();
				          ByteArrayOutputStream jsonHolder = new ByteArrayOutputStream();
				          entity.writeTo(jsonHolder);
				          jsonHolder.flush();
				          JSONObject json = new JSONObject(jsonHolder.toString());
				          jsonHolder.close();
			
				          Message message = Message.obtain(handler, R.id.post_succeeded);
				          message.obj = json;
				          message.sendToTarget();
			          } else {
				          Log.w(TAG, "HTTP returned " + response.getStatusLine().getStatusCode() + " for " + uri);
				          Message message = Message.obtain(handler, R.id.post_failed);
				          message.sendToTarget();
			        }
			      } catch (Exception e) {
			        Log.w(TAG, "Error accessing search", e);
			        Message message = Message.obtain(handler, R.id.post_failed);
			        message.sendToTarget();
			      } finally {
			        if (client != null) {
			          client.close();
			        }
			      }
			    }
		  }
    
    public static class CallListAdapter extends BaseAdapter {
    	private LayoutInflater mInflater;
    	private Activity activity;
    	private List<CallItem> callList;
    	Locale locale;
    	
    	public CallListAdapter(Activity activity, List<CallItem> callList) {
    		this.activity = activity;
    		this.mInflater = LayoutInflater.from(activity);	
    		this.callList = callList;
    		locale = Locale.getDefault();
    	}
    	public int getCount() {
    		return callList.size();
    	}
    	
    	public Object getItem(int position) {
    		return position;
    	}
    	
    	public long getItemId(int position) {
    		return position;
    	}
    	
    	public View getView(int position, View convertView, ViewGroup parent) {
    		ViewHolder holder;
    		if (convertView == null) {
    			convertView = mInflater.inflate(R.layout.calllist_item, null);
    			holder = new ViewHolder();
    			holder.name = (TextView) convertView.findViewById(R.id.calllist_name);
    			holder.date = (TextView) convertView.findViewById(R.id.calllist_date);
    			holder.org = (TextView) convertView.findViewById(R.id.calllist_org);
    			
    			convertView.setTag(holder);
    		} else {
    			holder = (ViewHolder) convertView.getTag();
    		}  		
    		holder.org.setText(callList.get(position).getOrg());  
    		String name = callList.get(position).getFullname();
    		if (name == null || name.equals("")) name = callList.get(position).getNumber();
    		holder.name.setText(name);
    		 		
    		
    		Calendar cal = Calendar.getInstance();
    		cal.setTimeInMillis(((long) callList.get(position).getTimestamp()) * 1000);
    		String date = DateFormat.format("dd MMMM h:mmaa",cal).toString();
    		holder.date.setText(date);
    		
    		return convertView;
    	}
    	
    	static class ViewHolder {
    		TextView name;
    		TextView org;
    		TextView date;
    	}
    }
}