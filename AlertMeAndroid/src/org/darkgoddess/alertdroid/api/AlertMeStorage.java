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

import org.darkgoddess.alertdroid.api.utils.APIUtilities;
import org.darkgoddess.alertdroid.api.utils.Device;
import org.darkgoddess.alertdroid.api.utils.Event;
import org.darkgoddess.alertdroid.api.utils.Hub;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * @author      foo bar <address@ example.com>
 * @version     201111.0210
 * @since       1.6
 */
public class AlertMeStorage {
	private static final boolean DEBUGOUT = false;	
	private final Context context;
	private DatabaseHelper dbHelper;
	private SQLiteDatabase db;

	public AlertMeStorage(Context ctx) {
		context = ctx;
	}
	public AlertMeStorage open() throws SQLException {
		dbHelper = new DatabaseHelper(context);
		db = dbHelper.getWritableDatabase();
		return this;
	}
	public void close() {
		// Run on exit
		if (db!=null) {
			db.close();
			db = null;
		}	
	}

	public long addUser(String username, String password, String sessId, long timestamp, String data) {
		long res = -1;
        if (isStringValid(username) && isStringValid(password)) {
            ContentValues initialValues = new ContentValues();
            initialValues.put("username", username);
            initialValues.put("password", password);
            if (isStringValid(data)) { initialValues.put("session_id", sessId); }
			if (timestamp>-1) { initialValues.put("timestamp", timestamp); }
			initialValues.put("info", data);
            res = db.insert(tableUser, null, initialValues);
        }
        return res;
	}
	public long addUser(String username, String password, String sessId, String data) {
		return addUser(username, password, sessId, System.currentTimeMillis(), data);
	}
	public long createUserEntry(final String user, final String passwd) {
		long res = -1;
		if (isStringValid(user) && isStringValid(passwd)) {
			ContentValues initialValues = new ContentValues();
			initialValues.put("username", user);
			initialValues.put("password", passwd);
			res = db.insert(tableUser, null, initialValues);
		}
		return res;
	}
	public ArrayList<AlertMeUser> getUsers() {
		ArrayList<AlertMeUser> users = new ArrayList<AlertMeUser>();
		Cursor entries = null;
		try {
			entries = db.query(tableUser, userTableColumns, null, null, null, null, null);
			if (entries!=null && entries.getCount()!=0) {
				entries.moveToFirst();
				do {
					AlertMeUser user = new AlertMeUser(
							getLongFromCursor(KEY_ROWID, entries),
							getStringFromCursor("username", entries),
							getStringFromCursor("password", entries),
							getStringFromCursor("session_id", entries),
							getLongFromCursor("timestamp", entries),
							getStringFromCursor("info", entries));
					if (user.id>=0 && user.username!=null && user.username.length()!=0) {
						users.add(user);
					}
				} while (entries.moveToNext());
			}
		} catch (SQLException se) {
			if (DEBUGOUT) Log.w(TAG, "getUsers() FAILED [SQLException] " + se.getMessage());
		} catch (Exception e) {
			if (DEBUGOUT) Log.w(TAG, "getUsers() FAILED [Exception] " + e.getMessage());
		} finally {
			if (entries!=null) { entries.close(); }
		}
		if (entries!=null) { entries.close(); }
		return users;
	}
	public AlertMeUser getUserByUsername(String username) {
		AlertMeUser res = null;
		Cursor entry = null;
		try {
			entry = db.query(true, tableUser, userTableColumns, "username = '"+username+"'", null, null, null, null, null);
			res = new AlertMeUser(
					getLongFromCursor(KEY_ROWID, entry),
					getStringFromCursor("username", entry),
					getStringFromCursor("password", entry),
					getStringFromCursor("session_id", entry),
					getLongFromCursor("timestamp", entry),
					getStringFromCursor("info", entry));
		} catch (SQLException se) {
			if (DEBUGOUT) Log.w(TAG, "getUsers() FAILED [SQLException] " + se.getMessage());
		} catch (Exception e) {
			if (DEBUGOUT) Log.w(TAG, "getUsers() FAILED [Exception] " + e.getMessage());
		} finally {
			if (entry!=null) { entry.close(); }
		}
		if (entry!=null) { entry.close(); }
		return res;
	}
	public AlertMeUser getUserByHub(long hubId) {
		AlertMeUser res = null;
		if (hubId>=0) {
			Cursor entry = null;
			try {
				entry = fetchSystemEntry(tableHub, hubTableColumns, hubId);
				if (entry!=null && entry.getCount()!=0) {
					HubCache cache = null;
					entry.moveToFirst();
					cache = getHubCacheFromCursor(entry);
					if (cache!=null) {
						res = this.getUser(cache.user_id);
					}
				}
			} catch (SQLException se) {
			} catch (Exception e) {
			} finally {
				if (entry!=null) { entry.close(); }
			}
			if (entry!=null) { entry.close(); }
		}
		
		return res;
	}
	public AlertMeUser getUser(final long userId) {
		AlertMeUser res = null;
		
		if (userId>=0) {
			Cursor entry = null;
			try {
				entry = fetchSystemEntry(tableUser, userTableColumns, userId);
				if (entry!=null && entry.getCount()!=0) {
					//long i, String user, String passwd, String sess, String data, Long epoch
					res = new AlertMeUser(
							getLongFromCursor(KEY_ROWID, entry),
							getStringFromCursor("username", entry),
							getStringFromCursor("password", entry),
							getStringFromCursor("session_id", entry),
							getLongFromCursor("timestamp", entry),
							getStringFromCursor("info", entry));
				}
			} catch (SQLException se) {
			} catch (Exception e) {
			} finally {
				if (entry!=null) { entry.close(); }
			}
			if (entry!=null) { entry.close(); }
		}
		
		return res;
	}
	public boolean updateUserEntryLogin(final long userId, String username, String password) {
		return updateUserEntryLogin(userId, username, password, null, 0, null);
	}
	public boolean updateUserEntryLogin(final long userId, String username, String password, String sessId, long timestamp, String data) {
        if (isStringValid(username) && isStringValid(password)) {
            ContentValues args = new ContentValues();
			args.put("username", username);
			args.put("password", password);
            if (isStringValid(data)) { args.put("session_id", sessId); }
			if (timestamp>-1) { args.put("timestamp", timestamp); }
			args.put("info", data);
			return db.update(tableUser, args, KEY_ROWID + "=" + userId, null) > 0;
        }
        return false;
	}
	public boolean updateUserEntrySession(final long userId, String sessId, long timestamp, String data) {
        if (isStringValid(sessId)) {
            ContentValues args = new ContentValues();
			args.put("session_id", sessId);
			args.put("timestamp", timestamp);
			if (isStringValid(data)) { args.put("info", data); }
			return db.update(tableUser, args, KEY_ROWID + "=" + userId, null) > 0;
		}
		return false;
	}
	public boolean updateUserEntryInfo(final long userId, String info) {
        if (isStringValid(info)) {
            ContentValues args = new ContentValues();
			args.put("info", info);
			return db.update(tableUser, args, KEY_ROWID + "=" + userId, null) > 0;
		}
		return false;
	}
	public boolean updateUserEntrySession(final long userId, String sessId) {
		return updateUserEntrySession(userId, sessId, System.currentTimeMillis(), null);
	}
	public int getUsersSize() {
		int res = 0;		
		Cursor entries = db.query(tableUser, userTableColumns, null, null, null, null, null);
		if (entries!=null) {
			res = entries.getCount();
			entries.close();
		}

		return res;
	}
	public boolean deleteUserEntry(long userId) {
		int userRows = 0;
		// First remove all the events, devices and then hubs
		Cursor entry = null;
		try {
			//int internalCount = 0;
			entry = db.query(true, tableHub, hubTableColumns, "user_id" + "=" + userId, null, null, null, null, null);
			if (entry!=null && entry.getCount()!=0) {
				entry.moveToFirst();
				do {
					long hubId = getLongFromCursor(KEY_ROWID, entry);
					//internalCount += db.delete(tableEvent, "hub_id = " + hubId, null);
					//internalCount += db.delete(tableDevice, "hub_id = " + hubId, null);
					db.delete(tableEvent, "hub_id = " + hubId, null);
					db.delete(tableDevice, "hub_id = " + hubId, null);
				} while (entry.moveToNext());
			}
		} catch (SQLException se) {
			if (DEBUGOUT) Log.w(TAG, "deleteUserEntry() FAILED [SQLException] " + se.getMessage());
		} catch (Exception e) {
			if (DEBUGOUT) Log.w(TAG, "deleteUserEntry() FAILED [Exception] " + e.getMessage());
		} finally {
			if (entry!=null) { entry.close(); }
		}
		if (entry!=null) entry.close();
		
		
		userRows = db.delete(tableUser, KEY_ROWID + "=" + userId, null);
		return (userRows>0);
	}

