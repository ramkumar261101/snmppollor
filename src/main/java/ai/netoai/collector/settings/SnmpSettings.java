package ai.netoai.collector.settings;


import ai.netoai.collector.model.GenericJavaBean;

public class SnmpSettings extends GenericJavaBean {

    public static final String _CATEGORY = "snmpSettings";
    private Integer trapReceiverThreads = new Integer(20);
    private Integer getBulkMaxRep = new Integer(60);
    private Integer snmpPoolSize = new Integer(20);
    private Integer responseListCount = new Integer(10);
    private Integer defaultAgentPort = new Integer(162);
    private String defaultProtocol = "SNMPV2c";
    private String defaultCommunity = "public";
    private Integer snmpRetries = 3;
    private Integer snmpTimeout = 3000;
    private Integer reconnectInterval = new Integer(60);
    // in hours
    private Integer inventoryReSyncInterval = new Integer(6);
	

	public Integer getInventoryReSyncInterval() {
		return inventoryReSyncInterval;
	}

	public void setInventoryReSyncInterval(Integer inventoryReSyncInterval) {
		this.inventoryReSyncInterval = inventoryReSyncInterval;
	}

	public Integer getReconnectInterval() {
		return reconnectInterval;
	}

	public void setReconnectInterval(Integer reconnectInterval) {
		this.reconnectInterval = reconnectInterval;
	}

    public Integer getSnmpRetries() {
        return snmpRetries;
    }

    public void setSnmpRetries(Integer snmpRetries) {
        this.snmpRetries = snmpRetries;
    }

    public Integer getSnmpTimeout() {
        return snmpTimeout;
    }

    public void setSnmpTimeout(Integer snmpTimeout) {
        this.snmpTimeout = snmpTimeout;
    }

    public String getDefaultCommunity() {
        return defaultCommunity;
    }

    public void setDefaultCommunity(String defaultCommunity) {
        this.defaultCommunity = defaultCommunity;
    }

    public Integer getResponseListCount() {
        return responseListCount;
    }

    public Integer getDefaultAgentPort() {
        return defaultAgentPort;
    }

    public void setDefaultAgentPort(Integer defaultAgentPort) {
        this.defaultAgentPort = defaultAgentPort;
    }

    public String getDefaultProtocol() {
        return defaultProtocol;
    }

    public void setDefaultProtocol(String defaultProtocol) {
        this.defaultProtocol = defaultProtocol;
    }

    public void setResponseListCount(Integer responseListCount) {
        this.responseListCount = responseListCount;
    }

    /**
     * @return the trapReceiverThreads
     */
    public Integer getTrapReceiverThreads() {
        return trapReceiverThreads;
    }

    /**
	 * @param trapReceiverThreads
	 *            the trapReceiverThreads to set
     */
    public void setTrapReceiverThreads(Integer trapReceiverThreads) {
        this.trapReceiverThreads = trapReceiverThreads;
    }

    /**
     * @return the getBulkMaxRep
     */
    public Integer getGetBulkMaxRep() {
        return getBulkMaxRep;
    }

    /**
	 * @param getBulkMaxRep
	 *            the getBulkMaxRep to set
     */
    public void setGetBulkMaxRep(Integer getBulkMaxRep) {
        this.getBulkMaxRep = getBulkMaxRep;
    }

    /**
     * @return the snmpPoolSize
     */
    public Integer getSnmpPoolSize() {
        return snmpPoolSize;
    }

    /**
	 * @param snmpPoolSize
	 *            the snmpPoolSize to set
     */
    public void setSnmpPoolSize(Integer snmpPoolSize) {
        this.snmpPoolSize = snmpPoolSize;
    }

}
