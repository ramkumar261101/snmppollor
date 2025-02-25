package ai.netoai.collector.snmp.discovery;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import ai.netoai.collector.cache.NodeCacheManager;
import ai.netoai.collector.deviceprofile.*;
import ai.netoai.collector.deviceprofile.Property.PropertyFetchType;
import ai.netoai.collector.deviceprofile.Property.DataTypes;
import ai.netoai.collector.model.*;
import ai.netoai.collector.model.EndPoint.Type;
import ai.netoai.collector.model.EndPoint.AdminStatus;
import ai.netoai.collector.model.EndPoint.OperStatus;
import ai.netoai.collector.model.EndPoint.EndPointType;
import ai.netoai.collector.settings.SettingsManager;
import ai.netoai.collector.settings.SnmpSettings;
import ai.netoai.collector.utils.Expression;
import ai.netoai.collector.utils.ExpressionUtil;
import ai.netoai.collector.utils.SimpleExpression;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.AbstractTarget;
import org.snmp4j.PDU;
import org.snmp4j.Target;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;

import ai.netoai.collector.snmp.SnmpPoller;




public class SnmpProfileHelper {
	private static final Logger log = LoggerFactory.getLogger(SnmpProfileHelper.class);
	private static final String tracePrefix = "[" + SnmpProfileHelper.class.getSimpleName() + "]: ";

	private static final OID ifHcInOctetsOid = new OID("1.3.6.1.2.1.31.1.1.1.6");
	private static final OID ifHcOutOctetsOid = new OID(".1.3.6.1.2.1.31.1.1.1.10");
	private static final OID ifSpeedOid = new OID("1.3.6.1.2.1.2.2.1.5");

	private SnmpPoller poller;
	private SnmpSettings snmpSettings;

	/*
	 * This should be a utility class. All the functions should be static/This
	 * should be a singleton
	 */
	public SnmpProfileHelper() {
		this.poller = SnmpPoller.getInstance();
		SettingsManager settings = SettingsManager.getInstance();
		snmpSettings = new SnmpSettings();
	}

	public List<GenericJavaBean> createGenericBeanObjects(Config config, Target target) {
		List<GenericJavaBean> list = null;
		if (config instanceof DeviceConfig) {
			DeviceConfig dc = (DeviceConfig) config;
			list = createNetworkElementObjects(dc, target);
		} else if (config instanceof InventoryConfig) {
			InventoryConfig ic = (InventoryConfig) config;
			list = createEndPointObjects(ic, target);
			if (CollectionUtils.isNotEmpty(list)) {
				pollUtilization(list, target);
			}
		}
		return list;
	}

	private Long getInOctets(EndPoint ep, Target target) {
		PDU ifHcInOctetsPdu = new PDU();
		ifHcInOctetsPdu.add(new VariableBinding(new OID(ifHcInOctetsOid.toString() + "." + ep.getEndPointIndex())));
		ifHcInOctetsPdu.setType(PDU.GET);
		PDU responsePdu = poller.sendSyncGetRequest(target, ifHcInOctetsPdu, 0);
		Long inOctets = responsePdu.get(0).getVariable().toLong();
		return inOctets;
	}

	private Long getOutOctets(EndPoint ep, Target target) {
		PDU ifHcOutOctetsPdu = new PDU();
		ifHcOutOctetsPdu.add(new VariableBinding(new OID(ifHcOutOctetsOid.toString() + "." + ep.getEndPointIndex())));
		ifHcOutOctetsPdu.setType(PDU.GET);
		PDU responsePdu = poller.sendSyncGetRequest(target, ifHcOutOctetsPdu, 0);
		Long outOctets = responsePdu.get(0).getVariable().toLong();
		return outOctets;
	}

	private Long getIfSpeed(EndPoint ep, Target target) {
		PDU ifSpeedPdu = new PDU();
		ifSpeedPdu.add(new VariableBinding(new OID(ifSpeedOid.toString() + "." + ep.getEndPointIndex())));
		ifSpeedPdu.setType(PDU.GET);
		PDU responsePdu = poller.sendSyncGetRequest(target, ifSpeedPdu, 0);
		return responsePdu.get(0).getVariable().toLong();
	}

