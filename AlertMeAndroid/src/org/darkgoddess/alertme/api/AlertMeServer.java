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

package org.darkgoddess.alertme.api;

import java.net.URI;
import java.util.ArrayList;

import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import android.util.Log;

/**
 * @author      exacat <android.com@darkgoddess.org>
 * @version     201111.0210
 * @since       1.6
 * 
 * This provides an interface for calls to the AlertMe API, as provided in script form elsewhere.
 * There is no session handling here, you must provide it yourself in a 'session' object that uses this
 * class. All results are in raw String form for external classes to parse themselves; apart from the
 * function {@link #login(String, String)} you must always pass in the appropriate session key
 * 
 * Example usage of this object:
 * 
 *  String username;
 *  String password;
 *  // .. obtain the account details
 *  
 *  AlertMeServer alertme = new AlertMeServer();
 *  String sessionKey = alertme.login(username, password);
 *  String rawUserInfo = alertme.getUserInfo(sessionKey);
 *  // .. parse rawUserInfo ..
 *  // .. do other calls ..
 *  
 *  // Now we're done!
 *  alertme.logout(sessionKey);
 * 
 */
public class AlertMeServer {
	private static final String TAG = "AlertMeServer";
	public static final String LOGIN_TAG = "AlertMe for Android";
	private static final boolean DEBUGOUT = false;
	public static final long SESSION_SPAN = 1000*60*10; // 10 minute sessions
	//public static final String APIURL = "http://10.0.2.2:8888"; // TESTING SERVER
	public static final String APIURL = "https://api.alertme.com/webapi/v2";

	private static URI clientURI = URI.create(APIURL);
	private XMLRPCClient client = null;
	private ArrayList<String> errorLog = new ArrayList<String>();
	
	public AlertMeServer() { client = new XMLRPCClient(clientURI); }

	/**
	 * Perform a login call to the AlertMe API.
	 *
	 * Be warned, that too many requests may stop future requests,
	 * so it is best to create a sort of session
	 *
	 * @param  username The login username; often an email address
	 * @param  password The account password
	 * @return A session key (string) to use for further requests with a 15(?) minute lifespan; null if invalid
	 */
	public String login(final String username, final String password) {
		String res = null;
		boolean hasErrors = false;
		if (DEBUGOUT) Log.w(TAG, "login() CALLED");
		if (client==null) {
			if (DEBUGOUT) Log.w(TAG, "login() END (PREMATURE: failed client null test)");
			return res;
		}
		try {
			Object rawRes = client.call("login", username, password, LOGIN_TAG);
			String resStr = rawRes.toString();
			res = resStr;
			//if (DEBUGOUT) Log.w(TAG, "login() call returned type: " + rawRes.getClass().getName()+" "+resStr);
			if (DEBUGOUT) Log.w(TAG, "XML::LOGIN call returned session ID: " + resStr+" for details ["+username+"]["+password+"]");
		} catch (XMLRPCException e) {
			hasErrors = true;
			errorLog.add(e.toString());
		} catch (Exception ex) {
			hasErrors = true;
			errorLog.add(ex.toString());
		}
		if (hasErrors) {
			if (DEBUGOUT) Log.w(TAG, "login() ENDED (ERRORS present)");
		} else {
			if (DEBUGOUT) Log.w(TAG, "login() ENDED (OK)");
		}
		return res;
	}

	/**
	 * A list of devices in the system, revealing their zigbee IDs (for use in other device calls) and type
	 *
	 * @param  sessionKey The session key string received from {@link #login(String, String)}
	 * @return Raw device data (in CSV format); null if invalid (session key or errors). Rows include device names, (zigbee)ID and type
	 */
	public String getAllDevices(final String sessionKey) {
		String res = null;
		boolean hasErrors = false;
		if (DEBUGOUT) Log.w(TAG, "getAllDevices() CALLED");
		if (client==null) {
			if (DEBUGOUT) Log.w(TAG, "getAllDevices() END (PREMATURE: failed client null test)");
			return res;
		}
		try {
			Object rawRes = client.call("getAllDevices", sessionKey);
			String resStr = rawRes.toString();
			res = resStr;
			//if (DEBUGOUT) Log.w(TAG, "getAllDevices() call returned type: " + rawRes.getClass().getName()+" "+resStr);
		} catch (XMLRPCException e) {
			errorLog.add(e.toString());
		} catch (Exception ex) {
			errorLog.add(ex.toString());
		}
		if (hasErrors) {
			if (DEBUGOUT) Log.w(TAG, "getAllDevices() ENDED (ERRORS present)");
		} else {
			if (DEBUGOUT) Log.w(TAG, "getAllDevices() ENDED (OK)");
		}
		return res;
	}

	/**
	 * Retrieve the raw string of all the devices for a particular account.
	 *
	 * @param  sessionKey The session key string received from {@link #login(String, String)}
	 * @return Result if command was registered and valid (string "ok"? or "notok"?); null if invalid (session key or errors).
	 */
	public String logout(final String sessionKey) {
		String res = null;
		boolean hasErrors = false;
		if (DEBUGOUT) Log.w(TAG, "logout() CALLED");
		if (client==null) {
			if (DEBUGOUT) Log.w(TAG, "logout() END (PREMATURE: failed client null test)");
			return res;
		}
		try {
			Object rawRes = client.call("logout", sessionKey);
			String resStr = rawRes.toString();
			res = resStr;
			//if (DEBUGOUT) Log.w(TAG, "getAllDevices() call returned type: " + rawRes.getClass().getName()+" "+resStr);
		} catch (XMLRPCException e) {
			errorLog.add(e.toString());
		} catch (Exception ex) {
			errorLog.add(ex.toString());
		}
		if (hasErrors) {
			if (DEBUGOUT) Log.w(TAG, "logout() ENDED (ERRORS present)");
		} else {
			if (DEBUGOUT) Log.w(TAG, "logout() ENDED (OK)");
		}
		return res;
	}

