package org.darkgoddess.alertme.api.utils;

public class DeviceEvent extends Event {
	public String zIdLabel = null;
	public String loggedId = null;
	public String typeLabel = null;
	public String loggedType = null;
	//1297278581|zigbeeId|00-0D-6F-00-00-1B-61-DD|devType|AMKeyFob|Yvan Seth's Keyfob has arrived home

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
			return zIdLabel+"|"+loggedId+"|"+typeLabel+"|"+loggedType+"|"+message;
		}
		return null;
	}
}

