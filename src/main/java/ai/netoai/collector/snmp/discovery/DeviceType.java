/**
 * 
 */
package ai.netoai.collector.snmp.discovery;

import java.util.ArrayList;
import java.util.List;

import org.snmp4j.Target;
import org.snmp4j.smi.VariableBinding;

/**
 * @author lokesh
 *
 */
public class DeviceType {
	
	private String deviceId;
	private String deviceModel;
	private List<DeviceTypeRule> ruleList = new ArrayList<>();
	
	public DeviceType() {
		// TODO Auto-generated constructor stub
	}
	
	/**
         * Constructs a new device type instance
         * @param deviceId profile id
         * @param deviceModel make of the device
         * @param rules list of the device type identification rules
         */
	public DeviceType(String deviceId, String deviceModel, List<DeviceTypeRule> rules) {
		super();
		this.deviceId = deviceId;
		this.deviceModel = deviceModel;
		this.ruleList = rules;
	}

	/**
	 * @return the deviceId
	 */
	public String getDeviceId() {
		return deviceId;
	}
	/**
	 * @param deviceId the deviceId to set
	 */
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}
	/**
	 * @return the deviceModel
	 */
	public String getDeviceModel() {
		return deviceModel;
	}
	/**
	 * @param deviceModel the deviceModel to set
	 */
	public void setDeviceModel(String deviceModel) {
		this.deviceModel = deviceModel;
	}
	/**
	 * @return the rules
	 */
	public List<DeviceTypeRule> getRuleList() {
		return ruleList;
	}
	public void setRuleList(List<DeviceTypeRule> ruleList) {
		this.ruleList = ruleList;
	}
	/**
	 * @param rules the rules to set
	 */
	public void addRules(List<DeviceTypeRule> rules) {
		if ( rules != null ) {
			this.ruleList.addAll(rules);
		}
	}
	
	public void addRule(DeviceTypeRule rule) {
		if ( rule != null ) {
			ruleList.add(rule);
		}
	}
	
	public String evaluate(Target target, List<VariableBinding> varbindCache) {
		int count = 0;
		for(DeviceTypeRule rule : ruleList) {
			if ( !rule.evaluate(target, varbindCache,count) ) {
				return null;
			}
			count++;
		}
		return this.deviceId;
	}
	
	@Override
	public String toString() {
		return this.deviceId + "(" + this.deviceModel + "), rules=" + this.ruleList.size();
	}

}