	/**
	 * Retrieve the raw string of all the hubs for a particular account.
	 *
	 * Although an account normally has one hub, the API is prepared to return a list of hubs.
	 * Correct data is in the form Hub1Name|Hub1ID,...,HubnName|HubNID
	 *
	 * @param  sessionKey The session key string received from {@link #login(String, String)}
	 * @return Raw hub data (in CSV format); null if invalid (session key or errors). Rows include hub names and (zigbee)ID
	 */
	public String getAllHubs(final String sessionKey) {
		String res = null;
		boolean hasErrors = false;
		if (DEBUGOUT) Log.w(TAG, "getAllHubs() CALLED");
		if (client==null) {
			if (DEBUGOUT) Log.w(TAG, "getAllHubs() END (PREMATURE: failed client null test)");
			return res;
		}
		try {
			Object rawRes = client.call("getAllHubs", sessionKey);
			String resStr = rawRes.toString();
			res = resStr;
			//if (DEBUGOUT) Log.w(TAG, "getAllDevices() call returned type: " + rawRes.getClass().getName()+" "+resStr);
		} catch (XMLRPCException e) {
			errorLog.add(e.toString());
		} catch (Exception ex) {
			errorLog.add(ex.toString());
		}
		if (hasErrors) {
			if (DEBUGOUT) Log.w(TAG, "getAllHubs() ENDED (ERRORS present)");
		} else {
			if (DEBUGOUT) Log.w(TAG, "getAllHubs() ENDED (OK)");
		}
		return res;
	}


	/**
	 * Retrieve the raw string of a home systems mode (armed/away, disarmed/home, night-armed/night)
	 *
	 * @param  sessionKey The session key string received from {@link #login(String, String)}
	 * @return String with value [ Away | Home | Night ] {@link #getAllBehaviours(String)} for accurate list
	 */
	public String getBehaviour(final String sessionKey) {
		String res = null;
		boolean hasErrors = false;
		if (DEBUGOUT) Log.w(TAG, "getBehaviour() CALLED");
		if (client==null) {
			if (DEBUGOUT) Log.w(TAG, "getBehaviour() END (PREMATURE: failed client null test)");
			return res;
		}
		try {
			Object rawRes = client.call("getBehaviour", sessionKey);
			String resStr = rawRes.toString();
			res = resStr;
			//if (DEBUGOUT) Log.w(TAG, "getBehaviour() call returned type: " + rawRes.getClass().getName()+" "+resStr);
		} catch (XMLRPCException e) {
			errorLog.add(e.toString());
		} catch (Exception ex) {
			errorLog.add(ex.toString());
		}
		if (hasErrors) {
			if (DEBUGOUT) Log.w(TAG, "getBehaviour() ENDED (ERRORS present)");
		} else {
			if (DEBUGOUT) Log.w(TAG, "getBehaviour() ENDED (OK)");
		}
		return res;
	}

	/**
	 * Retrieve the raw string of all device attributes, or a particular attribute
	 *
	 * Correct data is in the form Attribute1|Val1,Attribute|Val2,...,AttributeN|ValN if no attribute is specified,
	 *   otherwise the plain value is given
	 *
	 * @param  sessionKey The session key string received from {@link #login(String, String)}
	 * @param  deviceId   The ID string received from {@link #getAllDevices(String)}
	 * @param  attribute  A device attribute. Can be null or zero-sized strings to get all the attributes
	 * @return Raw attribute data (in CSV format) if attribute is not null or zero-sized string otherwise single string value; null if invalid (session key or errors).
	 */
	public String getDeviceChannelValue(final String sessionKey, final String deviceId, final String attribute) {
		String res = null;
		boolean hasErrors = false;
		if (DEBUGOUT) Log.w(TAG, "getDeviceChannelValue(attribute:"+attribute+") CALLED");
		if (client==null) {
			if (DEBUGOUT) Log.w(TAG, "getDeviceChannelValue() END (PREMATURE: failed client null test)");
			return res;
		}
		try {
			Object rawRes = null;
			if (attribute!=null && attribute.length()!=0) {
				rawRes = client.call("getDeviceChannelValue", sessionKey, deviceId, attribute);				
			} else {
				rawRes = client.call("getDeviceChannelValue", sessionKey, deviceId);				
			}
			String resStr = rawRes.toString();
			res = resStr;
			//if (DEBUGOUT) Log.w(TAG, "getDeviceChannelValue() call returned type: " + rawRes.getClass().getName()+" "+resStr);
		} catch (XMLRPCException e) {
			errorLog.add(e.toString());
		} catch (Exception ex) {
			errorLog.add(ex.toString());
		}
		if (hasErrors) {
			if (DEBUGOUT) Log.w(TAG, "getDeviceChannelValue(:"+attribute+") ENDED (ERRORS present)");
		} else {
			if (DEBUGOUT) Log.w(TAG, "getDeviceChannelValue(:"+attribute+") ENDED (OK)");
		}
		return res;
	}

