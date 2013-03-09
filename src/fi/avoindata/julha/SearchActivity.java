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
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.zxing.client.android.AndroidHttpClient;

import fi.avoindata.julha.history.CallHistoryManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.text.ClipboardManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class SearchActivity extends ListActivity {
	CallListAdapter cla;
    ListView listView;
    List<CallItem> callList;
    
    private NetworkThread networkThread;
	private static final String TAG = SearchActivity.class.getSimpleName();
	private static final String USER_AGENT = "Puhelu (Android)";
	private final Handler handler = new Handler() {
	    @Override
	    public void handleMessage(Message message) {
	      switch (message.what) {
	        case R.id.get_succeeded:
	        	Log.i(TAG,"get_succeeded");
	        	handleGetResult((JSONObject) message.obj);
	        	resetForNewQuery();
	        	break;	
	        case R.id.post_failed:
	        	Log.i(TAG,"post_failed");
	        	resetForNewQuery();
	            break;
	      }
	    }
	  };
	private ProgressDialog dialog;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.i(TAG, "Search started");
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        
        setContentView(R.layout.calllist);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.calllist_title);
        
               
        TextView title = (TextView) findViewById(R.id.calllist_title_name);
        title.setText(R.string.searchactivity_title);
        listView = getListView(); 
        
        callList = CallHistoryManager.getCallItems(this);
        cla = new CallListAdapter(this, callList);

        listView.setAdapter(cla);
        
        handleIntent(getIntent());

        
        OnItemClickListener oicl = new OnItemClickListener() {
        	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        		
        		if (callList.size() == 0) return;
                final Bundle callItem = ((CallItem) callList.get(position)).getBundle();
                AlertDialog alert = buildAlert(callItem);
                alert.show();
        	}
        };
        
        final ImageButton button = (ImageButton) findViewById(R.id.button_calllist);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
            	onSearchRequested();
            }
        });

        listView.setOnItemClickListener(oicl);
        registerForContextMenu(getListView());
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.menu_search:
        	onSearchRequested();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
      super.onCreateContextMenu(menu, v, menuInfo);
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.context_menu, menu);
    }
    @Override
    public boolean onContextItemSelected(MenuItem item) {
      AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
      switch (item.getItemId()) {
      case R.id.menu_store_contact:
    	  storeContact(info.position);
    	  return true;
      case R.id.menu_share_contact:
    	  shareContact(info.position);
    	  return true;
    /* 
      case R.id.menu_copypaste: 
    	  // Gets a handle to the clipboard service.
    	  ClipboardManager clipboard = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
    	  ClipData clip = ClipData.newPlainText("simple text","Hello, World!");
    	  return true; */
      default:
    	  return super.onContextItemSelected(item);
      }
    }
    private void shareContact(int position){
    	final Bundle callItem = ((CallItem) callList.get(position)).getBundle();
 	   	String fullname = callItem.getString(CallItem.FULLNAME);
 	   	String number = callItem.getString(CallItem.NUMBER);
 	   	String org = callItem.getString(CallItem.ORG);
	   //store contact
 	   	Log.i(TAG, "Sharing contact...");
		Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);   	   
		shareIntent.setType("text/plain");
		shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getText(R.string.share_contact));
		String entry = fullname + "\n";
		entry += number + "\n";
		entry += org + "\n";

		shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, entry);
		
		startActivity(Intent.createChooser(shareIntent, getText(R.string.share_contact)));    
    }
    private void storeContact(int position){
    	final Bundle callItem = ((CallItem) callList.get(position)).getBundle();
 	   	String fullname = callItem.getString(CallItem.FULLNAME);
	   //store contact
 	   	Log.i(TAG, "Storing contact...");
 	   	ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
 	   	int rawContactInsertIndex = ops.size();

 	   	ops.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
	      .withValue(RawContacts.ACCOUNT_TYPE, null)
	      .withValue(RawContacts.ACCOUNT_NAME,null )
	      .build());
 	   	ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
	      .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
	      .withValue(Data.MIMETYPE,Phone.CONTENT_ITEM_TYPE)
	      .withValue(Phone.NUMBER, callItem.getString(CallItem.NUMBER))
	      .build());
 	   	ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
	      .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
	      .withValue(Data.MIMETYPE,StructuredName.CONTENT_ITEM_TYPE)
	      .withValue(StructuredName.DISPLAY_NAME, fullname)
	      .build()); 
 	   	ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
 		      .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
 		      .withValue(Data.MIMETYPE,ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
 		      .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, callItem.getString(CallItem.ORG))
 		      .withValue(ContactsContract.CommonDataKinds.Organization.TYPE, ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
 		      .build());
	   try {
		ContentProviderResult[] res = getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
	} catch (RemoteException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (OperationApplicationException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}

    }
    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
          String query = intent.getStringExtra(SearchManager.QUERY);
          doMySearch(query);
        }
    }
    private void doMySearch(String query) {
		// TODO Auto-generated method stub
    	Log.i(TAG, "Doing search: " + query);
    	dialog = ProgressDialog.show(this, "", getText(R.string.dialog_searching), true);
    	startNetworkGet(query);
	}
    
    @Override
    public boolean onSearchRequested() {
     //   pauseSomeStuff();
    	Log.i(TAG, "onserachrequested");
    	//return true;
        return super.onSearchRequested();
    }
	@Override
    protected void onResume() {
        super.onResume();
    }
    private void handleGetResult(JSONObject json) {
		try {
				JSONArray ar = json.getJSONArray("items");
				List<CallItem> items = new ArrayList<CallItem>();
				for (int i=0; i<ar.length();i++){
					CallItem ci = new CallItem();
					JSONObject j = ar.getJSONObject(i);
					ci.setFullname(j.getString("name"));
					ci.setLocation(j.getString("location"));
					ci.setStreet(j.getString("street"));
					ci.setUnit(j.getString("unit"));
					ci.setEntity(j.getString("entity"));
					ci.setOrg(j.getString("org"));
					ci.setTitle(j.getString("title"));
					ci.setNumber(j.getString("number"));
					items.add(ci);
					Log.i(TAG, "name: " + j.getString("name"));
				}
		        callList = items;
		        cla = new CallListAdapter(this, callList);
		    	listView.setAdapter(cla);
		    	cla.notifyDataSetChanged();
				//Toast.makeText(this,response, Toast.LENGTH_LONG).show();
		} catch (JSONException e) {	    	
		    Log.w(TAG, "Bad JSON", e);
		}
	}
    private AlertDialog buildAlert(Bundle callItem){
    	final Bundle ci = callItem;
    	String dialogText;
    	String fullname = ci.getString(CallItem.FULLNAME);
    	Log.i(TAG, "Spam: " + fullname);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	dialogText = getResources().getString(R.string.dialog_call)
    	+ "\n" + ci.getString(CallItem.NUMBER)
    	+ "\n\n" + fullname
    	+ "\n" + ci.getString(CallItem.TITLE)
    	+ "\n" + ci.getString(CallItem.ORG)
    	+ "\n" + ci.getString(CallItem.UNIT);	
    	
    	if (!ci.getString(CallItem.LOCATION).equals("")) dialogText += "\n" + ci.getString(CallItem.LOCATION);
    	if (!ci.getString(CallItem.STREET).equals("")) dialogText += "\n" + ci.getString(CallItem.STREET);

    	builder.setMessage(dialogText)
               .setCancelable(false)
               .setPositiveButton(getResources().getString(R.string.dialog_yes), new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                	   	String url = "tel:" + (ci.getString(CallItem.NUMBER)).trim();
                	    Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse(url));
                	    startActivity(intent);
                	    dialog.cancel();
                   }
              })
               .setNegativeButton(getResources().getString(R.string.dialog_no), new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                   }
               });
        AlertDialog alert = builder.create();
        return alert;
    }
    
    private void resetForNewQuery() {
		    networkThread = null;
		    dialog.cancel();
	}


    private void startNetworkGet(String query) {
    	if (networkThread == null) {
    		if (query != null && query.length() > 0) {
    			networkThread = new NetworkThread(query, handler);
    			networkThread.start();
    		}
	    }
    }	  
    private static final class NetworkThread extends Thread {
	    private final String search;
	    private final Handler handler;

	    NetworkThread(String search, Handler handler) {
	      this.search = search;
	      this.handler = handler;
	    }

	    @Override
	    public void run() {
	      AndroidHttpClient client = null;
	      Log.i(TAG,"NetworkThread: " + search);
	      try {
	    	  Locale l = Locale.getDefault();
	    	  String lang = l.getLanguage();
	    	  client = AndroidHttpClient.newInstance(USER_AGENT);
	    	  
	    	  
    		  URI uri= new URI("http", null, "www.botsbot.com", -1, "/puhelu/search.php", "lang=" + lang + "&out=json&q=" + search, null);
    		  HttpGet req = new HttpGet(uri);
    		  HttpResponse response = client.execute(req);
              
	          if (response.getStatusLine().getStatusCode() == 200) {
	        	  HttpEntity entity = response.getEntity();
	        	  ByteArrayOutputStream jsonHolder = new ByteArrayOutputStream();
	        	  entity.writeTo(jsonHolder);
	        	  jsonHolder.flush();
	        	  JSONObject json = new JSONObject(jsonHolder.toString());
	        	  jsonHolder.close();
	        	  Message message = Message.obtain(handler, R.id.get_succeeded);
		    	  
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
    			holder.number = (TextView) convertView.findViewById(R.id.calllist_number);
    			holder.org = (TextView) convertView.findViewById(R.id.calllist_org);
    			
    			convertView.setTag(holder);
    		} else {
    			holder = (ViewHolder) convertView.getTag();
    		}  		
    		holder.org.setText(callList.get(position).getOrg());  
    		String name = callList.get(position).getFullname();
    		holder.name.setText(name);
    		holder.number.setText(callList.get(position).getNumber());
    		
    		return convertView;
    	}
    	
    	static class ViewHolder {
    		TextView name;
    		TextView org;
    		TextView number;
    	}
    }

}
