/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.netoai.collector.snmp.trap;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.CommunityTarget;
import org.snmp4j.MessageDispatcher;
import org.snmp4j.MessageDispatcherImpl;
import org.snmp4j.MessageException;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.mp.MPv1;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.StateReference;
import org.snmp4j.mp.StatusInformation;
import org.snmp4j.security.Priv3DES;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TcpAddress;
import org.snmp4j.smi.TransportIpAddress;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.AbstractTransportMapping;
import org.snmp4j.transport.DefaultTcpTransportMapping;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.MultiThreadedMessageDispatcher;
import org.snmp4j.util.ThreadPool;

public class TrapReciever implements CommandResponder {
    private static final Logger log = LoggerFactory.getLogger(TrapReciever.class);
    private static final String tracePrefix = "[" + TrapReciever.class.getSimpleName() + "] : ";
    
    @Override
    public void processPdu(CommandResponderEvent event) {
        log.info(tracePrefix + "Processing PDU");
        PDU pdu = event.getPDU();
        
        if(pdu != null) {
            int pduType = pdu.getType();
            if(pduType != PDU.TRAP && pduType != PDU.V1TRAP
                    && pduType != PDU.REPORT && pduType != PDU.RESPONSE) {
                pdu.setErrorIndex(0);
                pdu.setErrorStatus(0);
                pdu.setType(PDU.RESPONSE);
                
                StatusInformation statusInfo = new StatusInformation();
                StateReference stateRef = event.getStateReference();
                 try
                {
                  System.out.println(event.getPDU());
                  event.getMessageDispatcher().returnResponsePdu(event.getMessageProcessingModel(),
                  event.getSecurityModel(), event.getSecurityName(), event.getSecurityLevel(),
                  pdu, event.getMaxSizeResponsePDU(), stateRef, statusInfo);
                }
                catch (MessageException ex)
                {
                  log.error(tracePrefix + "Error while sending response: " + ex.getMessage());
                }
            }
            
        }
    }
    
    public synchronized void listen(TransportIpAddress address) throws IOException {
        AbstractTransportMapping transport = null;
        if(address instanceof TcpAddress) {
            transport = new DefaultTcpTransportMapping((TcpAddress) address);
        } else {
            transport = new DefaultUdpTransportMapping((UdpAddress) address);
        }
        
        ThreadPool threadPool = ThreadPool.create("DispatcherPool", 10);
        MessageDispatcher mDispatcher = new MultiThreadedMessageDispatcher(threadPool, new MessageDispatcherImpl());
        mDispatcher.addMessageProcessingModel(new MPv1());
        mDispatcher.addMessageProcessingModel(new MPv2c());
        
        SecurityProtocols.getInstance().addDefaultProtocols();
        SecurityProtocols.getInstance().addPrivacyProtocol(new Priv3DES());
        
        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString("public"));
        
        Snmp snmp = new Snmp(mDispatcher, transport);
        snmp.addCommandResponder(this);
        
        transport.listen();
        
        try {
            this.wait();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        
    }
    
    public static void main(String[] args) {
        TrapReciever snmp4jTrapReciever =  new TrapReciever();
        try {
            snmp4jTrapReciever.listen(new UdpAddress("localhost/1622"));
        } catch (IOException ex) {
            System.err.println("Error in listening for Trap");
            System.err.println("Error message : " + ex.getMessage());
        }
    }
    
}