	private void pollUtilization(List<GenericJavaBean> list, Target target) {
		List<EndPoint> endpointList = list.stream().map(bean -> (EndPoint) bean).collect(Collectors.toList());
		Map<String, Long> inOctetsMapt1 = new HashMap<>();
		Map<String, Long> outOctetsMapt1 = new HashMap<>();
		Map<String, Long> inOctetsMapt2 = new HashMap<>();
		Map<String, Long> outOctetsMapt2 = new HashMap<>();
		Map<String, Long> speedMap = new HashMap<>();
		for (EndPoint ep : endpointList) {
			if ( ep.getType() != Type.ethernetCsmacd ) {
				continue;
			}

			Long inOctets = getInOctets(ep, target);
			inOctetsMapt1.put(ep.getSourceId(), inOctets);

			Long outOctets = getOutOctets(ep, target);
			outOctetsMapt1.put(ep.getSourceId(), outOctets);

			Long ifSpeed = getIfSpeed(ep, target);
			speedMap.put(ep.getSourceId(), ifSpeed);
		}
		if (inOctetsMapt1.isEmpty()) {
			return;
		}
		Map<String, GenericJavaBean> endPointMap = list.stream().collect(Collectors.toMap(key -> ((EndPoint)key).getSourceId(), bean -> bean));
 		log.info("ifInOctetsMap: {}", inOctetsMapt1);
		log.info("ifOutOctetsMap: {}", outOctetsMapt1);
		log.info("ifSpeedMap: {}", speedMap);

		log.info("-------------------------- SLEEPING 10 seconds to collect util data ------------------------");
        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        for (EndPoint ep : endpointList) {
			if ( ep.getType() != Type.ethernetCsmacd ) {
				continue;
			}
			Long inOctets = getInOctets(ep, target);
			inOctetsMapt2.put(ep.getSourceId(), inOctets);

			Long outOctets = getOutOctets(ep, target);
			outOctetsMapt2.put(ep.getSourceId(), outOctets);
		}
		log.info("ifInOctetsMap: {}", inOctetsMapt2);
		log.info("ifOutOctetsMap: {}", outOctetsMapt2);
		log.info("ifSpeedMap: {}", speedMap);

		for (EndPoint ep : endpointList) {
			String index = ep.getSourceId();
			if ( speedMap.containsKey(index)
					&& inOctetsMapt1.containsKey(index)
					&& outOctetsMapt1.containsKey(index)
					&& inOctetsMapt2.containsKey(index)
					&& outOctetsMapt2.containsKey(index) ) {
				float speed = speedMap.get(index).floatValue();
				float inOctets1 = inOctetsMapt1.get(index).floatValue();
				float outOctets1 = outOctetsMapt1.get(index).floatValue();
				float inOctets2 = inOctetsMapt2.get(index).floatValue();
				float outOctets2 = outOctetsMapt2.get(index).floatValue();
				float deltaBytes = (inOctets2 + outOctets2) - (inOctets1 + outOctets1);
				float utilPerc = ( (deltaBytes * 8) / (10 * speed) ) * 100;
				log.info("Utilization of Endpoint {} with index: {}, NE: {} is: {}%", ep.getDescription(), ep.getSourceId(), target.getAddress(), utilPerc);
				if ( endPointMap.containsKey(index) ) {
					((EndPoint)endPointMap.get(index)).setUtilPercentage(utilPerc);
				} else {
					((EndPoint)endPointMap.get(index)).setUtilPercentage(0.0f);
				}
			}
		}
	}
        
        public List<GenericJavaBean> createGenericBeanObjects(Config config, Target target, NetworkElement ne) {
            List<GenericJavaBean> list = null;
            if (config instanceof PhysicalLinkConfig) {
                    PhysicalLinkConfig pc = (PhysicalLinkConfig) config;
                    list = createPhysicalLinkObjects(pc, target, ne);
            }
            return  list;
        }


