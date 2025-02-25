package ai.netoai.collector.snmp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import ai.netoai.collector.Constants;
import ai.netoai.collector.cache.CacheManager;
import ai.netoai.collector.model.*;
import ai.netoai.collector.settings.SettingsManager;
import ai.netoai.collector.settings.SnmpSettings;
import ai.netoai.collector.snmp.mapping.MuxTransportMapping;
import ai.netoai.collector.snmp.mapping.MuxUdpTransportMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.AbstractTarget;
import org.snmp4j.CommunityTarget;
import org.snmp4j.MessageDispatcher;
import org.snmp4j.MessageDispatcherImpl;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.TransportMapping;
import org.snmp4j.UserTarget;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.mp.MPv1;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.TSM;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TcpAddress;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultTcpTransportMapping;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SnmpPoller {
	private static final Logger log = LoggerFactory.getLogger(SnmpPoller.class);
	private static final String tracePrefix = "[" + SnmpPoller.class.getSimpleName() + "]: ";

	private static final String IF_INDEX_OID = "1.3.6.1.2.1.2.2.1.1";
	private static final String IF_DESCR_OID = "1.3.6.1.2.1.2.2.1.2";
	private static final String IF_TYPE_OID = "1.3.6.1.2.1.2.2.1.3";
	private static final String IF_SPEED_OID = "1.3.6.1.2.1.2.2.1.5";
	private static final String IF_MAC_OID = "1.3.6.1.2.1.2.2.1.6";
	private static final String IF_ADMIN_OID = "1.3.6.1.2.1.2.2.1.7";
	private static final String IF_OPER_OID = "1.3.6.1.2.1.2.2.1.8";
	private static final String IFX_NAME_OID = "1.3.6.1.2.1.31.1.1.1.1";
	private static final String IFX_SPEED_OID = "1.3.6.1.2.1.31.1.1.1.15";
	private static final String IFX_ALIAS_OID = "1.3.6.1.2.1.31.1.1.1.18";

	private static SnmpPoller instance;
//	private static CollectorSettings settings;
	private static SettingsManager sm;
	private static final int MIN_PORT_NUMBER = 33000;
	private static final int MAX_PORT_NUMBER = 65500;

	private List<Snmp> snmpPool = new ArrayList<>();
	private MuxTransportMapping transportMapping;
	private List<TransportMapping> transportMappings = new ArrayList<TransportMapping>();
	private Address listenAddress;
	private SettingsManager settings;
	private SnmpSettings snmpSettings;
	private Map<String,Object> collectorsettingsMap;
	private static AtomicInteger globalCounter = new AtomicInteger(0);
        private AtomicBoolean started = new AtomicBoolean(false);

	private SnmpPoller() {
		settings = SettingsManager.getInstance();
		snmpSettings = new SnmpSettings();

		this.collectorsettingsMap = settings.getSettings();
	}

	public synchronized static SnmpPoller getInstance() {
		if (instance == null) {
			instance = new SnmpPoller();
		}
		return instance;
	}

	public void start() {
            if ( started.get() ) {
                log.info(tracePrefix + "Already started, returning ...");
                return;
            }
            started.set(true);
		initSnmp();
		log.info(tracePrefix + "Started Snmp listen ...");
	}

	public void stop() {
		try {
			for (Snmp snmp : snmpPool) {
				if (snmp != null) {
					snmp.close();
				}
			}
		} catch (Exception ex) {
			log.error(tracePrefix + "Failed closing Snmp instances", ex);
		}
	}

	private int getListenPort(String address) {
		Pattern p = Pattern.compile(".*:.*\\/(\\d+)$");
		Matcher m = p.matcher(address);
		if (m.matches()) {
			return Integer.parseInt(m.group(1));
		}
		return 37000;
	}

	private String getListenAddress(String address) {
		Pattern p = Pattern.compile(".*:(.*)\\/\\d+$");
		Matcher m = p.matcher(address);
		if (m.matches()) {
			return m.group(1);
		}
		return "0.0.0.0";
	}

	private String getListenProtocol(String address) {
		Pattern p = Pattern.compile("(.*):.*\\/\\d+$");
		Matcher m = p.matcher(address);
		if (m.matches()) {
			return m.group(1);
		}
		return "udp";
	}

	private String getAddressString(String protocol, String address, int port) {
		return protocol + ":" + address + "/" + port;
	}

	public MuxTransportMapping getMultiplexedTransportMapping(String callerId, int snmpUdpSocketTimeout) {
		return (MuxTransportMapping) getNextAvailableTransportMapping(callerId, snmpUdpSocketTimeout, true, null);
	}

	private TransportMapping getNextAvailableTransportMapping(String callerId, int snmpUdpSocketTimeout,
			boolean isMultiplexed, MuxTransportMapping multiplexedMapping) {
		if (this.listenAddress == null) {
			log.error(
					"Severe Error!!! listen address for this Poller is null. SNMP will not function correctly for "
							+ callerId);
			return null;
		}

		TransportMapping transportMapping = null;

		synchronized (this.listenAddress) {
			try {
				transportMapping = bindTransportMapping(this.listenAddress, 0, 5000, snmpUdpSocketTimeout,
						isMultiplexed, multiplexedMapping);

				log.info(tracePrefix + "Found available address : "
						+ transportMapping.getListenAddress() + " for caller " + callerId);

				// set listen Address to the next possible address
				this.listenAddress = getNextPossibleAddress(transportMapping.getListenAddress());
			} catch (Exception x) {
				log.error("ERROR getting NextAvailableTransportMapping for caller " + callerId
						+ ". Error while initializing transport mapping. Using null transport mapping.", x);
				// throw x;
				// return;
				// not returning or throwing exception, but using null
				// transport mapping to initialize Snmp object. This would throw
				// exception while sending SNMP messages, but can still receive
				// traps
				// if trap receiver got bound successfully during SnmpEntity
				// init.
				transportMapping = null;
			}
		}

		return transportMapping;
	}

	private TransportMapping bindTransportMapping(Address listenAddress, int retryCount, int maxRetryCount,
			int snmpUdpSocketTimeout, boolean isMultiplexed, MuxTransportMapping multiplexedMapping) {
		try {
			TransportMapping transport = null;

			if (listenAddress instanceof UdpAddress) {
				if (isMultiplexed) {
					if (multiplexedMapping != null) {
						multiplexedMapping.addListenAddress((UdpAddress) listenAddress);
						TransportMapping udpTransport = multiplexedMapping
								.getTransportMapping((UdpAddress) listenAddress);
						transport = udpTransport;
					} else {
						MuxUdpTransportMapping udpTransport = new MuxUdpTransportMapping((UdpAddress) listenAddress);
						udpTransport.setSocketTimeout(snmpUdpSocketTimeout);
						transport = udpTransport;
					}
				} else {
					DefaultUdpTransportMapping udpTransport = new DefaultUdpTransportMapping(
							(UdpAddress) listenAddress);
					udpTransport.setSocketTimeout(snmpUdpSocketTimeout);
					transport = udpTransport;
				}
			} else {
				transport = new DefaultTcpTransportMapping((TcpAddress) listenAddress);
			}

			return transport;
		} catch (IOException ioe) {
			log.warn("SnmpEntity.initTransportMapping(): Error binding address : " + listenAddress, ioe);
			retryCount += 1;
			if (retryCount > maxRetryCount) {
				log.error("SnmpEntity.initTransportMapping(): Exceeded max retries, giving up");
				return null;
			}

			listenAddress = getNextPossibleAddress(listenAddress);

			log.warn("SnmpEntity.initTransportMapping(): Trying address ( retry = " + retryCount + " ) : "
					+ listenAddress);

			// call recursively
			return bindTransportMapping(listenAddress, retryCount, maxRetryCount, snmpUdpSocketTimeout, isMultiplexed,
					multiplexedMapping);
		}
	}

	private Address getNextPossibleAddress(Address address) {
		// try new address using next available port
		if (address instanceof UdpAddress) {
			UdpAddress uAddr = (UdpAddress) address;
			int port = uAddr.getPort() + 1;
			if (port == MAX_PORT_NUMBER)
				port = MIN_PORT_NUMBER;

			return (new UdpAddress(uAddr.getInetAddress(), port));
		} else {
			TcpAddress tAddr = (TcpAddress) address;
			int port = tAddr.getPort() + 1;
			if (port == MAX_PORT_NUMBER)
				port = MIN_PORT_NUMBER;

			return (new TcpAddress(tAddr.getInetAddress(), port));
		}
	}

	public MuxTransportMapping getMultiplexedMapping() {
		return this.transportMapping;
	}

	public TransportMapping getExtendedTransportMapping(String callerId, int snmpUdpSocketTimeout,
			MuxTransportMapping multiplexedMapping) {
		return getNextAvailableTransportMapping(callerId, snmpUdpSocketTimeout, true, multiplexedMapping);
	}
	
	private Address getTransportListenAddress(String addressString) {
		Address listenAddress = null;

		try {
			listenAddress = GenericAddress.parse(addressString);
		} catch (Exception e) {
			log.info(tracePrefix + "Exception while parsing address string : " + addressString, e);

			listenAddress = GenericAddress.parse("udp:0.0.0.0/" + MIN_PORT_NUMBER);
			log.info(tracePrefix + "Using default Address : " + listenAddress);
		}

		if (listenAddress == null) {
			listenAddress = GenericAddress.parse("udp:0.0.0.0/" + MIN_PORT_NUMBER);
			log.info(tracePrefix + "Found null address, using default Address : " + listenAddress);
		}

		return listenAddress;
	}

	private void initSnmp() {
		int poolSize = snmpSettings.getSnmpPoolSize();
		String listenAddress = collectorsettingsMap.get("listenAddress").toString();
		int port = getListenPort(listenAddress);
		String ipAddress = getListenAddress(listenAddress);
		String protocol = getListenProtocol(listenAddress);

		log.info("SNMP POOL SIZE "+poolSize);
		String threadPoolName = "GloblaMuxTransportMapping";
		this.listenAddress = getTransportListenAddress(listenAddress);
		int socketTimeout = Integer.parseInt(collectorsettingsMap.get("udpSocketTimeout").toString());
		this.transportMapping = getMultiplexedTransportMapping(threadPoolName,socketTimeout);
		this.transportMapping.setProcessingParameters(snmpSettings.getSnmpPoolSize(), 1000);
		for (int i = 0; i < poolSize; i++) {
			try {

				String snmpName = "Snmp:" + i;

				TransportMapping<?> transportMapping = null;
				MessageDispatcher msgDispatcher = null;


				MuxTransportMapping multiplexor = getMultiplexedMapping();
				transportMapping = getExtendedTransportMapping(snmpName, socketTimeout, multiplexor);

				this.transportMappings.add(transportMapping);


				msgDispatcher = new MessageDispatcherImpl();
				Snmp snmp = new Snmp(msgDispatcher, transportMapping);

				USM usm = new USM(SecurityProtocols.getInstance(),
			                new OctetString(MPv3.createLocalEngineID()), 0);
				SecurityModels.getInstance().addSecurityModel(usm);
			    
				snmp.getMessageDispatcher().addMessageProcessingModel(new MPv1());
				snmp.getMessageDispatcher().addMessageProcessingModel(new MPv2c());

				snmp.listen();

				this.snmpPool.add(snmp);

			} catch (Throwable t) {
				log.error(tracePrefix + "Failed creating Snmp instance", t);
			}
			port++;
		}
	}

	public List<String> getInterfaceIndices(PDU requestPdu, Target target) {
		target.setRetries(3);
		target.setTimeout(3000);
		List<String> list = new ArrayList<String>();
		OID tableOid = requestPdu.get(0).getOid();
		try {
			VariableBinding lastVb = null;
			while (true) {
				// log.debug(tracePrefix + "Request: " + requestPdu + ", Reps: "
				// + requestPdu.getMaxRepetitions());
				SnmpManager.getInstance().incrementReqSent();
				int index = (globalCounter.incrementAndGet()) % snmpPool.size();
				ResponseEvent re = this.snmpPool.get(index).send(requestPdu, target);
				PDU response = re.getResponse();
				SnmpManager.getInstance().incrementRespRec();
				if (response == null) {
					log.error(tracePrefix + "Response Timed out, will wait for retry ...");
					SnmpManager.getInstance().incrementRespTimeouts();
					return list;
				}
				log.debug(tracePrefix + "Request: " + re.getRequest());
				log.debug(tracePrefix + response);
				Vector<VariableBinding> vbs = (Vector<VariableBinding>) response.getVariableBindings();
				if (vbs == null || vbs.isEmpty()) {
					SnmpManager.getInstance().incrementRespProcessed();
					return list;
				}

				for (int i = 0; i < vbs.size(); i++) {
					lastVb = vbs.get(i);
					if (lastVb.getOid().startsWith(tableOid)) {
						list.add(lastVb.getVariable().toString());
					} else {
						lastVb = null;
						break;
					}
				}
				if (lastVb != null) {
					requestPdu.set(0, new VariableBinding(lastVb.getOid()));
					requestPdu.setMaxRepetitions(snmpSettings.getGetBulkMaxRep());
					requestPdu.setRequestID(new Integer32(0));
					requestPdu.setType(PDU.GETBULK);
					requestPdu.setNonRepeaters(0);
				} else {
					if (log.isTraceEnabled()) {
						log.trace(tracePrefix + target + ", Found a different OID so quitting");
					}
					break;
				}
			}
		} catch (Throwable t) {
			log.error(tracePrefix + "Failed to collect device indices for " + target, t);
		}
		SnmpManager.getInstance().incrementRespProcessed();
		return list;
	}

	public List<VariableBinding> getResponseVariableBindings(String tableOidStr, String colSuffix, Target target) {
		OID oid = new OID(tableOidStr);
		oid = oid.append(colSuffix);
		if (log.isTraceEnabled()) {
			log.trace(tracePrefix + "Table OID formed: " + oid);
		}
		PDU requestPdu = new PDU();
		requestPdu.add(new VariableBinding(oid));
		requestPdu.setMaxRepetitions(snmpSettings.getGetBulkMaxRep());
		requestPdu.setType(PDU.GETBULK);
		requestPdu.setNonRepeaters(0);

		target.setRetries(3);
		target.setTimeout(3000);
		List<VariableBinding> list = new ArrayList<VariableBinding>();
		OID tableOid = requestPdu.get(0).getOid();
		try {
			VariableBinding lastVb = null;
			while (true) {
				// log.debug(tracePrefix + "Request: " + requestPdu + ", Reps: "
				// + requestPdu.getMaxRepetitions());
				int index = (globalCounter.incrementAndGet()) % snmpPool.size();
				ResponseEvent re = this.snmpPool.get(index).send(requestPdu, target);
				PDU response = re.getResponse();
				if (response == null) {
					log.error(tracePrefix + "Response Timed out, will wait for retry ...");
					return list;
				}
				log.debug(tracePrefix + "Request: " + re.getRequest());
				log.debug(tracePrefix + response);
				Vector<VariableBinding> vbs = (Vector<VariableBinding>) response.getVariableBindings();
				if (vbs == null || vbs.isEmpty()) {
					return list;
				}

				for (int i = 0; i < vbs.size(); i++) {
					lastVb = vbs.get(i);
					if (lastVb.getOid().startsWith(tableOid)) {
						list.add(lastVb);
					} else {
						lastVb = null;
						break;
					}
				}
				if (lastVb != null) {
					requestPdu.set(0, new VariableBinding(lastVb.getOid()));
					requestPdu.setMaxRepetitions(snmpSettings.getGetBulkMaxRep());
					requestPdu.setRequestID(new Integer32(0));
					requestPdu.setType(PDU.GETBULK);
					requestPdu.setNonRepeaters(0);
				} else {
					if (log.isTraceEnabled()) {
						log.trace(tracePrefix + target + ", Found a different OID so quitting");
					}
					break;
				}
			}
		} catch (Throwable t) {
			log.error(tracePrefix + "Failed to collect device indices for " + target, t);
		}
		return list;
	}

	public void sendAsyncRequest(Target target, PDU reqPdu, ResponseListener respListener, Object userObj,
			int deviceSeed) {
		if (reqPdu == null || target == null) {
			log.warn(tracePrefix + "Not sending SNMP request information not found, Target: [" + target
					+ "], RequestPDU: [" + reqPdu + "]");
			return;
		}
		try {
			if (log.isDebugEnabled())
				log.debug(tracePrefix + reqPdu);
			int index = deviceSeed % snmpPool.size();
			if (index >= snmpPool.size()) {
				log.error(tracePrefix + "**** ERROR **** Invalid device seed found: " + deviceSeed
						+ ", Snmp Pool size: " + snmpPool.size());
				index = 0;
			}
			this.snmpPool.get(index).send(reqPdu, target, userObj, respListener);
		} catch (Exception ex) {
			log.error(tracePrefix + "Failed sending SNMP request Target: " + target + ", RequestPDU: " + reqPdu, ex);
		}
	}

	public PDU sendSyncGetRequest(Target target, PDU reqPdu,int deviceSeed) {	
		if (reqPdu == null || target == null) {
			log.warn(tracePrefix + "Not sending SNMP request information not found, Target: [" + target
					+ "], RequestPDU: [" + reqPdu + "]");
			return null;
		}
		try {
			if (log.isDebugEnabled())
				log.debug(tracePrefix + reqPdu);
			
			int index = (globalCounter.incrementAndGet()) % this.snmpPool.size();
			ResponseEvent re = this.snmpPool.get(index).send(reqPdu, target);
			return re.getResponse();
		} catch (Exception ex) {
			log.error(tracePrefix + "Failed sending SNMP request Target: " + target + ", RequestPDU: " + reqPdu, ex);
		}
		return null;
	}
	
	public PDU sendSyncGetRequest(Target target, PDU reqPdu) {	
		if (reqPdu == null || target == null) {
			log.warn(tracePrefix + "Not sending SNMP request information not found, Target: [" + target
					+ "], RequestPDU: [" + reqPdu + "]");
			return null;
		}
		try {
			if (log.isDebugEnabled())
				log.debug(tracePrefix + reqPdu);
			
			int index = (globalCounter.incrementAndGet()) % this.snmpPool.size();
			ResponseEvent re = this.snmpPool.get(index).send(reqPdu, target);
			return re.getResponse();
		} catch (Exception ex) {
			log.error(tracePrefix + "Failed sending SNMP request Target: " + target + ", RequestPDU: " + reqPdu, ex);
		}
		return null;
	}

	private SnmpEndPoint createNodeEndPoint(String deviceIp, String name, String network, String subnet) {
		SnmpEndPoint ep = new SnmpEndPoint();
		ep.setNode(deviceIp);
		ep.setSourceIndex(deviceIp);
		ep.setAdminStatus(true);
		ep.setOpStatus(true);
		ep.setName(name);
		ep.setNetwork(network);
		ep.setSubNetwork(subnet);
		ep.setMac("NA");
		ep.setNodeName(name);
		ep.setSourceId(deviceIp);
		ep.setSourceType("Node");
		ep.setSpeed(0l);
		return ep;
	}

	/**
         * Polls the list of endpoints
	 * @param target the target network element to be polled
	 * @param deviceIp IP of the network element to be polled
	 * @param idxs list of end point indexes
	 * @return list of end point objects
	 */
	public List<SnmpEndPoint> pollEndPoints(Target target, String deviceIp, List<String> idxs) {
		ISNode node = (ISNode) CacheManager.getInstance().getDeviceTypeByIp(deviceIp);
		List<SnmpEndPoint> list = new ArrayList<>();
		if (deviceIp == null || idxs == null || idxs.isEmpty()) {
			log.error(tracePrefix + "DeviceIP null or Blank indexes received: " + deviceIp + "/" + idxs);
			return list;
		}
		String network = "";
		String subNetwork = "";
		for (String idx : idxs) {
			SnmpEndPoint ep = new SnmpEndPoint();
			ep.setNode(deviceIp);
			ep.setNodeName(node.getName());
			if (node.getProperties().containsKey(Constants.NETWORK)) {
				network = node.getProperties().get(Constants.NETWORK);
				ep.setNetwork(network);
			} else if (node.getProperties().containsKey(Constants.NETWORK.toLowerCase())) {
				network = node.getProperties().get(Constants.NETWORK.toLowerCase());
				ep.setNetwork(network);
			}
			if (node.getProperties().containsKey(Constants.SUB_NETWORK)) {
				subNetwork = node.getProperties().get(Constants.SUB_NETWORK);
				ep.setSubNetwork(subNetwork);
			} else if (node.getProperties().containsKey(Constants.SUB_NETWORK.toLowerCase())) {
				subNetwork = node.getProperties().get(Constants.SUB_NETWORK.toLowerCase());
				ep.setSubNetwork(subNetwork);
			}
			String val = pollValue(IF_INDEX_OID + "." + idx, target);
			if (val != null) {
				ep.setSourceIndex(val + "_0");
				ep.setSourceId(val);
			}

			val = pollValue(IF_DESCR_OID + "." + idx, target);
			if (val != null) {
				ep.setName(val);
			}

			val = pollValue(IF_TYPE_OID + "." + idx, target);
			if (val != null) {
				ep.setSourceType(val);
			}

			val = pollValue(IF_SPEED_OID + "." + idx, target);
			if (val != null) {
				Long lval = Long.parseLong(val);
				ep.setSpeed(lval);
			}

			val = pollValue(IF_MAC_OID + "." + idx, target);
			if (val != null) {
				ep.setMac(val);
			}

			val = pollValue(IF_ADMIN_OID + "." + idx, target);
			if (val != null) {
				if (val.equals("1")) {
					ep.setAdminStatus(true);
				} else {
					ep.setAdminStatus(false);
				}
			}

			val = pollValue(IF_OPER_OID + "." + idx, target);
			if (val != null) {
				if (val.equals("1")) {
					ep.setOpStatus(true);
				} else {
					ep.setOpStatus(false);
				}
			}

			val = pollValue(IFX_NAME_OID + "." + idx, target);
			if (val != null) {
				ep.setName(val);
			}

			val = pollValue(IFX_SPEED_OID + "." + idx, target);
			if (val != null) {
				Long lval = Long.parseLong(val) * 1000000;
				ep.setSpeed(lval);
			}

			val = pollValue(IFX_ALIAS_OID + "." + idx, target);
			if (val != null) {
				ep.setAlias(val);
			}

			list.add(ep);
		}
		SnmpEndPoint nodeEp = createNodeEndPoint(deviceIp, node.getName(), network, subNetwork);
		list.add(nodeEp);
		return list;
	}

	private String pollValue(String oid, Target target) {
		try {
			PDU reqPdu = new PDU();
			reqPdu.add(new VariableBinding(new OID(oid)));
			reqPdu.setType(PDU.GET);
			int index = (globalCounter.incrementAndGet()) % snmpPool.size();
			ResponseEvent re = this.snmpPool.get(index).send(reqPdu, target);
			PDU response = re.getResponse();
			if (response == null) {
				log.error(tracePrefix + "Response Timed out, will wait for retry ...");
				return null;
			}
			log.debug(tracePrefix + "Request: " + re.getRequest());
			log.debug(tracePrefix + response);
			Vector<VariableBinding> vbs = (Vector<VariableBinding>) response.getVariableBindings();
			if (vbs == null || vbs.isEmpty()) {
				return null;
			}
			VariableBinding vb = vbs.get(0);
			if (vb != null && vb.getVariable() != null) {
				return vb.getVariable().toString();
			} else {
				return null;
			}
		} catch (Exception ex) {
			log.error(tracePrefix + "Unknown error polling the OID: " + oid, ex);
		}
		return null;
	}
	
	public void setSNMPV3Config(AuthenticationProtocol authnProtocol, PrivateProtocol privtProtocol, String userName, String authPaswd, String privPswrd){
		OctetString securityName = new OctetString(userName);
		
		OID authProtocol = new OID(authnProtocol.toString());
		OID privProtocol = new OID(privtProtocol.toString());
		OctetString authPassphrase = new OctetString(authPaswd);
	    OctetString privPassphrase = new OctetString(privPswrd);
	    
	    for(Snmp snmp : this.snmpPool){
	    	snmp.getUSM().addUser(securityName, new UsmUser(securityName, authProtocol, authPassphrase, privProtocol, privPassphrase));
	  	    SecurityModels.getInstance().addSecurityModel(new TSM(new OctetString(MPv3.createLocalEngineID()), false));
	    }
	}
	
	public AbstractTarget createSnmpV1Target(String ip, SnmpAuthProfile auth) throws UnknownHostException {
		InetAddress inet = InetAddress.getByName(ip);
		CommunityTarget target = new CommunityTarget();
		target.setAddress(new UdpAddress(inet, auth.getPort()));
		target.setCommunity(new OctetString(auth.getCommunity()));
		target.setVersion(auth.getSnmpVersion().getVersionId());
		target.setRetries(auth.getRetries());
		target.setTimeout(auth.getTimeout());
		return target;
	}

	public AbstractTarget createAppropriateTarget(NetworkElement ne) throws UnknownHostException{
		AbstractTarget target = null;
		if(ne.getProtocol().equals(NetworkProtocol.SNMPv1)){
			target = createSnmpV1Target(ne);
		}else if(ne.getProtocol().equals(NetworkProtocol.SNMPv2c)){
			target = createSnmpV2Target(ne);
		}else if(ne.getProtocol().equals(NetworkProtocol.SNMPv3)){
			target = createSnmpV3Target(ne);
		}
		return target;
	}
	
	public AbstractTarget createSnmpV1Target(NetworkElement networkEle) throws UnknownHostException {
		InetAddress inet = InetAddress.getByName(networkEle.getIp());
		CommunityTarget target = new CommunityTarget();
		target.setAddress(new UdpAddress(inet, networkEle.getPort()));
		target.setCommunity(new OctetString(networkEle.getCommunityString()));
		target.setVersion(SnmpConstants.version1);
		target.setRetries(networkEle.getSnmpRetries());
		target.setTimeout(networkEle.getSnmpTimeout());
		return target;
	}

	public AbstractTarget createSnmpV2Target(String ip, SnmpAuthProfile auth) throws UnknownHostException {
		InetAddress inet = InetAddress.getByName(ip);
		CommunityTarget target = new CommunityTarget();
		target.setAddress(new UdpAddress(inet, auth.getPort()));
		target.setCommunity(new OctetString(auth.getCommunity()));
		target.setVersion(auth.getSnmpVersion().getVersionId());
		target.setRetries(auth.getRetries());
		target.setTimeout(auth.getTimeout());
		return target;
	}

	public AbstractTarget createSnmpV2Target(NetworkElement networkEle) throws UnknownHostException {
		InetAddress inet = InetAddress.getByName(networkEle.getIp());
		CommunityTarget target = new CommunityTarget();
		target.setAddress(new UdpAddress(inet, networkEle.getPort()));
		target.setCommunity(new OctetString(networkEle.getCommunityString()));
		target.setVersion(SnmpConstants.version2c);
		target.setRetries(networkEle.getSnmpRetries());
		target.setTimeout(networkEle.getSnmpTimeout());
		return target;
	}

	public AbstractTarget createSnmpV3Target(String ip, SnmpAuthProfile auth) throws UnknownHostException {
		setSNMPV3Config(auth.getAuthProtocol(), auth.getPrivProtocol(), auth.getUserName(),
				auth.getAuthPassword(), auth.getPrivPassword());
		InetAddress inet = InetAddress.getByName(ip);
		UserTarget target = new UserTarget();
		int securityLevel = 0;
		if (auth.getAuthProtocol() == null && auth.getPrivProtocol() == null) {
			securityLevel = SecurityLevel.NOAUTH_NOPRIV;
		} else if (auth.getAuthProtocol() != null && auth.getPrivProtocol() == null) {
			securityLevel = SecurityLevel.AUTH_NOPRIV;
		} else if (auth.getAuthProtocol() != null && auth.getPrivProtocol() != null) {
			securityLevel = SecurityLevel.AUTH_PRIV;
		}
		target.setSecurityLevel(securityLevel);
		target.setSecurityName(new OctetString(auth.getUserName()));
		target.setAddress(new UdpAddress(inet, auth.getPort()));
		target.setVersion(SnmpConstants.version3);
		target.setRetries(auth.getRetries());
		target.setTimeout(auth.getTimeout());
		target.setAuthoritativeEngineID(auth.getContextName().getBytes());
		return target;
	}

	public AbstractTarget createSnmpV3Target(NetworkElement networkEle) throws UnknownHostException {
		setSNMPV3Config(networkEle.getAuthProtocol(), networkEle.getPrivProtocol(), networkEle.getUserName(),
				networkEle.getAuthPassword(), networkEle.getPrivPassword());
		InetAddress inet = InetAddress.getByName(networkEle.getIp());
		UserTarget target = new UserTarget();
		int securityLevel = 0;
		if (networkEle.getAuthProtocol() == null && networkEle.getPrivProtocol() == null) {
			securityLevel = SecurityLevel.NOAUTH_NOPRIV;
		} else if (networkEle.getAuthProtocol() != null && networkEle.getPrivProtocol() == null) {
			securityLevel = SecurityLevel.AUTH_NOPRIV;
		} else if (networkEle.getAuthProtocol() != null && networkEle.getPrivProtocol() != null) {
			securityLevel = SecurityLevel.AUTH_PRIV;
		}
		target.setSecurityLevel(securityLevel);
		target.setSecurityName(new OctetString(networkEle.getUserName()));
		target.setAddress(new UdpAddress(inet, networkEle.getPort()));
		target.setVersion(SnmpConstants.version3);
		target.setRetries(networkEle.getSnmpRetries());
		target.setTimeout(networkEle.getSnmpTimeout());
		target.setAuthoritativeEngineID(networkEle.getAuthorativeEngineId().getBytes());
		return target;
	}

	// public static void main(String[] args) {
	// PropertiesManager.init();
	// SnmpPoller.init();
	// String addressStr = "tcp:127.0.0.1/9999";
	// String proto = SnmpPoller.getInstance().getListenProtocol(addressStr);
	// String address = SnmpPoller.getInstance().getListenAddress(addressStr);
	// int port = SnmpPoller.getInstance().getListenPort(addressStr);
	// System.out.println(SnmpPoller.getInstance().getAddressString(proto,
	// address, port));
	// }
}
