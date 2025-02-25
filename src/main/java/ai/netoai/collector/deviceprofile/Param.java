package ai.netoai.collector.deviceprofile;

public class Param {

	public enum Type {
       COUNTER, GAUGE32, GAUGE, COUNTER64, BOOLEAN, OCTET_STRING;
		public static Type asEnum(String value) {
			if (value.equalsIgnoreCase("counter")) {
				return COUNTER;
			} else if (value.equalsIgnoreCase("gauge32")) {
				return GAUGE32;
			} else if (value.equalsIgnoreCase("gauge")) {
				return GAUGE;
			} else if (value.equalsIgnoreCase("counter64")) {
				return COUNTER64;
			} else if (value.equalsIgnoreCase("boolean")) {
				return BOOLEAN;
			} else if (value.equalsIgnoreCase("octet_string")) {
                                return OCTET_STRING;
                        }
			return null;
		}
	}
	public enum Collector {
        NONE;
		public static Collector asEnum(String value) {
			if (value.equalsIgnoreCase("none")) {
				return NONE;
			}
			return null;
		}
	}
	
	private String id;
	private String name;
	private String oid;
	private Type type;
	private Boolean indexed;
	private Collector collector;
	
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
	 * @return the type
	 */
	public Type getType() {
		return type;
	}
	/**
	 * @param type the type to set
	 */
	public void setType(Type type) {
		this.type = type;
	}
	/**
	 * @return the indexed
	 */
	public boolean isIndexed() {
		return indexed;
	}
	/**
	 * @param indexed the indexed to set
	 */
	public void setIndexed(boolean indexed) {
		this.indexed = indexed;
	}
	/**
	 * @return the collector
	 */
	public Collector getCollector() {
		return collector;
	}
	/**
	 * @param collector the collector to set
	 */
	public void setCollector(Collector collector) {
		this.collector = collector;
	}
	
}
