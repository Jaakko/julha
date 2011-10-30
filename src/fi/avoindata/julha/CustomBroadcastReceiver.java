package fi.avoindata.julha;

import java.util.Enumeration;
import java.util.Iterator;

import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPAttributeSet;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPDN;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSearchResults;
import com.novell.ldap.util.Base64;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;
import android.provider.ContactsContract.PhoneLookup;

public class CustomBroadcastReceiver extends BroadcastReceiver {

	private static final String TAG = "CustomBroadcastReceiver";
	String org = "";
	String name = "";
	
	@Override
	public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        
        if(null == bundle)
                return;
        
        Log.i(TAG,bundle.toString());
        
        String state = bundle.getString(TelephonyManager.EXTRA_STATE);
                        
        Log.i(TAG,"State: "+ state);
        
        if(state.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_RINGING))
        {
                String phonenumber = bundle.getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
                        
                Log.i(TAG,"Incomng Number: " + phonenumber);
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
                
                int searchScope = LDAPConnection.SCOPE_SUB;
                int ldapVersion  = LDAPConnection.LDAP_V3;
                String searchBase = "dc=julkishallinto, c=fi";

                //Testinumero
                //phonenumber = "+358 40 536 8056";
                String searchFilter = "(|(mobileTelephoneNumber=*" + phonenumber + "*)" +
                						"(telephoneNumber=*" + phonenumber + "*))";

                LDAPConnection lc = new LDAPConnection();

                try {
                   // connect to the server
                   lc.connect( "ldap.julha.fi", 3800);

                   // bind to the server
                   Log.i(TAG,"connecting to LDAP");
                    
                   lc.bind( ldapVersion, "dc=julkishallinto, c=fi", "".getBytes("UTF8"));
                   Log.i(TAG,"bind to LDAP");
                   if(lc.isConnected())Log.i("IncomingCallReceiver","is connected"); 	

                   LDAPSearchResults searchResults =
                        lc.search(  searchBase,
                                    searchScope,
                                    searchFilter,
                                    null,         // return all attributes
                                    false);       // return attrs and values

                    Log.i(TAG,"searching LDAP: " + searchFilter);
                    /* To print out the search results,
                     *   -- The first while loop goes through all the entries
                     *   -- The second while loop goes through all the attributes
                     *   -- The third while loop goes through all the attribute values
                     */
                    Log.i(TAG,searchResults.toString());
                    while ( searchResults.hasMore()) {
                    	LDAPEntry nextEntry = null;
                        try {
                            nextEntry = searchResults.next();
                        } catch(LDAPException e) {
                        	 Log.i(TAG,"Error: " + e.toString());
                        	 // Exception is thrown, go for next entry
                        	 if(e.getResultCode() == LDAPException.LDAP_TIMEOUT || e.getResultCode() == LDAPException.CONNECT_ERROR)
                        		 break;
                            else
                               continue;
                        }
                        Log.i(TAG,nextEntry.getDN());
                        String[] strs = LDAPDN.explodeDN(nextEntry.getDN(),false);
                        
                        int size = strs.length;
                        for (int i=0; i<size; i++)
                        {
                            Log.i(TAG,"strs["+i+"]: " + strs[i]);
                            if (strs[i].startsWith("o=")) org = strs[i].substring(2); 
                        }

                        Log.i(TAG,"  Attributes: ");
                        LDAPAttributeSet attributeSet = nextEntry.getAttributeSet();
                        Iterator allAttributes = attributeSet.iterator();
                      
                        while(allAttributes.hasNext()) {
                            LDAPAttribute attribute =
                                        (LDAPAttribute)allAttributes.next();

                            String attributeName = attribute.getName();
                            Log.i(TAG,"    " + attributeName);

                            Enumeration allValues = attribute.getStringValues();
                            if( allValues != null) {
                                while(allValues.hasMoreElements()) {
                                    String Value = (String) allValues.nextElement();
                                    if (Base64.isLDIFSafe(Value)) {
                                       // is printable
                                    	Log.i(TAG,"      " + Value);
                                    } else {
                                    	// base64 encode and then print out
                                        Value = Base64.encode(Value.getBytes());
                                        Log.i(TAG,"      " + Value);
                                    }
                                    if (attributeName.equals("cn")) {
                                    	name = Value;
                                    	Log.i(TAG,"      Commmonname: " + name);
                                    }
                                }
                            }
                        }
                    }
                   // disconnect with the server
                   Log.i(TAG,"disconnecting");
                   Log.i(TAG,name + " soittaa, " + org);
                   if (name.equals(""))Toast.makeText(context,"Tuntematon numero", Toast.LENGTH_LONG).show();
                   else Toast.makeText(context,name + " soittaa\n" + org, Toast.LENGTH_LONG).show();
                   lc.disconnect();
                }catch(Exception e){
                    Log.i(TAG,"exseption: " + e.getMessage());
                    e.printStackTrace();  
                }
        	}
        }
	}