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
import org.darkgoddess.alertme.api.AlertMeStorage;
import org.darkgoddess.alertme.api.utils.APIUtilities;
import org.darkgoddess.alertme.api.utils.Hub;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


public class AlertMeSettings extends Activity {
	private static final String TAG = "ACTIVITY:AlertMeSettings";
	private final static int PROGRESS_DIALOG = 0;
	private ProgressDialog progressDialog = null;
	private AlertMeSession alertMe = null;
	private boolean modeSet = false;
	private boolean isBusy = false;
	private boolean isNewMode = false;
	private boolean isActive = false;
	private boolean hasCreated = false;
	private TextView titleText = null;
	private TextView hubText = null;
	private Button confirmButton = null;
	private Button cancelButton = null;
	private EditText userText = null;
	private EditText passText = null;
	private String login = null;
	private String password = null;
	private Bundle savedState = null;

    // Handler to update the interface..        
	final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
        	populateFields();
        }
    };
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		savedState = savedInstanceState;
		isActive = true;
		loadFromRestoredState();
		if (!hasCreated) {
			hasCreated = true;
			initView();
		}
		//populateFields();
		initViewMode();
	}
	@Override
    public void onStart() {
		super.onStart();
		loadFromRestoredState();
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
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onRetainNonConfigurationInstance()  START");
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onRetainNonConfigurationInstance()  END");
		return alertMe.retrieveCurrentState();
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
	protected Dialog onCreateDialog(int id) {
    	Dialog res = null;
        switch(id) {
        	case PROGRESS_DIALOG:
        		if (progressDialog==null) {
            		progressDialog = new ProgressDialog(AlertMeSettings.this);        			
        		}
        		res = progressDialog;
        		break;
        }
        return res;
    }
	@Override
	public void finish() {
		alertMe.clean();
		isActive = false;
		super.finish();
		
	}
	private void initView() {
		setContentView(R.layout.alertme_settings);
		titleText = (TextView) findViewById(R.id.settings_title);
		hubText = (TextView) findViewById(R.id.settings_housename);
		confirmButton = (Button) findViewById(R.id.login_ok);
		cancelButton = (Button) findViewById(R.id.login_cancel);
		userText = (EditText) findViewById(R.id.login_username);
		passText = (EditText) findViewById(R.id.login_password);
		alertMe = (alertMe==null)? new AlertMeSession(this): alertMe;
	}
	private void initViewMode() {
		SharedPreferences sharedPrefs = getSharedPreferences(AlertMeConstants.PREFERENCE_NAME, AlertMeConstants.PREFERENCE_MODE);
		int intendedMode = 0;
		SettingsStarter starter = null;
		Intent intent = getIntent();
        
		// get the mode from the bundle
		if (!modeSet) {
			intendedMode = AlertMeConstants.getInvokeKeycodeFromIntentBundle(getIntent(), savedState, AlertMeConstants.INVOKE_SETTINGS_CREATE);
			// 
			switch(intendedMode) {
				case AlertMeConstants.INVOKE_SETTINGS_CREATE:
					if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onCreate()  -- CREATE MODE");
					isNewMode = true;
					setButtonsForCreate();
					break;
				case AlertMeConstants.INVOKE_SETTINGS_EDIT:
					if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onCreate()  -- EDIT MODE");
					setButtonsForEdit();
					isNewMode = false;
					break;
				case AlertMeConstants.INVOKE_SETTINGS_CREATE_FIRSTTIME:
					if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onCreate()  -- CREATE FIRST TIME MODE");
					isNewMode = true;
					setButtonsForCreateFirstTime();
					break;
			}
			
			modeSet = true;
			starter = new SettingsStarter(alertMe, handler, intent, savedState, sharedPrefs);
			starter.instruction = AlertMeConstants.UPDATE_ALL;
			setBusy();
			starter.start(); // show the screen..
			
		}

	}
    private void setBusy(final String title, final String message) {
    	if (progressDialog!=null) {
    		if (title!=null && title.length()!=0) { progressDialog.setMessage(title); }
    		if (message!=null && message.length()!=0) { progressDialog.setMessage(message); }
    		if (!isBusy) {
        		isBusy = true;
        		showDialog(PROGRESS_DIALOG);
        		//progressDialog.show(this, title, message);
    		}
    	}
    }
    private void setBusy() {
    	setBusy("", getString(R.string.home_command_update_systemname));
    }
	private void setNotBusy() {
		if (progressDialog!=null && isBusy) {
			isBusy = false;
			dismissDialog(PROGRESS_DIALOG);
			//	progressDialog.dismiss();
		}
	}

	private void populateFields() {
		if (isActive && alertMe.hasSessionValues()) {
			Hub activeHub = null;
			if (!isNewMode) {
				AlertMeStorage.AlertMeUser details = alertMe.getCurrentSession();
				if (details!=null) {
					activeHub = alertMe.retrieveActiveHub();
					if (activeHub!=null && APIUtilities.isStringNonEmpty(activeHub.name)) {
						hubText.setText(activeHub.name);
					}
					login = details.username;
					password = details.password;
					if (APIUtilities.isStringNonEmpty(login) && APIUtilities.isStringNonEmpty(password)) {
						userText.setText(login);
						passText.setText(password);
					}
				}
			}
		}
		setNotBusy();
	}
	private String getUserText() {
		String res = null;
		if (userText!=null) {
			res = userText.getText().toString();
		}
		return res;
	}
	private String getPassText() {
		String res = null;
		if (passText!=null) {
			res = passText.getText().toString();
		}
		return res;
	}
	private void setButtonsForCreate() {
		if (confirmButton!=null) {
			confirmButton.setOnClickListener(new View.OnClickListener() {
		        public void onClick(View view) {
		        	Intent intent = alertMe.createIntentFromSession();
		        	intent.putExtra(AlertMeConstants.INTENT_RETURN_KEY, AlertMeConstants.COMMAND_SETTINGS_CREATE_CONFIRM);
		        	intent.putExtra(AlertMeConstants.INTENT_RETURN_USERNAME, getUserText());
		        	intent.putExtra(AlertMeConstants.INTENT_RETURN_PASSWORD, getPassText());
		            setResult(RESULT_OK, intent);
		            finish();
		        }
		
			});
			
		}
		if (cancelButton!=null) {
			cancelButton.setOnClickListener(new View.OnClickListener() {
		        public void onClick(View view) {
		        	Intent intent = alertMe.createIntentFromSession();
		        	populateFields(); // reset to DB values
		        	intent.putExtra(AlertMeConstants.INTENT_RETURN_KEY, AlertMeConstants.COMMAND_SETTINGS_CREATE_CANCEL);
		            setResult(RESULT_CANCELED, intent);
		            finish();
		        }
		
			});		
		}
		if (titleText!=null) {
			titleText.setText(getText(R.string.login_title_add));
		}
	}
	private void setButtonsForCreateFirstTime() {
		if (confirmButton!=null) {
			confirmButton.setOnClickListener(new View.OnClickListener() {
		        public void onClick(View view) {
		        	Intent intent = alertMe.createIntentFromSession();
		        	intent.putExtra(AlertMeConstants.INTENT_RETURN_KEY, AlertMeConstants.COMMAND_SETTINGS_CREATE_CONFIRMFIRSTTIME);
		        	intent.putExtra(AlertMeConstants.INTENT_RETURN_USERNAME, getUserText());
		        	intent.putExtra(AlertMeConstants.INTENT_RETURN_PASSWORD, getPassText());
		            setResult(RESULT_OK, intent);
		            finish();
		        }
		
			});
			
		}
		if (cancelButton!=null) {
			cancelButton.setVisibility(View.GONE); // INVISIBLE
		}
		if (titleText!=null) {
			titleText.setText(getText(R.string.login_title_add_firsttime));
		}
	}
	private void setButtonsForEdit() {
		if (confirmButton!=null) {
			confirmButton.setOnClickListener(new View.OnClickListener() {
		        public void onClick(View view) {
		        	Intent intent = alertMe.createIntentFromSession();
		        	intent.putExtra(AlertMeConstants.INTENT_RETURN_KEY, AlertMeConstants.COMMAND_SETTINGS_CONFIRM);
		        	intent.putExtra(AlertMeConstants.INTENT_RETURN_USERNAME, getUserText());
		        	intent.putExtra(AlertMeConstants.INTENT_RETURN_PASSWORD, getPassText());
		            setResult(RESULT_OK, intent);
		            finish();
		        }
		
			});
			
		}
		if (cancelButton!=null) {
			cancelButton.setOnClickListener(new View.OnClickListener() {
		        public void onClick(View view) {
		        	populateFields(); // reset to DB values
		        	completeSettingsAction(AlertMeConstants.COMMAND_SETTINGS_CANCEL, RESULT_CANCELED);
		        }
		
			});		
		}				
		if (titleText!=null) {
			titleText.setText(getText(R.string.login_title_edit));
		}
	}
	
	private void completeSettingsAction(int command, int exitCode) {
		int result = (exitCode==RESULT_OK||exitCode==RESULT_CANCELED)? exitCode: RESULT_OK;
    	Intent intent = alertMe.createIntentFromSession();
		
		intent.putExtra(AlertMeConstants.INTENT_RETURN_KEY, command);
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "completeSettingsAction("+command+", "+exitCode+")   -- returning with exit:"+result+"; intent"+intent);
		setResult(result, intent);
        finish();
	}
	private void loadFromRestoredState() {
		final Object data = getLastNonConfigurationInstance();
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "loadFromRestoredState()  START");
		if (data != null) {
			final AlertMeSession.SessionState oldState = (AlertMeSession.SessionState) data;
			boolean reloaded = false;
			if (alertMe==null) {
				alertMe = new AlertMeSession(this);
			}
			reloaded = alertMe.loadFromCachedState(this, oldState);

			if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "loadFromRestoredState()  did reload from old state:"+reloaded);
		}
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "loadFromRestoredState()  END");
	}
	

	class SettingsStarter extends Thread {
		public boolean forceLogin = true;
		public int instruction = -1;
		private AlertMeSession alertme;
		private Handler handler;
		private Intent intent;
		private Bundle bundle;
		private SharedPreferences sharedPrefs;

		public SettingsStarter(AlertMeSession client, Handler handle, Intent intentIn, Bundle bundleIn, SharedPreferences prefs) {
			alertme = client;
			handler = handle;
			intent = intentIn;
			bundle = bundleIn;
			sharedPrefs = prefs;
		}
        @Override
        public void run() {
        	boolean hasCurrentSys = alertme.hasSessionValues();
        	
			//if (hasCurrentSys) alertme.login();
		
			// Check for the current systemID in the bundle if not in preferences..		
			if (!hasCurrentSys) hasCurrentSys = alertme.loadFromIntentBundle(intent, bundle);
			if (!hasCurrentSys) hasCurrentSys = alertme.loadFromPreference(sharedPrefs);
			//if (!hasCurrentSys) hasCurrentSys = alertme.loadFromOnlyEntry();

			if (hasCurrentSys) {
				alertme.getActiveHub();
			}
			
    		if (handler!=null) {
    			Message msg = handler.obtainMessage();
                Bundle b = new Bundle();
                msg.setData(b);
                handler.sendMessage(msg);
    		}        		

        }
	}
}
