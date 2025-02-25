package ai.netoai.collector.snmp;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import ai.netoai.collector.cache.CacheManager;
import ai.netoai.collector.deviceprofile.Config;
import ai.netoai.collector.model.EndPoint;
import ai.netoai.collector.model.NetworkElement;
import ai.netoai.collector.settings.SettingsManager;
import ai.netoai.collector.settings.SnmpSettings;
import ai.netoai.collector.startup.PollerThreadFactory;
import ai.netoai.collector.utils.TopicJsonSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SnmpPerformancePoller implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(SnmpPerformancePoller.class);
    private static final String tracePrefix = "[" + SnmpPerformancePoller.class.getSimpleName() + "]: ";
    private static SnmpPerformancePoller instance;
    private SnmpManager snmpManager;
    private CacheManager cacheManager;
    private SettingsManager setting;
    private ScheduledExecutorService perfPollExecutor;
    private ScheduledExecutorService appMonScanTask;
    private ExecutorService appMonPollExecutor;
    private List<AsyncSnmpResponseListener> respListeners = new ArrayList<>();
    private SnmpSettings snmpSettings;
    private TopicJsonSender topicSender;
    private int count = 0;
	private String tenantId;
	private static int globalCounter = 0;

    private SnmpPerformancePoller() {
        setting = SettingsManager.getInstance();
        this.snmpSettings = new SnmpSettings();

        this.snmpManager = SnmpManager.getInstance();
        this.cacheManager = CacheManager.getInstance();
		this.tenantId = System.getProperty("tenantId");
        for (int i = 0; i < snmpSettings.getResponseListCount(); i++) {
            AsyncSnmpResponseListener asyncRespListener = new AsyncSnmpResponseListener();
            this.respListeners.add(asyncRespListener);
        }
        topicSender = TopicJsonSender.getInstance();
        log.info(tracePrefix + "Number of async response listeners intialized: " + respListeners.size());
        this.perfPollExecutor = Executors.newScheduledThreadPool(EndPoint.PerfPeriodTime.values().length,
                new PollerThreadFactory("PerformancePollExecutor"));

        this.appMonScanTask = Executors.newScheduledThreadPool(EndPoint.PerfPeriodTime.values().length,
                new PollerThreadFactory("MonitorPollExecutor"));

        log.info(tracePrefix + "Init ...");
    }

    public static synchronized SnmpPerformancePoller getInstance() {
        if (instance == null) {
            instance = new SnmpPerformancePoller();
        }
        return instance;
    }
    
    private class AppMonWorker implements Runnable {
        private NetworkElement ne;
        private EndPoint monitorEndPoint;
        private List<Config> pmConfigs;
        private String monitorScriptsLocation;
        private long reqTime;
        
        public AppMonWorker(NetworkElement ne, EndPoint monitorEndPoint, List<Config> pmConfigs, String monitorScriptsLocation, long reqTime) {
            this.ne = ne;
            this.monitorEndPoint = monitorEndPoint;
            this.pmConfigs = pmConfigs;
            this.monitorScriptsLocation = monitorScriptsLocation;
            this.reqTime = reqTime;
        }
        
        @Override
        public void run() {
            log.info("Performance polling not enabled ...");
        }
        
    }

    @Override
    public void run() {
        log.warn("Performance polling not enabled ...");
    }
}
