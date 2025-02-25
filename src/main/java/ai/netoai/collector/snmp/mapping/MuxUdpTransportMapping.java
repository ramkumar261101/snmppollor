package ai.netoai.collector.snmp.mapping;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.snmp4j.SNMP4JSettings;
import org.snmp4j.TransportMapping;
import org.snmp4j.TransportStateReference;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.UdpTransportMapping;
import org.snmp4j.util.WorkerTask;

/**
 * Improvement over the default implementation of UdpTransportMapping by snmp4j.
 */
public class MuxUdpTransportMapping extends UdpTransportMapping implements MuxTransportMapping {

	private static final Logger logger = org.slf4j.LoggerFactory.getLogger(MuxUdpTransportMapping.class);

	private List<UdpAddress> addresses = Collections.synchronizedList(new ArrayList<UdpAddress>());
	private Map<UdpAddress, ExtendedUdpTransportMapping> transports = new ConcurrentHashMap<UdpAddress, ExtendedUdpTransportMapping>();
	protected Selector selector = Selector.open();
	protected WorkerTask listener;
	protected ListenThread listenerThread;
	private int socketTimeout = 1000;
	private int receiveBufferSize = 0; // not set by default
	// lock used to intercept the select process for registration of new nodes
	// to happen.
	// required when new nodes are added to the simulation.
	private ReentrantLock selectRegisterLock = new ReentrantLock();
	private TransportMapping baseTransport = null;
	// Seen this queue becoming full in columba
	private static final int INCOMING_QUEUE_CAPACITY = 5000000;
	private LinkedBlockingQueue<QueuedUdpMessage> incomingQueue = new LinkedBlockingQueue<QueuedUdpMessage>(
			INCOMING_QUEUE_CAPACITY);
	private IncomingQueueProcessor incomingQueueProcessor;
	private Integer transportListenerPoolSize = 20;
	private Integer transportProcessorPoolSize = 20;

	/**
	 * Creates a UDP transport with an arbitrary local port on all local
	 * interfaces.
	 *
	 * @throws IOException
	 *             if socket binding fails.
	 */
	public MuxUdpTransportMapping() throws IOException {
		super(new UdpAddress(InetAddress.getLocalHost(), 0));
		setupSelector();
		addAddress(udpAddress, false, true);
	}

	/**
	 * Creates a UDP transport with optional reusing the address if is currently
	 * in timeout state (TIME_WAIT) after the connection is closed.
	 *
	 * @param udpAddress
	 *            the local address for sending and receiving of UDP messages.
	 * @param reuseAddress
	 *            if <code>true</code> addresses are reused which provides
	 *            faster socket binding if an application is restarted for
	 *            instance.
	 * @throws IOException
	 *             if socket binding fails.
	 * @since 1.7.3
	 */
	public MuxUdpTransportMapping(UdpAddress udpAddress, boolean reuseAddress) throws IOException {
		super(udpAddress);
		setupSelector();
		addAddress(udpAddress, reuseAddress, true);
	}

	/**
	 * Creates a UDP transport on the specified address. The address will not be
	 * reused if it is currently in timeout state (TIME_WAIT).
	 *
	 * @param udpAddress
	 *            the local address for sending and receiving of UDP messages.
	 * @throws IOException
	 *             if socket binding fails.
	 */
	public MuxUdpTransportMapping(UdpAddress udpAddress) throws IOException {
		super(udpAddress);
		setupSelector();
		addAddress(udpAddress, false, true);
		startStatsCounter();
	}

	public void setProcessingParameters(int transportListenerPoolSize, int transportProcessorPoolSize) {
		if (this.isListening())
			throw new IllegalStateException("MultiplexedUdpTransportMapping: Cannot set the pool sizes when listening");

		this.transportListenerPoolSize = transportListenerPoolSize;
		this.transportProcessorPoolSize = transportProcessorPoolSize;
	}

	private long keysLongestTimeExe = -1;

