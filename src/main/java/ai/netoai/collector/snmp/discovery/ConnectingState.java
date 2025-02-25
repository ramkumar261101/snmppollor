package ai.netoai.collector.snmp.discovery;

import java.net.UnknownHostException;

import ai.netoai.collector.model.NetworkElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectingState implements NodeState {

	private static final Logger log = LoggerFactory.getLogger(ConnectingState.class);
	private static final String tracePrefix = "[" + ConnectingState.class.getSimpleName() + "]: ";
	private NodeAdapter adapter;

	public ConnectingState(NodeAdapter adapter) {
		super();
		this.adapter = adapter;
	}

	@Override
	public void checkConnection() throws UnknownHostException {
		boolean success = adapter.checkSnmpConnection(adapter);
		if (success) {
			adapter.setCurrentState(adapter.getConnectedStateObj());
		} else {
			adapter.setCurrentState(adapter.getDisConnectingStateObj());
		}
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
		// TODO Auto-generated method stub

	}

	@Override
	public NetworkElement.State getState() {
		// TODO Auto-generated method stub
		return NetworkElement.State.CONNECTING;
	}

}
