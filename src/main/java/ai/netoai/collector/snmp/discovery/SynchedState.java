package ai.netoai.collector.snmp.discovery;

import java.net.UnknownHostException;
import java.util.Date;

import ai.netoai.collector.model.NetworkElement;
import ai.netoai.collector.settings.SettingsManager;
import ai.netoai.collector.settings.SnmpSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.netoai.collector.snmp.SnmpPoller;

public class SynchedState implements NodeState {
	private static final Logger log = LoggerFactory.getLogger(SynchedState.class);
	private static final String tracePrefix = "[" + SynchedState.class.getSimpleName() + "]: ";
	private NodeAdapter adapter;
	private SnmpPoller poller;
	private SettingsManager manager;
	private SnmpSettings snmpSettings;

	public SynchedState(NodeAdapter adapter) {
		this.adapter = adapter;
		this.poller = SnmpPoller.getInstance();
		this.manager = SettingsManager.getInstance();
		snmpSettings = new SnmpSettings();
	}

	@Override
	public void checkConnection() throws UnknownHostException {
		NetworkElement ne = adapter.getNetworkelement();
		int reconnectInterval = snmpSettings.getReconnectInterval();
		if (ne.getLastSyncTime() == null) {
			boolean success = adapter.checkSnmpConnection(adapter);
			if (!success) {
				adapter.setCurrentState(adapter.getDisConnectingStateObj());
			}
			ne.setLastSyncTime(new Date());
		} else {
			long syncTime = ne.getLastSyncTime().getTime();
			long syncDiff = (System.currentTimeMillis()) - syncTime;
			if (syncDiff >= (reconnectInterval * 1000l)) {
				boolean success = adapter.checkSnmpConnection(adapter);
				if (!success) {
					adapter.setCurrentState(adapter.getDisConnectingStateObj());
				}
				ne.setLastSyncTime(new Date());
			}
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
		return NetworkElement.State.SYNCED;
	}

}
