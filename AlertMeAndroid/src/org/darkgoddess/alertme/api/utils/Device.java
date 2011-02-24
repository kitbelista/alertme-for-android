package org.darkgoddess.alertme.api.utils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;


public class Device implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8311905524725950977L;
	public static final int UNKNOWN = 0;
	public static final int ALARM_DETECTOR = 1;
	public static final int BUTTON = 2;
	public static final int CAMERA = 3;
	public static final int CONTACT_SENSOR = 4;
	public static final int KEYFOB = 5;
	public static final int LAMP = 6;
	public static final int MOTION_SENSOR = 7;
	public static final int POWER_CONTROLLER = 8;
	public static final int POWERCLAMP = 9;
	public String name = "";
	public String id = "";
	public String dtype = "";
	public int type = UNKNOWN;
	public double batteryLevel = 0;
	public HashMap<String, String> attributes = new HashMap<String, String>();
	public Device() {}
	public Device(final String label, final String dId, final String rawType) {
		name=label;
		id=dId;
		dtype=rawType;
		type = getTypeFromString(rawType);
	}
	public Device(final String label, final String dId, final int typeIn) {
		name=label;
		id=dId;
		type=typeIn;
		dtype=getTypeFromInt(typeIn);
	}
	public String getStringType() {
		return getTypeFromInt(type);
	}
	public static String getTypeFromInt(int input) {
		String res = "Unknown";
		switch(input) {
			case ALARM_DETECTOR:
				res = "Alarm Detector";
				break;
			case BUTTON:
				res = "Button";
				break;
			case CAMERA:
				res = "Camera Motion";
				break;
			case CONTACT_SENSOR:
				res = "Contact Sensor";
				break;
			case KEYFOB:
				res = "Keyfob";
				break;
			case LAMP:
				res = "Lamp";
				break;
			case MOTION_SENSOR:
				res = "Motion Sensor";
				break;
			case POWER_CONTROLLER:
				res = "Power Controller";
				break;
			case POWERCLAMP:
				res = "Powerclamp";
				break;
		}
		return res;
	}
	public static int getTypeFromString(final String input) {
		int res = UNKNOWN;
		String s = input.toLowerCase().trim();
		if (s.equals("button")) {
			res = BUTTON;
		} else if (s.equals("lamp")) {
			res = LAMP;
		} else if (s.matches("contact\\s*sensor")) {
			res = CONTACT_SENSOR;
		} else if (s.matches("key\\s*fob")) {
			res = KEYFOB;
		} else if (s.matches("motion\\s*sensor")) {
			res = MOTION_SENSOR;
		} else if (s.matches("key\\s*fob")) {
			res = KEYFOB;
		} else if (s.matches("alarm\\s*detector")) {
			res = ALARM_DETECTOR;
		} else if (s.matches("power\\s*controller")) {
			res = POWER_CONTROLLER;
		} else if (s.matches("power\\s*clamp")) {
			res = POWERCLAMP;
		} else if (s.matches("camera\\s*motion")) {
			res = CAMERA; // TODO:: get all strings
		}
		return res;
	}
	public String getAttribute(String attrKey) {
		String res = null;
		
		if (!attributes.isEmpty() && attributes.containsKey(attrKey)) {
			res = attributes.get(attrKey);
		}
		return res;
	}
	public void setAttribute(String attrKey, String value) {
		
		attributes.put(attrKey, value);
	}
	public void setAttributesFromString(String resStr) {
		attributes = getAttributesFromString(resStr);
		for (String key: attributes.keySet()) {
			if (key.equals("batterylevel")) {
				batteryLevel = getDoubleFromString(attributes.get(key));
			}
		}
	}
	public boolean isAttributeValid(String attributeKey) {
		boolean res = false;
		
		switch(type) {
			case ALARM_DETECTOR:
				res = keyInKeyList(attributeKey, alarmDetectorAttributes);
				break;
			case BUTTON:
				res = keyInKeyList(attributeKey, buttonAttributes);
				break;
			case CAMERA:
				res = keyInKeyList(attributeKey, cameraAttributes);
				break;
			case CONTACT_SENSOR:
				res = keyInKeyList(attributeKey, contactSensorAttributes);
				break;
			case KEYFOB:
				res = keyInKeyList(attributeKey, keyfobAttributes);
				break;
			case LAMP:
				res = keyInKeyList(attributeKey, lampAttributes);
				break;
			case MOTION_SENSOR:
				res = keyInKeyList(attributeKey, motionSensorAttributes);
				break;
			case POWER_CONTROLLER:
				res = keyInKeyList(attributeKey, powerPlugAttributes);
				break;
			case POWERCLAMP:
				res = keyInKeyList(attributeKey, meterAttributes);
				break;
			case UNKNOWN:
				res = true; // Don't know, pass them all!
				break;
		}
		
		return res;
	}
	public String getAttributesString() {
		return getAttributeString(attributes);
	}
	public static double getDoubleFromString(final String s) {
		double res = 0;
		try {
	         res = Double.valueOf(s.trim()).doubleValue();
	      } catch (NumberFormatException nfe) {
	      } catch (Exception e) {}
	      return res;
	}
	public static HashMap<String, String> getAttributesFromString(String resStr) {
		//Log.w("DEVICE:: ", "Attribute raw FIRST:"+resStr);
		HashMap<String, String> attrs = new HashMap<String, String>();
		if (resStr!=null) {
			//Log.w("DEVICE:: ", "Attribute raw:"+resStr);
			if (resStr.contains(",")) {
				String[] attArr = resStr.split(","); 
				String[] attTmp;
				for (String tmp: attArr) {
					//Log.w("DEVICE:: ", "Attribute line:"+tmp);
					attTmp = tmp.split("\\|");
					if (attTmp!=null && attTmp.length>=2) {
						//Log.w("DEVICE:: ", "Attribute line (first): "+attTmp[0]);
						attrs.put(attTmp[0].toLowerCase(), attTmp[1]);
					}
					//attTmp = tmp.split("|");
					//if (attTmp!=null && attTmp.length>=2) {
						//Log.w("DEVICE:: ", "Attribute line (second): "+attTmp[0]);
					//	attrs.put(attTmp[0].toLowerCase(), attTmp[1]);
					//}
				}
			} else {
				String[] attTmp = resStr.split("\\|");
				if (attTmp!=null && attTmp.length>=2) {
					attrs.put(attTmp[0].toLowerCase(), attTmp[1]);
				}
			}
		}
		return attrs;
	}
	public static String getAttributeString(HashMap<String, String> attrs) {
		String res = null;
		
		if (attrs!=null && !attrs.isEmpty()) {
			int i = 0;
			res = "";
			for (String key: attrs.keySet()) {
				String val = attrs.get(key);
				res += (i++==0)? "": ",";
				res  += key+"|"+val;
			}
		}
		
		return res;
	}
	
	public static Comparator<Device> getComparator(boolean reverse) {
		Comparator<Device> res = null;
		if (!reverse) {
			res = new Comparator<Device>() {
				@Override
				public int compare(Device d1, Device d2) {
					return d1.name.compareToIgnoreCase(d2.name); 
				}
			};
		} else {
			res = new Comparator<Device>() {
				@Override
				public int compare(Device d1, Device d2) {
					return d2.name.compareToIgnoreCase(d1.name); 
				}
			};
		}
		return res;
	}
	
	// Return 5 (highest) or 0 (none)
	public int getSignalLevel() {
		int res = 0;
		String lqiStr = attributes.get("lqi");
		if (lqiStr!=null && lqiStr.length()!=0) {
			try {
				int perCentVal = Integer.parseInt(lqiStr);
				if (perCentVal == 0) {
					res = 0;
				} else if (perCentVal< 20) {
					res = 1;
				} else if (perCentVal>= 20 && perCentVal< 40) {
					res = 2;
				} else if (perCentVal>= 40 && perCentVal< 60) {
					res = 3;
				} else if (perCentVal>= 60 && perCentVal< 80) {
					res = 4;
				} else {
					res = 5;
				}
			} catch (Exception e) {}
		}
		
		return res;
	}
	// Return 5 (highest) or 0 (none)
	public int getBatteryLevel() {
		int res = 0;
		
		// TODO: ratings need to vary for other types of sensors..
		if (batteryLevel==0) {
			res = 0;
		} else if (batteryLevel < 2.6) {
			res = 1; // 1 bar
		} else if (batteryLevel >= 2.6 && batteryLevel < 2.77) {
			res = 2; // 2 bar
		} else if (batteryLevel >= 2.77 && batteryLevel < 2.84) {
			res = 3; // 3 bar
		} else if (batteryLevel >= 2.84 && batteryLevel < 2.95) {
			res = 4; // 4 bar
		} else if (batteryLevel >= 2.95) {
			res = 5; //
		}
		return res;
	}
	

	private static boolean keyInKeyList(String key, String[] keyList) {
		boolean res = false;
		
		if (keyList!=null) {
			for (String s: keyList) {
				if (s.equals(key)) {
					res = true;
					break;
				}
			}
		}
		
		return res;
	}
	
	public static String[] alarmDetectorAttributes = {"presence", "tamper", "upgrade", "batterylevel", "lqi", "temperature"}; 	
	public static String[] buttonAttributes = {"presence", "tamper", "upgrade", "batterylevel", "lqi", "temperature"}; 	
	public static String[] cameraAttributes = {"presence", "tamper", "upgrade", "batterylevel", "lqi", "temperature"}; // TODO: not sure
	public static String[] contactSensorAttributes = {"presence", "tamper", "upgrade", "batterylevel", "lqi", "temperature"}; 
	public static String[] keyfobAttributes = {"presence", "tamper", "upgrade", "batterylevel", "lqi"}; 
	public static String[] lampAttributes = {"presence", "tamper", "upgrade", "batterylevel", "lqi"}; 
	public static String[] motionSensorAttributes = {"presence", "tamper", "upgrade", "batterylevel", "lqi", "temperature"}; 
	public static String[] powerPlugAttributes = {"presence", "tamper", "upgrade", "batterylevel", "lqi", "temperature", "remotecontrol", "relaystate", "mainsstate", "powerlevel"}; 
	public static String[] meterAttributes = {"presence", "tamper", "upgrade", "batterylevel", "lqi", "temperature", "remotemode", "relaystate", "mainsstate", "powerlevel"}; 


	   /**
	   * Always treat de-serialization as a full-blown constructor, by
	   * validating the final state of the de-serialized object.
	   */
	   private void readObject(
	     ObjectInputStream aInputStream
	   ) throws ClassNotFoundException, IOException {
	     //always perform the default de-serialization first
	     aInputStream.defaultReadObject();
	  }

	    /**
	    * This is the default implementation of writeObject.
	    * Customise if necessary.
	    */
	    private void writeObject(
	      ObjectOutputStream aOutputStream
	    ) throws IOException {
	      //perform the default serialization for all non-transient, non-static fields
	      aOutputStream.defaultWriteObject();
	    }
	    
}


