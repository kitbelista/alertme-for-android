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

package org.darkgoddess.alertdroid.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.darkgoddess.alertdroid.AlertMeConstants;
import org.darkgoddess.alertdroid.api.utils.APIUtilities;
import org.darkgoddess.alertdroid.api.utils.Device;
import org.darkgoddess.alertdroid.api.utils.Event;
import org.darkgoddess.alertdroid.api.utils.Hub;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;



public class AlertMeSession {
	public static final String TAG = "AlertMeSession";
	private static final boolean DEBUGOUT = false;	
	public static final int MODE_HOME = 0;
	public static final int MODE_ARM = 1;
	public static final int MODE_NIGHT = 2;
	public static final int SERVICE_INTRUDER_ALARM = 0;
	public static final int SERVICE_EMERGENCY_ALARM = 1;
	
	public static final String PREFERENCE_SETTING_ID = "alertMeSessionUserId";
	public static final String PREFERENCE_SETTING_USERID = "userId";
	public static final String PREFERENCE_SETTING_HUBID = "hubId";
	
	private static final long SESSION_SPAN = 1000*60*20; // 20 minute sessions (session key and event)
	private static final long ITEM_SPAN = 1000*60*5;     //  5 minute sessions (for device and hub)
	private AlertMeStorage db = null;
	private AlertMeServer alertMe = null;
	
	private AlertMeStorage.AlertMeUser session = null; // Stores the session
	private Hub activeHub = null;     // Current active hub, with behaviour Home | Away | Night
	private long activeHubId = -1;

	private int accountSizeCache = 0; // The number of registered accounts
	private ArrayList<Hub> hubs = null;
	private ArrayList<Device> devices = null;
	private ArrayList<Event> events = null;
	private boolean isHubActive = false;
	private long hubsLastRefresh = 0;
	private long deviceLastRefresh = 0;
	private long eventLastRefresh = 0;
	private long hubSettingsLastRefresh = 0;
	private long lastActionTime = 0; // time of last (public) action that alters state
	private Context appContext = null;
	private String lastRawAPIResult = null;
	
	/**
	 * Creates a plain instance of the object, without any state except to initialize DB access
	 *
	 * @param  context  Context to start the DB adapters with
	 */
	public AlertMeSession(Context context) {
		db = new AlertMeStorage(context);
		alertMe = new AlertMeServer();
		session = new AlertMeStorage.AlertMeUser();
		db.open();
		appContext = context;
		accountSizeCache = db.getUsersSize();
	}

	public Bundle onSaveInstanceState(Bundle outState) {
		Long uid = (session!=null && session.id!=-1)? session.id: null;
		Long hid = (activeHub!=null && activeHubId!=-1)? activeHubId: null;
		outState.putSerializable(PREFERENCE_SETTING_USERID, uid);
		outState.putSerializable(PREFERENCE_SETTING_HUBID, hid);
		return outState;
	}

	public String getLastRawAPIResult() {
		return lastRawAPIResult;
	}
	
	/**
	 * Create an Intent object to save our state between other AlertMe applications
	 *
	 * @returns  Intent that contains information to restore the state, namely the active hub and user id
	 */
	public Intent createIntentFromSession() {
		Intent intent = new Intent();
		if (DEBUGOUT) Log.w(TAG, "createIntentFromSession()  START");
		if (session!=null && session.id!=-1) {
			intent.putExtra(PREFERENCE_SETTING_USERID, session.id);
			intent.putExtra(PREFERENCE_SETTING_HUBID, activeHubId);
			if (DEBUGOUT) Log.w(TAG, "createIntentFromSession  -- intent storing ["+PREFERENCE_SETTING_USERID+"]["+session.id+"]");
			if (DEBUGOUT) Log.w(TAG, "createIntentFromSession  -- intent storing ["+PREFERENCE_SETTING_HUBID+"]["+activeHubId+"]");
		}
		if (DEBUGOUT) Log.w(TAG, "createIntentFromSession()  END");
		return intent;
	}
	
	/**
	 * Using the current session settings, see if the session is still valid
	 *
	 * @returns  True if time within the session span; False if not logged in or otherwise not within session
	 */
	public boolean isSessionAlive() {
		if (session!=null && session.id!=-1 && session.timestamp!=null) {
			return timeIsWithinSpan(session.timestamp.longValue(), SESSION_SPAN);
		}
		return false;
	}

	/**
	 * See if the session requires refreshing of cached objects (except events: they always are taken on demand)
	 *
	 * @returns  True if the at least one aspect of the cache needs to be fetched from the API
	 */
	public boolean requiresRefresh() {
		boolean stillFresh = (timeIsWithinSpan(deviceLastRefresh, ITEM_SPAN));
		if (stillFresh) stillFresh = stillFresh && (timeIsWithinSpan(hubsLastRefresh, ITEM_SPAN));
		if (stillFresh) stillFresh = stillFresh && (timeIsWithinSpan(hubSettingsLastRefresh, ITEM_SPAN));

		return !stillFresh;
	}

	/**
	 * See if the device data requires refreshing of into cache
	 *
	 * @returns  True if the device data is too old
	 */
	public boolean devicesRequiresRefresh() {
		boolean stillFresh = (timeIsWithinSpan(deviceLastRefresh, ITEM_SPAN));

		return !stillFresh;
	}

	/**
	 * See if the hub data requires refreshing of into cache
	 *
	 * @returns  True if the hub data is too old
	 */
	public boolean hubsRequiresRefresh() {
		boolean stillFresh = (timeIsWithinSpan(hubsLastRefresh, ITEM_SPAN));

		return !stillFresh;
	}
	
	/**
	 * Using the current session settings, see if the session key for AlertMe API calls is still valid
	 *
	 * @returns  True if the session key is usable for API calls
	 */
	public boolean hasSessionKey() {
		return (session!=null && APIUtilities.isStringNonEmpty(session.sessionKey));
	}
	
	/**
	 * Using the current session settings, see if the session details for account are valid 
	 *
	 * @returns  True if the session username/password is usable for API calls
	 */
	public boolean hasSessionValues() {
		return (session!=null && APIUtilities.isStringNonEmpty(session.username) && APIUtilities.isStringNonEmpty(session.password));
	}
	
	/**
	 * Using the current session settings, see if the hub is active
	 *
	 * @returns  True if a previous call to load the hub detected that it is available
	 */
	public boolean isCurrentHubActive() {
		return isHubActive;
	}
	
	/**
	 * Using the current context, see if we have an internet connection
	 *
	 * @returns  True if there is an internet connection
	 */
	public boolean hasInternetConnection() {
		ConnectivityManager conMgr = (ConnectivityManager) appContext.getSystemService (Context.CONNECTIVITY_SERVICE);
		// ARE WE CONNECTED TO THE NET
		if (conMgr.getActiveNetworkInfo() != null && conMgr.getActiveNetworkInfo().isConnectedOrConnecting()) {
				//conMgr.getActiveNetworkInfo().isAvailable() &&
				//conMgr.getActiveNetworkInfo().isConnected()) {
			if (DEBUGOUT) Log.w(TAG, "hasInternetConnection()  -- Internet Connection Present");
			return true;
		} else {
			if (DEBUGOUT) Log.w(TAG, "hasInternetConnection()  -- Internet Connection Not Present");
			return false;
		}		
	} 

