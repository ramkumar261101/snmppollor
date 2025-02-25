package ai.netoai.collector.snmp.mapping;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.TransportStateReference;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.UdpTransportMapping;

public class ExtendedUdpTransportMapping extends UdpTransportMapping {
	private static final Logger logger = LoggerFactory.getLogger(ExtendedUdpTransportMapping.class);
	private MuxUdpTransportMapping multiplexor = null;
	private DatagramChannel channel = null;
	private UdpAddress udpAddress = null;
	private boolean reuseAddress = false;
	//
	// public ExtendedUdpTransportMapping( UdpAddress ua )
	// {
	// super( ua );
	// }

	public ExtendedUdpTransportMapping(UdpAddress ua, MuxUdpTransportMapping multiplexor, boolean reuseAddress)
			throws IOException {
		super(ua);
		this.udpAddress = ua;
		this.reuseAddress = reuseAddress;
		this.multiplexor = multiplexor;
		bindAddress();
	}

	private void bindAddress(UdpAddress udpAddress) throws IOException {
		this.udpAddress = udpAddress;
		this.reuseAddress = false;
		bindAddress();
	}

	private void bindAddress() throws IOException {
		// ServerSocketChannel ssc = ServerSocketChannel.open();
		// ssc.configureBlocking( false );
		// ServerSocket ss = ssc.socket();
		// ss.setReuseAddress( reuseAddress );
		// InetSocketAddress address = new InetSocketAddress(
		// udpAddress.getInetAddress(), udpAddress.getPort() );
		// ss.bind( address );
		//
		// SelectionKey key = ssc.register( selector, SelectionKey.OP_ACCEPT );

		// if( logger.isDebugEnabled() )

		DatagramChannel dgc = DatagramChannel.open();
		dgc.configureBlocking(false);
		DatagramSocket dgs = dgc.socket();
		dgs.setReuseAddress(reuseAddress);
		int oldReceiveBufferSize = dgs.getReceiveBufferSize();
		// dgs.setReceiveBufferSize( oldReceiveBufferSize * 2 );
		InetSocketAddress address = new InetSocketAddress(udpAddress.getInetAddress(), udpAddress.getPort());
		logger.info("Binding on address " + udpAddress + ". Old BufferSize=" + oldReceiveBufferSize + ", new="
				+ dgs.getReceiveBufferSize());
		dgs.bind(address);

		// Try getting the lock so select loop is intercepted before the next
		// select and registration may proceed.
		this.multiplexor.lockSelector();

		try {
			this.multiplexor.getSelector().wakeup();

			SelectionKey readKey = dgc.register(this.multiplexor.getSelector(), SelectionKey.OP_READ);

			if (logger.isTraceEnabled())
				logger.trace("Going to listen on " + udpAddress + " Key = " + readKey);
		} finally {
			this.multiplexor.unlockSelector();
		}

		this.channel = dgc;
	}

	@Override
	public void listen() throws IOException {
		if (!multiplexor.isListening())
			multiplexor.listen();
	}

	@Override
	public void close() throws IOException {
		synchronized (this) {
			if (this.channel != null)
				this.channel.close();
		}
	}

	@Override
	public void sendMessage(UdpAddress targetAddress, byte[] message, TransportStateReference ref) throws IOException {
		// multiplexor.sendMessage( adrs, bytes );

		UdpAddress udpAddress = null;

		if (UdpAddress.class.isAssignableFrom(targetAddress.getClass())) {
			udpAddress = (UdpAddress) targetAddress;
		}

		if (udpAddress == null) {
			logger.error("Non UDP addresses not supported at the moment.");
			return;
		}

		InetSocketAddress targetSocketAddress = new InetSocketAddress(udpAddress.getInetAddress(),
				udpAddress.getPort());

		if (logger.isTraceEnabled()) {
			logger.trace("Sending message to " + targetAddress + " with length " + message.length + ": "
					+ new OctetString(message).toHexString());
		}

		ByteBuffer outBuffer = ByteBuffer.wrap(message);

		synchronized (this) {
			if (!channel.isOpen()) {
				logger.info("Channel not open, InetAddress: " + udpAddress + " recreating channel");
				// try
				// {
				// if( channel.socket() != null && channel.socket().isBound() )
				// {
				// logger.info( "Socked still bound, InetAddress: " + udpAddress
				// + " releasing socket" );
				// channel.socket().close();
				// }
				// }
				// catch( Exception e )
				// {
				// logger.error( "Exception while closing socket ", e );
				// }
				bindAddress();
			}
			channel.send(outBuffer, targetSocketAddress);
		}

		this.multiplexor.incRequestsSent();
	}

	public boolean isListening() {
		return multiplexor.isListening();
	}

	public String toString() {
		return "ExtendedUdpTransportMapping[ " + this.udpAddress + "]";
	}

	public void fireProcessMessage(UdpAddress address, ByteBuffer buf, TransportStateReference ref) {
		super.fireProcessMessage(address, buf, ref);
	}
}