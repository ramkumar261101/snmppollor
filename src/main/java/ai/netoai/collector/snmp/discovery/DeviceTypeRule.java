package ai.netoai.collector.snmp.discovery;

import java.util.List;
import java.util.regex.Pattern;

import org.snmp4j.PDU;
import org.snmp4j.Target;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;

import ai.netoai.collector.snmp.SnmpPoller;

public class DeviceTypeRule {
	
	public enum OpType {
		SNMP_GET,
		SNMP_PING;
	}
	
	private String oid;
	private String startingOid;
	private String regexp;
	private OpType opType;
	
	public DeviceTypeRule() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * @param oid oid to be evaluated for ping test
	 * @param startingOid oid to be used to get value for get test
	 * @param regExp value to be matched
	 * @param opType type of request get/ping
	 */
	public DeviceTypeRule(String oid, String startingOid, String regExp, OpType opType) {
		super();
		this.oid = oid;
		this.startingOid = startingOid;
		this.regexp = regExp;
		this.opType = opType;
	}

	/**
	 * @return the oid
	 */
	public String getOid() {
		return oid;
	}
	/**
	 * @param oid the oid to set
	 */
	public void setOid(String oid) {
		this.oid = oid;
	}
	/**
	 * @return the startingOid
	 */
	public String getStartingOid() {
		return startingOid;
	}
	/**
	 * @param startingOid the startingOid to set
	 */
	public void setStartingOid(String startingOid) {
		this.startingOid = startingOid;
	}
	/**
	 * @return the regExp
	 */
	public String getRegexp() {
		return regexp;
	}
	/**
	 * @param regExp the regExp to set
	 */
	public void setRegexp(String regExp) {
		this.regexp = regExp;
	}
	/**
	 * @return the opType
	 */
	public OpType getOpType() {
		return opType;
	}
	/**
	 * @param opType the opType to set
	 */
	public void setOpType(OpType opType) {
		this.opType = opType;
	}
	
	public boolean evaluate(Target target, List<VariableBinding> varbindCache,int deviceSeed) {
		OID currentOid = null;
		if ( opType == OpType.SNMP_GET ) {
			currentOid = new OID(this.oid);
		} else {
			currentOid = new OID(this.startingOid);
		}
		boolean oidFoundInCache = false;
		if ( varbindCache != null ) {
			for(VariableBinding vb : varbindCache) {
				if ( vb.getOid().equals(currentOid) ) {
					oidFoundInCache = true;
					if ( opType == OpType.SNMP_PING ) {
						// We have got the required OID from device
						// so returning true
						return true;
					}
					break;
				}
			}
		}
		if ( oidFoundInCache ) {
			return evaluate(varbindCache);
		}
		
		PDU reqPdu = new PDU();
		reqPdu.setType(PDU.GET);
		reqPdu.add(new VariableBinding(currentOid));
		PDU respPdu = SnmpPoller.getInstance().sendSyncGetRequest(target, reqPdu, deviceSeed);
		
		if ( respPdu == null || respPdu.getErrorStatus() != SnmpConstants.SNMP_ERROR_SUCCESS ) {
			return false;
		}
		VariableBinding[] vbs = respPdu.toArray();
		if ( opType == OpType.SNMP_GET ) {
			
			if ( varbindCache != null ) {
				// If cache is present, cache the OIDs
				for(VariableBinding vb : vbs) {
					if ( vb == null || vb.isException() || vb.getOid() == null || vb.getVariable() == null ) {
						continue;
					}
					varbindCache.add(vb);
				}
				return evaluate(vbs);
			} else {
				// Else WARNING WARNING There will be too many SNMP requests
				// And finding a right device type will take lot of time.
				return evaluate(vbs);
			}
		} else {
			// IF this is ping type of rule
			VariableBinding vb = vbs[0];
			if ( vb == null || vb.isException() || vb.getOid() == null || vb.getVariable() == null ) {
				return false;
			} else {
				varbindCache.add(vb);
				return true;
			}
		}
	}
	
	private boolean evaluate(List<VariableBinding> varbindCache) {
		VariableBinding[] vbs = varbindCache.toArray(new VariableBinding[]{});
		return evaluate(vbs);
	}

	private boolean evaluate(VariableBinding[] vbs) {
		OID currentOid = null;
		Pattern regExpPattern = null;
		if ( opType == OpType.SNMP_GET ) {
			currentOid = new OID(this.oid);
			regExpPattern = Pattern.compile(this.regexp);
		} else {
			currentOid = new OID(this.startingOid);
		}
		for(VariableBinding vb : vbs) {
			if ( vb == null || vb.isException() || vb.getOid() == null || vb.getVariable() == null ) {
				continue;
			}
			if ( vb.getOid().equals(currentOid) ) {
				if ( regExpPattern.matcher(vb.getVariable().toString()).matches() ) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public String toString() {
		if ( opType == OpType.SNMP_GET ) {
			return oid + " " + opType + " " + regexp;
		} else {
			return opType + " " + startingOid;
		}
	}
        
}
