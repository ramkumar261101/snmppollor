package ai.netoai.collector.deviceprofile;

import java.util.ArrayList;
import java.util.List;


public class PmConfig implements Config{

	private String id;
	private String name;
	private List<MetricFamily> metricFamilies = new ArrayList<MetricFamily>();
	
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	/**
	 * @param id the id to set
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
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the metricFamilies
	 */
	public List<MetricFamily> getMetricFamilies() {
		return metricFamilies;
	}
	/**
	 * @param metricFamilies the metricFamilies to set
	 */
	public void setMetricFamilies(List<MetricFamily> metricFamilies) {
		this.metricFamilies = metricFamilies;
	}
	
}
