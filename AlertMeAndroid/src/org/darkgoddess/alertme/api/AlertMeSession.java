package org.darkgoddess.alertme.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.darkgoddess.alertme.api.utils.APIUtilities;
import org.darkgoddess.alertme.api.utils.Device;
import org.darkgoddess.alertme.api.utils.Event;
import org.darkgoddess.alertme.api.utils.Hub;

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
	
	public static final String PREFERENCE_SETTING_ID = "alertMeSessionUserId";
	public static final String PREFERENCE_SETTING_USERID = "userId";
	public static final String PREFERENCE_SETTING_HUBID = "hubId";
	
	private static final long SESSION_SPAN = 1000*60*10; // 10 minute sessions (session key and event)
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
		if (conMgr.getActiveNetworkInfo() != null &&
				conMgr.getActiveNetworkInfo().isAvailable() &&
				conMgr.getActiveNetworkInfo().isConnected()) {
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
			alertMe.logout(session.sessionKey);
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
				String mode = (isRelayState)? "on": "off";
				String rawRes = alertMe.setRelayState(session.sessionKey, mode, device.id);
				int isOk = APIUtilities.getCommandResult(rawRes);
				res = (isOk == APIUtilities.COMMAND_OK);
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
							String newMode = (isRelayState)? "True": "False";
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
	
	public ArrayList<Event> getEventData() {
		helperKeepAlive();

		if (session!=null && session.id>=0) {
			if (!timeIsWithinSpan(eventLastRefresh, ITEM_SPAN)) {
				events = null;
				helperGetEventLog();
			} else {
				if (events==null||events!=null&&events.isEmpty()) {
					if (activeHubId!=1) events = db.getEvents(activeHubId);
				}
			}
			lastActionTime = System.currentTimeMillis();
		}
		
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
		String sess = alertMe.login(username, password);
		
		if (APIUtilities.isStringNonEmpty(sess)) {
			// create all the details as possible
			long timestamp = System.currentTimeMillis();
			long uid = -1;
			String userinfo = alertMe.getUserInfo(sess);
			uid = db.addUser(username, password, sess, timestamp, userinfo);
			accountSizeCache = db.getUsersSize();
			if (uid!=-1) {
				res = new AlertMeStorage.AlertMeUser(uid, username, password, sess, timestamp, userinfo);		
			}
		}
		return res;
	}
	
	public HashMap<String, String> getUserInfo() {
		if (isSessionAlive()) return APIUtilities.getDeviceChannelValues(alertMe.getUserInfo(session.sessionKey));
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
			alertMe.logout(session.sessionKey);
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
		if (session!=null && session.id>=0) {
			String service = "null";
			int limit = 50;
			String start = "null";
			String end = "null";
			boolean getAllCurrent = false;
			// TODO: get stuff from DB
			if (events==null|| (events!=null && events.isEmpty())) {
				getAllCurrent = true;
			}
			if (getAllCurrent) {
				events = APIUtilities.getEventLog(alertMe.getEventLog(session.sessionKey, service, limit, start, end));
				if (activeHubId!=1) db.updateEvents(activeHubId, events);
				if (!events.isEmpty()) Collections.sort(events, Event.getComparator(true));
			} else {
				String currentStart = null;
				String currentEnd = null;
				int limitDiff = limit;
				int eventSize = events.size();
				
				if (eventSize>0) {
					int lastI = (eventSize-1);
					Event first = events.get(0);
					Event last = events.get(lastI);
					if (last!=null) {
						currentStart = last.epochTimestamp + "";
					}
					if (first!=null) {
						currentEnd = first.epochTimestamp + "";
					}
					
					// Appending the past
					if (currentStart!=null) {
						ArrayList<Event> append = APIUtilities.getEventLog(alertMe.getEventLog(session.sessionKey, service, limit, start, currentStart));
						if (activeHubId!=1) db.updateEvents(activeHubId, append);

						if (!append.isEmpty()) {
							Collections.sort(append, Event.getComparator(true));
							for (Event e: append) {
								events.add(e);
								limitDiff--;
							}
						}
					}
					// Appending the future
					if (currentEnd!=null && limitDiff>0) {
						ArrayList<Event> append = APIUtilities.getEventLog(alertMe.getEventLog(session.sessionKey, service, limitDiff, currentEnd, null));
						if (activeHubId!=1) db.updateEvents(activeHubId, append);

						if (!append.isEmpty()) {
							Collections.sort(append, Event.getComparator(true));
							for (Event e: append) {
								events.add(e);
								limitDiff--;
							}
						}
						
					}
					
				}
			}
			
			eventLastRefresh = System.currentTimeMillis();
		}
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
			if (APIUtilities.isStringNonEmpty(sess)) {
				session.sessionKey = sess;
				db.updateUserEntrySession(session.id, sess); // Store!
				res = true;
				session.timestamp = System.currentTimeMillis();
			}
		}
		return res;
	}
	public void loadActiveHub() {
		if (DEBUGOUT) Log.w(TAG, "loadActiveHub():: START");
		if (activeHub!=null && session!=null) {
			String rawRes = alertMe.getHubStatus(session.sessionKey);
			HashMap<String, String> hubStatus = APIUtilities.getDeviceChannelValues(rawRes);
			if (DEBUGOUT) Log.w(TAG, "loadActiveHub()::  hubStatus ["+rawRes+"]");
			if (hubStatus!=null) {
				if (hubStatus.containsKey("isavailable")) {					
					String avail = hubStatus.get("isavailable");
		    		isHubActive = (avail!=null && avail.trim().toLowerCase().equals("yes"));
				}
			}
			if (DEBUGOUT) Log.w(TAG, "loadActiveHub()::  isHubActive ["+isHubActive+"]");
		}
		if (DEBUGOUT) Log.w(TAG, "loadActiveHub():: END");
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
				hubs = APIUtilities.getAllHubs(alertMe.getAllHubs(session.sessionKey));
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
						alertMe.setHub(session.sessionKey, activeHub.id);
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
	private void helperLoadDevices() {
		if (devices==null) {
			if (hasSessionKey() && activeHub!=null && activeHubId!=-1) {
				devices = APIUtilities.getAllDevices(alertMe.getAllDevices(session.sessionKey));
				if(devices!=null && !devices.isEmpty()) {
					// Load all the data for the devices
					int count = 0;
					for (Device device: devices) {
						device.setAttributesFromString(alertMe.getDeviceChannelValue(session.sessionKey, device.id));
						devices.set(count++, device);
					}
					if (session.id>=0) {
						db.updateDevices(activeHubId, devices);
					}
				}
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
		
	}
	
}