package ai.netoai.collector.startup;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import ai.netoai.collector.Constants;
import ai.netoai.collector.settings.KafkaTopicSettings;
import ai.netoai.collector.settings.SettingsManager;
import ai.netoai.collector.snmp.SnmpManager;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TopicProducer {

	private static final Logger log = LoggerFactory.getLogger(TopicProducer.class);
	private static final String tracePrefix = "[" + TopicProducer.class.getSimpleName() + "]: ";
	
	private static final Logger trapsLog=LoggerFactory.getLogger("traps");
	private static final Logger perfLog=LoggerFactory.getLogger("perf");
	private static final Logger invLog=LoggerFactory.getLogger("inv");
	
	private static final int _BATCH_SIZE = 100;
	
	private static TopicProducer instance;
	private static int eventsPartitionCounter = 0;
	private static int perfPartitionCounter = 0;
	private static int invPartitionCounter = 0;
	
	private Producer<String, String> producer;
	/**
	 * @return the producer
	 */
	public Producer<String, String> getProducer() {
		return producer;
	}

	/**
	 * @param producer the producer to set
	 */
	public void setProducer(Producer<String, String> producer) {
		this.producer = producer;
	}

	private boolean alive = true;
	private static Map<String, Object> settings = null;
	private static KafkaTopicSettings kts = null;
	private String broker;
	private Map<String, Boolean> topicStatus = new ConcurrentHashMap<>();
	private Map<String, Boolean> topicUnderMonitoring = new ConcurrentHashMap<>();
	
	public boolean isTopicAlive(String topic) {
		boolean alive = topicStatus.containsKey(topic) && topicStatus.get(topic) == true;
		return alive;
	}
	
	public boolean isInvTopicAlive() {
		boolean alive = topicStatus
				.containsKey(KafkaTopicSettings.INVENTORY_TOPIC)
				&& topicStatus.get(KafkaTopicSettings.INVENTORY_TOPIC) == true;
		return alive;
	}
	
	public boolean isPerfTopicAlive() {
		boolean alive = topicStatus
				.containsKey(KafkaTopicSettings.PERF_TOPIC)
				&& topicStatus.get(KafkaTopicSettings.PERF_TOPIC) == true;
		return alive;
	}
	
	public boolean isFaultTopicAlive() {
		boolean alive = topicStatus
				.containsKey(KafkaTopicSettings.FAULT_TOPIC)
				&& topicStatus.get(KafkaTopicSettings.FAULT_TOPIC) == true;
		return alive;
	}

	public void setKafkaAlive(String topicName, boolean kafkaAlive) {
		if ( !topicStatus.containsKey(topicName) ) {
			log.error(tracePrefix + "Topic " + topicName + " is not monitored yet ...");
			return;
		}
		topicStatus.put(topicName, kafkaAlive);
		   
		//When Kafka is Alive True Send Log Files to kafka
		if (kafkaAlive) {
			topicUnderMonitoring.put(topicName, false);
			List<String> batch = TrapsForwardManager.readStoredMessages(topicName);
			if (batch.size() > 0) {
				log.info(tracePrefix + "Waiting 60 seconds before sending stored traps...");
                try {
                    Thread.sleep(10*1000l);
                } catch (Exception ex) {
                    log.error(tracePrefix + "Interrupted while waiting to send stored traps", ex);
                }
				long startTime = System.currentTimeMillis();
				try {
					for(String msg : batch) {
						String[] parts = msg.split("__");
						forward(topicName, getPartition(topicName), parts[0], parts[1], getLogger(topicName));
					}
				} catch (Exception e) {
					log.error(tracePrefix + e);
				}
				long endTime = System.currentTimeMillis();
				log.info(tracePrefix + " Time taken to send " + batch.size() + " traps in batch mode: " + (System.currentTimeMillis() - startTime) + " ms");
			}
			// Cancelling Monitoring Kafka Thread
			KafkaConnectionManager.futures.get(0).cancel(true);
		}
	}

	/**
	 * @param topicName
	 * @return
	 */
	private Logger getLogger(String topicName) {
		switch (topicName) {
			case KafkaTopicSettings.INVENTORY_TOPIC:
				return invLog;
			case KafkaTopicSettings.PERF_TOPIC:
				return perfLog;
			case KafkaTopicSettings.FAULT_TOPIC:
				return trapsLog;
			default:
				return null;
		}
	}

	/**
	 * @param topicName
	 * @return
	 */
	private int getPartition(String topicName) {
		switch (topicName) {
			case KafkaTopicSettings.INVENTORY_TOPIC:
				return invPartition();
			case KafkaTopicSettings.PERF_TOPIC:
				return perfPartition();
			case KafkaTopicSettings.FAULT_TOPIC:
				return eventPartition();
			default:
				return 0;
		}
	}

	private TopicProducer() {
		settings = SettingsManager.getInstance().getSettings();
		kts = new KafkaTopicSettings();
	}
	
	public static void init() {
		if ( instance != null ) {
			throw new IllegalStateException("TopicProducer is already initialized");
		}
		instance = new TopicProducer();
	}
	
	public static TopicProducer getInstance() {
		if ( instance == null ) {
			throw new IllegalStateException("TopicProducer not initialized");
		}
		return instance;
	}
	
	public void start() {
		broker = settings.get(Constants.KAFKA_BROKER_PROP).toString();
		log.info(tracePrefix + "Connecting to Kafka brokers: " + broker);
		Properties props = new Properties();
		props.put("bootstrap.servers", broker);
		props.put("acks", "all");
		props.put("retries", 1);
		props.put("batch.size", 16384);
		props.put("linger.ms", 1);
		props.put("buffer.memory", 33554432);
		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		
		this.producer = new KafkaProducer<>(props);
		log.info(tracePrefix + "Started ...");
	}
	
	public void stop() {
		if ( this.producer != null ) {
			this.producer.close();
		}
	}
	
	private static int eventPartition() {
		int partId = eventsPartitionCounter % kts.getFaultTopicPart();
		eventsPartitionCounter++;
		if ( eventsPartitionCounter == _BATCH_SIZE ) {
			eventsPartitionCounter = 0;
		}
		return partId;
	}
	
	private static int perfPartition() {
		int partId = perfPartitionCounter % kts.getPerfTopicPart();
		perfPartitionCounter++;
		if ( perfPartitionCounter == _BATCH_SIZE ) {
			perfPartitionCounter = 0;
		}
		return partId;
	}
	
	private static int invPartition() {
		int partId = invPartitionCounter % kts.getInvTopicPart();
		invPartitionCounter++;
		if ( invPartitionCounter == _BATCH_SIZE ) {
			invPartitionCounter = 0;
		}
		return partId;
	}
	
	public void forward(String topicName, int partition, String key, String message, Logger logger) {
		
		ProducerRecord<String, String> rec = new ProducerRecord<String, String>(
				topicName, partition,
				key, message);
		if ( kts.getFaultAsyncSendEnabled() != null && kts.getFaultAsyncSendEnabled() ) {
			// Send asynchronously
			this.producer.send(rec, new Callback() {
				
				@Override
				public void onCompletion(RecordMetadata metadata, Exception ex) {
					if ( metadata == null ) {
						handleError(ex, topicName, key + "__" + message, logger);
					}
				}
			});
		} else {
			// Send synchronously
    		try {
    			this.producer.send(rec).get();
    		} catch (Exception e) {
    			handleError(e, topicName, key + "__" + message, logger);
    
    		}
		}
	}

	
	public void forwardEvent(String key, String msg) {
		forward(KafkaTopicSettings.FAULT_TOPIC, eventPartition(), key, msg, trapsLog);
	}


	/**
	 * @param e
	 */
	private void handleError(Exception e, String topicName, String message, Logger logger) {
		topicStatus.put(topicName, false);
		// Store Traps into Log File
		log.error(tracePrefix + "Failed sending messages to: "
				+ broker, e);
		logger.info("" + message);
		boolean underMonitoring = topicUnderMonitoring.containsKey(topicName) && topicUnderMonitoring.get(topicName) == true;
		if ( !underMonitoring ) {
			KafkaConnectionManager kafkaConnection = new KafkaConnectionManager(topicName);
			kafkaConnection.monitorKafkaConnection();
			topicUnderMonitoring.put(topicName, true);
		}
	}
	
	public void sendHeartBeatInv(String key, String message) throws Exception {
		sendHeartbeat(KafkaTopicSettings.INVENTORY_TOPIC, key, message);
	}
	
	public void sendHeartBeatPerf(String key, String message) throws Exception {
		sendHeartbeat(KafkaTopicSettings.PERF_TOPIC, key, message);
	}
	
	public void sendHeartBeatFault(String key, String message) throws Exception {
		sendHeartbeat(KafkaTopicSettings.FAULT_TOPIC, key, message);
	}
	
	public void sendHeartbeat(String topicName, String key, String message) throws Exception {
		try {
			ProducerRecord<String, String> rec = new ProducerRecord<String, String>(topicName, key, message);
			this.producer.send(rec).get();
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

}


