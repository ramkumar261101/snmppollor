package ai.netoai.collector.snmp;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import ai.netoai.collector.Constants;
import ai.netoai.collector.cache.CacheManager;
import ai.netoai.collector.convert.ConvertManagerHelper;
import ai.netoai.collector.model.NetworkElement;
import ai.netoai.collector.settings.SettingsManager;
import ai.netoai.collector.settings.SnmpSettings;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.CommunityTarget;
import org.snmp4j.MessageDispatcherImpl;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.TransportMapping;
import org.snmp4j.mp.MPv1;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.AuthMD5;
import org.snmp4j.security.AuthSHA;
import org.snmp4j.security.Priv3DES;
import org.snmp4j.security.PrivDES;
import org.snmp4j.security.PrivacyProtocol;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TcpAddress;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultTcpTransportMapping;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.MultiThreadedMessageDispatcher;
import org.snmp4j.util.ThreadPool;

import ai.netoai.collector.startup.CollectorMain;


public class SnmpManager {
	
	private static final Logger log = LoggerFactory.getLogger(SnmpManager.class);
	private static final String tracePrefix = "[" + SnmpManager.class.getSimpleName() + "]: ";
	
	private static SnmpManager instance;
	private SnmpSettings snmpSettings;
	private Map<String, Object> collectorSettings;
	private TransportMapping trapTransportMapping;
	private MultiThreadedMessageDispatcher dispatcher;
	private ThreadPool threadPool;
	private Address listenAddress;
	private LinkedBlockingQueue<WrappedCREvent> trapQueue = new LinkedBlockingQueue<>();
	private boolean alive;
	private ScheduledExecutorService indexPoller;
	private ScheduledExecutorService epPoller;
	private ScheduledExecutorService devicePoller;
	private ScheduledExecutorService performancePoller;
	private long lastPerformanceRequestTime;
	private Map<String, PerfPollValue> lastPollMap = new ConcurrentHashMap<>();
	private AtomicInteger eventsReceived = new AtomicInteger(0);
	private AtomicInteger eventsProcessed = new AtomicInteger(0);
	private long lastEventProcessed;
	private AtomicInteger metricsReceived = new AtomicInteger(0);
	private AtomicInteger metricsProcessed = new AtomicInteger(0);
	
	private AtomicInteger requestsSent = new AtomicInteger(0);
	private AtomicInteger responsesReceived = new AtomicInteger(0);
	private AtomicInteger responseTimeouts = new AtomicInteger(0);
	private AtomicInteger responseProcessed = new AtomicInteger(0);
	private long maxRespProcTime = -1;
	private long minRespProcTime = Integer.MAX_VALUE;
	private long maxQueuedTime = -1;
	private ReentrantReadWriteLock statsLock = new ReentrantReadWriteLock();
	
	public void incrementReqSent() {
		requestsSent.incrementAndGet();
	}
	
	public void incrementRespRec() {
		responsesReceived.incrementAndGet();
	}
	
	public void incrementRespTimeouts() {
		responseTimeouts.incrementAndGet();
	}
	
	public void incrementRespProcessed() {
		responseProcessed.incrementAndGet();
	}
	
	public void setLastPerformanceRequestTime(long time) {
		this.lastPerformanceRequestTime = time;
	}
	
	public long getLastPerformanceRequestTime() {
		return this.lastPerformanceRequestTime;
	}
	
	public void doneRespProcessing(long procTime, long queueIdleTime) {
		statsLock.writeLock().lock();
		try {
			if ( procTime > maxRespProcTime ) {
				maxRespProcTime = procTime;
			}
			if ( procTime < minRespProcTime ) {
				minRespProcTime = procTime;
			}
			if ( queueIdleTime > maxQueuedTime ) {
				maxQueuedTime = queueIdleTime;
			}
		} finally {
			statsLock.writeLock().unlock();
		}
	}
	
	private SnmpManager() {
		SettingsManager sm = SettingsManager.getInstance();
		snmpSettings = new SnmpSettings();

		this.collectorSettings = sm.getSettings();
            
		indexPoller = Executors.newSingleThreadScheduledExecutor();
		epPoller = Executors.newSingleThreadScheduledExecutor();
		devicePoller = Executors.newSingleThreadScheduledExecutor();
		performancePoller = Executors.newSingleThreadScheduledExecutor();
		SnmpPoller.getInstance().start();
	}
	
