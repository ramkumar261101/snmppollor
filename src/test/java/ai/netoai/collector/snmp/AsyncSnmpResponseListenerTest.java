package ai.netoai.collector.snmp;

import ai.netoai.collector.deviceprofile.ConfigManager;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

public class AsyncSnmpResponseListenerTest {
    private static final Logger log = LoggerFactory.getLogger(AsyncSnmpResponseListenerTest.class);

    private static final String METRIC_NAME = "Ethernet Rx Avg Utilization(64 Bit)";
    private static final String EP_INDEX = "1";
    private static final String EP_ID = "12345";
    private static final long IN_OCTETS_T1 = 35529092l;
    private static final long IN_OCTETS_T2 = 35530347l;
    private static final double SPEED = 10000;
    private static final int TIME_DIFF = 60;

    static {
        System.setProperty("platform.home", "src/test/resources");
        System.setProperty("nmsConfig.nms_system", "src/test/resources/nms_system.properties");
        System.setProperty("nmsConfig.collector", "src/test/resources/collector.properties");
        try {
            ConfigManager.getInstance().start();
            SnmpManager.init();
        } catch (Exception ex) {
            log.error("Failed", ex);
        }
    }

    @Test
    public void testEthRxUtilization() throws InterruptedException {

    }



}