	public long getDeviceId(long hubId, String deviceZId) {
		long res = -1;
		String select = "hub_id" + "=" + hubId + " AND zid = '"+deviceZId+"'";
		Cursor cursor = db.query(true, tableDevice, deviceTableColumns, select, null, null, null, null, null);
		if (cursor!=null) {
			cursor.moveToFirst();
			if (DEBUGOUT) Log.w(TAG, "getDeviceId()  select: [" + select + "]    COUNT:: "+cursor.getCount());
			if (cursor.getCount()>0) {
				res = getLongFromCursor(KEY_ROWID, cursor);				
			}
			if (cursor!=null) cursor.close();
		}
		
		return res;
	}
	public ArrayList<Device> getDevices(long hubId) {
		ArrayList<Device> devices = new ArrayList<Device>();
		Cursor entry = null;
		try {
			entry = db.query(true, tableDevice, deviceTableColumns, "hub_id" + "=" + hubId, null, null, null, null, null);
			if (entry!=null && entry.getCount()!=0) {
				entry.moveToFirst();
				do {
					Device d = getDeviceFromCursor(entry);
					if (d!=null) {
						devices.add(d);
					}
				} while (entry.moveToNext());
			}
		} catch (SQLException se) {
			if (DEBUGOUT) Log.w(TAG, "getDevices() FAILED [SQLException] " + se.getMessage());
		} catch (Exception e) {
			if (DEBUGOUT) Log.w(TAG, "getDevices() FAILED [Exception] " + e.getMessage());
		} finally {
			if (entry!=null) { entry.close(); }
		}
		if (entry!=null) { entry.close(); }
		
		return devices;
	}

