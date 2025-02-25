package ai.netoai.collector.snmp.discovery;

import ai.netoai.collector.model.NetworkElement;

import java.net.UnknownHostException;

public interface NodeState {
	
	void checkConnection() throws UnknownHostException;
	void doDiscovery();
	void doInventorySync();
	void doAlarmSync();
	void doCleanUp();
	void doNoOperation();
	public NetworkElement.State getState();
}
