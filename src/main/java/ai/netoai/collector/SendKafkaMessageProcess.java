package ai.netoai.collector;


import ai.netoai.collector.model.*;
import ai.netoai.collector.settings.KafkaTopicSettings;
import com.google.common.collect.Lists;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.ArrayList;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Path("/kafka")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SendKafkaMessageProcess {
    @POST
    @Path("/sendDiscoveryTaskMessage")
    public void sendDiscoveryTaskMessage() {
        System.out.println("Sending a Kafka message for discovery task");
        ConfigMessage discoveryMessage = createDiscoveryTaskMessage();
        sendMessage(discoveryMessage);
        System.out.println("Sent a Kafka message for discovery task");

        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @POST
    @Path("/sendTopologyDiscoveryTaskMessage")
    public void sendTopologyDiscoveryTaskMessage() {
        System.out.println("Sending a Kafka message for topology discovery task");
        ConfigMessage discoveryMessage = createTopologyDiscoveryTaskMessage();
        sendMessage(discoveryMessage);
        System.out.println("Sent a Kafka message for topology discovery task");

        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @POST
    @Path("/test")
    public String test() {
        return "Topology discovery task message sent successfully";
    }

    private static void sendMessage(ConfigMessage message) {
        String broker = "localhost:9092";
        Properties props = new Properties();
        props.put("bootstrap.servers", broker);
        props.put("acks", "all");
        props.put("retries", 1);
        props.put("batch.size", 16384);
        props.put("linger.ms", 1);
        props.put("buffer.memory", 33554432);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        KafkaProducer producer = new KafkaProducer<>(props);
        ProducerRecord<String, String> rec = new ProducerRecord<String, String>(
                KafkaTopicSettings.COLLECTOR_INCOMING_TOPIC, 0,
                UUID.randomUUID().toString(), GenericJavaBean.toJson(message));
        producer.send(rec);
        System.out.println("Discovery Message sent to " + broker);
    }

    private static ConfigMessage createDiscoveryTaskMessage() {
        DiscoveryTask discoveryTask = createDiscoveryTask();
        return new ConfigMessage(ConfigMessage.MsgType.START_DISCOVERY, discoveryTask);
    }

    private static DiscoveryTask createDiscoveryTask() {
        DiscoveryTask discoveryTask = new DiscoveryTask();
        discoveryTask.setDiscoveryType(DiscoveryType.INVENTORY.name());
        discoveryTask.setActive(true);
        discoveryTask.setCredentials(new ArrayList<>());
        discoveryTask.setName("Sample Discovery Task");
        discoveryTask.setIpRange(Lists.newArrayList("192.168.100.1-192.168.100.120"));
        discoveryTask.setSchedule("");
        discoveryTask.setEnableIcmp(false);
        discoveryTask.setStatus(DiscoveryStatus.CREATED);
        discoveryTask.setEnableSchedule(false);
        SnmpAuthProfile authProfile = new SnmpAuthProfile();
        authProfile.setUuid(UUID.randomUUID().toString());
        authProfile.setSnmpVersion(SnmpVersion.SNMPv2C);
        authProfile.setCommunity("public");
        authProfile.setTimeout(15);
        authProfile.setRetries(3);
        discoveryTask.setAuthProfiles(Lists.newArrayList(authProfile));
        return discoveryTask;
    }

    private static ConfigMessage createTopologyDiscoveryTaskMessage() {
        DiscoveryTask discoveryTask = createTopologyDiscoveryTask();
        return new ConfigMessage(ConfigMessage.MsgType.START_DISCOVERY, discoveryTask);
    }

    private static DiscoveryTask createTopologyDiscoveryTask() {
        DiscoveryTask discoveryTask = new DiscoveryTask();
        discoveryTask.setDiscoveryType(DiscoveryType.TOPOLOGY.name());
        discoveryTask.setActive(true);
        discoveryTask.setCredentials(new ArrayList<>());
        discoveryTask.setName("Sample Topology Discovery Task");
        discoveryTask.setIpRange(Lists.newArrayList("192.168.100.1-192.168.100.120"));
        discoveryTask.setSchedule("");
        discoveryTask.setEnableIcmp(true);
        discoveryTask.setStatus(DiscoveryStatus.CREATED);
        discoveryTask.setEnableSchedule(false);
        SnmpAuthProfile authProfile = new SnmpAuthProfile();
        authProfile.setUuid(UUID.randomUUID().toString());
        authProfile.setSnmpVersion(SnmpVersion.SNMPv2C);
        authProfile.setCommunity("public");
        authProfile.setTimeout(15);
        authProfile.setRetries(3);
        discoveryTask.setAuthProfiles(Lists.newArrayList(authProfile));
        return discoveryTask;
    }
}
