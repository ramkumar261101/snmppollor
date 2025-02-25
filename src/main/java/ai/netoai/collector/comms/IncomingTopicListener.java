package ai.netoai.collector.comms;

import ai.netoai.collector.Constants;
import ai.netoai.collector.model.*;
import ai.netoai.collector.settings.KafkaTopicSettings;
import ai.netoai.collector.settings.SettingsManager;
import ai.netoai.collector.snmp.discovery.DiscoveryManager;
import ai.netoai.collector.snmp.discovery.NodeManager;
import ai.netoai.collector.cache.NodeCacheManager;
import ai.netoai.collector.startup.CollectorMain;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;

public class IncomingTopicListener {

    private static final Logger log = LoggerFactory
            .getLogger(IncomingTopicListener.class);
    private static final String tracePrefix = "["
            + IncomingTopicListener.class.getSimpleName() + "]: ";

    private static IncomingTopicListener instance;
    private SettingsManager sm;
    private KafkaTopicSettings kts;
    private Map<String, Object> collectorSettings;
    private KafkaConsumer<String, String> consumer;
    private boolean alive;
    private LinkedBlockingQueue<ConfigMessage> msgsQueue = new LinkedBlockingQueue<>();
    private ExecutorService executor;

    private IncomingTopicListener() {
        sm = SettingsManager.getInstance();
        kts = new KafkaTopicSettings();
        collectorSettings = sm.getSettings();
        this.executor = Executors.newFixedThreadPool(kts.getCollectorIcMsgProcThreads());
    }

    public synchronized static IncomingTopicListener getInstance() {
        if (instance == null) {
            instance = new IncomingTopicListener();
        }

        return instance;
    }

    public void stop() {
        this.alive = false;
        this.consumer.close();
    }

    public void start() {
        if (alive) {
            log.warn(tracePrefix + "Already started ...");
            return;
        }
        alive = true;
        log.info(tracePrefix + "Starting to listen on the incoming topic: "
                + KafkaTopicSettings.COLLECTOR_INCOMING_TOPIC);

        Thread processingThread = new Thread(new Runnable() {

            @Override
            public void run() {

                while (alive) {
                    try {
                        ConfigMessage message = msgsQueue.take();
                        executor.execute(new MessageWorker(message));
                    } catch (InterruptedException e) {
                        log.error(tracePrefix + "Failed queuing the message", e);
                    }
                }

            }

        }, "Collector Ic Message Processor");
        processingThread.setDaemon(true);
        processingThread.start();

        Properties props = new Properties();
        props.put("bootstrap.servers", collectorSettings.get("kafkaBrokers"));
        props.put("group.id", collectorSettings.get("adapterGroupName"));
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");
        consumer = new KafkaConsumer<>(props, new StringDeserializer(), new StringDeserializer());

        consumer.subscribe(Arrays.asList(KafkaTopicSettings.COLLECTOR_INCOMING_TOPIC));
        log.info(tracePrefix + "Polling the topic: " + KafkaTopicSettings.COLLECTOR_INCOMING_TOPIC);
        while (alive) {
            ConsumerRecords<String, String> records = consumer.poll(1000);
            try {
                for (ConsumerRecord<String, String> rec : records) {
                    log.info(tracePrefix + "Message received on collector_incoming_topic: \n" + rec.value());
                    ConfigMessage msg = GenericJavaBean.fromJson(rec.value());
                    log.info(tracePrefix + "Received the message: " + msg);
                    boolean success = msgsQueue.offer(msg);
                    if ( success ) {
                        log.warn(tracePrefix + "Failed processing the message: " + msg + ", Queue stats: " + msgsQueue.size());
                    }
                }
            } catch (Throwable t) {
                log.error(tracePrefix + "Failed processing batch of records: " + records.count(), t);
            }
        }
    }

    private static class MessageWorker implements Runnable {

        ConfigMessage message;

        MessageWorker(ConfigMessage msg) {
            this.message = msg;
        }

        @Override
        public void run() {
            log.info(tracePrefix + "Processing the message type: {}",message.getMsgType());
            switch (message.getMsgType()) {
                case START_DISCOVERY: {
                    DiscoveryTask dt = (DiscoveryTask) message.getPayload().get(0);
                    List<String> ipRange = dt.getIpRange();
                    List<SnmpAuthProfile> authProfiles = dt.getAuthProfiles();
                    log.info(tracePrefix + "Discovery task name: " + dt.getName() + ", Ip Range: " + ipRange + ", Auth profiles: " + authProfiles);
                    DiscoveryManager.getInstance().startDiscovery(dt);
                }
                break;
                case DELETE_NODE: {
                    Serializable payload = message.getPayload().get(0);
                    log.info(tracePrefix + "DeleteNode: Payload received: " + payload.getClass() + ", Payload: " + payload);
                    String[] ids = null;
                    if (payload instanceof String[]) {
                        log.info(tracePrefix + "Payload is a string array");
                        ids = (String[]) payload;
                        log.info(tracePrefix + "The NE Ids are: " + Arrays.toString(ids));
                    }
                    if ( payload instanceof String) {
                        log.info(tracePrefix + "Payload is a string");
                        ids = new String[1];
                        ids[0] = ((String) payload).toString();
                    }
                    NodeManager.getInstance().removeNodes(ids);
                }
                break;
                case ENDPOINT_MODIFY: {
                    Serializable payload = message.getPayload().get(0);
                    log.info(tracePrefix + "Modify EndPoint: Payload received: " + payload);
                    
                    if ( ArrayList.class.isAssignableFrom(payload.getClass()) ) {
                        ArrayList<EndPoint> endPointList = (ArrayList<EndPoint>) payload;
                        NodeCacheManager.getInstance().invalidate(endPointList);
                    }
                }
                break;
            }
        }

    }
}
