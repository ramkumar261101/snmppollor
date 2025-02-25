package ai.netoai.collector.snmp.discovery;

import java.net.UnknownHostException;

import ai.netoai.collector.model.NetworkElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnknownState implements NodeState {

	private static final Logger log = LoggerFactory.getLogger(UnknownState.class);
	private static final String tracePrefix = "[" + UnknownState.class.getSimpleName() + "]: ";
	private NodeAdapter adapter;

	public UnknownState(NodeAdapter adapter) {
		super();
		this.adapter = adapter;
	}

	@Override
	public void checkConnection() throws UnknownHostException {
		
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
		// TODO Auto-generated method stub

	}

	@Override
	public void doNoOperation() {
        adapter.setCurrentState(adapter.getConnectingStateObj());
	}

	@Override
	public NetworkElement.State getState() {
		return NetworkElement.State.UNKNOWN;
	}

}
