package org.darkgoddess.alertme;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.darkgoddess.alertme.api.AlertMeServer;
import org.darkgoddess.alertme.api.AlertMeStorage;
import org.darkgoddess.alertme.api.utils.APIUtilities;
import org.darkgoddess.alertme.api.utils.Device;
import org.darkgoddess.alertme.api.utils.Hub;

public class AlertMeTest extends Activity {
	private static final String TAG = "AlertMeTest";
	private AMViewItems screenStuff = null;
	private AlertMeStorage db = null;
	private TextView title = null;
	private TextView body = null;
	private LinearLayout setup = null;
	private LinearLayout results = null;
	private EditText userText = null;
	private EditText passText = null;
	private String username = null;
	private String password = null;
	private long sessionStart = 0;
	private ArrayList<String> tests = null; 

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
		setContentView(R.layout.alertme_debug);
		restoreCurrentTestState();
		initView();
		enableSetup();
	}
	@Override
    public void onStart() {
		super.onStart();
		//DebugStarter starter;

		restoreCurrentTestState();
		screenStuff = new AMViewItems(this, this);
		screenStuff.registerQuitDialog(quitClick, quitCancelClick);
		screenStuff.initProgressDialog();
		screenStuff.initQuitDialog();
		
		
		//starter = new DebugStarter(handler, username, password);
		//starter.addTest("4");
		//starter.start();
	}
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        MenuItem refresh = null;
        inflater.inflate(R.menu.basic, menu);
    	refresh = menu.findItem(R.id.menu_home_refresh);
    	if (refresh!=null) refresh.setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
			case R.id.menu_home_quit:
				screenStuff.showDialog(AMViewItems.QUIT_DIALOG);
				return true;
			// TODO: other items..
		    default:
		        return super.onOptionsItemSelected(item);
		}
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
		if (db!=null) {
			db.close();
		}
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onDestroy()  END");
    }
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onSaveInstanceState()  START");
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onSaveInstanceState()  END");
	}
	@Override
	protected Dialog onCreateDialog(int id) {
    	Dialog res = screenStuff.onCreateDialog(id);
        return res;
    }
	@Override
	public Object onRetainNonConfigurationInstance() {
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onRetainNonConfigurationInstance()  START");
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onRetainNonConfigurationInstance()  END");
		return getCurrentTestState();
	}
	private AMTestState getCurrentTestState() {
		AMTestState res = new AMTestState();
		int i = 0;
		res.username = username;
		res.password = password;
		res.sessionStart = sessionStart;
		res.testList = "";
		for (String s: tests) {
			res.testList += (i++==0)? "": ",";
			res.testList += s;
		}
		
		return res;
	}
	private void initView() {
		title = (TextView) findViewById(R.id.debug_title);
		body = (TextView) findViewById(R.id.debug_body);
		userText = (EditText) findViewById(R.id.debug_username);
		passText = (EditText) findViewById(R.id.debug_password);
		setup = (LinearLayout) findViewById(R.id.debug_section_setup);
		results = (LinearLayout) findViewById(R.id.debug_section_results);
	}
	private void enableSetup() {
		if (setup!=null) {
			setup.setVisibility(View.VISIBLE);
		}
		if (results!=null) {
			results.setVisibility(View.GONE);
		}
	}
	private void enableResults() {
		if (setup!=null) {
			setup.setVisibility(View.GONE);
		}
		if (results!=null) {
			results.setVisibility(View.VISIBLE);
		}		
	}
	private boolean accountDetailsValid() {
		boolean res = false;
		if (userText!=null) {
			username = userText.getText().toString();
		}
		if (passText!=null) {
			password = passText.getText().toString();			
		}
		res = (APIUtilities.isStringNonEmpty(username) && APIUtilities.isStringNonEmpty(password));
		return res;
	}
	private ArrayList<String> getTestArrayList() {
		ArrayList<String> res = null;
		final CheckBox checkBox1 = (CheckBox) findViewById(R.id.test1);
		final CheckBox checkBox2 = (CheckBox) findViewById(R.id.test2);
		final CheckBox checkBox3 = (CheckBox) findViewById(R.id.test3);
		final CheckBox checkBox4 = (CheckBox) findViewById(R.id.test4);
        if (checkBox1!=null && checkBox1.isChecked()) {
        	if (res == null) res = new ArrayList<String>();
        	res.add("1");
        }
        if (checkBox2!=null && checkBox2.isChecked()) {
        	if (res == null) res = new ArrayList<String>();
        	res.add("2");
        }
        if (checkBox3!=null && checkBox3.isChecked()) {
        	if (res == null) res = new ArrayList<String>();
        	res.add("3");
        }
        if (checkBox4!=null && checkBox4.isChecked()) {
        	if (res == null) res = new ArrayList<String>();
        	res.add("4");
        }
		
		return res;
	}
	public void retakeTests(View view) {
		retakeTests();
	}
	public void invokeTests(View view) {
		invokeTests();
	}
	private void retakeTests() {
		enableSetup();
	}
	
	private void invokeTests() {
		ArrayList<String> testList = getTestArrayList();
		boolean testsValid = (testList!=null && !testList.isEmpty());
		if(accountDetailsValid() && testsValid) {
			DebugStarter starter;
			starter = new DebugStarter(handler, username, password);
			for (String s: testList) {
				starter.addTest(s);
			}
			screenStuff.setBusy(AlertMeConstants.INVOKE_TEST);
			starter.start();
		} else {
    		Toast.makeText(getApplicationContext(), "Cannot run tests until the account details have been added", Toast.LENGTH_SHORT).show();
		}

	}
	private void restoreCurrentTestState() {
		final Object data = getLastNonConfigurationInstance();
		if (data!=null) {
			final AMTestState state = (AMTestState) data;
			final String[] rawtests = (state.testList!=null)? state.testList.split(","): new String[0];
			username = state.username;
			password = state.password;
			sessionStart = state.sessionStart;
			if (tests!=null && !tests.isEmpty()) {
				tests.clear();
			} else {
				tests = new ArrayList<String>();
			}
			for (String s: rawtests) {
				tests.add(s);
			}
		}
	}
	/*
	 * 
	// never used
    private void initAccountChoiceCache() {
    	ArrayList<AlertMeStorage.AlertMeUser> userList = db.getUsers();
    	int accSize = (userList!=null && !userList.isEmpty())? userList.size(): 0;
    	accountNamesChoiceList = null;
    	accountIDsChoiceList = null;
    	if (accSize>1) {
    		int i = 0;
    		accountNamesChoiceList = new String[accSize];
    		accountIDsChoiceList = new long[accSize];
    		// Alpha sort things..
    		Collections.sort(userList, AlertMeStorage.AlertMeUser.getComparator(false));
    		for (AlertMeStorage.AlertMeUser user: userList) {
    			accountNamesChoiceList[i] = user.username;
    			accountIDsChoiceList[i] = user.id;
    			i++;
    		}
    	}
    }
	 */
	
	private void updateText(TextView textfield, String s) {
		if (textfield!=null) {
			textfield.setText(s);
		}
	}
	private void performUpdate(int mesgType, String mesgData) {
		if (mesgData!=null) {
			updateText(title, getString(R.string.test_complete_title));
			updateText(body, mesgData);
		} else {
			updateText(body, getString(R.string.test_complete_failed_title));
		}
		enableResults();
		screenStuff.setNotBusy();
	}
	class DebugStarter extends Thread {
		private Handler handler = null;
		private AlertMeServer alertme = null;
		private ArrayList<String> tests = new ArrayList<String>();
		private String login = null;
		private String passwd = null;
		private String[] accServiceList = null;
		private ArrayList<Hub> hubs = null;
		private ArrayList<Device> devices = null;
		private HashMap<String, Device> deviceTypes = null;
		
		public DebugStarter(Handler h, String user, String pass) {
			login = user;
			passwd = pass;
			alertme = new AlertMeServer();
        	handler = h;
		}
		public void addTest(String t) {
			tests.add(t);
		}
		
        @Override
        public void run() {
        	String sessionKey = alertme.login(login, passwd);
        	String data = performTests(sessionKey);
    		if (handler!=null) {
    			Message msg = handler.obtainMessage();
                Bundle b = new Bundle();
                b.putInt("type", AlertMeConstants.UPDATE_ALL);
                b.putString("value", data);
                msg.setData(b);
                handler.sendMessage(msg);
    		}
        }
        private String performTests(String sessionKey) {
        	String res = "NOTHING";
        	if (sessionKey!=null && sessionKey.length()!=0) {
        		String tmp = "";
        		for (String s: tests) {
        			String t = doTest(s, sessionKey);
        			if (t!=null) { tmp += t; }
        		}
        		if (tmp!=null) res = tmp;
        		res += "logout()>>\n";
        		res += alertme.logout(sessionKey);
        		res += "\n-------------------\n";
        		res += "-- END TESTS --";
        	}
        	return res;
        }
        private String doTest(String s, String sessionKey) {
        	String res = "";
        	
        	if (s.equals("1")) {
        		res = performSessionKeyTests(s);
        	} else if (s.equals("2")) {
        		res = performHubTests(sessionKey);
        		
        	} else if (s.equals("3")) {
        		res = performDeviceTests(sessionKey);
        		
        	} else if (s.equals("4")) {
        		res = performEventTests(sessionKey);
        		
        	}
        	
        	return res;
        }

        private void setServiceList(String serviceList) {
    		if (APIUtilities.isStringNonEmpty(serviceList)) {
    			if (serviceList.contains(",")) {
    				accServiceList = serviceList.split(",");
    			} else {
    				accServiceList = new String[] { serviceList };
    			}
    		}
        }
        private void setHubList(String hubList) {
        	hubs = APIUtilities.getAllHubs(hubList);
        }
        private void setDeviceList(String deviceList) {
        	devices = APIUtilities.getAllDevices(deviceList);
        	
        	if (devices!=null && !devices.isEmpty()) {
    			if (deviceTypes==null) {
    				deviceTypes = new HashMap<String, Device>();
    			}        		
        		for (Device d: devices) {
        			String dt = Device.getTypeFromInt(d.type);
        			if (!deviceTypes.containsKey(dt)) {
        				deviceTypes.put(dt, d);
        			}
        		}
        	}
        	
        }
        private String performSessionKeyTests(String sessionKey) {
        	String res = "";
    		res = "--STARTING TEST 1 ["+sessionKey+"]--\n";
    		res += "getUserInfo()>>\n";
    		res += alertme.getUserInfo(sessionKey);
    		res += "\n-------------------\n";
    		
    		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, res);
        	return res;
        }
        private String performHubTests(String sessionKey) {
        	String res = "";
    		String serviceList = "";
    		String hubList = "";
    		res = "--STARTING TEST 2 ["+sessionKey+"]--\n";
    		res += "getAllHubs()>>\n";
    		
    		hubList = alertme.getAllHubs(sessionKey);
    		setHubList(hubList);
    		
    		res += hubList;
    		res += "\n-------------------\n";
    		
    		if (hubs!=null && !hubs.isEmpty()) {
    			String hubId = hubs.get(0).id;
    			res += "setHub("+hubId+")>>\n";
        		res += alertme.setHub(sessionKey, hubId);
    		} else {
	    		res += "setHub(HUBID)>>\n";
        		res += "FAILED:: could not retrieve a hub list"; 	
    		}
    		res += "\n-------------------\n";    		
    		res += "getAllBehaviours()>>\n";
    		res += alertme.getAllBehaviours(sessionKey);
    		res += "\n-------------------\n"; 		
    		res += "getBehaviour()>>\n";
    		res += alertme.getBehaviour(sessionKey);
    		res += "\n-------------------\n";
    		res += "getHubStatus()>>\n";
    		res += alertme.getHubStatus(sessionKey);
    		res += "\n-------------------\n";
			res += "getAllDeviceChannelValues()>>\n";
    		res += alertme.getAllDeviceChannelValues(sessionKey);
    		res += "\n-------------------\n";
    		res += "getAllServices>>\n";
    		
    		serviceList = alertme.getAllServices(sessionKey);
    		setServiceList(serviceList);
    		
    		res += serviceList;
    		res += "\n-------------------\n";
			if (accServiceList!=null) {
				for (String s: accServiceList) {
		    		res += "getAllServiceStates("+s+")>>\n";
		    		res += alertme.getAllServiceStates(sessionKey, s);
		    		res += "\n-------------------\n";
				}
	    		for (String s: accServiceList) {
					res += "getCurrentServiceState('"+s+"')>>\n";
	        		res += alertme.getCurrentServiceState(sessionKey, s);
	        		res += "\n-------------------\n";
	    		}
				
			} else {
	    		res += "getAllServiceStates(SERVICE)>>\n";
        		res += "FAILED:: could not retrieve a service list";        			
        		res += "\n-------------------\n";
	    		res += "getCurrentServiceState(SERVICE)>>\n";
        		res += "FAILED:: could not retrieve a service list";        			
    		}
    		res += "\n-------------------\n";
    		
    		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, res);
        	return res;
        }
        private String performDeviceTests(String sessionKey) {
        	String res = "";
    		String deviceList = "";
    		res = "--STARTING TEST 3 ["+sessionKey+"]--\n";
    		res += "getAllDevices()>>\n";
    		
    		deviceList = alertme.getAllDevices(sessionKey);
    		setDeviceList(deviceList);
    		
    		res += deviceList;
    		res += "\n-------------------\n";

    		if (deviceTypes!=null && !deviceTypes.isEmpty()) {
    			for(String type: deviceTypes.keySet()) {
    				Device d = deviceTypes.get(type);
    				res += "getAllDeviceChannels("+d.id+" [type: "+type+"])>>\n";
            		res += alertme.getAllDeviceChannels(sessionKey, d.id);
            		res += "\n-------------------\n";
    			}
    			for(String type: deviceTypes.keySet()) {
    				Device d = deviceTypes.get(type);
    	    		res += "getDeviceDetails("+d.id+" [type: "+type+"])>>\n";
    	    		res += alertme.getDeviceDetails(sessionKey, d.id);    
    	    		res += "\n-------------------\n";
    			}
    			for(String type: deviceTypes.keySet()) {
    				Device d = deviceTypes.get(type);
    	    		res += "getDeviceChannelValue("+d.id+" [type: "+type+"], channel=null)>>\n";
    	    		res += alertme.getDeviceChannelValue(sessionKey, d.id);    
    	    		res += "\n-------------------\n";
    			}
    		} else {
	    		res += "getAllDeviceChannels(DEVICEID)>>\n";
        		res += "FAILED:: could not retrieve a device list";        			
        		res += "\n-------------------\n";
	    		res += "getDeviceDetails(DEVICEID)>>\n";
        		res += "FAILED:: could not retrieve a device list";        			
        		res += "\n-------------------\n";
	    		res += "getDeviceChannelValue(DEVICEID)>>\n";
        		res += "FAILED:: could not retrieve a device list";        			
        		res += "\n-------------------\n";
    		}

    		// DEVICE CHANNEL LOG IS TITCHY!
    		//res += alertme.getDeviceChannelLog(sessionKey, deviceId, att, 3, firstTime, lastTime);
    		
    		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, res);
        	return res;
        }
        private String performEventTests(String sessionKey) {
        	String res = "";
        	String start = "null";
        	String end = "null";
        	long now = System.currentTimeMillis();
        	Time yestTime = new Time();
        	String nowFormat = (now*1000)+"";
        	String yestFormat = "";
        	int limit = 3;
        	long yesMilli = 1000*60*60*24;
        	yestTime.set(now-yesMilli);
        	yestFormat = (yestTime.toMillis(false)*1000)+"";
    		res = "--STARTING TEST 4 ["+sessionKey+"]--\n";
    		
    		
    		if (accServiceList==null) {
        		String serviceList = alertme.getAllServices(sessionKey);
        		setServiceList(serviceList);
    		}
    		if (accServiceList!=null) {
    			for (String service: accServiceList) {
    				res += "getAllDeviceChannelValues("+service+", limit:"+limit+", start:"+start+", end:"+end+")>>\n";
    	    		res += alertme.getEventLog(sessionKey, service, limit, start, end);
    	    		res += "\n-------------------\n";    				
    			}
    			start = "1212131517";
    			for (String service: accServiceList) {
    				res += "getAllDeviceChannelValues("+service+", limit:"+limit+", start:"+start+", end:"+end+")>>\n";
    	    		res += alertme.getEventLog(sessionKey, service, limit, start, end);
    	    		res += "\n-------------------\n";    				
    			}
    			start = "null";
    			end = nowFormat;
    			for (String service: accServiceList) {
    				res += "getAllDeviceChannelValues("+service+", limit:"+limit+", start:"+start+", end:"+end+")>>\n";
    	    		res += alertme.getEventLog(sessionKey, service, limit, start, end);
    	    		res += "\n-------------------\n";    				
    			}
    			start = yestFormat;
    			end = nowFormat;
    			for (String service: accServiceList) {
    				res += "getAllDeviceChannelValues("+service+", limit:"+limit+", start:"+start+", end:"+end+")>>\n";
    	    		res += alertme.getEventLog(sessionKey, service, limit, start, end);
    	    		res += "\n-------------------\n";    				
    			}
    			start = yestFormat;
    			end = nowFormat;
    			limit=-1;
    			for (String service: accServiceList) {
    				res += "getAllDeviceChannelValues("+service+", limit:"+limit+", start:"+start+", end:"+end+")>>\n";
    	    		res += alertme.getEventLog(sessionKey, service, limit, start, end);
    	    		res += "\n-------------------\n";    				
    			}
    		} else {
	    		res += "getEventLog(SERVICE)>>\n";
        		res += "FAILED:: could not retrieve a service list";        			
        		res += "\n-------------------\n";
        		res += "FAILED getAllDeviceChannelValues(SERVICE, limit:"+limit+", start:"+start+", end:"+end+")>>\n";
	    		res += "\n-------------------\n";    				
    			start = "1212131517";
				res += "getAllDeviceChannelValues(SERVICE, limit:"+limit+", start:"+start+", end:"+end+")>>\n";
	    		res += "\n-------------------\n";    				
    			start = "null";
    			end = nowFormat;
				res += "getAllDeviceChannelValues(SERVICE, limit:"+limit+", start:"+start+", end:"+end+")>>\n";
	    		res += "\n-------------------\n";    				
    			start = yestFormat;
    			end = nowFormat;
				res += "getAllDeviceChannelValues(SERVICE, limit:"+limit+", start:"+start+", end:"+end+")>>\n";
	    		res += "\n-------------------\n";    				
    			start = yestFormat;
    			end = nowFormat;
    			limit=-1;
				res += "getAllDeviceChannelValues(SERVICE, limit:"+limit+", start:"+start+", end:"+end+")>>\n";
	    		res += "\n-------------------\n";    				    			
    		}
    		
    		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, res);
        	return res;
        }

	}
	
	public static class AMTestState {
		public String username = null;
		public String password = null;
		public String testList = null;
		public long sessionStart = 0;
	}

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