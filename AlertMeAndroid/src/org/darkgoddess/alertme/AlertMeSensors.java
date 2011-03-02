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

import org.darkgoddess.alertme.api.AlertMeSession;
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
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author      foo bar <address@ example.com>
 * @version     201111.0210
 * @since       1.6
 */
public class AlertMeSensors extends Activity {
	private static final String TAG = "ACTIVITY:AlertMeSensors";
	private Bundle savedState = null;
	private AMViewItems screenStuff = null;
	private AlertMeSession alertMe = null;
	private ArrayList<Device> devices = new ArrayList<Device>();
	private ListView deviceList = null;
	private TextView systemName = null;
	private boolean isActive = false;
	private boolean hasCreated = false;
	private Device viewDevice = null;
	private int[] rowBg = null;

	private final View.OnClickListener deviceControllerClick = new View.OnClickListener() {
		public void onClick(View view) {
			// first do the click
			initScreenStuff();
			if (viewDevice!=null) {
				// attempt to toggle!
				SharedPreferences sharedPrefs = getSharedPreferences(AlertMeConstants.PREFERENCE_NAME, AlertMeConstants.PREFERENCE_MODE);
				SensorListStarter listloader = new SensorListStarter(alertMe, handler, getIntent(), null, sharedPrefs);
				String relay = viewDevice.getAttribute("relaystate");
				
				if (relay.equals("True")) {
					listloader.instruction = AlertMeConstants.INVOKE_SENSOR_CLAMP_OFF;
				} else {
					listloader.instruction = AlertMeConstants.INVOKE_SENSOR_CLAMP_ON;					
				}
				screenStuff.setBusy(AlertMeConstants.UPDATE_SENSORS);
				listloader.start();					
			}
			// now dismiss
			if (screenStuff!=null) {
				screenStuff.dismissActiveDialog(AMViewItems.DEVICE_DIALOG);
			}
		}
	};
	
