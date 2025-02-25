package ai.netoai.collector.model;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyDescriptor;
import java.util.Date;
import java.util.List;
import java.util.Set;


public class NetworkElement extends Node {
	private static final Logger log = LoggerFactory.getLogger(NetworkElement.class);
	public enum State {
		UNKNOWN, CONNECTING, CONNECTED, VERIFYING, SYNC_INV, SYNC_ALARMS, SYNCED, DISCONNECTING, DISCONNECTED;
		
		public static State asEnum(String value) {
			if (value.equalsIgnoreCase("unknown")) {
				return UNKNOWN;
			} else if (value.equalsIgnoreCase("connecting")) {
				return State.CONNECTING;
			} else if (value.equalsIgnoreCase("connected")) {
				return CONNECTED;
			} else if (value.equalsIgnoreCase("verifying")) {
				return VERIFYING;
			} else if (value.equalsIgnoreCase("sync_inv")) {
				return SYNC_INV;
			} else if (value.equalsIgnoreCase("sync_alarms")) {
				return SYNC_ALARMS;
			} else if (value.equalsIgnoreCase("synced")) {
				return SYNCED;
			} else if (value.equalsIgnoreCase("disconnecting")) {
				return DISCONNECTING;
			} else if (value.equalsIgnoreCase("disconnected")) {
				return DISCONNECTED;
			} 
			return null;
		}
	}

	public enum ReachabilityState {
		UP,DOWN;
		public static ReachabilityState asEnum(String value) {
			if (value.equalsIgnoreCase("up")) {
				return UP;
			} else if (value.equalsIgnoreCase("down")) {
				return DOWN;
			} 
			return null;
		}
	}
	public enum DeviceType {
		ROUTER, SWITCH, SERVER, FIREWALL, WIFIHOTSPOT, STORAGE;
		
		public static DeviceType asEnum(String value) {
			if (value.equalsIgnoreCase("router")) {
				return ROUTER;
			} else if (value.equalsIgnoreCase("switch")) {
				return SWITCH;
			} else if (value.equalsIgnoreCase("server")) {
				return SERVER;
			} else if (value.equalsIgnoreCase("firewall")) {
				return FIREWALL;
			} else if (value.equalsIgnoreCase("wifihotspot")) {
				return WIFIHOTSPOT;
			} else if (value.equalsIgnoreCase("storage")) {
				return STORAGE;
			}
			return null;
		}
	}

	private String ip;
	private int port;
	private NetworkProtocol protocol;
	private State connState;
	private ReachabilityState reachableState;
	private DeviceType deviceType;
	private String vendorName;
	private String productName;
	private String productVersion;
	private String sysObjId;
	private String profileId;
	private int snmpTimeout;
	private int snmpRetries;
	private Date lastSyncTime;
	private boolean enableInventorySync;
	private int invSyncInterval;
	private String communityString;
	private AuthenticationProtocol authProtocol;
	private PrivateProtocol privProtocol;
	private String authPassword;
	private String privPassword;
	private String userName;
	private String authorativeEngineId;
	private String description;
	private String baseBridgeAddress;
	private String sysUpTime;
	private List<String> monitorIds;
	private String connUserName;
	private String connPassword;
	private String connectionType;
    private String clliCode;
    private String phoneNumber;
    private String contactPerson;
    private String address;
    private String drivingInstructions;

	public String getConnectionType() {
		return connectionType;
	}

	public void setConnectionType(String connectionType) {
		this.connectionType = connectionType;
	}

	public String getConnUserName() {
		return connUserName;
	}

	public void setConnUserName(String connUserName) {
		this.connUserName = connUserName;
	}

	public String getConnPassword() {
		return connPassword;
	}

	public void setConnPassword(String connPassword) {
		this.connPassword = connPassword;
	}

	public List<String> getMonitorIds() {
		return monitorIds;
	}

	public void setMonitorIds(List<String> monitorIds) {
		this.monitorIds = monitorIds;
	}

	public String getSysUpTime() {
		return sysUpTime;
	}

	public void setSysUpTime(String sysUpTime) {
		this.sysUpTime = sysUpTime;
	}

