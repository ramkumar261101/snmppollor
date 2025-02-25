package ai.netoai.collector.utils;

import ai.netoai.collector.Constants;
import ai.netoai.collector.settings.KafkaTopicSettings;
import ai.netoai.collector.settings.SettingsManager;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;


public class TopicJsonSender {

	private static final Logger log = LoggerFactory.getLogger(TopicJsonSender.class);
	private static final String tracePrefix = "[" + TopicJsonSender.class.getSimpleName() + "]: ";

	private static TopicJsonSender instance;
	private Producer<String, String> producer;
	private static Map<String, Object> settings = null;
	private String broker;
        private List<TopicSendListener> sendListeners;
        
        public interface TopicSendListener {
            public void sent(String topicName, String bean);
        }
        
        public void addTopicSendListener(TopicSendListener listener) {
            this.sendListeners.add(listener);
        }

	private TopicJsonSender() {
		sendListeners = new ArrayList();
		settings = SettingsManager.getInstance().getSettings();
		init();
	}

	private void init() {
		broker = settings.get(Constants.KAFKA_BROKER_PROP).toString();
		log.info(tracePrefix + "Connecting to Kafka brokers: " + broker);
		Properties props = new Properties();
		props.put("bootstrap.servers", broker);
		props.put("acks", "all");
		props.put("retries", 1);
		props.put("batch.size", 16384);
		props.put("linger.ms", 1);
		props.put("buffer.memory", 33554432);

		this.producer = new KafkaProducer<>(props, new StringSerializer(), new StringSerializer());
	}

	public synchronized static TopicJsonSender getInstance() {
		if (instance == null) {
			instance = new TopicJsonSender();
		}
		return instance;
	}

	public void send(String topicName, String bean) {
		try {
			RecordMetadata metadata = this.producer.send(new ProducerRecord<String, String>(topicName, "", bean)).get();
			if ( log.isDebugEnabled() ) {
				log.debug(tracePrefix + "Message sent to: " + metadata.topic());
			}
			for (TopicSendListener listener : sendListeners) {
				listener.sent(topicName, bean);
			}
		} catch (Exception e) {
			log.error("Failed", e);
		}
	}

}
