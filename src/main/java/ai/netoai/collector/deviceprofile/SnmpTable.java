package ai.netoai.collector.deviceprofile;

import java.util.List;

public class SnmpTable {

	private String name;
	private String oid;
	private String filter;
	private List<SnmpTablecolumn> columns;
	private Class beanType;
	private List<Property> properties;
	private SnmpTable appends;
	private List<String> metricFamilyIds;
	private List<String> trapIds;
	
	private String id;
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
	 * @return the oid
	 */
	public String getOid() {
		return oid;
	}
	/**
	 * @param oid the oid to set
	 */
	public void setOid(String oid) {
		this.oid = oid;
	}
	/**
	 * @return the filter
	 */
	public String getFilter() {
		return filter;
	}
	/**
	 * @param filter the filter to set
	 */
	public void setFilter(String filter) {
		this.filter = filter;
	}
	/**
	 * @return the columns
	 */
	public List<SnmpTablecolumn> getColumns() {
		return columns;
	}
	/**
	 * @param columns the columns to set
	 */
	public void setColumns(List<SnmpTablecolumn> columns) {
		this.columns = columns;
	}
	/**
	 * @return the beanType
	 */
	public Class getBeanType() {
		return beanType;
	}
	/**
	 * @param beanType the beanType to set
	 */
	public void setBeanType(Class beanType) {
		this.beanType = beanType;
	}
	/**
	 * @return the properties
	 */
	public List<Property> getProperties() {
		return properties;
	}
	/**
	 * @param properties the properties to set
	 */
	public void setProperties(List<Property> properties) {
		this.properties = properties;
	}
	/**
	 * @return the appends
	 */
	public SnmpTable getAppends() {
		return appends;
	}
	/**
	 * @param appends the appends to set
	 */
	public void setAppends(SnmpTable appends) {
		this.appends = appends;
	}
	/**
	 * @return the metricFamilyIds
	 */
	public List<String> getMetricFamilyIds() {
		return metricFamilyIds;
	}
	/**
	 * @param metricFamilyIds the metricFamilyIds to set
	 */
	public void setMetricFamilyIds(List<String> metricFamilyIds) {
		this.metricFamilyIds = metricFamilyIds;
	}
	public List<String> getTrapIds() {
		return trapIds;
	}
	public void setTrapIds(List<String> trapIds) {
		this.trapIds = trapIds;
	}
        
        public SnmpTablecolumn getColumnByName(String name) {
            for(SnmpTablecolumn col : columns) {
                if(col.getName().equalsIgnoreCase(name)) {
                    return col;
                }
            }
            return null;
        }

}