	/**
	 * Retrieve the raw string value of all device attributes. Uses {@link #getDeviceChannelValue(String, String, String)}
	 *
	 * @param  sessionKey The session key string received from {@link #login(String, String)}
	 * @param  deviceId   The ID string received from {@link #getAllDevices(String)}
	 * @return Raw attribute data; null if invalid (session key or errors).
	 */
	public String getDeviceChannelValue(final String sessionKey, final String deviceId) {
		return getDeviceChannelValue(sessionKey, deviceId, null);
	}
	/**
	 * Retrieves the battery level of a device; a wrapper of {@link #getDeviceChannelValue(String, String, String)} in making this task more intuitive
	 *
	 * @param  sessionKey The session key string received from {@link #login(String, String)}
	 * @param  deviceId   The ID string received from {@link #getAllDevices(String)}
	 * @return Raw attribute data (decimal string); null if invalid (session key or errors).
	 */
	public String getBatteryLevel(final String sessionKey, final String deviceId) {
		return getDeviceChannelValue(sessionKey, deviceId, "BatteryLevel");
	}
	/**
	 * See if a device is registered with the hub(s); a wrapper of {@link #getDeviceChannelValue(String, String, String)} in making this task more intuitive
	 *
	 * @param  sessionKey The session key string received from {@link #login(String, String)}
	 * @param  deviceId   The ID string received from {@link #getAllDevices(String)}
	 * @return Raw attribute data (string "True" or "False"); null if invalid (session key or errors).
	 */
	public String getPresence(final String sessionKey, final String deviceId) {
		return getDeviceChannelValue(sessionKey, deviceId, "Presence");		
	}
	/**
	 * Retrieves the temperature detected by a device; a wrapper of {@link #getDeviceChannelValue(String, String, String)} in making this task more intuitive
	 *
	 * Note that the values are quite larger if the device is a smartplug/power controller that is in use
	 *
	 * @param  sessionKey The session key string received from {@link #login(String, String)}
	 * @param  deviceId   The ID string received from {@link #getAllDevices(String)}
	 * @return Raw attribute data (decimal string); null if invalid (session key or errors).
	 */
	public String getTemperature(final String sessionKey, final String deviceId) {
		return getDeviceChannelValue(sessionKey, deviceId, "Temperature");		
	}
	/**
	 * Sees whether or not the smartplug/power controller device is plugged in; a wrapper of {@link #getDeviceChannelValue(String, String, String)} in making this task more intuitive
	 *
	 * @param  sessionKey The session key string received from {@link #login(String, String)}
	 * @param  deviceId   The ID string received from {@link #getAllHubs(String)}; should refer to a smartplug/power controller 
	 * @return Raw attribute data (string "True" or "False"); null if invalid (session key or errors).
	 */
	public String getRelayState(final String sessionKey, final String deviceId) {
		return getDeviceChannelValue(sessionKey, deviceId, "RelayState");		
	}


	/**
	 * Retrieve a list of events for a particular account, starting from the current time to the past for limit entries. A wrapper for {@link #getEventLog(String, String, int, String, String)}
	 *
	 * Due to the request limits, it is best to not retrieve more than 50 items at a time.
	 *
	 * @param  sessionKey The session key string received from {@link #login(String, String)}
	 * @param  limit      The number of events to retrieve
	 * @return Raw event data (in CSV format); null if invalid (session key or errors). Rows should at least start with an epoch timestamp and end with a message
	 */
	public String getEventLog(final String sessionKey, final int limit) {
		return getEventLog(sessionKey, "null", limit, "null", "null", false);
	}

	/**
	 * Retrieve a list of events for a particular account. A wrapper for {@link #getEventLog(String, String, int, String, String, boolean)}
	 * 
	 * See {@link #getEventLog(String, String, int, String, String, boolean)} for details
	 *
	 * @param  sessionKey The session key string received from {@link #login(String, String)}
	 * @param  service    The service state being inquired. See {@link #getAllServices(String)} for possible values. Can be set to string "null" if not used
	 * @param  limit      The number of events to retrieve
	 * @param  start      The starting date of events to return, in epoch time (seconds). Can be set to string "null" if not used
	 * @param  end        The end date of events to return, in epoch time (seconds). Can be set to string "null" if not used
	 * @param  localiseTimes  If the "true" then show the times as localized rather than epochs
	 * @return Raw event data (in CSV format); null if invalid (session key or errors). Rows should at least start with an epoch timestamp and end with a message
	 */
	public String getEventLog(final String sessionKey, final String service, final int limit, final String start, final String end) {
		return getEventLog(sessionKey, service, limit, start, end, false);
	}
	
