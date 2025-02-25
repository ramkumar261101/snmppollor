package ai.netoai.collector.deviceprofile;

import java.util.ArrayList;
import java.util.List;

public class MetricFamily {

	public enum ProtoCol {
		SNMP, SCRIPT;
		
		@Override
		public String toString() {
			switch (this) {
			case SNMP:
				return "Snmp";
			case SCRIPT:
				return "Script";
			default:
				return this.name();
			}
		}

		public static ProtoCol asEnum(String value) {
			if (value.equalsIgnoreCase("Snmp")) {
				return SNMP;
			}else if (value.equalsIgnoreCase("Script")) {
				return SCRIPT;
			}
			return null;
		}
	}
	

	private String id;
	private String name;
	private ProtoCol protocol;
	private List<Metric> metrics = new ArrayList<Metric>();


	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the protocol
	 */
	public ProtoCol getProtocol() {
		return protocol;
	}

	/**
	 * @param protocol
	 *            the protocol to set
	 */
	public void setProtocol(ProtoCol protocol) {
		this.protocol = protocol;
	}
	
	/**
	 * @return the metrics
	 */
	public List<Metric> getMetrics(){
		return metrics;
	}

	/**
	 * @param metrics
	 *            the metrics to set
	 */
	public void setMetrics(List<Metric> metrics){
		this.metrics = metrics;
	}

}
