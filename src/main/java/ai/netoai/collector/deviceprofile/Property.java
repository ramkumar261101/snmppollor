package ai.netoai.collector.deviceprofile;

public class Property {

	public enum PropertyFetchType {
		EXPLICIT, SNMP_GET, JAVASCRIPT, RPN, COLUMN, OID, INDEX,STRING_TEMPLATE, HEXTOIP, NODEOBJ;
		
		public static PropertyFetchType asEnum(String value) {
			if (value.equalsIgnoreCase("explicit")) {
				return EXPLICIT;
			} else if (value.equalsIgnoreCase("snmp_get")) {
				return SNMP_GET;
			} else if (value.equalsIgnoreCase("javascript")) {
				return JAVASCRIPT;
			} else if (value.equalsIgnoreCase("rpn")) {
				return RPN;
			} else if (value.equalsIgnoreCase("column")) {
				return COLUMN;
			} else if (value.equalsIgnoreCase("oid")) {
				return OID;
			} else if (value.equalsIgnoreCase("index")) {
				return INDEX;
			} else if (value.equalsIgnoreCase("string_template")) {
				return STRING_TEMPLATE;
			} else if (value.equalsIgnoreCase("hextoip")) {
                                return HEXTOIP;
                        } else if (value.equalsIgnoreCase("nodeobj")) {
                                return NODEOBJ;
                        }
			return null;
		}
	}

	public static enum DataTypes {
		INTEGER, LONG, DOUBLE, STRING, DATE, TIMESTAMP, BINARY, BOOLEAN, TYPE, OPERSTATUS, ADMINSTATUS, ENDPOINTTYPE, DEVICETYPE;
		@Override
		public String toString() {
			switch (this) {
				case INTEGER:
					return "Integer";
				case LONG:
					return "Long";
				case DOUBLE:
					return "Double";
				case STRING:
					return "String";
				case DATE:
					return "Date";
				case TIMESTAMP:
					return "TimeStamp";
				case BINARY:
					return "Binary";
				case BOOLEAN:
					return "Boolean";
				case TYPE:
					return "Type";
				case OPERSTATUS:
					return "OperStatus";
				case ADMINSTATUS:
					return "AdminStatus";
				case ENDPOINTTYPE:
					return "endPointType";
				case DEVICETYPE:
					return "deviceType";	
				default:
					return this.name();
			}
	      }
		
		public static DataTypes asEnum(String value) {
			if (value.equalsIgnoreCase("Integer")) {
				return INTEGER;
			} else if (value.equalsIgnoreCase("Long")) {
				return LONG;
			} else if (value.equalsIgnoreCase("Double")) {
				return DOUBLE;
			} else if (value.equalsIgnoreCase("String")) {
				return STRING;
			} else if (value.equalsIgnoreCase("Date")) {
				return DATE;
			} else if (value.equalsIgnoreCase("TimeStamp")) {
				return TIMESTAMP;
			} else if (value.equalsIgnoreCase("Binary")) {
				return BINARY;
			} else if (value.equalsIgnoreCase("Boolean")) {
				return BOOLEAN;
			} else if (value.equalsIgnoreCase("TYPE")) {
				return TYPE;
			} else if (value.equalsIgnoreCase("operstatus")) {
				return OPERSTATUS;
			} else if (value.equalsIgnoreCase("adminstatus")) {
				return ADMINSTATUS;
			} else if (value.equalsIgnoreCase("endpointtype")) {
				return ENDPOINTTYPE;
			} else if (value.equalsIgnoreCase("devicetype")){
				return DataTypes.DEVICETYPE;
			}
			return null;

		}
	}	
		
	private String name;
	private DataTypes dataType;
	private String value;
	private PropertyFetchType fetchType;
	private String column;
	private String fetchIndex;
	private String index;  // This is the index using which the SNMP GET can be performed for specific properties
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public DataTypes getDataType() {
		return dataType;
	}
	public void setDataType(DataTypes dataType) {
		this.dataType = dataType;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public PropertyFetchType getFetchType() {
		return fetchType;
	}
	public void setFetchType(PropertyFetchType fetchType) {
		this.fetchType = fetchType;
	}
	public String getColumn() {
		return column;
	}
	public void setColumn(String column) {
		this.column = column;
	}

	public String getFetchIndex() {
		return fetchIndex;
	}

	public void setFetchIndex(String fetchIndex) {
		this.fetchIndex = fetchIndex;
	}

	public String getIndex() {
		return index;
	}

	public void setIndex(String index) {
		this.index = index;
	}
}