	/**
	 * Retrieve a list of events for a particular account.
	 *
	 * Returning yet another comma-delimited list of events in two formats (two types of events):
	 *   - Generic event: epochTimestamp||Message  (eg: when the hub is set to different modes) 
	 *   - Device event: epochTimestamp|zigbeeLabel|deviceID|deviceTypeLabel|'AM'deviceType|Message  (eg: when a keyfob is out of range)
	 *   (Note that device events are best described as: time|ATTRIBUTES|message
	 *
	 * @param  sessionKey The session key string received from {@link #login(String, String)}
	 * @param  service    The service state being inquired. See {@link #getAllServices(String)} for possible values. Can be set to string "null" if not used
	 * @param  limit      The number of events to retrieve. If negative, set no limit
	 * @param  start      The starting date of events to return, in epoch time (seconds). Can be set to string "null" if not used
	 * @param  end        The end date of events to return, in epoch time (seconds). Can be set to string "null" if not used
	 * @param  localiseTimes  If the "true" then show the times as localized rather than epochs
	 * @return Raw event data (in CSV format); null if invalid (session key or errors). Rows should at least start with an epoch timestamp and end with a message
	 */
	public String getEventLog(final String sessionKey, final String service, final int limit, final String start, final String end, final boolean localizeTimes) {
		String res = null;
		boolean hasErrors = false;
		String localizeIn = (localizeTimes)? "true": "false";
		if (DEBUGOUT) Log.w(TAG, "getEventLog() CALLED");
		if (client==null) {
			if (DEBUGOUT) Log.w(TAG, "getEventLog() END (PREMATURE: failed client null test)");
			return res;
		}
		try {
			Object rawRes;
			if (limit<0) {
				rawRes = client.call("getEventLog", sessionKey, service, "null", start, end, localizeIn);
			} else {
				rawRes = client.call("getEventLog", sessionKey, service, limit, start, end, localizeIn);				
			}
			String resStr = rawRes.toString();
			res = resStr;
			//if (DEBUGOUT) Log.w(TAG, "getEventLog() call returned type: " + rawRes.getClass().getName()+" "+resStr);
		} catch (XMLRPCException e) {
			errorLog.add(e.toString());
		} catch (Exception ex) {
			errorLog.add(ex.toString());
		}
		if (hasErrors) {
			if (DEBUGOUT) Log.w(TAG, "getEventLog() ENDED (ERRORS present)");
		} else {
			if (DEBUGOUT) Log.w(TAG, "getEventLog() ENDED (OK)");
		}
		return res;
	}

	/**
	 * Sends out a command to the accounts AlertMe system. Returns (normally) a string indicating success ("ok") or otherwise failure ("notok"??)
	 *
	 * @param  sessionKey     The session key string received from {@link #login(String, String)}
	 * @param  deviceLabel    The command to issue (eg: "IntruderAlarm", "Energy")
	 * @param  deviceCommand  The mode the command is to be set to (eg: "arm"/"disarm"/"nightArm", "on"/"off")
	 * @param  deviceId       The ID string received from {@link #getAllDevices(String)} if required (eg: should refer to a smartplug/power controller if doing "Energy")
	 * @return Result if command was registered and valid (string "ok" or "notok"?); null if invalid (session key or errors).
	 */
	public String sendCommand(final String sessionKey, final String deviceLabel, final String deviceCommand, final String deviceId) {
		String res = null;
		boolean hasErrors = false;
		if (DEBUGOUT) Log.w(TAG, "sendCommand() CALLED");
		if (client==null) {
			if (DEBUGOUT) Log.w(TAG, "sendCommand() END (PREMATURE: failed client null test)");
			return res;
		}
		try {
			Object rawRes;
			if (deviceId!=null && deviceId.length()!=0) {
				rawRes = client.call("sendCommand", sessionKey, deviceLabel, deviceCommand, deviceId);
			} else {
				rawRes = client.call("sendCommand", sessionKey, deviceLabel, deviceCommand);
			}
			String resStr = rawRes.toString();
			res = resStr;
			//if (DEBUGOUT) Log.w(TAG, "sendCommand() call returned type: " + rawRes.getClass().getName()+" "+resStr);
		} catch (XMLRPCException e) {
			errorLog.add(e.toString());
		} catch (Exception ex) {
			errorLog.add(ex.toString());
		}
		if (hasErrors) {
			if (DEBUGOUT) Log.w(TAG, "sendCommand() ENDED (ERRORS present)");
		} else {
			if (DEBUGOUT) Log.w(TAG, "sendCommand() ENDED (OK)");
		}
		return res;
	}

	/**
	 * Requests the system to go into a particular behaviour mode {@link #getBehaviour(String)}; a wrapper for {@link #sendCommand(String, String, String, String)}
	 *
	 * @param  sessionKey  The session key string received from {@link #login(String, String)}
	 * @param  mode        The string [ arm | disarm | nightArm ]
	 * @return Result if command was registered and valid (string "ok" or "notok"?); null if invalid (session key or errors).
	 */
	public String setBehaviourMode(final String sessionKey, final String mode) {
		return sendCommand(sessionKey, "IntruderAlarm", mode, null);
	}
	/**
	 * Requests a smartplug/power controller to switch on or off. See {@link #getRelayState(String, String)} for the current state. A wrapper for {@link #sendCommand(String, String, String, String)}
	 *
	 * @param  sessionKey The session key string received from {@link #login(String, String)}
	 * @param  mode       The string [ on | off ]
	 * @param  deviceId   The ID string received from {@link #getAllDevices(String)}; should refer to a smartplug/power controller 
	 * @return Result if command was registered and valid (string "ok" or "notok"?); null if invalid (session key or errors).
	 */
	public String setRelayState(final String sessionKey, final String mode, final String deviceId) {
		return sendCommand(sessionKey, "Energy", mode, deviceId);
	}

