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

package org.darkgoddess.alertdroid.api.utils;

import java.util.ArrayList;
import java.util.HashMap;



public class APIUtilities {
	public static final int COMMAND_INVALID = -1;
	public static final int COMMAND_OK = 1;
	public static final int COMMAND_NOTOK = 0;
	
	public static String getBehaviour(final String rawString) {
		return (isStringNonEmpty(rawString))? rawString.trim(): "Unavailable";
	}

	public static HashMap<String, String> getDeviceChannelValues(final String rawString) {
		if (!isStringNonEmpty(rawString)) return new HashMap<String, String>();
		return Device.getAttributesFromString(rawString.trim());
	}
	
	public static ArrayList<Event> getEventLog(final String rawString) {
		ArrayList<Event> events = new ArrayList<Event>();
		String resStr;
		if (!isStringNonEmpty(rawString)) return events;
		
		resStr = rawString.trim();
		for(String tmp: resStr.split(",")) {
			Event e = getEventFromString(tmp);
			if (e!=null) events.add(e);
		}
		return events;
	}
	public static ArrayList<Hub> getAllHubs(final String rawString) {
		ArrayList<Hub> hubs = new ArrayList<Hub>();
		Hub hubTmp;
		String resStr;
		if (!isStringNonEmpty(rawString)) return hubs;
		
		resStr = rawString.trim();
		if (resStr.contains(",")) {
			String[] hubArr = resStr.split(","); 
			for (String tmp: hubArr) {
				hubTmp = getHubFromString(tmp);
				if (hubTmp!=null) {
					hubs.add(hubTmp);
				}
			}					
		} else {
			hubTmp = getHubFromString(resStr);
			if (hubTmp!=null) {
				hubs.add(hubTmp);
			}

		}
		
		resStr = rawString.trim();
		return hubs;
	}
	
	public static ArrayList<Device> getAllDevices(final String rawString) {
		ArrayList<Device> devices = new ArrayList<Device>();
		Device deviceTmp;
		String resStr;
		if (!isStringNonEmpty(rawString)) return devices;

		resStr = rawString.trim();
		if (resStr.contains(",")) {
			String[] deviceArr = resStr.split(",");
			for (String tmp: deviceArr) {
				deviceTmp = getDeviceFromString(tmp);
				if (deviceTmp!=null) {
					devices.add(deviceTmp);
				}
			}					
		} else {
			deviceTmp = getDeviceFromString(resStr);
			if (deviceTmp!=null) {
				devices.add(deviceTmp);
			}

		}
		return devices;
	}
	public static ArrayList<String> getCommaStringList(final String rawString) {
		ArrayList<String> res = new ArrayList<String>();
		String resStr;
	
		if (!isStringNonEmpty(rawString)) return res;

		resStr = rawString.trim();
		if (resStr.contains(",")) {
			String[] strArr = resStr.split(",");
			for (String tmp: strArr) {
				if (isStringNonEmpty(tmp)) {
					res.add(tmp.trim());
				}
			}					
		} else {
			if (isStringNonEmpty(resStr)) {
				res.add(resStr.trim());
			}
		}
		
		return res;
	}
	
	// the results of performing the command, typically “ok” or “failed”.
	public static int getCommandResult(final String rawString) {
		if (!isStringNonEmpty(rawString)) return COMMAND_INVALID;
		String resStr = rawString.trim().toLowerCase();
		return (resStr.equals("ok"))? COMMAND_OK: COMMAND_NOTOK;
	}
	
