package ai.netoai.collector.deviceprofile;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.List;

public class DeviceConfig implements Config {

	private String id;
	private Class beanType;
	private List<Property> properties;
	// private List<TrapGroup> trapGroups;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Class getBeanType() {
		return beanType;
	}

	public void setBeanType(Class beanType) {
		this.beanType = beanType;
	}

	public List<Property> getProperties() {
		return properties;
	}

	public void setProperties(List<Property> properties) {
		this.properties = properties;
	}
	/*
	 * public List<TrapGroup> getTrapGroups() { return trapGroups; } public void
	 * setTrapGroups(List<TrapGroup> trapGroups) { this.trapGroups = trapGroups;
	 * }
	 */

}

class DeviceConfigAdapter implements JsonSerializer<DeviceConfig>, JsonDeserializer<DeviceConfig> {
	private static final Logger log = LoggerFactory.getLogger(DeviceConfigAdapter.class);
	
	private DeviceConfig config;
	private String beanType;
	
	public DeviceConfigAdapter(DeviceConfig dc,String beanType){
		this.config = dc;
		this.beanType = beanType;
	}
	
	
	@Override
	public DeviceConfig deserialize(JsonElement arg0, Type arg1, JsonDeserializationContext arg2)
			throws JsonParseException {
		// TODO Auto-generated method stub
		if (arg0 != null) {
			if (arg0.isJsonObject()) {
				JsonObject jo = arg0.getAsJsonObject();
				Class className;
				try {
					System.out.println("--- " + jo.get("beanType").toString());
					className = Class.forName(jo.get("beanType").getAsString());
					jo.remove("beanType");
					config.setBeanType(className);
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					log.error("Failed", e);
				}
				return config;
			}
		}
		return null;
	}

	@Override
	public JsonElement serialize(DeviceConfig arg0, Type arg1, JsonSerializationContext arg2) {
		// TODO Auto-generated method stub
		return null;
	}

}