    // Handler to update the interface..        
	final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            int mesgType = msg.getData().getInt("type");
            String mesgData = msg.getData().getString("value");
            performUpdate(mesgType, mesgData);
        }
    };
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		isActive = true;
		setContentView(R.layout.alertme_sensors);
		loadFromRestoredState();
		alertMe = (alertMe==null)? new AlertMeSession(this): alertMe;
		if (!hasCreated) {
			hasCreated = true;
			initView();
		}
		rowBg = AMViewItems.getRowColours(this);
	}
	@Override
    public void onStart() {
		super.onStart();

		loadFromRestoredState();
		initScreenStuff();
		loadDeviceList(savedState);
	}
	@Override
	protected Dialog onCreateDialog(int id) {
    	Dialog res = screenStuff.onCreateDialog(id);
        return res;
    }
	@Override
	public void finish() {
		isActive = false;
		alertMe.clean();
		super.finish();
		
	}
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		alertMe.onSaveInstanceState(outState);
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
	public Object onRetainNonConfigurationInstance() {
		SensorState saveState = new SensorState();
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onRetainNonConfigurationInstance()  START");
		saveState.sessionState = alertMe.retrieveCurrentState();
		saveState.currentDevice = viewDevice;
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onRetainNonConfigurationInstance()  END");
		return saveState;
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

	private void loadFromRestoredState() {
		final Object data = getLastNonConfigurationInstance();
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "loadFromRestoredState()  START");
		if (data != null) {
			boolean reloaded = false;
			final SensorState oldSensorState = (SensorState) data;
			final AlertMeSession.SessionState oldState = oldSensorState.sessionState;
			viewDevice = oldSensorState.currentDevice;
			if (alertMe==null) {
				alertMe = new AlertMeSession(this);
			}
			reloaded = alertMe.loadFromCachedState(this, oldState);
			
			if (reloaded) {
				performUpdate(AlertMeConstants.UPDATE_ALL, null);
			}
			if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "loadFromRestoredState()  did reload from old state:"+reloaded);
		}
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "loadFromRestoredState()  END");
	}
	
	private void initView() {
		deviceList = (deviceList==null)? (ListView) findViewById(R.id.sensors_list): deviceList;
		systemName = (systemName==null)? (TextView) findViewById(R.id.sensors_housename): systemName;
	}
	private void loadDeviceList(Bundle savedInstanceState) {
		//DeviceAdapter deviceAd = null;
		if (alertMe.requiresRefresh()) {
			SharedPreferences sharedPrefs = getSharedPreferences(AlertMeConstants.PREFERENCE_NAME, AlertMeConstants.PREFERENCE_MODE);
			Intent intent = getIntent();
			SensorListStarter listloader = new SensorListStarter(alertMe, handler, intent, savedInstanceState, sharedPrefs);
			initScreenStuff();
			screenStuff.setBusy(AlertMeConstants.UPDATE_SENSORS);
			listloader.start();
		}
	}
    private void updateScreenSystemName(String name) {
    	if (systemName!=null && name!=null && name.length()!=0) {
    		systemName.setText(name);
    	}
    }
    
    private void performUpdate(int command, final String mesgData) {
    	int toastMessage = 0;
    	if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate("+command+", '"+mesgData+"')  START");
    	initScreenStuff();
    	if (!isActive) {
    		// Not active when onCreate is not done OR finish() is called
    		screenStuff.setNotBusy();
    		return;
    	} else {
    		Hub hub = alertMe.retrieveActiveHub();
    		if (hub!=null) {
        		updateScreenSystemName(hub.name);    			
    		}
    		if (devices!=null && !devices.isEmpty()) {
        		DeviceAdapter deviceAd = new DeviceAdapter(this, R.layout.alertme_sensors_row, devices);
        		initView();
        		deviceList.setAdapter(deviceAd);
    		} else {
     			// empty
         		ArrayAdapter<String> emptyList;
     			String[] empty = { getString(R.string.sensor_list_isempty) }; 
     			emptyList = new ArrayAdapter<String>(this, R.layout.alertme_listempty, empty);
     			initView();
     			deviceList.setAdapter(emptyList);
     		}
    	}
    	switch (command) {
    		case AlertMeConstants.UPDATE_ALL:
    			// nothing...
    			break;
    		case AlertMeConstants.INVOKE_SENSOR_CLAMP_CHANGE_FAIL:
    			toastMessage = R.string.powercontroller_change_fail;
        		break;
    		case AlertMeConstants.INVOKE_SENSOR_CLAMP_CHANGE_NONE:
    			toastMessage = R.string.powercontroller_change_none;
        		break;
    		case AlertMeConstants.INVOKE_SENSOR_CLAMP_OFF:
    			toastMessage = R.string.powercontroller_change_off;
        		break;
    		case AlertMeConstants.INVOKE_SENSOR_CLAMP_ON:
    			toastMessage = R.string.powercontroller_change_on;
        		break;
    	}
    	screenStuff.setNotBusy();
    	if (toastMessage!=0) {
    		Toast.makeText(getApplicationContext(), getString(toastMessage), Toast.LENGTH_SHORT).show();
    	}
    	if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  END");
    }
    private void initScreenStuff() {
		if (screenStuff==null) {
			screenStuff = new AMViewItems(this, this);
			screenStuff.registerSystemName(R.id.sensors_housename);
			screenStuff.registerDeviceDialog(viewDevice, deviceControllerClick);
			screenStuff.initDeviceDialog();
		}
    } 
    class SensorState {
    	public AlertMeSession.SessionState sessionState = null;
    	public Device currentDevice = null;
    }

	class SensorListStarter extends Thread {
		public boolean forceLogin = true;
		public int instruction = -1;
		private AlertMeSession alertme;
		private Handler handler;
		private Intent intent;
		private Bundle bundle;
		private SharedPreferences sharedPrefs;

		public SensorListStarter(AlertMeSession client, Handler handle, Intent intentIn, Bundle bundleIn, SharedPreferences prefs) {
			alertme = client;
			handler = handle;
			intent = intentIn;
			bundle = bundleIn;
			sharedPrefs = prefs;
		}		
		
        @Override
        public void run() {
    		boolean hasCurrentSys = alertme.hasSessionValues();
    		int updateInstruction = AlertMeConstants.UPDATE_ALL;

    		// Check for the current systemID in the bundle if not in preferences..		
    		if (!hasCurrentSys) hasCurrentSys = alertme.loadFromIntentBundle(intent, bundle);
    		if (!hasCurrentSys) hasCurrentSys = alertme.loadFromPreference(sharedPrefs);
    		if (!hasCurrentSys) hasCurrentSys = alertme.loadFromOnlyEntry();
    		if (hasCurrentSys) {
    			if (alertme.devicesRequiresRefresh()) {
        			devices = alertme.getDeviceData();    				
    			} else {
        			devices = alertme.retrieveDevices();	
    			}    			
    		}

    		switch(instruction) {
				case AlertMeConstants.INVOKE_SENSOR_CLAMP_ON:
    			case AlertMeConstants.INVOKE_SENSOR_CLAMP_OFF:
    				boolean doChange = false;
    				boolean newRelayMode = false;
    				boolean wasOk = false;
    				String relay = "";
    				updateInstruction = AlertMeConstants.INVOKE_SENSOR_CLAMP_CHANGE_FAIL;
    				if (viewDevice!=null) {
    					relay = viewDevice.getAttribute("relaystate");
    					
        				if (instruction==AlertMeConstants.INVOKE_SENSOR_CLAMP_ON) {
        					// Currently off, instruct to go on
        					doChange = (relay!=null && relay.equals("False"));
        					newRelayMode = true;
        				} else {
        					// Currently on, instruct to go off
        					doChange = (relay!=null && relay.equals("True"));
        					newRelayMode = false;
        				}
        				if (doChange) {
        					wasOk = alertme.changeRelayState(viewDevice, newRelayMode);
        					updateInstruction = (wasOk)? instruction: AlertMeConstants.INVOKE_SENSOR_CLAMP_CHANGE_FAIL;
        				} else {
        					updateInstruction = AlertMeConstants.INVOKE_SENSOR_CLAMP_CHANGE_NONE;
        				}
    					if (wasOk) {
    						String newVal = (newRelayMode)? "True": "False";
    						viewDevice.setAttribute("relaystate", newVal);
    						devices = alertme.retrieveDevices(); // refresh
    						alertMe = alertme; // store!
    					}
    				} else {
    					updateInstruction = AlertMeConstants.INVOKE_SENSOR_CLAMP_CHANGE_FAIL;
    				}
    				break;
    		}
    		
    		if (handler!=null) {
    			Message msg = handler.obtainMessage();
                Bundle b = new Bundle();
                b.putInt("type", updateInstruction);
                msg.setData(b);
                handler.sendMessage(msg);
    		}        		
        }
		
	}
	class DeviceClicker implements OnClickListener {
		private Device device = null;
		
		public DeviceClicker(Device d) { device = d; }
        @Override
            public void onClick(View v) {
        	if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "deviceList::onClick()  START");
        	initScreenStuff();
        	viewDevice = device;
        	screenStuff.showDeviceInDialog(device);
        	if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "deviceList::onClick()  END");
        }
	}
	class DeviceAdapter extends ArrayAdapter<Device> {
		private ArrayList<Device> items;

        public DeviceAdapter(Context context, int textViewResourceId, ArrayList<Device> deviceList) {
                super(context, textViewResourceId, deviceList);
                items = deviceList;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = vi.inflate(R.layout.alertme_sensors_row, null);
                }
                Device d = items.get(position);
                if (d != null) {
                		ImageView icon = (ImageView) v.findViewById(R.id.sensorline_type_icon);
                		ImageView batticon = (ImageView) v.findViewById(R.id.sensorline_battery_icon);
                        TextView battery = (TextView) v.findViewById(R.id.sensorline_battery);
                        TextView status = (TextView) v.findViewById(R.id.sensorline_status);
                        TextView name = (TextView) v.findViewById(R.id.sensorline_name);
                        TextView type = (TextView) v.findViewById(R.id.sensorline_type);
                		ImageView sigicon = (ImageView) v.findViewById(R.id.sensorline_signal_icon);
                        if (battery!=null) {
                        	battery.setText(""+d.batteryLevel);
                        }
                        if (status!=null) {
                        	status.setText(getStatusString(d));
                        }
                        if (name!=null) {
                        	name.setText(d.name);
                        }
                        if (type!=null) {
                        	type.setText(d.dtype);
                        }
                        if (icon!=null) {
                        	icon.setImageResource(AlertMeConstants.getTypeIcon(d.type));
                        }
                        if (batticon!=null) {
                        	batticon.setImageResource(AlertMeConstants.getBatteryIcon(d));
                        }
                        if (sigicon!=null) {
                        	sigicon.setImageResource(AlertMeConstants.getSignalIcon(d));
                        }
                        //if (position%2==0) { v.setBackgroundColor(R.color.menu_even); }
                        //else { v.setBackgroundColor(R.color.menu_odd); }
                        v.setOnClickListener(new DeviceClicker(d));
                }
                if (rowBg!=null) {
                	int colorPos = position % rowBg.length;
                	v.setBackgroundColor(rowBg[colorPos]);
                }
                return v;
        }
        private String getStatusString(Device d) {
        	String res = "";
        	
        	if (d.type==Device.KEYFOB) {
        		String pres = d.getAttribute("presence");
        		if (pres!=null) {
        			if (pres.equals("True")) {
        				res = "Present";
        			} else {
        				res = "Away";
        			}
        		}
        	} else {
        		if (d.type == Device.CONTACT_SENSOR) {
        			String closed = d.attributes.get("closed");
        			if (closed!=null && closed.length()!=0 && closed.equalsIgnoreCase("true")) {
        				res = "Closed";
        			} else {
        				res = "Open";
        			}
        		} else {
            		String tampered = d.attributes.get("tamper");
            		if (tampered!=null) {
            			if (tampered.equals("True")) {
            				res = "Not Ok";
            			} else {
            				res = "All Ok";
            				if (d.type != Device.POWER_CONTROLLER) {
            					if (d.getBatteryLevel()<3) {
            						res = "Battery low";
            					}
            				}
            			}
            			
            		}
        			
        		}
        	}
        	
        	return res;
        }
	}
}
