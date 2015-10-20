package fr.unice.polytech.soa1.cookbook.flows.utils;

import fr.unice.polytech.soa1.cookbook.flows.business.TaxForm;

import java.util.HashMap;
import java.util.Map;


public final class Database {

	// Local mock for a database
	private static Map<String,TaxForm> contents = new HashMap<String, TaxForm>();

	public void setData(String uid, TaxForm f) {
		contents.put(uid, f);
	}

	public TaxForm getData(String uuid) {
		if (contents.containsKey(uuid))
			return contents.get(uuid);
		else
			throw new IllegalArgumentException("Unknown uuid: [" + uuid + "]");
	}

}
