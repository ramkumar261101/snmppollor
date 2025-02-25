package ai.netoai.collector.deviceprofile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SnmpConfig {

	public SnmpConfig(){
		
	}
	
	private String id;
	private String vendor;
	private String product;
	private String version;
	private boolean pollInventory;
	private boolean pollPerformance;
	private boolean collectFaults;
        private boolean discoverPhsyicalLinks;
        private Map<String, List<String>> metricKeyMap;

	private List<Config> childConfigs = new ArrayList<Config>();

        public Map<String, List<String>> getMetricKeyMap() {
            return metricKeyMap;
        }

        public void setMetricKeyMap(Map<String, List<String>> metricKeyMap) {
            this.metricKeyMap = metricKeyMap;
        }
	
        
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}
	/**
	 * @return the vendor
	 */
	public String getVendor() {
		return vendor;
	}
	/**
	 * @param vendor the vendor to set
	 */
	public void setVendor(String vendor) {
		this.vendor = vendor;
	}
	/**
	 * @return the product
	 */
	public String getProduct() {
		return product;
	}
	/**
	 * @param product the product to set
	 */
	public void setProduct(String product) {
		this.product = product;
	}
	/**
	 * @return the pollInventory
	 */
	public boolean isPollInventory() {
		return pollInventory;
	}
	/**
	 * @param pollInventory the pollInventory to set
	 */
	public void setPollInventory(boolean pollInventory) {
		this.pollInventory = pollInventory;
	}
	/**
	 * @return the pollPerformance
	 */
	public boolean isPollPerformance() {
		return pollPerformance;
	}
	/**
	 * @param pollPerformance the pollPerformance to set
	 */
	public void setPollPerformance(boolean pollPerformance) {
		this.pollPerformance = pollPerformance;
	}
	/**
	 * @return the collectFaults
	 */
	public boolean isCollectFaults() {
		return collectFaults;
	}
	/**
	 * @param collectFaults the collectFaults to set
	 */
	public void setCollectFaults(boolean collectFaults) {
		this.collectFaults = collectFaults;
	}
	
        public boolean isDiscoverPhsyicalLinks() {
            return discoverPhsyicalLinks;
        }

        public void setDiscoverPhsyicalLinks(boolean discoverPhsyicalLinks) {
            this.discoverPhsyicalLinks = discoverPhsyicalLinks;
        }
        
	/**
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}
	/**
	 * @param version the version to set
	 */
	public void setVersion(String version) {
		this.version = version;
	}
	
	/**
	 * @return the childConfigs
	 */
	public List<Config> getChildConfigs() {
		return childConfigs;
	}
	/**
	 * @param childConfigs the childConfigs to set
	 */
	public void setChildConfigs(List<Config> childConfigs) {
		this.childConfigs = childConfigs;
	}
        
        public List<Metric> getMetricsByFamliyId(String familyId) {
            List<Config> configs = getChildConfig(PmConfig.class);
            for(Config config : configs) {
                PmConfig pmConf = (PmConfig) config;
                 for(MetricFamily mf : pmConf.getMetricFamilies()) {
                     if(mf.getId().equals(familyId)) {
                         return mf.getMetrics();
                     }
                 }
            }
            return new ArrayList<>();
        }
        
        public List<Metric> getAllMetrics() {
            List<Metric> metrics = new ArrayList<>();
            List<Config> configs = getChildConfig(PmConfig.class);
            for(Config config : configs) {
                PmConfig pmConf = (PmConfig) config;
                 for(MetricFamily mf : pmConf.getMetricFamilies()) {
                     metrics.addAll(mf.getMetrics());
                 }
            }
            return metrics;
        }
        
        public List<Config> getChildConfig(Class clazz) {
            List<Config> configs = new ArrayList<>();
            for(Config config : getChildConfigs()) {
                if(config.getClass().equals(clazz)) {
                    configs.add(config);
                }
            }
            return configs;
        }
	
        public List<TrapConfig> getTrapConfigs() {
            List<TrapConfig> trapConfigs  = null;
            for(Config config : getChildConfigs()) {
                if(config instanceof TrapConfig) {
                    if(trapConfigs == null) trapConfigs = new ArrayList<>();
                    trapConfigs.add((TrapConfig) config);
                }
            }
            return trapConfigs;
        }
	

}