	/**
	 * Retrieve the raw string of the state of the system for a particular account.
	 *
	 * A correct data row is in the format attribute1|value1,...,attributeN|valueN. Known attributes are: "IsAvailable" ["yes"|"no"] and "IsUpgrading" ["yes"|"no"?]
	 *
	 * @param  sessionKey The session key string received from {@link #login(String, String)}
	 * @return Raw device data (in CSV format); null if invalid (session key or errors). Rows include device names, (zigbee)ID and type
	 */
	public String getHubStatus(final String sessionKey) {
		String res = null;
		boolean hasErrors = false;
		if (DEBUGOUT) Log.w(TAG, "getHubStatus() CALLED");
		if (client==null) {
			if (DEBUGOUT) Log.w(TAG, "getHubStatus() END (PREMATURE: failed client null test)");
			return res;
		}
		try {
			Object rawRes = client.call("getHubStatus", sessionKey);
			String resStr = rawRes.toString();
			res = resStr;
			//if (DEBUGOUT) Log.w(TAG, "getHubStatus() call returned type: " + rawRes.getClass().getName()+" "+resStr);
		} catch (XMLRPCException e) {
			errorLog.add(e.toString());
		} catch (Exception ex) {
			errorLog.add(ex.toString());
		}
		if (hasErrors) {
			if (DEBUGOUT) Log.w(TAG, "getHubStatus() ENDED (ERRORS present)");
		} else {
			if (DEBUGOUT) Log.w(TAG, "getHubStatus() ENDED (OK)");
		}
		return res;
	}

	/**
	 * Retrieve the user information for the current account.
	 *
	 * Correct data row is in the format attribute1|value1,...,attributeN|valueN.
	 *    A typical response is:  firstname|FIRSTNAME,lastname|SURNAME,username|useraccountemail@example.com
	 *
	 * @param  sessionKey The session key string received from {@link #login(String, String)}
	 * @return Raw device data (in CSV format); null if invalid (session key or errors).
	 */
	public String getUserInfo(final String sessionKey) {
		String res = null;
		boolean hasErrors = false;
		if (DEBUGOUT) Log.w(TAG, "getUserInfo() CALLED");
		if (client==null) {
			if (DEBUGOUT) Log.w(TAG, "getUserInfo() END (PREMATURE: failed client null test)");
			return res;
		}
		try {
			Object rawRes = client.call("getUserInfo", sessionKey);
			String resStr = rawRes.toString();
			res = resStr;
			//if (DEBUGOUT) Log.w(TAG, "getUserInfo() call returned type: " + rawRes.getClass().getName()+" "+resStr);
		} catch (XMLRPCException e) {
			errorLog.add(e.toString());
		} catch (Exception ex) {
			errorLog.add(ex.toString());
		}
		if (hasErrors) {
			if (DEBUGOUT) Log.w(TAG, "getUserInfo() ENDED (ERRORS present)");
		} else {
			if (DEBUGOUT) Log.w(TAG, "getUserInfo() ENDED (OK)");
		}
		return res;
	}

	/**
	 * Get the current state of a service, if applicable
	 *
	 * The value returned appears in the corresponding call {@link #getAllServiceStates(String, String)}
	 *
	 * @param  sessionKey The session key string received from {@link #login(String, String)}
	 * @param  service    The service state being inquired. See {@link #getAllServices(String)} for possible values
	 * @return A state as a string, see: {@link #getAllServiceStates(String, String)} for possible states; null if invalid (session key or errors).
	 */
	public String getCurrentServiceState(final String sessionKey, final String service) {
		String res = null;
		boolean hasErrors = false;
		if (DEBUGOUT) Log.w(TAG, "getCurrentServiceState() CALLED");
		if (client==null) {
			if (DEBUGOUT) Log.w(TAG, "getCurrentServiceState() END (PREMATURE: failed client null test)");
			return res;
		}
		try {
			String serviceCalled = (service!=null&&service.length()!=0)? service: "null";
			Object rawRes = client.call("getCurrentServiceState", sessionKey, serviceCalled);
			String resStr = rawRes.toString();
			res = resStr;
			//if (DEBUGOUT) Log.w(TAG, "getCurrentServiceState() call returned type: " + rawRes.getClass().getName()+" "+resStr);
		} catch (XMLRPCException e) {
			errorLog.add(e.toString());
		} catch (Exception ex) {
			errorLog.add(ex.toString());
		}
		if (hasErrors) {
			if (DEBUGOUT) Log.w(TAG, "getCurrentServiceState() ENDED (ERRORS present)");
		} else {
			if (DEBUGOUT) Log.w(TAG, "getCurrentServiceState() ENDED (OK)");
		}
		return res;
	}

