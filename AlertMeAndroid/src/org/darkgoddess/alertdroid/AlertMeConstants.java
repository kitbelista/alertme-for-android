/**
 * 
 * Copyright 2012 Kathlene Belista
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

package org.darkgoddess.alertdroid;


import org.darkgoddess.alertdroid.api.utils.Device;
import org.darkgoddess.alertdroid.R;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.Time;

public class AlertMeConstants {
	public static final boolean DEBUGOUT = false;
	
	public static final int IS_OFFLINE = -1;
	public static final int UPDATE_CANCEL = 0;
	public static final int UPDATE_STATUS = 1;
	public static final int UPDATE_PEOPLE = 2;
	public static final int UPDATE_SENSORS = 3;
	public static final int UPDATE_SYSTEMNAME = 4;
	public static final int UPDATE_COMMANDLIST = 7;
	public static final int UPDATE_ALL = 8;
	public static final int UPDATE_ON_START = 9;
	
	
	public static final int COMMAND_SETTINGS = 10;
	public static final int COMMAND_SETTINGS_CONFIRM = 11;
	public static final int COMMAND_SETTINGS_CANCEL = 12;
	public static final int INVOKE_SETTINGS_CREATE = 13;
	public static final int INVOKE_SETTINGS_EDIT = 14;
	public static final int INVOKE_SETTINGS_CREATE_FIRSTTIME = 15;
	public static final int COMMAND_SETTINGS_CREATE_CONFIRM = 16;
	public static final int COMMAND_SETTINGS_CREATE_CANCEL = 17;
	public static final int COMMAND_SETTINGS_CREATE_CONFIRMFIRSTTIME = 18;
	public static final int COMMAND_SETTINGS_CREATE_FAILED = 19;
	
	public static final int INVOKE_STATUS = 20;
	public static final int COMMAND_STATUS_HOME = 21;
	public static final int COMMAND_STATUS_AWAY = 22;
	public static final int COMMAND_STATUS_NIGHT = 23;
	public static final int COMMAND_STATUS_STOPALARM = 24;
	public static final int COMMAND_STATUS_STOPEMERGENCY = 25;
	public static final int COMMAND_STATUS_STOPALARM_OK = 26;
	public static final int COMMAND_STATUS_STOPALARM_FAILED = 27;
	public static final int COMMAND_STATUS_CANCEL = 29;
	public static final int INVOKE_STATUS_CONFIRM = 28;

	public static final int INVOKE_SENSORS = 30;

	public static final int INVOKE_PEOPLE = 40;
	
	public static final int INVOKE_HISTORY = 50;
	public static final int COMMAND_HISTORY_CLEAR = 52;
	
	public static final int INVOKE_HELP = 60;

	public static final int INVOKE_HUB_CHOICE = 70;
	public static final int INVOKE_HUB_SELECT = 71;
	public static final int INVOKE_HUB_SELECT_OK = 72;
	public static final int INVOKE_HUB_SELECT_FAIL = 73;
	public static final int INVOKE_ACCOUNT_CHOICE = 74;
	public static final int INVOKE_ACCOUNT_SELECT = 75;
	public static final int INVOKE_ACCOUNT_SELECT_OK = 76;
	public static final int INVOKE_ACCOUNT_SELECT_FAIL = 77;
	
	public static final int INVOKE_SENSOR_CLAMP_OFF = 111;
	public static final int INVOKE_SENSOR_CLAMP_ON = 112;
	public static final int INVOKE_SENSOR_CLAMP_CHANGE_FAIL = 113;
	public static final int INVOKE_SENSOR_CLAMP_CHANGE_NONE = 114;

	public static final int COMMAND_TEST = 80;
	public static final int INVOKE_TEST = 80;
	
	public static final int COMMAND_REFRESH = 99;
	public static final int COMMAND_QUIT = 100;
	
	public static final int EVENT_ITEM_LIMIT = 50;

	public final static String PREFERENCE_NAME = "AlertMeAndroid";
	public final static int PREFERENCE_MODE = Context.MODE_PRIVATE; // private mode
	public static final String INTENT_RETURN_USERNAME = "alertMeUsername";
	public static final String INTENT_RETURN_PASSWORD = "alertMePassword";
	
	public static final String INTENT_RETURN_KEY = "alertMeReturn";
	public static final String INTENT_REQUEST_KEY = "alertMeRequest";
	public static final String INTENT_RETURN_SYSID = "alertMeId";
	public static final String INTENT_DEVICE_LIST = "alertMeDevices";
	
	public static final String HANDLER_DATA_TYPE = "type";
	public static final String HANDLER_DATA_VALUE = "value";
	public static final String STR_TRUE = "true";
	public static final String STR_FALSE = "false";
	public static final String RELAYMODE_TRUE = "True";
	public static final String RELAYMODE_FALSE = "False";
	public static final String STR_COMMA = ",";
	public static final String STR_STOP = ".";
	public static final String EMPTY_STR = "";
	public static final String STR_PRESENCE = "presence";
	public static final String STR_FIRSTNAME = "firstname";
	public static final String STR_LASTNAME = "lastname";
	public static final String STR_RELAYSTATE = "relaystate";
	public static final String STR_BATTERYLEVEL = "batterylevel";
	public static final String STR_TEMPERATURE = "temperature";
	public static final String STR_LQI = "lqi";
	public static final String STR_CLOSED = "closed";
	public static final String STR_TAMPER = "tamper";
	public static final String STR_AM = "AM";
	public static final String STR_HOME = "home";
	public static final String STR_AWAY = "away";
	public static final String STR_NIGHT = "night";
	public static final String ALARM_STR_ALARMED = "alarmed";
	public static final String ALARM_STR_CONFIRMED = "alarmConfirmed";
	public static final String STR_SEP_DASH = " - ";
	public static final String EVENT_START_ARMEDBY = "The Intruder Alarm was armed by ";
	public static final String EVENT_START_DISARMEDBY = "The Intruder Alarm was disarmed by ";
	public static final String EVENT_KEYFOB_OWNED = "'s Keyfob";
	public static final String EVENT_MODE_CHANGE = "Behaviour changed to";
	public static final String STR_DEGREE_CELCIUS = "\u00B0C";
	
	public static final char STR_V = 'V';
	public static final char STR_W = 'W';
	public static final char STR_ZERO = '0';
	public static final char STR_DEGREE = '\u00B0';
	public static final char STR_PERCENT = '%';

	public static final String MODE_HOME_OK = "home_ok";
	public static final String MODE_AWAY_OK = "away_ok";
	public static final String MODE_NIGHT_OK = "night_ok";
	
	public static boolean isCommandValid(int command) {
		boolean res = false;
        switch (command) {
        	case AlertMeConstants.UPDATE_CANCEL:
        	case AlertMeConstants.UPDATE_SYSTEMNAME:
        	case AlertMeConstants.UPDATE_STATUS:
        	case AlertMeConstants.UPDATE_PEOPLE:
        	case AlertMeConstants.UPDATE_SENSORS:
        	case AlertMeConstants.UPDATE_COMMANDLIST:
        	case AlertMeConstants.UPDATE_ALL:
        		res = true;
            	break;
        }
		return res;
	}
	public static int getInvokeKeycodeFromIntentBundle(Intent intent, Bundle savedInstanceState, int defaultAction) {
		int intendedMode = savedInstanceState != null ? savedInstanceState.getInt(AlertMeConstants.INTENT_REQUEST_KEY) : 0;
		intendedMode = (intent!=null && intendedMode==0)? intent.getIntExtra(AlertMeConstants.INTENT_REQUEST_KEY, 0) : intendedMode;
        if (intent!=null && intendedMode == 0) {
                Bundle extras = intent.getExtras();
                intendedMode = extras != null ? extras.getInt(AlertMeConstants.INTENT_REQUEST_KEY) : 0;
        }
        if (intendedMode==0) {
        	intendedMode = defaultAction;
        }
        return intendedMode;
	}
	public static int getReturnKeycodeFromIntentBundle(Intent intent, Bundle savedInstanceState, int defaultAction) {
		int intendedMode = (savedInstanceState != null)? savedInstanceState.getInt(AlertMeConstants.INTENT_RETURN_KEY) : defaultAction;
		intendedMode = (intent!=null && intendedMode==defaultAction)? intent.getIntExtra(AlertMeConstants.INTENT_RETURN_KEY, defaultAction) : intendedMode;
        if (intent!=null && intendedMode == defaultAction) {
                Bundle extras = intent.getExtras();
                intendedMode = (extras!=null)? extras.getInt(AlertMeConstants.INTENT_RETURN_KEY) : defaultAction;
        }
        return intendedMode;
	}
    public static int getSignalIcon(Device d) {
    	int res = R.drawable.ic_sensor_signal_0;
    	switch (d.getSignalLevel()) {
		case 1:
			res = R.drawable.ic_sensor_signal_1;
			break;
		case 2:
			res = R.drawable.ic_sensor_signal_2;
			break;
		case 3:
			res = R.drawable.ic_sensor_signal_3;
			break;
		case 4:
			res = R.drawable.ic_sensor_signal_4;
			break;
		case 5:
			res = R.drawable.ic_sensor_signal_5;
			break;
    	}
    	return res;
    }
    public static int getBatteryIcon(Device d) {
    	int res = R.drawable.ic_sensor_battery_0;
    	switch (d.getBatteryLevel()) {
    		case 1:
    			res = R.drawable.ic_sensor_battery_1;
    			break;
    		case 2:
    			res = R.drawable.ic_sensor_battery_2;
    			break;
    		case 3:
    			res = R.drawable.ic_sensor_battery_3;
    			break;
    		case 4:
    			res = R.drawable.ic_sensor_battery_4;
    			break;
    		case 5:
    			res = R.drawable.ic_sensor_battery_5;
    			break;
    	}
    	return res;
    }
    public static int getTypeIcon(int deviceTypeId) {
    	int res = R.drawable.icon;
    	switch (deviceTypeId) {
    		case Device.ALARM_DETECTOR:
    			res = R.drawable.ic_sensor_alarmdetect;
    			break;
    		case Device.BUTTON:
    			res = R.drawable.ic_sensor_button;
    			break;
    		case Device.CONTACT_SENSOR:
    			res = R.drawable.ic_sensor_contact;
    			break;
    		case Device.KEYFOB:
    			res = R.drawable.ic_sensor_keyfob;
    			break;
    		case Device.LAMP:
    			res = R.drawable.ic_sensor_lamp;
    			break;
    		case Device.MOTION_SENSOR:
    			res = R.drawable.ic_sensor_motion;
    			break;
    		case Device.POWER_CONTROLLER:
    			res = R.drawable.ic_sensor_powerplug;
    			break;
    		case Device.POWERCLAMP:
    			res = R.drawable.ic_sensor_meter;
    			break;
    	}
    	return res;
    }
    public static int getDetailTypeIcon(int deviceTypeId) {
    	int res = R.drawable.icon;
    	switch (deviceTypeId) {
    		case Device.ALARM_DETECTOR:
    			res = R.drawable.ic_sensordetail_alarmdetect;
    			break;
    		case Device.BUTTON:
    			res = R.drawable.ic_sensordetail_button;
    			break;
    		case Device.CONTACT_SENSOR:
    			res = R.drawable.ic_sensordetail_contact;
    			break;
    		case Device.KEYFOB:
    			res = R.drawable.ic_sensordetail_keyfob;
    			break;
    		case Device.LAMP:
    			res = R.drawable.ic_sensordetail_lamp;
    			break;
    		case Device.MOTION_SENSOR:
    			res = R.drawable.ic_sensordetail_motion;
    			break;
    		case Device.POWER_CONTROLLER:
    			res = R.drawable.ic_sensordetail_powerplug;
    			break;
    		case Device.POWERCLAMP:
    			res = R.drawable.ic_sensordetail_meter;
    			break;
    	}
    	return res;
    }
	public static boolean isStateAlarmed(String s) {
		boolean res = false;
		if (s!=null) {
			if (s.equals("alarmed")||s.equals("alarmConfirmed")) {
				res = true;
			}							
		}
		return res;
	}
    // TODO: Make the format a formula for other languages
    public static String getDateTitle(Time currentDate, Time titleTime) {
    	String res = null;
    	int compareDate = Time.compare(currentDate, titleTime);
    	// res = titleTime.format("%Y%m%d");
    	
    	// CurrentDate is supposed to be the day start (midnight)
    	// test if the date is the same
    	if (compareDate<=0) {
    		res = "Today";
    	} else {
    		// currentDate > titleTime
    		long dayMilli = (1000*60*60*24);
    		long yesterdayTime = currentDate.toMillis(true) - dayMilli;
    		long compareYest = titleTime.toMillis(true) - yesterdayTime;
    		
    		if (compareYest>0) {
    			res = "Yesterday";
    		} else {
    			String dayPost = null;
        		int dd = titleTime.monthDay;
        		int cDay = dd%10;
        		
        		switch(cDay) {
        			case 1:
        				if (dd!=11) dayPost = "st";
        				break;
        			case 2:
        				if (dd!=12) dayPost = "nd";
        				break;
        			case 3:
        				if (dd!=13) dayPost = "rd";
        				break;
        			default:
        				dayPost = "th";
        				break;
        		}
    			String formatStr = "%A "+dd+""+dayPost+" %B";
    			if (currentDate.year!=titleTime.year) {
    				formatStr += ", %Y";
    			}
    			res = titleTime.format(formatStr);
    		}
    	}
    	if (res==null) res = ""; // \0_o/
    	return res;
    }

    public static final String ALARM_INTRUDER = "IntruderAlarm";
    public static final String ALARM_EMERGENCY = "EmergencyAlarm";
    public static final String RELAYSTATE_ON = "on";
    public static final String RELAYSTATE_OFF = "off";
    public static final String EVENTLOG_MODE_TO_HOME = "Behaviour changed to At home";
    public static final String EVENTLOG_MODE_TO_AWAY = "Behaviour changed to Away";
    public static final String EVENTLOG_MODE_TO_NIGHT = "Behaviour changed to Night";
    public static final String EVENTLOG_HUB_GONE = "The hub disappeared from network";
    public static final String EVENTLOG_DISARMED_FROM = "The Intruder Alarm was disarmed from ";
    public static final String EVENTLOG_ARMED_FROM = "The Intruder Alarm was armed from ";
	public static final String LEGACY_APPNAME = "AlertMe for Android";

    public static int getIconFromEventMessage(String appName, String eventMessage) {
    	int res = 0;
    	String mesg = eventMessage.trim();
    	if (mesg.equalsIgnoreCase(EVENTLOG_MODE_TO_HOME)) {
    		res = R.drawable.ic_sensor_hub;        		
    	} else if (mesg.equalsIgnoreCase(EVENTLOG_MODE_TO_AWAY)) {
    		res = R.drawable.ic_sensor_hub;
    	} else if (mesg.equalsIgnoreCase(EVENTLOG_MODE_TO_NIGHT)) {
    		res = R.drawable.ic_sensor_hub;
    	} else if (mesg.equalsIgnoreCase(EVENTLOG_HUB_GONE)) {
    		res = R.drawable.ic_home_sensors_notok;
    	} else if (mesg.equalsIgnoreCase(EVENTLOG_DISARMED_FROM+appName)) {
    		res = R.drawable.icon_online;
    	} else if (mesg.equalsIgnoreCase(EVENTLOG_ARMED_FROM+appName)) {
    		res = R.drawable.icon_online;
    	} else if (mesg.equalsIgnoreCase(EVENTLOG_DISARMED_FROM+LEGACY_APPNAME)) {
    		res = R.drawable.icon_online;
    	} else if (mesg.equalsIgnoreCase(EVENTLOG_ARMED_FROM+LEGACY_APPNAME)) {
    		res = R.drawable.icon_online;
    	}
    	//The Intruder Alarm was set off by Front Hall Motion Sensor	22:06
    	//The Front Door Door/Window Sensor was triggered. The alarm will be raised if further triggers are detected
    	
    	return res;
    }
}