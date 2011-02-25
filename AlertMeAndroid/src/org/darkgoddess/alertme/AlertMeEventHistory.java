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
import java.util.Comparator;
import java.util.HashMap;

import org.darkgoddess.alertme.api.AlertMeServer;
import org.darkgoddess.alertme.api.AlertMeSession;
import org.darkgoddess.alertme.api.utils.Device;
import org.darkgoddess.alertme.api.utils.DeviceEvent;
import org.darkgoddess.alertme.api.utils.Event;
import org.darkgoddess.alertme.api.utils.Hub;
import org.darkgoddess.android.utils.SeparatedListAdapter;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class AlertMeEventHistory extends Activity {
	private static final String TAG = "ACTIVITY:AlertMeEventHistory";
	private Bundle savedState = null;
	private AMViewItems screenStuff = null;
	private AlertMeSession alertMe = null;
	private ListView eventList = null;
	private ArrayList<Event> events = null;
	private boolean isActive = false;
	private boolean hasCreated = false;

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
		isActive = true;
		setContentView(R.layout.alertme_events);
		loadFromRestoredState();
		if (!hasCreated) {
			hasCreated = true;
			alertMe = (alertMe==null)? new AlertMeSession(this): alertMe;
			initView();
			savedState = savedInstanceState;
		}
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onCreate()   END");
	}
	@Override
    public void onStart() {
		super.onStart();

		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onStart()   START");
		loadFromRestoredState();
		if (screenStuff==null) {
			screenStuff = new AMViewItems(this, this);
			screenStuff.registerSystemName(R.id.events_housename);
			loadEventList(savedState);
		}
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onStart()   END");
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
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onRetainNonConfigurationInstance()  START");
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "onRetainNonConfigurationInstance()  END");
		return alertMe.retrieveCurrentState();
	}
	@Override
	public void finish() {
		isActive = false;
		alertMe.clean();
		super.finish();
		
	}
	private void initView() {
		eventList = (ListView) findViewById(R.id.events_list);
	}
	private void loadEventList(Bundle savedInstanceState) {
		SharedPreferences sharedPrefs = getSharedPreferences(AlertMeConstants.PREFERENCE_NAME, AlertMeConstants.PREFERENCE_MODE);
		EventListStarter listloader;

		// Create the list with adapters here 
		if (events==null) {
			listloader = new EventListStarter(alertMe, handler, getIntent(), savedInstanceState, sharedPrefs);
			screenStuff.setBusy(AlertMeConstants.INVOKE_HISTORY);
			listloader.start();
		} else {
			performUpdate(AlertMeConstants.UPDATE_ALL, null);
		}
	}
    private Comparator<Event> getEventSort(boolean doReverseChrono) {
    	Comparator<Event> res = null;
    	if (doReverseChrono) {
        	res = new Comparator<Event>() {
    			@Override
    			public int compare(Event e1, Event e2) {
    				int res = 0;
    				long diff = e2.epochTimestamp - e1.epochTimestamp;
    				if (diff!=0) {
    					res = (diff<0)? -1: 1;
    				}
    				return res; 
    			}
    		};
    	} else {
        	res = new Comparator<Event>() {
    			@Override
    			public int compare(Event e1, Event e2) {
    				int res = 0;
    				long diff = e1.epochTimestamp - e2.epochTimestamp;
    				if (diff!=0) {
    					res = (diff<0)? -1: 1;
    				}
    				return res;
    			}
    		};	
    	}
    	return res;
    }
    private HashMap<String, ArrayList<Event>> getDatesFromEventList() {
    	HashMap<String, ArrayList<Event>> dateMap = new HashMap<String, ArrayList<Event>>();
    	
    	if (events!=null && !events.isEmpty()) {
        	for(Event e: events) {
        		ArrayList<Event> elist = null;
        		Time ts = e.getTime();
        		int mm = ts.month+1;
        		String dateStamp = ts.year+"|"+mm+"|"+ts.monthDay;
        		if (!dateMap.containsKey(dateStamp)) {
        			elist = new ArrayList<Event>();
        			//dateMap.put(dateStamp, ts);
        			elist.add(e);
        			dateMap.put(dateStamp, elist);
        		} else {
        			dateMap.get(dateStamp).add(e);
        		}
        	}
    	}
    	
    	return dateMap;
    }
    private void performUpdate(int command, final String mesgData) {
    	if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate("+command+", '"+mesgData+"')   START");
    	if (!isActive) {
    		// Not active when onCreate is not done OR finish() is called
    		screenStuff.setNotBusy();
    		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()   END (premature - not active)");
    		return;
    	}
    	// Update list with the events found
    	if (command == AlertMeConstants.UPDATE_ALL) {
    		Hub hub = alertMe.retrieveActiveHub();

    		if (events!=null && !events.isEmpty()) {
    			SeparatedListAdapter adapter;
    			//EventAdapter eventAdapter;
    			//Collections.sort(events, getEventSort(false));
    			HashMap<String, ArrayList<Event>> dateStamps = getDatesFromEventList();
    			ArrayList<String> sortedDates = new ArrayList<String> (dateStamps.keySet());
    			Collections.sort(sortedDates, Event.getComparatorForStringEpoch(true));
    			// more complex plz...
    			//eventAdapter = new EventAdapter(this, R.layout.alertme_events_row, events);
    			//eventList.setAdapter(eventAdapter);
    			
    			adapter = new SeparatedListAdapter(this, R.layout.list_header);
    			if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()   command: update non-empty event list ("+events.size()+" entries) withint "+sortedDates.size()+" dates");
				for(String date: sortedDates) {
    				ArrayList<Event> elist = dateStamps.get(date);
    				if (elist!=null && !elist.isEmpty()) {
    					String title = elist.get(0).getTime().format("%Y%m%d");
    					EventAdapter eventAdapter;
    					Collections.sort(elist, getEventSort(true));
    					eventAdapter = new EventAdapter(this, R.layout.alertme_events_row, elist);
            			adapter.addSection(title, eventAdapter);
    				}
    			}
    			eventList.setAdapter(adapter);
    			
    			if (hub!=null) {    				
            		screenStuff.setSystemName(hub);    			
    			}
    		} else {
    			// empty
        		ArrayAdapter<String> emptyList;
    			String[] empty = { getString(R.string.event_list_isempty) }; 
    			emptyList = new ArrayAdapter<String>(this, R.layout.alertme_listempty, empty);
    			eventList.setAdapter(emptyList);
    		}
    	}
    	screenStuff.setNotBusy();
    	if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()   END");
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
			if (reloaded) {
				events = alertMe.retrieveEvents();
			}
			if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "loadFromRestoredState()  did reload from old state:"+reloaded);
		}
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "loadFromRestoredState()  END");
	}
	
    
    class EventAdapter extends ArrayAdapter<Event> {
		private ArrayList<Event> items;

        public EventAdapter(Context context, int textViewResourceId, ArrayList<Event> eventList) {
                super(context, textViewResourceId, eventList);
                items = eventList;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = vi.inflate(R.layout.alertme_events_row, null);
                }
                Event e = items.get(position);
                if (e != null) {
                    TextView type = (TextView) v.findViewById(R.id.eventline_type);
            		ImageView typeIcon = (ImageView) v.findViewById(R.id.eventline_type_icon);
                    TextView mesg = (TextView) v.findViewById(R.id.eventline_message);
                    TextView time = (TextView) v.findViewById(R.id.eventline_time);
                	
                    // Check if the event is a device event.. then disable if not
                    if (e instanceof DeviceEvent) {
                    	DeviceEvent de = (DeviceEvent) e;
                    	String dtype = getRawTypeFormated(de.loggedType);
                        if (type!=null) {
                        	type.setText(dtype);
                        }	
                        if (typeIcon!=null) {
                        	typeIcon.setImageResource(AlertMeConstants.getTypeIcon(Device.getTypeFromString(dtype)));
                        }
                    	
                    } else {
                    	int resource = getIconFromEventMessage(e.message);
                        if (type!=null) {
                        	type.setVisibility(View.GONE);
                        }	
                    	if (resource==0) {
                            if (typeIcon!=null) {
                            	typeIcon.setVisibility(View.GONE);
                            }
                    	} else {
                            if (typeIcon!=null) {
                            	typeIcon.setImageResource(getIconFromEventMessage(e.message));
                            }                    		
                    	}
                    }

                    if (mesg!=null) {
                    	mesg.setText(getTidiedMessage(e.message));
                    }	
                    if (time!=null) {
                    	Time ts = e.getTime();
                    	if (ts!=null) {
                        	time.setText(ts.format("%H:%M:%S"));
                    	} else {
                    		time.setText("");
                    	}
                    }
                }
                return v;
        }
        private String getTidiedMessage(String eventMessage) {
        	String res = eventMessage.trim();
        	String appName = AlertMeServer.LOGIN_TAG;
        	if (res.startsWith("The Intruder Alarm was disarmed by ")) {
        		res = res.replace("The Intruder Alarm was disarmed by ", "");
        		res = res.concat(" disarmed the alarm");
        	} else if (res.startsWith("The Intruder Alarm was armed by ")) {
        		res = res.replace("The Intruder Alarm was armed by ", "");
        		res = res.concat(" armed the alarm");
        	} else if (res.equals("The Intruder Alarm was disarmed from "+appName)) {
        		res = appName + " disarmed the alarm";
        	} else if (res.equals("The Intruder Alarm was armed from "+appName)) {
        		res = appName + " armed the alarm";
        	}
        	if (res.contains("'s Keyfob")) {
        		res = res.replace("'s Keyfob", "");
        	}
        	if (res.contains("Behaviour changed to ")) {
        		res = res.replace("Behaviour changed to ", "Mode changed to ");
        	}
        	
        	return res;
        }
        private int getIconFromEventMessage(String eventMessage) {
        	int res = 0;
        	String appName = AlertMeServer.LOGIN_TAG;
        	String mesg = eventMessage.trim();
        	if (mesg.equals("Behaviour changed to At home")) {
        		res = R.drawable.ic_sensor_hub;        		
        	} else if (mesg.equals("Behaviour changed to Away")) {
        		res = R.drawable.ic_sensor_hub;
        	} else if (mesg.equals("Behaviour changed to Night")) {
        		res = R.drawable.ic_sensor_hub;
        	} else if (mesg.equals("The hub disappeared from network")) {
        		res = R.drawable.ic_home_sensors_notok;
        	} else if (mesg.equals("The Intruder Alarm was disarmed from "+appName)) {
        		res = R.drawable.icon;
        	} else if (mesg.equals("The Intruder Alarm was armed from "+appName)) {
        		res = R.drawable.icon;
        	}
        	//The Intruder Alarm was set off by Front Hall Motion Sensor	22:06
        	//The Front Door Door/Window Sensor was triggered. The alarm will be raised if further triggers are detected
        	
        	return res;
        }
        private String getRawTypeFormated(String input) {
        	String res = (input!=null)? input.trim(): null;
        	if (res!=null && res.startsWith("AM")) {
        		res = res.substring(2);
        	}
        	return res;
        }
    }
	class EventListStarter extends Thread {
		public boolean forceLogin = true;
		public int instruction = -1;
		private AlertMeSession alertme;
		private Handler handler;
		private Intent intent;
		private Bundle bundle;
		private SharedPreferences sharedPrefs;

		public EventListStarter(AlertMeSession client, Handler handle, Intent intentIn, Bundle bundleIn, SharedPreferences prefs) {
			alertme = client;
			handler = handle;
			intent = intentIn;
			bundle = bundleIn;
			sharedPrefs = prefs;
		}
        @Override
        public void run() {
      		boolean hasCurrentSys = alertme.hasSessionValues();
      		if (!hasCurrentSys) hasCurrentSys = alertme.loadFromIntentBundle(intent, bundle);
    		if (!hasCurrentSys) hasCurrentSys = alertme.loadFromPreference(sharedPrefs);
        	//if (alertme!=null && !alertme.hasValidLogin()) {
        	//	alertMe.setActiveSystemDefault();
        	//}
        	if (hasCurrentSys) {
    			events = alertme.getEventData();
	    		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "EVENTLISTSTARTER::Thread run()  event list size :"+events.size());
    		}

    		if (handler!=null) {
    			Message msg = handler.obtainMessage();
                Bundle b = new Bundle();
                b.putInt("type", AlertMeConstants.UPDATE_ALL);
                msg.setData(b);
                handler.sendMessage(msg);
    		}        		
        }
		
	}
	
}