	/**
	 * Get a comma-delimited list of values of states for a particular service
	 *
	 * Currently known services at the time of writing are:
	 *   - IntruderAlarm:  disarmed,queryError,error,armGrace,armed,alarmGrace,alarmed,serverAlarmCleared,alarmWarning
	 *   - EmergencyAlarm: passive,alarmConfirm,alarmGrace,alarmed,serverAlarmCleared,alarmWarning
	 *   - Doorbell:       idle,ringing
	 *
	 * @param  sessionKey The session key string received from {@link #login(String, String)}
	 * @param  service    The service state being inquired. See {@link #getAllServices(String)} for possible values
	 * @return A raw string of states, comma-separated - blank string if no states available; null if invalid (session key or errors).
	 */
	public String getAllServiceStates(final String sessionKey, final String service) {
		String res = null;
		boolean hasErrors = false;
		if (DEBUGOUT) Log.w(TAG, "getAllServiceStates() CALLED");
		if (client==null) {
			if (DEBUGOUT) Log.w(TAG, "getAllServiceStates() END (PREMATURE: failed client null test)");
			return res;
		}
		try {
			String serviceCalled = (service!=null&&service.length()!=0)? service: "null";
			Object rawRes = client.call("getAllServiceStates", sessionKey, serviceCalled);
			String resStr = rawRes.toString();
			res = resStr;
			//if (DEBUGOUT) Log.w(TAG, "getAllServiceStates() call returned type: " + rawRes.getClass().getName()+" "+resStr);
		} catch (XMLRPCException e) {
			errorLog.add(e.toString());
		} catch (Exception ex) {
			errorLog.add(ex.toString());
		}
		if (hasErrors) {
			if (DEBUGOUT) Log.w(TAG, "getAllServiceStates() ENDED (ERRORS present)");
		} else {
			if (DEBUGOUT) Log.w(TAG, "getAllServiceStates() ENDED (OK)");
		}
		return res;
	}

	/**
	 * Get individual device details. Normally just a single attribute: "Software Version"
	 *
	 * Correct data rows are in the format attribute1|value1,...,attributeN|valueN. So far, only one attribute found (eg: Software Version|2.0r12)
	 *
	 * @param  sessionKey The session key string received from {@link #login(String, String)}
	 * @param  deviceId   The ID string received from {@link #getAllDevices(String)}
	 * @return Raw attribute data (in CSV format); null if invalid (session key or errors).
	 */
	public String getDeviceDetails(final String sessionKey, final String deviceId) {
		String res = null;
		boolean hasErrors = false;
		if (DEBUGOUT) Log.w(TAG, "getDeviceDetails() CALLED");
		if (client==null) {
			if (DEBUGOUT) Log.w(TAG, "getDeviceDetails() END (PREMATURE: failed client null test)");
			return res;
		}
		try {
			Object rawRes = client.call("getDeviceDetails", sessionKey, deviceId);
			String resStr = rawRes.toString();
			res = resStr;
			//if (DEBUGOUT) Log.w(TAG, "getDeviceDetails() call returned type: " + rawRes.getClass().getName()+" "+resStr);
		} catch (XMLRPCException e) {
			errorLog.add(e.toString());
		} catch (Exception ex) {
			errorLog.add(ex.toString());
		}
		if (hasErrors) {
			if (DEBUGOUT) Log.w(TAG, "getDeviceDetails() ENDED (ERRORS present)");
		} else {
			if (DEBUGOUT) Log.w(TAG, "getDeviceDetails() ENDED (OK)");
		}
		return res;
	}


	/**
	 * A list of available behaviours for the system.
	 *
	 * So far these are modes armed/away, disarmed/home, night-armed/night. The exact values are "Home", "Away", "Night"
	 *
	 * @param  sessionKey The session key string received from {@link #login(String, String)}
	 * @return A raw string of states, comma-separated 
	 */
	public String getAllBehaviours(final String sessionKey) {
		String res = null;
		boolean hasErrors = false;
		if (DEBUGOUT) Log.w(TAG, "getAllBehaviours() CALLED");
		if (client==null) {
			if (DEBUGOUT) Log.w(TAG, "getAllBehaviours() END (PREMATURE: failed client null test)");
			return res;
		}
		try {
			Object rawRes = client.call("getAllBehaviours", sessionKey);
			String resStr = rawRes.toString();
			res = resStr;
			//if (DEBUGOUT) Log.w(TAG, "getAllBehaviours() call returned type: " + rawRes.getClass().getName()+" "+resStr);
		} catch (XMLRPCException e) {
			errorLog.add(e.toString());
		} catch (Exception ex) {
			errorLog.add(ex.toString());
		}
		if (hasErrors) {
			if (DEBUGOUT) Log.w(TAG, "getAllBehaviours() ENDED (ERRORS present)");
		} else {
			if (DEBUGOUT) Log.w(TAG, "getAllBehaviours() ENDED (OK)");
		}
		return res;
	}

	/**
	 * A list of available services. Examples are "IntruderAlarm", "EmergencyAlarm", "Doorbell", "Presence"
	 *
	 * @param  sessionKey The session key string received from {@link #login(String, String)}
	 * @return A raw string of states, comma-separated
	 */
	public String getAllServices(final String sessionKey) {
		String res = null;
		boolean hasErrors = false;
		if (DEBUGOUT) Log.w(TAG, "getAllServices() CALLED");
		if (client==null) {
			if (DEBUGOUT) Log.w(TAG, "getAllServices() END (PREMATURE: failed client null test)");
			return res;
		}
		try {
			Object rawRes = client.call("getAllServices", sessionKey);
			String resStr = rawRes.toString();
			res = resStr;
			//if (DEBUGOUT) Log.w(TAG, "getAllBehaviours() call returned type: " + rawRes.getClass().getName()+" "+resStr);
		} catch (XMLRPCException e) {
			errorLog.add(e.toString());
		} catch (Exception ex) {
			errorLog.add(ex.toString());
		}
		if (hasErrors) {
			if (DEBUGOUT) Log.w(TAG, "getAllServices() ENDED (ERRORS present)");
		} else {
			if (DEBUGOUT) Log.w(TAG, "getAllServices() ENDED (OK)");
		}
		return res;
	}

