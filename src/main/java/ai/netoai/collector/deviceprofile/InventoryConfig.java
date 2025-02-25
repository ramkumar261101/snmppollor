package ai.netoai.collector.deviceprofile;

import java.util.List;

public class InventoryConfig implements Config{

	private String name;
	private List<SnmpTable> tables;
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
	 * @return the tables
	 */
	public List<SnmpTable> getTables() {
		return tables;
	}
	/**
	 * @param tables the tables to set
	 */
	public void setTables(List<SnmpTable> tables) {
		this.tables = tables;
	}
}
