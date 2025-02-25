package ai.netoai.collector.snmp;

import ai.netoai.collector.snmp.trap.SnmpTrap;
import org.snmp4j.CommandResponderEvent;

public class WrappedCREvent {
	
	private CommandResponderEvent event;
	private long receivedTime;
	private SnmpTrap trap;
	
	public WrappedCREvent(CommandResponderEvent event, long time) {
		this.event = event;
		this.receivedTime = time;
	}

	public WrappedCREvent(SnmpTrap trap, long time) {
		this.trap = trap;
		this.receivedTime = time;
	}

	public CommandResponderEvent getEvent() {
		return event;
	}

	public long getReceivedTime() {
		return receivedTime;
	}

	public SnmpTrap getTrap() {
		return trap;
	}
}