	public int getDeviceSize(long hubId) {
		int res = 0;		
		Cursor entries = db.query(tableDevice, deviceTableColumns, "hub_id" + "=" + hubId, null, null, null, null);
		if (entries!=null) {
			res = entries.getCount();
			entries.close();
		}
			
		return res;
	}
	public long getLatestUpdateDevices(long hubId) {
		long res = -1;
		
		Cursor entries = null;
		try {
			entries = db.query(true, tableDevice, deviceTableColumns, "hub_id" + "=" + hubId, null, null, null, "timestamp desc", null);
			if (entries!=null && entries.getCount()!=0) {
				entries.moveToFirst();
				res = getLongFromCursor("timestamp", entries);
			}
		} catch (SQLException se) {
		} catch (Exception e) {
		} finally {
			if (entries!=null) { entries.close(); }
		}
		if (entries!=null) { entries.close(); }
		
		return res;
	}
	//private static final String[] deviceTableColumns = { KEY_ROWID, "hub_id", "name", "zid", "type", "attributes", "timestamp" };
	public long addDevice(final long hubId, final String name, final String zid, final int type, final String attributes, final long timestamp) {
		long res = -1;
        if (isStringValid(name) && isStringValid(zid) && hubId>-1) {
            ContentValues initialValues = new ContentValues();
            initialValues.put("hub_id", hubId);
            initialValues.put("name", name);
            initialValues.put("zid", zid);
            initialValues.put("type", type);
            initialValues.put("attributes", attributes); 
            initialValues.put("timestamp", timestamp);
            res = db.insert(tableDevice, null, initialValues);
        }
		
		return res;
	}
	public long addDevice(final long hubId, Device device) {
		return addDevice(hubId, device.name, device.id, device.type, device.getAttributesString(), System.currentTimeMillis());
	}
	public Device getDevice(final long deviceId) {
		Device res = null;
		Cursor entry = null;
		try {
			entry = fetchSystemEntry(tableDevice, deviceTableColumns, deviceId);
			////entry = db.query(true, tableDevice, deviceTableColumns, KEY_ROWID + "=" + deviceId, null, null, null, null, null);
			if (entry!=null && entry.getCount()!=0) {
				entry.moveToFirst();
				res = getDeviceFromCursor(entry);
			}
		} catch (SQLException se) {
			if (DEBUGOUT) Log.w(TAG, "getDevice() FAILED [SQLException] " + se.getMessage());
		} catch (Exception e) {
			if (DEBUGOUT) Log.w(TAG, "getDevice() FAILED [Exception] " + e.getMessage());
		} finally {
			if (entry!=null) { entry.close(); }
		}
		if (entry!=null) { entry.close(); }
		return res;
	}
	public boolean updateDevice(final long deviceId, final int hubId, final String name, final String zid, final int type, final String attributes, final long timestamp) {
        if (isStringValid(name) && isStringValid(zid) && hubId>-1) {
            ContentValues args = new ContentValues();
			args.put("hub_id", hubId);
			args.put("name", name);
			args.put("zid", zid);
			args.put("type", type);
			args.put("attributes", attributes); 
			args.put("timestamp", timestamp);
			return db.update(tableDevice, args, KEY_ROWID + "=" + deviceId, null) > 0;
		}
		return false;
	}
	public boolean updateDevice(final long deviceId, final int hubId, final String name, final String zid, final int type, final String attributes) {
		return updateDevice(deviceId, hubId, name, zid, type, attributes, System.currentTimeMillis());
	}
	public boolean updateDevice(final long deviceId, final String name, final String zid, final int type, final String attributes, final long timestamp) {
        if (isStringValid(name) && isStringValid(zid)) {
            ContentValues args = new ContentValues();
			args.put("name", name);
			args.put("zid", zid);
			args.put("type", type); 
			args.put("attributes", attributes);
			args.put("timestamp", timestamp);
			return db.update(tableDevice, args, KEY_ROWID + "=" + deviceId, null) > 0;
		}
		return false;
	}
	public boolean updateDevice(final long deviceId, final String name, final String zid, final int type, final String attributes) {
		return updateDevice(deviceId, name, zid, type, attributes, System.currentTimeMillis());		
	}
	public boolean updateDevice(final long deviceId, Device device) {
		return updateDevice(deviceId, device.name, device.id, device.type, device.getAttributesString());
	}
	public boolean updateDeviceDataByZID(long hubId, final String name, final String zid, final int type, final String attributes, final long timestamp) {
        if (isStringValid(name) && isStringValid(zid)) {
            ContentValues args = new ContentValues();
			args.put("name", name);
			args.put("zid", zid);
			args.put("type", type); 
			args.put("attributes", attributes);
			args.put("timestamp", timestamp);
			return db.update(tableDevice, args, "zid = " + zid + " AND hub_id = "+hubId, null) > 0;
		}
		return false;
	}
	public boolean updateDeviceDataByZID(long hubId, final String name, final String zid, final int type, final String attributes) {
		return updateDeviceDataByZID(hubId, name, zid, type, attributes, System.currentTimeMillis());
	}
	public boolean updateDeviceDataByZID(long hubId, Device device) {
		return updateDeviceDataByZID(hubId, device.name, device.id, device.type, device.getAttributesString());
	}
	public boolean updateDevices(long hubId, ArrayList<Device> devices) {
		boolean res = false;
		Cursor entry = null;
		if (devices==null || devices!=null && devices.isEmpty()) return res;
		try {
			ArrayList<DeviceCache> currentDevices = new ArrayList<DeviceCache>();
			entry = db.query(true, tableDevice, deviceTableColumns, "hub_id" + "=" + hubId, null, null, null, null, null);
			if (entry!=null && entry.getCount()!=0) {
				entry.moveToFirst();
				do {
					DeviceCache d = getDeviceCacheFromCursor(entry);
					currentDevices.add(d);
				} while (entry.moveToNext());
			}

			// Easy: insert them all!
			if (currentDevices.isEmpty()) {
				long dId;
				for(Device d: devices) {
					dId = addDevice(hubId, d);
					res = res||(dId!=-1);
				}
			} else {
				int changed = helperDevicePickyUpdate(hubId, currentDevices, devices);
				res = (changed>0);
			}
		} catch (SQLException se) {
			if (DEBUGOUT) Log.w(TAG, "updateDevices() FAILED [SQLException] " + se.getMessage());
		} catch (Exception e) {
			if (DEBUGOUT) Log.w(TAG, "updateDevices() FAILED [Exception] " + e.getMessage());
		} finally {
			if (entry!=null) { entry.close(); }
		}
		if (entry!=null) { entry.close(); }
		return res;
	}
	
