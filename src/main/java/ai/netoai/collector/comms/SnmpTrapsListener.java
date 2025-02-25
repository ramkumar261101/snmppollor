package ai.netoai.collector.comms;

import ai.netoai.collector.Constants;
import ai.netoai.collector.convert.ConvertManagerHelper;
import ai.netoai.collector.settings.KafkaTopicSettings;
import ai.netoai.collector.settings.SettingsManager;
import ai.netoai.collector.snmp.WrappedCREvent;
import ai.netoai.collector.snmp.trap.SnmpTrap;
import ai.netoai.collector.startup.CollectorMain;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.smi.*;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class SnmpTrapsListener {

    private static final Logger log = LoggerFactory
            .getLogger(SnmpTrapsListener.class);
    private static final String tracePrefix = "["
            + SnmpTrapsListener.class.getSimpleName() + "]: ";

    private static SnmpTrapsListener instance;
    private SettingsManager sm;
    private KafkaTopicSettings kts;
    private Map<String, Object> collectorSettings;
    private KafkaConsumer<String, String> consumer;
    private boolean alive;
    private LinkedBlockingQueue<String> msgsQueue = new LinkedBlockingQueue<>();
    private ExecutorService executor;
    private Gson gson = new GsonBuilder().serializeNulls().create();

    private SnmpTrapsListener() {
        sm = SettingsManager.getInstance();
        kts = new KafkaTopicSettings();
        collectorSettings = sm.getSettings();
        this.executor = Executors.newFixedThreadPool(kts.getCollectorIcMsgProcThreads());
    }

    public synchronized static SnmpTrapsListener getInstance() {
        if (instance == null) {
            instance = new SnmpTrapsListener();
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
                + KafkaTopicSettings.DK_SNMP_TRAPS);

        Thread processingThread = new Thread(new Runnable() {

            @Override
            public void run() {

                while (alive) {
                    try {
                        String message = msgsQueue.take();
                        executor.execute(new MessageWorker(message));
                    } catch (InterruptedException e) {
                        log.error(tracePrefix + "Failed queuing the message", e);
                    }
                }

            }

        }, "Trap Listener");
        processingThread.setDaemon(true);
        processingThread.start();

        Properties props = new Properties();
        props.put("bootstrap.servers", collectorSettings.get("kafkaBrokers"));
        props.put("group.id", collectorSettings.get("adapterGroupName"));
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");
        consumer = new KafkaConsumer<>(props, new StringDeserializer(), new StringDeserializer());

        consumer.subscribe(Arrays.asList(KafkaTopicSettings.DK_SNMP_TRAPS));
        log.info(tracePrefix + "Polling the topic: " + KafkaTopicSettings.DK_SNMP_TRAPS);
        while (alive) {
            ConsumerRecords<String, String> records = consumer.poll(1000);
            try {
                for (ConsumerRecord<String, String> rec : records) {
                    log.info(tracePrefix + "Message received on dk_snmp_traps: \n" + rec.value());
                    boolean success = msgsQueue.offer(rec.value());
                    if ( success ) {
                        log.warn(tracePrefix + "Failed processing message: " + rec.value() + ", Queue size: " + msgsQueue.size());
                    }
                }
            } catch (Throwable t) {
                log.error(tracePrefix + "Failed processing batch of records: " + records.count(), t);
            }
        }
    }

    private class MessageWorker implements Runnable {

        Map<String, Object> trapMap;

        MessageWorker(String trapJson) {
            trapMap = gson.fromJson(trapJson, Map.class);
        }

        @Override
        public void run() {
            log.info(tracePrefix + "Processing the message trap: " + trapMap);
            if ( !trapMap.containsKey("domainName") ) {
                log.error(tracePrefix + "Received trap without domain name: " + trapMap);
                return;
            }
            String domainName = trapMap.get("domainName").toString();
            SnmpTrap snmpTrap = convertToSnmpTrap();
            ConvertManagerHelper.getInstance().processTrapEvents(new WrappedCREvent(snmpTrap, System.currentTimeMillis()));
        }

        private SnmpTrap convertToSnmpTrap() {
            SnmpTrap snmpTrap = new SnmpTrap(null);
            if ( trapMap.containsKey("agentAddress") && trapMap.get("agentAddress") != null ) {
                snmpTrap.setAgentAddress(trapMap.get("agentAddress").toString());
            }
            if ( trapMap.containsKey("trapType") && trapMap.get("trapType") != null ) {
                Double d = Double.parseDouble(trapMap.get("trapType").toString());
                snmpTrap.setTrapType(d.intValue());
            }
            if ( trapMap.containsKey("trapOid") && trapMap.get("trapOid") != null ) {
                snmpTrap.setTrapOid(new OID(trapMap.get("trapOid").toString()));
            }
            if ( trapMap.containsKey("enterpriseOID") && trapMap.get("enterpriseOID") != null ) {
                snmpTrap.setEnterpriseOid(new OID(trapMap.get("enterpriseOID").toString()));
            }
            if ( trapMap.containsKey("genericTrap") && trapMap.get("genericTrap") != null ) {
                Double d = Double.parseDouble(trapMap.get("genericTrap").toString());
                snmpTrap.setGenericTrap(d.intValue());
            }
            if ( trapMap.containsKey("specificTrap") && trapMap.get("specificTrap") != null ) {
                Double d = Double.parseDouble(trapMap.get("specificTrap").toString());
                snmpTrap.setSpecificTrap(d.intValue());
            }
            if ( trapMap.containsKey("timeStamp") && trapMap.get("timeStamp") != null ) {
                snmpTrap.setTimeStamp(Long.parseLong(trapMap.get("timeStamp").toString()));
            }
            if ( trapMap.containsKey("varbinds") && trapMap.get("varbinds") != null ) {
                Map<String, Object> vbMap = (Map<String, Object>) trapMap.get("varbinds");
                List<VariableBinding> vbList = new ArrayList<>();
                vbMap.forEach((key, val) -> {
                    VariableBinding vb = new VariableBinding();
                    vb.setOid(new OID(key));
                    try {
                        if ( val != null ) {
                            int intVal = Integer.parseInt(val.toString());
                            vb.setVariable(new Integer32(intVal));
                        }
                    } catch (Exception ex) {
                        // If int parse has failed then assuming the value as octet string
                        if ( val != null ) {
                            vb.setVariable(new OctetString(val.toString()));
                        }
                    }
                    vbList.add(vb);
                });
                snmpTrap.setVarBind(vbList);
            }
            return snmpTrap;
        }

    }
}

