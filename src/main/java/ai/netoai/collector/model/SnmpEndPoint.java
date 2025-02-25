package ai.netoai.collector.model;

import java.io.Serializable;

public class SnmpEndPoint implements Serializable {

	private static final long serialVersionUID = -2785312575888664268L;
	
	public static final String _NETWORK = "network";
	public static final String _SUB_NETWORK = "subNetwork";
	public static final String _NODE = "node";
	public static final String _SOURCE_INDEX = "sourceIndex";
	public static final String _SOURCE_ID = "sourceId";
	public static final String _NAME = "name";
	public static final String _SOURCE_TYPE = "sourceType";
	public static final String _SPEED = "speed";
	public static final String _MAC = "mac";
	public static final String _ADMIN_STATUS = "adminStatus";
	public static final String _OP_STATUS = "opStatus";
	public static final String _NODE_NAME = "nodeName";
	public static final String _ALIAS = "alias";
	
	private String network;
	private String subNetwork;
	private String node;
	private String nodeName;
	private String sourceIndex;
	private String sourceId;
	private String name;
	private String sourceType;
	private Long speed;
	private String mac;
	private Boolean adminStatus;
	private Boolean opStatus;
	private String alias;
	/**
	 * @return the network
	 */
	public String getNetwork() {
		return network;
	}
	/**
	 * @param network the network to set
	 */
	public void setNetwork(String network) {
		this.network = network;
	}
	/**
	 * @return the subNetwork
	 */
	public String getSubNetwork() {
		return subNetwork;
	}
	/**
	 * @param subNetwork the subNetwork to set
	 */
	public void setSubNetwork(String subNetwork) {
		this.subNetwork = subNetwork;
	}
	/**
	 * @return the node
	 */
	public String getNode() {
		return node;
	}
	/**
	 * @param node the node to set
	 */
	public void setNode(String node) {
		this.node = node;
	}
	/**
	 * @return the index
	 */
	public String getSourceIndex() {
		return sourceIndex;
	}
	/**
	 * @param index the index to set
	 */
	public void setSourceIndex(String index) {
		this.sourceIndex = index;
	}
	/**
	 * @return the id
	 */
	public String getSourceId() {
		return sourceId;
	}
	/**
	 * @param id the id to set
	 */
	public void setSourceId(String id) {
		this.sourceId = id;
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
	 * @return the type
	 */
	public String getSourceType() {
		return sourceType;
	}
	/**
	 * @param type the type to set
	 */
	public void setSourceType(String type) {
		this.sourceType = type;
	}
	/**
	 * @return the speed
	 */
	public Long getSpeed() {
		return speed;
	}
	/**
	 * @param speed the speed to set
	 */
	public void setSpeed(Long speed) {
		this.speed = speed;
	}
	/**
	 * @return the mac
	 */
	public String getMac() {
		return mac;
	}
	/**
	 * @param mac the mac to set
	 */
	public void setMac(String mac) {
		this.mac = mac;
	}
	/**
	 * @return the adminStatus
	 */
	public Boolean getAdminStatus() {
		return adminStatus;
	}
	/**
	 * @param adminStatus the adminStatus to set
	 */
	public void setAdminStatus(Boolean adminStatus) {
		this.adminStatus = adminStatus;
	}
	/**
	 * @return the opStatus
	 */
	public Boolean getOpStatus() {
		return opStatus;
	}
	/**
	 * @param opStatus the opStatus to set
	 */
	public void setOpStatus(Boolean opStatus) {
		this.opStatus = opStatus;
	}
	
	/**
	 * @return the nodeName
	 */
	public String getNodeName() {
		return nodeName;
	}
	/**
	 * @param nodeName the nodeName to set
	 */
	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	/**
	 * @return the alias
	 */
	public String getAlias() {
		return alias;
	}
	/**
	 * @param alias the alias to set
	 */
	public void setAlias(String alias) {
		this.alias = alias;
	}
	
	
	@Override
	public String toString() {
		return node + "-" + name + " [" + sourceId + "]";
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((sourceId == null) ? 0 : sourceId.hashCode());
		result = prime * result + ((node == null) ? 0 : node.hashCode());
		return result;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SnmpEndPoint other = (SnmpEndPoint) obj;
		if (sourceId == null) {
			if (other.sourceId != null)
				return false;
		} else if (!sourceId.equals(other.sourceId))
			return false;
		if (node == null) {
			if (other.node != null)
				return false;
		} else if (!node.equals(other.node))
			return false;
		return true;
	}
	
}