	public long getHubId(long userId, String hubZId) {
		long res = -1;
		String select = "user_id" + "=" + userId + " AND zid = '"+hubZId+"'";
		Cursor cursor = db.query(true, tableHub, hubTableColumns, select, null, null, null, null, null);
		if (cursor!=null) {
			HubCache hub = null;
			cursor.moveToFirst();
			if (DEBUGOUT) Log.w(TAG, "getHubId()  select: [" + select + "]    COUNT:: "+cursor.getCount());
			if (cursor.getCount()>0) {
				hub = getHubCacheFromCursor(cursor);
				if (hub!=null) {
					res = hub.hubId;
				}			
				
			}
			if (cursor!=null) cursor.close();
		}
		
		return res;
	}
	public long getHubTimestamp(long userId, String hubZId) {
		long res = 0;
		String select = "user_id" + "=" + userId + " AND zid = '"+hubZId+"'";
		Cursor cursor = db.query(true, tableHub, hubTableColumns, select, null, null, null, null, null);
		if (cursor!=null) {
			HubCache hub = null;
			cursor.moveToFirst();
			if (DEBUGOUT) Log.w(TAG, "getHubId()  select: [" + select + "]    COUNT:: "+cursor.getCount());
			hub = getHubCacheFromCursor(cursor);
			if (hub!=null) {
				res = hub.timestamp;
			}			
		}

		if (cursor!=null) cursor.close();
		
		return res;
	}
	public ArrayList<Hub> getHubs(long userId) {
		ArrayList<Hub> hubs = new ArrayList<Hub>();
		Cursor entry = null;
		try {
			entry = db.query(true, tableHub, hubTableColumns, "user_id" + "=" + userId, null, null, null, "timestamp desc", null);
			if (entry!=null && entry.getCount()!=0) {
				entry.moveToFirst();
				do {
					Hub h = getHubFromCursor(entry);
					if (h!=null) {
						hubs.add(h);
					}
				} while (entry.moveToNext());
			}
		} catch (SQLException se) {
			if (DEBUGOUT) Log.w(TAG, "getHubs() FAILED [SQLException] " + se.getMessage());
		} catch (Exception e) {
			if (DEBUGOUT) Log.w(TAG, "getHubs() FAILED [Exception] " + e.getMessage());
		} finally {
			if (entry!=null) { entry.close(); }
		}
		if (entry!=null) { entry.close(); }
		
		return hubs;
	}
	public int getHubSize(long userId) {
		int res = 0;		
		Cursor entries = db.query(tableHub, hubTableColumns, "user_id" + "=" + userId, null, null, null, null);
		if (entries!=null) {
			res = entries.getCount();
			entries.close();
		}

		return res;
	}

	public long getLatestUpdateHubs(long userId) {
		long res = -1;
		
		Cursor entries = null;
		try {
			entries = db.query(true, tableHub, hubTableColumns, "user_id" + "=" + userId, null, null, null, "timestamp desc", null);
			if (entries!=null && entries.getCount()!=0) {
				entries.moveToFirst();
				res = getLongFromCursor("timestamp", entries);
			}
		} catch (SQLException se) {
			if (DEBUGOUT) Log.w(TAG, "getLatestUpdateHubs() FAILED [SQLException] " + se.getMessage());
		} catch (Exception e) {
			if (DEBUGOUT) Log.w(TAG, "getLatestUpdateHubs() FAILED [Exception] " + e.getMessage());
		} finally {
			if (entries!=null) { entries.close(); }
		}
		if (entries!=null) { entries.close(); }
		
		return res;
	}
	//	private static final String[] hubTableColumns = { KEY_ROWID, "user_id", "name", "zid", "behaviour", "services", "timestamp" };
	public long addHub(final long userId, final String name, final String zid, final String behaviour, final String services, final long timestamp) {
		long res = -1;
		if (isStringValid(name) && isStringValid(zid) && userId>-1) {
            ContentValues initialValues = new ContentValues();
            initialValues.put("user_id", userId);
            initialValues.put("name", name);
            initialValues.put("zid", zid);
            initialValues.put("behaviour", behaviour); 
            initialValues.put("services", services);
            initialValues.put("timestamp", timestamp);
			res = db.insert(tableHub, null, initialValues);
		}
		return res;
	}
	public long addHub(final long userId, Hub hub) {
		return addHub(userId, hub.name, hub.id, hub.behaviour, hub.getServicesString(), System.currentTimeMillis());
	}
	public Hub getHub(final long hubId) {
		Hub res = null;
		Cursor entry = null;
		try {
			entry = fetchSystemEntry(tableHub, hubTableColumns, hubId);
			//entry = db.query(true, tableHub, hubTableColumns, KEY_ROWID + "=" + hubId, null, null, null, null, null);
			if (entry!=null && entry.getCount()!=0) {
				entry.moveToFirst();
				res = getHubFromCursor(entry);
			}
		} catch (SQLException se) {
			if (DEBUGOUT) Log.w(TAG, "getHub() FAILED [SQLException] " + se.getMessage());
		} catch (Exception e) {
			if (DEBUGOUT) Log.w(TAG, "getHub() FAILED [Exception] " + e.getMessage());
		} finally {
			if (entry!=null) { entry.close(); }
		}
		if (entry!=null) { entry.close(); }
		return res;
	}
	public boolean updateHub(final long hubId, final int userId, final String name, final String zid, final String behaviour, final String services, final long timestamp) {
        if (isStringValid(name) && isStringValid(zid) && userId>-1) {
            ContentValues args = new ContentValues();
			args.put("user_id", userId);
			args.put("name", name);
			args.put("zid", zid);
			args.put("behaviour", behaviour); 
			args.put("services", services);
			args.put("timestamp", timestamp);
			return db.update(tableHub, args, KEY_ROWID + "=" + hubId, null) > 0;
		}
		return false;
	}
	public boolean updateHub(final long hubId, final int userId, final String name, final String zid, final String behaviour, final String services) {
		return 	updateHub(hubId, userId, name, zid, behaviour, services, System.currentTimeMillis());
	}
	public boolean updateHubData(final long hubId, final String name, final String zid, final String behaviour, final String services, final long timestamp) {
		if (DEBUGOUT) Log.w(TAG, "updateHubData(hubId:"+hubId+", name:"+name+", zid:"+zid+", behaviour:"+behaviour+", services:"+services+", timestamp:"+timestamp+")  START");
        if (isStringValid(name) && isStringValid(zid)) {
            ContentValues args = new ContentValues();
            int rowCount = 0;
			args.put("name", name);
			args.put("zid", zid);
			args.put("behaviour", behaviour); 
			args.put("services", services);
			args.put("timestamp", timestamp);
			rowCount = db.update(tableHub, args, KEY_ROWID + "=" + hubId, null);
    		if (DEBUGOUT) Log.w(TAG, "updateHubData()  END");
			return rowCount > 0;
		}
		if (DEBUGOUT) Log.w(TAG, "updateHubData()  END (FAIL: name/zid null test)");
		return false;
	}
	public boolean updateHubData(final long hubId, final String name, final String zid, final String behaviour, final String services) {
		return updateHubData(hubId, name, zid, behaviour, services, System.currentTimeMillis());
	}
	public boolean updateHubData(final long hubId, Hub hub) {
		return updateHubData(hubId, hub.name, hub.id, hub.behaviour, hub.getServicesString());
	}
	public boolean updateHubDataByZID(long userId, final String name, final String zid, final String behaviour, final String services, final long timestamp) {
		if (DEBUGOUT) Log.w(TAG, "updateHubDataByZID(userId:"+userId+", name:"+name+", zid:"+zid+", behaviour:"+behaviour+", services:"+services+", timestamp:"+timestamp+")  START");
        if (isStringValid(name) && isStringValid(zid)) {
            ContentValues args = new ContentValues();
            int rowCount = 0;
			args.put("name", name);
			args.put("zid", zid);
			args.put("behaviour", behaviour); 
			args.put("services", services);
			args.put("timestamp", timestamp);
			rowCount = db.update(tableHub, args, "zid = " + zid + " AND user_id = "+userId, null);
    		if (DEBUGOUT) Log.w(TAG, "updateHubDataByZID()  END");
			return rowCount > 0;
		}
        if (DEBUGOUT) Log.w(TAG, "updateHubDataByZID()  END (FAIL: name/zid null test)");
		return false;
	}
	public boolean updateHubDataByZID(long userId, final String name, final String zid, final String behaviour, final String services) {
		return updateHubDataByZID(userId, name, zid, behaviour, services, System.currentTimeMillis());		
	}
	public boolean updateHubDataByZID(long userId, Hub hub) {
		return updateHubDataByZID(userId, hub.name, hub.id, hub.behaviour, hub.getServicesString());
	}
	
