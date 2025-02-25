package ai.netoai.collector.snmp.discovery;

import java.net.UnknownHostException;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import ai.netoai.collector.model.NetworkElement;
import ai.netoai.collector.model.NetworkProtocol;
import ai.netoai.collector.settings.KafkaTopicSettings;
import ai.netoai.collector.utils.TopicJsonSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.AbstractTarget;
import org.snmp4j.PDU;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;

import ai.netoai.collector.snmp.SnmpPoller;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class NodeAdapter {
	private static final Logger log = LoggerFactory.getLogger(NodeAdapter.class);
	private static final String tracePrefix = "[" + NodeAdapter.class.getSimpleName() + "]: ";
	private NodeState currentstate;
	private NetworkElement networkelement;
	private NodeState unknownStateObj = new UnknownState(this);
	private NodeState connectingStateObj = new ConnectingState(this);
	private NodeState connectedStateObj = new ConnectedState(this);
	private NodeState syncInvStateObj = new SyncInventoryState(this);
	private NodeState syncAlarmStateObj = new SyncAlarmsState(this);
	private NodeState synchedStateObj = new SynchedState(this);
	private NodeState disConnectingStateObj = new DisconnectingState(this);
	private NodeState disConnectedStateObj = new DisconnectedState(this);
	private NodeState verifyingStateObj = new VerifyingState(this);
        private Gson gson = new GsonBuilder().serializeNulls().create();
	private static ExecutorService connectionandNEExecutor = Executors.newFixedThreadPool(100);
	private static ExecutorService discoverEndPointExecutor = Executors.newFixedThreadPool(100);
	private static ExecutorService discoverAlarmExecutor = Executors.newFixedThreadPool(10);
	private AtomicBoolean inventorySyncInProgress = new AtomicBoolean(false);

	private SnmpPoller poller;

	public NodeAdapter(NetworkElement element) {
		this.currentstate = unknownStateObj;
                if ( element.getConnState() != null ) {
                    switch(element.getConnState()) {
                        case CONNECTED:
                            currentstate = connectedStateObj;
                            break;
                        case CONNECTING:
                            currentstate = connectingStateObj;
                            break;
                        case DISCONNECTED:
                            currentstate = disConnectedStateObj;
                            break;
                        case DISCONNECTING:
                            currentstate = disConnectingStateObj;
                            break;
                        case SYNCED:
                            currentstate = synchedStateObj;
                            break;
                        case SYNC_ALARMS:
                            currentstate = syncAlarmStateObj;
                            break;
                        case SYNC_INV:
                            currentstate = syncInvStateObj;
                            break;
                        case UNKNOWN:
                            currentstate = unknownStateObj;
                            break;
                        case VERIFYING:
                            currentstate = verifyingStateObj;
                            break;
                        default:
                            currentstate = unknownStateObj;
                    }
                }
                log.info(tracePrefix + "Added NetworkElement: " + element.getName() + ", with state: " + this.currentstate.getState());
		this.networkelement = element;
		this.poller = SnmpPoller.getInstance();
	}

	public NetworkElement getNetworkelement() {
		return networkelement;
	}

	public NodeState getCurrentState() {
		return currentstate;
	}
        
	public void setNetworkElement(NetworkElement ne) {
		if ( ne == null ) {
			return;
		}
		this.networkelement = ne;
	}

	public void setCurrentState(NodeState state) {
		if (state != null && !state.getState().name().equalsIgnoreCase(this.currentstate.getState().name())) {
			log.info(tracePrefix + "Node: " + networkelement + " State Changed, Old State: " + currentstate.getState() + ", New State: " + state.getState());
			this.currentstate = state;
			this.networkelement.setConnState(state.getState());
			String neString = gson.toJson(this.networkelement);
//			TopicJsonSender.getInstance().send(KafkaTopicSettings.INVENTORY_TOPIC, neString);
		} else {
			log.warn(tracePrefix + "Node: " + networkelement + ", Old State: " + currentstate.getState() + ", New State: " + state.getState());
		}
	}

	public NodeState getUnknownStateObj() {
		return unknownStateObj;
	}

	public void setUnknownStateObj(NodeState unknownStateObj) {
		this.unknownStateObj = unknownStateObj;
	}

	public NodeState getConnectingStateObj() {
		return connectingStateObj;
	}

	public void setConnectingStateObj(NodeState connectingStateObj) {
		this.connectingStateObj = connectingStateObj;
	}

	public NodeState getConnectedStateObj() {
		return connectedStateObj;
	}

	public void setConnectedStateObj(NodeState connectedStateObj) {
		this.connectedStateObj = connectedStateObj;
	}

	public NodeState getSyncInvStateObj() {
		return syncInvStateObj;
	}

	public void setSyncInvStateObj(NodeState syncInvStateObj) {
		this.syncInvStateObj = syncInvStateObj;
	}

	public NodeState getSyncAlarmStateObj() {
		return syncAlarmStateObj;
	}

	public void setSyncAlarmStateObj(NodeState syncAlarmStateObj) {
		this.syncAlarmStateObj = syncAlarmStateObj;
	}

	public NodeState getSynchedStateObj() {
		return synchedStateObj;
	}

	public void setSynchedStateObj(NodeState synchedStateObj) {
		this.synchedStateObj = synchedStateObj;
	}

	public NodeState getDisConnectingStateObj() {
		return disConnectingStateObj;
	}

	public void setDisConnectingStateObj(NodeState disConnectingStateObj) {
		this.disConnectingStateObj = disConnectingStateObj;
	}

	public NodeState getDisConnectedStateObj() {
		return disConnectedStateObj;
	}

	public void setDisConnectedStateObj(NodeState disConnectedStateObj) {
		this.disConnectedStateObj = disConnectedStateObj;
	}

	public NodeState getVerifyingStateObj() {
		return verifyingStateObj;
	}

	public void setVerifyingStateObj(NodeState verifyingStateObj) {
		this.verifyingStateObj = verifyingStateObj;
	}

	public boolean checkSnmpConnection(NodeAdapter adapter) {
		NetworkElement networkEle = adapter.getNetworkelement();
		OID nameOID = SnmpConstants.sysName;
		PDU pdu = new PDU();
		pdu.setType(PDU.GET);
		pdu.add(new VariableBinding(nameOID));
		NetworkProtocol protocol = networkEle.getProtocol();

		AbstractTarget target = null;
		try {
			target = poller.createAppropriateTarget(networkEle);
		} catch (Exception e) {
			log.error("Failed creating the target for node: " + networkEle, e);
			return false;
		}
		boolean success = false;
		int retries = 5;
		int counter = 0;
		while ( counter < retries ) {
			if ( counter > 0 ) {
				log.warn("{}Retrying the Address {} for connectivity check {}/{} time", tracePrefix, target.getAddress().toString(), (counter + 1), retries);
			}
			try {
				PDU responsePdu = poller.sendSyncGetRequest(target, pdu);
				if (responsePdu != null) {
					Vector<VariableBinding> vbs = (Vector<VariableBinding>) responsePdu.getVariableBindings();
					if (vbs != null && vbs.size() != 0) {
						String value = vbs.get(0).getVariable().toString();
						if (!value.equals("noSuchObject") || !value.isEmpty()) {
							success = true;
							break;
						} else {
							log.error("{}CONN-CHECK: Failed to get valid response for node: {}, Resp: {}", tracePrefix, networkEle, value);
						}
					} else {
						log.error("{}CONN-CHECK: Failed to get the connection for node: {}", tracePrefix, networkEle);
					}
				}
			} finally {
				counter += 1;
				if ( !success ) {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        log.error("Failed sleeping during reconnect", e);
                    }
                }
			}
		}
		return success;
	}

	public void checkConnection() {
		connectionandNEExecutor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					currentstate.checkConnection();
				} catch (UnknownHostException e) {
					log.error("Failed", e);
				}
			}
		});
	}

	public void doDiscovery() {
		connectionandNEExecutor.execute(new Runnable() {
			@Override
			public void run() {
				currentstate.doDiscovery();
			}
		});
	}

	public void doInvSync() {
		if ( this.inventorySyncInProgress.get() ) {
			log.info("{}Node {} inventory sync in progress ...", tracePrefix, this.networkelement);
			return;
		}
		if (!this.inventorySyncInProgress.get()) {
			this.inventorySyncInProgress.set(true);
		}
		discoverEndPointExecutor.execute(new Runnable() {
			@Override
			public void run() {
				currentstate.doInventorySync();
			}
		});
	}

	public void doAlarmSync() {
		discoverAlarmExecutor.execute(new Runnable() {
			@Override
			public void run() {
				currentstate.doAlarmSync();
			}
		});
	}

	public void doNoOperation() {
		currentstate.doNoOperation();
	}

	public void doCleanUp() {
		currentstate.doCleanUp();
	}

	public void setInventorySyncInProgress() {
		this.inventorySyncInProgress.set(true);
	}

	public void resetInventorySyncInProgress() {
		this.inventorySyncInProgress.set(false);
	}

}
