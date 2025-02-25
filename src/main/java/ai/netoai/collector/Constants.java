package ai.netoai.collector;

public class Constants {
    public static final String rootDir = System.getProperty("platform.home");

    public static final String KAFKA_BROKER_PROP = "kafkaBrokers";


    // Properties defined in "nms_system.properties" file
    public static final String ZOOKEEPER_URL = "zookeeperUrl";
    public static final String SNMP_COLLECTOR_SERVICE_URL = "snmpCollectorServiceURL";
    public static final String SNMP_DOCKER_SERVICE_NAME = "serviceName";
    public static final String PROM_TARGET_SERVICE_URL = "promTargetServiceURL";

    // Properties defined in "collector.properties" file
    public static final String ZK_SESSION_TIMEOUT = "zkSessionTimeout";
    public static final String KAFKA_MONITOR_TIME = "kafkaMonitorTime";
    public static final String BIND_ADDRESS = "bindAddress";
    public static final String PING_MONITOR = "ping-monitor";
    public static final String PING_STATS_METRIC_FAMILY= "pingStats";
    public static final String PING_MONITOR_SCRIPT = "ping-monitor.py";
    public static final String SNMP_COLLECTOR_GROUP = "adapterGroupName";

    public static final String NETWORK = "NETWORK";
    public static final String SUB_NETWORK = "SUBNETWORK";
}