        public String convertHexToIp(String hex){
            StringBuilder sb = new StringBuilder();
            String values[] = hex.split(":");
            for(int i = 0; i < values.length ; i++) {
                int value = Integer.parseInt(values[i], 16);
                sb.append(value + ((i != values.length-1) ? "." : ""));
            }
            return sb.toString();
        }


        
        private List<GenericJavaBean> createPhysicalLinkObjects(PhysicalLinkConfig linkConfig, Target target, NetworkElement ne) {
            log.info(tracePrefix + " creating physical link objects");
            List<GenericJavaBean> beanL = new ArrayList<>();
            List<SnmpTable> tables = linkConfig.getTables();
			try {
				for(SnmpTable table : tables) {
					String tableOid = table.getOid();
					List<SnmpTablecolumn> tableColL = table.getColumns();
					Map<String, Map<String, VariableBinding>> mibMap = new LinkedHashMap<>();
					for(SnmpTablecolumn column : tableColL) {
						String suffix = column.getSuffix();
						List<VariableBinding> varL = poller.getResponseVariableBindings(tableOid, suffix, target);
						for(VariableBinding vb : varL) {
							if(tableOid.startsWith(".")) {
								tableOid = tableOid.trim().substring(1);
							}
							String index = vb.getOid().toString().replace(tableOid+"."+suffix+".", "");
							if(!mibMap.containsKey(index)) {
								mibMap.put(index, new LinkedHashMap<>());
							}
							mibMap.get(index).put(column.getName(), vb);
						}
					}
					log.info("{}Mib map for NE: {} is: {}", tracePrefix, ne, mibMap.size());
					String[] address = target.getAddress().toString().split("\\/");
					for(Entry<String, Map<String, VariableBinding>> e : mibMap.entrySet()) {
						String key = e.getKey();
						Map<String, VariableBinding> map = e.getValue();
						PhysicalLink pLink = new PhysicalLink();
						NetworkElement destNetwork = NodeManager.getInstance().getNetworkElement(address[0]);
						if(destNetwork == null) continue;
						List<EndPoint> endPoints = NodeCacheManager.getInstance().getEndPoints(destNetwork);
						log.info("{}Number of endpoints for node {} are: {}", tracePrefix, ne, endPoints.size());
						if(endPoints == null) continue;
						for(Property prop : table.getProperties()) {
							String var = null;
							if(map.containsKey(prop.getColumn())) {
								var = map.get(prop.getColumn()).getVariable().toString();
							}
							Object value = null;
							if(prop.getFetchType().equals(PropertyFetchType.NODEOBJ)) {
								if(prop.getFetchIndex() != null) {
									value = ne.getAllProperties().get(prop.getFetchIndex());
								}
							} else if (prop.getFetchType().equals(PropertyFetchType.INDEX)) {
								SnmpTablecolumn col = table.getColumnByName(prop.getColumn());
								String cIndex = map.get(prop.getColumn()).getOid().toString();
								if(cIndex.startsWith(".")) cIndex = cIndex.trim().substring(1);
								cIndex = cIndex.replace(tableOid+"."+col.getSuffix(), "");
								if(prop.getFetchIndex() != null) {
									OID oid = new OID(cIndex);
									int index = Integer.parseInt(prop.getFetchIndex());
									value = "1.3.6.1.2.1.2.2.1.1."+oid.get(index)+"";
								}
							} else if (prop.getFetchType().equals(PropertyFetchType.HEXTOIP)) {
								value = convertHexToIp(var);
							} else if (prop.getFetchType().equals(PropertyFetchType.COLUMN)) {
								value = var;
							} else if (prop.getFetchType().equals(PropertyFetchType.SNMP_GET)) {
								String oidIndex = "";
								if (StringUtils.isNotBlank(prop.getIndex())) {
									String[] indexTokens = key.split("\\.");
									oidIndex = indexTokens[Integer.parseInt(prop.getIndex())];
								}
								OID reqOid = new OID(prop.getValue() + "." + oidIndex);
								log.info("{}ReqOID for SNMP GET fetch type: {}", tracePrefix, reqOid);
								value = this.getValueFromSnmpGet(target, reqOid);
							}

//							if(prop.getName().equals("destinationPort")) {
//								EndPoint ep = null;
//								for(EndPoint e : endPoints) {
//									if(e.getDescription() != null &&
//											e.getDescription().equals(var)) {
//										log.info("{}Endpoint desc = {}, var = {}", tracePrefix, e.getDescription(), var);
//										ep = e;
//										break;
//									}
//								}
//								if(ep == null) continue;
//								value = ep.getSourceId();
//							}
							pLink.setProperty(prop.getName(), value);
						}
						if ( pLink.getSourceNodeIP() == null && pLink.getSourceNodeName() != null ) {
							NetworkElement _ne = NodeManager.getInstance().getNetworkElementByName(pLink.getSourceNodeName());
							if (_ne != null) {
								pLink.setSourceNodeIP(_ne.getIp());
							}
						}
						if ( pLink.getSourceNodeIP() != null && pLink.getSourceNodeName() == null ) {
							NetworkElement _ne = NodeManager.getInstance().getNetworkElement(pLink.getSourceNodeIP());
							if (_ne != null) {
								pLink.setSourceNodeName(_ne.getName());
							}
						}
						if ( pLink.getDestinationNodeIP() == null && pLink.getDestinationNodeName() != null ) {
							NetworkElement _ne = NodeManager.getInstance().getNetworkElementByName(pLink.getDestinationNodeName());
							if (_ne != null) {
								pLink.setDestinationNodeIP(_ne.getIp());
							}
						}
						if ( pLink.getDestinationNodeIP() != null && pLink.getDestinationNodeName() == null ) {
							NetworkElement _ne = NodeManager.getInstance().getNetworkElement(pLink.getDestinationNodeIP());
							if (_ne != null) {
								pLink.setDestinationNodeName(_ne.getName());
							}
						}
						beanL.add(pLink);
					}
				}
			} catch (Exception ex) {
				log.error("{}Failed building the physical links", tracePrefix, ex);
			}

            log.info("{}Number of physcial links formed for NE: {} are: {}", tracePrefix, ne, beanL.size());
            return beanL;
        }

	private Object getValueFromSnmpGet(Target target, OID reqOid) {
		Object value = null;
		PDU pdu = new PDU();
		pdu.add(new VariableBinding(reqOid));
		pdu.setType(PDU.GET);
		PDU getPdu = poller.sendSyncGetRequest(target, pdu, 0);
		if (log.isDebugEnabled()) {
			log.debug(tracePrefix + "NE: " + target.getAddress() + ", Response: " + getPdu);
		}
		if (getPdu != null) {
			Vector<VariableBinding> vbs = (Vector<VariableBinding>) getPdu.getVariableBindings();
			if (vbs != null && vbs.size() != 0) {
				value = vbs.get(0).getVariable().toString();
				if (value.equals("noSuchObject")) {
					value = null;
				}
			}
		}
		return value;
	}

