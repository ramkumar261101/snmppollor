package ai.netoai.collector.deviceprofile;

import ai.netoai.collector.deviceprofile.Metric.ConsolidationFunction;
import ai.netoai.collector.deviceprofile.Metric.ConversionFunction;
import ai.netoai.collector.deviceprofile.Metric.MetricValueType;
import ai.netoai.collector.deviceprofile.Metric.Units;
import ai.netoai.collector.deviceprofile.MetricFamily.ProtoCol;
import ai.netoai.collector.deviceprofile.Param.Collector;
import ai.netoai.collector.deviceprofile.Param.Type;
import ai.netoai.collector.deviceprofile.Property.DataTypes;
import ai.netoai.collector.deviceprofile.Property.PropertyFetchType;
import ai.netoai.collector.utils.ExpressionUtil;
import ai.netoai.collector.utils.Expression;
import ai.netoai.collector.settings.SettingsManager;
import ai.netoai.collector.utils.DirectoryWatcher;
import ai.netoai.collector.utils.TarGzArchive;
import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigManager implements Observer {

    private static ConfigManager instance;
    protected static Map<String, SnmpConfig> configMap = new HashMap<String, SnmpConfig>();
    protected static Map<String, List<Metric>> metricsDifferByMetricFamiles = new HashMap<>();
    protected static Set<Metric> metrics = new HashSet<>();
    protected static String tarFilepath = "";
    private static final Logger log = LoggerFactory.getLogger(ConfigManager.class);
    private static final String tracePrefix = "[" + ConfigManager.class.getSimpleName() + "]: ";
    private SettingsManager settings;

    private ConfigManager() {
        this.settings = SettingsManager.getInstance();
    }

    public SnmpConfig getConfigObjectForDevice(String name) {
        if (configMap.get(name) != null) {
            return configMap.get(name);
        }
        return null;
    }

    public static Map<String, SnmpConfig> getConfigMap() {
        return configMap;
    }

    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    public void start() throws InterruptedException {
        Map<String, Object> collectorSettings = settings.getSettings();
        tarFilepath = collectorSettings.get("snmpConfigLocation").toString();
        log.info("TAR FILE PATH " + tarFilepath);
        start(tarFilepath);

    }

    public void start(String tarFilepath) {
        DirectoryWatcher dirWatch = DirectoryWatcher.newInstance(tarFilepath);
        dirWatch.addObserver(getInstance());
        dirWatch.start();

        File file = new File(tarFilepath);
        /* Creating SnmpObjects based on the no of tars in given location */
        for (File fi : file.listFiles()) {
            // searching for tar file
            if (fi.getName().endsWith(".tar")) {
                // log.info(fi.getAbsolutePath());
                try {
                    // extracting tar file to "basePath" location
                    TarGzArchive tarExtract = new TarGzArchive(fi);
                    createSnmpConfigObject(tarExtract, tarFilepath);
                } catch (IOException e) {
                    log.error(tracePrefix + " Error While Extracting Tar", e);
                }
            }
        }
        log.info("Total No of Config Objects -- " + configMap.size());
    }

    public void createSnmpConfigObject(TarGzArchive tarExtract, String tarFilePath)
            throws JsonIOException, JsonSyntaxException, IOException {
        String parentDirectoryPath = tarFilePath + "/" + tarExtract.getParentDirectoryName();
        // log.info(parentDirectoryPath);
        File parentFile = new File(parentDirectoryPath);
        for (File childFiles : parentFile.listFiles()) {
            if (!childFiles.isDirectory()) {
                String configFilePath = childFiles.getAbsolutePath();
                // log.info(configFilePath);
                // converting the config-file to jsonobject
                JsonParser parser = new JsonParser();
                JsonElement element = parser.parse(new FileReader(configFilePath));
                JsonObject snmpConfObj = null;
                if (element.isJsonObject()) {
                    snmpConfObj = element.getAsJsonObject();
                }
                SnmpConfig snmpConfig = new SnmpConfig();
                configMap.put(tarExtract.getParentDirectoryName(), snmpConfig);
                if (snmpConfObj.get("id") != null) {
                    snmpConfig.setId(snmpConfObj.get("id").getAsString());
                }
                if (snmpConfObj.get("vendor") != null) {
                    snmpConfig.setVendor(snmpConfObj.get("vendor").getAsString());
                }
                if (snmpConfObj.get("product") != null) {
                    snmpConfig.setProduct(snmpConfObj.get("product").getAsString());
                }
                if (snmpConfObj.get("version") != null) {
                    snmpConfig.setVersion(snmpConfObj.get("version").getAsString());
                }
                if (snmpConfObj.get("pollInventory") != null) {
                    snmpConfig.setPollInventory(snmpConfObj.get("pollInventory").getAsBoolean());
                }
                if (snmpConfObj.get("pollPerformance") != null) {
                    snmpConfig.setPollPerformance(snmpConfObj.get("pollPerformance").getAsBoolean());
                }
                if (snmpConfObj.get("collectFault") != null) {
                    snmpConfig.setCollectFaults(snmpConfObj.get("collectFault").getAsBoolean());
                }
                if (snmpConfObj.get("discoverPhysicalLinks") != null) {
                    snmpConfig.setDiscoverPhsyicalLinks(snmpConfObj.get("discoverPhysicalLinks").getAsBoolean());
                }
                if (snmpConfObj.get("metricKeyMap") != null) {
                    Map<String, List<String>> map = new HashMap<>();
                    Gson gson = new Gson();
                    String value = snmpConfObj.get("metricKeyMap").toString();
                    map = gson.fromJson(snmpConfObj.get("metricKeyMap"), map.getClass());
                    snmpConfig.setMetricKeyMap(map);
                }
                if (snmpConfObj.get("childConfigs") != null) {
                    JsonElement je = snmpConfObj.get("childConfigs");
                    if (je.isJsonArray()) {
                        JsonArray array = je.getAsJsonArray();
                        for (JsonElement ele : array) {
                            String childPath = ele.getAsString();
                            //log.info("Child Path " + childPath);
                            String fPath = childPath.replaceFirst("snmp", parentDirectoryPath);
                            if (fPath.startsWith(parentDirectoryPath + "/device/")) {
                                createDeviceConfigObject(fPath, snmpConfig);
                            } else if (fPath.startsWith(parentDirectoryPath + "/inventory/")) {
                                createInventoryConfigObject(fPath, snmpConfig);
                            } else if (fPath.startsWith(parentDirectoryPath + "/perf/")) {
                                createPerfConfigObjects(fPath, snmpConfig);
                            } else if (fPath.startsWith(parentDirectoryPath + "/traps")) {
                                createTrapConfigObject(fPath, snmpConfig);
                            } else if (fPath.startsWith(parentDirectoryPath + "/topology")) {
                                createTopologyConfigObject(fPath, snmpConfig);
                            }
                        }
                    }
                }
            }
        }
        log.info("Populated SnmpConfig Object with Configuration of " + tarExtract.getParentDirectoryName());
        org.apache.commons.io.FileUtils.deleteDirectory(new File(parentDirectoryPath));
        log.info("Deleted the Temp File in Location " + parentDirectoryPath);
    }

    private void createTopologyConfigObject(String path, SnmpConfig snmpConfig) {
        log.info("CreateTopologyConfigObjects:" + path);
        Gson gson = new Gson();
        File file = new File(path);
        log.info("Before File");
        if (file.exists()) {
            log.info("file exists");
            PhysicalLinkConfig physicalLinkConifg = new PhysicalLinkConfig();
            JsonParser jsonParser = new JsonParser();
            try {
                JsonElement element = jsonParser.parse(new FileReader(path));
                if (element.isJsonObject()) {
                    JsonObject mainObj = element.getAsJsonObject();
                    if (mainObj.get("name") != null) {
                        physicalLinkConifg.setName(mainObj.get("name").getAsString());
                    }
                    log.info("Main Json Object Name: " + mainObj.get("name").toString());
                    if (mainObj.get("tables") != null) {
                        List<SnmpTable> tablesList = new ArrayList<SnmpTable>();
                        JsonElement tableje = mainObj.get("tables");
                        if (tableje.isJsonArray()) {

                            JsonArray tablesArray = tableje.getAsJsonArray();

                            for (JsonElement tableElement : tablesArray) {

                                if (tableElement.isJsonObject()) {

                                    JsonObject tableObj = tableElement.getAsJsonObject();
                                    SnmpTable snmptable = new SnmpTable();

                                    tablesList.add(snmptable);

                                    log.info("Table Obj ***** :" + tableObj.toString());

                                    if (tableObj.get("id") != null) {
                                        snmptable.setId(tableObj.get("id").getAsString());
                                    }
                                    log.info("TableObj ID " + tableObj.get("id"));
                                    if (tableObj.get("name") != null) {
                                        snmptable.setName(tableObj.get("name").getAsString());
                                    }
                                    log.info("TableObj Name " + tableObj.get("name"));
                                    if (tableObj.get("oid") != null) {
                                        snmptable.setOid(tableObj.get("oid").getAsString());
                                    }
                                    log.info("TableObj OID " + tableObj.get("oid"));
                                    if (tableObj.get("filter") != null) {
                                        JsonObject filterObj = tableObj.get("filter").getAsJsonObject();
                                        String filterExpression = gson.toJson(filterObj);
                                        snmptable.setFilter(filterExpression);
                                    }

                                    if (tableObj.get("beanType") != null) {
                                        String className = tableObj.get("beanType").getAsString();
                                        Class classType = null;
                                        try {
                                            classType = Class.forName(className);
                                            snmptable.setBeanType(classType);
                                            log.info("Bean Type " + tableObj.get("beanType"));
                                        } catch (ClassNotFoundException e) {
                                            log.error(tracePrefix, e);
                                        }
                                    }

                                    List<SnmpTablecolumn> snmpTableColumnsList = null;
                                    if (tableObj.get("columns") != null) {
                                        snmpTableColumnsList = new ArrayList<SnmpTablecolumn>();
                                        JsonElement tableColumnsJE = tableObj.get("columns");
                                        if (tableColumnsJE.isJsonArray()) {
                                            JsonArray tableColumnsArray = tableColumnsJE.getAsJsonArray();
                                            for (JsonElement ele : tableColumnsArray) {
                                                if (ele.isJsonObject()) {
                                                    SnmpTablecolumn snmpTableColumn = new SnmpTablecolumn();
                                                    JsonObject tableColumn = ele.getAsJsonObject();
                                                    if (tableColumn.get("id") != null) {
                                                        snmpTableColumn.setId(tableColumn.get("id").getAsString());
                                                    }
                                                    log.info("Column ID " + tableColumn.get("id").getAsString());
                                                    if (tableColumn.get("name") != null) {
                                                        snmpTableColumn.setName(tableColumn.get("name").getAsString());
                                                    }
                                                    log.info("Column name " + tableColumn.get("name").getAsString());
                                                    if (tableColumn.get("suffix") != null) {
                                                        snmpTableColumn.setSuffix(tableColumn.get("suffix").getAsString());
                                                    }
                                                    log.info("Column suffix" + tableColumn.get("suffix").getAsString());
                                                    if (tableColumn.get("required") != null) {
                                                        snmpTableColumn.setRequired(tableColumn.get("required").getAsBoolean());
                                                    }
                                                    snmpTableColumnsList.add(snmpTableColumn);
                                                }
                                            }
                                        }
                                        if (snmpTableColumnsList != null && snmpTableColumnsList.size() != 0) {
                                            snmptable.setColumns(snmpTableColumnsList);
                                        }
                                    }

                                    // Setting property objects to snmpTable
                                    List<Property> tablePropertyList = null;
                                    if (tableObj.get("properties") != null) {
                                        tablePropertyList = new ArrayList<Property>();
                                        JsonArray propsArray = tableObj.get("properties").getAsJsonArray();
                                        for (JsonElement prpArrEle : propsArray) {
                                            if (prpArrEle.isJsonObject()) {
                                                JsonObject propObj = prpArrEle.getAsJsonObject();
                                                Property property = new Property();
                                                if (propObj.get("name") != null) {
                                                    property.setName(propObj.get("name").getAsString());
                                                }
                                                log.info("Property Name " + propObj.get("name").getAsString());
                                                if (propObj.get("dataType") != null) {
                                                    property.setDataType(
                                                            DataTypes.asEnum(propObj.get("dataType").getAsString()));
                                                }
                                                log.info("Property dataType " + propObj.get("dataType").getAsString());
                                                if (propObj.get("fetchType") != null) {
//                                                    property.setFetchType(PropertyFetchType
//                                                            .asEnum(propObj.get("fetchType").getAsString()));
                                                      String value = propObj.get("fetchType").getAsString();
                                                      property.setFetchIndex(getFetchIndex(value));
                                                      property.setFetchType(getFetchType(value));
                                                }
                                                log.info("FetchType " + propObj.get("fetchType").getAsString());
                                                if (propObj.get("column") != null) {
                                                    property.setColumn(propObj.get("column").getAsString());
                                                }
                                                log.info("Column " + propObj.get("column").getAsString());
                                                if (propObj.get("value") != null) {
                                                    property.setValue(propObj.get("value").getAsString());
                                                }
                                                log.info("value " + propObj.get("value").getAsString());
                                                if ( propObj.get("index") != null ) {
                                                    property.setIndex(propObj.get("index").getAsString());
                                                }
                                                tablePropertyList.add(property);
                                            }
                                        }
                                        if (tablePropertyList != null && tablePropertyList.size() != 0) {
                                            snmptable.setProperties(tablePropertyList);
                                        }
                                    }
                                }
                            }
                        }
                        physicalLinkConifg.setTables(tablesList);
                    }
                    snmpConfig.getChildConfigs().add(physicalLinkConifg);
                }
            } catch (Exception e) {
                log.info("Exception while creating PhysicalLinkConfig object" + e);
            }

        }

    }

    private void createDeviceConfigObject(String path, SnmpConfig snmpConfig) {
        File file = new File(path);
        if (file.exists()) {
            DeviceConfig deviceConfig = new DeviceConfig();
            // log.info("Creating Device Config Object with File " + path);
            JsonParser parser = new JsonParser();
            try {
                JsonElement element = parser.parse(new FileReader(path));
                if (element.isJsonObject()) {
                    JsonObject mainObj = element.getAsJsonObject();
                    if (mainObj.get("id") != null) {
                        deviceConfig.setId(mainObj.get("id").getAsString());
                    }
                    String className = mainObj.get("beanType").getAsString();
                    Class classType = null;
                    try {
                        classType = Class.forName(className);
                        deviceConfig.setBeanType(classType);
                    } catch (ClassNotFoundException e) {
                        log.error(tracePrefix, e);
                    }

                    if (mainObj.get("properties") != null) {
                        List<Property> propertyList = new ArrayList<Property>();
                        JsonArray propertiesArray = mainObj.get("properties").getAsJsonArray();
                        for (JsonElement je : propertiesArray) {
                            if (je.isJsonObject()) {
                                Property property = new Property();
                                JsonObject prop = je.getAsJsonObject();
                                property.setName(prop.get("name").getAsString());
                                property.setDataType(DataTypes.asEnum(prop.get("dataType").getAsString()));
//                                property.setFetchType(PropertyFetchType.asEnum(prop.get("fetchType").getAsString()));
                                String val = prop.get("fetchType").getAsString();
                                property.setFetchIndex(getFetchIndex(val));
                                property.setFetchType(getFetchType(val));
                                property.setValue(prop.get("value").getAsString());

                                propertyList.add(property);

                                String value = null;
                                if (property.getFetchType().equals(PropertyFetchType.EXPLICIT)) {
                                    value = property.getValue();
                                }
                            }
                        }
                        deviceConfig.setProperties(propertyList);
                    }
                    snmpConfig.getChildConfigs().add(deviceConfig);
                }
            } catch (Exception e) {
                log.error(tracePrefix + " Error While Creating DeviceConfigObject ", e);
            }
        }
    }

    private void createInventoryConfigObject(String path, SnmpConfig snmpConfig) {
        Gson gson = new Gson();
        File file = new File(path);
        if (file.exists()) {
            InventoryConfig inventoryConfig = new InventoryConfig();
            // log.info("Create Inventory Config Object with path "+path);
            JsonParser parser = new JsonParser();
            try {
                JsonElement element = parser.parse(new FileReader(path));
                if (element.isJsonObject()) {
                    JsonObject mainObj = element.getAsJsonObject();
                    if (mainObj.get("name") != null) {
                        inventoryConfig.setName(mainObj.get("name").getAsString());
                    }
                    // Setting snmpTable Objects to InventoryConfig
                    if (mainObj.get("tables") != null) {
                        List<SnmpTable> tablesList = new ArrayList<SnmpTable>();
                        JsonElement tablesje = mainObj.get("tables");
                        if (tablesje.isJsonArray()) {
                            JsonArray tablesArray = tablesje.getAsJsonArray();
                            for (JsonElement tableElement : tablesArray) {
                                if (tableElement.isJsonObject()) {
                                    JsonObject tableObj = tableElement.getAsJsonObject();
                                    SnmpTable snmptable = new SnmpTable();
                                    tablesList.add(snmptable);
                                    if (tableObj.get("id") != null) {
                                        snmptable.setId(tableObj.get("id").getAsString());
                                    }
                                    if (tableObj.get("name") != null) {
                                        snmptable.setName(tableObj.get("name").getAsString());
                                    }
                                    if (tableObj.get("oid") != null) {
                                        snmptable.setOid(tableObj.get("oid").getAsString());
                                    }
                                    if (tableObj.get("filter") != null) {
                                        JsonObject filterObj = tableObj.get("filter").getAsJsonObject();
                                        String filterExpression = gson.toJson(filterObj);
                                        snmptable.setFilter(filterExpression);
                                    }
                                    /*
									 * if (tableObj.get("filter") != null)
									 * snmptable.setFilter(tableObj.get("filter"
									 * ));
                                     */
                                    if (tableObj.get("appends") != null) {
                                        String appendsTable = tableObj.get("appends").getAsString();
                                        if (tablesList != null && tablesList.size() != 0) {
                                            for (SnmpTable st : tablesList) {
                                                if (st.getName().equals(appendsTable)) {
                                                    snmptable.setAppends(st);
                                                }
                                            }
                                        }
                                    }
                                    if (tableObj.get("beanType") != null) {
                                        String className = tableObj.get("beanType").getAsString();
                                        Class classType = null;
                                        try {
                                            classType = Class.forName(className);
                                            snmptable.setBeanType(classType);
                                        } catch (ClassNotFoundException e) {
                                            log.error(tracePrefix, e);
                                        }
                                    }

                                    // Setting snmpTableColumn Objects to
                                    // SnmpTable
                                    List<SnmpTablecolumn> snmpTableColumnsList = null;
                                    if (tableObj.get("columns") != null) {
                                        snmpTableColumnsList = new ArrayList<SnmpTablecolumn>();
                                        JsonElement tableColumnsJE = tableObj.get("columns");
                                        if (tableColumnsJE.isJsonArray()) {
                                            JsonArray tableColumnsArray = tableColumnsJE.getAsJsonArray();
                                            for (JsonElement ele : tableColumnsArray) {
                                                if (ele.isJsonObject()) {
                                                    SnmpTablecolumn snmpTableColumn = new SnmpTablecolumn();
                                                    JsonObject tableColumn = ele.getAsJsonObject();
                                                    if (tableColumn.get("id") != null) {
                                                        snmpTableColumn.setId(tableColumn.get("id").getAsString());
                                                    }
                                                    if (tableColumn.get("name") != null) {
                                                        snmpTableColumn.setName(tableColumn.get("name").getAsString());
                                                    }
                                                    if (tableColumn.get("suffix") != null) {
                                                        snmpTableColumn
                                                                .setSuffix(tableColumn.get("suffix").getAsString());
                                                    }
                                                    if (tableColumn.get("required") != null) {
                                                        snmpTableColumn.setRequired(
                                                                tableColumn.get("required").getAsBoolean());
                                                    }
                                                    if (tableColumn.get("fallbackColumn") != null) {
                                                        snmpTableColumn.setFallbackColumn(
                                                                tableColumn.get("fallbackColumn").getAsString());
                                                    }

                                                    snmpTableColumnsList.add(snmpTableColumn);
                                                }
                                            }
                                        }
                                        if (snmpTableColumnsList != null && snmpTableColumnsList.size() != 0) {
                                            snmptable.setColumns(snmpTableColumnsList);
                                        }
                                    }

                                    // Setting property objects to snmpTable
                                    List<Property> tablePropertyList = null;
                                    if (tableObj.get("properties") != null) {
                                        tablePropertyList = new ArrayList<Property>();
                                        JsonArray propsArray = tableObj.get("properties").getAsJsonArray();
                                        for (JsonElement arrEle : propsArray) {
                                            if (arrEle.isJsonObject()) {
                                                JsonObject propObj = arrEle.getAsJsonObject();
                                                Property property = new Property();
                                                if (propObj.get("name") != null) {
                                                    property.setName(propObj.get("name").getAsString());
                                                }
                                                if (propObj.get("dataType") != null) {
                                                    property.setDataType(
                                                            DataTypes.asEnum(propObj.get("dataType").getAsString()));
                                                }
                                                if (propObj.get("fetchType") != null) {
//                                                    property.setFetchType(PropertyFetchType
//                                                            .asEnum(propObj.get("fetchType").getAsString()));
                                                    String value = propObj.get("fetchType").getAsString();
                                                    property.setFetchIndex(getFetchIndex(value));
                                                    property.setFetchType(getFetchType(value));
                                                }
                                                if (propObj.get("column") != null) {
                                                    property.setColumn(propObj.get("column").getAsString());
                                                }
                                                if (propObj.get("value") != null) {
                                                    property.setValue(propObj.get("value").getAsString());
                                                }
                                                tablePropertyList.add(property);
                                            }
                                        }
                                        if (tablePropertyList != null && tablePropertyList.size() != 0) {
                                            snmptable.setProperties(tablePropertyList);
                                        }
                                    }

                                    if (tableObj.get("metricFamilyIds") != null) {
                                        JsonArray familyIdsArray = tableObj.get("metricFamilyIds").getAsJsonArray();
                                        if (familyIdsArray != null && familyIdsArray.size() != 0) {
                                            List<String> familyIds = new ArrayList<String>();
                                            for (JsonElement ele : familyIdsArray) {
                                                if (ele.isJsonPrimitive()) {
                                                    familyIds.add(ele.getAsString());
                                                }
                                            }
                                            snmptable.setMetricFamilyIds(familyIds);
                                        }
                                    }
                                    if (tableObj.get("syncTrapIds") != null) {
                                        JsonArray syncTrapsArray = tableObj.get("syncTrapIds").getAsJsonArray();
                                        if (syncTrapsArray != null && syncTrapsArray.size() != 0) {
                                            List<String> trapIds = new ArrayList<String>();
                                            for (JsonElement ele : syncTrapsArray) {
                                                if (ele.isJsonPrimitive()) {
                                                    trapIds.add(ele.getAsString());
                                                }
                                            }
                                            snmptable.setTrapIds(trapIds);
                                        }
                                    }
                                }
                            }
                        }
                        inventoryConfig.setTables(tablesList);
                    }
                    snmpConfig.getChildConfigs().add(inventoryConfig);
                }
            } catch (Exception e) {
                log.error(tracePrefix + " Error While Creating InventoryConfigObject ", e);
            }
        }
    }

    private void createPerfConfigObjects(String path, SnmpConfig snmpConfig) {
        File file = new File(path);
        if (file.exists()) {
            // log.info("Creating Perf Config Objects From Path "+path);
            PmConfig perfConfig = new PmConfig();
            JsonParser parser = new JsonParser();
            try {
                JsonElement element = parser.parse(new FileReader(path));
                if (element.isJsonObject()) {
                    JsonObject mainObj = element.getAsJsonObject();
                    if (mainObj != null) {
                        if (mainObj.get("id") != null) {
                            perfConfig.setId(mainObj.get("id").getAsString());
                        }
                        if (mainObj.get("name") != null) {
                            perfConfig.setName(mainObj.get("name").getAsString());
                        }
                        if (mainObj.get("metricFamilies") != null) {
                            JsonArray familiesArray = mainObj.get("metricFamilies").getAsJsonArray();
                            if (familiesArray != null && familiesArray.size() != 0) {
                                for (JsonElement familyEle : familiesArray) {
                                    if (familyEle.isJsonObject()) {
                                        JsonObject familyObj = familyEle.getAsJsonObject();
                                        MetricFamily family = new MetricFamily();
                                        if (familyObj.get("id") != null) {
                                            family.setId(familyObj.get("id").getAsString());
                                        }
                                        if (familyObj.get("name") != null) {
                                            family.setName(familyObj.get("name").getAsString());
                                        }
                                        if (familyObj.get("protocol") != null) {
                                            family.setProtocol(
                                                    ProtoCol.asEnum(familyObj.get("protocol").getAsString()));
                                        }
                                        if (familyObj.get("metrics") != null) {
                                            if (familyObj.get("metrics").isJsonArray()) {
                                                JsonArray metricsArray = familyObj.get("metrics").getAsJsonArray();
                                                for (JsonElement metricEle : metricsArray) {
                                                    if (metricEle.isJsonObject()) {
                                                        JsonObject metricObj = metricEle.getAsJsonObject();
                                                        Metric metric = new Metric();
                                                        if (metricObj.get("id") != null) {
                                                            metric.setId(metricObj.get("id").getAsString());
                                                        }
                                                        if (metricObj.get("name") != null) {
                                                            metric.setName(metricObj.get("name").getAsString());
                                                        }
                                                        if (metricObj.get("descr") != null) {
                                                            metric.setDescr(metricObj.get("descr").getAsString());
                                                        }
                                                        if (metricObj.get("metricValueType") != null) {
                                                            metric.setMetricValueType(MetricValueType.asEnum(
                                                                    metricObj.get("metricValueType").getAsString()));
                                                        }
                                                        if (metricObj.get("protocol") != null) {
                                                            metric.setProtocol(ProtoCol
                                                                    .asEnum(metricObj.get("protocol").getAsString()));
                                                        }
                                                        if (metricObj.get("units") != null) {
                                                            metric.setUnits(
                                                                    Units.asEnum(metricObj.get("units").getAsString()));
                                                        }
                                                        if (metricObj.get("conversionFunction") != null) {
                                                            metric.setConversionFunction(ConversionFunction.asEnum(
                                                                    metricObj.get("conversionFunction").getAsString()));
                                                        }
                                                        if (metricObj.get("consolidation") != null) {
                                                            metric.setConsolidation(ConsolidationFunction.asEnum(
                                                                    metricObj.get("consolidation").getAsString()));
                                                        }
                                                        if (metricObj.get("value") != null) {
                                                            metric.setValue(metricObj.get("value").getAsString());
                                                        }
                                                        if (metricObj.get("scriptVariables") != null) {
                                                            metric.setScriptVariables(metricObj.get("scriptVariables").getAsString());
                                                        }
                                                        if (metricObj.get("evalType") != null) {
                                                            metric.setEvalType(metricObj.get("evalType").getAsString());
                                                        }
                                                        if (metricObj.get("plotType") != null) {
                                                            metric.setPlotType(metricObj.get("plotType").getAsString());
                                                        }
                                                        if (metricObj.get("color") != null) {
                                                            metric.setColor(metricObj.get("color").getAsString());
                                                        }
                                                        if (metricObj.get("type") != null) {
                                                            metric.setType(MetricType.asEnum(metricObj.get("type").getAsString()));
                                                        }
                                                        if (metricObj.get("paramList") != null) {
                                                            if (metricObj.get("paramList").isJsonArray()) {
                                                                JsonArray paramArray = metricObj.get("paramList")
                                                                        .getAsJsonArray();
                                                                for (JsonElement paramEle : paramArray) {
                                                                    if (paramEle.isJsonObject()) {
                                                                        JsonObject paramObj = paramEle
                                                                                .getAsJsonObject();
                                                                        Param param = new Param();
                                                                        if (paramObj.get("id") != null) {
                                                                            param.setId(
                                                                                    paramObj.get("id").getAsString());
                                                                        }
                                                                        if (paramObj.get("name") != null) {
                                                                            param.setName(
                                                                                    paramObj.get("name").getAsString());
                                                                        }
                                                                        if (paramObj.get("oid") != null) {
                                                                            param.setOid(
                                                                                    paramObj.get("oid").getAsString());
                                                                        }
                                                                        if (paramObj.get("type") != null) {
                                                                            param.setType(Type.asEnum(paramObj
                                                                                    .get("type").getAsString()));
                                                                        }
                                                                        if (paramObj.get("indexed") != null) {
                                                                            param.setIndexed(paramObj.get("indexed")
                                                                                    .getAsBoolean());
                                                                        }
                                                                        if (paramObj.get("collector") != null) {
                                                                            param.setCollector(Collector.asEnum(paramObj
                                                                                    .get("collector").getAsString()));
                                                                        }
                                                                        metric.getParamList().add(param);
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        family.getMetrics().add(metric);
                                                        metrics.add(metric);
                                                    }
                                                }
                                            }
                                        }
                                        perfConfig.getMetricFamilies().add(family);
                                        metricsDifferByMetricFamiles.put(family.getId(), family.getMetrics());
                                    }
                                }
                            }
                        }
                        snmpConfig.getChildConfigs().add(perfConfig);
                    }
                }
            } catch (Exception e) {
                log.error(tracePrefix + " Error While Creating PerfConfigObject ", e);
            }
        }
    }

    public void createTrapConfigObject(String path, SnmpConfig snmpConfig) {
        File file = new File(path);
        if (file.exists()) {
            if ( log.isTraceEnabled() ) {
                log.trace("Creating Trap Config Object From Path " + path);
            }
            JsonParser parser = new JsonParser();
            try {
                JsonElement mainEle = parser.parse(new FileReader(path));
                if (mainEle.isJsonObject()) {
                    JsonObject mainObj = mainEle.getAsJsonObject();
                    TrapConfig trapConfig = new TrapConfig();
                    // setting id to trapconfig
                    if (mainObj.get("id") != null) {
                        trapConfig.setId(mainObj.get("id").getAsString());
                    }
                    // setting traps to trapconfig
                    if (mainObj.get("traps") != null) {
                        JsonElement trapsEle = mainObj.get("traps");
                        if (trapsEle.isJsonArray()) {
                            JsonArray trapsArr = trapsEle.getAsJsonArray();
                            for (JsonElement trapEle : trapsArr) {
                                if (trapEle.isJsonObject()) {
                                      Trap trap = new Trap();
                                      JsonObject trapObj = trapEle.getAsJsonObject();
                                      if (trapObj.get("id") != null) {
                                            trap.setId(trapObj.get("id").getAsString());
                                        }
                                        if (trapObj.get("condition") != null) {
                                            JsonElement condEle = trapObj.get("condition");
                                            Gson g = new Gson();
                                            String condition = g.toJson(condEle);
                                            trap.setCondition(condition);
                                        }
                                        if (trapObj.get("beanType") != null) {
                                            String className = trapObj.get("beanType").getAsString();
                                            Class classType = null;
                                            try {
                                                classType = Class.forName(className);
                                                trap.setBeanType(classType);
                                            } catch (ClassNotFoundException e) {
                                                log.error(tracePrefix, e);
                                            }
                                        }
                                      if ( trapObj.get("properties") != null ) {
                                          Map<String, Object> props = new Gson().fromJson(trapObj.get("properties").toString(), Map.class);
                                          trap.setProperties(props);
                                      }
                                    trapConfig.getTraps().add(trap);
                                }
                            }
                        }
                    }
                    // setting synchronize to trap object
                    if (mainObj.get("synchronize") != null) {
                        JsonElement syncEle = mainObj.get("synchronize");
                        if (syncEle.isJsonObject()) {
                            log.info("Not handling events as of now");
                        }
                    }
                    snmpConfig.getChildConfigs().add(trapConfig);
                }
            } catch (Exception e) {
                log.error(tracePrefix + " Error While Creating TrapConfigObject ", e);
            }
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        DirectoryWatcher.FileChangeNotification notif = (DirectoryWatcher.FileChangeNotification) arg;
        log.info(tracePrefix + "Event type: " + notif.getType() + ", File name: " + notif.getName());
        if (notif.getType().equals(DirectoryWatcher.FILE_CREATE)
                || notif.getType().equals(DirectoryWatcher.FILE_MODIFY)) {
            String[] splits = notif.getName().split("\\/");
            if (splits[splits.length - 1].trim().indexOf(".tar") != -1) {
                String name = splits[splits.length - 1];
                if (name.indexOf(".tar") != -1) {
                    TarGzArchive tarExtract;
                    try {
                        tarExtract = new TarGzArchive(new File(notif.getName()));
                        createSnmpConfigObject(tarExtract, tarFilepath);
                    } catch (Exception e) {
                        log.error(tracePrefix + " Error While Extracting Tar ", e);
                    }
                }
            }
            log.info("Total No of Config Objects -- " + configMap.size());
        } else if (notif.getType().equals(DirectoryWatcher.FILE_DELETE)) {
            String[] splits = notif.getName().split("\\/");
            if (splits[splits.length - 1].trim().indexOf(".tar") != -1) {
                String removeDevice = splits[splits.length - 1].replaceFirst(".tar", "");
                configMap.remove(removeDevice);
                log.info("Total No of Config Objects -- " + configMap.size());
            }
        }
    }
    
    private PropertyFetchType getFetchType(String value) {
        PropertyFetchType result = null;
        if(!value.contains("(")) 
            return PropertyFetchType.asEnum(value);
        String val = value.split("\\(")[0];
        return PropertyFetchType.asEnum(val);
    }
    
    private String getFetchIndex(String value) {
        String result = null;
        if(!value.contains("(")) return result;
        Matcher m = Pattern.compile("\\(([^)]+)\\)").matcher(value);
        while(m.find()) {
            result = m.group(1);
            break;
        }
        return result;
    } 

}
