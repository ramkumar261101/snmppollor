package ai.netoai.collector.snmp.mapping;

import java.io.IOException;
import java.nio.channels.Selector;

import org.snmp4j.TransportMapping;
import org.snmp4j.smi.UdpAddress;

public interface MuxTransportMapping extends TransportMapping<UdpAddress> {
	
	public void addListenAddress( UdpAddress udpAddress ) throws IOException;
    public TransportMapping getTransportMapping( UdpAddress udpAddress );
    public Selector getSelector();
    public void lockSelector();
    public void unlockSelector();
    public void setProcessingParameters( int transportListenerPoolSize, int transportProcessorPoolSize );
	
}
