package ai.netoai.collector.snmp;

import ai.netoai.collector.cache.CacheManager;
import ai.netoai.collector.deviceprofile.ConfigManager;
import ai.netoai.collector.settings.SettingsManager;
import ai.netoai.collector.settings.SnmpSettings;
import ai.netoai.collector.snmp.discovery.NodeManager;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;

public class SnmpPerfPollTest {
    
    
    @Test
    public void testPerfPoll() throws InterruptedException {
        System.setProperty("nmsConfig.nms_system", "src/test/resources/nms_system.properties");
        System.setProperty("nmsConfig.collector","src/test/resources/collector.properties");
        
        SettingsManager settings = SettingsManager.getInstance();
        SnmpSettings snmpSettings = new SnmpSettings();
        snmpSettings.setAllProperties(settings.getSettings());
        snmpSettings.setSnmpPoolSize(20);
        CacheManager.init();
        SnmpManager.init();
        NodeManager.getInstance().start();
        ConfigManager config = ConfigManager.getInstance();
        try {
            config.start();
        } catch (InterruptedException ex) {
            Logger.getLogger(SnmpPerfPollTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        SnmpManager.getInstance().start();
        Thread.sleep(Long.MAX_VALUE);
    }
    
    @Test
    public void testAppMonQuery() {
        System.setProperty("tenantId", "defaulttenant");
        System.setProperty("nmsConfig.nms_system", "src/test/resources/nms_system.properties");
        System.setProperty("nmsConfig.collector","src/test/resources/collector.properties");
        CacheManager.init();
        SnmpManager.init();
        SettingsManager settings = SettingsManager.getInstance();
        SnmpPerformancePoller poller = SnmpPerformancePoller.getInstance();

    }
}
