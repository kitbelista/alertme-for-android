/**
 * 
 * Copyright 2011 Kathlene Belista
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.darkgoddess.alertme;

import java.util.ArrayList;
import java.util.HashMap;

import org.darkgoddess.alertme.api.AlertMeSession;
import org.darkgoddess.alertme.api.AlertMeStorage;
import org.darkgoddess.alertme.api.utils.APIUtilities;
import org.darkgoddess.alertme.api.utils.Device;
import org.darkgoddess.alertme.api.utils.Hub;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;



public class AlertMePeople extends Activity {
	private static final String TAG = "ACTIVITY:AlertMePeople";
	private Bundle savedState = null;
	private AMViewItems screenStuff = null;
	private AlertMeSession alertMe = null;
	private boolean isActive = false;
	private boolean hasInitView = false;
	private boolean hasCreated = false;
	private TextView accountUsername = null;
	private ListView peopleList = null;
	private TextView accountFirstName = null;
	private TextView accountSurname = null;
	private TextView accountHub = null;
	private ArrayList<Device> keyFobs = null;
	private int[] rowBg = null;

	// Handler to update the interface..        
	final Handler handler = new Handler() {
	    public void handleMessage(Message msg) {
	        int mesgType = msg.getData().getInt("type");
	        String first = msg.getData().getString("firstname");
	        String surname = msg.getData().getString("lastname");
	        performUpdate(mesgType, first, surname);
	    }
	};
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		alertMe = (alertMe==null)? new AlertMeSession(this): alertMe;
		isActive = true;
		loadFromRestoredState();
		if (!hasCreated) {
			hasCreated = true;
			savedState = savedInstanceState;
			initView();
		}
		rowBg = AMViewItems.getRowColours(this);
	}
	@Override
    public void onStart() {
		super.onStart();

		isActive = true;
		loadFromRestoredState();

		initScreenStuff();
		if (alertMe.devicesRequiresRefresh()) {
			SharedPreferences sharedPrefs = getSharedPreferences(AlertMeConstants.PREFERENCE_NAME, AlertMeConstants.PREFERENCE_MODE);
			PeopleListStarter listloader = new PeopleListStarter(alertMe, handler, getIntent(), savedState, sharedPrefs);
			screenStuff.setBusy(AlertMeConstants.UPDATE_PEOPLE);
			listloader.start();
		}
	}
	@Override
	protected void onPause() {
        super.onPause();
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onPause()  START");
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onPause()  END");
    }
	@Override
	protected void onResume() {
        super.onResume();
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onResume()  START");
		//loadFromRestoredState();
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onResume()  END");
	}
	@Override
    protected void onRestart() {
    	super.onRestart();
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onRestart()  START");
		//loadFromRestoredState();
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onRestart()  END");
    }
	@Override
    protected void onStop() {
		super.onStop();
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onStop()  START");
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onStop()  END");
	}
	@Override
    protected void onDestroy() {
    	super.onDestroy();
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onDestroy()  START");
		if (alertMe!=null) {
			alertMe.clean();
		}
		if (screenStuff!=null) {
			screenStuff.clean();
		}
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onDestroy()  END");
    }
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onSaveInstanceState()  START");
		alertMe.onSaveInstanceState(outState);
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onSaveInstanceState()  END");
	}
	@Override
	protected Dialog onCreateDialog(int id) {
    	Dialog res = screenStuff.onCreateDialog(id);
        return res;
    }
	@Override
	public Object onRetainNonConfigurationInstance() {
		PeopleState saveState = new PeopleState();
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onRetainNonConfigurationInstance()  START");
		saveState.sessionState = alertMe.retrieveCurrentState();
		saveState.keyFobs = keyFobs;
		saveState.currentUser = alertMe.getCurrentSession();
		if (saveState.currentUser!=null) if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onRetainNonConfigurationInstance()  userdetails [id:"+saveState.currentUser.id+"; username:"+saveState.currentUser.username+"; info:"+saveState.currentUser.info+"]");
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onRetainNonConfigurationInstance()  END");
		return saveState;
	}
	@Override
	public void finish() {
		isActive = false;
		alertMe.clean();
		super.finish();
		
	}
	
	private void initView() {
		if (!hasInitView) {
			hasInitView = true;
			setContentView(R.layout.alertme_people);
			peopleList = (ListView) findViewById(R.id.keyfob_list);
			accountFirstName = (TextView) findViewById(R.id.account_firstname);
			accountSurname = (TextView) findViewById(R.id.account_lastname);
			accountUsername = (TextView) findViewById(R.id.account_username);
			accountHub = (TextView) findViewById(R.id.account_hub);
		}
	}
	private void loadCurrentUserDetails(String firstname, String lastname) {
		if (accountFirstName!=null) {
			if (APIUtilities.isStringNonEmpty(firstname)) accountFirstName.setText(firstname);
			else {
				TextView label = (TextView) findViewById(R.id.account_firstname_label);
				label.setVisibility(View.GONE);
			}
		}
		if (accountSurname!=null) {
			if (APIUtilities.isStringNonEmpty(lastname)) accountSurname.setText(lastname);
			else {
				TextView label = (TextView) findViewById(R.id.account_lastname_label);
				label.setVisibility(View.GONE);
			}	
		}
	}
    private String getPeopleString() {
    	// Call the client for the current people cached..
    	String personValue = getString(R.string.people_unavailable);
		String countTag = getString(R.string.people_count_tag);
    	int personCount = 0;
    	if (keyFobs!=null) {
        	for(Device keyfob: keyFobs) {
        		if (keyfob.attributes.containsKey("presence")) {
        			String pres = (String) keyfob.attributes.get("presence");
        			if (pres!=null) {
        				pres = pres.trim().toLowerCase();
            			personCount += (pres.equals("true"))? 1: 0;
        			}
        		}
        	}
        	if (personCount == 0) {
        		personValue = getString(R.string.people_none_present);
        	} else if (personCount == keyFobs.size()) {
        		personValue = getString(R.string.people_all_present);
        	} else if (personCount == 1) {
        		personValue = getString(R.string.people_single_present);
        	} else if (personCount > 1) {
        		personValue = getString(R.string.people_multiple_present);
        	}
    	}
		if (personValue.contains(countTag)) {
			personValue = personValue.replace(countTag, personCount+"");
		}    		
		return personValue;
    }
    private void performUpdate(int command, final String first, final String last) {
    	AlertMeStorage.AlertMeUser currentUser = alertMe.getCurrentSession();
    	Hub hub = alertMe.retrieveActiveHub();
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate("+command+", '"+first+"', '"+last+"')  START");

    	initScreenStuff();
    	initView();
    	if (!isActive) {
    		screenStuff.setNotBusy();
    		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  END  - failed isActive");
    		return;
    	}
    	
    	if (APIUtilities.isStringNonEmpty(first) && APIUtilities.isStringNonEmpty(last) ) {
    		loadCurrentUserDetails(first, last);
    	}
    	if (currentUser!=null) {
    		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  updating currentUser:"+currentUser.username);
        	if (accountUsername!=null) accountUsername.setText(currentUser.username);    		
    	}
    	// LOAD THE KEYFOB LIST
    	if (peopleList!=null) {
    		if (keyFobs!=null && !keyFobs.isEmpty()) {
    			KeyFobAdapter deviceAd = new KeyFobAdapter(this, R.layout.alertme_people_row, keyFobs);
        		peopleList.setAdapter(deviceAd);
        		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  updating keyfob list size:"+keyFobs.size());
    		}
    		else {
     			// empty
         		ArrayAdapter<String> emptyList;
     			String[] empty = { getString(R.string.keyfob_list_isempty) }; 
     			emptyList = new ArrayAdapter<String>(this, R.layout.alertme_listempty, empty);
     			peopleList.setAdapter(emptyList);
        		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  updating keyfob list size: 0");
     		}
    	}
    	if (hub!=null && accountHub!=null) {
    		String peopleString = getPeopleString();
    		accountHub.setText(hub.name+ " - "+peopleString);
    		screenStuff.setSystemName(hub);
    		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  updating hub:"+hub.name);
    	}
		screenStuff.setNotBusy();
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  END");
    }
	
	private void loadFromRestoredState() {
		final Object data = getLastNonConfigurationInstance();
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "loadFromRestoredState()  START");
		if (data != null) {
			PeopleState savedState = (PeopleState) data;
			final AlertMeSession.SessionState oldState = savedState.sessionState;
			boolean reloaded = false;
			if (alertMe==null) {
				alertMe = new AlertMeSession(this);
			}
			if (savedState.keyFobs!=null) keyFobs = savedState.keyFobs;
			reloaded = alertMe.loadFromCachedState(this, oldState);

			if (reloaded) {
				AlertMeStorage.AlertMeUser currentUser = savedState.currentUser;
				if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "loadFromRestoredState()  currentUser: "+currentUser+"; info:"+currentUser.info);
				restoreDetailsFromCache(currentUser);
			}
			if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "loadFromRestoredState()  did reload from old state:"+reloaded);
		}
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "loadFromRestoredState()  END");
	}
	

    class KeyFobAdapter extends ArrayAdapter<Device> {
		private ArrayList<Device> items;

        public KeyFobAdapter(Context context, int textViewResourceId, ArrayList<Device> list) {
                super(context, textViewResourceId, list);
                items = list;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = vi.inflate(R.layout.alertme_people_row, null);
                }
                Device k = items.get(position);
                if (k != null) {
                	String pres = k.getAttribute("presence");
                	boolean isPres = pres.equals("True");
                    TextView name = (TextView) v.findViewById(R.id.person_name);
            		ImageView sigIcon = (ImageView) v.findViewById(R.id.person_signal_icon);
            		ImageView presIcon = (ImageView) v.findViewById(R.id.person_presence_icon);
                    TextView mesg = (TextView) v.findViewById(R.id.person_status);
                	String status = (isPres)? "At home": "Away";
                    
                    if (name!=null) name.setText(k.name);
                    if (sigIcon!=null) sigIcon.setImageResource(AlertMeConstants.getSignalIcon(k));
                    if (presIcon!=null) {
                		int presR = (isPres)? R.drawable.ic_keyfob_present: R.drawable.ic_keyfob_offline;
                		presIcon.setImageResource(presR);
                    }
                    if (mesg!=null) mesg.setText(status);
                }
                if (rowBg!=null) {
                	int colorPos = position % rowBg.length;
                	v.setBackgroundColor(rowBg[colorPos]);
                }
                return v;
        }
    }
	class PeopleListStarter extends Thread {
		public boolean forceLogin = true;
		public int instruction = -1;
		private AlertMeSession alertme;
		private Handler handler;
		private Intent intent;
		private Bundle bundle;
		private SharedPreferences sharedPrefs;

		public PeopleListStarter(AlertMeSession client, Handler handle, Intent intentIn, Bundle bundleIn, SharedPreferences prefs) {
			alertme = client;
			handler = handle;
			intent = intentIn;
			bundle = bundleIn;
			sharedPrefs = prefs;
		}
        @Override
        public void run() {
      		boolean hasCurrentSys = alertme.hasSessionValues();
			String firstname = null;
			String lastname = null;
    		if (!hasCurrentSys) hasCurrentSys = alertme.loadFromIntentBundle(intent, bundle);
    		if (!hasCurrentSys) hasCurrentSys = alertme.loadFromPreference(sharedPrefs);

        	if (hasCurrentSys) {
        		HashMap<String, String> attributes = null;
        		AlertMeStorage.AlertMeUser currentUser = alertme.getCurrentSession();
        		alertme.getDeviceData();
        		keyFobs = alertme.retrieveDevices(Device.KEYFOB);
        		if (APIUtilities.isStringNonEmpty(currentUser.info)) {
        			attributes = APIUtilities.getDeviceChannelValues(currentUser.info);
        		}
        		if (alertme.hasSessionValues() && attributes == null) {
        			attributes = alertme.getUserInfo();
        		}
        		if (attributes!=null) {
        			if (attributes.containsKey("firstname")) {
        				firstname = attributes.get("firstname");
        			}
        			if (attributes.containsKey("lastname")) {
        				lastname = attributes.get("lastname");
        			}
        		}
    		}

    		if (handler!=null) {
    			Message msg = handler.obtainMessage();
                Bundle b = new Bundle();
                b.putInt("type", AlertMeConstants.UPDATE_ALL);
                b.putString("firstname", firstname);
                b.putString("lastname", lastname);
                msg.setData(b);
                handler.sendMessage(msg);
    		}        		
        }
		
	}
	private void initScreenStuff() {
		if (screenStuff==null) {
			screenStuff = new AMViewItems(this, this);
			screenStuff.registerSystemName(R.id.people_housename);
			screenStuff.initProgressDialog();
			screenStuff.initSystemName();
			
		}
	}
	private void restoreDetailsFromCache(AlertMeStorage.AlertMeUser currentUser) {
		String firstname = null;
		String lastname = null;
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "restoreDetailsFromCache()  START");
		if (currentUser!=null) {			
			HashMap<String, String> attributes = null;
			if (APIUtilities.isStringNonEmpty(currentUser.info)) {
	    		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "restoreDetailsFromCache()  currentUser info: "+currentUser.info);
				attributes = APIUtilities.getDeviceChannelValues(currentUser.info);
			}
			if (attributes!=null) {
				if (attributes.containsKey("firstname")) {
					firstname = attributes.get("firstname");
				}
				if (attributes.containsKey("lastname")) {
					lastname = attributes.get("lastname");
				}
			}
			performUpdate(AlertMeConstants.UPDATE_ALL, firstname, lastname);
		}
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "restoreDetailsFromCache()  END");
	}
	
	class PeopleState {
		public AlertMeSession.SessionState sessionState = null;
		public ArrayList<Device> keyFobs = null;
		public AlertMeStorage.AlertMeUser currentUser = null;
	}
}
