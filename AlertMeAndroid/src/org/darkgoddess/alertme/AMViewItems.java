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
import org.darkgoddess.alertme.api.utils.Hub;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 
 * Just a class to move the dialogs from the main activities (AlertMe[X]).  Since
 * the ProgressDialog gets used quite a bit, this mainly provides a simple way of
 * displaying 'busy-ness' by having functions {@link #setBusy(int)} and
 * {@link #setNotBusy()} (where the int is a constant identifying state)
 *
 * Note that creation needs to occur onStart and not before so the
 * passed-in activity has set the context view to use.
 * 
 * When using a dialog from this, you must call the register function AND init
 * as not all activities require all dialogs provided
 */ 
public class AMViewItems {
	private static final String TAG = "AMViewItems";
	protected Activity activity = null;
	protected Context context = null;
	private Device viewDevice = null;
	
	public AMViewItems(Activity activityIn, Context contextIn) {
		activity = activityIn;
		context = contextIn;
		initProgressDialog();
		
	}
	public void dismissActiveDialog(int id) {
		currentDialog = -1;
        switch(id) {
	    	case PROGRESS_DIALOG:
	    		if (progressDialog!=null) progressDialog.dismiss();
	    		break;
	    	case DEVICE_DIALOG:
	    		if (deviceDialog!=null) deviceDialog.dismiss();
	    		break;
	    	case DISMISS_DIALOG:
	    		if (dismissDialog!=null) dismissDialog.dismiss();
	    		break;
	    	case HUB_CHOICE_DIALOG:
	    		if (hubChoiceDialog!=null) hubChoiceDialog.dismiss();
	    		break;
	    	case ACCOUNT_CHOICE_DIALOG:
	    		if (accountChoiceDialog!=null) accountChoiceDialog.dismiss();
	    		break;
	    	case QUIT_DIALOG:
	    		if (quitDialog!=null) quitDialog.dismiss();
	    		break;
        }
	}
	public Dialog onCreateDialog(int id) {
    	Dialog res = null;
        switch(id) {
        	case PROGRESS_DIALOG:
        		initProgressDialog();
        		res = progressDialog;
        		break;
        	case DEVICE_DIALOG:
        		initDeviceDialog();
        		if (viewDevice!=null) {
        			prepareDeviceInDialog(viewDevice);
        		}
        		res = deviceDialog;
        		break;
        	case DISMISS_DIALOG:
        		initDismissDialog();
        		res = dismissDialog;
        		break;
        	case HUB_CHOICE_DIALOG:
        		initHubDialog();
        		res = hubChoiceDialog;
        		break;
        	case ACCOUNT_CHOICE_DIALOG:
        		initAccountDialog();
        		res = accountChoiceDialog;
        		break;
        	case QUIT_DIALOG:
        		initQuitDialog();
        		res = quitDialog;
        		break;
        	case INFO_DIALOG:
        		initInfoDialog();
        		res = infoDialog;
        		break;
        }
        return res;
    }
	public void clean() {
		if (currentDialog!=-1) {
			if (createdDialogs[currentDialog]) dismissActiveDialog(currentDialog);
		}
		if (isBusy) {
			setNotBusy();
		}
		isActive = false;
		progressDialog = null;
		deviceDialog = null;
		dismissDialog = null;
		hubChoiceDialog = null;
		accountChoiceDialog = null;
		quitDialog = null;
	}
	public void showDialog(int id) {
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "showDialog("+id+")    START");
		currentDialog = id;
		if (id>=0 && id < 7) createdDialogs[id] = true;
		activity.showDialog(id);
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "showDialog()    END");
	}
	
    public void setBusy(int command) {
	    switch (command) {
    	case AlertMeConstants.UPDATE_SYSTEMNAME:	
    		setBusy("", activity.getString(R.string.home_command_update_systemname));
    		break;
    	case AlertMeConstants.UPDATE_STATUS:
    		setBusy("", activity.getString(R.string.home_command_update_behaviour));
    		break;
    	case AlertMeConstants.UPDATE_PEOPLE:
    		setBusy("", activity.getString(R.string.home_command_update_people));
        	break;
    	case AlertMeConstants.UPDATE_SENSORS:
    		setBusy("", activity.getString(R.string.home_command_update_sensors));
        	break;
    	case AlertMeConstants.INVOKE_HUB_CHOICE:
    		setBusy("", activity.getString(R.string.hubchoice_select_wait));
        	break;
    	case AlertMeConstants.INVOKE_HUB_SELECT:
    		setBusy("", activity.getString(R.string.hubchoice_invoke_change));
        	break;
    	case AlertMeConstants.COMMAND_QUIT:
    		setBusy("", activity.getString(R.string.quit_dialog_wait));
    		break;
    	case AlertMeConstants.INVOKE_HISTORY:
    		setBusy("", activity.getString(R.string.home_command_update_history));
    		break;
    	case AlertMeConstants.COMMAND_STATUS_AWAY:
    		setBusy("", activity.getString(R.string.behaviour_busy_message_away));
    		break;
    	case AlertMeConstants.COMMAND_STATUS_NIGHT:
    		setBusy("", activity.getString(R.string.behaviour_busy_message_night));
    		break;
    	case AlertMeConstants.COMMAND_STATUS_HOME:
    		setBusy("", activity.getString(R.string.behaviour_busy_message_home));
    		break;
    	case AlertMeConstants.INVOKE_ACCOUNT_SELECT:
    		setBusy("", activity.getString(R.string.accountchoice_invoke_change));
    		break;
    	case AlertMeConstants.INVOKE_SETTINGS_CREATE_FIRSTTIME:
    		setBusy("", activity.getString(R.string.settings_updating_values));
    		break;
    	case AlertMeConstants.INVOKE_TEST:
    		setBusy("", activity.getString(R.string.test_run_wait));
    		break;
    	default:
    		setBusy("", activity.getString(R.string.home_command_update_systemname));
    		break;
	    }
	}
    public void setNotBusy() {
    	if (progressDialog!=null && isBusy) {
    		isBusy = false;
    		unlockFixedScreenRotation();
    		try {
        		if (createdDialogs[PROGRESS_DIALOG]) activity.dismissDialog(PROGRESS_DIALOG);
    		} catch (Exception e) {}
    	}
    }
    public void setSystemName(Hub hub) {
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "setSystemName()   START");
    	if (hub!=null && systemName!=null) {
    		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "setSystemName()   -- value: "+hub.name);
    		systemName.setText(hub.name);
    	}
    	if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "setSystemName()   END");
    }
    public void setDismissMessage(final String message) {
		if (dismissDialog!=null) {
			dismissDialog.setMessage(message);
		}
    }

    public ProgressDialog getProgressDialog() {
    	return progressDialog;
    }
    public Dialog getDeviceDialog() {
    	return deviceDialog;
    }
    public AlertDialog getDismissDialog() {
    	return dismissDialog;
    }
    public AlertDialog getHubChoiceDialog() {
    	return hubChoiceDialog;
    }
    public AlertDialog getAccountChoiceDialog() {
    	return accountChoiceDialog;
    }
    public AlertDialog getQuitDialog() {
    	return quitDialog;
    }
    public AlertDialog getInfoDialog() {
    	return infoDialog;
    }
    public void registerSystemName(int resourceId) {
    	systemNameResourceId = resourceId;
    	initSystemName();
    }
    public void registerDeviceDialog() {
    	deviceDialogRegistered = true;
    }
    public void registerDeviceDialog(Device device) {
    	deviceDialogRegistered = true;
    	if (device!=null) {
    		viewDevice = device;
    	}
    }
    public void registerDeviceDialog(View.OnClickListener onPowerClick) {
    	deviceDialogRegistered = true;
    	deviceControlListener = onPowerClick;
    }
    public void registerDeviceDialog(Device device, View.OnClickListener onPowerClick) {
    	deviceDialogRegistered = true;
    	deviceControlListener = onPowerClick;
    	if (device!=null) {
    		viewDevice = device;
    	}
    }
    public void registerDismissDialog(DialogInterface.OnClickListener onclick) {
    	if (onclick!=null) {
    		dismissAction = onclick;
        	dismissDialogRegistered = true;    		
    	}
    }
    public void registerHubChoiceDialog(DialogInterface.OnClickListener onclick, String[] items) {
    	if (onclick!=null && items!=null) {
    		hubChoiceListener = onclick;
    		hubChoiceRegistered = true;
    		hubNamesChoiceList = items;
    	}
    }
    public void registerAccountChoiceDialog(DialogInterface.OnClickListener onclick, String[] items) {
    	if (onclick!=null && items!=null) {
    		accountChoiceListener = onclick;
    		accountChoiceRegistered = true;
    		accountNamesChoiceList = items;
    	}
    }
    public void registerQuitDialog(DialogInterface.OnClickListener onclick) {
    	if (onclick!=null) {
    		quitListener = onclick;
        	quitDialogRegistered = true;    		
    	}
    }
    public void registerQuitDialog(DialogInterface.OnClickListener onclickOk, DialogInterface.OnClickListener onclickCancel) {
    	if (onclickOk!=null) {
    		quitListener = onclickOk;
    		quitCancelListener = onclickCancel;
        	quitDialogRegistered = true;    		
    	}
    }
    public void registerInfoDialog() {
        infoDialogRegistered = true;    		
    }
    public void registerInfoDialog(DialogInterface.OnClickListener onclick) {
    	if (onclick!=null) {
    		infoListener = onclick;
        	infoDialogRegistered = true;    		
    	}
    }
    public void prepareDeviceInDialog(Device device) {
		if (deviceDialog!=null) {
			TextView deviceName = (TextView) deviceDialog.findViewById(R.id.device_label);
			TextView deviceType = (TextView) deviceDialog.findViewById(R.id.device_type);
			ImageView deviceImage = (ImageView) deviceDialog.findViewById(R.id.device_image);

    		ImageView batticon = (ImageView) deviceDialog.findViewById(R.id.device_data_battery_icon);
            TextView battlabel = (TextView) deviceDialog.findViewById(R.id.device_data_battery_label);
            TextView battery = (TextView) deviceDialog.findViewById(R.id.device_data_battery);
            ImageView sigicon = (ImageView) deviceDialog.findViewById(R.id.device_data_signal_icon);
            TextView siglabel = (TextView) deviceDialog.findViewById(R.id.device_data_signal_label);
            TextView signal = (TextView) deviceDialog.findViewById(R.id.device_data_signal);
            TextView temperature = (TextView) deviceDialog.findViewById(R.id.device_data_temperature);
            ImageView tempicon = (ImageView) deviceDialog.findViewById(R.id.device_data_temperature_icon);
            TextView templabel = (TextView) deviceDialog.findViewById(R.id.device_data_temperature_label);
            TextView powerlevel = (TextView) deviceDialog.findViewById(R.id.device_data_powerlevel);
            ImageView powicon = (ImageView) deviceDialog.findViewById(R.id.device_data_powerlevel_icon);
            TextView powlabel = (TextView) deviceDialog.findViewById(R.id.device_data_powerlevel_label);
            
            ImageView presicon = (ImageView) deviceDialog.findViewById(R.id.device_data_presence_icon);
            TextView presence = (TextView) deviceDialog.findViewById(R.id.device_data_presence);
			LinearLayout presenceSection =  (LinearLayout) deviceDialog.findViewById(R.id.device_data_presence_section);
			LinearLayout controlSection =  (LinearLayout) deviceDialog.findViewById(R.id.device_data_control_section);
			
			boolean batterySet = false;
			boolean signalSet = false;
			boolean tempSet = false;
			boolean presenceSet = false;
			boolean powerControlSet = false;
			boolean powerLevelSet = false;
			
			String type = device.getStringType();
			if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "showDeviceInDialog()   Showing device: " +device.name+" [id:"+device.id+"]");
			if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "showDeviceInDialog()   deviceName (view) ["+deviceName+"]");
			if (deviceName!=null) {
				deviceName.setText(device.name);
			}
			if (deviceType!=null) {
				deviceType.setText(type);
			}
			if (deviceImage!=null) {
				deviceImage.setImageResource(AlertMeConstants.getDetailTypeIcon(device.type));
			}
			// normally show the presence and hide controll
			if (presenceSection!=null) presenceSection.setVisibility(View.VISIBLE);
			if (controlSection!=null) controlSection.setVisibility(View.GONE);
			
			if (device.attributes!=null && !device.attributes.isEmpty()) {
				for (String channel: device.attributes.keySet()) {
					String val = device.attributes.get(channel);
					if (channel.equals("batterylevel") && device.isAttributeValid(channel)) {						
						// format the battery value to 2 decimal places
						String battLevel = getDecimalFromString(val, 2)+"V";
                        if (battery!=null) battery.setText(battLevel);
                        if (batticon!=null) batticon.setImageResource(AlertMeConstants.getBatteryIcon(device));
						batterySet = true;
						
					} else if (channel.equals("lqi") && device.isAttributeValid(channel)) {
						String LQILevel = getDecimalFromString(val, 0);
						val = LQILevel+"%";
						if (signal!=null) signal.setText(val);
						if (sigicon!=null) sigicon.setImageResource(AlertMeConstants.getSignalIcon(device));
						signalSet = true;
					} else if (channel.equals("temperature") && device.isAttributeValid(channel)) {
						char degree = '\u00B0';
						// format the battery value to one decimal place
						String temp = getDecimalFromString(val, 1)+degree+"C";
                        if (temperature!=null) temperature.setText(temp);
                        tempSet = true;
					} else if (channel.equals("presence") && device.isAttributeValid(channel)) {
						if (val.equals("True")) {
							if (presicon!=null) presicon.setImageResource(R.drawable.ic_keyfob_present);
							if (presence!=null) presence.setText(activity.getString(R.string.devicedetail_present));	
							
						} else {
							if (presicon!=null) presicon.setImageResource(R.drawable.ic_keyfob_offline);
							if (presence!=null) presence.setText(activity.getString(R.string.devicedetail_notpresent));	
						}
						presenceSet = true;
					} else if (device.type == Device.POWER_CONTROLLER && channel.equals("relaystate")) {
						TextView controlText = (TextView) deviceDialog.findViewById(R.id.device_data_control);
						Button powerButton = (Button) deviceDialog.findViewById(R.id.device_data_control_icon);
						if (val.equals("True")) {
							if (controlText!=null) controlText.setText(activity.getString(R.string.devicedetail_control_ison));
							if (powerButton!=null) powerButton.setBackgroundResource(R.drawable.ic_device_power_on);
						} else {
							if (controlText!=null) controlText.setText(activity.getString(R.string.devicedetail_control_isoff));
							if (powerButton!=null) powerButton.setBackgroundResource(R.drawable.ic_device_power_off);
						}						
						if (powerButton!=null) {
							if (deviceControlListener!=null) {
								powerButton.setOnClickListener(deviceControlListener);
							} else {
								powerButton.setOnClickListener(new View.OnClickListener() {
									public void onClick(View view) {
										deviceDialog.dismiss();
									}
								});
							}
						}
						powerControlSet = true;
						// end relay state
					} else if (device.type == Device.POWERCLAMP && channel.equals("powerlevel")) {
						if (powerlevel!=null) powerlevel.setText(val+"W");
						if (powicon!=null) powicon.setImageResource(R.drawable.ic_sensor_powerlevel);
						powerLevelSet = true;
					} else if (device.type == Device.POWER_CONTROLLER && channel.equals("powerlevel")) {
						if (powerlevel!=null) powerlevel.setText(val+"W");
						if (powicon!=null) powicon.setImageResource(R.drawable.ic_sensor_power_plug);
						powerLevelSet = true;
					}
				}
				if (powerControlSet && presenceSet) {
					presenceSet = false; // don't show presence if state can be controlled
					if (presenceSection!=null) presenceSection.setVisibility(View.GONE);
					if (controlSection!=null) controlSection.setVisibility(View.VISIBLE);
				}
				
				/*
				
				SimpleAdapter attad;
				ListView attributes = (ListView) deviceDialog.findViewById(R.id.device_attributes);
				if (attributes!=null) {
					// Set the attributes into attlist and then use the simple adapter
					List<Map<String,?>> attlist = new LinkedList<Map<String,?>>();
					for (String attr: device.attributes.keySet()) {
						attlist.add(createDeviceAttribute(attr, device.attributes.get(attr)));
					}
					if (!attlist.isEmpty()) {
						attad = new SimpleAdapter(context, attlist, R.layout.alertme_sensors_dialogrow, DEVICEDIALOG_COLUMNS, DEVICEDIALOG_COLUMN_VALUES);
						attributes.setVisibility(View.VISIBLE);
						attributes.setAdapter(attad);						
					} else {
						ScrollView scroller = (ScrollView) deviceDialog.findViewById(R.id.device_attribute_body);
						if (scroller!=null) {
							scroller.setVisibility(View.GONE);
						}
					}
				} */
			}

			if (!batterySet) {
				if (batticon!=null) batticon.setVisibility(View.GONE);
				if (battery!=null) battery.setVisibility(View.GONE);
				if (battlabel!=null) battlabel.setVisibility(View.GONE);
			} else {
				if (batticon!=null) batticon.setVisibility(View.VISIBLE);
				if (battery!=null) battery.setVisibility(View.VISIBLE);
				if (battlabel!=null) battlabel.setVisibility(View.VISIBLE);
			}

			if (!signalSet) {
				if (sigicon!=null) sigicon.setVisibility(View.GONE);
				if (signal!=null) signal.setVisibility(View.GONE);
				if (siglabel!=null) siglabel.setVisibility(View.GONE);
			} else {
				if (sigicon!=null) sigicon.setVisibility(View.VISIBLE);
				if (signal!=null) signal.setVisibility(View.VISIBLE);
				if (siglabel!=null) siglabel.setVisibility(View.VISIBLE);
			}

			if (!tempSet) {
				if (temperature!=null) temperature.setVisibility(View.GONE);
				if (tempicon!=null) tempicon.setVisibility(View.GONE);
				if (templabel!=null) templabel.setVisibility(View.GONE);

			} else {
				if (temperature!=null) temperature.setVisibility(View.VISIBLE);
				if (tempicon!=null) tempicon.setVisibility(View.VISIBLE);
				if (templabel!=null) templabel.setVisibility(View.VISIBLE);
			}

			if (!presenceSet) {
				if (presicon!=null) presicon.setVisibility(View.GONE);
				if (presence!=null) presence.setVisibility(View.GONE);				
			} else {
				if (presicon!=null) presicon.setVisibility(View.VISIBLE);
				if (presence!=null) presence.setVisibility(View.VISIBLE);				
			}
			
			if (!powerLevelSet) {
				if (powerlevel!=null) powerlevel.setVisibility(View.GONE);
				if (powicon!=null) powicon.setVisibility(View.GONE);
				if (powlabel!=null) powlabel.setVisibility(View.GONE);				
			} else {
				if (powerlevel!=null) powerlevel.setVisibility(View.VISIBLE);
				if (powicon!=null) powicon.setVisibility(View.VISIBLE);
				if (powlabel!=null) powlabel.setVisibility(View.VISIBLE);								
			}
			//deviceDialog.setTitle(type+": "+device.name);
			deviceDialog.setTitle(device.name+" - "+type);
		}
    } 
	public void showDeviceInDialog(Device device) {
    	prepareDeviceInDialog(device);
		if (deviceDialog!=null) {
			TextView deviceName = (TextView) deviceDialog.findViewById(R.id.device_label);
			boolean showthis = deviceName!=null;
			
			//if (showthis) {  showDialog(DEVICE_DIALOG); }
			if (showthis) {
				viewDevice = device;
				showDialog(AMViewItems.DEVICE_DIALOG);
			}
			//showDialog(AMViewItems.DEVICE_DIALOG);
		} else {
			if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "showDeviceInDialog()   FAILED: deviceDialog is null");
		}
	}
    
	private String getDecimalFromString(String input, int places) {
		String res = "";
		if (input!=null) {
			res = input;
			if (input.contains(".")) {
				String endStr = "";
				int sz = input.length();
				int dec = input.indexOf(".");
				int lim = dec+places+1;
				if (places>0) {
					if (lim>dec) {
						if (lim<sz) {
							res = input.substring(0, lim);
						} else {
							while (lim>dec) {
								if (lim<sz) {
									res = input.substring(0, lim)+endStr;
									break;
								}
								endStr += "0";
								lim--;
							}
						}
					}
				} else if (places==0) {
					res = input.substring(0, dec);
				}
			}
			
		}
		return res;
	}
	
    protected void initSystemName() {    
    	if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "initSystemName()   START");
    	if (systemName==null && systemNameResourceId!=0) {
    		systemName = (TextView) activity.findViewById(systemNameResourceId);
    		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "initSystemName()  -- systemName is set :"+systemName);
    	}
    	if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "initSystemName()   END");
    }
    public void reloadSystemName() {
    	if (systemNameResourceId!=0) {
        	systemName=null;
        	initSystemName();
    	}
    }
    
    protected void initProgressDialog() {
    	if (progressDialog==null) {
    		progressDialog = new ProgressDialog(activity);
    	}
    }
	protected void initDeviceDialog() {
		if (deviceDialogRegistered && deviceDialog==null) {
			Button ok;
			deviceDialog = new Dialog(activity);
			deviceDialog.setTitle("");
			deviceDialog.setContentView(R.layout.alertme_device_dialog);
			ok = (Button) deviceDialog.findViewById(R.id.device_cancel);
			deviceDialog.setCancelable(true);
			if (ok!=null) {
				ok.setOnClickListener(new View.OnClickListener() {
					public void onClick(View view) {
						deviceDialog.dismiss();
					}
				});
			}
			/*
			Button ok;
			AlertDialog.Builder builder;
			Context context = activity.getApplicationContext();
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
			View layout = inflater.inflate(R.layout.alertme_device_dialog, (ViewGroup) activity.findViewById(R.id.layout_device_root));
			ok = (Button) layout.findViewById(R.id.device_cancel);
			if (ok!=null) {
				ok.setOnClickListener(new View.OnClickListener() {
					public void onClick(View view) {
						deviceDialog.dismiss();
					}
				});
			}

			builder = new AlertDialog.Builder(context);
			builder.setView(layout);
			builder.setCancelable(true);
			deviceDialog = builder.create();
			*/
		}
	}
	protected void initDismissDialog() {
		if (dismissDialogRegistered && dismissDialog==null) {
			AlertDialog.Builder builder = new AlertDialog.Builder(activity);
			builder.setMessage("")
				.setCancelable(false)
				.setPositiveButton(activity.getString(R.string.behaviour_dialog_dismiss), dismissAction);
			dismissDialog = builder.create();
		}
	}

	// assumes hubNamesChoiceList is set (and click)
    protected void initHubDialog() {
    	if (hubChoiceRegistered && hubChoiceDialog==null && hubChoiceListener!=null) {
    		if (hubNamesChoiceList!=null) {
            	AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            	builder.setTitle(activity.getString(R.string.hubchoice_select_title));
            	builder.setItems(hubNamesChoiceList, hubChoiceListener);
            	hubChoiceDialog = builder.create();
    		}

    	}
    }
    protected void editHubDialog(String[] items) {
    	if (hubChoiceRegistered && hubChoiceListener!=null) {
    		hubChoiceDialog=null;
    		if (items!=null) hubNamesChoiceList = items;
    		initHubDialog();
    	}
    }

    // assumes that accountNamesChoiceList is set
    protected void initAccountDialog() {
    	if (accountChoiceRegistered && accountChoiceDialog==null && accountChoiceListener!=null) {
    		if (accountNamesChoiceList!=null) {
            	AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            	builder.setTitle(activity.getString(R.string.accountchoice_select_title));
            	builder.setItems(accountNamesChoiceList, accountChoiceListener);
            	accountChoiceDialog = builder.create();
    		}

    	}
    }
    protected void editAccountDialog(String[] items) {
    	if (accountChoiceRegistered && accountChoiceListener!=null) {
        	accountChoiceDialog=null;
        	if (items!=null) accountNamesChoiceList = items;
        	initAccountDialog();
    	}
    }
    
    protected void initQuitDialog() {
    	if (quitDialogRegistered && quitDialog==null && quitListener!=null) {
    		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
    		builder.setMessage(activity.getString(R.string.quit_dialog_title))
    			.setPositiveButton(activity.getString(R.string.quit_dialog_ok), quitListener);
    		if (quitCancelListener!=null) {
    			builder.setCancelable(true).setNegativeButton(activity.getString(R.string.quit_dialog_cancel), quitCancelListener);
    		}
    		quitDialog = builder.create();
    	}
    }
	protected void initInfoDialog() {
		if (infoDialogRegistered && infoDialog==null) {
			//Context context = activity.getApplicationContext();
			LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
			View layout = inflater.inflate(R.layout.alertme_info, (ViewGroup) activity.findViewById(R.id.layout_info_root));

			AlertDialog.Builder builder = new AlertDialog.Builder(activity);
			builder.setView(layout)
				.setCancelable(false)
				.setPositiveButton(activity.getString(R.string.infomation_dialog_dismiss), infoListener);
			infoDialog = builder.create();
		}
	}
	public static int[] getRowColours(Activity act) {
		Resources resource = act.getResources();
		int[] res = new int[] { resource.getColor(R.color.menu_even), resource.getColor(R.color.menu_odd) };
		return res;
	}
	
    private void setBusy(final String title, final String message) {
    	if (progressDialog!=null) {
    		if (title!=null && title.length()!=0) { progressDialog.setMessage(title); }
    		if (message!=null && message.length()!=0) { progressDialog.setMessage(message); }
    		if (!isBusy) {
        		isBusy = true;
        		lockScreenRotation();
        		showDialog(PROGRESS_DIALOG);
        		//progressDialog.show(this, title, message);
    		}
    	}
    }

    private void lockScreenRotation()
    {
    	if (activity!=null) {
    		// Stop the screen orientation changing during an event
    		switch (activity.getResources().getConfiguration().orientation) {
				case Configuration.ORIENTATION_PORTRAIT:
					activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
					break;
				case Configuration.ORIENTATION_LANDSCAPE:
					activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
					break;
    		}
    	}
    }
    
    private void unlockFixedScreenRotation() {
    	if (activity!=null) activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }
    
    /*
     *
     // Never used
	private Map<String,?> createDeviceAttribute(String title, String caption) {
		Map<String,String> item = new HashMap<String,String>();
		item.put(DEVICEDIALOG_NAME, title);
		item.put(DEVICEDIALOG_TYPE, caption);
		return item;
	}
	private static long getLongFromString(String s) {
		long res = -1;
		
		try {
			Long rawRes = new Long(s);
			res = rawRes.longValue();
		} catch (Exception e) {}
		
		return res;
	}*/

    private int currentDialog = -1;
    private boolean[] createdDialogs = {false, false, false, false, false, false, false};
    
	private TextView systemName = null;
	private int systemNameResourceId = 0;
	
	public final static int PROGRESS_DIALOG = 0;
	protected ProgressDialog progressDialog = null;
	protected boolean isBusy = false;
	
	public final static int DEVICE_DIALOG = 1;
	protected Dialog deviceDialog = null;
	public final static String DEVICEDIALOG_NAME = "title";
	public final static String DEVICEDIALOG_TYPE = "caption";
	public final static String[] DEVICEDIALOG_COLUMNS = new String[] { DEVICEDIALOG_NAME, DEVICEDIALOG_TYPE };
	public final static int[] DEVICEDIALOG_COLUMN_VALUES = new int[] { R.id.device_dialogline_attribute, R.id.device_dialogline_attribute_value };
	private View.OnClickListener deviceControlListener = null;
	private boolean deviceDialogRegistered = false;
	
	public final static int DISMISS_DIALOG = 2;
	protected AlertDialog dismissDialog = null;
	protected DialogInterface.OnClickListener dismissAction = null;
	private boolean dismissDialogRegistered = false;
	
	public final static int HUB_CHOICE_DIALOG = 3;
	private AlertDialog hubChoiceDialog = null;
	private DialogInterface.OnClickListener hubChoiceListener = null;
	private String[] hubNamesChoiceList = null;
	private boolean hubChoiceRegistered = false;
	
	public final static int ACCOUNT_CHOICE_DIALOG = 4;
	private AlertDialog accountChoiceDialog = null;
	private DialogInterface.OnClickListener accountChoiceListener = null;
	private String[] accountNamesChoiceList = null;
	private boolean accountChoiceRegistered = false;
	
	public final static int QUIT_DIALOG = 5;
	private AlertDialog quitDialog = null;
	private DialogInterface.OnClickListener quitListener = null;
	private DialogInterface.OnClickListener quitCancelListener = null;
	private boolean quitDialogRegistered = false;
	
	public final static int INFO_DIALOG = 6;
	private AlertDialog infoDialog = null;
	private DialogInterface.OnClickListener infoListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
            dialog.cancel();
       }
	};
	private boolean infoDialogRegistered = false;
	
	protected boolean isActive = false;
}