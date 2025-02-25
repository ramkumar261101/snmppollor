package ai.netoai.collector.model;

import java.io.PrintWriter;
import java.util.*;

public class ISNode implements java.io.Serializable{



	public ISNode() {
		// TODO Auto-generated constructor stub
	}

	private String name ="";
	private String id = "";
	
	

	/**
	 * 
	 */
	private Map<String,String> properties = new TreeMap<String,String>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties.putAll(properties);
	}
	
	@Override
	public String toString() {
		return this.name + "(" + this.id + ")";
	}
	

	
}
