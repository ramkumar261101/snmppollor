package ai.netoai.collector.snmp.discovery;

import ai.netoai.collector.snmp.discovery.NodeManager;
import org.junit.Test;


public class NodeManagerTest {
	@Test
	public void test() {
		/*for (int i = 0; i < 10000; i++) {
			NetworkElement ne = new NetworkElement();
			ne.setConnState(State.UNKNOWN);
			ne.setIp(i + "");
			ne.setTenantId(i + "");
			ne.setIpDomain(i + "");
			ne.setName(i + "");
			ne.setPort(i);
			ne.setProtocol(NetworkProtocol.SNMPv2c);
			ne.setDeviceType(DeviceType.ROUTER);
			ne.setVendorName("cisco");
			ne.setProductName(i + "");
			ne.setProductVersion(i + "");
			ne.setSysObjId(i + "");
			ne.setProfileId(i + "");
			ne.setLocation(i + "");
			ne.setSnmpTimeout(i);
			ne.setSnmpRetries(i);
			ne.setLastSyncTime(new Long(i + ""));
			ne.setEnableInventorySync(true);
			ne.setInvSyncInterval(i);
			ne.setCollectorId(i + "");
			ne.setCommunityString(i + "");
			
			NodeAdapter adapter = new NodeAdapter(ne);
			NodeManager.map.put(ne, adapter);
		}*/
		
		NodeManager manager = NodeManager.getInstance();
		manager.start();
	}

}
