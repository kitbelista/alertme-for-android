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

package org.darkgoddess.alertme.api.utils;


public class DeviceEvent extends Event {
	public String zIdLabel = null;
	public String loggedId = null;
	public String typeLabel = null;
	public String loggedType = null;

	public DeviceEvent(final long timestamp, final String zid, final String lid, final String tl, final String lty, final String mesg) {
		super(timestamp, mesg);
		zIdLabel = zid;
		loggedId = lid;
		typeLabel = tl;
		loggedType = lty;
	}
	@Override
	public String getEventString() {
		String res = null;
		if (epochTimestamp>0) {
			res = epochTimestamp+"|"+getMessage();
		}
		return res;
	}
	@Override
	public String getMessage() {
		if (epochTimestamp>0) {
			if (loggedId.contains("| ")) return zIdLabel+"|[("+loggedId+")]|"+typeLabel+"|"+loggedType+"|"+message;
			return zIdLabel+"|"+loggedId+"|"+typeLabel+"|"+loggedType+"|"+message;
		}
		return null;
	}
}

