package ai.netoai.collector.snmp.discovery;

import ai.netoai.collector.model.NetworkElement;
import ai.netoai.collector.model.NetworkProtocol;
import ai.netoai.collector.snmp.discovery.DeviceTypeIdentifier;
import org.junit.Test;

import ai.netoai.collector.snmp.SnmpPoller;

public class DeviceTypeIdentifierTest {

	@Test
	public void testLoadDiscoveryTree() {
		SnmpPoller.getInstance().start();
		SnmpPoller.getInstance().start();
		DeviceTypeIdentifier deviceIden = DeviceTypeIdentifier.getInstance();
		System.out.println("Done loading discovery tree");
		NetworkElement ne = createNe();
		String deviceType = deviceIden.identifyNE(ne);
		System.out.println("Identified device as: " + deviceType);
	}

	private NetworkElement createNe() {
		NetworkElement ne = new NetworkElement();
		ne.setIp("10.0.0.108");
		ne.setPort(1161);
		ne.setCommunityString("public");
		ne.setSnmpRetries(3);
		ne.setSnmpTimeout(3000);
		ne.setProtocol(NetworkProtocol.SNMPv2c);
		return ne;
	}

}
