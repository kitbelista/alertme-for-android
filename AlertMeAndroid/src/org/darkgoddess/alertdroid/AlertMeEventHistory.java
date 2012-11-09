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

package org.darkgoddess.alertdroid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import org.darkgoddess.alertdroid.api.AlertMeServer;
import org.darkgoddess.alertdroid.api.AlertMeSession;
import org.darkgoddess.alertdroid.api.utils.Device;
import org.darkgoddess.alertdroid.api.utils.DeviceEvent;
import org.darkgoddess.alertdroid.api.utils.Event;
import org.darkgoddess.alertdroid.api.utils.Hub;
import org.darkgoddess.alertdroid.R;
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class AlertMeEventHistory extends Activity {
	private static final String TAG = "ACTIVITY:AlertMeEventHistory";
	private Bundle savedState = null;
	private AMViewItems screenStuff = null;
	private AlertMeSession alertMe = null;
	private ListView eventList = null;
	private ArrayList<Event> events = null;
	private boolean isActive = false;
	private boolean hasCreated = false;
	private boolean createdList = false;
	private int[] rowBg = null;
	private int rowBgLen = 0;

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
		isActive = true;
		setContentView(R.layout.alertme_events);
		loadFromRestoredState();
		if (!hasCreated) {
			hasCreated = true;
			alertMe = (alertMe==null)? new AlertMeSession(this): alertMe;
			initView();
			savedState = savedInstanceState;
		}
		rowBg = AMViewItems.getRowBackgrounds(this);
		if (rowBg!=null) rowBgLen = rowBg.length;
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
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.events, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        	case R.id.menu_events_refresh:
    			SharedPreferences sharedPrefs = getSharedPreferences(AlertMeConstants.PREFERENCE_NAME, AlertMeConstants.PREFERENCE_MODE);
    			invokeEventLoad(null, sharedPrefs);
        		return true;
        	case R.id.menu_events_clear:
        		events = null;
        		alertMe.flushEventData();
        		performUpdate(AlertMeConstants.UPDATE_ALL, null);
        		Toast.makeText(getApplicationContext(), getString(R.string.history_cleared), Toast.LENGTH_SHORT).show();
        		return true;
	        default:
	            return super.onOptionsItemSelected(item);
        }
    }
    
	private void initView() {
		eventList = (ListView) findViewById(R.id.events_list);
	}
	private void invokeEventLoad(Bundle savedInstanceState, SharedPreferences sharedPrefs) {
		EventListStarter listloader = new EventListStarter(alertMe, handler, getIntent(), savedInstanceState, sharedPrefs);
		screenStuff.setBusy(AlertMeConstants.INVOKE_HISTORY);
		listloader.start();
	}
	private void loadEventList(Bundle savedInstanceState) {
		// Create the list with adapters here 
		if (events==null) {
			SharedPreferences sharedPrefs = getSharedPreferences(AlertMeConstants.PREFERENCE_NAME, AlertMeConstants.PREFERENCE_MODE);
			invokeEventLoad(savedInstanceState, sharedPrefs);
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
        		String dd = ts.monthDay+"";
        		String dateStamp = (dd.length()<2)? ts.year+"|"+mm+"|0"+dd: ts.year+"|"+mm+"|"+dd;
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
    private void initNonEmptyEventList() {
		Time currentTime = new Time();
		Time currentDate = new Time();
		SeparatedListAdapter adapter;
		long currentDateTs = 0;
		//EventAdapter eventAdapter;
		//Collections.sort(events, getEventSort(false));
		HashMap<String, ArrayList<Event>> dateStamps = getDatesFromEventList();
		ArrayList<String> sortedDates = new ArrayList<String> (dateStamps.keySet());
		Collections.sort(sortedDates, Event.getComparatorForStringEpoch(true));
		// more complex plz...
		//eventAdapter = new EventAdapter(this, R.layout.alertme_events_row, events);
		//eventList.setAdapter(eventAdapter);
		currentTime.setToNow();
    	currentDateTs = currentTime.toMillis(true) - (currentTime.second*1000) - (currentTime.minute*1000*60) - (currentTime.hour*1000*60*60) ;
    	currentDate.set(currentDateTs);
		adapter = new SeparatedListAdapter(this, R.layout.list_header);
		if (AlertMeConstants.DEBUGOUT) Log.w(TAG, "performUpdate()   command: update non-empty event list ("+events.size()+" entries) withint "+sortedDates.size()+" dates");
		for(String date: sortedDates) {
			ArrayList<Event> elist = dateStamps.get(date);
			if (elist!=null && !elist.isEmpty()) {
				String title = AlertMeConstants.getDateTitle(currentDate, elist.get(0).getTime());
				EventAdapter eventAdapter;
				Collections.sort(elist, getEventSort(true));
				eventAdapter = new EventAdapter(this, R.layout.alertme_events_row, elist);
    			adapter.addSection(title, eventAdapter);
			}
		}
		eventList.setAdapter(adapter);
    }
    private void initEmptyEventList() {
		// empty
		ArrayAdapter<String> emptyList;
		String[] empty = { getString(R.string.event_list_isempty) }; 
		emptyList = new ArrayAdapter<String>(this, R.layout.alertme_listempty, empty);
		eventList.setAdapter(emptyList);
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
			if (hub!=null) {    				
        		screenStuff.setSystemName(hub);    			
			}
    		if (!createdList) {
        		if (events!=null && !events.isEmpty()) {
        			initNonEmptyEventList();  
        			createdList = true;      			
        		} else {
        			initEmptyEventList();
        		}
    		} else {
				eventList.setAdapter(null);
    			if (events!=null && !events.isEmpty()) {
    				initNonEmptyEventList();
    			} else {
    				initEmptyEventList();
    			}
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
        
        public void appendEvent(Event newEvent) {
        	if (items!=null) {
        		if (!items.isEmpty()) {
        			// check if the event already exists
        			boolean insertOk = false;
        			boolean eventExists = false;
        			int sz = items.size();
        			// items are in reverse chrono
        			for (int i=0; i<sz; i++) {
        				Event e = items.get(i);
        				if (newEvent.epochTimestamp==e.epochTimestamp) {
        					// skip if the event is the same..
        					String nStr = newEvent.getEventString();
        					String eStr = e.getEventString();
        					if (!(nStr!=null && nStr.equals(eStr))) {
        						// add here if event is different
        						insertOk = true;
        					} else {
        						eventExists = true;
        					}
        				} else if (newEvent.epochTimestamp>e.epochTimestamp) {
    						// add here!    
    						insertOk = true;    					
        				}
        				if (insertOk) {
    						items.add(i, newEvent);
        					break;
        				}
        				if (eventExists) {
        					break;
        				}
        			}
        			if (!insertOk && !eventExists) {
        				items.add(sz, newEvent);
        			}
        		} else {
            		items.add(0, newEvent);
        		}
        	} else {
        		items = new ArrayList<Event>();
        		items.add(0, newEvent);
    		}
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
                if (rowBg!=null) {
                	int colorPos = position % rowBgLen;
                	v.setBackgroundResource(rowBg[colorPos]);
                	//v.setBackgroundColor(rowBg[colorPos]);
                }
                return v;
        }
        private String getTidiedMessage(String eventMessage) {
        	String res = eventMessage.trim();
        	String appName = AlertMeServer.LOGIN_TAG;
        	if (res.startsWith(AlertMeConstants.EVENT_START_DISARMEDBY)) {
        		res = res.replace(AlertMeConstants.EVENT_START_DISARMEDBY, AlertMeConstants.EMPTY_STR);
        		res = res.concat(getString(R.string.eventlist_end_disarmedby));
        	} else if (res.startsWith(AlertMeConstants.EVENT_START_ARMEDBY)) {
        		res = res.replace(AlertMeConstants.EVENT_START_ARMEDBY, AlertMeConstants.EMPTY_STR);
        		res = res.concat(getString(R.string.eventlist_end_armedby));
        	} else if (res.equals(AlertMeConstants.EVENT_START_DISARMEDBY+appName)) {
        		res = appName + getString(R.string.eventlist_end_disarmedby);
        	} else if (res.equals(AlertMeConstants.EVENT_START_DISARMEDBY+appName)) {
        		res = appName + getString(R.string.eventlist_end_armedby);
        	}
        	if (res.contains(AlertMeConstants.EVENT_KEYFOB_OWNED)) {
        		res = res.replace(AlertMeConstants.EVENT_KEYFOB_OWNED, AlertMeConstants.EMPTY_STR);
        	}
        	if (res.contains(AlertMeConstants.EVENT_MODE_CHANGE)) {
        		res = res.replace(AlertMeConstants.EVENT_MODE_CHANGE, getString(R.string.eventlist_mode_change));
        	}
        	
        	return res;
        }
        private int getIconFromEventMessage(String eventMessage) {
        	return AlertMeConstants.getIconFromEventMessage(AlertMeServer.LOGIN_TAG, eventMessage);
        }
        private String getRawTypeFormated(String input) {
        	String res = (input!=null)? input.trim(): null;
        	if (res!=null && res.startsWith(AlertMeConstants.STR_AM)) {
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
                b.putInt(AlertMeConstants.HANDLER_DATA_TYPE, AlertMeConstants.UPDATE_ALL);
                msg.setData(b);
                handler.sendMessage(msg);
    		}        		
        }
		
	}
	
}

