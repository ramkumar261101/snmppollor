package ai.netoai.collector.startup;

import ai.netoai.collector.startup.CollectorMain;
import org.junit.Test;
import org.snmp4j.PDU;
import org.snmp4j.PDUv1;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.VariableBinding;

import ai.netoai.collector.convert.ConvertManager;

public class CollectorMainTest {

//	static {
//		PropertiesManager.init();
//		PropertiesManager.getInstance();
//		ConvertManager.init();
//	}
	
	@Test
	public void testEnsureTopic() {
		CollectorMain instance = new CollectorMain();
		instance.ensureTopic();
	}
	
	@Test
	public void testPduV1ToJson() {
		String peerAddress = "10.0.0.1";
		long recTime = System.currentTimeMillis();
		PDU pdu = createV1PDU();
		String pduJson = ConvertManager.getInstance().pduToJson(pdu, peerAddress, recTime);
		System.out.println(pduJson);
	}
	
	@Test
	public void testPduV2ToJson() {
		String peerAddress = "10.0.0.1";
		long recTime = System.currentTimeMillis();
		PDU pdu = createV2PDU();
		String pduJson = ConvertManager.getInstance().pduToJson(pdu, peerAddress, recTime);
		System.out.println(pduJson);
	}
	
	private PDU createV1PDU() {
		PDUv1 pdu = new PDUv1();
		pdu.setType(PDU.V1TRAP);
		pdu.setEnterprise(SnmpConstants.linkDown);
		pdu.setGenericTrap(3);
		pdu.setSpecificTrap(0);
		pdu.setTimestamp(1000l);
		pdu.setAgentAddress(new IpAddress("10.0.0.1"));
		
		VariableBinding vb1 = new VariableBinding(new OID("1.3.6.1.2.1.1.1"), new Integer32(10));
		VariableBinding vb2 = new VariableBinding(new OID("1.3.6.1.2.1.1.2"), new OctetString("GigabitE-1/10"));
		VariableBinding vb3 = new VariableBinding(new OID("1.3.6.1.2.1.1.3"), new OctetString("DOWN"));
		
		pdu.add(vb1);
		pdu.add(vb2);
		pdu.add(vb3);
		return pdu;
	}
	
	private PDU createV2PDU() {
		PDU pdu = new PDU();
		pdu.setType(PDU.TRAP);
		// OID - 1.3.6.1.4.1.1.0.5
		OID oid = new OID(SnmpConstants.linkUp);
		pdu.add(new VariableBinding(SnmpConstants.sysUpTime, new TimeTicks(1000)));
		pdu.add(new VariableBinding(SnmpConstants.snmpTrapOID, oid));
		
		VariableBinding vb1 = new VariableBinding(new OID("1.3.6.1.2.1.1.1"), new Integer32(10));
		VariableBinding vb2 = new VariableBinding(new OID("1.3.6.1.2.1.1.2"), new OctetString("GigabitE-1/10"));
		VariableBinding vb3 = new VariableBinding(new OID("1.3.6.1.2.1.1.3"), new OctetString("UP"));
		
		pdu.add(vb1);
		pdu.add(vb2);
		pdu.add(vb3);
		return pdu;
	}

	@Test
	public void createTopics() {
		System.setProperty("platform.home", "src/test/resources/");
		System.setProperty("nmsConfig.nms_system", "src/test/resources/nms_system.properties");
		CollectorMain main = new CollectorMain();
		main.createTopics();
	}

}