	public boolean updateHubs(long userId, ArrayList<Hub> hubs) {
		boolean res = false;
		Cursor entry = null;
		if (DEBUGOUT) Log.w(TAG, "updateHubs()  START");
		if (hubs==null || hubs!=null && hubs.isEmpty()) {
			if (DEBUGOUT) Log.w(TAG, "updateHubs()  END (FAIL: no hubs to update)");
			return res;
		}
		try {
			ArrayList<HubCache> currentHubs = new ArrayList<HubCache>();
			entry = db.query(true, tableHub, hubTableColumns, "user_id" + "=" + userId, null, null, null, null, null);
			if (entry!=null && entry.getCount()!=0) {
				entry.moveToFirst();
				do {
					HubCache h = getHubCacheFromCursor(entry);
					if (h!=null) currentHubs.add(h);
				} while (entry.moveToNext());
				entry.close();
			}

			// Easy: insert them all!
			if (currentHubs.isEmpty()) {
				long hId;
				for(Hub hub: hubs) {
					hId = addHub(userId, hub);
					res = res||(hId!=-1);
				}
			} else {
				int changed = helperHubPickyUpdate(userId, currentHubs, hubs);
				res = (changed>0);
			}
		} catch (SQLException se) {
			if (DEBUGOUT) Log.w(TAG, "updateHubs() FAILED [SQLException] " + se.getMessage());
		} catch (Exception e) {
			if (DEBUGOUT) Log.w(TAG, "updateHubs() FAILED [Exception] " + e.getMessage());
		} finally {
			if (entry!=null) { entry.close(); }
		}
		if (DEBUGOUT) Log.w(TAG, "updateHubs()  END");
		if (entry!=null) { entry.close(); }
		return res;
	}
	
	
	public ArrayList<Event> getEvents(long hubId) {
		ArrayList<Event> events = new ArrayList<Event>();
		Cursor entry = null;
		if (DEBUGOUT) Log.w(TAG, "getEvents()  START");
		try {
			entry = db.query(true, tableEvent, eventTableColumns, "hub_id" + "=" + hubId, null, null, null, "timestamp", null);
			if (entry!=null && entry.getCount()!=0) {
				entry.moveToFirst();
				do {
					Event e = getEventFromCursor(entry);
					if (e!=null) {
						events.add(e);
					}
				} while (entry.moveToNext());
			}
		} catch (SQLException se) {
			if (DEBUGOUT) Log.w(TAG, "getEvents() FAILED [SQLException] " + se.getMessage());
		} catch (Exception e) {
			if (DEBUGOUT) Log.w(TAG, "getEvents() FAILED [Exception] " + e.getMessage());
		} finally {
			if (entry!=null) { entry.close(); }
		}
		if (DEBUGOUT) Log.w(TAG, "getEvents()  END with "+events.size()+" events");
		if (entry!=null) { entry.close(); }
		
		return events;
	}