	/**
	 * Set the current session to one that corresponds to the session stored in SharedPreferences
	 *
	 * Also reloads the bare minimum of data to cache if needed (hubs)
	 *
	 * @param  settings  SharedPreferences from the current context
	 * @returns  True if session is valid (at least a user is identified)
	 */	
	public boolean loadFromPreference(SharedPreferences settings) {
		boolean res = false;
		long invalid = -1;
		long rawUId = (settings==null)? invalid: settings.getLong(PREFERENCE_SETTING_USERID, invalid);
		if (DEBUGOUT) Log.w(TAG, "loadFromPreference()  START");
		if (rawUId!=invalid) {
			long rawHId = (settings==null)? invalid: settings.getLong(PREFERENCE_SETTING_HUBID, invalid);
			
			helperLoadUserAndHubFromSettings(rawUId, rawHId);
			res = hasSessionValues();
			if (DEBUGOUT) Log.w(TAG, "loadFromPreference -- Settings ["+PREFERENCE_SETTING_HUBID+"]["+rawHId+"]");
			lastActionTime = System.currentTimeMillis();
		}
		if (DEBUGOUT) Log.w(TAG, "loadFromPreference -- Settings ["+PREFERENCE_SETTING_USERID+"]["+rawUId+"]");
		if (DEBUGOUT) Log.w(TAG, "loadFromPreference()  START");
		return res;
	}
	
	/**
	 * Attempt to restore state from intent/bundle settings
	 *
	 * @param  intent Intent between activities, if any
	 * @param  savedInstanceState Bundle from onCreate/resume
	 * @returns  True if loading was possible (bare minimum was obtaining the user id)
	 */	
	public boolean loadFromIntentBundle(Intent intent, Bundle savedInstanceState) {
		boolean res = false;
		long invalid = -1;
		long uId = invalid;
		long hId = invalid;
		if (DEBUGOUT) Log.w(TAG, "loadFromIntentBundle()  START");

		uId = (savedInstanceState != null)? savedInstanceState.getLong(PREFERENCE_SETTING_USERID): uId;
		uId = (intent!=null && uId==invalid)? intent.getLongExtra(PREFERENCE_SETTING_USERID, invalid): uId;
		hId = (savedInstanceState != null)? savedInstanceState.getLong(PREFERENCE_SETTING_HUBID): hId;
		hId = (intent!=null && hId==invalid)? intent.getLongExtra(PREFERENCE_SETTING_HUBID, invalid): hId;
		
		if (intent!=null) {
			Bundle extras  = intent.getExtras();
			uId = (extras != null && uId == invalid)? extras.getLong(PREFERENCE_SETTING_USERID): uId;
			hId = (extras != null && hId == invalid)? extras.getLong(PREFERENCE_SETTING_HUBID): hId;
		}
		
		if (uId!=invalid) {
			helperLoadUserAndHubFromSettings(uId, hId);
			res = this.hasSessionValues();
			lastActionTime = System.currentTimeMillis();
		}

		if (DEBUGOUT) Log.w(TAG, "loadFromIntentBundle -- Settings ["+PREFERENCE_SETTING_USERID+"]["+uId+"]["+PREFERENCE_SETTING_HUBID+"]["+hId+"]");
		if (DEBUGOUT) Log.w(TAG, "loadFromIntentBundle()  END");
		return res;
	}

	/**
	 * Set the current session to one that corresponds to the first *and only* user account entry found
	 *
	 * Also reloads the bare minimum of data to cache if needed (hubs)
	 *
	 * @returns  True if successfully loaded state from a single user account entry found; False otherwise
	 */	
	public boolean loadFromOnlyEntry() {
		boolean res = false;
		ArrayList<AlertMeStorage.AlertMeUser> users = db.getUsers();
		int userSize = (users!=null)? users.size(): 0;
		if (DEBUGOUT) Log.w(TAG, "loadFromOnlyEntry()  START");		
		if (DEBUGOUT) Log.w(TAG, "loadFromOnlyEntry -- "+userSize+ " user accounts registered");
		if (userSize==1) {
			AlertMeStorage.AlertMeUser user = users.get(0);
			res = loginAsUser(user);
			
			// try to load the first hub from memory
			if (res) {
				ArrayList<Hub> hubsFromDB = (hubs!=null)? hubs: db.getHubs(user.id);
				if (hubsFromDB!=null && !hubsFromDB.isEmpty()) {
					activeHub = hubsFromDB.get(0);
					if (activeHub!=null && user.id!=-1) {
						if (DEBUGOUT) Log.w(TAG, "loadFromOnlyEntry -- calling db.getHubId("+user.id+", "+activeHub.id+")");
						activeHubId = db.getHubId(user.id, activeHub.id);
					}					
				}
				lastActionTime = System.currentTimeMillis();
			}
			if (DEBUGOUT) Log.w(TAG, "loadFromOnlyEntry -- User found::"+user);
			if (DEBUGOUT) Log.w(TAG, "loadFromOnlyEntry -- Settings ["+PREFERENCE_SETTING_HUBID+"]["+activeHubId+"]");
		}

		if (DEBUGOUT) Log.w(TAG, "loadFromOnlyEntry()  END");		
		return res;
	}
	
	/**
	 * Get a complete list of user accounts in storage
	 *
	 * @returns  An ArrayList of user details, or possibly null
	 */	
	public ArrayList<AlertMeStorage.AlertMeUser> getUserAccountList() {
		ArrayList<AlertMeStorage.AlertMeUser> res = db.getUsers();
		accountSizeCache = (res!=null)? res.size(): accountSizeCache;
		return res;
	}
	/**
	 * Get the session details given an id
	 *
	 * @returns  A user session detail object, or possibly null
	 */	
	public AlertMeStorage.AlertMeUser getUserByID(long userId) {
		return db.getUser(userId);
	}

	/**
	 * Manipulate the given SharedPreferences to save our state
	 *
	 * @param  settings The current SharedPrefrences from the context
	 */
    public void setCurrentSessionAsPreference(SharedPreferences settings) {
		if (session!=null && session.id>=0 && settings!=null) {
    		SharedPreferences.Editor editor = settings.edit();
    	    editor.putLong(PREFERENCE_SETTING_USERID, session.id).putLong(PREFERENCE_SETTING_HUBID, activeHubId);
    	    editor.commit();
    	}
    }

	/**
	 * Set the given session settings as our session, if a valid entry
	 *
	 * @param  settings The session item to use. Accepts it if the ID is valid
	 */
    public void setSession(AlertMeStorage.AlertMeUser settings) {
    	if (settings!=null && settings.id>=0) {
    		session = settings;
    		lastActionTime = System.currentTimeMillis();
    		isHubActive = true; // TODO: load correct active state
    	}
    }

	/**
	 * Get the current session from state
	 *
	 * @returns  User session 
	 */
    public AlertMeStorage.AlertMeUser getCurrentSession() {
    	return session;
    }