	public static void init() {
		if ( instance != null ) {
			throw new IllegalStateException("Manager already initialized");
		}
		instance = new SnmpManager();
	}
	
	public static SnmpManager getInstance() {
		if ( instance == null ) {
			throw new IllegalStateException("Manager not initialized");
		}
		return instance;
	}
	
	private void initSnmp() throws IOException {
		threadPool = ThreadPool.create("SnmpTrapDispatcherThreads", snmpSettings.getTrapReceiverThreads());
	    dispatcher =
	        new MultiThreadedMessageDispatcher(threadPool,
	                                           new MessageDispatcherImpl());
	    
	    USM usm = new USM(SecurityProtocols.getInstance(),
                new OctetString(MPv3.createLocalEngineID()), 0);
	    SecurityModels.getInstance().addSecurityModel(usm);
	    
	    PrivacyProtocol privacyProtocol = SecurityProtocols.getInstance().getPrivacyProtocol(PrivDES.ID);
	    log.info(tracePrefix + "Privacy protocol for DES: " + privacyProtocol);
	    SecurityProtocols.getInstance().addPrivacyProtocol(new PrivDES());
	    SecurityProtocols.getInstance().addPrivacyProtocol(new Priv3DES());
	    log.info(tracePrefix + "Added Privacy protocol for DES");
	    privacyProtocol = SecurityProtocols.getInstance().getPrivacyProtocol(PrivDES.ID);
	    SecurityProtocols.getInstance().addDefaultProtocols();
	    log.info(tracePrefix + "Privacy protocol for DES: " + privacyProtocol);
	}
	
	private OID getAuthProto(String authProto) {
		if ( authProto.equalsIgnoreCase("MD5") ) {
			return AuthMD5.ID; 
		} else if ( authProto.equalsIgnoreCase("SHA") ) {
			return AuthSHA.ID;
		}
		return AuthMD5.ID;
	}
	
	private OID getPrivProto(String privProto) {
		if ( privProto.equalsIgnoreCase("DES") ) {
			return PrivDES.ID;
		} else if ( privProto.equalsIgnoreCase("DES3") ) {
			return Priv3DES.ID;
		}
		return PrivDES.ID;
	}
	
	public void start() {
		try {
			SnmpPoller.getInstance().start();
                        log.info(tracePrefix + "Initializing the SNMP instance");
			initSnmp();
                        log.info(tracePrefix + "Done initializing the SNMP instance");
			alive = true;
                        
			SnmpPoller.getInstance().start();

			SnmpPerformancePoller.getInstance().run();
			
			long offset = calculateOffset(60000);
			Timer rateLogger = new Timer("Event Rate Logger", false);
			rateLogger.schedule(new TimerTask() {
				
				@Override
				public void run() {
					log.info(tracePrefix + "STATS: " + getAlarmRate());
				}
			}, offset, 60000);
			
		} catch ( Exception ioe ) {
			log.error(tracePrefix + "Failed to start the trap listener", ioe );
		}
	}
	
	public void updateEventProcessedCount(int size) {
		this.eventsProcessed.getAndAdd(size);
	}
	
	public void updatePerfProcessedCount(int size) {
		this.metricsProcessed.getAndAdd(size);
	}
	
