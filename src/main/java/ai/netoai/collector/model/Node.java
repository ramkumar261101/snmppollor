package ai.netoai.collector.model;

import ai.netoai.collector.utils.JsonGenerator;
import ai.netoai.collector.utils.JsonGenerator.ByteArrayToBase64TypeAdapter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.*;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.BeanUtilsBean2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.*;


public class Node extends GenericJavaBean {
	
	private static final Logger log = LoggerFactory.getLogger(Node.class);
    private static final String tracePrefix = "[" + Node.class.getSimpleName() + "]: ";

    @JsonIgnore
    protected static Gson gson = new GsonBuilder().serializeNulls().create();

    @JsonIgnore
    public static List<String> getChildren() {
        return Lists.newArrayList(
                NetworkElement.class.getSimpleName()
        );
    }
    
    private static BiMap<Class<? extends Node>, String> displayNames;

    public enum NodeType {
        NETWORK_ELEMENT;
    }

    public enum DiscoverySource {
		SNMP_DISCOVERY,
        OTHER;
    }
    
    static {
    	displayNames = HashBiMap.create();
    	displayNames.put(NetworkElement.class, "Network Element");
    }

    private String id;    
    private String name;
    private NodeType nodeType;
    private Date lastContacted;
    private String group; // A node may belong to a group which is assigned during discovery.
    private boolean status;
    private DiscoverySource discoverySource;
	private String location;
	private String latitude;
	private String longitude;
    
    public Node() {
    	
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Searchable(alias = "Name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Searchable(alias = "Type")
    @Id
    public NodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    public Date getLastContacted() {
        return lastContacted;
    }

    public void setLastContacted(Date lastContacted) {
        this.lastContacted = lastContacted;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @Searchable(alias = "Status")
    public boolean getStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    @Searchable(alias = "Discovery Source")
    public DiscoverySource getDiscoverySource() {
        return discoverySource;
    }

    public void setDiscoverySource(DiscoverySource discoverySource) {
        this.discoverySource = discoverySource;
    }

    @JsonIgnore
    public String toJson() {
        String neJson = JsonGenerator.getJSONString(this, true);
        JsonObject jsonObject = gson.fromJson(neJson, JsonObject.class);
        jsonObject.addProperty("@class", this.getClass().getSimpleName());
        jsonObject.remove("nonIdentifyingAttribs");
        jsonObject.remove("identifyingAttribs");
        jsonObject.remove("rid");
        neJson = jsonObject.toString();
        return neJson;
    }

    @JsonIgnore
    public String toJsonForOrient() {
        String neJson = JsonGenerator.getJSONString(this, true);
        JsonObject jsonObject = gson.fromJson(neJson, JsonObject.class);
        jsonObject.addProperty("@class", this.getClass().getSimpleName());
        jsonObject.remove("agentBased");
        jsonObject.remove("agentRunning");
        neJson = jsonObject.toString();
        return neJson;
    }

	public Map<String, String> getIdentifyingAttribs() {
		Map<String, String> idVals = new HashMap<>();
		Map<String, String> ids = BeanInfoCache.identifyingAttribs(this.getClass());
		BeanUtilsBean utils = BeanUtilsBean2.getInstance();
		ids.forEach((property, alias) -> {
			try {
				String val = utils.getProperty(this, property);
				idVals.put(property, val);
			} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
				log.error(tracePrefix + "Couldn't access property " + property + " for class "
						+ this.getClass().getCanonicalName(), e);
			}
		});
		return idVals;
	}
	
	@Transient
	public Map<String, String> getNonIdentifyingAttribs() {
		Map<String, String> others = new HashMap<>();
		Map<String, String> ids = BeanInfoCache.identifyingAttribs(this.getClass());
		Map<String, String> all = BeanInfoCache.searchableFieldAliases(this.getClass());
		BeanUtilsBean utils = BeanUtilsBean2.getInstance();
		all.forEach((property, alias) -> {
			try {
				if (!ids.containsKey(property)) {
					String val = utils.getProperty(this, property);
					others.put(property, val);
				}				
			} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
				log.error(tracePrefix + "Couldn't access property " + property + " for class "
						+ this.getClass().getCanonicalName(), e);
			}
		});
		return others;
	}

	/**
	 * @return the location
	 */
	@Searchable(alias = "Location")
	public String getLocation() {
		return location;
	}

	/**
	 * @param location
	 *            the location to set
	 */
	public void setLocation(String location) {
		this.location = location;
	}

	public String getLatitude() {
		return latitude;
	}

	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}

	public String getLongitude() {
		return longitude;
	}

	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}

	public static String prettyName(Class<? extends Node> type) {
		return displayNames.get(type);
	}
	
	public static Class<? extends Node> nameTotype(String prettyName) {
		return displayNames.inverse().get(prettyName);
	}
	
	public static Map<String, Class<? extends Node>> subTypes(){
		return displayNames.inverse();
	}
	
	/**
	 * Gson JsonDeserializer for deserialising subclasses of Node
	 *
	 */
	public static class NodeDeserializer implements JsonDeserializer<Node> {
		private Gson gson = new GsonBuilder().setPrettyPrinting().setDateFormat("yyyy-MM-dd HH:mm:ss")
	            .registerTypeHierarchyAdapter(byte[].class, new ByteArrayToBase64TypeAdapter()).create();
		@Override
		public Node deserialize(JsonElement json, Type arg1, JsonDeserializationContext arg2)
				throws JsonParseException {
			JsonObject jsonObject = json.getAsJsonObject();
			String nodeType = jsonObject.get("nodeType").getAsString();
			NodeType type = NodeType.valueOf(nodeType);
			switch (type) {
			case NETWORK_ELEMENT:
				return gson.fromJson(json, NetworkElement.class);
			default:
				return gson.fromJson(json, Node.class);
			}
		}	    
	}
	
	public static NodeType nodeTypeFor(String className) {
		if (className.equalsIgnoreCase(NetworkElement.class.getSimpleName())) {
			return NodeType.NETWORK_ELEMENT;
		}else{
		    return null;
		}
	}
	
	public static Class<? extends Node> nodeClassFor(String className) {
		if (className.equalsIgnoreCase(NetworkElement.class.getSimpleName())) {
			return NetworkElement.class;
		}else {
			return Node.class;
		}		
	}
	
	@SuppressWarnings("unchecked")
	public static Set<Class<? extends Node>> allSubtypes() {
		return Sets.newHashSet(NetworkElement.class);
	}
	
	
}