	private Object getObjectValueBasedOnDataType(DataTypes dataType, String value) {
		Object objVal = null;
		if (value != null && !value.isEmpty()) {
			if (dataType.equals(DataTypes.INTEGER)) {
				if (value != null)
					objVal = Integer.parseInt(value);
			} else if (dataType.equals(DataTypes.STRING)) {
				if (value != null)
					objVal = value;
			} else if (dataType.equals(DataTypes.TYPE)) {
				if (value != null)
					objVal = Type.getEnumFromInt(Integer.parseInt(value));
			} else if (dataType.equals(DataTypes.ADMINSTATUS)) {
				if (value != null)
					objVal = AdminStatus.getEnumFromInt(Integer.parseInt(value));
			} else if (dataType.equals(DataTypes.OPERSTATUS)) {
				if (value != null)
					objVal = OperStatus.getEnumFromInt(Integer.parseInt(value));
			} else if (dataType.equals(DataTypes.ENDPOINTTYPE)) {
				if (value != null)
					objVal = EndPointType.getEnumFromString(value);
			} else if (dataType.equals(DataTypes.DEVICETYPE)) {
				if (value != null)
					objVal = NetworkElement.DeviceType.asEnum(value);
			}
		}
		return objVal;
	}

	private List<GenericJavaBean> createNetworkElementObjects(DeviceConfig device, Target target) {
		List<GenericJavaBean> beansList = new ArrayList<GenericJavaBean>();
		List<Property> deviceProperties = device.getProperties();
		Class beanType = device.getBeanType();
		NetworkElement networkEle = null;
		try {
			networkEle = (NetworkElement) beanType.newInstance();
		} catch (Exception e) {
			log.error("Failed", e);
		}

		if (deviceProperties != null && deviceProperties.size() != 0) {
			int count = 0;
			for (Property prop : deviceProperties) {
				DataTypes dataType = prop.getDataType();

				String value = null;
				if (prop.getFetchType().equals(PropertyFetchType.EXPLICIT)) {
					value = prop.getValue();
				} else if (prop.getFetchType().equals(PropertyFetchType.SNMP_GET)) {
					PDU pdu = new PDU();
					pdu.add(new VariableBinding(new OID(prop.getValue())));
					pdu.setType(PDU.GET);
					PDU getPdu = poller.sendSyncGetRequest(target, pdu, count);
					if (log.isDebugEnabled()) {
						log.debug(tracePrefix + "NE: " + target.getAddress() + ", Response: " + getPdu);
					}
					if (getPdu != null) {
						Vector<VariableBinding> vbs = (Vector<VariableBinding>) getPdu.getVariableBindings();
						if (vbs != null && vbs.size() != 0) {
							value = vbs.get(0).getVariable().toString();
							if (prop.getName().equals("baseBridgeAddress"))
								value = value.toUpperCase();
							if (value.equals("noSuchObject")) {
								value = null;
							}
						}
					}
				}
				Object objVal = getObjectValueBasedOnDataType(dataType, value);
				networkEle.setProperty(prop.getName(), objVal);
				count++;
			}
		}

		beansList.add(networkEle);
		// networkEle.logAllProperties(beansList);
		return beansList;
	}

