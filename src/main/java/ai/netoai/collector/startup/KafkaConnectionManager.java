package ai.netoai.collector.startup;

import ai.netoai.collector.Constants;
import ai.netoai.collector.settings.SettingsManager;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class KafkaConnectionManager {
	private static final Logger log = LoggerFactory
			.getLogger(KafkaConnectionManager.class);
	private static final String tracePrefix = "["
			+ KafkaConnectionManager.class.getSimpleName() + "]: ";

	private ScheduledExecutorService executor = Executors
			.newSingleThreadScheduledExecutor();
	static ConcurrentMap<Integer, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();
	private String topicName;

	/**
	 * This method monitors Kafka connection every 'kafkaMonitorInterval'
	 * seconds. This property is set in collector.properties.
         * 
         * @param topicName name of the topic to monitor
	 */

	public KafkaConnectionManager(String topicName) {
		this.topicName = topicName;
	}


	public void monitorKafkaConnection() {
		// Once the connection is up, intimate the TopicProducer
		// by setting kafkaAlive to true.
                int kafkaMonitorTime = Integer.parseInt(
                        SettingsManager.getInstance().getSettings().get(Constants.KAFKA_MONITOR_TIME).toString());
		Runnable thread = new Runnable() {
			@Override
			public void run() {
				try {
					TopicProducer.getInstance().sendHeartbeat(topicName, "", "heartbeat");
					log.info("Monitored kafka and it is successfully running");
					TopicProducer.getInstance().setKafkaAlive(topicName, true);
				} catch (Exception e) {
					log.error("Monitored kafka and it is  not running", e);
					TopicProducer.getInstance().setKafkaAlive(topicName, false);
				}

				log.info("Is Kafka Alive "
						+ TopicProducer.getInstance().isTopicAlive(topicName));

			}
		};

		ScheduledFuture<?> future=executor.scheduleAtFixedRate(thread, 0,
				kafkaMonitorTime, TimeUnit.SECONDS);
		futures.put(0, future);
	}

}
