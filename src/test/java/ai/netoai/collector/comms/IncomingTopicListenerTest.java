package ai.netoai.collector.comms;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IncomingTopicListenerTest {
	private static final Logger log = LoggerFactory
			.getLogger(IncomingTopicListenerTest.class);
	private static final String tracePrefix = "["
			+ IncomingTopicListenerTest.class.getSimpleName() + "]: ";

	@Test
	public void testListen() throws InterruptedException {
		System.setProperty("nmsConfig.collector", "src/test/resources/collector.properties");
                System.setProperty("nmsConfig.nms_system", "src/test/resources/nms_system.properties");
                try {
                    IncomingTopicListener.getInstance().start();
                } catch (Exception ex) {
                    log.error(tracePrefix + "Failed starting the listener", ex);
                }
		Thread.currentThread().sleep(60000l);
		
	}
}