	/**
	 * Replace the session values, if session has been established. Save changes if required
	 *
	 * @param  username User account username
	 * @param  password User account password
	 */
    public void updateSessionValues(String username, String password, String sessId) {
    	if (hasSessionValues()) {
    		if (APIUtilities.isStringNonEmpty(username) && APIUtilities.isStringNonEmpty(password)) {
    			session.username = username;
    			session.password = password;
    			db.updateUserEntryLogin(session.id, username, password, sessId, System.currentTimeMillis(), null);
    			lastActionTime = System.currentTimeMillis();
    		}
    	}
    }

	/**
	 * Called when finishing: clean up database connections
	 *
	 */
    public void clean() {
    	db.close();
    }

	/**
	 * Called when finishing: logout
	 *
	 */
    public void logout() {
		if (session!=null && isSessionAlive()) {		
			lastRawAPIResult = alertMe.logout(session.sessionKey);
			session.sessionKey = null;
		}
    }
    
	/**
	 * Using the current session settings, attempt to login (if needed)
	 *
	 * @returns  True on successful login; False if *could* not attempt login or login failed
	 */
	public boolean login() {
		boolean res = false;
		
		if (session!=null) {
			if (!isSessionAlive()) {
				res = helperLogin();
				lastActionTime = System.currentTimeMillis();
			} else {
				res = true;
			}
		}
		
		return res;
	}


	/**
	 * Attempt to change the state of the session with a different hub. Returns true if a valid change
	 * 
	 * Note that even if an account only has one hub, as it is a valid choice then the result is true.
	 *    If the active hub *is* changed then the state will reset and reload the appropriate hub-dependant data
	 *    (namely the devices and event data will be forced to reload on demand)
	 *
	 * @returns  True if the choice is valid, otherwise False
	 */
	public boolean changeActiveHub(String hubZId) {
		boolean res = false;
		boolean hubChanged = false;
		if (APIUtilities.isStringNonEmpty(hubZId) && hubs!=null) {
			if (activeHub!=null && activeHub.id.equals(hubZId)) {
				res = true;
			} else {
				for(Hub hub: hubs) {
					if (hub.id.equals(hubZId)) {
						if (session!=null && session.id!=-1) {
							long hid = db.getHubId(session.id, hub.id);
							activeHubId = (hid>-1)? hid: activeHubId;
						}
						activeHub = hub;
						hubChanged = true;
						res = true;
						break;
					}
				}
			}
		}
		// If the hub changed, then reset the hub specific data from cache
		if (hubChanged) {
			clearEventCache();
			clearDeviceCache();
			getDeviceData();
			lastActionTime = System.currentTimeMillis();
		}
			
		return res;
	}
	
	public String getActiveServiceState(int serviceType) {
		int serve = SERVICE_INTRUDER_ALARM;
		String res = null;
		helperKeepAlive();
		switch (serviceType) {
			case SERVICE_INTRUDER_ALARM:
			case SERVICE_EMERGENCY_ALARM:
				serve = serviceType;
				break;
		}
		
		if (session!=null && session.id!=-1) {
			res = alertMe.getCurrentServiceState(session.sessionKey, getServiceStringFromId(serve));
			lastRawAPIResult = res;
		}
		return res;
	}
	
	public static String getServiceStringFromId(int service) {
		String res = null;

		switch (service) {
			case SERVICE_INTRUDER_ALARM:
				res = AlertMeConstants.ALARM_INTRUDER;
				break;
			case SERVICE_EMERGENCY_ALARM:
				res = AlertMeConstants.ALARM_EMERGENCY;
				break;
		}
		return res;
	}
	
	/**
	 * Attempt to set the relay mode of the given (power controller) device 
	 * 
	 * @param    device The (power controller) device
	 * @param    isRelayState  The boolean to either make relay state ok (true) or off (false)
	 * @returns  True if the action had success
	 */
	public boolean changeRelayState(Device device, boolean isRelayState) {
		boolean res = false;
		if (device!=null && hasSessionValues()) {
			helperKeepAlive();
			if (hasSessionKey()) {
				String mode = (isRelayState)? AlertMeConstants.RELAYSTATE_ON: AlertMeConstants.RELAYSTATE_OFF;
				String rawRes = alertMe.setRelayState(session.sessionKey, mode, device.id);
				int isOk = APIUtilities.getCommandResult(rawRes);
				res = (isOk == APIUtilities.COMMAND_OK);
				lastRawAPIResult = rawRes;
				if (devices!=null && !devices.isEmpty()) {
					boolean refresh = false;
					int i = 0;
					for (Device d: devices) {
						if (d.id.equals(device.id)) {
							refresh = true;
							devices.set(i, device);
							break;
						}
						i++;
					}
					if (refresh) {
						long deviceId = db.getDeviceId(activeHubId, device.id);
						// if ok, save to database
						if (deviceId!=-1) {
							String newMode = (isRelayState)? AlertMeConstants.RELAYMODE_TRUE: AlertMeConstants.RELAYMODE_FALSE;
							device.setAttribute("relaystate", newMode);
							db.updateDevice(deviceId, device);
						}
						//db.updateDevices(activeHubId, devices);
						//deviceLastRefresh = System.currentTimeMillis();
					}
				}
				deviceLastRefresh = 0;
			}
		}
		return res;
	}
	
	/**
	 * Get the hubs from current state (the cache) IF there is more than one hub
	 * 
	 * If there is more than one hub then the state can change by changing the active hub;
	 *   the list may be used to change the hub. Refreshes data if necessary
	 *
	 * @returns  Hub list from current state (if size is >1) or null
	 */
	public ArrayList<Hub> getHubListSelection() {
		getHubData();
		return (hubs!=null && !hubs.isEmpty() && hubs.size()>1)? hubs: null;
	}	
	/**
	 * Get the hubs from current state (the cache) 
	 * 
	 * @returns  Hub list from current state 
	 */
	public ArrayList<Hub> retrieveHubs() {
		if (hubs==null || hubs!=null && hubs.isEmpty()) {
			if (session!=null && session.id!=-1) hubs = db.getHubs(session.id);
		}
		return hubs;
	}
	/**
	 * Get the hub from current state (the cache) given the ZID 
	 * 
	 * @param  hubZId  the (zigbee) identifier for the hub
	 * @returns  Hub with the corresponding ID or null
	 */
	public Hub retrieveHubByID(String hubZId) {
		Hub res = null;
		if (hubs==null || hubs!=null && hubs.isEmpty()) {
			if (session!=null && session.id!=-1) hubs = db.getHubs(session.id);
		}
		if (hubs!=null && !hubs.isEmpty()) {
			for(Hub h: hubs) {
				if (hubZId.equals(h.id)) {
					res = h;
					break;
				}
			}
		}
		return res;
	}
	/**
	 * Get the hub from current state (the cache) given the database ID 
	 * 
	 * @param  hubId  the database identifier for the hub
	 * @returns  Hub with the corresponding ID or null
	 */
	public Hub retrieveHubByDBID(long hubId) {
		Hub res = db.getHub(hubId);
		return res;
	}
	/**
	 * Retrieve the cached amount of accounts are stored
	 * 
	 * This number can be obtained from a list with {@link #getUserAccountList()}
	 * 
	 * @returns  Number of accounts detected, in cache
	 */
	public int retrieveUserCount() {
		return accountSizeCache;
	}

