package ai.netoai.collector.snmp.discovery;

import java.io.File;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import ai.netoai.collector.deviceprofile.*;
import ai.netoai.collector.model.GenericJavaBean;
import ai.netoai.collector.model.NetworkElement;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.CommunityTarget;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;

import ai.netoai.collector.snmp.SnmpPoller;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.snmp4j.Target;


public class SnmpProfileHelperTest {

	private static final Logger log = LoggerFactory.getLogger(SnmpProfileHelperTest.class);
    
    @Test
    public void testNetworkElement() throws Exception {
        
        System.setProperty("nmsConfig.nms_system", "src/test/resources/nms_system.properties");
        System.setProperty("nmsConfig.collector", "src/test/resources/collector.properties");
        System.out.println("Starting the poller ...");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                SnmpPoller.getInstance().start();
            }
        });
        System.out.println("Started the poller ...");
        
        Thread.currentThread().sleep(5000);
        
        System.out.println("Woke up after the sleep ...");
        
        DeviceConfig config = new DeviceConfig();
        config.setBeanType(NetworkElement.class);
        config.setId("cisco-3850");
        config.setProperties(new ArrayList());
        
        Property prop1 = new Property();
        prop1.setName("productName");
        prop1.setDataType(Property.DataTypes.STRING);
        prop1.setFetchType(Property.PropertyFetchType.SNMP_GET);
        prop1.setValue("1.3.6.1.2.1.47.1.1.1.1.2.1");
        
        Property prop2 = new Property();
        prop2.setName("sysUpTime");
        prop2.setDataType(Property.DataTypes.STRING);
        prop2.setFetchType(Property.PropertyFetchType.SNMP_GET);
        prop2.setValue("1.3.6.1.2.1.1.3.0");
        
        config.getProperties().add(prop1);
        config.getProperties().add(prop2);
        
        Target target = new CommunityTarget();
        target.setAddress(new UdpAddress(InetAddress.getByName("11.0.0.1"), 161));
        target.setRetries(3);
        target.setTimeout(3000);
        target.setVersion(SnmpConstants.version2c);
        
        SnmpProfileHelper helper = new SnmpProfileHelper();
        List<GenericJavaBean> nes = helper.createGenericBeanObjects(config, target);
        for(GenericJavaBean gjb : nes) {
            NetworkElement ne = (NetworkElement) gjb;
            System.out.println("Product name: " + ne.getProductName());
            System.out.println("Sys Up Time: " + ne.getSysUpTime());
        }
    }

	@Test
	public void test() {
		InventoryConfig inventoryConfig = new InventoryConfig();
		File file = new File("snmp/config/inventory/if-mib.conf");
		if (file.exists()) {
			//System.out.println("Create Inventory Config Object with path "+path);
			JsonParser parser = new JsonParser();
			try {
				JsonElement element = parser.parse(new FileReader("snmp/config/inventory/if-mib.conf"));
				if(element.isJsonObject()){
				JsonObject mainObj = element.getAsJsonObject();
				if (mainObj.get("name") != null)
					inventoryConfig.setName(mainObj.get("name").getAsString());
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
								if (tableObj.get("id") != null)
									snmptable.setId(tableObj.get("id").getAsString());
								if (tableObj.get("name") != null)
									snmptable.setName(tableObj.get("name").getAsString());
								if (tableObj.get("oid") != null)
									snmptable.setOid(tableObj.get("oid").getAsString());
								/*if (tableObj.get("filter") != null)
									snmptable.setFilter(tableObj.get("filter"));*/
								if (tableObj.get("appends") != null){
									String appendsTable = tableObj.get("appends").getAsString();
									if(tablesList != null && tablesList.size() != 0){
										for(SnmpTable st : tablesList){
											if(st.getName().equals(appendsTable)){
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
										log.error("Failed", e);
									}
								}

								// Setting snmpTableColumn Objects to SnmpTable
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
												if (tableColumn.get("id") != null)
													snmpTableColumn.setId(tableColumn.get("id").getAsString());
												if (tableColumn.get("name") != null)
													snmpTableColumn.setName(tableColumn.get("name").getAsString());
												if (tableColumn.get("suffix") != null)
													snmpTableColumn.setSuffix(tableColumn.get("suffix").getAsString());
												if (tableColumn.get("required") != null)
													snmpTableColumn.setRequired(tableColumn.get("required").getAsBoolean());
												snmpTableColumnsList.add(snmpTableColumn);
											}
										}
									}
									if (snmpTableColumnsList != null && snmpTableColumnsList.size() != 0)
										snmptable.setColumns(snmpTableColumnsList);
								}
								
								// Setting property objects to snmpTable
								List<Property> tablePropertyList = null;
								if(tableObj.get("properties") != null){
									tablePropertyList = new ArrayList<Property>();
									JsonArray propsArray = tableObj.get("properties").getAsJsonArray();
									for(JsonElement arrEle : propsArray){
										if(arrEle.isJsonObject()){
											JsonObject propObj = arrEle.getAsJsonObject();
											Property property = new Property();
											if(propObj.get("name") != null ) property.setName(propObj.get("name").getAsString());
											if(propObj.get("dataType") != null) property.setDataType(Property.DataTypes.asEnum(propObj.get("dataType").getAsString()));
											if(propObj.get("fetchType") != null) property.setFetchType(Property.PropertyFetchType.asEnum(propObj.get("fetchType").getAsString()));
											if(propObj.get("column") != null) property.setColumn(propObj.get("column").getAsString());
										    if(propObj.get("value") != null) property.setValue(propObj.get("value").getAsString());
										    tablePropertyList.add(property);
										}
									}
									if (tablePropertyList != null && tablePropertyList.size() != 0)
										snmptable.setProperties(tablePropertyList);
								}
								
								if(tableObj.get("metricFamilyIds") != null){
									JsonArray familyIdsArray = tableObj.get("metricFamilyIds").getAsJsonArray();
									if(familyIdsArray != null && familyIdsArray.size() != 0){
										List<String> familyIds = new ArrayList<String>();
										for(JsonElement ele : familyIdsArray){
											if(ele.isJsonPrimitive()){
												familyIds.add(ele.getAsString());
											}
										}
										snmptable.setMetricFamilyIds(familyIds);
									}
								}
								
							}
						}
					}
					inventoryConfig.setTables(tablesList);
				}
			}
			} catch (Exception e) {
				log.error("Failed", e);
			}
		}
	
		
		InetAddress inet = null;
		try {
			inet = InetAddress.getByName("10.0.0.105");
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			log.error("Failed", e);
		}
		CommunityTarget target = new CommunityTarget();
		target.setAddress(new UdpAddress(inet, 161));
		target.setCommunity(new OctetString("public"));
		target.setVersion(SnmpConstants.version2c);
		target.setRetries(3);
		target.setTimeout(10000);
	    
		System.out.println("TARGET - "+target);
		SnmpPoller.getInstance().start();
		SnmpProfileHelper helper = new SnmpProfileHelper();
		helper.createGenericBeanObjects(inventoryConfig, target);
	}

}