	private List<GenericJavaBean> createEndPointObjects(InventoryConfig inventory, Target target) {
		Map<String, Map<String, GenericJavaBean>> tablesMap = new LinkedHashMap<String, Map<String, GenericJavaBean>>();
		List<GenericJavaBean> genericBeansList = new ArrayList<GenericJavaBean>();
		List<SnmpTable> snmpTables = inventory.getTables();

		for (SnmpTable table : snmpTables) {
			Map<String, Map<String, VariableBinding>> mibMap = new LinkedHashMap<String, Map<String, VariableBinding>>();
			String tableOID = table.getOid();
			String filter = table.getFilter();
			SimpleExpression ExpFilter = null;
			String filterOID = null;
			if (filter != null && !StringUtils.equals(filter, "{}")) {
				log.info("{}Table OID: {}, Filter: {}", tracePrefix, tableOID, filter);
				Expression expression = ExpressionUtil.fromJson(filter);
				if (expression instanceof SimpleExpression) {
					ExpFilter = (SimpleExpression) expression;
					filterOID = ExpFilter.getOperand();
				}
			}

			List<SnmpTablecolumn> tableColumns = table.getColumns();
			// filling the mib-map for a table
			for (SnmpTablecolumn column : tableColumns) {
				String suffix = column.getSuffix();
				List<VariableBinding> list = poller.getResponseVariableBindings(tableOID, column.getSuffix(), target);
				if ((list == null || list.size() == 0) && !column.isRequired()) {
					suffix = getFallBackColumnSuffix(tableColumns, column.getFallbackColumn());
					list = poller.getResponseVariableBindings(tableOID, suffix, target);
				}
				// log.info("Total VarBinds For Column " + column.getName() + "
				// is " + list.size());
				for (VariableBinding vb : list) {
					String index = null;
					if (vb != null) {
						char ch = table.getOid().toString().trim().charAt(0);
						String tableOid = table.getOid().toString().trim();
						if (ch == '.') {
							tableOid = table.getOid().toString().trim().substring(1);
						}
						index = vb.getOid().toString().replace(tableOid + "." + suffix + ".", "");
					}

					try {
						if (index != null) {
							boolean insertIntoMib = true;
							if (filterOID != null) {
								String oid = filterOID + "." + index;
								PDU pdu = new PDU();
								pdu.add(new VariableBinding(new OID(oid)));
								pdu.setType(PDU.GET);
								PDU resPdu = poller.sendSyncGetRequest(target, pdu);
								String value = null;
								if (resPdu != null) {
									Vector<VariableBinding> vbs = (Vector<VariableBinding>) resPdu
											.getVariableBindings();
									if (vbs != null && vbs.size() != 0) {
										value = vbs.get(0).getVariable().toString();
										if (value.equals("noSuchObject"))
											value = null;
									}
								}
								if (value != null) {
									Object obj = ExpFilter.getValue();
									if (obj instanceof List) {
										List<String> valuesList = (List<String>) obj;
										if (!valuesList.contains(value)) {
											insertIntoMib = false;
										}
									} else if (obj instanceof String) {
										String expectedValue = (String) obj;
										if (!expectedValue.equals(value)) {
											insertIntoMib = false;
										}
									}
								}
							}

							if (insertIntoMib) {
								if (mibMap.containsKey(index)) {
									Map<String, VariableBinding> map = mibMap.get(index);
									map.put(column.getName(), vb);
								} else {
									Map<String, VariableBinding> map = new LinkedHashMap<String, VariableBinding>();
									map.put(column.getName(), vb);
									mibMap.put(index, map);
								}
							}
						}
					} catch (Exception e) {
						log.error(tracePrefix, e);
					}
				}
			}

			String appendsTable = null;
			if (table.getAppends() != null) {
				appendsTable = table.getAppends().getName();
			}

			Map<String, GenericJavaBean> rowGenericBeanMap = new LinkedHashMap<String, GenericJavaBean>();
			List<Property> properties = table.getProperties();
			for (Entry<String, Map<String, VariableBinding>> entry : mibMap.entrySet()) {
				String rowNo = entry.getKey();
				Class beanType = table.getBeanType();
				EndPoint genericBean = null;
				if (appendsTable != null) {
					genericBean = (EndPoint) tablesMap.get(appendsTable).get(rowNo);
				} else {
					try {
						genericBean = (EndPoint) beanType.newInstance();
					} catch (Exception e) {
						log.error("Failed", e);
					}
					genericBeansList.add(genericBean);
				}

				if (genericBean != null) {
					if (table.getMetricFamilyIds() != null && table.getMetricFamilyIds().size() != 0) {
						genericBean.setMetricFamilyIds(table.getMetricFamilyIds());
					}
					if (table.getTrapIds() != null && table.getTrapIds().size() != 0) {
						genericBean.setSyncTrapIds(table.getTrapIds());
					}
					for (Property prop : properties) {
						Map<String, VariableBinding> map = mibMap.get(rowNo);

						DataTypes dataType = prop.getDataType();
						PropertyFetchType fetchType = prop.getFetchType();
						String suffix = null;
						for (SnmpTablecolumn column : table.getColumns()) {
							if (column.getName().equals(prop.getColumn())) {
							    if(column.isRequired())  suffix = column.getSuffix();
							    else  suffix = getFallBackColumnSuffix(table.getColumns(), column.getFallbackColumn());
								break;
							}
						}

						String name = prop.getName();
						if (map != null && map.size() != 0) {
							VariableBinding vb = map.get(prop.getColumn());
							String value = null;
							if (fetchType.equals(PropertyFetchType.OID)) {
								if (vb != null)
									value = vb.getOid().toString();
							} else if (fetchType.equals(PropertyFetchType.INDEX)) {
								    String propColumn = prop.getColumn();
								    if(propColumn.contains("{")){
                                        String column = propColumn.substring(propColumn.indexOf("{") + 1,
                                                        propColumn.indexOf("}"));
                                        String text = propColumn.substring(0, propColumn.indexOf("{"));
                                        VariableBinding varBind = map.get(column);
                                        if (varBind != null) {
                                            for (SnmpTablecolumn tablecolumn : table.getColumns()) {
                                                if(tablecolumn.getName().equals(column)){
                                                    if(tablecolumn.isRequired())  suffix = tablecolumn.getSuffix();
                                                    else  suffix = getFallBackColumnSuffix(table.getColumns(), tablecolumn.getFallbackColumn());
                                                    break;
                                                }
                                            }
                                             String index = varBind.getOid().toString().replace(table.getOid() + "." + suffix + ".", "");
                                             value = text + index;
                                        }
								    }else{
								        if (vb != null) value = vb.getOid().toString().replace(table.getOid() + "." + suffix + ".", "");
								    }
							} else if (fetchType.equals(PropertyFetchType.COLUMN)) {
								if (vb != null)
									value = vb.getVariable().toString();
							} else if (fetchType.equals(PropertyFetchType.EXPLICIT)) {
								value = prop.getColumn();
							} else if (fetchType.equals(PropertyFetchType.STRING_TEMPLATE)) {
								String propColumn = prop.getColumn();
                                                                log.info(tracePrefix + "Column is: " + propColumn + ", Map is: " + map);
                                                                if ( propColumn.contains("{") ) {
                                                                    String column = propColumn.substring(propColumn.indexOf("{") + 1,
                                                                                    propColumn.indexOf("}"));
                                                                    String text = propColumn.substring(0, propColumn.indexOf("{"));
                                                                    VariableBinding binding = map.get(column);
                                                                    if (binding != null) {
									value = text + binding.getVariable().toString();
                                                                    }
                                                                } else {
                                                                    VariableBinding binding = map.get(propColumn);
                                                                    if ( binding != null ) {
                                                                        value = binding.getVariable().toString();
                                                                    }
                                                                }
								
							}

							Object objVal = getObjectValueBasedOnDataType(dataType, value);
//							log.info(tracePrefix + "Property name: " + name + ", Value: " + objVal + ", DataType: " + dataType + ", ObjValClass: " + (objVal != null ? objVal.getClass() : "NULL"));
							if ( value != null ) {
								genericBean.setProperty(name, objVal);
							}
						}
					}
					rowGenericBeanMap.put(rowNo + "", genericBean);
					// genericBean.logAllProperties();
				}

			}
			tablesMap.put(table.getName(), rowGenericBeanMap);
		}

		return genericBeansList;
	}

