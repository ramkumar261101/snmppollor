package ai.netoai.collector.snmp.discovery;

import ai.netoai.collector.model.NetworkElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DisconnectingState implements NodeState{

	private static final Logger log = LoggerFactory.getLogger(DisconnectingState.class);
	private static final String tracePrefix = "[" + DisconnectingState.class.getSimpleName() + "]: ";
    private NodeAdapter adapter;
	
	public DisconnectingState(NodeAdapter adapter) {
		this.adapter = adapter;
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
		// TODO Auto-generated method stub
		
	}

	@Override
	public void doAlarmSync() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void doCleanUp() {
		adapter.setCurrentState(adapter.getDisConnectedStateObj());
	}

	@Override
	public void doNoOperation() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public NetworkElement.State getState() {
		return NetworkElement.State.DISCONNECTING;
	}

}
