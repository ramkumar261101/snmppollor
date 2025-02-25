package ai.netoai.collector.snmp.discovery;

import java.util.Arrays;

import ai.netoai.collector.deviceprofile.ConfigManager;
import ai.netoai.collector.model.*;
import ai.netoai.collector.settings.SettingsManager;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.netoai.collector.comms.IncomingTopicListener;
import ai.netoai.collector.snmp.SnmpPoller;


public class SnmpDiscoveryTest {

	private static final Logger log = LoggerFactory.getLogger(SnmpDiscoveryTest.class);
	private static final String tracePrefix = "["+ SnmpDiscoveryTest.class.getSimpleName() + "]: ";
	   
	@Test
	public void test() throws InterruptedException {
		System.setProperty("nmsConfig.nms_system", "src/test/resources/nms_system.properties");
		System.setProperty("nmsConfig.collector","src/test/resources/collector.properties");
		System.setProperty("tenantId", "defaulttenant");
		
		SettingsManager.getInstance();
		SnmpPoller.getInstance().start();
		//CollectorMain cm = new CollectorMain();
		//cm.init();
		/*InetAddress inet = null;
		try {
			inet = InetAddress.getByName("11.0.0.1");
		} catch (UnknownHostException e1) {
			log.error("Failed", e1);
		}
		CommunityTarget target = new CommunityTarget();
		target.setAddress(new UdpAddress(inet, 161));
		target.setCommunity(new OctetString("public"));
		target.setVersion(SnmpConstants.version1);
		target.setRetries(6);
		target.setTimeout(3000);
		PDU pdu = new PDU();
		pdu.add(new VariableBinding(new OID("1.3.6.1.2.1.2.2.1.7")));
		pdu.setType(PDU.GET);
		
		SnmpPoller poller = SnmpPoller.getInstance();
		poller.start();
		poller.sendSyncGetRequest(target,pdu);
		log.info("sending Request to snmp");
		PDU getPdu = poller.sendSyncGetRequest(target, pdu);
		log.info("Response Received"+getPdu);
		if (getPdu != null) {
			Vector<VariableBinding> vbs = (Vector<VariableBinding>) getPdu.getVariableBindings();
			if (vbs != null && vbs.size() != 0) {
				for(VariableBinding vb : vbs){
					System.out.println("**** "+vb);
				}
			}
		}*/
		

		/*SnmpSettings snmpSettings = new SnmpSettings();
		snmpSettings.setAllProperties(settings.getSettings(SnmpSettings._CATEGORY));
		snmpSettings.setSnmpPoolSize(20);
		settings.storeSettings(SnmpSettings._CATEGORY, snmpSettings.getAllProperties());*/
	    //PropertiesManager.init();
		
		
		/*ScheduledExecutorService exec = Executors.newScheduledThreadPool(10);
		exec.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				log.info("THREAD ..........");
				DiscoveryTask bean = createDiscoveryTask();
				ConfigMessage msg = new ConfigMessage(MsgType.START_DISCOVERY, "col-1", bean);
				TopicSender.getInstance().send(KafkaTopicSettings.COLLECTOR_INCOMING_TOPIC, msg);
			}
		}, 5, 500, TimeUnit.SECONDS);
		
		IncomingTopicListener.getInstance().start();*/
				
		ConfigManager configManager = ConfigManager.getInstance();
		try {
			configManager.start();
		} catch (InterruptedException e) {
			log.error("Failed", e);
		}
		
		DiscoveryTask task = createDiscoveryTask();
		SnmpDiscovery disc = new SnmpDiscovery(task);
		disc.start();
		
		NodeManager manager = NodeManager.getInstance();
		manager.start();
		
		IncomingTopicListener.getInstance().start();
		
		
		
		Thread.sleep(500*1000l);
	}

	private DiscoveryTask createDiscoveryTask() {
		// 11.0.0.1 - 11.0.3.239  - 11.0.1.254
		DiscoveryTask task = new DiscoveryTask();
		task.setEnableIcmp(true);
		// task.setIpRange("10.0.0.1-100");
		task.setDiscoveryType(DiscoveryType.INVENTORY.toString());
		task.setIpRange(Arrays.asList("10.0.0.191"));
		task.setLastRun(System.currentTimeMillis());
		task.setName("LocalDiscTask");
		task.setSchedule("* * * * * *");
		task.setStatus(DiscoveryStatus.CREATED);
		// List<SnmpAuthProfile> authProfiles = new ArrayList<>();
		// authProfiles.add(createAuthProfile());
		task.getAuthProfiles().add(createAuthProfile());
		return task;
	}



	private SnmpAuthProfile createAuthProfile() {
		SnmpAuthProfile authProfile = new SnmpAuthProfile();
		authProfile.setName("DefaultProfile");
		authProfile.setPort(1161);
		authProfile.setSnmpVersion(SnmpVersion.SNMPv2C);
		authProfile.setTimeout(3000);
		authProfile.setRetries(6);
		authProfile.setCommunity("public");
		return authProfile;
	}
        
        @Test
        public void testSendPingRequest() {
            boolean result = SnmpDiscovery.sendPingRequest("172.217.26.206");
            log.info(tracePrefix + "10.0.0.104 reachable: " + result);
        }

}