	private String getFallBackColumnSuffix(List<SnmpTablecolumn> tableColumns, String fallbackColumn) {
		String fallbackColumnSuffix = null;
		for (SnmpTablecolumn stc : tableColumns) {
			if (stc.getId().equals(fallbackColumn)) {
				fallbackColumnSuffix = stc.getSuffix();
				break;
			}
		}
		return fallbackColumnSuffix;
	}

	private List<String> getTableOIDandSuffix(SimpleExpression sExp) {
		String oid = sExp.getOperand();
		String suffix = null;
		String[] splits = oid.split("\\.");
		int increment = 0;
		for (String s : splits) {
			if ((splits.length - 1) == increment) {
				suffix = splits[increment];
			}
			increment++;
		}
		String tableOid = oid.substring(0, (oid.length() - suffix.length()) - 1);
		List<String> list = new ArrayList<String>();
		list.add(suffix);
		list.add(tableOid);
		return list;
	}

	private String getTableOidFromEndPoint(String oid) {
		String suffix = null;
		String[] splits = oid.split("\\.");
		int increment = 0;
		String tableOID = "";
		for (String s : splits) {
			if ((splits.length - 2) == increment) {
				suffix = splits[increment];
				break;
			} else {
				tableOID += splits[increment] + ".";
			}
			increment++;
		}
		String tOid = tableOID.substring(0, tableOID.length() - 1);
		return tOid;
	}

	private void createIndexMapwithMibValues(SimpleExpression sExp, Target target, Map<String, List<String>> indexMap,
			int count) {
		String oid = sExp.getOperand();
		String operandType = sExp.getOperandType();
		String expectedOutput = (String) sExp.getValue();

		List<String> tableOIDList = getTableOIDandSuffix(sExp);
		String suffix = null;
		String tableOid = null;
		if (tableOIDList != null && tableOIDList.size() != 0) {
			suffix = tableOIDList.get(0);
			tableOid = tableOIDList.get(1);
		}

		List<? extends VariableBinding> list = poller.getResponseVariableBindings(tableOid, suffix, target);
		if (list != null && list.size() != 0) {
			for (VariableBinding vb : list) {
				String index = vb.getOid().toString().replace(oid + ".", "");
				String value = vb.getVariable().toString();
				if (expectedOutput.equals(value)) {
					if (indexMap.containsKey(index)) {
						List<String> valuesList = indexMap.get(index);
						valuesList.add(count, value);
					} else {
						List<String> valuesList = new ArrayList<String>();
						valuesList.add(count, value);
						indexMap.put(index, valuesList);
					}
				} else {
					if (indexMap.containsKey(index)) {
						List<String> valuesList = indexMap.get(index);
						valuesList.add(count, null);
					} else {
						List<String> valuesList = new ArrayList<String>();
						valuesList.add(count, null);
						indexMap.put(index, valuesList);
					}
				}
			}
		}
	}

	private static final OID entPhysicalContainedIn = new OID("1.3.6.1.2.1.47.1.1.1.1.4");
	private static final OID entPhysicalClass = new OID("1.3.6.1.2.1.47.1.1.1.1.5");
	private static final OID entPhysicalParentRelPos = new OID("1.3.6.1.2.1.47.1.1.1.1.6");
	private static final OID entPhysicalName = new OID("1.3.6.1.2.1.47.1.1.1.1.7");

