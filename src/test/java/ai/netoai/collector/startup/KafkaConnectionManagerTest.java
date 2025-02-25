package ai.netoai.collector.startup;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class KafkaConnectionManagerTest {
	private static final Logger log = LoggerFactory
			.getLogger(KafkaConnectionManagerTest.class);
	private static final String tracePrefix = "["
			+ KafkaConnectionManagerTest.class.getSimpleName() + "]: ";

	static {
	}

	@Test
	public void testForward() throws InterruptedException {
		TopicProducer.init();
		TopicProducer.getInstance().start();	
		KafkaConnectionManager manager=new KafkaConnectionManager("sample_topic");
		manager.monitorKafkaConnection();
		Thread.sleep(10000l);
	}
}