	/**
	 * Get the hubs from current state (the cache)
	 * 
	 * Using the current session settings, attempts to refresh the data is done automatically:
	 *   if no hub data OR the last time the hub data was saved was too long ago there will be 
	 *   an API call to retrieve it (and then saved to DB for backup: see {@link #helperLoadHubs()})
	 *
	 * @returns  Hub list from current state
	 */
	public ArrayList<Hub> getHubData() {
		helperKeepAlive();
		if (session!=null && session.id>=0) {
			if (hubsLastRefresh<=0) {
				hubsLastRefresh = db.getLatestUpdateHubs(session.id);
			}
			if (!timeIsWithinSpan(hubsLastRefresh, ITEM_SPAN)) {
				hubs = null;
				helperLoadHubs();
			} else {
				if (hubs==null||hubs!=null && hubs.isEmpty()) {
					hubs = db.getHubs(session.id);
				}
			}
			lastActionTime = System.currentTimeMillis();
		}
		return hubs;
	}
	/**
	 * Get the events from the current state (the cache) 
	 * 
	 * @returns  Array of events list from current state
	 */
	public ArrayList<Event> retrieveEvents() {
		if (events==null || events!=null && events.isEmpty()) {
			if (activeHub!=null && activeHubId!=-1) events = db.getEvents(activeHubId);
		}
		return events;
	}

	/**
	 * Get the current active hub from current state (the cache). Contains data on behaviour and services
	 * 
	 * Using the current session settings, attempts to refresh the data is done automatically:
	 *   if no hub data OR the last time the device data was saved was too long ago there will be 
	 *   an API call to retrieve it (and then saved to DB for backup: see {@link #helperLoadDevices()})
	 *
	 * @returns  Current active hub from state
	 */		
	public Hub getActiveHub() {
		helperKeepAlive();
		// Active hub data
		boolean reloadActiveHub = false;
		if (activeHub!=null) {
			if (hubSettingsLastRefresh<=0 && session!=null && session.id>=0) {
				hubSettingsLastRefresh = db.getHubTimestamp(session.id, activeHub.id);
			}
			if (!APIUtilities.isStringNonEmpty(activeHub.behaviour)) {
				reloadActiveHub = true;
			} else if (!timeIsWithinSpan(hubSettingsLastRefresh, ITEM_SPAN)) {
				reloadActiveHub = true;
			}
		} else {
			reloadActiveHub = true;
		}

		if (reloadActiveHub) {
			helperLoadActiveHub();
			lastActionTime = System.currentTimeMillis();
		} else if (activeHubId!=-1) {
			activeHub = db.getHub(activeHubId);
			lastActionTime = System.currentTimeMillis();
		}

		return activeHub;
	}

	/**
	 * Delete cached event entries and return them
	 */
	public ArrayList<Event> flushEventData() {
		ArrayList<Event> res = events;
		if (events==null||events!=null&&events.isEmpty()) {
			if (activeHubId!=-1) {
				res = db.getEvents(activeHubId);
			}
		}
		// delete cache..
		if (activeHubId!=-1) {
			db.deleteEvents(activeHubId);			
		}
		
		if (events!=null&&events.isEmpty()) {
			events.clear();
			events = null;
			eventLastRefresh = 0;
		}
		
		return res;
	}

	public ArrayList<Event> getEventData() {
		helperKeepAlive();
		if (DEBUGOUT) Log.w(TAG, "getEventData()  START");
		if (session!=null && session.id>=0) {
			if (!timeIsWithinSpan(eventLastRefresh, ITEM_SPAN)) {
				events = null;
				helperGetEventLog();
			} else {
				if (events==null||events!=null&&events.isEmpty()) {
					if (activeHubId!=-1) events = db.getEvents(activeHubId);
				}
			}
			lastActionTime = System.currentTimeMillis();
		}
		if (DEBUGOUT) {
			if (events!=null) Log.w(TAG, "getEventData()  events size: "+events.size());
			else Log.w(TAG, "getEventData()  events size 0 as NULL");
		}
		if (DEBUGOUT) Log.w(TAG, "getEventData()  END");
		
		return events;
	}

	/**
	 * Get the devices from current state (the cache)
	 *
	 * @returns  Devices from the current state
	 */	
	public ArrayList<Device> retrieveDevices() {
		if (devices==null || devices!=null && devices.isEmpty()) {
			if (activeHub!=null && activeHubId!=-1) devices = db.getDevices(activeHubId);
		}
		return devices;
	}

	/**
	 * Get the active hub from current state (the cache)
	 *
	 * @returns  The active hub in current state
	 */	
	public Hub retrieveActiveHub() {
		return activeHub;
	}

	/**
	 * Get the active hub ID from current state (the cache)
	 *
	 * @returns  The active hub ID in current state
	 */	
	public long retrieveActiveHubID() {
		return activeHubId;
	}

	/**
	 * Get the devices from current state (the cache)
	 * 
	 * Using the current session settings, attempts to refresh the data is done automatically:
	 *   if no hub data OR the last time the device data was saved was too long ago there will be 
	 *   an API call to retrieve it (and then saved to DB for backup: see {@link #helperLoadDevices()})
	 *
	 * @returns  Devices from the current state
	 */	
	public ArrayList<Device> getDeviceData() {
		helperKeepAlive();
		if (session!=null && session.id>=0) {
			if (deviceLastRefresh<=0) {
				deviceLastRefresh = db.getLatestUpdateDevices(session.id);
			}
			if (!timeIsWithinSpan(deviceLastRefresh, ITEM_SPAN)) {
				devices = null;
				helperLoadDevices();
			} else {
				if (devices==null||devices!=null&&devices.isEmpty()) {
					devices = db.getDevices(session.id);
				}
			}
			lastActionTime = System.currentTimeMillis();
		}
		return devices;
	}
	
	/**
	 * Get the devices from current state (the cache) given a device type
	 * 
	 * Uses {
	 *
	 * @returns  Devices from the current state
	 */	
	public ArrayList<Device> retrieveDevices(int type) {
		ArrayList<Device> res = new ArrayList<Device>();
		if (devices==null || devices!=null && devices.isEmpty()) {
			if (session!=null && session.id!=-1) devices = db.getDevices(session.id);
		}
		if (devices!=null && !devices.isEmpty()) {
			for (Device d: devices) {
				if (d.type==type) {
					res.add(d);
				}
			}
		}
		return res;
	}

	/**
	 * Attempt to stop the provided alarm
	 *
	 * WARNING:: TO TEST
	 *
	 * @param  serviceId  One of the services corresponding to IntruderAlarm | EmergencyAlarm
	 * @returns  True if the system registered the change, False otherwise
	 */	
	public boolean stopAlarm(int serviceID) {
		boolean res = false;
		boolean doAction = (serviceID == SERVICE_INTRUDER_ALARM) || (serviceID == SERVICE_EMERGENCY_ALARM);
		
		if (doAction) {
			String deviceCommand = "serverClear";
			String rawRes = "";
			int isOk = -1;
			helperKeepAlive();
			rawRes = alertMe.sendCommand(session.sessionKey, getServiceStringFromId(serviceID), deviceCommand, null);
			isOk = APIUtilities.getCommandResult(rawRes);
			res = (isOk == APIUtilities.COMMAND_OK);
			lastRawAPIResult = rawRes;
		}
		
		return res;
	}
	