	private Map<String, String> getPhysicalContainedIn(Target target) {
		List<VariableBinding> result = poller.getResponseVariableBindings(entPhysicalContainedIn.toString(), "", target);
		Map<String, String> resultMap = new HashMap<>();
		for(VariableBinding vb : result) {
			String index = vb.getOid().toString().replace(entPhysicalContainedIn +".", "");
			resultMap.put(index, vb.getVariable().toString());
		}
		return resultMap;
	}

	private Map<String, String> getPhyscialClass(Target target) {
		List<VariableBinding> result = poller.getResponseVariableBindings(entPhysicalClass.toString(), "", target);
		Map<String, String> resultMap = new HashMap<>();
		for(VariableBinding vb : result) {
			String index = vb.getOid().toString().replace(entPhysicalClass +".", "");
			resultMap.put(index, vb.getVariable().toString());
		}
		return resultMap;
	}

	private Map<String, String> getPhysicalParentRelPos(Target target) {
		List<VariableBinding> result = poller.getResponseVariableBindings(entPhysicalParentRelPos.toString(), "", target);
		Map<String, String> resultMap = new HashMap<>();
		for(VariableBinding vb : result) {
			String index = vb.getOid().toString().replace(entPhysicalParentRelPos +".", "");
			resultMap.put(index, vb.getVariable().toString());
		}
		return resultMap;
	}

	private Map<String, String> getPhysicalNames(Target target) {
		List<VariableBinding> result = poller.getResponseVariableBindings(entPhysicalName.toString(), "", target);
		Map<String, String> resultMap = new HashMap<>();
		for(VariableBinding vb : result) {
			String index = vb.getOid().toString().replace(entPhysicalName +".", "");
			resultMap.put(vb.getVariable().toString(), index);
		}
		return resultMap;
	}

