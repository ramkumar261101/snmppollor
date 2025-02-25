package ai.netoai.collector.snmp.discovery;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import ai.netoai.collector.cache.NodeCacheManager;
import ai.netoai.collector.deviceprofile.Config;
import ai.netoai.collector.deviceprofile.ConfigManager;
import ai.netoai.collector.deviceprofile.InventoryConfig;
import ai.netoai.collector.deviceprofile.SnmpConfig;
import ai.netoai.collector.model.DeviceEntity;
import ai.netoai.collector.model.EndPoint;
import ai.netoai.collector.model.GenericJavaBean;
import ai.netoai.collector.model.NetworkElement;
import ai.netoai.collector.settings.KafkaTopicSettings;
import ai.netoai.collector.utils.TopicJsonSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.AbstractTarget;

import ai.netoai.collector.snmp.SnmpPoller;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


public class SyncInventoryState implements NodeState {

	private static final Logger log = LoggerFactory.getLogger(SyncInventoryState.class);
	private static final String tracePrefix = "[" + SyncInventoryState.class.getSimpleName() + "]: ";
	private NodeAdapter adapter;
	private SnmpPoller poller;
	private TopicJsonSender topicInstance;

	public SyncInventoryState(NodeAdapter adapter) {
		this.adapter = adapter;
		this.poller = SnmpPoller.getInstance();
		this.topicInstance = TopicJsonSender.getInstance();
	}

	@Override
	public void checkConnection() {
		// TODO Auto-generated method stub

	}

	@Override
	public void doDiscovery() {
		// TODO Auto-generated method stub

	}

	@Override
	public void doInventorySync() {
		try {
			log.info(tracePrefix + "Starting inventory sync for Node: " + adapter.getNetworkelement());
			NetworkElement ne = adapter.getNetworkelement();
			String profileId = ne.getProfileId();
			AbstractTarget target = null;
			try {
				target = poller.createAppropriateTarget(ne);
			} catch (UnknownHostException e) {
				log.error(tracePrefix + "Failed polling node: " + ne, e);
			}

			Gson gson = new GsonBuilder().serializeNulls().create();
			log.info("{}Created the target, profile for the NE: {} is: {}", tracePrefix, adapter.getNetworkelement(), profileId);
			if (profileId != null && !profileId.equalsIgnoreCase("unknown")) {
				List<GenericJavaBean> endPointsList = new ArrayList<GenericJavaBean>();
				if (!ConfigManager.getConfigMap().isEmpty() && ConfigManager.getConfigMap().get(profileId) != null) {
					SnmpConfig snmpConfig = ConfigManager.getConfigMap().get(profileId);
					List<Config> childConfigs = snmpConfig.getChildConfigs();
					log.info("{}Number of child configs found: {}", tracePrefix, childConfigs.size());
					for (Config conf : childConfigs) {
						if (conf instanceof InventoryConfig) {
							InventoryConfig ic = (InventoryConfig) conf;
							SnmpProfileHelper profileHelper = new SnmpProfileHelper();
							List<GenericJavaBean> beansList = profileHelper.createGenericBeanObjects(ic, target);
							endPointsList.addAll(beansList);
						}
					}
					log.info("{}Constructed endpoints ...", tracePrefix);

					List<EndPoint> endPointL = new ArrayList<>();
					for (GenericJavaBean bean : endPointsList) {
						EndPoint endPoint = (EndPoint) bean;
						endPoint.setNetworkElementIp(ne.getIp());
						endPoint.setNetworkElementName(ne.getName());

						String key = endPoint.getNetworkElementIp() + "_" + endPoint.getSourceId();
						endPoint.setId(key.hashCode() + "");
						endPointL.add(endPoint);
					}

					EndPoint monitorEndPoint = new EndPoint();
					monitorEndPoint.setNetworkElementIp(ne.getIp());
					monitorEndPoint.setSourceId(ne.getIp());
					monitorEndPoint.setSourceName(ne.getName());
					monitorEndPoint.setDescription(ne.getDescription());
					monitorEndPoint.setNetworkElementName(ne.getName());
					monitorEndPoint.setEndPointType(EndPoint.EndPointType.NETWORK_ELEMENT);
					String key = monitorEndPoint.getNetworkElementIp() + "_" + monitorEndPoint.getSourceId();
					monitorEndPoint.setId(key.hashCode() + "");
					endPointL.add(monitorEndPoint);
					log.info(tracePrefix + "Number of EndPoints for Node: " + ne + ": " + endPointL.size());
//					String jsonString = gson.toJson(endPointL);
					NodeCacheManager.getInstance().cacheEndpoints(ne, endPointL);
					SnmpProfileHelper profileHelper = new SnmpProfileHelper();
					DeviceEntity root = profileHelper.fetchRelationships(ne, endPointL, target);
					String nodeJson = JsonUtils.convertToJson(ne, endPointL, root);
					topicInstance.send(KafkaTopicSettings.INVENTORY_TOPIC, nodeJson);
					adapter.setCurrentState(adapter.getSynchedStateObj());
				}
			}
		} finally {
			adapter.resetInventorySyncInProgress();
		}
	}

	@Override
	public void doAlarmSync() {

	}

	@Override
	public void doCleanUp() {
		// TODO Auto-generated method stub

	}

	@Override
	public void doNoOperation() {
		// TODO Auto-generated method stub

	}

	@Override
	public NetworkElement.State getState() {
		return NetworkElement.State.SYNC_INV;
	}

}