	/**
	 * Set the current hub to the given mode
	 *
	 * Also reloads the bare minimum of data to cache if needed (hubs)
	 *
	 * @param  mode  One of the modes corresponding to Home | Away | Night
	 * @returns  True if the system registered the change, False otherwise
	 */
	public boolean setHubMode(int mode) {
		boolean res = false;
		String behaviour = null;
		helperKeepAlive();
		if (isSessionAlive()) {
			String strMode = null;
			switch (mode) {
				case MODE_HOME:
					behaviour = "Home";
					strMode = "disarm";
					break;
				case MODE_ARM:
					behaviour = "Away";
					strMode = "arm";
					break;
				case MODE_NIGHT:
					behaviour = "Night";
					strMode = "nightArm";
					break;
			}
			if (APIUtilities.isStringNonEmpty(strMode)) {
				String behaveRes = alertMe.setBehaviourMode(session.sessionKey, strMode);
				int isOk = APIUtilities.getCommandResult(behaveRes);
				res = (isOk == APIUtilities.COMMAND_OK);
				lastRawAPIResult = behaveRes;
				if (activeHub!=null) {
					activeHub.behaviour = behaviour;
					db.updateHubData(activeHubId, activeHub);
					hubs = db.getHubs(session.id);
					if (res) hubSettingsLastRefresh = 0;	
					lastActionTime = System.currentTimeMillis();				
				}
				Log.w(TAG, "setHubMode("+strMode+")  -- raw ["+behaveRes+"] result::"+res+" code["+isOk+"]");
			}
			
		}

		return res;
	}
	
	/**
	 * Set the current active hub to the one corresponding to the hubID provided
	 *
	 * Assumes that the cache is up to date and stored in memory, as the hubID is checked on the last recorded entries
	 *
	 * @param  hubZId  The (zigbee) ID for the hub to change to
	 * @returns  True if the active hub was changed
	 */
	public boolean setActiveHub(String hubZId) {
		boolean res = false;
		helperKeepAlive();
		if (DEBUGOUT) Log.w(TAG, "setActiveHub("+hubZId+")  START");
		if (isSessionAlive() && APIUtilities.isStringNonEmpty(hubZId)) {
			Hub newHub = retrieveHubByID(hubZId);
			if (newHub!=null) {
				String rawRes = alertMe.setHub(session.sessionKey, hubZId);
				int isOk = APIUtilities.getCommandResult(rawRes);
				res = (isOk == APIUtilities.COMMAND_OK);
				lastRawAPIResult = rawRes;
				if (DEBUGOUT) Log.w(TAG, "setActiveHub()  called setHub::"+rawRes);
				if (res) {
					// reload everything related to the hub and reset the event and device cache
					activeHub = newHub;
					activeHubId = db.getHubId(session.id, hubZId);
					clearDeviceCache();
					clearEventCache();
					lastActionTime = System.currentTimeMillis();
					//helperLoadDevices(); // reload the devices
				}
				
			}
		}
		if (DEBUGOUT) Log.w(TAG, "setActiveHub()  END (returning: "+res+")");
		
		return res;
	}

	/**
	 * Refresh all the data and store into 'cache' (this objects state)
	 *
	 * Calls {@link #getDeviceData()} {@link #getHubData()} to set up state internally
	 *
	 */
	public void refreshAll() {
		helperClearCache();
		getHubData();
		getActiveHub();
		getDeviceData();
		lastActionTime = System.currentTimeMillis();
	}

	/**
	 * Refresh all the device data and store into 'cache' (this objects state)
	 *
	 * Calls {@link #getDeviceData()} {@link #getHubData()} to set up state internally
	 *
	 */
	public void refreshDevices() {
		clearDeviceCache();
		getDeviceData();
		lastActionTime = System.currentTimeMillis();
	}

	/**
	 * Attempt to load all data to the 'cache' (this objects state)
	 *
	 * Calls {@link #getDeviceData()} {@link #getHubData()} to set up state internally
	 *
	 */
	public void loadAll() {
		getHubData();
		getActiveHub();
		getDeviceData();
		lastActionTime = System.currentTimeMillis();
	}

	/**
	 * Attempt to load all data to the 'cache' (this objects state) from the database if not set
	 *
	 * Calls {@link #retrieveHubs} {@link #retrieveDevices()} to set up state internally
	 *
	 */
	public void retrieveAll() {
		retrieveHubs();
		retrieveDevices();
	}
	

	/**
	 * Attempt to remove the user settings
	 *
	 * This PERMENANTLY REMOVES the user record, if it exists
	 *
	 * @param  username Account user name
	 * @return True if the account was removed; null on failure
	 */
	public boolean removeUserAccount(long userId) {
		boolean res = db.deleteUserEntry(userId);
		accountSizeCache = db.getUsersSize();
		return res;
	}
	
	/**
	 * Attempt to login with given credentials and then store details if successful
	 *
	 * This stores the user info to the database if it is possible to do so
	 *
	 * @param  username Account user name
	 * @param  password Account password
	 * @return AlertMeStorage.AlertMeUser User details corresponding to a successful login; null on failure
	 */
	public AlertMeStorage.AlertMeUser loginFirstTime(String username, String password) {
		AlertMeStorage.AlertMeUser res = null;
		String sess = null;
		
		if ( APIUtilities.isStringNonEmpty(username) && APIUtilities.isStringNonEmpty(password)) {
			sess = alertMe.login(username, password);
			lastRawAPIResult = sess;			
		}
		
		if (APIUtilities.isStringNonEmpty(sess) && AlertMeServer.Exceptions.loginOkFromRawResult(sess)) {
			// create all the details as possible
			long timestamp = System.currentTimeMillis();
			long uid = -1;
			String userinfo = alertMe.getUserInfo(sess);
			lastRawAPIResult = userinfo;
			// first check if the user details exist in the DB - overwrite otherwise add
			res = db.getUserByUsername(username);
			if (res==null) {
				uid = db.addUser(username, password, sess, timestamp, userinfo);
				accountSizeCache = db.getUsersSize();
				if (uid!=-1) {
					res = new AlertMeStorage.AlertMeUser(uid, username, password, sess, timestamp, userinfo);		
				}					
			} else {
				res.info = userinfo;
				res.password = password;
				res.sessionKey = sess;
				db.updateUserEntryLogin(res.id, username, password, sess, timestamp, userinfo);
			}
		}
		return res;
	}
	
	public HashMap<String, String> getUserInfo() {
		if (isSessionAlive()) {
			String userinfo = alertMe.getUserInfo(session.sessionKey);
			HashMap<String, String> res = APIUtilities.getDeviceChannelValues(userinfo);
			lastRawAPIResult = userinfo;
			db.updateUserEntryInfo(session.id, userinfo);
			return res;
		}
		return null;
	}
	
	public HashMap<String, String> retrieveUserInfo() {
		String userInfo = (session!=null)? session.info: "";
		return APIUtilities.getDeviceChannelValues(userInfo);
	}

	/**
	 * Set the current session to one that corresponds to the user ID
	 *
	 * Also reloads the bare minimum of data to cache if needed (hubs)
	 *
	 * @param  userId  Identifies a session object from the database to load
	 * @returns  True if session is valid
	 */	
	public boolean loginAsUser(long userId) {
		return loginAsUser(db.getUser(userId));
	}

