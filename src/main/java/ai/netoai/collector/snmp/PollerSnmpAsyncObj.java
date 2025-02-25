
package ai.netoai.collector.snmp;

import java.util.List;

import ai.netoai.collector.deviceprofile.Metric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class PollerSnmpAsyncObj {

    private static final Logger log = LoggerFactory.getLogger(PollerSnmpAsyncObj.class);
    private static final String tracePrefix = "[" + PollerSnmpAsyncObj.class.getSimpleName() + "]: ";

    private String epId;
    private String epIndex;
    private long requestTime;
    private Collection<Metric> metrics;
    private String diff; // Differentiator if the index values are same for different tables.
    private long respRecTime;
    private String tenantId;
    private String ipDomain;
    private String networkElementName;
    private String networkElementIp;
    private String sourceName;
    private String deviceType;

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public PollerSnmpAsyncObj() {

    }
    
    public PollerSnmpAsyncObj(String epId, String epIndex, Collection<Metric> metrics, long requestTime
            , String tenantId, String ipDomain, String networkElementName, String networkElementIp, String sourceName) {
        super();
        this.epId = epId;
        this.epIndex = epIndex;
        this.metrics = metrics;
        this.requestTime = requestTime;
        this.tenantId = tenantId;
        this.ipDomain = ipDomain;
        this.networkElementIp = networkElementIp;
        this.networkElementName = networkElementName;
        this.sourceName = sourceName;
    }

    public PollerSnmpAsyncObj(String epId, long requestTime, Collection<Metric> metrics, String diff) {
        super();
        this.epId = epId;
        this.requestTime = requestTime;
        this.metrics = metrics;
        this.diff = diff;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getIpDomain() {
        return ipDomain;
    }

    public void setIpDomain(String ipDomain) {
        this.ipDomain = ipDomain;
    }

    public String getNetworkElementName() {
        return networkElementName;
    }

    public void setNetworkElementName(String networkElementName) {
        this.networkElementName = networkElementName;
    }

    public String getNetworkElementIp() {
        return networkElementIp;
    }

    public void setNetworkElementIp(String networkElementIp) {
        this.networkElementIp = networkElementIp;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getEpId() {
        return epId;
    }

    public void setEpId(String epId) {
        this.epId = epId;
    }

    public String getEpIndex() {
        return epIndex;
    }

    public void setEpIndex(String epIndex) {
        this.epIndex = epIndex;
    }

    public Collection<Metric> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<Metric> metrics) {
        this.metrics = metrics;
    }

    public String getDiff() {
        return diff;
    }

    public void setDiff(String diff) {
        this.diff = diff;
    }

    public long getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(long requestTime) {
        this.requestTime = requestTime;
    }

    /**
     * @return the respRecTime
     */
    public long getRespRecTime() {
        return respRecTime;
    }

    /**
     * @param respRecTime the respRecTime to set
     */
    public void setRespRecTime(long respRecTime) {
        this.respRecTime = respRecTime;
    }

    @Override
    public String toString() {
        return "PollerSnmpAsyncObj [EpId=" + epId + ", metrics=" + metrics
                + ", requestTime=" + requestTime + "]";
    }
}