	private String getAlarmRate() {
		StringBuilder sb = new StringBuilder();
		int received = eventsReceived.getAndSet(0);
		int processed = eventsProcessed.getAndSet(0);
		
		int metricsRec = metricsReceived.getAndSet(0);
		int metricsProc = metricsProcessed.getAndSet(0);
		
		int reqSent = requestsSent.getAndSet(0);
		int respRec = responsesReceived.getAndSet(0);
		int respTimeout = responseTimeouts.getAndSet(0);
		int respProcessed = responseProcessed.getAndSet(0);
		
		sb.append("\n===================================================\n");
		sb.append("Requests sent: ").append(reqSent).append('\n');
		sb.append("Responses received: ").append(respRec).append('\n');
		sb.append("Response Timeouts: ").append(respTimeout).append('\n');
		sb.append("Responses Processed: ").append(respProcessed).append('\n');
		
		statsLock.readLock().lock();
		try {
			sb.append("Max Response Process Time: ").append(maxRespProcTime).append(" ms\n");
			sb.append("Min Response Process Time: ").append(minRespProcTime).append(" ms\n");
			sb.append("Max Queued Time: ").append(maxQueuedTime).append(" ms\n");
		} finally {
			statsLock.readLock().unlock();
		}
		
		sb.append("Events received: ").append(received).append('\n');
		sb.append("Events Processed: ").append(processed).append('\n');
		
		sb.append("Metrics received: ").append(metricsRec).append('\n');
		sb.append("Metrics Processed: ").append(metricsProc).append('\n');
		long currentTime = System.currentTimeMillis();
		if ( lastEventProcessed > 0 ) {
    		long timeDelta = currentTime - lastEventProcessed;
    		double receiveRate = (double)received / (timeDelta/1000);
    		double processRate = (double)processed / (timeDelta/1000);
    		sb.append("Events Process Time: ").append(timeDelta).append(" millis").append('\n');
    		sb.append("Events ReceiveRate: ").append(receiveRate).append(" events/sec").append('\n');
    		sb.append("Events ProcessRate: ").append(processRate).append(" events/sec").append('\n');
		}
		
		sb.append("===================================================\n");
		lastEventProcessed = currentTime;
		
		statsLock.writeLock().lock();
		try {
			maxRespProcTime = -1;
			minRespProcTime = Integer.MAX_VALUE;
			maxQueuedTime = -1;
		} finally {
			statsLock.writeLock().unlock();
		}
		
		return sb.toString();
	}
	
	public void stop() {
		try {
			this.alive = false;
			this.trapQueue.clear();
			if ( this.trapTransportMapping != null ) {
				this.trapTransportMapping.close();
			}
			SnmpPoller.getInstance().stop();
		} catch ( Exception ex ) {
			log.error(tracePrefix + "Failed to stop the SnmpManager", ex);
		}
	}
	
	private class SnmpTrapReceiver implements CommandResponder, Runnable {

		@Override
		public void processPdu(CommandResponderEvent event) {
			try {
				if ( log.isDebugEnabled() ) {
					log.debug(tracePrefix + "TRAP RECEIVED: " + event.getPDU());
				}
				eventsReceived.incrementAndGet();
				event.setProcessed(true);
				// Here we are forwarding the trap further to be processed, before
				// sending them we put a check whether this adapter is a leader or
				// follower, if it is a follower we will not send the trap further.
				WrappedCREvent wEvent = new WrappedCREvent(event, System.currentTimeMillis());
				trapQueue.put(wEvent);
			} catch( Exception ex ) {
				log.error("Failed processing the trap: " + event.getPDU(), ex);
			}
		}

		@Override
		public void run() {
			log.info("Snmp Receiver thread starting ...");
			WrappedCREvent event = null;
			while(alive) {
				try {
					 event = trapQueue.take();
					 if ( alive ) {
						 // Send the trap to converter and then to the Kafka topic.
//						 ConvertManager.getInstance().processTrapEvent(event);
                                                ConvertManagerHelper.getInstance().processTrapEvents(event);
					 }
				} catch ( Exception ex ) {
					log.error("Failed to forward the trap to converter...", ex);
				}
			}
			log.info(tracePrefix + "Snmp trap receiver thread exiting ...");
		}
		
	}
	
	private long calculateOffset(long interval) {
		long current = System.currentTimeMillis();
		long offset = (interval) - (current % interval);
		return offset;
	}
	
