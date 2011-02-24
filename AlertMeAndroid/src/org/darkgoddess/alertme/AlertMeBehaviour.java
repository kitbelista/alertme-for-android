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
import android.widget.TextView;


public class AlertMeBehaviour extends Activity {
	private static final String TAG = "ACTIVITY:AlertMeBehaviour";
	private final String OFFLINE = "unavailable";
	private final String MHOME = "home";
	private final String MAWAY = "away";
	private final String MNIGHT = "night";
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
            int mesgType = msg.getData().getInt("type");
            String mesgData = msg.getData().getString("value");
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
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onRetainNonConfigurationInstance()  END");
		return saveState;
	}
	@Override
	public void finish() {
		isActive = false;
		alertMe.clean();
		super.finish();
		
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
					setAwayAction(awayButton);
					setNightAction(nightButton);
					setCancelAction(cancelHomeButton);
					if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "setView()  currentView is HOME");
					break;
				case R.layout.alertme_behave_armed:
					Button homeButton = (Button) findViewById(R.id.behavehome_button_arm);
					Button cancelArmButton = (Button) findViewById(R.id.behavearmed_cancel);
					ImageView currentModeImg = (ImageView) findViewById(R.id.behavearmed_currentmode_icon);
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
    	    	if (mesgData.contains(",")) {
    	    		String[] spl = mesgData.split(",");
    	    		if (spl.length>=2) {
        	    		ret = spl[0];
        	    		//hubID = spl[1];
    	    		}
    	    	}
    	    	loadCurrentHub(alertMe);
    			screenStuff.reloadSystemName();
    			screenStuff.setSystemName(currentHub);
    	    	ret = ret.toLowerCase();
				// do nothing, expect the handler action to invoke set view
				if (ret.equals(OFFLINE)) {
					if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  case UPDATE_ON_START  -- view is alertme_behave_offline");
					currentView = R.layout.alertme_behave_offline;
				} else if (ret.equals(MHOME)) {
					if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  case UPDATE_ON_START  -- view is alertme_behave_home");
					currentView = R.layout.alertme_behave_home;					
				} else if (ret.equals(MAWAY)) {
					if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  case UPDATE_ON_START  -- view is alertme_behave_armed (away)");
					currentView = R.layout.alertme_behave_armed;					
				} else if (ret.equals(MNIGHT)) {
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
				ret = ret.toUpperCase();
				int mesgResource = R.string.behaviour_dialog_message_offline;
				if (command == AlertMeConstants.COMMAND_STATUS_AWAY) {
					if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  case COMMAND_STATUS_AWAY  returning: "+ret+" with command "+command);
					if (ret.equals("AWAY_OK")) {
						returnCode = AlertMeConstants.COMMAND_STATUS_AWAY;
						mesgResource = R.string.behaviour_dialog_message_away_ok;
					} else {
						mesgResource = R.string.behaviour_dialog_message_away_fail;
					}
				} else if (command == AlertMeConstants.COMMAND_STATUS_NIGHT) {
					if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  case COMMAND_STATUS_NIGHT  returning: "+ret+" with command "+command);
					if (ret.equals("NIGHT_OK")) {
						returnCode = AlertMeConstants.COMMAND_STATUS_NIGHT;
						mesgResource = R.string.behaviour_dialog_message_night_ok;
					} else {
						mesgResource = R.string.behaviour_dialog_message_night_fail;
					}
				} else {
					if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  case COMMAND_STATUS_HOME  returning: "+ret+" with command "+command);
					if (ret.equals("HOME_OK")) {
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
			if (behaviour.equals(MHOME)) {
				currentView = R.layout.alertme_behave_home;					
			} else if (behaviour.equals(MAWAY)) {
				currentView = R.layout.alertme_behave_armed;					
			} else if (behaviour.equals(MNIGHT)) {
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
    			bCurrentHub = alertme.getActiveHub();
    			alertme.loadActiveHub();
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
	    				data = behaviour+","+hubID;
	    				break;
	    			case AlertMeConstants.COMMAND_STATUS_AWAY:
	    			case AlertMeConstants.COMMAND_STATUS_HOME:
	    			case AlertMeConstants.COMMAND_STATUS_NIGHT:
	    				messageEnd = instruction;
	    				data = getBehaviourChangeActions(instruction);
	    				break;
	    			case AlertMeConstants.INVOKE_STATUS_CONFIRM:
	    				messageEnd = AlertMeConstants.INVOKE_STATUS_CONFIRM;
	    		}
    		}

    		if (handler!=null) {
    			Message msg = handler.obtainMessage();
                Bundle b = new Bundle();
                b.putInt("type", messageEnd);
                if (data!=null) { b.putString("value", data); }
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
				res += (ok)? "_OK" : "_COMMAND_FAILED";
			}
			
			return res;
		}
	}
	
}
