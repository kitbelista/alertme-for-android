package org.darkgoddess.alertme.api.utils;

import java.util.ArrayList;
import java.util.Comparator;

public class Hub {
	public String name = "";
	public String id = "";
	public String behaviour = "";
	public ArrayList<String> services = new ArrayList<String>();
	public Hub() {}
	public Hub(final String n, final String i) { name=n; id=i; }
	public Hub(final String n, final String i, final String b) { name=n; id=i; behaviour=b; }
	public static ArrayList<String> getServicesFromString(final String input) {
		ArrayList<String> res = new ArrayList<String>();
		if (input!=null) {
			String[] list = (input.contains(","))? input.trim().split(","): new String[] { input.trim() };
			for (String s: list) {
				if (s!=null && s.length()!=0) {
					res.add(s);
				}
			}
		}
		return res;
	}
	public void setServicesFromString(String input) {
		services = getServicesFromString(input);
	}
	public String getServicesString() {
		String res = null;
		if (services!=null && !services.isEmpty()) {
			int i = 0;
			res = "";			
			for(String service: services) {
				res += (i++==0)? "": ",";
				res += service;
			}
		}
		return res;
	}
	
	public static Comparator<Hub> getComparator(boolean reverse) {
		Comparator<Hub> res = null;
		
		if (!reverse) {
			res = new Comparator<Hub>() {
				@Override
				public int compare(Hub h1, Hub h2) {
					return h1.name.compareToIgnoreCase(h2.name); 
				}
			};
		} else {
			res = new Comparator<Hub>() {
				@Override
				public int compare(Hub h1, Hub h2) {
					return h2.name.compareToIgnoreCase(h1.name); 
				}
			};
		}
		return res;
	}
}