	public SessionState retrieveCurrentState() {
		SessionState res = new SessionState();
		res.session = session;
		res.activeHub = activeHub;
		res.activeHubId = activeHubId;
		res.hubs = hubs;
		res.devices = devices;
		res.events = events;
		res.hubsLastRefresh = hubsLastRefresh;
		res.deviceLastRefresh = deviceLastRefresh;
		res.eventLastRefresh = eventLastRefresh;
		res.hubSettingsLastRefresh = hubSettingsLastRefresh;
		res.cacheAge = lastActionTime;
		res.isHubActive = isHubActive;
		res.lastRawAPIResult = lastRawAPIResult;
		
		return res;
	}

	
	public boolean loadFromCachedState(Context context, SessionState state) {
		boolean res = false;
		
		if (state.cacheAge > lastActionTime) {
			session = state.session;
			activeHub = state.activeHub;
			activeHubId = state.activeHubId;
			hubs = state.hubs;
			devices = state.devices;
			events = state.events;
			hubsLastRefresh = state.hubsLastRefresh;
			deviceLastRefresh = state.deviceLastRefresh;
			eventLastRefresh = state.eventLastRefresh;
			hubSettingsLastRefresh = state.hubSettingsLastRefresh;
			lastActionTime = state.cacheAge;
			isHubActive = state.isHubActive;
			lastRawAPIResult = state.lastRawAPIResult;
			res = true;
		}
		if (res) {
			if (db==null) {
				db = new AlertMeStorage(context);
				db.open();
			}
			appContext = (appContext==null)? context: appContext;
		}
		
		return res;
	}
	
	/**
	 * Set the current session with the details as retrieved from the database
	 *
	 * Also reloads the bare minimum of data to cache if needed (hubs)
	 *
	 * @param  userDetails  A session object that (hopefully) would be valid when required
	 * @returns  True if session is valid
	 */
	private boolean loginAsUser(AlertMeStorage.AlertMeUser userDetails) {
		boolean res = false;
		boolean requiresRefresh = true;
		boolean requiresOldLogout = false;
		boolean requiresNewLogin = true;
	
		// First check if we are changing the user details dramatically to warrant reloading
		if (session!=null && userDetails!=null) {
			if (session.id==userDetails.id || session.username.equals(userDetails.username)) {
				if (isSessionAlive()) {
					requiresRefresh = false;
					requiresNewLogin = false;
				}
			} else {
				requiresOldLogout = true;
			}
		} else if (session!=null && isSessionAlive()) {
			requiresOldLogout = true;
		}
		if (requiresRefresh) {
			helperClearCache();
		}
		if (requiresOldLogout && session!=null && APIUtilities.isStringNonEmpty(session.sessionKey)) {
			lastRawAPIResult = alertMe.logout(session.sessionKey);
		}
		session = userDetails;
		if (session.timestamp!=null && isSessionAlive()) {
			requiresNewLogin = false;
		}
		if (session!=null) {
			boolean okLogin = true;
			if (requiresNewLogin) {
				okLogin = helperLogin();
				res = okLogin;
			} else {
				res = true;
			}
			getHubData();
			lastActionTime = System.currentTimeMillis();
		}
		return res;
	}

	/**
	 * Clear the cache (current state) to force a refresh
	 *
	 * This stores the user info to the database if it is possible to do so
	 */
	private void helperClearCache() {
		helperClearHubCache();
		clearEventCache();
		clearDeviceCache();
	}

	/**
	 * Set the first hub discovered in the database as the active hub
	 *
	 */
	public void loadDefaultHubAsActive() {
		hubs = retrieveHubs();
		if (hubs!=null && !hubs.isEmpty() && isSessionAlive()) {
			activeHub = hubs.get(0);
			activeHubId = db.getHubId(session.id, activeHub.id);
			lastActionTime = System.currentTimeMillis();
		}
	}
	
	public void clearDeviceCache() {
		if (devices!=null && !devices.isEmpty()) {
			devices.clear();
		}
		devices = null;
		deviceLastRefresh = 0;
		lastActionTime = System.currentTimeMillis();
	}
	public void clearEventCache() {
		if (events!=null && !events.isEmpty()) {
			events.clear();
		}
		events = null;
		eventLastRefresh = 0;
		lastActionTime = System.currentTimeMillis();
	}
	private void helperClearHubCache() {
		activeHub = null;
		activeHubId = -1;
		if (hubs!=null && !hubs.isEmpty()) {
			hubs.clear();
		}
		hubs = null;
		hubsLastRefresh = 0;
	}
	private void helperLoadLastActiveHub() {
		if (session!=null && session.id!=-1) {
			if (hubs==null || hubs!=null && hubs.isEmpty()) {
				hubs = db.getHubs(session.id);
			}
			if (hubs!=null && !hubs.isEmpty()) {
				activeHub = hubs.get(0);
				activeHubId = db.getHubId(session.id, activeHub.id);
			}
		}
	}
	private void helperGetEventLog() {
		if (DEBUGOUT) Log.w(TAG, "helperGetEventLog()  START");
		if (session!=null && session.id>=0) {
			String service = "null";
			int limit = 50;
			String start = "null";
			String end = "null";
			boolean getAllCurrent = false;
			// TODO: get stuff from DB
			if (events==null||events!=null&&events.isEmpty()) {
				if (activeHubId!=-1) events = db.getEvents(activeHubId);
			}
			if (events!=null && !events.isEmpty()) {
				Collections.sort(events, Event.getComparator(true));
			}
			if (events==null|| (events!=null && events.isEmpty())) {
				getAllCurrent = true;
			}
			if (getAllCurrent) {
				if (DEBUGOUT) Log.w(TAG, "helperGetEventLog()  getAllCurrent: retrieving complete list");
				String rawRes = alertMe.getEventLog(session.sessionKey, service, limit, start, end);
				events = APIUtilities.getEventLog(rawRes);
				lastRawAPIResult = rawRes;
				if (activeHubId!=-1) db.updateEvents(activeHubId, events);
				if (!events.isEmpty()) Collections.sort(events, Event.getComparator(true));
			} else {
				boolean addedToList = false;
				String currentStart = "null";
				String currentEnd = "null";
				int limitDiff = limit;
				int eventSize = events.size();
				long futureDiff = 0;
				if (DEBUGOUT) Log.w(TAG, "helperGetEventLog()  notAllCurrent: ammending list");
				if (!events.isEmpty()) Collections.sort(events, Event.getComparator(true));
				
				if (eventSize>0) {
					int lastI = (eventSize-1);
					Event first = events.get(0);
					Event last = events.get(lastI);
					if (last!=null) {
						currentStart = last.epochTimestamp + "";
						if (DEBUGOUT) Log.w(TAG, "helperGetEventLog()  start at: "+currentStart);
					}
					if (first!=null) {
						long currEp = System.currentTimeMillis()/1000;
						futureDiff = currEp - first.epochTimestamp;
						currentEnd = first.epochTimestamp + "";
						if (DEBUGOUT) Log.w(TAG, "helperGetEventLog()  end at: "+currentEnd + " where future diff is: ["+currEp+"-"+first.epochTimestamp+"] = "+futureDiff);
					}
					
					// Appending the future
					if (currentEnd!=null && limitDiff>0 && futureDiff>0) {
						String rawRes = alertMe.getEventLog(session.sessionKey, service, limitDiff, currentEnd, "null");
						ArrayList<Event> append = APIUtilities.getEventLog(rawRes);
						lastRawAPIResult = rawRes;
						if (DEBUGOUT) Log.w(TAG, "helperGetEventLog()  appending future with (limit:"+limitDiff+", start:"+currentEnd+", end:null)");
						if (activeHubId!=-1) db.updateEvents(activeHubId, append);

						if (!append.isEmpty()) {
							Collections.sort(append, Event.getComparator(true));
							for (Event e: append) {
								events.add(e);
								limitDiff--;
							}
							addedToList = true;
							if (DEBUGOUT) Log.w(TAG, "helperGetEventLog()  append current: ammending list size: "+append.size());
						}
						
					}
					// Appending the past
					if (currentStart!=null && limitDiff>0) {
						String rawRes = alertMe.getEventLog(session.sessionKey, service, limitDiff, start, currentStart);
						ArrayList<Event> append = APIUtilities.getEventLog(rawRes);
						lastRawAPIResult = rawRes;
						if (DEBUGOUT) Log.w(TAG, "helperGetEventLog()  appending past with (limit:"+limitDiff+", start:"+start+", end:"+currentStart+")");
						if (activeHubId!=-1) db.updateEvents(activeHubId, append);

						if (!append.isEmpty()) {
							Collections.sort(append, Event.getComparator(true));
							for (Event e: append) {
								events.add(e);
								limitDiff--;
							}
							addedToList = true;
							if (DEBUGOUT) Log.w(TAG, "helperGetEventLog()  append past: ammending list size: "+append.size());
						}
					}
					if (addedToList) Collections.sort(events, Event.getComparator(true));
				}
			}
			
			eventLastRefresh = System.currentTimeMillis();
		}
		if (DEBUGOUT) Log.w(TAG, "helperGetEventLog()  END");
	}
	