	public DeviceEntity fetchRelationships(NetworkElement ne, List<EndPoint> endPointsList, Target target) {
		Map<String, String> containedInMap = getPhysicalContainedIn(target);
		Map<String, String> physicalClassMap = getPhyscialClass(target);
		Map<String, String> parentRelPosMap = getPhysicalParentRelPos(target);
		Map<String, String> nameToIndexMap = getPhysicalNames(target);
		Map<String, String> indexToNameMap = MapUtils.invertMap(nameToIndexMap);

		log.info("Contained In Map: {}", containedInMap);
		log.info("Physical class Map: {}", physicalClassMap);
		log.info("Parent rel pos Map: {}", parentRelPosMap);
		log.info("Physical name Map: {}", nameToIndexMap);

		Map<String, EndPoint> endpointMap = new HashMap<>();
		endPointsList.forEach(ep -> {
			endpointMap.put(ep.getDescription(), ep);
		});

		DeviceEntity root = new DeviceEntity();
		root.setId(0);
		root.setName(ne.getName());
		root.setPosition(0);
		Map<String, DeviceShelf> shelvesMap = new HashMap<>();
		Map<String, DeviceSlot> slotsMap = new HashMap<>();
		Map<String, DeviceCard> cardsMap = new HashMap<>();
		Map<String, DevicePort> portsMap = new HashMap<>();
		for (Map.Entry<String, String> entry : nameToIndexMap.entrySet()) {
			String epName = entry.getKey();
			String index = entry.getValue();
			EndPoint endPoint = new EndPoint();
			if ( endpointMap.containsKey(epName) ) {
				endPoint = endpointMap.get(epName);
			} else {
				endPoint.setDescription(epName);
			}
			log.debug("Endpoint: {}, SourceName: {}, SourceID: {}, EndPointIndex: {}", endPoint.getDescription(), endPoint.getSourceName(), endPoint.getSourceId(), endPoint.getEndPointIndex());
			if ( nameToIndexMap.containsKey(endPoint.getDescription()) || nameToIndexMap.containsKey(endPoint.getSourceName()) ) {
				index = nameToIndexMap.get(endPoint.getDescription());
				if ( index == null ) {
					log.info("{}Fetching the index using source name: {}", tracePrefix, endPoint.getSourceName());
					index = nameToIndexMap.get(endPoint.getSourceName());
				}
				String containedIn = containedInMap.get(index);
				String phyClass = physicalClassMap.get(index);
				String classStr = convertToClassString(phyClass);
				String relPos = parentRelPosMap.get(index);
				String parentName = indexToNameMap.get(containedIn);
				String parentClass = physicalClassMap.get(containedIn);
				if ( parentClass == null || parentName == null ) {
					continue;
				}
				log.debug("Contained In: {}, Parent Name: {}, Parent Physical class: {}, Rel pos: {}", containedIn, parentName, convertToClassString(parentClass), relPos);
				endPoint.setPosition(Integer.parseInt(relPos));
				endPoint.setPhysicalClass(classStr);

				if ( StringUtils.equalsIgnoreCase(classStr, "stack") ) {
					DeviceShelf shelfObj = new DeviceShelf();
					shelfObj.setId(0);
					shelfObj.setName(indexToNameMap.get(index));
					shelfObj.setPosition(relPos);
					shelvesMap.put(index, shelfObj);
				}

				if ( StringUtils.equalsIgnoreCase(classStr, "chassis") || StringUtils.equalsIgnoreCase(classStr, "container") ) {
					DeviceSlot slotObj = new DeviceSlot();
					slotObj.setId(0);
					slotObj.setName(indexToNameMap.get(index));
					slotObj.setPosition(Integer.parseInt(relPos));
					slotObj.setParentId(containedIn);
					slotsMap.put(index, slotObj);
				}

				if ( StringUtils.equalsIgnoreCase(classStr, "module") ) {
					DeviceCard cardObj = new DeviceCard();
					cardObj.setId(0);
					cardObj.setName(indexToNameMap.get(index));
					cardObj.setParentId(containedIn);
					cardsMap.put(index, cardObj);
				}

				if ( StringUtils.equalsIgnoreCase(classStr, "port") ) {
					DevicePort portObj = new DevicePort();
					portObj.setId(0);
					portObj.setName(endPoint.getDescription());
					portObj.setPortSpeed(endPoint.getSpeed());
					portObj.setPortType(endPoint.getType().toString());
					portObj.setAdminState(endPoint.getAdminStatus().toString());
					portObj.setOpState(endPoint.getOperStatus().toString());
					portObj.setUtilizedBandwidth(endPoint.getUtilPercentage() + "");
					portObj.setPositionOnCard(relPos);
					portObj.setParentId(containedIn);
					portsMap.put(index, portObj);
				}

			} else {
				log.error("This end point not present in the entity mib ...");
			}
		}

		if (shelvesMap.isEmpty()) {
			DeviceShelf shelfObj = new DeviceShelf();
			shelfObj.setId(0);
			shelfObj.setName("Shelf 1");
			shelfObj.setPosition("0");
			shelvesMap.put("1", shelfObj);
		}

		for ( DevicePort port : portsMap.values() ) {
			if ( cardsMap.containsKey(port.getParentId()) ) {
				DeviceCard card = cardsMap.get(port.getParentId());
				card.getPorts().add(port);
				log.debug("Adding the port {} to the card: {}", port.getName(), card.getName());
			} else {
				log.error("Port with name: {} has no parent: {}, Card keys: {}", port.getName(), port.getParentId(), cardsMap.keySet());
			}
		}

		for ( DeviceCard card : cardsMap.values() ) {
			if ( slotsMap.containsKey(card.getParentId()) ) {
				DeviceSlot slot = slotsMap.get(card.getParentId());
				slot.getCards().add(card);
				log.debug("Adding the card {} to the slot: {}", card.getName(), slot.getName());
			} else {
				log.error("Card with name: {} has no parent: {}, Slot keys: {}", card.getName(), card.getParentId(), slotsMap.keySet());
			}
		}

		for ( DeviceSlot slot : slotsMap.values() ) {
			if ( shelvesMap.containsKey(slot.getParentId()) ) {
				DeviceShelf shelf = shelvesMap.get(slot.getParentId());
				shelf.getSlots().add(slot);
				log.debug("Adding the slot {} to the shelf: {}", slot.getName(), shelf.getName());
			} else {
				log.error("Slot with name: {} has no parent: {}, Shelf keys: {}", slot.getName(), slot.getParentId(), shelvesMap.keySet());
			}
		}

		root.setShelves(new ArrayList<>(shelvesMap.values()));

		StringBuilder sb = new StringBuilder();
		for ( DeviceShelf shelf : root.getShelves() ) {
			sb.append(shelf.getName()).append(", Position: ").append(shelf.getPosition()).append("\n");
			for ( DeviceSlot slot : shelf.getSlots() ) {
				sb.append("|__").append(slot.getName()).append(", Position: ").append(slot.getPosition()).append("\n");
				for ( DeviceCard card : slot.getCards() ) {
					sb.append("|____").append(card.getName()).append(", Position: ").append(card.getSlotPosition()).append("\n");
					for ( DevicePort port : card.getPorts() ) {
						sb.append("|______").append(port.getName()).append(", Position: ").append(port.getPositionOnCard()).append("\n");
					}
				}
			}
		}
		log.debug("Slots, Cards: \n\n{}", sb);
		return root;
	}

	private String convertToClassString(String classStr) {
		int classInt = Integer.parseInt(classStr);
		if ( classInt == 1 ) {
			return "other";
		} else if ( classInt == 2 ) {
			return "unknown";
		} else if ( classInt == 3 ) {
			return "chassis";
		} else if ( classInt == 4 ) {
			return "backplane";
		} else if ( classInt == 5 ) {
			return "container";
		} else if ( classInt == 6 ) {
			return "powerSupply";
		} else if ( classInt == 7 ) {
			return "fan";
		} else if ( classInt == 8 ) {
			return "sensor";
		} else if ( classInt == 9 ) {
			return "module";
		} else if ( classInt == 10 ) {
			return "port";
		} else if ( classInt == 11 ) {
			return "stack";
		} else if ( classInt == 12 ) {
			return "cpu";
		} else if ( classInt == 13 ) {
			return "energyObject";
		} else if ( classInt == 14 ) {
			return "battery";
		} else if ( classInt == 15 ) {
			return "storageDrive";
		}
		return "None";
	}
}