	//private static final String[] deviceTableColumns = { KEY_ROWID, "hub_id", "name", "zid", "type", "attributes", "timestamp" };
	public long addEvent(final long hubId, final String message, final long timestamp) {
		long res = -1;
        if (hubId>-1) {
    		if (DEBUGOUT) Log.w(TAG, "addEvent("+hubId+", '"+message+"', "+timestamp+")  START");

            ContentValues initialValues = new ContentValues();
            initialValues.put("hub_id", hubId);
            initialValues.put("message", message);
            initialValues.put("timestamp", timestamp);
            res = db.insert(tableEvent, null, initialValues);
    		if (DEBUGOUT) Log.w(TAG, "addEvent()  END -- "+res);
        }
		
		return res;
	}
	public long addEvent(final long hubId, final String message) {
		return addEvent(hubId, message, System.currentTimeMillis());
	}
	public long addEvent(final long hubId, Event event) {
		return addEvent(hubId, event.getMessage(), event.epochTimestamp);
	}
	public boolean updateEvents(long hubId, ArrayList<Event> events) {
		boolean res = false;
		Cursor entries = null;
		if (DEBUGOUT) Log.w(TAG, "updateEvents("+hubId+")  START");
		if (events==null || events!=null && events.isEmpty()) {
			if (DEBUGOUT) Log.w(TAG, "updateEvents()  END (FAIL:events empty/null)");
			return res;	
		}
		// First, see if we have events..
		try {
			int rowsAdded = 0;
			entries = db.query(true, tableEvent, eventTableColumns, "hub_id" + "=" + hubId, null, null, null, "timestamp desc", null);
			if (entries!=null && entries.getCount()!=0) {
				boolean startAdding = false;
				EventCache lastEvent = null;
				EventCache firstEvent = null;
				if (entries.getCount()==0) {
					startAdding = true;
				} else {
					entries.moveToFirst();
					lastEvent = getEventCacheFromCursor(entries);
					entries.moveToLast();
					firstEvent = getEventCacheFromCursor(entries);
				}
				Collections.sort(events, Event.getComparator(false));
				for (Event e: events) {
					if (startAdding) {
						long newEvent = addEvent(hubId, e);
						if (DEBUGOUT) Log.w(TAG, "updateEvents() adding ALL");
						if (newEvent!=-1) {
							rowsAdded++;
						}
					} else {
						if (DEBUGOUT) Log.w(TAG, "updateEvents() adding FIRST/LAST");
						if (lastEvent!=null && firstEvent!=null) {
							if (e.epochTimestamp > lastEvent.timestamp) {
								long newEvent = addEvent(hubId, e);
								if (newEvent!=-1) {
									rowsAdded++;
								}
								startAdding = true;
							} else if (e.epochTimestamp<firstEvent.timestamp) {
									long newEvent = addEvent(hubId, e);
									if (newEvent!=-1) {
										rowsAdded++;
									}
							} else {
								String mesgComp = (e.epochTimestamp==firstEvent.timestamp)? firstEvent.message: lastEvent.message;
								if (mesgComp!=null && !mesgComp.equals(e.getMessage())) {
									long newEvent = addEvent(hubId, e);
									if (newEvent!=-1) {
										rowsAdded++;
									}									
								}
							}				
						} else if (firstEvent!=null) {
							// impossible if results exist: there would always be a first and last
						} else if (lastEvent!=null) {
							// impossible if results exist: there would always be a first and last
							
						}
					}
				}
			} else {
				// just add them all
				for (Event e: events) {
					long newEvent = addEvent(hubId, e);
					if (DEBUGOUT) Log.w(TAG, "updateEvents() adding ALL");
					if (newEvent!=-1) {
						rowsAdded++;
					}
				}
			}
			res = (rowsAdded!=0);
		} catch (SQLException se) {
			if (DEBUGOUT) Log.w(TAG, "updateEvents() FAILED [SQLException] " + se.getMessage());
		} catch (Exception e) {
			if (DEBUGOUT) Log.w(TAG, "updateEvents() FAILED [Exception] " + e.getMessage());
		} finally {
			if (entries!=null) { entries.close(); }
		}

		if (entries!=null) { entries.close(); }
		if (DEBUGOUT) Log.w(TAG, "updateEvents()  END result::"+res);
		return res;
	}
	public boolean deleteEvents(long hubId) {
		int internalCount = db.delete(tableEvent, "hub_id = " + hubId, null);
		return (internalCount>0);
	}
	
	public static class AlertMeUser {
		public long id = -1;
		public String username = "";
		public String password = "";
		public String sessionKey = "";
		public String info = "";
		public Long timestamp = null;
		public AlertMeUser() {}
		public AlertMeUser(String user, String passwd) {
			username = user;
			password = passwd;
		}
		public AlertMeUser(long i, String user, String passwd, String sess, Long epoch, String data) {
			id = i;
			username = user;
			password = passwd;
			sessionKey = sess;
			info = data;
			timestamp = epoch;
		}
		public static Comparator<AlertMeUser> getComparator(boolean reverse) {
			Comparator<AlertMeUser> res = null;
			
			if (!reverse) {
				res = new Comparator<AlertMeUser>() {
					@Override
					public int compare(AlertMeUser h1, AlertMeUser h2) {
						return h1.username.compareToIgnoreCase(h2.username); 
					}
				};
			} else {
				res = new Comparator<AlertMeUser>() {
					@Override
					public int compare(AlertMeUser h1, AlertMeUser h2) {
						return h2.username.compareToIgnoreCase(h1.username); 
					}
				};
			}
			return res;
		}
	}
	/**
	 * Private helpers... 
	 */
	
	
	private int helperHubPickyUpdate(final long userId, ArrayList<HubCache> currentHubs, ArrayList<Hub> hubs) {
		ArrayList<HubCache> deleteList = new ArrayList<HubCache>();
		ArrayList<Hub> addList = new ArrayList<Hub>();
		HashSet<String> seenIds = new HashSet<String>(); // matched entries
		boolean foundMatch = false;
		int rowsChanged = 0;

		// First look for matches to insert
		for(HubCache cachedHub: currentHubs) {
			foundMatch = false;

			for(Hub hub: hubs) {
				if (hub.id.equals(cachedHub.zid)) {
					foundMatch = true;
					seenIds.add(hub.id);
					// update the matches
					if (updateHubData(cachedHub.hubId, hub)) {
						rowsChanged++;
					}
					break;
				}
			}
			// If no match, need to delete this!
			if (!foundMatch) {
				deleteList.add(cachedHub);
			}
		}
		// Now check out if any matches the other way
		for(Hub hub: hubs) {
			// skip if we've seen the hub in a match
			if (!seenIds.contains(hub.id)) {
				addList.add(hub);
			}
		}
		// Delete step
		if (!deleteList.isEmpty()) {
			for(HubCache hubc: deleteList) {
				rowsChanged += db.delete(tableHub, KEY_ROWID + " = " + hubc.hubId, null);
			}
		}
		// Add step
		if (!addList.isEmpty()) {
			for(Hub hub: addList) {
				rowsChanged += this.addHub(userId, hub);
			}
			
		}
		return rowsChanged;
	}

	
	private int helperDevicePickyUpdate(final long hubId, ArrayList<DeviceCache> currentDevices, ArrayList<Device> devices) {
		ArrayList<DeviceCache> deleteList = new ArrayList<DeviceCache>();
		ArrayList<Device> addList = new ArrayList<Device>();
		HashSet<String> seenIds = new HashSet<String>(); // matched entries
		boolean foundMatch = false;
		int rowsChanged = 0;

		// First look for matches to insert
		for(DeviceCache cachedDevice: currentDevices) {
			foundMatch = false;

			for(Device device: devices) {
				if (device.id.equals(cachedDevice.zid)) {
					foundMatch = true;
					seenIds.add(device.id);
					// update the matches
					if (this.updateDevice(cachedDevice.deviceId, device)) {
						rowsChanged++;
					}
					break;
				}
			}
			// If no match, need to delete this!
			if (!foundMatch) {
				deleteList.add(cachedDevice);
			}
		}
		// Now check out if any matches the other way
		for(Device device: devices) {
			// skip if we've seen the hub in a match
			if (!seenIds.contains(device.id)) {
				addList.add(device);
			}
		}
		// Delete step
		if (!deleteList.isEmpty()) {
			for(DeviceCache devicec: deleteList) {
				rowsChanged += db.delete(tableDevice, KEY_ROWID + " = " + devicec.deviceId, null);
			}
		}
		// Add step
		if (!addList.isEmpty()) {
			for(Device device: addList) {
				rowsChanged += this.addDevice(hubId, device);
			}
			
		}
		return rowsChanged;
	}


