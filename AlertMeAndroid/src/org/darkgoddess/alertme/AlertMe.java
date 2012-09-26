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
import java.util.Collections;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import org.darkgoddess.alertme.api.AlertMeSession;
import org.darkgoddess.alertme.api.AlertMeServer;
import org.darkgoddess.alertme.api.AlertMeStorage;
import org.darkgoddess.alertme.api.utils.APIUtilities;
import org.darkgoddess.alertme.api.utils.Device;
import org.darkgoddess.alertme.api.utils.Hub;

public class AlertMe extends Activity {
	private static final String TAG = "ACTIVITY:AlertMe";
	private Bundle savedState = null;
	private AMViewItems screenStuff = null;
	
	private boolean isActive = false;
	private boolean hasCreated = false;
	private AlertMeSession alertMe = null;
	
	private ImageView topIcon = null;
	//private TextView statusText = null;
	private Button statusButton = null;
	private ImageView statusAlarm = null;
	//private TextView peopleText = null;
	private Button peopleButton = null;
	private TextView peopleTextCount = null;
	//private TextView sensorsText = null;
	private Button sensorsButton = null;
	private Button historyButton = null;
	private Button helpButton = null;
	private String currentIntruderState = null;
	private String currentEmergencyState = null;

	private boolean hubChoiceActive = false;
	private String[] hubNamesChoiceList = null;
	private String[] hubIDsChoiceList = null;
	private String[] accountNamesChoiceList = null;
	private long[] accountIDsChoiceList = null;
	
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
		ArrayList<AlertMeStorage.AlertMeUser> accounts = null;
		boolean hasNoUsers = false;
		isActive = true;
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onCreate()  START");
		setContentView(R.layout.alertme_home);
		loadFromRestoredState();
		if (!hasCreated) {
			hasCreated = true;		
			initView();
			savedState = savedInstanceState;
			alertMe = (alertMe==null)? new AlertMeSession(this): alertMe;
			accounts = alertMe.getUserAccountList();
			hasNoUsers = (accounts==null || accounts!=null && accounts.isEmpty());
			if (hasNoUsers) {
				invokeAddNewSettingsFirst();	
			}
		}
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onCreate()  END");
	}
	@Override
    public void onStart() {
		super.onStart();

		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onStart()  START");
		loadFromRestoredState();
		if (screenStuff==null) {
			SharedPreferences sharedPrefs = getSharedPreferences(AlertMeConstants.PREFERENCE_NAME, AlertMeConstants.PREFERENCE_MODE);
			AlertStarter starter = null;
			
			screenStuff = new AMViewItems(this, this);
			screenStuff.registerSystemName(R.id.home_housename);
			screenStuff.registerHubChoiceDialog(hubClick, hubNamesChoiceList);
			screenStuff.registerAccountChoiceDialog(accountClick, accountNamesChoiceList);
			screenStuff.registerQuitDialog(quitClick, quitCancelClick);
			screenStuff.registerInfoDialog();
			screenStuff.initAccountDialog();
			screenStuff.initHubDialog();
			screenStuff.initProgressDialog();
			screenStuff.initQuitDialog();
			screenStuff.initInfoDialog();
			
			if (alertMe.requiresRefresh()) {
				starter = new AlertStarter(alertMe, handler, getIntent(), savedState, sharedPrefs, AlertMeConstants.UPDATE_ALL);
				screenStuff.setBusy(AlertMeConstants.UPDATE_ALL);
				starter.start(); // show the screen..
			} else {
				performUpdate(AlertMeConstants.UPDATE_ALL, null);
			}
		
		}
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onStart()  END");
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
	protected Dialog onCreateDialog(int id) {
    	Dialog res = screenStuff.onCreateDialog(id);
        return res;
    }
	@Override
	public Object onRetainNonConfigurationInstance() {
		AlertMeState saveState = new AlertMeState();
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onRetainNonConfigurationInstance()  START");
		saveState.session = alertMe.retrieveCurrentState();
		saveState.currentIntruderState = currentIntruderState;
		saveState.currentEmergencyState = currentEmergencyState;
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onRetainNonConfigurationInstance()  END");
		return saveState;
	}
	@Override
	public boolean onKeyDown(final int keyCode, final KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
			screenStuff.showDialog(AMViewItems.QUIT_DIALOG);
			return true;
		}	
		return super.onKeyDown(keyCode, event);
	}
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
		SharedPreferences sharedPrefs = getSharedPreferences(AlertMeConstants.PREFERENCE_NAME, AlertMeConstants.PREFERENCE_MODE);
    	boolean doUpdate = false;
    	int instruct = requestCode;
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onActivityResult("+requestCode+", "+resultCode+", intent)  START");
        // Check if settings have changed..
    	int cancelResource = 0;
    	int modeResult = -1;
    	if (intent!=null) {
    		modeResult = AlertMeConstants.getReturnKeycodeFromIntentBundle(intent, null, -1);
    	}
    	
    	if (screenStuff==null) {
    		screenStuff = new AMViewItems(this, this);
    		screenStuff.registerSystemName(R.id.home_housename);
    		screenStuff.registerHubChoiceDialog(hubClick, hubNamesChoiceList);
    		screenStuff.registerAccountChoiceDialog(accountClick, accountNamesChoiceList);
    		screenStuff.initAccountDialog();
    		screenStuff.initHubDialog();
    		screenStuff.initProgressDialog();
    	}
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onActivityResult()  modeResult: "+modeResult);
    	
        // refresh..
        alertMe = (alertMe!=null)? alertMe: new AlertMeSession(this);
        switch(requestCode) {
        	case AlertMeConstants.INVOKE_SETTINGS_CREATE:
        		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onActivityResult()  case INVOKE_SETTINGS_CREATE");
        		if (modeResult==AlertMeConstants.COMMAND_SETTINGS_CREATE_CONFIRM) {
        			doUpdate = true;
        		} else {
        			cancelResource = R.string.settings_update_canceled;
        		}
        		break;
        	case AlertMeConstants.INVOKE_SETTINGS_CREATE_FIRSTTIME:
        		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onActivityResult()  case INVOKE_SETTINGS_CREATE_FIRSTTIME");
        		if (modeResult==AlertMeConstants.COMMAND_SETTINGS_CREATE_CONFIRMFIRSTTIME) {
        			doUpdate = true;
        		} else {
        			cancelResource = R.string.settings_update_canceled;
        		}
        		break;
        	case AlertMeConstants.INVOKE_SETTINGS_EDIT:
        		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onActivityResult()  case INVOKE_SETTINGS_EDIT");
        		if (modeResult==AlertMeConstants.COMMAND_SETTINGS_CONFIRM) {
        			doUpdate = true;
        		} else {
        			cancelResource = R.string.settings_update_canceled;
        		}
        		break;
        	case AlertMeConstants.INVOKE_SENSORS:
        		doUpdate = true;
        		requestCode = AlertMeConstants.UPDATE_ALL;
        		instruct = AlertMeConstants.UPDATE_ALL;
        		break;
        	case AlertMeConstants.INVOKE_STATUS:
        		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onActivityResult()  case INVOKE_STATUS  -- behave:"+modeResult);
        		if (modeResult==AlertMeConstants.COMMAND_STATUS_AWAY) {
        			doUpdate = true;
        		} else if (modeResult==AlertMeConstants.COMMAND_STATUS_HOME) {
        			doUpdate = true;
        		} else if (modeResult==AlertMeConstants.COMMAND_STATUS_NIGHT) {
        			doUpdate = true;
        		} else if (modeResult==AlertMeConstants.INVOKE_STATUS_CONFIRM) {
					doUpdate = true;
				}
        		if (doUpdate) {
        			requestCode = AlertMeConstants.UPDATE_STATUS;
        		} else {
        			cancelResource = R.string.behaviour_update_canceled;
        		}
        		instruct = AlertMeConstants.UPDATE_STATUS;
        		break;        
        }

        if (doUpdate) {
        	AlertStarter starter = new AlertStarter(alertMe, handler, intent, null, sharedPrefs, instruct);
    		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onActivityResult()  ending with AlertMe refresh");
        	screenStuff.setBusy(requestCode);
			starter.start(); // show the screen..
        } else {
        	if (cancelResource!=0) {
        		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onActivityResult()  ending with cancel message");
        		Toast.makeText(getApplicationContext(), getString(cancelResource), Toast.LENGTH_SHORT).show();
        	}
        }
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onActivityResult()  END");
    }
    // For the 'settings' button to invoke TODO: include the DB id of this entry
    public void invokeBehaviour() {
		Intent i = alertMe.createIntentFromSession();
		i.setClass(this, AlertMeBehaviour.class);
        startActivityForResult(i, AlertMeConstants.INVOKE_STATUS);   
    }
    public void invokeEditCurrentSettings() {
    	if (alertMe.hasSessionValues()) {
    		Intent i = alertMe.createIntentFromSession();
    		i.setClass(this, AlertMeSettings.class);
        	i.putExtra(AlertMeConstants.INTENT_REQUEST_KEY, AlertMeConstants.INVOKE_SETTINGS_EDIT);
            startActivityForResult(i, AlertMeConstants.INVOKE_SETTINGS_EDIT);
    	} else {
    		invokeAddNewSettingsFirst(); // set the current screen to the result.. Hopefully
    	}
    }
    // For the menu to invoke
    public void invokeEvents() {
		Intent i = alertMe.createIntentFromSession();
		i.setClass(this, AlertMeEventHistory.class);
		i.putExtra(AlertMeConstants.INTENT_REQUEST_KEY, AlertMeConstants.INVOKE_HISTORY);
        startActivityForResult(i, AlertMeConstants.INVOKE_HISTORY);
    }
    public void invokePeople() {
		Intent i = alertMe.createIntentFromSession();
		i.setClass(this, AlertMePeople.class);
		i.putExtra(AlertMeConstants.INTENT_REQUEST_KEY, AlertMeConstants.INVOKE_PEOPLE);
        startActivityForResult(i, AlertMeConstants.INVOKE_PEOPLE);
    }
    public void invokeHelp() {
		Intent i = alertMe.createIntentFromSession();
		i.setClass(this, AlertMeHelp.class);
		i.putExtra(AlertMeConstants.INTENT_REQUEST_KEY, AlertMeConstants.INVOKE_HELP);
        startActivityForResult(i, AlertMeConstants.INVOKE_HELP);
    }
    public void invokeSensors() {
		Intent i = alertMe.createIntentFromSession();
		i.setClass(this, AlertMeSensors.class);
		i.putExtra(AlertMeConstants.INTENT_REQUEST_KEY, AlertMeConstants.INVOKE_SENSORS);
        startActivityForResult(i, AlertMeConstants.INVOKE_SENSORS);    	
    }
    public void invokeAddNewSettings() {
		Intent i = alertMe.createIntentFromSession();
		i.setClass(this, AlertMeSettings.class);
		i.putExtra(AlertMeConstants.INTENT_REQUEST_KEY, AlertMeConstants.INVOKE_SETTINGS_CREATE);
        startActivityForResult(i, AlertMeConstants.INVOKE_SETTINGS_CREATE);
    }
    public void invokeAddNewSettingsFirst() {
		Intent i = new Intent();
		i.setClass(this, AlertMeSettings.class);
    	//Intent i = new Intent(this, AlertMeSettings.class);
		i.putExtra(AlertMeConstants.INTENT_REQUEST_KEY, AlertMeConstants.INVOKE_SETTINGS_CREATE_FIRSTTIME);
        startActivityForResult(i, AlertMeConstants.INVOKE_SETTINGS_CREATE_FIRSTTIME);
    }
    private void invokeAccountSelection() {
		screenStuff.setNotBusy();
		screenStuff.showDialog(AMViewItems.ACCOUNT_CHOICE_DIALOG);
    }
    public void refreshAlertMe() {
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "refreshAlertMe()  START");
		SharedPreferences sharedPrefs = getSharedPreferences(AlertMeConstants.PREFERENCE_NAME, AlertMeConstants.PREFERENCE_MODE);
       	AlertStarter refreshThread = new AlertStarter(alertMe, handler, getIntent(), null, sharedPrefs);
       	screenStuff.setBusy(AlertMeConstants.UPDATE_ALL);
    	refreshThread.start();
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "refreshAlertMe()  END");
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.home, menu);
        return true;
    }
    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
    	MenuItem hubSelect = menu.findItem(R.id.menu_home_select_hub);
    	MenuItem userSelect = menu.findItem(R.id.menu_home_select_system);
		ArrayList<Hub> hubs = alertMe.getHubListSelection();
		int userSize = alertMe.retrieveUserCount();
		if (hubs!=null && !hubs.isEmpty()) {
			hubChoiceActive = true;
		}
    	if (hubSelect!=null) {
    		hubSelect.setVisible(hubChoiceActive);
    	}
    	if (userSelect!=null) {
    		userSelect.setVisible((userSize>1));
    	}
    	return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        	case R.id.menu_home_add_new_system:
        		invokeAddNewSettings();
        		return true;
        	case R.id.menu_home_select_hub:
        		ArrayList<Hub> hubs = alertMe.getHubListSelection();
        		if (hubs!=null && !hubs.isEmpty()) {
        			hubChoiceActive = true;
        		}
        		if (hubChoiceActive) {
        			AlertStarter starter = new AlertStarter(alertMe, handler, getIntent(), null, null, AlertMeConstants.INVOKE_HUB_CHOICE);
        			screenStuff.setBusy(AlertMeConstants.INVOKE_HUB_CHOICE);
        			starter.start(); // show the screen..
        			return true;
        		} else {
        			return super.onOptionsItemSelected(item);
        		}
        	case R.id.menu_home_select_system:
        		invokeAccountSelection();
    			return true;
        	case R.id.menu_home_refresh:
        		refreshAlertMe();
    			return true;
        	case R.id.menu_home_about:
        		screenStuff.showDialog(AMViewItems.INFO_DIALOG);
        		return true;
        	case R.id.menu_home_quit:
        		screenStuff.showDialog(AMViewItems.QUIT_DIALOG);
        		return true;
        	// TODO: other items..
	        default:
	            return super.onOptionsItemSelected(item);
        }
    }
    
    // PRIVATE FUNCTIONS
    /*
    private void setIconVisiblity(boolean isVisible) {
    	int visibleVal = (isVisible)? View.VISIBLE: View.INVISIBLE;
    	View[] viewItems = {statusText, statusButton, peopleText, peopleButton, sensorsText, sensorsButton};
    	for(View v: viewItems) {
        	if (v!=null) {
        		v.setVisibility(visibleVal);
        	}    		
    	}
    }*/
    
    private void performUpdate(int command, final String mesgData) {
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate("+command+", '"+mesgData+"')  START");
    	if (!isActive) {
    		// Not active when onCreate is not done OR finish() is called
    		screenStuff.setNotBusy();
    		return;
    	}
    	if (AlertMeConstants.isCommandValid(command)) {
    		// Do a command to update the current page..
		boolean isOffline = false;
    		ArrayList<AlertUpdateCommand> commandList = new ArrayList<AlertUpdateCommand>();
    		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  simple update screen command");
    		if (command == AlertMeConstants.UPDATE_COMMANDLIST) {
        		String[] commList = mesgData.split(",");
    			int i;
        		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onActivityResult()  simple update screen: case UPDATE_COMMANDLIST");
        		for(String tmp: commList) {
    				i = Integer.parseInt(tmp);
    				if (AlertMeConstants.isCommandValid(i)) {
    					commandList.add(new AlertUpdateCommand(i));
    				}
        		}
    		} else if (command == AlertMeConstants.UPDATE_ALL) {
        		AlertMeStorage.AlertMeUser user = alertMe.getCurrentSession();
        		alertMe.retrieveAll();
        		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onActivityResult()  simple update screen: case UPDATE_ALL");
			if (user!=null && user.id!=-1) {
				commandList.add(new AlertUpdateCommand(AlertMeConstants.UPDATE_SYSTEMNAME));    			
				commandList.add(new AlertUpdateCommand(AlertMeConstants.UPDATE_SYSTEMNAME));	
				commandList.add(new AlertUpdateCommand(AlertMeConstants.UPDATE_STATUS));
				commandList.add(new AlertUpdateCommand(AlertMeConstants.UPDATE_PEOPLE));
				commandList.add(new AlertUpdateCommand(AlertMeConstants.UPDATE_SENSORS));    			    			
			} else {
				isOffline = true;
			}
    		} else {
        		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onActivityResult()  simple update screen: case [SINGLE COMMAND]");
    			commandList.add(new AlertUpdateCommand(command));
    		}
		// TODO: activate the 'splash' page view if not 'online'
		if (isOffline) {
		} else {
		}
    		for(AlertUpdateCommand updateCommand: commandList) {
    	        switch (updateCommand.command) {
	            	case AlertMeConstants.UPDATE_SYSTEMNAME:	
	            		updateSystemName();
	            		break;
	            	case AlertMeConstants.UPDATE_STATUS:
	    				updateBehaviour();
	            		break;
	            	case AlertMeConstants.UPDATE_PEOPLE:
	            		updatePeople();
	                	break;
	            	case AlertMeConstants.UPDATE_SENSORS:
	            		updateSensors();
	                	break;
    	        }	
    		}
        } else {
    		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  complex command found");
        	switch(command) {
        		case AlertMeConstants.COMMAND_SETTINGS_CREATE_CONFIRMFIRSTTIME:
            		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  complex command: case COMMAND_SETTINGS_CREATE_CONFIRMFIRSTTIME");
    				alertMe.retrieveAll();
    				updateSystemName();
    				updateBehaviour();
            		updatePeople();
            		updateSensors();
            		resetHubChoice();
            		screenStuff.setNotBusy();
            		Toast.makeText(getApplicationContext(), getString(R.string.settings_create_ok), Toast.LENGTH_SHORT).show();
            		break;
    			case AlertMeConstants.COMMAND_SETTINGS_CREATE_CONFIRM:
            		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  complex command: case COMMAND_SETTINGS_CREATE_CONFIRM");
    				updateSystemName();
    				alertMe.retrieveAll();
	    	    	screenStuff.setNotBusy();
					Toast.makeText(getApplicationContext(), getString(R.string.settings_create_ok), Toast.LENGTH_SHORT).show();
	    	    	break;
    			case AlertMeConstants.COMMAND_SETTINGS_CONFIRM:
            		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  complex command: case COMMAND_SETTINGS_CONFIRM");
	    	    	screenStuff.setNotBusy();
					Toast.makeText(getApplicationContext(), getString(R.string.settings_update_ok), Toast.LENGTH_SHORT).show();
	    	    	break;
        		case AlertMeConstants.COMMAND_SETTINGS_CREATE_FAILED:
            		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  complex command: case COMMAND_SETTINGS_CREATE_FAILED");
        	    	screenStuff.setNotBusy();
					Toast.makeText(getApplicationContext(), getString(R.string.settings_update_fail), Toast.LENGTH_SHORT).show();
        	    	break;
    			case AlertMeConstants.INVOKE_HUB_CHOICE:
            		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  complex command: case INVOKE_HUB_CHOICE");
    				resetHubChoice();
    				screenStuff.setNotBusy();
    				screenStuff.showDialog(AMViewItems.HUB_CHOICE_DIALOG);
        	    	break;
    			case AlertMeConstants.INVOKE_HUB_SELECT_OK:
    				String selectOk = getString(R.string.hubchoice_invoke_select_ok);
    				Hub currentHub = null;
            		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  complex command: case INVOKE_HUB_SELECT_OK");
            		alertMe.retrieveAll();
            		currentHub = alertMe.retrieveActiveHub();
        	    	selectOk = (currentHub!=null)?  APIUtilities.getStringReplacedWithToken(selectOk, getString(R.string.hubchoice_hubname_tag), currentHub.name): selectOk;
        	    	updateSystemName();
    				updateBehaviour();
            		updatePeople();
            		updateSensors();
            		resetHubChoice();
            		screenStuff.setNotBusy();
        	    	Toast.makeText(getApplicationContext(), selectOk, Toast.LENGTH_SHORT).show();
        	    	break;
    			case AlertMeConstants.INVOKE_HUB_SELECT_FAIL:
            		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  complex command: case INVOKE_HUB_SELECT_FAIL");
    				screenStuff.setNotBusy();
        	    	Toast.makeText(getApplicationContext(), getString(R.string.hubchoice_invoke_select_fail), Toast.LENGTH_SHORT).show();
        	    	break;
    			case AlertMeConstants.INVOKE_STATUS_CONFIRM:
            		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  complex command: case INVOKE_STATUS_CONFIRM");
    				screenStuff.setNotBusy();
        	    	Toast.makeText(getApplicationContext(), getString(R.string.behaviour_update_ok), Toast.LENGTH_SHORT).show();
        	    	break;
    			case AlertMeConstants.INVOKE_ACCOUNT_SELECT:
            		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  complex command: case INVOKE_ACCOUNT_SELECT");
    				screenStuff.setNotBusy();
    				resetAccountChoice();
    				invokeAccountSelection();
    				break;
    			case AlertMeConstants.INVOKE_ACCOUNT_SELECT_OK:
    				String aSelectOk = getString(R.string.accountchoice_invoke_select_ok);
    				AlertMeStorage.AlertMeUser user = null;
            		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  complex command: case INVOKE_ACCOUNT_SELECT_OK");
            		alertMe.retrieveAll();
            		user = alertMe.getCurrentSession();
        	    	aSelectOk = (user!=null)?  APIUtilities.getStringReplacedWithToken(aSelectOk, getString(R.string.accountchoice_username_tag), user.username): aSelectOk;
        	    	updateSystemName();
    				updateBehaviour();
            		updatePeople();
            		updateSensors();
            		resetAccountChoice();
            		screenStuff.setNotBusy();
        	    	Toast.makeText(getApplicationContext(), aSelectOk, Toast.LENGTH_SHORT).show();
        	    	break;
    			case AlertMeConstants.INVOKE_ACCOUNT_SELECT_FAIL:
            		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  complex command: case INVOKE_ACCOUNT_SELECT_FAIL");
    				screenStuff.setNotBusy();
        	    	Toast.makeText(getApplicationContext(), getString(R.string.accountchoice_invoke_select_fail), Toast.LENGTH_SHORT).show();
        	    	break;
    			case AlertMeConstants.COMMAND_QUIT:
            		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  complex command: case COMMAND_QUIT");
    				screenStuff.setNotBusy();
    				finish();
    				break;
        	}
        	/*
    		// Update actions that aren't an update to the current screen
    		if (command == AlertMeConstants.INVOKE_SETTINGS_CREATE_FIRSTTIME) {
    			invokeAddNewSettingsFirst();
    		} else if (command == AlertMeConstants.INVOKE_SETTINGS_CREATE) {
    			invokeAddNewSettings();
    		} else if (command == AlertMeConstants.INVOKE_SETTINGS_EDIT) {
    			invokeEditCurrentSettings();
    		}*/
    	}
		resetAccountChoice();
		resetHubChoice();
    	screenStuff.setNotBusy();
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()  END");
    }
    
    private void initView() {
		Button settingsButton = null;
		Button refreshButton = null;
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "initView()  START");

		topIcon = (ImageView) findViewById(R.id.home_topico);
		//statusText = (TextView) findViewById(R.id.home_text_status);
		statusButton = (Button) findViewById(R.id.home_button_status);
		statusAlarm = (ImageView) findViewById(R.id.home_button_status_warning);
		//peopleText = (TextView) findViewById(R.id.home_text_people);
		peopleButton = (Button) findViewById(R.id.home_button_people);
		peopleTextCount = (TextView) findViewById(R.id.home_button_people_overlay);
		//sensorsText = (TextView) findViewById(R.id.home_text_sensors);
		sensorsButton = (Button) findViewById(R.id.home_button_sensors);
		settingsButton = (Button) findViewById(R.id.home_button_settings);
		historyButton = (Button) findViewById(R.id.home_button_history);
		helpButton = (Button) findViewById(R.id.home_button_help);
		refreshButton = (Button) findViewById(R.id.home_refresh);
		
		
		sensorsButton = (Button) findViewById(R.id.home_button_sensors);
		if (statusButton!=null) {
			statusButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					invokeBehaviour();
				}
			});
		}
		if (settingsButton!=null) {
			settingsButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					invokeEditCurrentSettings();
				}
			});
		}
		if (sensorsButton!=null) {
			sensorsButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					invokeSensors();
				}
			});
		}
		if (peopleButton!=null) {
			peopleButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					invokePeople();
				}
			});
		}
		if (historyButton!=null) {
			historyButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					invokeEvents();
				}
			});
		}
		if (helpButton!=null) {
			helpButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					invokeHelp();
				}
			});
		}
		if (refreshButton!=null) {
			refreshButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					refreshAlertMe();
				}
			});
		}
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "initView()  END");
    }
    private void initConnectionStatus() {
		if (topIcon != null) {
			String rawRes = alertMe.getLastRawAPIResult();
			int rawInfo = AlertMeServer.Exceptions.getErrorFromRawResult(rawRes);
			boolean hasConnection = true;
			switch (rawInfo) {
				case AlertMeServer.Exceptions.RESULT_NULL:
				break;
				case AlertMeServer.Exceptions.ERROR_NO_SESSION:
					if (AlertMeConstants.DEBUGOUT) Log.w("CONNECTION FAILED" , "ERROR_NO_SESSION");
					hasConnection = false;
				break;
				case AlertMeServer.Exceptions.ERROR_INVALID_USER_DETAILS:
					if (AlertMeConstants.DEBUGOUT) Log.w("CONNECTION FAILED" , "ERROR_INVALID_USER_DETAILS");
					hasConnection = false;
				break;
				case AlertMeServer.Exceptions.ERROR_NEEDS_HUB_UPGRADE:
				break;
				case AlertMeServer.Exceptions.ERROR_LOGIN_FAILED_ACCOUNT_LOCKED:
					if (AlertMeConstants.DEBUGOUT) Log.w("CONNECTION FAILED" , "ERROR_LOGIN_FAILED_ACCOUNT_LOCKED");
					hasConnection = false;
				break;
				default:
					if (rawRes==null || rawRes!=null && rawRes.equals("")) {
						// Check if there is a connection to the internet
						hasConnection = alertMe.hasInternetConnection();
						if (!hasConnection) {
							if (AlertMeConstants.DEBUGOUT) Log.w("CONNECTION FAILED" , "No INTERNET CONNECTION");
						}	
					}
					break;
			}			

			if (hasConnection) {
				if (AlertMeConstants.DEBUGOUT) Log.w("CONNECTION OK" , "ICON DRAWABLE");
				topIcon.setImageResource(R.drawable.icon);
			} else {
				if (AlertMeConstants.DEBUGOUT) Log.w("CONNECTION NOT OK" , "ICON OFFLINE");
				topIcon.setImageResource(R.drawable.icon_offline);
			}
		}
    }
    
    private void changeActiveHub(String hubZId) {
    	// Try to change the active hub
    	boolean doChange = false;
    	Hub active = alertMe.retrieveActiveHub();
		Hub hub = alertMe.retrieveHubByID(hubZId);
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "changeActiveHub('"+hubZId+"')  START");
		if (hub!=null) {
			if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "changeActiveHub()  changing hub to "+hub.name+" ["+hub.id+"] ");
			if (active!=null) {
				if (!active.id.equals(hub.id)) {
					// Do change
					doChange = true;
				}
			} else {
				doChange = true;
			}
		}
		if (doChange) {
			AlertStarter starter = new AlertStarter(alertMe, handler, null, null, null, AlertMeConstants.INVOKE_HUB_SELECT);
			hubChoiceActive = false;
			starter.data = hubZId;
			screenStuff.setBusy(AlertMeConstants.INVOKE_HUB_SELECT);
			starter.start(); // show the screen..
		} else {
			Toast.makeText(getApplicationContext(), getString(R.string.hubchoice_invoke_select_nochange), Toast.LENGTH_SHORT).show();
		}
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "changeActiveHub()  END");
    }
    private void changeActiveAccount(long userId) {
    	boolean doChange = false;
    	AlertMeStorage.AlertMeUser account = alertMe.getUserByID(userId); 
    	AlertMeStorage.AlertMeUser currentAcc = alertMe.getCurrentSession();
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "changeActiveAccount('"+userId+"')  START");
    	if (account!=null && account.id!=-1) {
	    	if (currentAcc!=null) {
	    		if (currentAcc.id!=account.id) {
	    			doChange = true;
	    		}
	    	} else {
				doChange = true;
	    	}
			if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "changeActiveAccount()  attempt to change active account with username: "+account.username);
	    }
    	if (doChange) {
    		AlertStarter starter = new AlertStarter(alertMe, handler, null, null, null, AlertMeConstants.INVOKE_ACCOUNT_SELECT);
    		starter.data = userId+"";
    		screenStuff.setBusy(AlertMeConstants.INVOKE_ACCOUNT_SELECT);
			starter.start(); // show the screen..
    	} else {
			Toast.makeText(getApplicationContext(), getString(R.string.accountchoice_invoke_select_nochange), Toast.LENGTH_SHORT).show();
		}
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "changeActiveAccount()  END");
    }
    
    private void initHubChoiceCache() {
		ArrayList<Hub> hubs = alertMe.retrieveHubs();
		int hubSize = (hubs!=null && !hubs.isEmpty())? hubs.size(): 0;
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "initHubChoiceCache()  START");
		hubIDsChoiceList = null;
		hubNamesChoiceList = null;
		if (hubSize>1) {
			// construct the lists to show in the dialog
			int i = 0;
			Collections.sort(hubs, Hub.getComparator(false)); // sort by name
			hubNamesChoiceList = new String[hubSize];
			hubIDsChoiceList = new String[hubSize];
			for (Hub hub: hubs) {
				hubIDsChoiceList[i] = hub.id;
				hubNamesChoiceList[i] = hub.name;
				i++;
			}
			
		}
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "initHubChoiceCache()  END");
    }
    private void resetHubChoice() {
		hubChoiceActive = false;
		hubNamesChoiceList = null;
		initHubChoiceCache();
    	//screenStuff.editHubDialog(hubNamesChoiceList);
		screenStuff.registerHubChoiceDialog(hubClick, hubNamesChoiceList);
    }
    private void initAccountChoiceCache() {
    	// Abandoned selective list (missing current account from options)
    	ArrayList<AlertMeStorage.AlertMeUser> accounts = alertMe.getUserAccountList();
    	//AlertMeStorage.AlertMeUser currentAcc = alertMe.getCurrentSession();
    	int accSize = (accounts!=null && !accounts.isEmpty())? accounts.size():0;
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "initAccountChoiceCache()  START");
    	accountNamesChoiceList = null;
    	accountIDsChoiceList = null;
    	if (accSize>1) {
    		int i = 0;
    		int cSz = accSize;
    		Collections.sort(accounts, AlertMeStorage.AlertMeUser.getComparator(false));
    		accountNamesChoiceList = new String[cSz];
    		accountIDsChoiceList = new long[cSz];
    		for (AlertMeStorage.AlertMeUser account: accounts) {
    			boolean addToList = false;
    			if (account!=null && account.id!=-1) {
    				addToList = true;
    			}
    			if (addToList) {
    				accountNamesChoiceList[i] = account.username;
    				accountIDsChoiceList[i] = account.id;
    				i++;
    			}
    		}
    	}
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "initAccountChoiceCache()  END");
    }
    private void resetAccountChoice() {
		accountNamesChoiceList = null;
		initAccountChoiceCache();
    	//screenStuff.editAccountDialog(accountNamesChoiceList);
		screenStuff.registerAccountChoiceDialog(accountClick, accountNamesChoiceList);
    }
    private boolean isActiveHubSame(String hubZId) {
    	boolean res = false;
    	Hub activeHub = alertMe.retrieveActiveHub();
    	if (activeHub!=null) {
    		res = hubZId.equals(activeHub.id);
    	}
    		
    	return res;
    }
    
    // Update the screen
    private void updateSystemName() {
    	Hub hub = alertMe.retrieveActiveHub();
    	if (hub!=null) {
    		screenStuff.setSystemName(hub);
    	}
	initConnectionStatus();
    }
    private void updateBehaviour() {
    	Hub hub = alertMe.retrieveActiveHub();
    	if (hub!=null) {
    		String behaviour = hub.behaviour;
        	updateScreenBehaviour(behaviour);   		
    	}
    }
    private void updatePeople() {
    	// Call the client for the current people cached..
    	ArrayList<Device> keyfobs = alertMe.retrieveDevices(Device.KEYFOB);
    	int personCount = 0;
    	int total = keyfobs.size();
    	for(Device keyfob: keyfobs) {
    		if (keyfob.attributes.containsKey("presence")) {
    			String pres = (String) keyfob.attributes.get("presence");
    			if (pres!=null) {
    				pres = pres.trim().toLowerCase();
        			personCount += (pres.equals("true"))? 1: 0;
    			}
    		}
    	}
    	updateScreenPeople(personCount, total);
    }
    private void updateSensors() {
    	ArrayList<Device> devices = alertMe.retrieveDevices();
    	//int totalCount = 0;
    	int missingCount = 0;
    	if (devices!=null) {
        	for(Device device: devices) {
        		if (device.type!=Device.POWER_CONTROLLER) {
        			if (device.batteryLevel<=0) {
        				if (device.type!=Device.KEYFOB) {
            				missingCount++;
        				}
        			}
        		}
    			//totalCount++;
        	}
        	updateScreenSensors(missingCount);    		
    	}
    }
    
    private void updateScreenBehaviour(String statusIn) {
    	boolean hasAlarm = false;
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "updateScreenBehaviour('"+statusIn+"')  START");
    	if (statusButton!=null) {
    		String status = (statusIn!=null)? statusIn.trim().toLowerCase(): "";
    		//String statusString = getString(R.string.behaviour_unavailable);
    		//boolean setText = false;
    		if (status == null) {
    			// Do nothing
    			if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "updateScreenBehaviour()  case NO CHANGE");
    		} else if (status.equalsIgnoreCase("home")) {
    			//statusString = getString(R.string.behaviour_home);
    			statusButton.setBackgroundResource(R.drawable.btn_home_status_home);
    			if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "updateScreenBehaviour()  case HOME");
    			//setText = true;
    		} else if (status.equalsIgnoreCase("away")) {
    			//statusString = getString(R.string.behaviour_away);
    			statusButton.setBackgroundResource(R.drawable.btn_home_status_lock);
    			if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "updateScreenBehaviour()  case AWAY");
    			//setText = true;
    		} else if (status.equalsIgnoreCase("night")) {
    			//statusString = getString(R.string.behaviour_night);
    			statusButton.setBackgroundResource(R.drawable.btn_home_status_night);
    			if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "updateScreenBehaviour()  case NIGHT");
    			//setText = true;
    		} else {
    			//statusString = getString(R.string.behaviour_unavailable);
    			statusButton.setBackgroundResource(R.drawable.btn_home_status_offline);
    			if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "updateScreenBehaviour()  case OFFLINE");
    			//setText = true;
    		}
    		//if (setText) {
    			// If setting the status button text:
        		//statusButton.setText(statusString);
    		//}
    		if (statusAlarm!=null) {
    			int visibility = View.GONE;
        		if (currentIntruderState!=null) {
        			hasAlarm = (currentEmergencyState.equals("alarmed")||currentEmergencyState.equals("alarmConfirmed"));
        		}
        		if (currentEmergencyState!=null) {
        			hasAlarm = hasAlarm||(currentEmergencyState.equals("alarmed")||currentEmergencyState.equals("alarmConfirmed"));
        		}
        		visibility = (hasAlarm)? View.VISIBLE: View.GONE;
        		statusAlarm.setVisibility(visibility);
    		}
    	}
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "updateScreenBehaviour()  END");
    }
    private void updateScreenPeople(int totalPresent, int total) {
    	if (peopleTextCount!=null) {
    		if (totalPresent==total) {
    			peopleTextCount.setBackgroundResource(R.drawable.indicator_ok);
        		peopleTextCount.setText("");    			
    		} else {
    			peopleTextCount.setBackgroundResource(R.drawable.indicator_count_overlay);
        		peopleTextCount.setText(totalPresent+"");    			
    		}
    	}
    }
    private void updateScreenSensors(int totalMissing) {
    	if (sensorsButton!=null) {
    		String sensorString = getString(R.string.sensor_none_missing);
    		String countTag = getString(R.string.sensor_count_tag);
    		if (totalMissing>0) {
    			sensorString = (totalMissing>1)? getString(R.string.sensor_multiple_missing): getString(R.string.sensor_single_missing);
    		}
    		if (sensorString.contains(countTag)) {
    			sensorString = sensorString.replace(countTag, totalMissing+"");
    		}
        	sensorsButton.setText(sensorString);
    		if (totalMissing>0) {
    			sensorsButton.setBackgroundResource(R.drawable.btn_home_sensors_notok);
    		} else  {
    			sensorsButton.setBackgroundResource(R.drawable.btn_home_sensors_ok);
    		}
    	}
    }
	public void exit() {
		finish();
	}
	@Override
	public void finish() {
		SharedPreferences sharedPrefs = getSharedPreferences(AlertMeConstants.PREFERENCE_NAME, AlertMeConstants.PREFERENCE_MODE);
		alertMe.setCurrentSessionAsPreference(sharedPrefs);
		alertMe.clean();
		isActive = false;
		super.finish();
	}    
    
    // PRIVATE CLASSES	
	class AlertUpdateCommand {
		public int command = AlertMeConstants.UPDATE_CANCEL;
		public AlertUpdateCommand(int i) {
			if (AlertMeConstants.isCommandValid(command)) {
				command = i;
			}
		}
	}
	class AlertStarter extends Thread {
		public boolean forceLogin = true;
		public int instruction = -1;
		public String data = null;
		private AlertMeSession alertme;
		private Handler handler;
		private Intent intent;
		private Bundle bundle;
		private SharedPreferences sharedPrefs;

		public AlertStarter(AlertMeSession client, Handler handle, Intent intentIn, Bundle bundleIn, SharedPreferences prefs) {
			alertme = client;
			handler = handle;
			intent = intentIn;
			bundle = bundleIn;
			sharedPrefs = prefs;
		}
		public AlertStarter(AlertMeSession client, Handler handle, Intent intentIn, Bundle bundleIn, SharedPreferences prefs, int instruct) {
			alertme = client;
			handler = handle;
			intent = intentIn;
			bundle = bundleIn;
			sharedPrefs = prefs;
			instruction = instruct;
		}
        @Override
        public void run() {
        	// This is the thread that should do all the alertme api calls
        	// By the time update screen functions are called, the data should
        	// be fresh enough not to delay..
        	int updateInstruction = AlertMeConstants.UPDATE_ALL;
        	//String updateDetails = null;
    		boolean hasCurrentSys = false;
    		int modeResult = -1;
    		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "ALERTSTARTER::Thread run()  START");
        	if (intent!=null) {
        		modeResult = AlertMeConstants.getReturnKeycodeFromIntentBundle(intent, null, -1);
        	}
    		// Check if currently logged in and refresh login if required
    		hasCurrentSys = alertme.hasSessionValues();
    		
    		if (instruction!=AlertMeConstants.COMMAND_QUIT && instruction!=AlertMeConstants.INVOKE_SETTINGS_CREATE_FIRSTTIME) {
        		if (hasCurrentSys && !alertme.isSessionAlive()) alertme.login();

        		// Check for the current systemID in the bundle if not in preferences..		
        		if (!hasCurrentSys) hasCurrentSys = alertme.loadFromIntentBundle(intent, bundle);
        		if (!hasCurrentSys) hasCurrentSys = alertme.loadFromPreference(sharedPrefs);
        		if (!hasCurrentSys) hasCurrentSys = alertme.loadFromOnlyEntry();
        		if (!hasCurrentSys) {
        			ArrayList<AlertMeStorage.AlertMeUser> users = alertme.getUserAccountList();
        			if (users!=null && !users.isEmpty() && users.size()>1) {
        				instruction = AlertMeConstants.INVOKE_ACCOUNT_CHOICE; // changing the instruction
        			}
        		}
        		resetAccountChoice();
        		resetHubChoice();
    		}
    		
    		if (hasCurrentSys) {
    			alertme.setCurrentSessionAsPreference(sharedPrefs);
    		} else {
				if (instruction == AlertMeConstants.COMMAND_SETTINGS) {
					invokeAddNewSettingsFirst();
					return;
				}
    		}
        	
    		switch (instruction) {
    			case AlertMeConstants.UPDATE_STATUS:
    	    		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "ALERTSTARTER::Thread run()  instruction case: UPDATE_STATUS");
    				alertme.getActiveHub();
	        		updateInstruction = AlertMeConstants.UPDATE_STATUS;
	        		break;
    			case AlertMeConstants.UPDATE_ALL:
    				alertme.loadAll();
	    			if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "ALERTSTARTER::Thread run()  instruction case: UPDATE_ALL");
	        		if (currentIntruderState==null) currentIntruderState = alertme.getActiveServiceState(AlertMeSession.SERVICE_INTRUDER_ALARM);
	    			if (currentEmergencyState==null) currentEmergencyState = alertme.getActiveServiceState(AlertMeSession.SERVICE_EMERGENCY_ALARM);

    				alertme.getActiveHub();
	        		resetHubChoice();
	        		updateInstruction = AlertMeConstants.UPDATE_ALL;
	        		break;
    			case AlertMeConstants.INVOKE_SETTINGS_CREATE_FIRSTTIME:
    			case AlertMeConstants.INVOKE_SETTINGS_CREATE:
	        	case AlertMeConstants.INVOKE_SETTINGS_EDIT:
        			String username =  (intent!=null)? intent.getStringExtra(AlertMeConstants.INTENT_RETURN_USERNAME): null;
        			String password =  (intent!=null)? intent.getStringExtra(AlertMeConstants.INTENT_RETURN_PASSWORD): null;
        			AlertMeStorage.AlertMeUser user = null;
        			boolean userOk = false;
        			
        			if (instruction==AlertMeConstants.INVOKE_SETTINGS_CREATE_FIRSTTIME || instruction==AlertMeConstants.INVOKE_SETTINGS_CREATE) {
        				user = alertme.loginFirstTime(username, password);
        				userOk = (user!=null && user.id!=-1);
        			} else {
        				userOk = APIUtilities.isStringNonEmpty(username) && APIUtilities.isStringNonEmpty(password);
        			}
        			
        			if (userOk) {
        				updateInstruction = AlertMeConstants.COMMAND_SETTINGS_CREATE_CONFIRM;
        			} else {
		        		updateInstruction = AlertMeConstants.COMMAND_SETTINGS_CREATE_FAILED;
	        		}
        			if (instruction==AlertMeConstants.INVOKE_SETTINGS_CREATE_FIRSTTIME) {
        	    		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "ALERTSTARTER::Thread run()  instruction case: INVOKE_SETTINGS_CREATE_FIRSTTIME  moderesult:"+modeResult);
    	        		if (modeResult==AlertMeConstants.COMMAND_SETTINGS_CREATE_CONFIRMFIRSTTIME) {
    		        		if (userOk) {
        		        		alertme.setSession(user);
        		        		alertme.loadAll();
        		        		resetHubChoice();
        		        		updateInstruction = AlertMeConstants.COMMAND_SETTINGS_CREATE_CONFIRMFIRSTTIME;
    		        		}
    	        		}
        			} else if (instruction == AlertMeConstants.INVOKE_SETTINGS_EDIT) {
        	    		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "ALERTSTARTER::Thread run()  instruction case: INVOKE_SETTINGS_EDIT  moderesult:"+modeResult);
		        		// If the user creation was ok, then replace this!
	        			// get the systemID created from this and update
		        		if (userOk) {
		        			AlertMeStorage.AlertMeUser current = alertMe.getCurrentSession();
		        			alertme.loadAll();
    		        		resetHubChoice();
    		        		if (current!=null && current.id>=0) {
    		        			alertme.updateSessionValues(username, password, current.sessionKey);
    		        			updateInstruction = AlertMeConstants.COMMAND_SETTINGS_CONFIRM;
    		        		}
		        		}
		        	} else if (instruction == AlertMeConstants.INVOKE_SETTINGS_CREATE) {
        	    		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "ALERTSTARTER::Thread run()  instruction case: INVOKE_SETTINGS_CREATE  moderesult:"+modeResult);
		        	}
        			
	        		break;
	        	case AlertMeConstants.INVOKE_STATUS:
    	    		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "ALERTSTARTER::Thread run()  instruction case: INVOKE_STATUS  moderesult:"+modeResult);
	        		boolean refreshBehaviour = false;
	        		if (modeResult==AlertMeConstants.COMMAND_STATUS_AWAY) {
	        			refreshBehaviour = true;
	        		} else if (modeResult==AlertMeConstants.COMMAND_STATUS_HOME) {
	        			refreshBehaviour = true;
	        		} else if (modeResult==AlertMeConstants.COMMAND_STATUS_NIGHT) {
	        			refreshBehaviour = true;
	        		}
	        		if (refreshBehaviour) {
	    	    		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "ALERTSTARTER::Thread run()  instruction case: INVOKE_STATUS  -- invoked API mode change");
	            		alertme.getActiveHub();
		        		updateInstruction = AlertMeConstants.INVOKE_STATUS_CONFIRM;
	        		}
	        		break;
    			case AlertMeConstants.INVOKE_HUB_CHOICE:
    	    		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "ALERTSTARTER::Thread run()  instruction case: INVOKE_HUB_CHOICE  moderesult:"+modeResult);
    				alertme.getHubData(); // reload the hubs to the cache
    				updateInstruction = AlertMeConstants.INVOKE_HUB_CHOICE; // update invokes the dialog
	        		break;
    			case AlertMeConstants.INVOKE_HUB_SELECT:
    	    		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "ALERTSTARTER::Thread run()  instruction case: INVOKE_HUB_SELECT  moderesult:"+modeResult+" setting active hub ["+data+"]");
    				if (data!=null) {
        				boolean isOk = false;
        				alertme.getHubData(); // reload the hubs to the cache
        				isOk = alertme.setActiveHub(data);
        				if (isOk) {
        					updateInstruction = AlertMeConstants.INVOKE_HUB_SELECT_OK;
    		        		alertme.clearDeviceCache();
    		        		alertme.clearEventCache();
    		        		alertme.setCurrentSessionAsPreference(sharedPrefs);
    		        		alertme.loadAll();
        				} else {
        					updateInstruction = AlertMeConstants.INVOKE_HUB_SELECT_FAIL;
        				}
    				} else {
    					updateInstruction = AlertMeConstants.INVOKE_HUB_SELECT_FAIL;
    				}
	        		break;
    			case AlertMeConstants.INVOKE_ACCOUNT_SELECT:
    				long dataId = (data!=null)? getLongFromString(data): -1;
    	    		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "ALERTSTARTER::Thread run()  instruction case: INVOKE_ACCOUNT_SELECT  moderesult:"+modeResult);
    				if (dataId!=-1) {
    					boolean isOk = alertme.loginAsUser(dataId);
    					if (isOk) {
    						Hub active = null;
        					updateInstruction = AlertMeConstants.INVOKE_ACCOUNT_SELECT_OK;
    		        		alertme.refreshAll();
    		        		active = alertme.getActiveHub();
    		        		if (active==null) {
    		        			alertme.loadDefaultHubAsActive();
    		        		}
    		        		alertme.setCurrentSessionAsPreference(sharedPrefs);
        				} else {
        					updateInstruction = AlertMeConstants.INVOKE_ACCOUNT_SELECT_FAIL;
        				}
    				} else {
    					updateInstruction = AlertMeConstants.INVOKE_ACCOUNT_SELECT_FAIL;
    				}
	        		break;
    			case AlertMeConstants.COMMAND_QUIT:
    	    		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "ALERTSTARTER::Thread run()  instruction case: COMMAND_QUIT");
    				alertme.logout();
    				updateInstruction = AlertMeConstants.COMMAND_QUIT;
    				break;
    		}    		
    		

    		if (handler!=null) {
    			Message msg = handler.obtainMessage();
                Bundle b = new Bundle();
                b.putInt("type", updateInstruction);
                //if (updateDetails!=null) {
                //	b.putString("value", updateDetails);
                //}
                msg.setData(b);
                
                alertMe = alertme;
                handler.sendMessage(msg);
    		}        		

    		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "ALERTSTARTER::Thread run()  END");
        } // end run

	}
	
	private void loadFromRestoredState() {
		final Object data = getLastNonConfigurationInstance();
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "loadFromRestoredState()  START");
		if (data != null) {
			final AlertMeState saveState = (AlertMeState) data;
			AlertMeSession.SessionState oldState = saveState.session;
			boolean reloaded = false;
			if (alertMe==null) {
				alertMe = new AlertMeSession(this);
			}
			if (saveState.currentIntruderState!=null) currentIntruderState = saveState.currentIntruderState;
			if (saveState.currentEmergencyState!=null) currentEmergencyState = saveState.currentEmergencyState;
			reloaded = alertMe.loadFromCachedState(this, oldState);
			if (reloaded) {
        		alertMe.retrieveAll();
			}
			
			if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "loadFromRestoredState()  did reload from old state:"+reloaded);
		}
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "loadFromRestoredState()  END");
	}
	
	private static long getLongFromString(String s) {
		long res = -1;
		
		try {
			Long rawRes = new Long(s);
			res = rawRes.longValue();
		} catch (Exception e) {}
		
		return res;
	}
	
	class AlertMeState {
		public AlertMeSession.SessionState session = null;
		public String currentIntruderState = null;
		public String currentEmergencyState = null;
	}

    interface AlertMeMethodCallback {
    	void callFinished(Object result);
    }
	
	// register sreen
	private final DialogInterface.OnClickListener hubClick = new DialogInterface.OnClickListener() {
	    public void onClick(DialogInterface dialog, int item) {
	    	if (hubNamesChoiceList==null || hubIDsChoiceList==null) {
	    		resetHubChoice();
	    	}
	    	if (hubNamesChoiceList!=null && hubIDsChoiceList!=null) {
	    		int sz = hubIDsChoiceList.length;
	    		if (item>=0 && item<sz) {
	    			String zid = hubIDsChoiceList[item];
	    			if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "HUBSELECTCLICK::Listener onClick(dialog, id)  -- selected hub "+hubNamesChoiceList[item]+" with ID:"+hubIDsChoiceList[item]);
	    			if (!isActiveHubSame(zid)) {
    	    			changeActiveHub(zid);
	    			}
	    		}
	    	} else {
    			if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "HUBSELECTCLICK::Listener onClick(dialog, id)  -- selected hub FAILED (null list)");
	    	}
	    }
	};
	
	private final DialogInterface.OnClickListener accountClick = new DialogInterface.OnClickListener() {
	    public void onClick(DialogInterface dialog, int item) {
	    	long currId = -1;
	    	if (accountNamesChoiceList==null || accountIDsChoiceList==null) {
	    		resetAccountChoice();
	    	}
	    	if (accountNamesChoiceList!=null && accountIDsChoiceList!=null) {
	    		int sz = accountIDsChoiceList.length;
	    		if (item>=0 && item<sz) {
	    			if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "ACCOUNTSELECTCLICK::Listener onClick(dialog, id)  -- selected username "+accountNamesChoiceList[item]+" with DB ID:"+accountIDsChoiceList[item]);
	    			currId = accountIDsChoiceList[item];
	    		}
	    	} else {
    			if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "ACCOUNTSELECTCLICK::Listener onClick(dialog, id)  -- selected hub FAILED (null list)");
	    	}
	    	if (currId!=-1) {
    			changeActiveAccount(currId);
	    	}
	    }
	};
	private final DialogInterface.OnClickListener quitClick = new DialogInterface.OnClickListener() {
	    public void onClick(DialogInterface dialog, int item) {
	    	finish();
	    }
	};
	private final DialogInterface.OnClickListener quitCancelClick = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
            dialog.cancel();
       }
		
	};


}
