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


import org.darkgoddess.alertme.api.AlertMeSession;
import org.darkgoddess.alertme.api.utils.Hub;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


public class AlertMeBehaviour extends Activity {
	private static final String TAG = "ACTIVITY:AlertMeBehaviour";
	private final String OFFLINE = "unavailable";
	private final String COMMAND_APPEND_OK = "_OK";
	private final String COMMAND_APPEND_FAIL = "_COMMAND_FAILED";
	private final String MHOME = AlertMeConstants.STR_HOME;
	private final String MAWAY = AlertMeConstants.STR_AWAY;
	private final String MNIGHT = AlertMeConstants.STR_NIGHT;
	private String currentIntruderState = null;
	private String currentEmergencyState = null;
	private Bundle savedState = null;
	private AMViewItems screenStuff = null;
	private AlertMeSession alertMe = null;
	private String behaviour = null;
	private Hub currentHub = null;
	private boolean hasSetView = false;
	private boolean isActive = false;
	private int returnCode = AlertMeConstants.COMMAND_STATUS_CANCEL;
	private int currentView = R.layout.alertme_behave_armed;
	
    // Handler to update the interface..        
	final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            int mesgType = msg.getData().getInt(AlertMeConstants.HANDLER_DATA_TYPE);
            String mesgData = msg.getData().getString(AlertMeConstants.HANDLER_DATA_VALUE);
            performUpdate(mesgType, mesgData);
        }
    };
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onCreate()   START");
		alertMe = (alertMe==null)? new AlertMeSession(this): alertMe;
		loadFromRestoredState();
		savedState = savedInstanceState;
		isActive = true;
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onCreate()   END");
	}
	@Override
    public void onStart() {
		super.onStart();

		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onStart()   START");
		loadFromRestoredState();
		initScreenStuff();
		initView(savedState);
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onStart()   END");
	}
	@Override
	protected Dialog onCreateDialog(int id) {
    	Dialog res = screenStuff.onCreateDialog(id);
        return res;
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
	public Object onRetainNonConfigurationInstance() {
		BehaviourState saveState = new BehaviourState();
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onRetainNonConfigurationInstance()  START");
		saveState.sessionState = alertMe.retrieveCurrentState();
		saveState.currentHub = currentHub;
		saveState.behaviour = behaviour;
		saveState.currentView = currentView;
		saveState.currentIntruderState = currentIntruderState;
		saveState.currentEmergencyState = currentEmergencyState;
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onRetainNonConfigurationInstance()  END");
		return saveState;
	}
	@Override
	public void finish() {
		isActive = false;
		alertMe.clean();
		super.finish();
		
	}
	private void setViewWarnings(TextView emergency, TextView intruder, TextView title, LinearLayout section) {
		boolean hasAlarmed = false;
		if (emergency!=null) {
			Button emerOff = (Button) findViewById(R.id.behavehome_button_stop_emergency);
			boolean setVis = false;
			int visibility = View.GONE;
			if (AlertMeConstants.isStateAlarmed(currentEmergencyState)) {
				setVis = true;
				hasAlarmed = true;
				if (emerOff!=null) {
					emerOff.setOnClickListener(emergencyStopListener);
				}
			}
			visibility = (setVis)? View.VISIBLE: View.GONE;
			emergency.setVisibility(visibility);
			if (emerOff!=null) emerOff.setVisibility(visibility);
		}
		if (intruder!=null) {
			Button alarmOff = (Button) findViewById(R.id.behavehome_button_stop_alarm);
			boolean setVis = false;
			int visibility = View.GONE;
			if (AlertMeConstants.isStateAlarmed(currentIntruderState)) {
				setVis = true;
				hasAlarmed = true;
				if (alarmOff!=null) {
					alarmOff.setOnClickListener(intruderStopListener);
				}
			}
			visibility = (setVis)? View.VISIBLE: View.GONE;
			intruder.setVisibility(visibility);
			if (alarmOff!=null) alarmOff.setVisibility(visibility);
		}
		if (section!=null) {
			int visibility = (hasAlarmed)? View.VISIBLE: View.GONE;
			section.setVisibility(visibility);
		}
		if (title!=null) {
			int visibility = (hasAlarmed)? View.VISIBLE: View.GONE;
			title.setVisibility(visibility);
		}
	}
	private void setWarningsOnCurrentView() {
		if (currentView==R.layout.alertme_behave_home) {
			TextView hemergency = (TextView) findViewById(R.id.behavehome_text_hubstate_emergency);
			TextView hintruder = (TextView) findViewById(R.id.behavehome_text_hubstate_intruder);
			TextView halarmtitle = (TextView) findViewById(R.id.behavearmed_text_alarmed_title);
			LinearLayout halarmsection = (LinearLayout) findViewById(R.id.behavearmed_text_alarmed_section);					
			setViewWarnings(hemergency, hintruder, halarmtitle, halarmsection);
		} else if (currentView==R.layout.alertme_behave_armed) {
			TextView aemergency = (TextView) findViewById(R.id.behavehome_text_hubstate_emergency);
			TextView aintruder = (TextView) findViewById(R.id.behavehome_text_hubstate_intruder);
			TextView alarmtitle = (TextView) findViewById(R.id.behavearmed_text_alarmed_title);
			LinearLayout alarmsection = (LinearLayout) findViewById(R.id.behavearmed_text_alarmed_section);
			setViewWarnings(aemergency, aintruder, alarmtitle, alarmsection);
		}
	}
	private void setViewOnCurrentView() {
		if (!hasSetView) {
			hasSetView = true;
			String currentMode = (behaviour!=null)? behaviour.trim().toLowerCase(): OFFLINE;
			initScreenStuff();
			currentHub = (currentHub!=null)? currentHub: alertMe.retrieveActiveHub();
			setContentView(currentView);
			screenStuff.reloadSystemName();
			screenStuff.setSystemName(currentHub);
			switch(currentView) {
				case R.layout.alertme_behave_home:
					Button awayButton = (Button) findViewById(R.id.behavehome_button_arm);
					Button nightButton = (Button) findViewById(R.id.behavehome_button_night);
					Button cancelHomeButton = (Button) findViewById(R.id.behavehome_cancel);				
					setWarningsOnCurrentView();
					setAwayAction(awayButton);
					setNightAction(nightButton);
					setCancelAction(cancelHomeButton);
					if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "setView()  currentView is HOME");
					break;
				case R.layout.alertme_behave_armed:
					Button homeButton = (Button) findViewById(R.id.behavehome_button_arm);
					Button cancelArmButton = (Button) findViewById(R.id.behavearmed_cancel);
					ImageView currentModeImg = (ImageView) findViewById(R.id.behavearmed_currentmode_icon);
					setWarningsOnCurrentView();
					if (behaviour!=null) {
						TextView message = (TextView) findViewById(R.id.behavearmed_text_current);
						if (currentMode.equals(MNIGHT)) {
							if (message!=null) {
								message.setText(R.string.behaviour_current_is_night);
								if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "setView()  currentView is ARMED: night");
							}
						} else {
							if (message!=null) message.setText(R.string.behaviour_current_is_away);
							if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "setView()  currentView is ARMED: away");
						}
					}
					if (currentModeImg!=null) {
						if (currentMode.equals(MNIGHT)) {
							currentModeImg.setImageResource(R.drawable.ic_home_status_night_mini);
						} else {
							currentModeImg.setImageResource(R.drawable.ic_home_status_lock_mini);
						}
					}
					setHomeAction(homeButton);
					setCancelAction(cancelArmButton);
					break;
				case R.layout.alertme_behave_offline:
					Button cancelOffButton = (Button) findViewById(R.id.behaveoffline_cancel);
					setCancelAction(cancelOffButton);
					if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "setView()  currentView is OFFLINE");
					break;
			}
			
		}
	}
	private void setView() {
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "setView()  START");
		initScreenStuff();
		if (!hasSetView) {
			currentHub = (currentHub!=null)? currentHub: alertMe.retrieveActiveHub();
			if (currentHub==null || currentHub!=null && !alertMe.isCurrentHubActive()) {
				currentView = R.layout.alertme_behave_offline;
			}
			setViewOnCurrentView();
		}
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "setView()  END");
	}
	private void loadCurrentHub(AlertMeSession alertme) {
		if (currentHub==null) { currentHub = alertMe.retrieveActiveHub(); }
		if (currentHub==null) {
			long cID = alertMe.retrieveActiveHubID();
			if (cID!=-1) {
				currentHub = alertMe.retrieveHubByDBID(cID);
			}
		}
	}
	private void exitOnSuccess() {
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "exitOnSuccess()  START -- resultCode: "+returnCode);
    	Intent intent = alertMe.createIntentFromSession();
    	intent.putExtra(AlertMeConstants.INTENT_RETURN_KEY, returnCode);
    	if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "exitOnSuccess()  END");
    	setResult(RESULT_OK, intent);
		finish();
	}
	private void cancelBehaviour() {
    	Intent intent = alertMe.createIntentFromSession();
    	intent.putExtra(AlertMeConstants.INTENT_RETURN_KEY, AlertMeConstants.COMMAND_STATUS_CANCEL);		
		finish();
	}
	private void setCancelAction(Button button) {
		if (button!=null) {
			button.setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					cancelBehaviour();
				}
			});
		}
	}
	private void initScreenStuff() {
		if (screenStuff==null) {
			screenStuff = new AMViewItems(this, this);
			screenStuff.registerSystemName(R.id.behavehome_housename);
			screenStuff.registerDismissDialog(new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					exitOnSuccess();
				}
			});
			screenStuff.initDismissDialog();
		}
	}
	private void setAwayAction(Button button) {
		if (button!=null) {
			button.setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					BehaviourStarter startThread;
					startThread = new BehaviourStarter(alertMe, handler, getIntent(), null, null, AlertMeConstants.COMMAND_STATUS_AWAY);
					screenStuff.setBusy(AlertMeConstants.COMMAND_STATUS_AWAY);
					startThread.start();
				}
			});
		}
	}
	private void setNightAction(Button button) {
		if (button!=null) {
			button.setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					BehaviourStarter startThread;
					startThread = new BehaviourStarter(alertMe, handler, getIntent(), null, null, AlertMeConstants.COMMAND_STATUS_NIGHT);
					screenStuff.setBusy(AlertMeConstants.COMMAND_STATUS_NIGHT);
					startThread.start();
				}
			});
		}
	}
	private void setHomeAction(Button button) {
		if (button!=null) {
			button.setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					BehaviourStarter startThread;
					startThread = new BehaviourStarter(alertMe, handler, getIntent(), null, null, AlertMeConstants.COMMAND_STATUS_HOME);
					screenStuff.setBusy(AlertMeConstants.COMMAND_STATUS_HOME);
					startThread.start();
				}
			});
		}
	}
	private void initView(Bundle bundleIn) {
		SharedPreferences sharedPrefs = getSharedPreferences(AlertMeConstants.PREFERENCE_NAME, AlertMeConstants.PREFERENCE_MODE);
		BehaviourStarter startThread;
		currentView = R.layout.alertme_behave_offline;
		startThread = new BehaviourStarter(alertMe, handler, getIntent(), bundleIn, sharedPrefs, AlertMeConstants.UPDATE_ON_START);
		screenStuff.setBusy(AlertMeConstants.UPDATE_STATUS);
		startThread.start();
	}
    
    private void performUpdate(int command, final String mesgData) {
    	String ret = null;
    	if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate("+command+", '"+mesgData+"')  START");
    	if (!isActive) {
    		// Not active when onCreate is not done OR finish() is called
    		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()   END (premature - not active)");
    		screenStuff.setNotBusy();
    		return;
    	}
		ret = (mesgData!=null)? mesgData: "";
    	switch (command) {
    		case AlertMeConstants.IS_OFFLINE:
    			if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  case IS_OFFLINE");
    			behaviour = OFFLINE;
    			currentView = R.layout.alertme_behave_offline;
				setView();
    	    	screenStuff.setNotBusy();
    	    	screenStuff.setDismissMessage(getString(R.string.behaviour_dialog_message_offline));
    	    	screenStuff.showDialog(AMViewItems.DISMISS_DIALOG);
    			break;
    		case AlertMeConstants.INVOKE_STATUS_CONFIRM:
    			if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  case INVOKE_STATUS_CONFIRM");
    	    	screenStuff.setNotBusy();
    	    	exitOnSuccess();
    			break;
			case AlertMeConstants.UPDATE_ON_START:
				if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  case UPDATE_ON_START");
				//String hubID = null;
    	    	if (mesgData.contains(AlertMeConstants.STR_COMMA)) {
    	    		String[] spl = mesgData.split(AlertMeConstants.STR_COMMA);
    	    		if (spl.length>=2) {
        	    		ret = spl[0];
        	    		//hubID = spl[1];
    	    		}
    	    	}
    	    	loadCurrentHub(alertMe);
    			screenStuff.reloadSystemName();
    			screenStuff.setSystemName(currentHub);
    	    	// do nothing, expect the handler action to invoke set view
				if (ret.equals(OFFLINE)) {
					if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  case UPDATE_ON_START  -- view is alertme_behave_offline");
					currentView = R.layout.alertme_behave_offline;
				} else if (ret.equalsIgnoreCase(MHOME)) {
					if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  case UPDATE_ON_START  -- view is alertme_behave_home");
					currentView = R.layout.alertme_behave_home;					
				} else if (ret.equalsIgnoreCase(MAWAY)) {
					if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  case UPDATE_ON_START  -- view is alertme_behave_armed (away)");
					currentView = R.layout.alertme_behave_armed;					
				} else if (ret.equalsIgnoreCase(MNIGHT)) {
					if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  case UPDATE_ON_START  -- view is alertme_behave_armed (night)");
					currentView = R.layout.alertme_behave_armed;		
				} else {
					if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  case UPDATE_ON_START  -- view is alertme_behave_offline (default)");
					currentView = R.layout.alertme_behave_offline;
				}
    	    	screenStuff.setNotBusy();
				setView();
				break;
			case AlertMeConstants.COMMAND_STATUS_AWAY:
			case AlertMeConstants.COMMAND_STATUS_HOME:
			case AlertMeConstants.COMMAND_STATUS_NIGHT:
				// if mesgData indicates OK, then show dialog 
				int mesgResource = R.string.behaviour_dialog_message_offline;
				if (command == AlertMeConstants.COMMAND_STATUS_AWAY) {
					if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  case COMMAND_STATUS_AWAY  returning: "+ret+" with command "+command);
					if (ret.equalsIgnoreCase(AlertMeConstants.MODE_AWAY_OK)) {
						returnCode = AlertMeConstants.COMMAND_STATUS_AWAY;
						mesgResource = R.string.behaviour_dialog_message_away_ok;
					} else {
						mesgResource = R.string.behaviour_dialog_message_away_fail;
					}
				} else if (command == AlertMeConstants.COMMAND_STATUS_NIGHT) {
					if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  case COMMAND_STATUS_NIGHT  returning: "+ret+" with command "+command);
					if (ret.equalsIgnoreCase(AlertMeConstants.MODE_NIGHT_OK)) {
						returnCode = AlertMeConstants.COMMAND_STATUS_NIGHT;
						mesgResource = R.string.behaviour_dialog_message_night_ok;
					} else {
						mesgResource = R.string.behaviour_dialog_message_night_fail;
					}
				} else {
					if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  case COMMAND_STATUS_HOME  returning: "+ret+" with command "+command);
					if (ret.equalsIgnoreCase(AlertMeConstants.MODE_HOME_OK)) {
						returnCode = AlertMeConstants.COMMAND_STATUS_HOME;
						mesgResource = R.string.behaviour_dialog_message_home_ok;
					} else {
						mesgResource = R.string.behaviour_dialog_message_home_fail;
					}
				}
    	    	screenStuff.setNotBusy();
    	    	screenStuff.setDismissMessage(getString(mesgResource));
    	    	screenStuff.showDialog(AMViewItems.DISMISS_DIALOG);
				break;
			case AlertMeConstants.COMMAND_STATUS_STOPALARM_OK:
				setWarningsOnCurrentView(); // should remove the sections affected
		    	screenStuff.setNotBusy();
        		Toast.makeText(getApplicationContext(), getString(R.string.alarm_stopped_ok), Toast.LENGTH_SHORT).show();
				break;
			case AlertMeConstants.COMMAND_STATUS_STOPALARM_FAILED:
		    	screenStuff.setNotBusy();
        		Toast.makeText(getApplicationContext(), getString(R.string.alarm_stopped_fail), Toast.LENGTH_SHORT).show();
				break;
    	}
    	screenStuff.setNotBusy();
    	if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  END");
    }
	private void loadFromRestoredState() {
		final Object data = getLastNonConfigurationInstance();
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "loadFromRestoredState()  START");
		if (data != null) {
			BehaviourState saveState = (BehaviourState) data;
			final AlertMeSession.SessionState oldState = saveState.sessionState;
			boolean reloaded = false;
			if (alertMe==null) {
				alertMe = new AlertMeSession(this);
			}
			if (saveState.currentHub!=null) {
				if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "loadFromRestoredState()  currentHub RESTORED");
				currentHub = saveState.currentHub;
			}
			if (saveState.currentView!=0) currentView = saveState.currentView;
			if (saveState.behaviour!=null) behaviour = saveState.behaviour;
			if (saveState.currentIntruderState!=null) currentIntruderState = saveState.currentIntruderState;
			if (saveState.currentEmergencyState!=null) currentEmergencyState = saveState.currentEmergencyState;
			
			reloaded = alertMe.loadFromCachedState(this, oldState);
			if (reloaded) {
				restoreDetailsFromCache();
			}
			if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "loadFromRestoredState()  did reload from old state:"+reloaded);
		}
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "loadFromRestoredState()  END");
	}
	private void restoreDetailsFromCache() {
		if (alertMe!=null) loadCurrentHub(alertMe);
		if (behaviour==null) behaviour = (currentHub!=null)? currentHub.behaviour: OFFLINE;
		if (behaviour!=null) {
			behaviour = behaviour.trim().toLowerCase();
			if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "restoreDetailsFromCache()  behaviour ["+behaviour+"]");
			if (behaviour.equalsIgnoreCase(MHOME)) {
				currentView = R.layout.alertme_behave_home;					
			} else if (behaviour.equalsIgnoreCase(MAWAY)) {
				currentView = R.layout.alertme_behave_armed;					
			} else if (behaviour.equalsIgnoreCase(MNIGHT)) {
				currentView = R.layout.alertme_behave_armed;					
			} else {
				currentView = R.layout.alertme_behave_offline;
			}
		}
		setViewOnCurrentView();
	}
    
	class BehaviourState {
		public AlertMeSession.SessionState sessionState = null;
		public Hub currentHub = null;
		public String behaviour = null;
		public int currentView = 0;
		public String currentIntruderState = null;
		public String currentEmergencyState = null;
	}

	class BehaviourStarter extends Thread {
		public boolean forceLogin = true;
		public int instruction = -1;
		private AlertMeSession alertme;
		private Handler handler;
		private Intent intent;
		private Bundle bundle;
		private SharedPreferences sharedPrefs;

		public BehaviourStarter(AlertMeSession client, Handler handle, Intent intentIn, Bundle bundleIn, SharedPreferences prefs) {
			alertme = client;
			handler = handle;
			intent = intentIn;
			bundle = bundleIn;
			sharedPrefs = prefs;
		}
		public BehaviourStarter(AlertMeSession client, Handler handle, Intent intentIn, Bundle bundleIn, SharedPreferences prefs, int instruct) {
			alertme = client;
			handler = handle;
			intent = intentIn;
			bundle = bundleIn;
			sharedPrefs = prefs;
			instruction = instruct;
		}

        @Override
        public void run() {
        	int messageEnd = instruction;
      		boolean hasCurrentSys = alertme.hasSessionValues();
      		Hub bCurrentHub = null;
    		if (!hasCurrentSys) hasCurrentSys = alertme.loadFromIntentBundle(intent, bundle);
    		if (!hasCurrentSys) hasCurrentSys = alertme.loadFromPreference(sharedPrefs);

        	String data = null;
    		if (hasCurrentSys) {
    			boolean loaded = false;
    			bCurrentHub = alertme.getActiveHub();
    			loaded = alertme.loadActiveHub();
    			if (currentIntruderState==null && loaded) currentIntruderState = alertme.getActiveServiceState(AlertMeSession.SERVICE_INTRUDER_ALARM);
    			if (currentEmergencyState==null && loaded) currentEmergencyState = alertme.getActiveServiceState(AlertMeSession.SERVICE_EMERGENCY_ALARM);
    		}
    		
    		if (bCurrentHub==null) {
    			messageEnd = AlertMeConstants.IS_OFFLINE;
    			data = OFFLINE;
    		} else {
    			currentHub = bCurrentHub;
    			loadCurrentHub(alertme);
        		switch (instruction) {
	    			case AlertMeConstants.UPDATE_ON_START:
	    				String hubID = null;
	    				behaviour = (currentHub!=null)? currentHub.behaviour: OFFLINE;
	    				hubID = (currentHub!=null)? currentHub.id: null;
	    				messageEnd = instruction;
	    				data = behaviour+AlertMeConstants.STR_COMMA+hubID;
	    				break;
	    			case AlertMeConstants.COMMAND_STATUS_AWAY:
	    			case AlertMeConstants.COMMAND_STATUS_HOME:
	    			case AlertMeConstants.COMMAND_STATUS_NIGHT:
	    				messageEnd = instruction;
	    				data = getBehaviourChangeActions(instruction);
	    				break;
	    			case AlertMeConstants.INVOKE_STATUS_CONFIRM:
	    				messageEnd = AlertMeConstants.INVOKE_STATUS_CONFIRM;
	    			case AlertMeConstants.COMMAND_STATUS_STOPALARM:
	    				boolean intruderStopOk = alertme.stopAlarm(AlertMeSession.SERVICE_INTRUDER_ALARM);
	    				if (intruderStopOk) {
	    					currentIntruderState = alertme.getActiveServiceState(AlertMeSession.SERVICE_INTRUDER_ALARM);
	    					messageEnd = (AlertMeConstants.isStateAlarmed(currentIntruderState))? AlertMeConstants.COMMAND_STATUS_STOPALARM_FAILED: AlertMeConstants.COMMAND_STATUS_STOPALARM_OK;
	    				} else {
	    					messageEnd = AlertMeConstants.COMMAND_STATUS_STOPALARM_FAILED;
	    				}
	    			case AlertMeConstants.COMMAND_STATUS_STOPEMERGENCY:
	    				boolean emergencyStopOk = alertme.stopAlarm(AlertMeSession.SERVICE_EMERGENCY_ALARM);
	    				if (emergencyStopOk) {
	    					currentEmergencyState = alertme.getActiveServiceState(AlertMeSession.SERVICE_EMERGENCY_ALARM);
	    					messageEnd = (AlertMeConstants.isStateAlarmed(currentEmergencyState))? AlertMeConstants.COMMAND_STATUS_STOPALARM_FAILED: AlertMeConstants.COMMAND_STATUS_STOPALARM_OK;
	    				} else {
	    					messageEnd = AlertMeConstants.COMMAND_STATUS_STOPALARM_FAILED;
	    				}	    				
	    				break;
	    		}
    		}

    		if (handler!=null) {
    			Message msg = handler.obtainMessage();
                Bundle b = new Bundle();
                b.putInt(AlertMeConstants.HANDLER_DATA_TYPE, messageEnd);
                if (data!=null) { b.putString(AlertMeConstants.HANDLER_DATA_VALUE, data); }
                msg.setData(b);
                handler.sendMessage(msg);
    		}        		
        }
		private String getBehaviourChangeActions(int command) {
			String res = null;
			int mode = -1;
			String currentMode = behaviour.toLowerCase();
			if (currentMode.equals(MHOME)) {
				if (command==AlertMeConstants.COMMAND_STATUS_AWAY) {
					res = MAWAY;
					mode = AlertMeSession.MODE_ARM;
				} else if (command==AlertMeConstants.COMMAND_STATUS_NIGHT) {
					res = MNIGHT;
					mode = AlertMeSession.MODE_NIGHT;
				}
			} else {
				if (command==AlertMeConstants.COMMAND_STATUS_HOME) {
					res = MHOME;	
					mode = AlertMeSession.MODE_HOME;
				}
			}
			if (res!=null && mode!=-1) {
				// need to invoke the action
				boolean ok = alertme.setHubMode(mode);
				// if ok, append _OK else _FAIL
				res += (ok)? COMMAND_APPEND_OK : COMMAND_APPEND_FAIL;
			}
			
			return res;
		}
	}
	
	private final View.OnClickListener emergencyStopListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (screenStuff!=null) {
				BehaviourStarter startThread;
				startThread = new BehaviourStarter(alertMe, handler, getIntent(), null, null, AlertMeConstants.COMMAND_STATUS_STOPEMERGENCY);
				screenStuff.setBusy(AlertMeConstants.COMMAND_STATUS_STOPEMERGENCY);
				startThread.start();
			}
		}
	};
	private final View.OnClickListener intruderStopListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (screenStuff!=null) {
				BehaviourStarter startThread;
				startThread = new BehaviourStarter(alertMe, handler, getIntent(), null, null, AlertMeConstants.COMMAND_STATUS_STOPALARM);
				screenStuff.setBusy(AlertMeConstants.COMMAND_STATUS_STOPALARM);
				startThread.start();
			}
		}
	};
}
