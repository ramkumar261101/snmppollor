package ai.netoai.collector.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.nio.file.*;
import java.nio.file.WatchEvent.Kind;
import java.util.*;

import static java.nio.file.StandardWatchEventKinds.*;


public class DirectoryWatcher extends Observable implements Serializable {
	private static final long serialVersionUID = -8801306782039004121L;
	private static final Logger log = LoggerFactory.getLogger(DirectoryWatcher.class);
	private static final String tracePrefix = "[" + DirectoryWatcher.class.getSimpleName() + "]: ";
	public static final String FILE_CREATE = "FILE_CREATE";
	public static final String FILE_MODIFY = "FILE_MODIFY";
	public static final String FILE_DELETE = "FILE_DELETE";
	
	private WatchService watchService;
	private Map<WatchKey, Path> keys;
	private boolean alive;
	private String location;
	private List<String> fileNameFilters;
	
	private DirectoryWatcher() {
		
	}
	
	/**
	 * Returns a new instance of this class.
	 * @param location to be watched
	 * @return returns this instance
	 * @throws IllegalArgumentException if the location specified does not exist.
	 */
	public static DirectoryWatcher newInstance(String location) throws IllegalArgumentException {
		if ( !Files.exists(Paths.get(location)) ) {
			throw new IllegalArgumentException("Location [" + location + "] does not exist");
		}
		DirectoryWatcher instance = new DirectoryWatcher();
		instance.setLocation(location);
		return instance;
	}
	
	/**
	 * Returns a new instance of this class.
	 * @param location to be watched
	 * @param filters which will be evaluated against the file names
	 * @return returns this instance
	 * @throws IllegalArgumentException if the location specified does not exist.
	 */
	public static DirectoryWatcher newInstance(String location, List<String> filters) throws IllegalArgumentException {
		if ( !Files.exists(Paths.get(location)) ) {
			throw new IllegalArgumentException("Location [" + location + "] does not exist");
		}
		DirectoryWatcher instance = new DirectoryWatcher();
		instance.setLocation(location);
		instance.setFilters(filters);
		return instance;
	}
	
	private void setLocation(String location) {
		this.location = location;
	}
	
	private void setFilters(List<String> filters) {
		if ( filters != null && !filters.isEmpty() ) {
			this.fileNameFilters = new ArrayList<>();
			this.fileNameFilters.addAll(filters);
		}
	}
	
	public void start() {
		initialize();
		
		if ( this.watchService == null || this.keys.isEmpty() ) {
			log.error(tracePrefix + "WatcherService not initialized properly, exiting ...");
			return;
		}
		log.info(tracePrefix + "Starting the scan of directory: " + location);
		Thread scannerThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while(alive) {
					try {
						WatchKey key = watchService.take();
						Path dir = keys.get(key);
						if ( dir == null ) {
							log.error(tracePrefix + "Unrecognized WatchKey !!" );
							continue;
						}
						for(WatchEvent<?> event : key.pollEvents() ) {
							WatchEvent<Path> pEvent = cast(event);
							Kind kind = pEvent.kind();
							if ( kind == OVERFLOW ) {
								log.error(tracePrefix + "Received overflow event ...");
								continue;
							}
							Path p = pEvent.context();
							Path child = dir.resolve(p);
							if ( fileNameFilters != null && !fileNameFilters.isEmpty() ) {
								boolean unwantedFile = false;
								for(String filter : fileNameFilters ) {
									log.info(tracePrefix + "Checking the filter: " + filter + " against file: " + child);
        							if ( child == null || !child.toString().endsWith(filter) ) {
        								log.info(tracePrefix + "Unknwon file: " + child);
        								unwantedFile = true;
        								break;
        							}
								}
								if ( unwantedFile ) {
									continue;
								}
							} else {
								//log.info(tracePrefix + "No file filters found ...");
							}
							boolean create = false;
							if ( kind == ENTRY_CREATE ) {
								//log.info(tracePrefix + "Created file: " + child);
								create = true;
								setChanged();
								notifyObservers(new FileChangeNotification(child.toString(), FILE_CREATE));
							} 
							
							// If a new file is touched or copied then both create and modify events are received.
							else if ( !create && kind == ENTRY_MODIFY ) {
								//log.info(tracePrefix + "Modified file: " + child);
								setChanged();
								notifyObservers(new FileChangeNotification(child.toString(), FILE_MODIFY));
							} else if ( kind == ENTRY_DELETE ) {
								//log.info(tracePrefix + "Deleted file: " + child);
								setChanged();
								notifyObservers(new FileChangeNotification(child.toString(), FILE_DELETE));
							} else {
								log.error(tracePrefix + "Unrecognized WatchEvent: " + kind);
							}
						}
						boolean valid = key.reset();
						if ( !valid ) {
							alive = false;
							log.info(tracePrefix + "Invalid reset on the WatchKey ...");
						}
					} catch (Throwable t) {
						log.error(tracePrefix + "Error in scanner thread", t);
					}
				}
				log.info(tracePrefix + "Scanner thread exiting ...");
			}
		});
		scannerThread.setName("DirectoryWatcher-"+location);
		scannerThread.setDaemon(false);
		scannerThread.start();
	}
	
	@SuppressWarnings("unchecked")
	static <T> WatchEvent<Path> cast(WatchEvent<?> event) {
	    return (WatchEvent<Path>)event;
	}
	
	private void initialize() {
		try {
			this.keys = new HashMap<WatchKey, Path>();
			this.watchService = FileSystems.getDefault().newWatchService();
			this.alive = true;
			register();
		} catch(Exception ex) {
			log.error(tracePrefix + "*** Failed watching the Directory: " + location, ex);
		}
	}
	
	private void register() {
		try {
			Path configPath = Paths.get(location);
			WatchKey key = configPath.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
			this.keys.put(key, configPath);
		} catch (Exception ex) {
			log.error(tracePrefix + "Failed registering SNMP config directory", ex);
		}
	}
	
	public class FileChangeNotification {
		private String name;
		private String type;

		public FileChangeNotification(String name, String type) {
			super();
			this.name = name;
			this.type = type;
		}
		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}
		/**
		 * @param name the name to set
		 */
		public void setName(String name) {
			this.name = name;
		}
		/**
		 * @return the type
		 */
		public String getType() {
			return type;
		}
		/**
		 * @param type the type to set
		 */
		public void setType(String type) {
			this.type = type;
		}
		
		
	}
}