	public Target createTarget(String ip, String portStr, String community) {
		int port = snmpSettings.getDefaultAgentPort();
		String protocol = snmpSettings.getDefaultProtocol();
		try {
			port = Integer.parseInt(portStr);
		} catch(NumberFormatException nfe) {
			log.error(tracePrefix + "Failed to parse the port: " + portStr);
		}
		if ( community == null || community.trim().isEmpty() ) {
			log.info(tracePrefix + "Using default community string");
			community = snmpSettings.getDefaultCommunity();
		}
		CommunityTarget target = new CommunityTarget();
		try {
			if ( protocol.equalsIgnoreCase("udp") ) {
				target.setAddress(new UdpAddress(InetAddress.getByName(ip), port));
			} else if ( protocol.equalsIgnoreCase("tcp") ) {
				target.setAddress(new TcpAddress(InetAddress.getByName(ip), port));
			}
			target.setCommunity(new OctetString(community));
			target.setVersion(SnmpConstants.version2c);
			target.setRetries(snmpSettings.getSnmpRetries());
			target.setTimeout(snmpSettings.getSnmpTimeout());
		} catch(Exception ex) {
			log.error(tracePrefix + "Failed constructing target", ex);
		}
		return target;
	}

	public static Target createTarget(NetworkElement ne) {
		Address address = GenericAddress.parse(ne.getIp() + "/" + ne.getPort());
		CommunityTarget target = new CommunityTarget();
		try {
			target.setCommunity(new OctetString(ne.getCommunityString()));
			target.setAddress(new UdpAddress(InetAddress.getByName(ne.getIp()), ne.getPort()));
			target.setVersion(1);
			target.setTimeout(ne.getSnmpTimeout());
			target.setRetries(ne.getSnmpRetries());
		} catch (Exception ex) {
			log.error(tracePrefix + "Failed constructing target", ex);
		}
		return target;
	}
	
	public static void main(String[] args) {
		
		long ct = System.currentTimeMillis();
		long cl = Clock.systemUTC().millis();
		Instant i = Instant.ofEpochMilli(ct);
		LocalDateTime ldt1 = i.atZone(ZoneId.systemDefault()).toLocalDateTime();
		i = Instant.ofEpochMilli(cl);
		LocalDateTime ldt2 = i.atZone(ZoneId.systemDefault()).toLocalDateTime();
		System.out.println(ldt1);
		System.out.println(ldt2);
		System.out.println(ct);
		System.out.println(cl);
		System.exit(1);
		
		SnmpManager.init();
//		SnmpManager.getInstance().start();
		SnmpManager.getInstance().calculateOffset(60*1000);
		log.info("exiting main");
	}
	
	public Double getConvertedMetricValue(String key, double newVal, String convFunction, long timeStamp) {
		try {
			PerfPollValue newPollVal = new PerfPollValue(newVal, timeStamp);
			PerfPollValue lastPollVal = getAndSetPreviousValue(key, newPollVal);
			if ( lastPollVal == null ) {
				if ( log.isTraceEnabled() ) {
					log.trace(tracePrefix + "Key: " + key + " not present in last value map, returning null");
				}
				return null;
			}
			
			Double lastVal = lastPollVal.getValue();
                        if ( convFunction != null ) {
                            convFunction = convFunction.toLowerCase();
                        }
                        log.info(tracePrefix + "Conversion function is: [" + convFunction + "]");
			if ( "per_poll".equals(convFunction)) {
				Double val = newVal - lastVal;
				log.debug(tracePrefix + "New Val: " + newVal + ", Last Val: " + lastVal + ", Returning: " + val + ", Conv func: " + convFunction);
				return val;
			} else if ( "per_poll_per_second".equals(convFunction)) {
				double pollInterval = ( newPollVal.getTimeStamp() - lastPollVal.getTimeStamp() ) / 1000;
				Double val = (newVal - lastVal) / pollInterval;
				log.debug(tracePrefix + "New Val: " + newVal + ", Last Val: " + lastVal + ", Returning: " + val + ", Conv func: " + convFunction + ", Poll interval: " + pollInterval);
				return val;
			} else {
				log.debug(tracePrefix + "Conversion function " + convFunction + " not supported so returning value as it is" );
				return newVal;
			}
		} catch (Throwable t) {
			log.error(tracePrefix + "Error converting value for " + key, t);
		}
		return newVal;
	}
	
	private PerfPollValue getAndSetPreviousValue(String key, PerfPollValue newVal) {
		PerfPollValue lastVal = this.lastPollMap.put(key, newVal);
		return lastVal;
	}

}
