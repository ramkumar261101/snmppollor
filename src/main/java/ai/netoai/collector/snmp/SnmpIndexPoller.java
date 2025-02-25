package ai.netoai.collector.snmp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ai.netoai.collector.cache.CacheManager;
import ai.netoai.collector.model.ISNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.PDU;
import org.snmp4j.Target;

public class SnmpIndexPoller implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(SnmpIndexPoller.class);
	private static final String tracePrefix = "[" + SnmpIndexPoller.class.getSimpleName() + "]: ";
	private static final long INDEX_POLL_STAT_DELAY = 30000;
	private SnmpManager snmpManager;
	private CacheManager cacheManager;
	private ExecutorService indexPollExecutor;
	private static Map<String, Object> settings;
	private AtomicInteger totalDevicesPolled;

	public SnmpIndexPoller() {

	}
	
	private long calculateOffset() {
		return INDEX_POLL_STAT_DELAY - (System.currentTimeMillis() % INDEX_POLL_STAT_DELAY);
	}

	@Override
	public void run() {
		try {
			log.info(tracePrefix + "Index polling started ...");
			List<String> deviceIps = this.cacheManager.getAllDeviceIps();  
			if ( deviceIps.isEmpty() ) {
				log.info(tracePrefix + "No devices found to poll ...");
				return;
			}
			if ( log.isTraceEnabled() )
				log.trace(tracePrefix + "Polling devices: " + deviceIps);
			
			log.info(tracePrefix + "Number of devices already polled: " + cacheManager.getPolledDeviceSize());
			for( String deviceIp : deviceIps ) {
				if ( cacheManager.isDevicePolled(deviceIp) || "Name".equalsIgnoreCase(deviceIp) ) {
					log.debug(tracePrefix + "Device: " + deviceIp + " Already polled ... or invalid");
					continue;
				}
				// If the device is not polled, then send snmp requests.
				log.info(tracePrefix + "Polling device: " + deviceIp);
				this.indexPollExecutor.submit(new Runnable() {

					@Override
					public void run() {

					}
					
					private void pollDeviceDetails(ISNode isn) {

					}
					
					private void pollIndices(ISNode isn) {
						try {
							Target target = SnmpManager.getInstance().createTarget(deviceIp, isn.getProperties().get("port"), isn.getProperties().get("community"));
							PDU reqPdu = new PDU();
							reqPdu.setType(PDU.GETBULK);
							reqPdu.setNonRepeaters(0);
							List<String> idxs = SnmpPoller.getInstance().getInterfaceIndices(reqPdu, target);
							if ( log.isTraceEnabled() ) {
								log.trace(tracePrefix + "List of indices: " + idxs);
							}
							if ( idxs != null && !idxs.isEmpty() ) {
								cacheManager.storeDevicePollInfo(deviceIp, idxs);
								totalDevicesPolled.incrementAndGet();
//								if ( settings.isPollInventory() ) {
//									List<SnmpEndPoint> endPoints = SnmpPoller.getInstance().pollEndPoints(target, deviceIp, idxs);
//									if ( log.isTraceEnabled() ) {
//										log.trace(tracePrefix + "END POINTS POLLED: " + endPoints);
//									}
//									if ( log.isTraceEnabled() ) {
//		    							StringBuilder sb = new StringBuilder();
//		    							sb.append('\n');
//		    							for(SnmpEndPoint ep : endPoints) {
//		    								sb.append("====================================").append('\n');
//		    								sb.append("Id=").append(ep.getSourceId()).append('\n');
//		    								sb.append("Index=").append(ep.getSourceIndex()).append('\n');
//		    								sb.append("Mac=").append(ep.getMac()).append('\n');
//		    								sb.append("Name=").append(ep.getName()).append('\n');
//		    								sb.append("Node=").append(ep.getNode()).append('\n');
//		    								sb.append("NodeName=").append(ep.getNodeName()).append('\n');
//		    								sb.append("Network=").append(ep.getNetwork()).append('\n');
//		    								sb.append("SubNetwork=").append(ep.getSubNetwork()).append('\n');
//		    								sb.append("Type=").append(ep.getSourceType()).append('\n');
//		    								sb.append("Admin=").append(ep.getAdminStatus()).append('\n');
//		    								sb.append("Oper=").append(ep.getOpStatus()).append('\n');
//		    								sb.append("Speed=").append(ep.getSpeed()).append('\n');
//		    								sb.append("====================================").append('\n');
//		    							}
//		    							log.trace(tracePrefix + sb.toString());
//									}
//									String epStr = JsonGenerator.getJSONString(endPoints);
//									KafkaUtils.postToKafkaTopic(
//											settings.getKafkaBroker(), settings.getInvTopic(), deviceIp, epStr);
//								}
//							} else {
								if ( cacheManager.isDevicePolled(deviceIp) ) {
									cacheManager.invalidatePolledDeviceCache(deviceIp);
								}
							}
						} catch (Throwable t) {
							log.error(tracePrefix + "Failed to poll the indices for device: " + isn.getId(), t);
						}
					}
					
				});
			}
			
		} catch (Throwable t) {
			// Catching throwable to not stop the index polling loop.
			log.error(tracePrefix + "Failed polling index", t);
		}
	}
	
}
