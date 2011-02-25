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


import org.darkgoddess.alertme.api.utils.Device;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

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
	public static final int COMMAND_STATUS_CANCEL = 29;
	public static final int INVOKE_STATUS_CONFIRM = 28;

	public static final int INVOKE_SENSORS = 30;

	public static final int INVOKE_PEOPLE = 40;
	
	public static final int INVOKE_HISTORY = 50;
	
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
}