	/**
	 * Set the session to look at the specified hub to view details on. This is not necessary if you have only one hub.
	 *
	 * I don't know for certain as I cannot test on an account that has multiple hubs, but this is what I assume.
	 *   FIXME: Test this to verify!
	 *
	 * @param  sessionKey    The session key string received from {@link #login(String, String)}
	 * @param  hubId         The ID string received from {@link #getAllHubs(String)}
	 * @return Result if command was registered and valid (string "ok" or "notok"?); null if invalid (session key or errors).
	 */
	public String setHub(final String sessionKey, final String hubId) {
		String res = null;
		boolean hasErrors = false;
		if (DEBUGOUT) Log.w(TAG, "setHub() CALLED");
		if (client==null) {
			if (DEBUGOUT) Log.w(TAG, "setHub() END (PREMATURE: failed client null test)");
			return res;
		}
		try {
			Object rawRes = client.call("setHub", sessionKey, hubId);
			String resStr = rawRes.toString();
			res = resStr;
			//if (DEBUGOUT) Log.w(TAG, "setHub() call returned type: " + rawRes.getClass().getName()+" "+resStr);
		} catch (XMLRPCException e) {
			errorLog.add(e.toString());
		} catch (Exception ex) {
			errorLog.add(ex.toString());
		}
		if (hasErrors) {
			if (DEBUGOUT) Log.w(TAG, "setHub() ENDED (ERRORS present)");
		} else {
			if (DEBUGOUT) Log.w(TAG, "setHub() ENDED (OK)");
		}
		return res;
	}	

	/**
	 * A like getAllBehaviours, this provides a list of available attributes for devices in the system.
	 *
	 * Returns a comma-separated list of all the devices and their current attribute values in the form:
	 *    DEVICENAME|DEVICEZID|DEVICETYPE;attribute1|att1Value;attribute2|att2Value;...;attributeN|attNValue|100
	 * The DEVICEZID and DEVICETYPE correspond to the results as found in {@link #getAllDevices(String)}
	 * The attribute results are the same as calling {@link #getDeviceChannelValue(String, String)} except the delimiter is ";"
	 *
	 * @param  sessionKey The session key string received from {@link #login(String, String)}
	 * @return A raw string of all the devices for the system with their attributes (device channels) AND current values, comma-separated 
	 */
	public String getAllDeviceChannelValues(final String sessionKey) {
		String res = null;
		boolean hasErrors = false;
		if (DEBUGOUT) Log.w(TAG, "getAllDeviceChannelValues() CALLED");
		if (client==null) {
			if (DEBUGOUT) Log.w(TAG, "getAllDeviceChannelValues() END (PREMATURE: failed client null test)");
			return res;
		}
		try {
			Object rawRes = client.call("getAllDeviceChannelValues", sessionKey);
			String resStr = rawRes.toString();
			res = resStr;
			//if (DEBUGOUT) Log.w(TAG, "getAllDeviceChannelValues() call returned type: " + rawRes.getClass().getName()+" "+resStr);
		} catch (XMLRPCException e) {
			errorLog.add(e.toString());
		} catch (Exception ex) {
			errorLog.add(ex.toString());
		}
		if (hasErrors) {
			if (DEBUGOUT) Log.w(TAG, "getAllDeviceChannelValues() ENDED (ERRORS present)");
		} else {
			if (DEBUGOUT) Log.w(TAG, "getAllDeviceChannelValues() ENDED (OK)");
		}
		return res;	
	}

	/**
	 * A like getAllDeviceChannelValues, this provides a list of available attributes for a particular device in the system.
	 *
	 * Normally the list will be: Presence,Tamper,Upgrade,BatteryLevel,LQI,Temperature
	 * 
	 * Exceptions are:
	 * Smart Plug/Meter Reader: Presence,Upgrade,LQI,Temperature,Power,Energy
	 * Lamp: Presence,Upgrade,LQI
	 * Keyfob: Presence,Upgrade,BatteryLevel,LQI
	 *
	 * @param  sessionKey The session key string received from {@link #login(String, String)}
	 * @param  deviceId   The ID string received from {@link #getAllDevices(String)} 
	 * @return A raw string of attributes (device channels), comma-separated 
	 */
	public String getAllDeviceChannels(final String sessionKey, final String deviceId) {
		String res = null;
		boolean hasErrors = false;
		if (DEBUGOUT) Log.w(TAG, "getAllDeviceChannels() CALLED");
		if (client==null) {
			if (DEBUGOUT) Log.w(TAG, "getAllDeviceChannels() END (PREMATURE: failed client null test)");
			return res;
		}
		try {
			Object rawRes = client.call("getAllDeviceChannels", sessionKey, deviceId);
			String resStr = rawRes.toString();
			res = resStr;
			//if (DEBUGOUT) Log.w(TAG, "getAllDeviceChannels() call returned type: " + rawRes.getClass().getName()+" "+resStr);
		} catch (XMLRPCException e) {
			errorLog.add(e.toString());
		} catch (Exception ex) {
			errorLog.add(ex.toString());
		}
		if (hasErrors) {
			if (DEBUGOUT) Log.w(TAG, "getAllDeviceChannels() ENDED (ERRORS present)");
		} else {
			if (DEBUGOUT) Log.w(TAG, "getAllDeviceChannels() ENDED (OK)");
		}
		return res;	
	}

