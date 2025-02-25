package ai.netoai.collector.snmp.discovery;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.snmp4j.Target;
import org.snmp4j.smi.VariableBinding;

public class DeviceTypeRepo {
	
	private Map<String, DeviceType> deviceTypes = new LinkedHashMap<>();

	/**
	 * @return the deviceTypes
	 */
	public Map<String, DeviceType> getDeviceTypes() {
		return deviceTypes;
	}

	/**
	 * @param deviceTypes the deviceTypes to set
	 */
	public void setDeviceTypes(Map<String, DeviceType> deviceTypes) {
		this.deviceTypes = deviceTypes;
	}
	
	public String searchDeviceType(Target target, List<VariableBinding> varbindCache) {
		String deviceType = null;
		for(Map.Entry<String, DeviceType> entry : deviceTypes.entrySet()) {
			deviceType = entry.getValue().evaluate(target, varbindCache);
			if ( deviceType != null ) {
				return deviceType;
			}
		}
		return null;
	}
	
}