	private void startStatsCounter() {
		Timer timer = new Timer("MuxUDPTransportMapping Stats Counter");
		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				if (logger.isInfoEnabled()) {
					StringBuilder sb = new StringBuilder();

					sb.append("** MuxUDPTransportMapping - Listener Thread Stats **").append("\n");
					sb.append("** Longest Keys Execution time: ").append(keysLongestTimeExe).append(" ms").append("\n");
					sb.append("** Total Keys processed: ").append(totalKeysProcessed.getAndSet(0)).append("\n");
					sb.append("** Max Keys processed: ").append(maxKeysProcessed).append("\n");
					sb.append("** Total Reads: ").append(totalReads.getAndSet(0)).append("\n");
					sb.append("** Total Empty reads: ").append(emptyReads.getAndSet(0)).append("\n");
					sb.append("** Total Rejected messages: ").append(totalRejectedMessages.getAndSet(0)).append("\n");
					sb.append("** Multi Read count: ").append(multiReadCount.getAndSet(0)).append("\n");
					sb.append("** Total Packets read: ").append(totalPacketsRead.getAndSet(0)).append("\n");
					sb.append("** Total Time to read packets: ").append(totalTimeToReadPackets.getAndSet(0))
							.append(" ms").append(" \n");
					sb.append("** Total Requests Sent: ").append(totalRequestsSent.getAndSet(0)).append("\n");
					sb.append("** Total Requests Fired: ").append(totalRequestsFired.getAndSet(0)).append("\n");
					sb.append("** Max Packet process Rate per ms: ").append(maxProcessRate).append("\n");
					sb.append("** Min Packet process Rate per ms: ").append(minProcessRate).append("\n");

					if (isListening()) {
						sb.append("** Pool sizes: Listener: ").append(listenerThread.getPoolSize())
								.append(", IncomingQueueProc: ").append(incomingQueueProcessor.getPoolSize())
								.append("\n");
						sb.append("** Incoming Queue size: ").append(incomingQueue.size()).append("\n");
						sb.append("** Max Packet Read Loop Time: ").append(maxPacketReadLoopTime).append(" ms")
								.append("\n");
						sb.append("** Packets queued longer than [1,3,15]s: ").append("[ ")
								.append(highQueuedCountThres1).append(", ").append(highQueuedCountThres2).append(", ")
								.append(highQueuedCountThres3).append(" ]").append("\n");
						sb.append("** Max Packet Delay in Queue: ").append(maxQueuedTime).append(" ms").append("\n");
						sb.append("** Listener Spin Wait: ").append(listenerSpinWait.getAndSet(0)).append(" ms")
								.append("\n");
						sb.append("** Incoming Proc Spin Wait: ").append(inProcSpinWait.getAndSet(0)).append(" ms")
								.append("\n");
					}

					logger.info(sb.toString());
				}
				keysLongestTimeExe = -1;
				totalKeysProcessed.set(0);
				totalReads.set(0);
				emptyReads.set(0);
				totalRejectedMessages.set(0);
				multiReadCount.set(0);
				totalPacketsRead.set(0);
				totalRequestsSent.set(0);
				totalRequestsFired.set(0);
				maxKeysProcessed = 0;
				maxTimeToProcessKeys = 0;
				maxProcessRate = 0.0;
				minProcessRate = Double.MAX_VALUE;
				maxQueuedTime = -1;
				highQueuedCountThres1.set(0);
				highQueuedCountThres2.set(0);
				highQueuedCountThres3.set(0);
				maxPacketReadLoopTime = -1;
				listenerSpinWait.set(0);
				inProcSpinWait.set(0);
			}
		}, calculateOffset(60*1000l), 60 * 1000l);
	}
	
	private long calculateOffset(long interval) {
		return interval - (System.currentTimeMillis() % interval);
	}

	private void setupSelector() throws IOException {
		selector = Selector.open();
	}

	public Selector getSelector() {
		return selector;
	}

	public void lockSelector() {
		this.selectRegisterLock.lock();
	}

	public void unlockSelector() {
		this.selectRegisterLock.unlock();
	}

	private void addAddress(UdpAddress udpAddress) throws IOException {
		addAddress(udpAddress, false, false);
	}

	private void addAddress(UdpAddress udpAddress, boolean reuseAddress) throws IOException {
		addAddress(udpAddress, reuseAddress, false);
	}

	private void addAddress(UdpAddress udpAddress, boolean reuseAddress, boolean setBaseMapping) throws IOException {
		ExtendedUdpTransportMapping udpMapping = new ExtendedUdpTransportMapping(udpAddress, this, reuseAddress);
		transports.put(udpAddress, udpMapping);
		addresses.add(udpAddress);

		if (setBaseMapping)
			this.baseTransport = udpMapping;
	}

	public TransportMapping getTransportMapping(UdpAddress udpAddress) {
		if (transports.containsKey(udpAddress))
			return transports.get(udpAddress);

		return null;
	}

	public void addListenAddress(UdpAddress udpAddress) throws IOException {
		addAddress(udpAddress);
	}

	/***
	 * Modify this method to send the corresponding transport mapping with the
	 * correct listenAddress. For the most part this is a dummy
	 * TransportMapping, More a hack in lieu of a better solution.
	 * 
         * @param listenAddress the address to be listened on for incoming requests
	 * @param address the address to be used for sending the messages
	 * @param buf data holder
         * @param stateRef just implementing the stuff in parent class
	 */
	protected void fireProcessMessage(Address listenAddress, Address address, ByteBuffer buf,
			TransportStateReference stateRef) {
		/**
		 * Deviation from the AbstractTransportMapping implementation - BEGIN
		 **/
		if (listenAddress.toString().contains("0:0:0:0:0:0:0:0")) {
			listenAddress.setValue(listenAddress.toString().replace("0:0:0:0:0:0:0:0", "0.0.0.0"));
		}
		if (this.transports.containsKey(listenAddress)) {
			ExtendedUdpTransportMapping transport = this.transports.get(listenAddress);

			if (transport == null && (listenAddress.toString().contains("0:0:0:0:0:0:0:0")
					|| listenAddress.toString().contains("0.0.0.0"))) {
				if (this.transports.size() > 0) {
					transport = this.transports.entrySet().iterator().next().getValue();
				}
			}

			if (logger.isTraceEnabled())
				logger.trace("Firing message to " + transport + " for address " + listenAddress);

			// Fire the message on the child.
			transport.fireProcessMessage((UdpAddress) address, buf, stateRef);
			totalRequestsFired.incrementAndGet();
		} else {
			logger.warn("Could not fire message for address " + listenAddress + ":" + address);
		}

		/** Deviation from the AbstractTransportMapping implementation - END **/
	}

	public void sendMessage(UdpAddress targetAddress, byte[] message, TransportStateReference ref)
			throws java.io.IOException {
		this.baseTransport.sendMessage(targetAddress, message, ref);
	}

	/**
	 * Closes the socket and stops the listener thread.
	 *
	 * @throws IOException in case the socket is not closed
	 */
        @Override
	public void close() throws IOException {
		WorkerTask l = listener;
		if (l != null) {
			l.terminate();
			l.interrupt();
			if (socketTimeout > 0) {
				try {
					l.join();
				} catch (InterruptedException ex) {
					logger.error("Failed closing the Mux transport mapping", ex);
				}
			}
			listener = null;
		}

		if (this.incomingQueueProcessor != null)
			this.incomingQueueProcessor.terminate();

		if (this.incomingQueue != null)
			this.incomingQueue.clear();

		this.incomingQueueProcessor = null;

		for (ExtendedUdpTransportMapping transport : transports.values()) {
			transport.close();
		}

		addresses.clear();

		if (selector.isOpen()) {
			selector.close();
		}
	}

	/**
	 * Starts the listener thread that accepts incoming messages. The thread is
	 * started in daemon mode and thus it will not block application terminated.
	 * Nevertheless, the {@link #close()} method should be called to stop the
	 * listen thread gracefully and free associated ressources.
	 *
	 * @throws IOException in case the socket is not opened properly
	 */
        @Override
	public synchronized void listen() throws IOException {
		if (listener != null) {
			throw new SocketException("Port already listening");
		}
		listenerThread = new ListenThread(transportListenerPoolSize);
		listener = SNMP4JSettings.getThreadFactory().createWorkerThread("MultiplexedUdpTransportMapping",
				listenerThread, true);

		incomingQueueProcessor = new IncomingQueueProcessor(transportProcessorPoolSize);
		incomingQueueProcessor.start();
		listener.run();
	}

	/**
	 * Changes the priority of the listen thread for this UDP transport mapping.
	 * This method has no effect, if called before {@link #listen()} has been
	 * called for this transport mapping.
	 *
	 * @param newPriority
	 *            the new priority.
	 * @see Thread#setPriority
	 * @since 1.2.2
	 */
	public void setPriority(int newPriority) {
		WorkerTask lt = listener;
		if (lt instanceof Thread) {
			((Thread) lt).setPriority(newPriority);
		}
	}

	/**
	 * Returns the priority of the internal listen thread.
	 *
	 * @return a value between {@link Thread#MIN_PRIORITY} and
	 *         {@link Thread#MAX_PRIORITY}.
	 * @since 1.2.2
	 */
	public int getPriority() {
		WorkerTask lt = listener;
		if (lt instanceof Thread) {
			return ((Thread) lt).getPriority();
		} else {
			return Thread.NORM_PRIORITY;
		}
	}

	/**
	 * Sets the name of the listen thread for this UDP transport mapping. This
	 * method has no effect, if called before {@link #listen()} has been called
	 * for this transport mapping.
	 *
	 * @param name
	 *            the new thread name.
	 * @since 1.6
	 */
	public void setThreadName(String name) {
		WorkerTask lt = listener;
		if (lt instanceof Thread) {
			((Thread) lt).setName(name);
		}
	}

	/**
	 * Returns the name of the listen thread.
	 *
	 * @return the thread name if in listening mode, otherwise
	 *         <code>null</code>.
	 * @since 1.6
	 */
	public String getThreadName() {
		WorkerTask lt = listener;
		if (lt instanceof Thread) {
			return ((Thread) lt).getName();
		} else {
			return null;
		}
	}

	public void setMaxInboundMessageSize(int maxInboundMessageSize) {
		this.maxInboundMessageSize = maxInboundMessageSize;
	}

	public int getSocketTimeout() {
		return socketTimeout;
	}

	/**
	 * Gets the requested receive buffer size for the underlying UDP socket.
	 * This size might not reflect the actual size of the receive buffer, which
	 * is implementation specific.
	 *
	 * @return &lt;=0 if the default buffer size of the OS is used, or a value &gt; 0
	 *         if the user specified a buffer size.
	 */
	public int getReceiveBufferSize() {
		return receiveBufferSize;
	}

	/**
	 * Sets the receive buffer size, which should be &gt; the maximum inbound
	 * message size. This method has to be called before {@link #listen()} to be
	 * effective.
	 *
	 * @param receiveBufferSize
	 *            an integer value &gt; 0 and &gt; {@link #getMaxInboundMessageSize()}.
	 */
	public void setReceiveBufferSize(int receiveBufferSize) {
		if (receiveBufferSize <= 0) {
			throw new IllegalArgumentException("Receive buffer size must be > 0");
		}
		this.receiveBufferSize = receiveBufferSize;
	}

	public void setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
	}

	public boolean isListening() {
		return (listener != null);
	}

	private class QueuedUdpMessage {
		private InetAddress localAddress;
		private int localPort;
		private InetAddress peerAddress;
		private int peerPort;
		private ByteBuffer data;
		private boolean killPill = false;
		private long requestTimeStamp = -1;

		public QueuedUdpMessage(InetAddress localAddress, int localPort, InetAddress peerAddress, int peerPort,
				ByteBuffer data, long requestTimeStamp) {
			this.localAddress = localAddress;
			this.localPort = localPort;
			this.peerAddress = peerAddress;
			this.peerPort = peerPort;
			this.data = data;
			this.requestTimeStamp = requestTimeStamp;
		}

		public QueuedUdpMessage(boolean killPill) {
			this.localAddress = null;
			this.localPort = -1;
			this.peerAddress = null;
			this.peerPort = 1;
			this.data = null;
			this.killPill = true;
		}

		public InetAddress getLocalAddress() {
			return localAddress;
		}

		public void setLocalAddress(InetAddress localAddress) {
			this.localAddress = localAddress;
		}

		public int getLocalPort() {
			return localPort;
		}

		public void setLocalPort(int localPort) {
			this.localPort = localPort;
		}

		public InetAddress getPeerAddress() {
			return peerAddress;
		}

		public void setPeerAddress(InetAddress peerAddress) {
			this.peerAddress = peerAddress;
		}

		public int getPeerPort() {
			return peerPort;
		}

		public void setPeerPort(int peerPort) {
			this.peerPort = peerPort;
		}

		public ByteBuffer getData() {
			return data;
		}

		public void setData(ByteBuffer data) {
			this.data = data;
		}

		public long getRequestTimeStamp() {
			return requestTimeStamp;
		}

		public void setRequestTimeStamp(long requestTimeStamp) {
			this.requestTimeStamp = requestTimeStamp;
		}

		@Override
		public String toString() {
			return "QueuedUdpMessage{" + "localAddress=" + localAddress + ", localPort=" + localPort + ", peerAddress="
					+ peerAddress + ", peerPort=" + peerPort + ", data=" + data + ", killPill=" + killPill
					+ ", requestTimeStamp=" + new Date(requestTimeStamp) + '}';
		}

	}

	private class IncomingQueueProcessor extends Thread {
		private volatile boolean alive = true;
		private static final int QUEUE_LOG_THRESHOLD = 999;
		private static final int PROCESS_LOG_THRESHOLD = 999;
		private ThreadPoolExecutor executor = null;
		private int transportProcessorPoolSize = 20;

		public IncomingQueueProcessor(int transportProcessorPoolSize) {
			this.transportProcessorPoolSize = transportProcessorPoolSize;
			this.executor = new ThreadPoolExecutor(1, transportProcessorPoolSize, 60, TimeUnit.SECONDS,
					new SynchronousQueue(), new ThreadPoolExecutor.AbortPolicy());

		}

		public int getPoolSize() {
			return ((executor == null) ? 0 : executor.getPoolSize());
		}

		public void terminate() {
			this.alive = false;
			boolean success = incomingQueue.offer(new QueuedUdpMessage(true));
			if ( !success ) {
				logger.warn("Failed terminating the: " + this.getClass().getSimpleName());
			}
		}

		@Override
		public void run() {
			logger.info("MultiplexedUdpTransportMapping: IncomingQueueProcessor: Thread starting with "
					+ transportProcessorPoolSize + " pool size.");
			QueuedUdpMessage message = null;
			int depth = 0;
			long iterCount = 0;
			long rejectedCount = 0;
			long millis = System.currentTimeMillis();

			while (alive) {
				try {
					message = incomingQueue.take();
					depth = incomingQueue.size();

					if (!alive) {
						return;
					}

					if (logger.isDebugEnabled())
						iterCount++;

					if (logger.isTraceEnabled()) {
						if (depth > QUEUE_LOG_THRESHOLD) {
							logger.trace(
									"MultiplexedUdpTransportMapping: IncomingQueueProcessor: Queue depth " + depth);
						}
					}

					long executionFailedTime = -1;
					while (message != null) {
						try {
							final QueuedUdpMessage thisMessage = message;

							// Process received UdpMessage
							this.executor.submit(new Runnable() {
								public void run() {
									try {
										TransportStateReference stateReference = new TransportStateReference(
												MuxUdpTransportMapping.this, udpAddress, null, SecurityLevel.undefined,
												SecurityLevel.undefined, false, this);
										fireProcessMessage(
												new UdpAddress(thisMessage.getLocalAddress(),
														thisMessage.getLocalPort()),
												new UdpAddress(thisMessage.getPeerAddress(), thisMessage.getPeerPort()),
												thisMessage.getData(), stateReference);

										long millis = System.currentTimeMillis() - thisMessage.getRequestTimeStamp();

										if (millis > 0) {
											if (millis > maxQueuedTime)
												maxQueuedTime = millis;

											if (millis > THRESHOLD_PROCESS_TIME_1)
												highQueuedCountThres1.incrementAndGet();

											if (millis > THRESHOLD_PROCESS_TIME_2)
												highQueuedCountThres2.incrementAndGet();

											if (millis > THRESHOLD_PROCESS_TIME_3)
												highQueuedCountThres3.incrementAndGet();
										}
									} catch (Exception ex) {
										logger.error(
												"MultiplexedUdpTransportMapping: IncomingQueueProcessor: Error processing UdpMessage Message : "
														+ thisMessage,
												ex);
									}
								}
							});

							if (executionFailedTime != -1) {
								long spinWait = System.currentTimeMillis() - executionFailedTime;
								inProcSpinWait.addAndGet(spinWait);
								executionFailedTime = -1;
							}
							message = null;
						} catch (RejectedExecutionException e) {
							if (executionFailedTime == -1)
								executionFailedTime = System.currentTimeMillis();

							if (logger.isDebugEnabled())
								rejectedCount++;
							// Probably should delay a bit here as we are too
							// busy
							try {
								Thread.sleep(1);
							} catch (Exception ex) {
								logger.error("Failed to Wait ", ex);
							}
						} catch (Exception e) {
							logger.error(
									"MultiplexedUdpTransportMapping: IncomingQueueProcessor: Error processing UdpMessage Message : "
											+ message,
									e);
						}
					}

					if (logger.isTraceEnabled()) {
						if (iterCount > PROCESS_LOG_THRESHOLD) {
							millis = System.currentTimeMillis() - millis;
							logger.trace("MuliplexedUdpTransportMapping: IncomingQueueProcessor: Processed " + iterCount
									+ " messages in " + millis + " ms with " + rejectedCount
									+ " rejected messages. Queue size " + incomingQueue.size() + ", Pool size "
									+ executor.getPoolSize() + ".");
							iterCount = 0;
							rejectedCount = 0;
							millis = System.currentTimeMillis();
						}
					}
				} catch (Exception x) {
					logger.error(
							"MultiplexedUdpTransportMapping: IncomingQueueProcessor: Unexpected exception while processing AsyncSnmpResponse Queue",
							x);
				}
			}

			synchronized (MuxUdpTransportMapping.this) {
				alive = false;
				if (executor != null)
					this.executor.shutdown();
				incomingQueue.clear();
				incomingQueueProcessor = null;
				executor = null;
			}
			logger.info("MultiplexedUdpTransportMapping: IncomingQueueProcessor: Thread exiting...");
		}
	}

	private final AtomicLong totalKeysProcessed = new AtomicLong();
	private int maxKeysProcessed = 0;
	private long maxTimeToProcessKeys = 0;
	private final AtomicLong totalReads = new AtomicLong();
	private final AtomicLong emptyReads = new AtomicLong();
	private final AtomicLong totalRejectedMessages = new AtomicLong();
	private final AtomicLong multiReadCount = new AtomicLong();
	private final AtomicLong totalPacketsRead = new AtomicLong();
	private final AtomicLong totalTimeToReadPackets = new AtomicLong();
	private final AtomicLong totalRequestsSent = new AtomicLong();
	private final AtomicLong totalRequestsFired = new AtomicLong();

	private volatile double maxProcessRate = 0.0;
	private volatile double minProcessRate = Double.MAX_VALUE;

	private volatile long maxPacketReadLoopTime = -1;
	private volatile long maxQueuedTime = -1;
	private final AtomicLong highQueuedCountThres1 = new AtomicLong();
	private final AtomicLong highQueuedCountThres2 = new AtomicLong();
	private final AtomicLong highQueuedCountThres3 = new AtomicLong();
	private final long THRESHOLD_PROCESS_TIME_1 = 1000l;
	private final long THRESHOLD_PROCESS_TIME_2 = 3000l;
	private final long THRESHOLD_PROCESS_TIME_3 = 15000l;
	private final AtomicLong listenerSpinWait = new AtomicLong();
	private final AtomicLong inProcSpinWait = new AtomicLong();

	void incRequestsSent() {
		totalRequestsSent.incrementAndGet();
	}

	class ListenThread implements WorkerTask {
		private volatile boolean stop = false;
		// private ByteBuffer buffer;

		private ThreadPoolExecutor executor = null;
		private int transportListenerPoolSize = 20;

		public ListenThread(int transportListenerPoolSize) throws SocketException {
			// buffer = ByteBuffer.allocate( getMaxInboundMessageSize() );
			this.transportListenerPoolSize = transportListenerPoolSize;

			// Fixing pool so 1 to 1 mapping between socket and listener. Its
			// expected there will be a reasonably small number
			// of sockets for the executor to listen on (i.e. not thousands).
			this.executor = new ThreadPoolExecutor(transportListenerPoolSize, transportListenerPoolSize, 0,
					TimeUnit.SECONDS, new SynchronousQueue(), new ThreadPoolExecutor.AbortPolicy());
		}

		public int getPoolSize() {
			return (this.executor.getPoolSize());
		}

		public void run() {
			if (logger.isInfoEnabled()) {
				logger.info("MuliplexedUdpTransportMapping: ListenerThread: Starting listener for address list "
						+ addresses + " Socket Timeout = " + getSocketTimeout() + " ms with "
						+ transportListenerPoolSize + " threads.");
			}

			long millis = System.currentTimeMillis();

			while (!stop) {

				try {
					// Try getting and releasing the lock so registration may
					// proceed.
					selectRegisterLock.lock();
					selectRegisterLock.unlock();

					int num = selector.selectNow();
					if (num == 0) {
						num = selector.select(getSocketTimeout());
					}

					if (num == 0) {
						emptyReads.incrementAndGet();
						continue;
					}
					totalKeysProcessed.addAndGet(num);

					long keyMillis = System.currentTimeMillis();

					Set selectedKeys = selector.selectedKeys();
					Iterator it = selectedKeys.iterator();
					SelectionKey key = null;

					if (logger.isTraceEnabled()) {
						logger.trace("MuliplexedUdpTransportMapping: ListenerThread: Keys from select: " + num
								+ ", Keys from" + " selectedKeys: " + selectedKeys.size());
					}

					boolean executionPassed = true;
					long executionFailedTime = -1;
					while (!executionPassed || it.hasNext()) {
						if (executionPassed) {
							key = (SelectionKey) it.next();
							totalReads.incrementAndGet();
						}

						if (key.isValid() && (key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
							// unregister interest in reads. The listner task
							// will reinterest when
							// socket has been fully read from
							key.interestOps(key.interestOps() & (~SelectionKey.OP_READ));

							// Read the data
							try {
								// stash the listener task (and resulting
								// buffer) on the selection key
								ListenerTask task = (ListenerTask) key.attachment();

								if (task == null) {
									task = new ListenerTask(key);
									key.attach(task);
								}

								executor.submit(task);
								if (executionFailedTime != -1) {
									long spinWait = System.currentTimeMillis() - executionFailedTime;
									listenerSpinWait.addAndGet(spinWait);
									executionFailedTime = -1;
								}
								executionPassed = true;
								it.remove();
							} catch (RejectedExecutionException ree) {
								if (executionFailedTime == -1)
									executionFailedTime = System.currentTimeMillis();

								executionPassed = false;
								totalRejectedMessages.incrementAndGet();
							}
						}
					}

					keyMillis = System.currentTimeMillis() - keyMillis;

					if (keyMillis > keysLongestTimeExe) {
						keysLongestTimeExe = keyMillis;
					}

					if (num > maxKeysProcessed) {
						maxKeysProcessed = num;
					}
					millis = System.currentTimeMillis() - millis;
					if (millis > maxTimeToProcessKeys) {
						maxTimeToProcessKeys = millis;
						millis = System.currentTimeMillis();
					}
				} catch (IOException iox) {
					logger.error("Failed submitting the task", iox);
					if (logger.isDebugEnabled()) {
						logger.error("Failed", iox);
					}
					if (SNMP4JSettings.isForwardRuntimeExceptions()) {
						throw new RuntimeException(iox);
					}
					logger.error("MuliplexedUdpTransportMapping: ListenerThread: IOException in ListenerTask: ", iox);
				} catch (Exception ex) {
					logger.error("Failed submitting the task", ex);
					if (logger.isDebugEnabled()) {
						logger.error("Failed", ex);
					}
					if (SNMP4JSettings.isForwardRuntimeExceptions()) {
						throw new RuntimeException(ex);
					}
					logger.error("MuliplexedUdpTransportMapping: ListenerThread: Unknown Exception in ListenerTask: ",
							ex);
				} finally {
					// fail-safe release the lock.
					if (selectRegisterLock.isHeldByCurrentThread())
						selectRegisterLock.unlock();
				}
			}

			synchronized (MuxUdpTransportMapping.this) {
				stop = true;

				if (executor != null)
					this.executor.shutdown();

				executor = null;
				listener = null;
				try {
					selector.close();
				} catch (IOException ex) {
				}
			}

			logger.info("MuliplexedUdpTransportMapping: ListenerThread: Exiting.");
		}

		public void close() {
			stop = true;
		}

		public void terminate() {
			close();
		}

		public void join() throws InterruptedException {
		}

		public void interrupt() {
		}
	}

	public class ListenerTask implements Runnable {
		private ByteBuffer buffer;
		private SelectionKey key = null;

		public ListenerTask(SelectionKey key) {
			this.buffer = ByteBuffer.allocate(getMaxInboundMessageSize());
			this.key = key;
		}

		@Override
		public void run() {
			try {
				DatagramChannel dgc = (DatagramChannel) key.channel();
				DatagramSocket ds = dgc.socket();

				// Read data
				SocketAddress peerSocketAddress = null;
				buffer.clear();
				int packetsRead = 0;
				long millis = System.currentTimeMillis();
				while ((peerSocketAddress = dgc.receive(buffer)) != null) {
					packetsRead++;

					buffer.flip();
					byte[] bytes = new byte[buffer.limit()];
					// ByteBuffer bis = buffer.get( bytes );

					byte[] buf = buffer.array();
					System.arraycopy(buf, 0, bytes, 0, bytes.length);
					ByteBuffer bis = ByteBuffer.wrap(bytes);

					buffer.clear();

					if (logger.isTraceEnabled()) {
						logger.trace("MuliplexedUdpTransportMapping: ListenerThread:ListenerTask: Address fields IA="
								+ ds.getInetAddress() + ";IAP=" + ds.getPort() + ";LA=" + ds.getLocalAddress() + ";LAP="
								+ ds.getLocalPort() + ";LSA=" + ds.getLocalSocketAddress() + ";RSA="
								+ ds.getRemoteSocketAddress());

						logger.trace(
								"MuliplexedUdpTransportMapping: ListenerThread:ListenerTask: Received message from "
										+ peerSocketAddress + " with length " + bytes.length + ": "
										+ new OctetString(bytes, 0, bytes.length).toHexString());
					}

					// Cast to InetSocketAddress to break down the SocketAddress
					// into Address & Port.
					InetSocketAddress peerSocketAddressI = (InetSocketAddress) peerSocketAddress;

					QueuedUdpMessage udpMessage = new QueuedUdpMessage(ds.getLocalAddress(), ds.getLocalPort(),
							peerSocketAddressI.getAddress(), peerSocketAddressI.getPort(), bis,
							System.currentTimeMillis());

					if (logger.isTraceEnabled()) {
						logger.trace(
								"MuliplexedUdpTransportMapping: ListenerThread:ListenerTask: Response received from Agent: "
										+ peerSocketAddressI.getAddress());
					}
					if (incomingQueue.offer(udpMessage) == false) {
						logger.warn(
								"MultiplexedUdpTransportMapping: ListenerThread:ListenerTask: Dropping incoming message as incomingQueue is full. Queue Depth = "
										+ incomingQueue.size());
					}
				}
				millis = System.currentTimeMillis() - millis;

				if (millis > 0) {
					if (millis > maxPacketReadLoopTime)
						maxPacketReadLoopTime = millis;

					double processRate = (double) packetsRead / millis;
					if (processRate > maxProcessRate) {
						maxProcessRate = processRate;
					}
				}
				if (packetsRead > 0) {
					double processRate = (double) packetsRead / millis;
					if (processRate < minProcessRate) {
						minProcessRate = processRate;
					}
				}
				totalPacketsRead.addAndGet(packetsRead);
				totalTimeToReadPackets.addAndGet(millis);

				if (packetsRead > 1) {
					multiReadCount.incrementAndGet();
				}
			} catch (java.nio.channels.ClosedChannelException x) {
				logger.info(
						"MuliplexedUdpTransportMapping: ListenerThread:ListenerTask: Channel was closed while reading, continuing",
						x);
			} catch (IOException iox) {
				logger.error(
						"MuliplexedUdpTransportMapping: ListenerThread:ListenerTask: IOException in ListenerTask: ",
						iox);
			} catch (Exception ex) {
				logger.error(
						"MuliplexedUdpTransportMapping: ListenerThread:ListenerTask: Unknown exception in ListenerTask: ",
						ex);
			} finally {
				// reregister interest in reads.
				key.interestOps(key.interestOps() | SelectionKey.OP_READ);
				// Wakeup the selector, so our new setting takes effect on the
				// next select (asap).
				Selector s = key.selector();
				s.wakeup();
			}
		}

	}

	public String toString() {
		return "MulttiplexedUdpTransportMapping[ " + this.addresses.size() + "]";
	}

}
