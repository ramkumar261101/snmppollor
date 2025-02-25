package ai.netoai.collector.snmp.discovery;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import ai.netoai.collector.cache.NodeCacheManager;
import ai.netoai.collector.model.NetworkElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.AbstractTarget;

import ai.netoai.collector.snmp.SnmpPoller;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


public class SyncAlarmsState implements NodeState {

	private static final Logger log = LoggerFactory.getLogger(SyncAlarmsState.class);
	private static final String tracePrefix = "[" + SyncAlarmsState.class.getSimpleName() + "]: ";
	private NodeAdapter adapter;
	private SnmpPoller poller;

	public SyncAlarmsState(NodeAdapter adapter) {
		this.adapter = adapter;
		this.poller = SnmpPoller.getInstance();
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
		log.info("Alarm sync not enabled ...");
		adapter.setCurrentState(adapter.getSynchedStateObj());
	}

	@Override
	public void doCleanUp() {
		// TODO Auto-generated method stub

	}

	@Override
	public void doNoOperation() {
		adapter.setCurrentState(adapter.getSynchedStateObj());
	}

	@Override
	public NetworkElement.State getState() {
		return NetworkElement.State.SYNC_ALARMS;
	}

}