	public static Device getDeviceFromCursor(Cursor cursor) {
		Device res = null;

		if (cursor!=null) {
			String label = getStringFromCursor("name", cursor);
			int type = getIntFromCursor("type", cursor);
			String dId = getStringFromCursor("zid", cursor);
			String rawAtt = getStringFromCursor("attributes", cursor);

			if (dId!=null) {
				res = new Device(label, dId, type);
				res.setAttributesFromString(rawAtt);
			}
		}

		return res;
	}
	public static Event getEventFromCursor(Cursor cursor) {
		Event res = null;
		
		if (cursor!=null) {
			long epoch = getLongFromCursor("timestamp", cursor);
			String rawMesg = getStringFromCursor("message", cursor);
			
			if (epoch>=0 && rawMesg!=null && rawMesg.length()!=0) {
				if (rawMesg.contains("|")) {
					String rawEvent = epoch+"|"+rawMesg;
					res = APIUtilities.getEventFromString(rawEvent);
				} else {
					res = new Event(epoch, rawMesg);
					if (DEBUGOUT) Log.w(TAG, "getEventFromCursor() returning Event("+epoch+", '"+rawMesg+"')");
				}
			}
		}
		
		return res;
	}
	public static Hub getHubFromCursor(Cursor cursor) {
		Hub res = null;
		//"user_id", "name", "zid", "behaviour", "services", "timestamp"
		if (cursor!=null) {
			String name = getStringFromCursor("name", cursor);
			String zId = getStringFromCursor("zid", cursor);
			String behave = getStringFromCursor("behaviour", cursor);
			String servelist = getStringFromCursor("services", cursor);
			
			if (zId!=null) {
				res = new Hub(name, zId, behave);
				res.setServicesFromString(servelist);
			}
		}
		
		return res;
	}
	public static HubCache getHubCacheFromCursor(Cursor cursor) {
		HubCache res = null;
		//"user_id", "name", "zid", "behaviour", "services", "timestamp"
		if (cursor!=null) {
			long hId = getLongFromCursor(KEY_ROWID, cursor);
			long uId = getLongFromCursor("user_id", cursor);
			String name = getStringFromCursor("name", cursor);
			String zId = getStringFromCursor("zid", cursor);
			String behave = getStringFromCursor("behaviour", cursor);
			String servelist = getStringFromCursor("services", cursor);
			long ts = getLongFromCursor("timestamp", cursor);
			
			if (hId!=0) {
				res = new HubCache(hId, uId, name, zId, behave, servelist, ts);
			}
		}
		
		return res;
	}

	public static DeviceCache getDeviceCacheFromCursor(Cursor cursor) {
		DeviceCache res = null;
		// { KEY_ROWID, "hub_id", "name", "zid", "type", "attributes", "timestamp" };
		if (cursor!=null) {
			long dId = getLongFromCursor(KEY_ROWID, cursor);
			long hId = getLongFromCursor("hub_id", cursor);
			String name = getStringFromCursor("name", cursor);
			String zId = getStringFromCursor("zid", cursor);
			int dtype = getIntFromCursor("type", cursor);
			String attr = getStringFromCursor("attributes", cursor);
			long ts = getLongFromCursor("timestamp", cursor);
			
			if (dId!=0) {
				res = new DeviceCache(dId, hId, name, zId, dtype, attr, ts);
			}
		}
		
		return res;
	}
	
	public static EventCache getEventCacheFromCursor(Cursor cursor) {
		EventCache res = null;

		if (cursor!=null) {
			long eId = getLongFromCursor(KEY_ROWID, cursor);
			long hId = getLongFromCursor("hub_id", cursor);
			String mesg = getStringFromCursor("message", cursor);
			long ts = getLongFromCursor("timestamp", cursor);
			
			if (eId!=0) {
				res = new EventCache(eId, hId, mesg, ts);
			}
		}
				
		return res;
	}
	

