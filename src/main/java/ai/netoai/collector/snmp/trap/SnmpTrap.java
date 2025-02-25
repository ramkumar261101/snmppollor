package ai.netoai.collector.snmp.trap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.PDU;
import org.snmp4j.PDUv1;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;

public class SnmpTrap {

	private static final Logger log = LoggerFactory.getLogger(SnmpTrap.class);

	private PDU pdu;
	private int trapType;
	/**
	 * Generic Trap value should be set based on 'snmpTrapOID' varbind value for
	 * 'v2c' traps. For 'v1' traps, the value should be set from 'GenericTrap'
	 * field of TrapV1 PDU.
	 */

	private int genericTrap;
	/**
	 * Specific trap value should be set to 0 for generic traps (for 'v1'
	 * traps). For 'v2c' traps, this should be set to 0 always.
	 */

	private int specificTrap;
	/**
	 * Trap OID value should be set from varbind ('snmpTrapOID' value) for 'v2c'
	 * traps and 'v1' generic traps. For 'v1' non-generic traps, this value is
	 * not set (this will be null).
	 */

	private OID trapOid;
	/**
	 * Trap OID value should be set from varbind ('snmpTrapEnterprise' value)
	 * for 'v2c' traps if found. If 'snmpTrapEnterprise' varbind not found, this
	 * is not set (this will be null). For 'v1' traps, the value is set from
	 * 'Enterprise' field of TrapV1 PDU.
	 */
	private OID enterpriseOid;
	/**
	 * Timestamp value should be set based on 'sysUpTime' varbind value for
	 * 'v2c' traps. For 'v1' traps, the value should be set from 'Timestamp'
	 * field of TrapV1 PDU.
	 */
	private long timeStamp;
	private List<VariableBinding> varBind;
	private String agentAddress;

	public SnmpTrap(PDU pdu) {
		this.pdu = pdu;
	}

	public String getAgentAddress() {
		return agentAddress;
	}

	public void setAgentAddress(String agentAddress) {
		this.agentAddress = agentAddress;
	}

	public int getTrapType() {
		return trapType;
	}

	public void setTrapType(int trapType) {
		this.trapType = trapType;
	}

	public int getGenericTrap() {
		return genericTrap;
	}

	public void setGenericTrap(int genericTrap) {
		this.genericTrap = genericTrap;
	}

	public int getSpecificTrap() {
		return specificTrap;
	}

	public void setSpecificTrap(int specificTrap) {
		this.specificTrap = specificTrap;
	}

	public OID getTrapOid() {
		return trapOid;
	}

	public void setTrapOid(OID trapOid) {
		this.trapOid = trapOid;
	}

	public OID getEnterpriseOid() {
		return enterpriseOid;
	}

	public void setEnterpriseOid(OID enterpriseOid) {
		this.enterpriseOid = enterpriseOid;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}

	public List<VariableBinding> getVarBind() {
		return varBind;
	}

	public void setVarBind(List<VariableBinding> varBind) {
		this.varBind = varBind;
	}