	private static boolean timeIsWithinSpan(long baseTimeMilli, long spanMilli) {
		boolean res = false;
		if (baseTimeMilli<=0) {
			res = false;
		} else {
			long currentTime = System.currentTimeMillis();
			if (currentTime>=baseTimeMilli) {
				long diff = currentTime - baseTimeMilli;
				res = diff < spanMilli;
				if (DEBUGOUT) {
					long spanMin = spanMilli / 60000;
					long diffMin = diff / 60000;
					Log.w(TAG, "timeIsWithinSpan(span: "+spanMilli+"/"+spanMin+" minutes)  RETURNING::"+res+" for "+baseTimeMilli+" where difference is "+diff+"/"+diffMin+" minute(s)");
				}
			} else {
				// Current time is in the past of baseTimeMilli
				res = true;
			}
		}
		return res;
	}
	
	private boolean helperLogin() {
		boolean res = false;
		if (session!=null && APIUtilities.isStringNonEmpty(session.username) 
				&& APIUtilities.isStringNonEmpty(session.password)) {
			String sess = alertMe.login(session.username, session.password);
			lastRawAPIResult = sess;
			if (APIUtilities.isStringNonEmpty(sess) && AlertMeServer.Exceptions.loginOkFromRawResult(sess)) {
				session.sessionKey = sess;
				db.updateUserEntrySession(session.id, sess); // Store!
				res = true;
				session.timestamp = System.currentTimeMillis();
			}
		}
		return res;
	}
	public boolean loadActiveHub() {
		boolean loaded = false;
		if (DEBUGOUT) Log.w(TAG, "loadActiveHub():: START");
		if (activeHub!=null && session!=null) {
			String rawRes = alertMe.getHubStatus(session.sessionKey);
			HashMap<String, String> hubStatus = APIUtilities.getDeviceChannelValues(rawRes);
			lastRawAPIResult = rawRes;
			if (DEBUGOUT) Log.w(TAG, "loadActiveHub()::  hubStatus ["+rawRes+"]");
			if (hubStatus!=null) {
				if (hubStatus.containsKey("isavailable")) {					
					String avail = hubStatus.get("isavailable");
		    		isHubActive = (avail!=null && avail.trim().toLowerCase().equals("yes"));
		    		loaded = isHubActive;
				}
			}
			if (DEBUGOUT) Log.w(TAG, "loadActiveHub()::  isHubActive ["+isHubActive+"]");
		}
		if (DEBUGOUT) Log.w(TAG, "loadActiveHub():: END");
		return loaded;
	}
	private void helperLoadActiveHub() {
		boolean didRefresh = false;
		if (activeHub==null && hubs!=null && !hubs.isEmpty() && hubs.size()==1) {
			// pick the first hub as active if only a list of 1 hub
			activeHub = hubs.get(0);
		}
		if (activeHub!=null && session!=null) {
			loadActiveHub();
			activeHub.behaviour = alertMe.getBehaviour(session.sessionKey);
			activeHub.setServicesFromString(alertMe.getAllServices(session.sessionKey));
			activeHubId = db.getHubId(session.id, activeHub.id);
			lastRawAPIResult = activeHub.behaviour;
			// Save changes to the DB
			if (activeHubId==-1) {
				activeHubId = db.addHub(session.id, activeHub);
			} else {
				db.updateHubData(activeHubId, activeHub.name, activeHub.id, activeHub.behaviour, activeHub.getServicesString());
			}
			// Also refresh the cached hubs
			if (hubs!=null && !hubs.isEmpty()) {
				int hi = 0;
				for(Hub h: hubs) {
					if (h.id.equals(activeHub.id)) {
						hubs.set(hi, activeHub);// Save the hub data to cache
						break;
					}
					hi++;
				}
			}
			didRefresh = true;
		}
		if (didRefresh) {
			hubSettingsLastRefresh = System.currentTimeMillis();
		}
	}
	private void helperLoadHubs() {
		if (DEBUGOUT) Log.w(TAG, "helperLoadHubs:: START");
		if (hubs==null) {
			if (DEBUGOUT) Log.w(TAG, "helperLoadHubs  -- null hubs: calling getAllHubs");
			if (hasSessionKey()) {
				String rawRes = alertMe.getAllHubs(session.sessionKey);
				hubs = APIUtilities.getAllHubs(rawRes);
				lastRawAPIResult = rawRes;
				if (DEBUGOUT) Log.w(TAG, "helperLoadHubs  -- null hubs: calling getAllHubs: DONE");
				if(hubs!=null && !hubs.isEmpty()) {
					int hSize = hubs.size();
					if (DEBUGOUT) Log.w(TAG, "helperLoadHubs  -- getAllHubs returned results");
					// Store these right away!
					if (session.id>=0) {
						boolean added = db.updateHubs(session.id, hubs);
						if (DEBUGOUT) Log.w(TAG, "helperLoadHubs  -- storing hubs to DB::" + added+" where hub size::"+hSize);
					}
					
					// See if activeHub and activeHubId has been set
					// before overriding
					if (activeHub!=null) {
						boolean activeHubChecked = false;
						for (Hub hub: hubs) {
							if (activeHub.id.equals(hub.id)) {
								activeHubChecked = true;
								break;
							}
						}
						if (!activeHubChecked) {
							activeHub = null;
							activeHubId = -1;
						}
					} else if (activeHubId!=-1) {
						// active null is dead.. check if this session owns this id
						if (session!=null && session.id>=0) {
							boolean activeHubChecked = false;
							AlertMeStorage.AlertMeUser huser = db.getUserByHub(session.id);
							activeHubChecked = (huser!=null && session.username.equals(huser.username));

							if (!activeHubChecked) {
								activeHub = null;
								activeHubId = -1;
							}
						}
					}
					// picked the first hub to view
					helperLoadActiveHub();

					if (hSize>1 && activeHub!=null) {
						lastRawAPIResult = alertMe.setHub(session.sessionKey, activeHub.id);
					}
					
					if (session.id>=0) {
						long hid = -1;
						if (activeHub!=null && activeHubId!=-1) {
							hid = db.getHubId(session.id, activeHub.id);
							if (hid!=-1) {
								activeHubId = hid;
							}
						}
					}
				}
				hubsLastRefresh = System.currentTimeMillis();
			}
		}
		if (DEBUGOUT) Log.w(TAG, "helperLoadHubs:: END");
	}
	private void helperLoadUserAndHubFromSettings(long userId, long hubId) {
		long invalid = -1;
		AlertMeStorage.AlertMeUser user = db.getUser(userId);
		if (user!=null && user.id!=invalid) {
			session = user;
			if (hubId!=invalid) {
				activeHub = db.getHub(hubId);
				if (activeHub!=null) {
					activeHubId = hubId;
				}
			}
			//res = loginAsUser(uId);
			if (activeHub==null) {
				helperLoadLastActiveHub();
			}
		}
	}
	// using getAllDeviceChannelValues has it's downsides:
	private void helperLoadMissingValues(Device device) {
		String[] checkAttrs = null;
		switch(device.type) {
			case Device.ALARM_DETECTOR:
				checkAttrs = Device.alarmDetectorAttributes;
				break;
			case Device.BUTTON:
				checkAttrs = Device.buttonAttributes;
				break;
			case Device.CAMERA:
				checkAttrs = Device.cameraAttributes;
				break;
			case Device.CONTACT_SENSOR:
				checkAttrs = Device.contactSensorAttributes;
				break;
			case Device.KEYFOB:
				checkAttrs = Device.keyfobAttributes;
				break;
			case Device.LAMP:
				checkAttrs = Device.lampAttributes;
				break;
			case Device.MOTION_SENSOR:
				checkAttrs = Device.motionSensorAttributes;
				break;
			case Device.POWER_CONTROLLER:
				checkAttrs = Device.powerPlugAttributes;
				break;
			case Device.POWERCLAMP:
				checkAttrs = Device.meterAttributes;
				break;
		}
		if (checkAttrs!=null) {
			String val = null;
			String tmpRes = null;
			HashMap<String, String> attrs = null;
			for (String key: checkAttrs) {
				val = device.getAttribute(key);
				if (val==null) {
					if (tmpRes==null) {
						tmpRes = alertMe.getDeviceChannelValue(session.sessionKey, device.id);
						lastRawAPIResult = tmpRes;
						attrs = Device.getAttributesFromString(tmpRes);					
					}
					if (attrs!=null && attrs.containsKey(key)) {
						val = attrs.get(key);
						device.attributes.put(key, val);
					}
				}
			}			
		}
	}
	private void helperLoadDevices() {
		if (devices==null) {
			if (hasSessionKey() && activeHub!=null && activeHubId!=-1) {
				String rawRes = alertMe.getAllDeviceChannelValues(session.sessionKey);
				devices = APIUtilities.getAllDevices(rawRes);
				lastRawAPIResult = rawRes;
				if(devices!=null && !devices.isEmpty()) {
					// 20120928 stupid getAllDeviceChannelValues doesn't get ALL channel values.. FAIL
					for (Device device: devices) {
						helperLoadMissingValues(device);
					}
					if (session.id!=-1) {
						db.updateDevices(activeHubId, devices);
					}
				}
				/*
				// 20120206 stupid getDeviceChannel doesn't do negatives.. FAIL
				String rawRes = alertMe.getAllDevices(session.sessionKey);
				devices = APIUtilities.getAllDevices(rawRes);
				lastRawAPIResult = rawRes;
				if(devices!=null && !devices.isEmpty()) {
					// Load all the data for the devices
					String tmpRes;
					int count = 0;
					for (Device device: devices) {
						tmpRes = alertMe.getDeviceChannelValue(session.sessionKey, device.id);
						lastRawAPIResult = tmpRes;
						device.setAttributesFromString(tmpRes);
						devices.set(count++, device);
					}
					if (session.id!=-1) {
						db.updateDevices(activeHubId, devices);
					}
				}
				*/
				deviceLastRefresh = System.currentTimeMillis();
			}
		}
	}
	private void helperKeepAlive() {
		if (hasSessionValues() && !isSessionAlive()) {
			boolean ok = login();
			if (!ok) {
				session = null;
			}
		}
	}
	
