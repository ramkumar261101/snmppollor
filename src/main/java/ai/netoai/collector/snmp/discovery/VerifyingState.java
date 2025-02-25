package ai.netoai.collector.snmp.discovery;

import java.net.UnknownHostException;
import java.util.List;

import ai.netoai.collector.deviceprofile.Config;
import ai.netoai.collector.deviceprofile.ConfigManager;
import ai.netoai.collector.deviceprofile.DeviceConfig;
import ai.netoai.collector.deviceprofile.SnmpConfig;
import ai.netoai.collector.model.GenericJavaBean;
import ai.netoai.collector.model.NetworkElement;
import ai.netoai.collector.model.NetworkProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.AbstractTarget;

import ai.netoai.collector.snmp.SnmpPoller;

public class VerifyingState implements NodeState{

	private static final Logger log = LoggerFactory.getLogger(VerifyingState.class);
	private static final String tracePrefix = "[" + VerifyingState.class.getSimpleName() + "]: ";
	private NodeAdapter adapter;
	private SnmpPoller poller;
	
	public VerifyingState(NodeAdapter adapter) {
		this.adapter = adapter;
		this.poller = SnmpPoller.getInstance();
	}
	
	@Override
	public void checkConnection() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void doDiscovery() {
		log.info(tracePrefix + "Starting Node Verification for: " + adapter.getNetworkelement() + ", Protocol: " + adapter.getNetworkelement().getProtocol());
		NetworkElement ne = adapter.getNetworkelement();
		if(!ne.getProtocol().equals(NetworkProtocol.ICMP)){
			DeviceTypeIdentifier dti = DeviceTypeIdentifier.getInstance();
			String deviceType = dti.identifyNE(adapter.getNetworkelement());
			ne.setProfileId(deviceType);
			
			String profileId = ne.getProfileId();
			AbstractTarget target = null;
			try {
				target = poller.createAppropriateTarget(ne);
			} catch (UnknownHostException e) {
				log.error(tracePrefix + "Failed polling the Node: " + ne, e);
			}
			if(profileId != null && !profileId.equalsIgnoreCase("unknown")){
				if(!ConfigManager.getConfigMap().isEmpty() && ConfigManager.getConfigMap().get(profileId) != null){
					SnmpConfig snmpConfig = ConfigManager.getConfigMap().get(profileId);
					List<Config> childConfigs = snmpConfig.getChildConfigs();
					for(Config conf : childConfigs){
						if(conf instanceof DeviceConfig){
							DeviceConfig dc = (DeviceConfig) conf;
							SnmpProfileHelper profileHelper = new SnmpProfileHelper();
							List<GenericJavaBean> list = profileHelper.createGenericBeanObjects(dc,target);
							// to-store on kafka
							for(GenericJavaBean bean : list){
								NetworkElement ele = (NetworkElement) bean;
								NetworkElement mergeEle = ne.merge(ele);
								bean.clearProfileValuesSet();        
							}
						}
					}
					adapter.setCurrentState(adapter.getSyncInvStateObj());
				}else{
					log.info(tracePrefix+"Couldnt Find Config TarFile for "+profileId);
					ne.setProtocol(NetworkProtocol.ICMP);
                                        adapter.setNetworkElement(ne);
                                        adapter.setCurrentState(adapter.getConnectedStateObj());
				}
			}
		}
	
		
	}

	@Override
	public void doInventorySync() {
		
	}

	@Override
	public void doAlarmSync() {
		// TODO Auto-generated method stub
		
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
		return NetworkElement.State.VERIFYING;
	}

}