	private static Device getDeviceFromString(final String rawinput) {
		Device res = null;
		String attrs = null;
		String input = rawinput;
		// 20120206 first see if the string has all the device values already as for getAllDeviceChannels
		if (input.contains(";")) {
			String[] alldetails = input.split(";");
			int asz = alldetails.length;
			if (asz>=2) {
				input = alldetails[0];
				attrs = "";
				for (int ai=1;ai<asz;ai++) {
					if (ai!=1) {
						attrs += ",";
					}
					attrs += alldetails[ai];
				}
			}
		} 
		String[] details = input.split("\\|");
		if (details.length>=3) {
			//Log.w(TAG, "DEVICE:: "+ details[0] +"|"+ details[1] +"|"+ details[2]);
			res = new Device(details[0], details[1], details[2]);
		}
		if (res!=null && attrs!=null) {
			res.setAttributesFromString(attrs);
		}
		return res;
	}
	private static Hub getHubFromString(final String input) {
		Hub res = null;
		String[] details = input.split("\\|");
		if (details.length>=2) {
			//Log.w(TAG, "HUB:: "+ details[0] +"|"+ details[1]);
			res = new Hub(details[0], details[1]);
		}
		return res;
	}
	public static Event getEventFromString(final String input) {
		Event event = null;
		//"||" // timestamp and message
		//"|"  // timestamp, zidlabel, loggedid, typelabel, logged type, message
		if (input==null) {
			// nothing..
		} else if (input.contains("||")) {
			String[] mesgL = input.split("\\|\\|");
			if (mesgL.length>=2) {
				long ts = 0;
				try {
					ts = Integer.parseInt(mesgL[0].trim());
					event = new Event(ts, mesgL[1]);
				} catch (NumberFormatException nfe) {
					//Log.w(TAG, "getEventFromString ::Integer error for "+ts);
				}
			}
		} else {
			boolean parsed = false;
			String timestamp = "";
			String zid = "";
			String lid = "";
			String tl = "";
			String tly = "";
			String mesg = "";
			if (input.contains("|[(")) {
				// Capture the events 'id' in the format [(...)]
				int isz = input.length();
				int startPos = input.indexOf("|[(");
				int endPos = input.indexOf(")]|");
				if (startPos!=-1&& endPos!=-1) {
					if (startPos+3<isz) {
						String[] dmesgL = {};
						String fixedInput = input;
						lid = input.substring(startPos+3, endPos);
						fixedInput = fixedInput.replace("|[("+lid+")]|", "|<ZID>|");
						dmesgL = fixedInput.split("\\|");
						if (dmesgL!=null && dmesgL.length>=6) {
							timestamp = dmesgL[0];
							zid = dmesgL[2];
							tl = dmesgL[3];
							tly = dmesgL[4];
							mesg = dmesgL[5];						
							parsed = true;							
						}
					}
				}
			} else {
				String[] dmesgL = input.split("\\|");
				if (dmesgL!=null && dmesgL.length>=6) {
					timestamp = dmesgL[0];
					zid = dmesgL[1];
					lid = dmesgL[2];
					tl = dmesgL[3];
					tly = dmesgL[4];
					mesg = dmesgL[5];
					parsed = true;
				}
			}
			/*
			 
	public DeviceEvent(final long timestamp, final String zid, final String lid, final String tl, final String lty, final String mesg) {
		super(timestamp, mesg);
		zIdLabel = zid;
		loggedId = lid;
		typeLabel = tl;
		loggedType = lty;
		Log.w("device", " DEVICE:::: ["+timestamp+"]  mesg::"+zIdLabel+"~"+loggedId+"~"+typeLabel+"~"+loggedType+"~"+message);
	}
			 * */
			
			if (parsed) {
				long ts = 0;
				
				try {
					ts = Integer.parseInt(timestamp);
					event = new DeviceEvent(ts, zid, lid, tl, tly, mesg);
				} catch (NumberFormatException nfe) {
					//Log.w(TAG, "getEventFromString ::Integer error for "+ts);
				}
			}
		}
		//Log.w(TAG, "getEventFromString ["+input+"] call returned type: "+event);
		return event;
	}
	public static String getStringReplacedWithToken(final String input, final String tag, final String value) {
		String res = input;
		
		if (isStringNonEmpty(input) && isStringNonEmpty(tag)) {
			if (res.contains(tag)) {
				res = res.replace(tag, value);
			}
		}
		
		return res;
	}
	public static boolean isStringNonEmpty(final String input) {
		return (input!=null && input.length()!=0);
	}
}
