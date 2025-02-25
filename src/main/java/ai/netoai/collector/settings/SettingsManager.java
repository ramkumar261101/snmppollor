package ai.netoai.collector.settings;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;


public class SettingsManager {
	private static final Logger log = LoggerFactory
			.getLogger(SettingsManager.class);
	private static final String tracePrefix = "["
			+ SettingsManager.class.getSimpleName() + "]: ";
	
	private static SettingsManager instance;
	private Map<String, Object> settingsMap = new HashMap<>();
	
	private SettingsManager() {
		this.loadAllSettings();
	}
	
	private void loadAllSettings() {
		Properties systemProps = System.getProperties();
		log.info(tracePrefix + "All System Properties: " + systemProps);
		loadPropertiesFile(System.getProperty("collectorSettings"));
		logAllSettings();
	}

	private void loadPropertiesFile(String propsFile) {
		log.info(tracePrefix + "Loading properties file: " + propsFile);
		File file = new File(propsFile);
		Properties props = new Properties();
		try {
			props.load(new FileReader(file));
			Map<String, Object> propsMap = new HashMap<>();
			for(Object key : props.keySet()) {
				propsMap.put(key.toString(), props.get(key));
				
				if ( key.toString().equals("configDbUrl") ) {
					System.setProperty("configdb.url", props.get(key).toString());
				} else if ( key.toString().equals("configDbUsername") ) {
					System.setProperty("configdb.username", props.get(key).toString());
				} else if ( key.toString().equals("configDbPassword") ) {
					System.setProperty("configdb.password", props.get(key).toString());
				}
			}
			this.settingsMap.putAll(propsMap);
		} catch (FileNotFoundException e) {
			log.error(tracePrefix + "File " + propsFile + " does not exists", e);
		} catch (IOException e) {
			log.error(tracePrefix + "Failed reading properties file: " + propsFile, e);
		}
	}

	/**
	 * 
	 */
	private void logAllSettings() {
		StringBuilder sb = new StringBuilder();
		sb.append('\n');
		settingsMap.forEach((key, value) -> sb.append(key).append(" = ")
				.append(value).append(" [").append(value.getClass())
				.append("]").append('\n'));
		log.info(sb.toString());
	}

	public static synchronized SettingsManager getInstance() {
		if ( instance == null ) {
			instance = new SettingsManager();
		}
		return instance;
	}
	
	public Object getSetting(String settingName) {
		Object value = this.settingsMap.get(settingName);
		if ( value == null ) {
			throw new IllegalStateException("Setting " + settingName + " not found");
		}
		return value;
	}

	public Map<String, Object> getSettings() {
		// Create new settings map from the current settings map
        return new HashMap<>(this.settingsMap);
	}
}
