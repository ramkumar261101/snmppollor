package ai.netoai.collector.startup;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ai.netoai.collector.Constants;
import ai.netoai.collector.cache.CacheManager;
import ai.netoai.collector.cache.NodeCacheManager;
import ai.netoai.collector.comms.IncomingTopicListener;
import ai.netoai.collector.comms.SnmpTrapsListener;
import ai.netoai.collector.convert.ConvertManager;
import ai.netoai.collector.deviceprofile.ConfigManager;
import ai.netoai.collector.settings.KafkaTopicSettings;
import ai.netoai.collector.settings.SettingsManager;
import ai.netoai.collector.snmp.SnmpManager;
import ai.netoai.collector.snmp.discovery.NodeAdapter;
import ai.netoai.collector.snmp.discovery.NodeManager;
import ai.netoai.collector.snmp.zk.LeaderElectionAware;
import ai.netoai.collector.snmp.zk.LeaderElectionSupport;
import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.apache.commons.daemon.support.DaemonLoader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.log4j.PropertyConfigurator;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.log.Log4jLogFactory;
import org.snmp4j.log.LogFactory;

public class CollectorMain implements Daemon {
	private static final Logger logger = LoggerFactory.getLogger(CollectorMain.class);
    private static final String tracePrefix = "[" + CollectorMain.class.getSimpleName() + "]: ";

    static {
        LogFactory.setLogFactory(new Log4jLogFactory());
        String log4jPropsPath = System.getProperty("collector.log4j");
        System.out.println("Log4jpath from system variable: " + log4jPropsPath);
        if (log4jPropsPath == null || log4jPropsPath.isEmpty()) {
            System.err.println("Log4j Props not found in the system properties: " + System.getProperties().toString());
            log4jPropsPath = "./conf/log4j.properties";
        }
        boolean log4jExists = Files.exists(Paths.get(log4jPropsPath));
        System.out.println("Log4j exists: " + log4jExists);
        System.out.println("Initializing Logger module from configuration: " + log4jPropsPath);
        PropertyConfigurator.configureAndWatch(log4jPropsPath, 30000);
    }

    public static void main(String[] args) {
        CollectorMain instance = new CollectorMain();
        DaemonLoader.Context ctx = new DaemonLoader.Context();
        ctx.setArguments(new String[0]);
        try {
            instance.init(ctx);
        } catch (Exception e) {
            logger.error("Failed", e);
        }
    }

    @Override
    public void destroy() {
        logger.info(tracePrefix + "SNMP Adapter Exiting ...");
    }

    private String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception ex) {
            logger.error(tracePrefix + "Failed retreiving the hostname, using localhost", ex);
            return "localhost";
        }
    }

    public void createTopic(String broker, String topic, int partitions, int replFactor, int minIsr) {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, broker);
        AdminClient client = KafkaAdminClient.create(properties);
        ListTopicsResult listTopicsResult = client.listTopics();
        try {
            Set<String> topics = listTopicsResult.names().get();
            if ( topics.contains(topic) ) {
                logger.info(tracePrefix + "Topic [" + topic + "] already exists ...");
                return;
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error(tracePrefix + "Failed getting the Kafka topics...", e);
        }
        CreateTopicsResult result = client.createTopics(
                Stream.of(topic).map(
                        name -> new NewTopic(name, partitions, (short) replFactor)
                ).collect(Collectors.toList())
        );
        try {
            Void aVoid = result.all().get();
            logger.info(tracePrefix + "Topic created: " + topic);
        } catch (InterruptedException | ExecutionException e) {
            logger.error(tracePrefix + "Failed getting the result for the create topic: " + topic, e);
        }
    }

    public void createTopics() {
        KafkaTopicSettings kts = new KafkaTopicSettings();
        String broker = SettingsManager.getInstance().getSetting(Constants.KAFKA_BROKER_PROP).toString();
        logger.info(tracePrefix + "Creating the topics, broker: " + broker);
        createTopic(broker, KafkaTopicSettings.COLLECTOR_INCOMING_TOPIC,
                kts.getCollectorIcTopicPart(), kts.getCollectorIcTopicRepl(), kts.getCollectorIcTopicMinIsr());

        createTopic(broker, KafkaTopicSettings.COLLECTOR_OUTGOING_TOPIC,
                kts.getCollectorOgTopicPart(), kts.getCollectorOgTopicRepl(), kts.getCollectorOgTopicMinIsr());

        createTopic(broker, KafkaTopicSettings.INVENTORY_TOPIC,
                kts.getInvTopicPart(), kts.getInvTopicRepl(), kts.getInvTopicMinIsr());

        createTopic(broker, KafkaTopicSettings.PERF_TOPIC,
                kts.getPerfTopicPart(), kts.getPerfTopicRepl(), kts.getPerfTopicMinIsr());

        createTopic(broker, KafkaTopicSettings.FAULT_TOPIC,
                kts.getFaultTopicPart(), kts.getFaultTopicRepl(), kts.getFaultTopicMinIsr());

        createTopic(broker, KafkaTopicSettings.INVENTORY_TOPIC_UPDATE,
                kts.getFaultTopicPart(), kts.getFaultTopicRepl(), kts.getFaultTopicMinIsr());
        createTopic(broker, KafkaTopicSettings.FAULT_TOPIC_UPDATE,
                kts.getFaultTopicPart(), kts.getFaultTopicRepl(), kts.getFaultTopicMinIsr());
    }

    public void ensureTopic() {
        createTopics();
    }

    @Override
    public void init(DaemonContext arg0) throws DaemonInitException, Exception {
        logger.info(tracePrefix + "Initializing SNMP Adapter with arguments: " + Arrays.toString(arg0.getArguments()));

        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                Set<NodeAdapter> adapters = NodeManager.getInstance().getNetworkElementAdapters();
                if (adapters != null && !adapters.isEmpty()) {
                    for (NodeAdapter adapter : adapters) {
                        logger.info("Setting to Unknown State For Networkelement " + adapter.getNetworkelement().getName());
                        adapter.setCurrentState(adapter.getUnknownStateObj());
                    }
                    exec.shutdownNow();
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
        // Initialize SettingsManager
        SettingsManager.getInstance();
//        GroupsLinkService.getInstance().start();
        CacheManager.init();
        TopicProducer.init();
        ConvertManager.init();
        SnmpManager.init();
        // Ensure the topic is present in Kafka
        ensureTopic();

        // Starting the Adapter
        CacheManager.getInstance().start();
        ConfigManager.getInstance().start();
        TopicProducer.getInstance().start();
        ConvertManager.getInstance().start();
        // Start the trap listener
        NodeManager.getInstance().start();
        try {
            NodeManager.getInstance().updateNetworkElements();
        } catch (Exception ex) {
            logger.error(tracePrefix + "Failed querying the network elements from GraphDB", ex);
        }
        NodeCacheManager.getInstance().start();
        SnmpManager.getInstance().start();
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            IncomingTopicListener.getInstance().start();
        });
    }
    


	@Override
    public void start() throws Exception {
        logger.info(tracePrefix + "Started the SNMP Adapter");
    }

    @Override
    public void stop() throws Exception {
        logger.info(tracePrefix + "Stopping SNMP Adapter");
        SnmpManager.getInstance().stop();
        CacheManager.getInstance().stop();
        TopicProducer.getInstance().stop();
        logger.info(tracePrefix + "Stopped SNMP Adapter");
    }

}