	public String getBaseBridgeAddress() {
		return baseBridgeAddress;
	}

	public void setBaseBridgeAddress(String baseBridgeAddress) {
		this.baseBridgeAddress = baseBridgeAddress;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	
	public NetworkElement() {
		setBeanType(BeanType.NETWORKELEMENT);
		setNodeType(NodeType.NETWORK_ELEMENT);
		// All NetworkElements will be created by SNMP Collector
		// so setting the discovery source to SNMP Collector
		setDiscoverySource(DiscoverySource.SNMP_DISCOVERY);
	}
	
	/**
	 * @return the id
	 */
	@Transient
	public String getId() {
		return this.ip;
	}

	/**
	 * @return the ip
	 */
    @Searchable(alias = "IP Address")
    @Id
	public String getIp() {
		return ip;
	}

	/**
	 * @param ip
	 *            the ip to set
	 */
	public void setIp(String ip) {
		this.ip = ip;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @param port
	 *            the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * @return the protocol
	 */
	public NetworkProtocol getProtocol() {
		return protocol;
	}

	/**
	 * @param protocol
	 *            the protocol to set
	 */
	public void setProtocol(NetworkProtocol protocol) {
		this.protocol = protocol;
	}

	/**
	 * @return the connState
	 */
	public State getConnState() {
		return connState;
	}

	/**
	 * @param connState
	 *            the connState to set
	 */
	public void setConnState(State connState) {
		this.connState = connState;
	}

	
	public ReachabilityState getReachableState() {
		return reachableState;
	}

	public void setReachableState(ReachabilityState pingState) {
		this.reachableState = pingState;
	}

	/**
	 * @return the deviceType
	 */
    @Searchable(alias = "Device Type")
	public DeviceType getDeviceType() {
		return deviceType;
	}

	/**
	 * @param deviceType
	 *            the deviceType to set
	 */
	public void setDeviceType(DeviceType deviceType) {
		this.deviceType = deviceType;
	}

	/**
	 * @return the vendorName
	 */
    @Searchable(alias = "Vendor")
	public String getVendorName() {
		return vendorName;
	}

	/**
	 * @param vendorName
	 *            the vendorName to set
	 */
	public void setVendorName(String vendorName) {
		this.vendorName = vendorName;
	}

	/**
	 * @return the productName
	 */
    @Searchable(alias = "Product")
	public String getProductName() {
		return productName;
	}

	/**
	 * @param productName
	 *            the productName to set
	 */
	public void setProductName(String productName) {
		this.productName = productName;
	}

	/**
	 * @return the productVersion
	 */
	public String getProductVersion() {
		return productVersion;
	}

	/**
	 * @param productVersion
	 *            the productVersion to set
	 */
	public void setProductVersion(String productVersion) {
		this.productVersion = productVersion;
	}

	/**
	 * @return the sysObjId
	 */
	public String getSysObjId() {
		return sysObjId;
	}

	/**
	 * @param sysObjId
	 *            the sysObjId to set
	 */
	public void setSysObjId(String sysObjId) {
		this.sysObjId = sysObjId;
	}

	/**
	 * @return the profileId
	 */
	public String getProfileId() {
		return profileId;
	}

	/**
	 * @param profileId
	 *            the profileId to set
	 */
	public void setProfileId(String profileId) {
		this.profileId = profileId;
	}

	/**
	 * @return the snmpTimeout
	 */
	public int getSnmpTimeout() {
		return snmpTimeout;
	}

	/**
	 * @param snmpTimeout
	 *            the snmpTimeout to set
	 */
	public void setSnmpTimeout(int snmpTimeout) {
		this.snmpTimeout = snmpTimeout;
	}

	/**
	 * @return the snmpRetries
	 */
	public int getSnmpRetries() {
		return snmpRetries;
	}

	/**
	 * @param snmpRetries
	 *            the snmpRetries to set
	 */
	public void setSnmpRetries(int snmpRetries) {
		this.snmpRetries = snmpRetries;
	}

	/**
	 * @return the lastSyncTime
	 */
	public Date getLastSyncTime() {
		return lastSyncTime;
	}

	/**
	 * @param lastSyncTime
	 *            the lastSyncTime to set
	 */
	public void setLastSyncTime(Date lastSyncTime) {
		this.lastSyncTime = lastSyncTime;
	}

	/**
	 * @return the enableInventorySync
	 */
	public boolean isEnableInventorySync() {
		return enableInventorySync;
	}

	/**
	 * @param enableInventorySync
	 *            the enableInventorySync to set
	 */
	public void setEnableInventorySync(boolean enableInventorySync) {
		this.enableInventorySync = enableInventorySync;
	}

	/**
	 * @return the invSyncInterval
	 */
	public int getInvSyncInterval() {
		return invSyncInterval;
	}

	/**
	 * @param invSyncInterval
	 *            the invSyncInterval to set
	 */
	public void setInvSyncInterval(int invSyncInterval) {
		this.invSyncInterval = invSyncInterval;
	}

	/**
	 * @return the communityString
	 */
	public String getCommunityString() {
		return communityString;
	}

	/**
	 * @param communityString
	 *            the communityString to set
	 */
	public void setCommunityString(String communityString) {
		this.communityString = communityString;
	}	
	
	public AuthenticationProtocol getAuthProtocol() {
		return authProtocol;
	}

	public void setAuthProtocol(AuthenticationProtocol authProtocol) {
		this.authProtocol = authProtocol;
	}

	public PrivateProtocol getPrivProtocol() {
		return privProtocol;
	}

	public void setPrivProtocol(PrivateProtocol privProtocol) {
		this.privProtocol = privProtocol;
	}
	
	public String getAuthPassword() {
		return authPassword;
	}

	public void setAuthPassword(String authPassword) {
		this.authPassword = authPassword;
	}

	public String getPrivPassword() {
		return privPassword;
	}

	public void setPrivPassword(String privPassword) {
		this.privPassword = privPassword;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getAuthorativeEngineId() {
		return authorativeEngineId;
	}

	public void setAuthorativeEngineId(String authorativeEngineId) {
		this.authorativeEngineId = authorativeEngineId;
	}


	public NetworkElement merge(NetworkElement ne){
		Set<String> values = ne.getProfileValuesSet();
		for(String key : values){
			PropertyDescriptor pds = BeanInfoCache.getPropertyDescriptor(ne.getClass(), key);
			Object getValue = null;
			if(pds != null && pds.getReadMethod() != null){
				try {
					getValue = pds.getReadMethod().invoke(ne);
				} catch (Exception e) {
					log.error("Failed", e);
				} 
			}
                        if ( getValue == null ) {
                            continue;
                        }
			PropertyDescriptor pd = BeanInfoCache.getPropertyDescriptor(this.getClass(), key);
			if ( pd != null && pd.getWriteMethod() != null ) {
				try {
					pd.getWriteMethod().invoke(this, new Object[]{getValue});
				} catch (Exception e) {
					log.error("Failed", e);
				}
			}
		}
		return this;
	}
	
    // Lazily initialized, cached hashCode
    private volatile int hashCode; // (See Item 71)

    @Override
    public int hashCode() {
        int result = hashCode;
        if (result == 0) {
            result = 17;
            result = 31 * result + this.ip.hashCode();
            hashCode = result;
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof NetworkElement)) {
            return false;
        }
        NetworkElement ne = (NetworkElement) o;
        return ne.ip.equals(ip);
    }

    @Override
    public String toString() {
        return this.ip;
    }

    @Override
	@Searchable(alias = "Status")
	public boolean getStatus() {
    	if ( this.connState == null || connState == State.DISCONNECTED || connState == State.DISCONNECTING ) {
    		return false;
		}
    	return true;
	}


	public String getClliCode() {
		return clliCode;
	}

	public void setClliCode(String clliCode) {
		this.clliCode = clliCode;
	}

	public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

	public String getDrivingInstructions() {
        return drivingInstructions;
    }

    public void setDrivingInstructions(String drivingInstructions) {
        this.drivingInstructions = drivingInstructions;
    }
}
