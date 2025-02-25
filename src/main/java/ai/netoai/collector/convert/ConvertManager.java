package ai.netoai.collector.convert;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ai.netoai.collector.utils.JsonGenerator;
import org.apache.commons.net.util.SubnetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.PDU;
import org.snmp4j.PDUv1;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.SMIConstants;
import org.snmp4j.smi.VariableBinding;

import ai.netoai.collector.cache.CacheManager;

public class ConvertManager {
	private static final Logger log = LoggerFactory.getLogger(ConvertManager.class);
	private static final String tracePrefix = "[" + ConvertManager.class.getSimpleName() + "]: ";
	private static ConvertManager instance;
	private ExecutorService trapConvertExecutor;
	private int executorThreads = 20;
	private boolean oidSearchEnabled;
	
	private ConvertManager() {}
	
	public static void init() {
		if ( instance != null ) {
			throw new IllegalStateException("Manager already initialized");
		}
		instance = new ConvertManager();
	}
	
	public static ConvertManager getInstance() {
		if ( instance == null ) {
			throw new IllegalStateException("Manager not initialized");
		}
		return instance;
	}
	
	public void start() {
		this.trapConvertExecutor = Executors.newFixedThreadPool(executorThreads);
		log.info(tracePrefix + "Started ...");
	}
	
	private Object findDeviceByNetMask(String address) {
		List<String> deviceIps = CacheManager.getInstance().getAllDeviceIps();
		log.info(tracePrefix + "All DeviceIps: " + deviceIps);
		for(String deviceIp : deviceIps) {
			if ( deviceIp.contains("/") ) {
				SubnetUtils utils = new SubnetUtils(deviceIp);
				boolean inRange = utils.getInfo().isInRange(address);
				log.info(tracePrefix + " Is " + address + " inRange of " + deviceIp + ": " + inRange);
				if ( inRange ) {
					return CacheManager.getInstance().getDeviceTypeByIp(deviceIp);
				}
			} else {
				log.info(tracePrefix + "DeviceIp: " + deviceIp + " does not contain /");
			}
		}
		return null;
	}
	

	
	private List<Map<String, Object>> varBindsToJson(VariableBinding[] vbs) {
		List<Map<String, Object>> list = new ArrayList<>();
		for(VariableBinding vb : vbs) {
			Map<String, Object> vbMap = new LinkedHashMap<>();
			vbMap.put("oid", vb.getOid().toDottedString());
			vbMap.put("syntax", vb.getVariable().getSyntaxString());
			if ( vb.getVariable().getSyntax() == SMIConstants.SYNTAX_INTEGER ||
					vb.getVariable().getSyntax() == SMIConstants.SYNTAX_INTEGER32 ||
					vb.getVariable().getSyntax() == SMIConstants.SYNTAX_UNSIGNED_INTEGER32 ) {
				vbMap.put("value", vb.getVariable().toInt());
			} else if ( vb.getVariable().getSyntax() == SMIConstants.SYNTAX_COUNTER32 ||
					vb.getVariable().getSyntax() == SMIConstants.SYNTAX_COUNTER64 ||
					vb.getVariable().getSyntax() == SMIConstants.SYNTAX_GAUGE32 ||
					vb.getVariable().getSyntax() == SMIConstants.SYNTAX_TIMETICKS ) {
				vbMap.put("value", vb.getVariable().toLong());
			} else {
				vbMap.put("value", vb.getVariable().toString());
			}
			list.add(vbMap);
		}
		return list;
	}
	
	/**
	 * This is a customized method to convert PDU into JSON String.
	 * @param pdu actual data unit to be converted
	 * @param peerAddress the IP address of device from where the trap has originated
	 * @param recTime the receiveTime of trap when it hits the SNMP Adapter.
         * 
         * @return returns a json formatted trap
	 */
	public String pduToJson(PDU pdu, String peerAddress, long recTime) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("type", pdu.getType());
		map.put("agentAddress", peerAddress);
		map.put("receivedTime", recTime);
		if ( pdu.getType() == PDU.V1TRAP ) {
			PDUv1 v1Pdu = (PDUv1) pdu;
			map.put("version", SnmpConstants.version1);
			map.put("enterprise", v1Pdu.getEnterprise().toDottedString());
			map.put("genericTrap", v1Pdu.getGenericTrap());
			map.put("specificTrap", v1Pdu.getSpecificTrap());
			map.put("timeStamp", v1Pdu.getTimestamp());
			VariableBinding[] vbs = v1Pdu.toArray();
			map.put("variableBindings", varBindsToJson(vbs));
		} else if ( pdu.getType() == PDU.TRAP || pdu.getType() == PDU.NOTIFICATION ) {
			map.put("version", SnmpConstants.version2c);
			map.put("variableBindings", varBindsToJson(pdu.toArray()));
		}
		return JsonGenerator.getJSONString(map, false);
	}
	
}