	private Cursor fetchSystemEntry(String tableName, String[] tableFetchRows, long rowId) throws SQLException {
		Cursor mCursor = db.query(true, tableName, tableFetchRows, KEY_ROWID + "=" + rowId, null, null, null, null, null);
		if (mCursor != null) {
		    mCursor.moveToFirst();
		}
		return mCursor;
	}
	private static String getStringFromCursor(String column, Cursor cursor) {
		int columnIndex = cursor.getColumnIndex(column);
		String res = (columnIndex!=-1)? cursor.getString(columnIndex): null;
		return res;
	}
	private static int getIntFromCursor(String column, Cursor cursor) {
		int columnIndex = cursor.getColumnIndex(column);
		int res = (columnIndex!=-1)? cursor.getInt(columnIndex): -1;
		return res;
	}
	private static long getLongFromCursor(String column, Cursor cursor) {
		int columnIndex = cursor.getColumnIndex(column);
		//Log.w("getLongFromCursor--------------- ", "COLUMN["+column+"]::: index:"+columnIndex);
		long res = (columnIndex!=-1)? cursor.getLong(columnIndex): -1;
		return res;
	}
	private static boolean isStringValid(String title) {
		if ((title != null) && (title.length() != 0)) {
			return true;
		}
		return false;
	}
	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}
		@Override
		public void onCreate(SQLiteDatabase db) {
			// TODO Auto-generated method stub
			for(String sql: DATABASE_CREATE) {
				db.execSQL(sql);
			}
		}
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
			+ newVersion + ", which will destroy all old data");
			//db.execSQL("DROP TABLE IF EXISTS notes");
			///onCreate(db);
			resetData(db);
		}
		public void resetData(SQLiteDatabase db) {
			for(String sql: DATABASE_RESET) {
				db.execSQL(sql);
			}
			onCreate(db);
		}
	}
	
	static class HubCache {
		public long hubId = -1;
		public long user_id = -1;
		public String name = null;
		public String zid = null;
		public String behaviour = null;
		public String services = null;
		public long timestamp = 0;
		public HubCache() {}
		public HubCache(long hid, long uid, String label, String id, String behave, String serve, long ts) {
			hubId = hid;
			user_id = uid;
			name = label;
			zid = id;
			behaviour = behave;
			services = serve;
			timestamp = ts;
		}
	}
	
	static class DeviceCache {
		public long deviceId = -1;
		public long hub_id = -1;
		public String name = null;
		public String zid = null;
		public int type = 0;
		public String attributes = null;
		public long timestamp = 0;
		public DeviceCache() {}
		public DeviceCache(long did, long hid, String label, String id, int dtype, String attrs, long ts) {
			deviceId = did;
			hub_id = hid;
			name = label;
			zid = id;
			type = dtype;
			attributes = attrs;
			timestamp = ts;
		}
	}
	
	static class EventCache {
		public long eventId = -1;
		public long hub_id = -1;
		public String message = null;
		public long timestamp = 0;
		public EventCache() {}
		public EventCache(long eid, long hid, String mesg, long ts) { eventId = eid; hub_id = hid; message = mesg; timestamp = ts; }
	}
	
	private static final String TAG = "AlertMeStorage";
	// sorting types
	public static final int SORT_NONE = -1;
	public static final int SORT_ALPHA_ASC = 0;
	public static final int SORT_ALPHA_DES = 1;
	public static final int SORT_INSERT_ASC = 2;
	public static final int SORT_INSERT_DES = 3;
	
	public static final String KEY_ROWID = "_id";
	private static final String DATABASE_NAME = "alertmeandroid";
	private static final int DATABASE_VERSION = 2;

	private static final String tableUser = "user";
	private static final String[] userTableColumns = { KEY_ROWID, "username", "password", "info", "session_id", "timestamp" };
	private static final String userTableCreate = "CREATE TABLE IF NOT EXISTS user (" +
		KEY_ROWID +" integer primary key autoincrement, " +
		"username text not null, " +
		"password text not null, " +
		"info text null, " +
		"session_id text null, " +
		"timestamp long null" +
		");";

	private static final String tableHub = "hub";
	private static final String[] hubTableColumns = { KEY_ROWID, "user_id", "name", "zid", "behaviour", "services", "timestamp" };
	private static final String hubTableCreate = "CREATE TABLE IF NOT EXISTS hub (" +
		KEY_ROWID +" integer primary key autoincrement, " +
		"user_id integer not null, " +
		"name text not null, " +
		"zid text not null, " +
		"behaviour text null, " +
		"services text null, " +
		"timestamp long null" +
		");";

	private static final String tableDevice = "device";
	private static final String[] deviceTableColumns = { KEY_ROWID, "hub_id", "name", "zid", "type", "attributes", "timestamp" };
	private static final String deviceTableCreate = "CREATE TABLE IF NOT EXISTS device (" +
		KEY_ROWID +" integer primary key autoincrement, " +
		"hub_id integer not null, " +
		"name text not null, " +
		"zid text not null, " +
		"type int not null, " +
		"attributes text null, " +
		"timestamp long null" +
		");";
	
	private static final String tableEvent = "event";
	private static final String[] eventTableColumns = { KEY_ROWID, "hub_id", "message", "timestamp" };
	private static final String eventTableCreate = "CREATE TABLE IF NOT EXISTS event (" +
		KEY_ROWID +" integer primary key autoincrement, " +
		"hub_id integer not null, " +
		"message text not null, " +
		"timestamp long not null" +
		");";
	private static final String[] DATABASE_CREATE = { userTableCreate, hubTableCreate, deviceTableCreate, eventTableCreate };
	private static final String[] DATABASE_RESET = { "DROP TABLE IF EXISTS "+ tableUser, "DROP TABLE IF EXISTS "+ tableHub, "DROP TABLE IF EXISTS "+ tableDevice, "DROP TABLE IF EXISTS "+ tableEvent };

	
	/**
	 * SELECT sql FROM 
   (SELECT * FROM sqlite_master UNION ALL
    SELECT * FROM sqlite_temp_master)
WHERE type!='meta'
ORDER BY tbl_name, type DESC, name

	 * 
	 * 
	 *CREATE TABLE device (_id integer primary key autoincrement, hub_id integer not null, name text not null, zid text not null, type int not null, attributes text null, timestamp long null)
CREATE TABLE event (_id integer primary key autoincrement, hub_id integer not null, message text not null, timestamp long not null)
CREATE TABLE hub (_id integer primary key autoincrement, user_id integer not null, name text not null, zid text not null, behaviour text null, services text null, timestamp long null)
CREATE TABLE sqlite_sequence(name,seq)
CREATE TABLE user (_id integer primary key autoincrement, username text not null, password text not null, info text null, session_id text null, timestamp long null)
 	
	 * 
	 * 
	 * 
	 */
	
}