	//// BLAUH
//	private void foo() {
//		alertMe.getAllBehaviours(sessionKey);
//		alertMe.getAllDevices(sessionKey);
//		alertMe.getAllHubs(sessionKey);
//		alertMe.getAllServices(sessionKey);
//		alertMe.getAllServiceStates(sessionKey, service);
//		//alertMe.getBatteryLevel(sessionKey, deviceId);
//		alertMe.getBehaviour(sessi)onKey);
//		alertMe.getCurrentServiceState(sessionKey, service);
//		alertMe.getDeviceChannelValue(sessionKey, deviceId);
//		alertMe.getDeviceChannelValue(sessionKey, deviceId, attribute);
//		alertMe.getDeviceDetails(sessionKey, deviceId);
//		//alertMe.getEventLog(sessionKey, limit)
//		alertMe.getEventLog(sessionKey, service, limit, start, end);
//		alertMe.getHubStatus(sessionKey);
//		//alertMe.getPresence(sessionKey, deviceId);
//		//alertMe.getRelayState(sessionKey, deviceId);
//		//alertMe.getTemperature(sessionKey, deviceId)
//		alertMe.getUserInfo(sessionKey)
//		alertMe.login(username, password);
//		alertMe.logout(sessionKey);
//		alertMe.sendCommand(sessionKey, deviceLabel, deviceCommand, deviceId);
//		//alertMe.setBehaviourMode(sessionKey, mode)
//		alertMe.setHub(sessionKey, hubId);
//		//alertMe.setRelayState(sessionKey, mode, deviceId)
//	}
	
	// This is for storing a cache (and reloading) if we have a need to restore 
	// the elements without doing any API or DB calls
	public static class SessionState {
		public AlertMeStorage.AlertMeUser session = null; // Stores the session
		public Hub activeHub = null;     // Current active hub, with behaviour Home | Away | Night
		public long activeHubId = -1;

		public ArrayList<Hub> hubs = null;
		public ArrayList<Device> devices = null;
		public ArrayList<Event> events = null;
		public boolean isHubActive = false;
		public long hubsLastRefresh = 0;
		public long deviceLastRefresh = 0;
		public long eventLastRefresh = 0;
		public long hubSettingsLastRefresh = 0;
		public long cacheAge = 0; // for comparing cache ages
		public String lastRawAPIResult = null;
	}
	
}
