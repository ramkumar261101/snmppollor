package ai.netoai.collector.snmp.discovery;

import java.net.UnknownHostException;
import java.util.List;

import ai.netoai.collector.cache.NodeCacheManager;
import ai.netoai.collector.deviceprofile.ConfigManager;
import ai.netoai.collector.deviceprofile.SnmpConfig;
import ai.netoai.collector.model.EndPoint;
import ai.netoai.collector.model.NetworkElement;
import ai.netoai.collector.settings.SettingsManager;
import ai.netoai.collector.settings.SnmpSettings;
import ai.netoai.collector.snmp.AsyncSnmpResponseListener;
import ai.netoai.collector.snmp.SnmpPerformancePoller;
import ai.netoai.collector.utils.TopicJsonSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.netoai.collector.snmp.SnmpPoller;

import java.util.Date;


public class DisconnectedState implements NodeState {

    private static final Logger log = LoggerFactory.getLogger(DisconnectedState.class);
    private static final String tracePrefix = "[" + DisconnectedState.class.getSimpleName() + "]: ";
    private NodeAdapter adapter;
    private SnmpPoller poller;
    private SettingsManager manager;
    private SnmpSettings snmpSettings;
    private TopicJsonSender topicInstance;

    public DisconnectedState(NodeAdapter adapter) {
        this.adapter = adapter;
        this.poller = SnmpPoller.getInstance();
        this.manager = SettingsManager.getInstance();
        this.topicInstance = TopicJsonSender.getInstance();
        snmpSettings = new SnmpSettings();
    }

    @Override
    public void checkConnection() throws UnknownHostException {
        NetworkElement ne = adapter.getNetworkelement();
        int reconnectInterval = snmpSettings.getReconnectInterval();
        log.info(tracePrefix + "NE: " + ne.getName() + ", Last Resync time: " + (ne.getLastSyncTime() == null ? "NA" : ne.getLastSyncTime()));
        if (ne.getLastSyncTime() == null) {
            log.info(tracePrefix + "Last resync time is 0");
            boolean success = adapter.checkSnmpConnection(adapter);
            ne.setLastSyncTime(new Date());
            if (success) {
                adapter.setCurrentState(adapter.getConnectedStateObj());
            }
        } else {
            long syncTime = ne.getLastSyncTime().getTime();
            long syncDiff = (System.currentTimeMillis()) - syncTime;
            log.info(tracePrefix + "NE: " + ne.getName() + ", Time last reconnected: " + syncDiff + " ms");
            if (syncDiff >= (reconnectInterval * 1000l)) {
                boolean success = adapter.checkSnmpConnection(adapter);
                log.info(tracePrefix + "NE: " + ne.getName() + ", Connection check success: " + success);
                ne.setLastSyncTime(new Date());
                if (success) {
                    adapter.setCurrentState(adapter.getConnectedStateObj());
                }
            }
        }
    }



    @Override
    public void doDiscovery() {
        // TODO Auto-generated method stub

    }

    @Override
    public void doInventorySync() {
        // TODO Auto-generated method stub

    }

    @Override
    public void doAlarmSync() {
        // TODO Auto-generated method stub

    }

    @Override
    public void doCleanUp() {
        // TODO Auto-generated method stub

    }

    @Override
    public void doNoOperation() {
        // TODO Auto-generated method stub

    }

    @Override
    public NetworkElement.State getState() {
        return NetworkElement.State.DISCONNECTED;
    }

}
