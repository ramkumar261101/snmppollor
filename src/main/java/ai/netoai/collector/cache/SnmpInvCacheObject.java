package ai.netoai.collector.cache;

import java.util.List;

public class SnmpInvCacheObject {
	
	private String deviceIp;
	private String snmpTable;
	private List<String> indices;
	private List<String> metrics;
	
	public SnmpInvCacheObject() {
		super();
	}
	
	public SnmpInvCacheObject(String deviceIp, String snmpTable, List<String> indices, List<String> metrics) {
		super();
		this.deviceIp = deviceIp;
		this.snmpTable = snmpTable;
		this.indices = indices;
		this.metrics = metrics;
	}
	public String getDeviceIp() {
		return deviceIp;
	}
	public void setDeviceIp(String deviceIp) {
		this.deviceIp = deviceIp;
	}
	public List<String> getIndices() {
		return indices;
	}
	public void setIndices(List<String> indices) {
		this.indices = indices;
	}
	public List<String> getMetrics() {
		return metrics;
	}
	public void setMetrics(List<String> metrics) {
		this.metrics = metrics;
	}
	
	

}
