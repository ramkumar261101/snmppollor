package ai.netoai.collector.snmp.discovery;

import ai.netoai.collector.model.NetworkElement;
import ai.netoai.collector.model.NetworkProtocol;
import ai.netoai.collector.utils.TopicJsonSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectedState implements NodeState{

	private static final Logger log = LoggerFactory.getLogger(ConnectedState.class);
	private static final String tracePrefix = "[" + ConnectedState.class.getSimpleName() + "]: ";
	private NodeAdapter adapter;
	
	public ConnectedState(NodeAdapter adapter) {
		this.adapter = adapter;
	}
	
	@Override
	public void checkConnection() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void doDiscovery() {
		
	}

	@Override
	public void doInventorySync() {
		// TODO Auto-generated method stub
		
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
		NetworkElement ne = adapter.getNetworkelement();
		log.info("{}NetworkElement: {} protocol: {}", tracePrefix, ne, ne.getProtocol());
		if(ne.getProtocol().equals(NetworkProtocol.ICMP)){
			adapter.setCurrentState(adapter.getConnectedStateObj());
		}else{
			adapter.setCurrentState(adapter.getVerifyingStateObj());
		}
	}

	@Override
	public NetworkElement.State getState() {
		return NetworkElement.State.CONNECTED;
	}

	

}