	/**
	 * Retrieve a list of events for a particular device attribute.
	 *
	 * WARNING: NOT ACCURATELY DOCUMENTED!!!
	 *    Ordering of inputs not verified beyond the 3rd argument [attribute]
	 *    If raw call 4th input is "null" (or receives 7 arguments), getting "no_data"; however if not it returns a fixed maximum
	 *      number of results (224 entries)
	 *
	 * @param  sessionKey The session key string received from {@link #login(String, String)}
	 * @param  deviceId   The ID string received from {@link #getAllDevices(String)} 
	 * @param  attribute  A device attribute as seen from {@link #getAllDeviceChannels(String, String)}
	 * @param  limit      The number of events to retrieve
	 * @param  start      The starting date of events to return, in epoch time (seconds). Can be set to string "null" if not used
	 * @param  end        The end date of events to return, in epoch time (seconds). Can be set to string "null" if not used
	 * @return Raw event data (in CSV format); null if invalid (session key or errors). Rows should at least start with an epoch timestamp and end with a message
	 */
	public String getDeviceChannelLog(final String sessionKey, final String deviceId, final String attribute, final int limit, final String start, final String end) {
		String res = null;
		boolean hasErrors = false;
		if (DEBUGOUT) Log.w(TAG, "getDeviceChannelLog() CALLED");
		if (client==null) {
			if (DEBUGOUT) Log.w(TAG, "getDeviceChannelLog() END (PREMATURE: failed client null test)");
			return res;
		}
		try {
			Object rawRes = client.call("getDeviceChannelLog", sessionKey, deviceId, attribute, limit, start, end); // these arguments are correct from the documentation
			String resStr = rawRes.toString();
			res = resStr;
			//if (DEBUGOUT) Log.w(TAG, "getEventLog() call returned type: " + rawRes.getClass().getName()+" "+resStr);
		} catch (XMLRPCException e) {
			errorLog.add(e.toString());
		} catch (Exception ex) {
			errorLog.add(ex.toString());
		}
		if (hasErrors) {
			if (DEBUGOUT) Log.w(TAG, "getDeviceChannelLog() ENDED (ERRORS present)");
		} else {
			if (DEBUGOUT) Log.w(TAG, "getDeviceChannelLog() ENDED (OK)");
		}
		return res;

	}
	/**
	 * Retrieve a list of events for a particular device attribute. A wrapper for {@link #getDeviceChannelLog(String, String, String, int, String, String)}
	 *
	 * Due to the request limits, it is best to not retrieve more than 50 items at a time.
	 *
	 * @param  sessionKey The session key string received from {@link #login(String, String)}
	 * @param  limit      The number of events to retrieve
	 * @return Raw event data (in CSV format); null if invalid (session key or errors). Rows should at least start with an epoch timestamp and end with a message
	 */
	public String getDeviceChannelLog(final String sessionKey, final String deviceId, final String attribute, final int limit) {
		return getDeviceChannelLog(sessionKey, deviceId, attribute, limit, "null", "null");
	}

	
	
	/**
	 * Static exception states
	 *
	 */	
	public static class Exceptions {
		public static int EXCEPTIONPROTOCOLERROR                     = 0;
		public static int EXCEPTIONNOSESSION                         = 1;
		public static int EXCEPTIONINTERNALERROR                     = 2;
		public static int EXCEPTIONINVALIDARGUMENTS                  = 3;
		public static int EXCEPTIONSERVICENOTAVAILABLEFORLOGINSTATUS = 4;
		public static int EXCEPTIONNOHUB                             = 5;
		public static int EXCEPTIONINVALIDHUBID                      = 6;
		public static int EXCEPTIONHUBNOTCONTACTABLE                 = 7;
		public static int EXCEPTIONUNKNOWNDEVICE                     = 8;
		public static int EXCEPTIONNEEDSHUBUPGRADE                   = 9;
		public static int EXCEPTIONDEVICENOTPRESENT                  = 10;
		public static int EXCEPTIONINVALIDMETHOD                     = 11;
		public static int EXCEPTIONINVALIDUSERDETAILS                = 12;
		public static int EXCEPTIONINVALIDUSERIDENTIFIER             = 13;
		public static int EXCEPTIONPRIVILEGED                        = 14;
		public static int EXCEPTIONACCOUNTLOCKED                     = 15;
		public static int EXCEPTIONUNKNOWNERROR                      = 16;
		public static int EXCEPTIONEXISTS                            = 17;

		public static String[] messages = new String[] {
			"The API server could not be contacted correctly",
			"A valid session must be established with login prior to making this call",
			"An internal error occured on the API server",
			"Invalid arguments were provided to a call",
			"Service cannot be used without a sucessfull login first",
			"The logged in user doesn't have a hub installed",
			"The hub ID specified is not owned by this user",
			"The hub is not currently attached to the AlertMe system",
			"The device ID provided is not known to this hub",
			"The hub requires upgrading if you wish to use this functionality",
			"The device specified is not currently attached to the hub",
			"The method specified does not exist",
			"The login details provided are incorrect",
			"The user specified does not exist",
			"This function may only be called by privilidged systems",
			"This account has been locked due to too many failed login attempts",
			"An unknown error has occured",
			"The specified item already exists"
		};
		
	}
}