	public SnmpTrap construct() {
		if (this.pdu == null) {
			return null;
		}

		this.setTrapType(this.pdu.getType());

		switch (this.getTrapType()) {
			// Handle V1TRAP PDUs
			case PDU.V1TRAP:
				PDUv1 pduV1 = (PDUv1) this.pdu;
				this.setGenericTrap(pduV1.getGenericTrap());

				switch (pduV1.getGenericTrap()) {
					case PDUv1.COLDSTART: // coldStart
						this.setSpecificTrap(0);
						this.setTrapOid(SnmpConstants.coldStart);
						break;

					case PDUv1.WARMSTART: // warmStart
						this.setSpecificTrap(0);
						this.setTrapOid(SnmpConstants.warmStart);
						break;

					case PDUv1.LINKDOWN: // linkDown
						this.setSpecificTrap(0);
						this.setTrapOid(SnmpConstants.linkDown);
						break;

					case PDUv1.LINKUP: // linkUp
						this.setSpecificTrap(0);
						this.setTrapOid(SnmpConstants.linkUp);
						break;

					case PDUv1.AUTHENTICATIONFAILURE: // authenticationFailure
						this.setSpecificTrap(0);
						this.setTrapOid(SnmpConstants.authenticationFailure);
						break;

					case (5): // egpNeighborLoss
						this.setSpecificTrap(0);
						this.setTrapOid(new OID(
								new int[] { 1, 3, 6, 1, 6, 3, 1, 1, 5, 6 }));
						break;

					default: // enterpriseSpecific
						this.setGenericTrap(6);
						this.setSpecificTrap(pduV1.getSpecificTrap());
				}
				this.setEnterpriseOid(pduV1.getEnterprise());
				this.setTimeStamp(pduV1.getTimestamp());
				this.setVarBind(new ArrayList<VariableBinding>(
						pduV1.getVariableBindings()));

				break;

			// Handle TRAP(v2c) and INFORM PDUs
			case PDU.TRAP:
			case PDU.INFORM:
				PDU pduV2c = this.pdu;
				VariableBinding[] varBinds = pduV2c.toArray();

				int genericTrapV2c = 6;
				OID trapOID = null;
				OID trapEnterprise = null;
				Long sysUpTime = null;
				ArrayList<VariableBinding> varBindList = new ArrayList<VariableBinding>();

				for (VariableBinding vb : varBinds) {
					if (vb.getOid().equals(SnmpConstants.sysUpTime)) {
						Variable vbVariable = vb.getVariable();
						if (vbVariable != null) {
							if (vbVariable instanceof TimeTicks)
								sysUpTime = ((TimeTicks) vbVariable)
										.toMilliseconds();
							else {
								try {
									sysUpTime = vbVariable.toLong() * 10;
								} catch (Exception e) {
									sysUpTime = 0l;
									log.error(
											"Error retrieving sysUpTime value from trap, setting it to '0'",
											e);
								}
							}
						}
					} else if (vb.getOid().equals(SnmpConstants.snmpTrapOID)) {
						trapOID = (OID) vb.getVariable();
					} else if (vb.getOid()
							.equals(SnmpConstants.snmpTrapEnterprise)) {
						trapEnterprise = (OID) vb.getVariable();
					} else {
						varBindList.add(vb);
					}
				}

				if (trapOID == null || sysUpTime == null) {
					throw new IllegalArgumentException(
							"Invalid VarBinds in V2c Trap : " + pduV2c);
				}

				if (trapOID.equals(SnmpConstants.coldStart)) { // coldStart
					genericTrapV2c = 0;
				} else if (trapOID.equals(SnmpConstants.warmStart)) { // warmStart
					genericTrapV2c = 1;
				} else if (trapOID.equals(SnmpConstants.linkDown)) { // linkDown
					genericTrapV2c = 2;
				} else if (trapOID.equals(SnmpConstants.linkUp)) { // linkUp
					genericTrapV2c = 3;
				} else if (trapOID
						.equals(SnmpConstants.authenticationFailure)) { // authenticationFailure
					genericTrapV2c = 4;
				} else if (trapOID.equals(
						new OID(new int[] { 1, 3, 6, 1, 6, 3, 1, 1, 5, 6 }))) { // egpNeighborLoss
					genericTrapV2c = 5;
				} else { // enterpriseSpecific
					genericTrapV2c = 6;
				}
				this.setTrapOid(trapOID);
				this.setGenericTrap(genericTrapV2c);
				this.setSpecificTrap(0);
				this.setEnterpriseOid(trapEnterprise);
				this.setTimeStamp(sysUpTime);
				this.setVarBind(varBindList);

				break;

			// Return if not a TRAP/INFORM PDU
			default:
				return null;
		}

		return this;
	}
        
        
        public Map<Object, Object> getEvalProperties() {
            Map<Object, Object> props = new HashMap();
            props.put("trapOid", getTrapOid() != null ? getTrapOid().toString() : null);
            props.put("enterpriseOid", getEnterpriseOid() != null ? getEnterpriseOid().toString() : null);
            props.put("genericTrap", getGenericTrap());
            props.put("specificTrap", getSpecificTrap());
            
            return props;
        }

}

