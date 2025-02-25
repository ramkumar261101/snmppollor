
package ai.netoai.collector.snmp.trap;

import ai.netoai.collector.cache.NodeCacheManager;
import ai.netoai.collector.deviceprofile.ConfigManager;
import ai.netoai.collector.snmp.SnmpManager;
import ai.netoai.collector.snmp.discovery.NodeManager;
import ai.netoai.collector.startup.TopicProducer;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SnmpTrapTest {

    private static final Logger log = LoggerFactory.getLogger(SnmpTrapTest.class);
    
    @Test
    public void trapRecieveTest() throws InterruptedException {
        try {
            System.out.println("trap recieve test");
            System.setProperty("nmsConfig.nms_system", "nms_system.properties");
            System.setProperty("nmsConfig.collector", "collector.properties");
            ConfigManager.getInstance().start("./config/");
            TopicProducer.init();
            TopicProducer.getInstance().start();
            NodeManager.getInstance().start();
            NodeManager.getInstance().updateNetworkElements();
            System.out.println("nodes list : " + NodeManager.getInstance().getNetworkElements());
            NodeCacheManager.getInstance().start();
            SnmpManager.init();
            SnmpManager snmpManager = SnmpManager.getInstance();
            snmpManager.start();
        } catch (Exception e) {
            log.error("Failed", e);
        }
        
        Thread.sleep(9999999999l);
    }
}
