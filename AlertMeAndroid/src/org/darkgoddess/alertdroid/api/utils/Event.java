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

package org.darkgoddess.alertdroid.api.utils;

import java.util.Comparator;
import java.util.Date;

import android.text.format.Time;

public class Event {
	public long epochTimestamp = 0; // epoch time in seconds
	public String message = null;
	public Event() {}
	public Event(final long timestamp, final String mesg) {
		epochTimestamp = timestamp;
		message = mesg;
	}
	public String getEventString() {
		String res = null;
		if (epochTimestamp>0) {
			res = epochTimestamp+"||"+message;
		}
		return res;
	}
	public Date getDate() {
		Date res = null;

		if (epochTimestamp>0) {
			res = new Date(epochTimestamp);			
		}

		return res;
	}
	public Time getTime() {
		Time res = null;

		if (epochTimestamp>0) {
			Time t = new Time();
			t.set(epochTimestamp*1000);
			//if (Time.isEpoch(t)) {
				res = t;
			//}
		}
		
		return res;
	}
	public String getMessage() {
		return message;
	}
	public static Comparator<String> getComparatorForStringEpoch(boolean reverse) {
		Comparator<String> res = null;
		if (!reverse) {
			res = new Comparator<String>() {
				@Override
				public int compare(String d1, String d2) {
					return d1.compareTo(d2);
				}
			};
		} else {
			res = new Comparator<String>() {
				@Override
				public int compare(String d1, String d2) {
					return d2.compareTo(d1);
				}
			};
		}
		return res;
	}
	
	public static Comparator<Event> getComparator(boolean reverse) {
		Comparator<Event> res = null;
		if (!reverse) {
			res = new Comparator<Event>() {
				@Override
				public int compare(Event d1, Event d2) {
					long diff = d1.epochTimestamp-d2.epochTimestamp;
					if (diff==0) return 0;
					if (diff<0) { return -1; }
					return 1;
				}
			};
		} else {
			res = new Comparator<Event>() {
				@Override
				public int compare(Event d1, Event d2) {
					long diff = d2.epochTimestamp-d1.epochTimestamp;
					if (diff==0) return 0;
					if (diff<0) { return -1; }
					return 1;
				}
			};
		}
		
		return res;
	}
}

