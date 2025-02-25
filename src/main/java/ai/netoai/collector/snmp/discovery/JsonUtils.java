package ai.netoai.collector.snmp.discovery;

import ai.netoai.collector.model.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonUtils {

    private static final Logger log = LoggerFactory.getLogger(JsonUtils.class);

    public static String convertToJson(NetworkElement ne, List<EndPoint> endPoints, DeviceEntity root) {
        JsonArray placeObjs = new JsonArray();
        JsonObject placeObj = new JsonObject();
        placeObj.add("id", null);
        placeObj.add("name", null);
        placeObj.addProperty("clliCode", ne.getClliCode());
        placeObj.addProperty("phoneNumber", ne.getPhoneNumber());
        placeObj.addProperty("contactPerson", ne.getContactPerson());
        placeObj.addProperty("address", ne.getAddress());
        placeObj.addProperty("latitude", ne.getLatitude());
        placeObj.addProperty("longitude", ne.getLongitude());
        placeObj.addProperty("drivingInstructions", ne.getDrivingInstructions());
        placeObj.add("href", null);
        placeObj.add("notes", null);
        placeObj.add("additionalAttributes", null);
        placeObjs.add(placeObj);
        log.info("Place Obj: {}", placeObj.toString());

        JsonObject networkElementJson = new JsonObject();
        networkElementJson.add("view", null);
        networkElementJson.add("id", null);
        networkElementJson.addProperty("name", ne.getName());
        networkElementJson.add("@type", null);
        networkElementJson.add("isBundle", null);
        networkElementJson.add("place", placeObjs);
        JsonObject serialnum = new JsonObject();
        serialnum.add("serialNumber", null);
        JsonObject managementIp = new JsonObject();
        managementIp.addProperty("managementIp", ne.getIp());
        JsonObject rackPosition = new JsonObject();
        rackPosition.add("rackPosition", null);
        JsonObject deviceModel = new JsonObject();
        deviceModel.addProperty("deviceModel", ne.getProductName());
        JsonArray resourceChars = new JsonArray();
        resourceChars.add(serialnum);
        resourceChars.add(managementIp);
        resourceChars.add(rackPosition);
        resourceChars.add(deviceModel);
        networkElementJson.add("resourceCharacteristics", resourceChars);
        networkElementJson.addProperty("operationalState", ne.getStatus());
        networkElementJson.add("administrativeState", null);
        networkElementJson.add("usageState", null);

        Map<String, EndPoint> devicePortsMap = new LinkedHashMap<>();
        endPoints.forEach(ep -> {
            if ( ep.getType() == EndPoint.Type.ethernetCsmacd ) {
                devicePortsMap.put(ep.getDescription(), ep);
            }
        });

        JsonArray shelves = new JsonArray();
        for (DeviceShelf shelf : root.getShelves()) {
            JsonObject shelveObj = new JsonObject();
            shelveObj.add("id", null);
            shelveObj.addProperty("name", shelf.getName());
            shelveObj.addProperty("position", shelf.getPosition());
            shelveObj.add("operationalState", null);
            shelveObj.add("administrativeState", null);
            shelveObj.add("href", null);
            shelveObj.add("usageState", null);
            shelveObj.add("view", null);

            JsonArray slotsArray = new JsonArray();
            shelveObj.add("slots", slotsArray);
            for ( DeviceSlot slot : shelf.getSlots() ) {
                JsonObject slotObj = new JsonObject();
                slotObj.add("id", null);
                slotObj.addProperty("name", slot.getName());
                slotObj.addProperty("slotPosition", slot.getPosition());
                slotObj.add("operationalState", null);
                slotObj.add("administrativeState", null);
                slotObj.add("href", null);
                slotObj.add("usageState", null);
                slotObj.add("view", null);
                slotsArray.add(slotObj);

                JsonArray cardsArray = new JsonArray();
                slotObj.add("cards", cardsArray);
                for ( DeviceCard card : slot.getCards() ) {
                    JsonObject cardObj = new JsonObject();
                    cardObj.add("id", null);
                    cardObj.addProperty("name", card.getName());
                    cardObj.addProperty("shelfPosition", shelf.getPosition());
                    cardObj.addProperty("slotPosition", slot.getPosition());
                    cardObj.add("operationalState", null);
                    cardObj.add("administrativeState", null);
                    cardObj.add("href", null);
                    cardObj.add("usageState", null);
                    cardObj.add("view", null);
                    cardsArray.add(cardObj);

                    JsonArray cardSlotsArray = new JsonArray();
                    cardObj.add("cardSlots", cardSlotsArray);
                    JsonObject cardSlotObj = new JsonObject();
                    cardSlotObj.add("id", null);
                    cardSlotObj.addProperty("name", slot.getName() + "_" + card.getName());
                    cardSlotObj.addProperty("slotPosition", slot.getPosition());
                    cardSlotObj.add("operationalState", null);
                    cardSlotObj.add("administrativeState", null);
                    cardSlotObj.add("usageState", null);
                    cardSlotObj.add("href", null);
                    cardSlotObj.add("view", null);
                    cardSlotsArray.add(cardSlotObj);
                    JsonArray portsArray = new JsonArray();
                    cardSlotObj.add("ports", portsArray);
                    for ( DevicePort port : card.getPorts() ) {
                        JsonObject portObj = new JsonObject();
                        portObj.add("id", null);
                        portObj.addProperty("name", port.getName());
                        portObj.addProperty("positionOnCard", port.getPositionOnCard());
                        portObj.addProperty("portType", port.getPortType());
                        portObj.addProperty("operationalState", port.getOpState());
                        portObj.addProperty("administrativeState", port.getAdminState());
                        portObj.add("usageState", null);
                        portObj.addProperty("portSpeed", port.getPortSpeed());
                        portObj.addProperty("utilizedBandwidth", port.getUtilizedBandwidth());
                        portObj.addProperty("capacity", port.getPortSpeed());
                        portsArray.add(portObj);
                        if (devicePortsMap.containsKey(port.getName()) ) {
                            devicePortsMap.remove(port.getName());
                        }
                    }
                }
            }

            shelves.add(shelveObj);
        }
        networkElementJson.add("shelves", shelves);

        JsonArray devicePorts = new JsonArray();

        log.info("Device Ports: {}", devicePortsMap);
        for (Map.Entry<String, EndPoint> entry : devicePortsMap.entrySet()) {
//            if ( endPoints.get(i).getType() == null || !endPoints.get(i).getType().toString().equalsIgnoreCase("ethernetCsmacd") ) {
//                continue;
//            }
            JsonObject devicePort = new JsonObject();
            devicePort.add("id", null);
            devicePort.addProperty("name", entry.getValue().getSourceName());
            devicePort.addProperty("positionOnCard", entry.getValue().getPosition());
            devicePort.addProperty("portType", String.valueOf(entry.getValue().getType()));
            devicePort.addProperty("operationalState", String.valueOf(entry.getValue().getOperStatus()));
            devicePort.addProperty("administrativeState", String.valueOf(entry.getValue().getAdminStatus()));
            //  devicePort.addProperty("usageStatus", String.valueOf(endPoints.get(i).getSyncTrapIds()));
            devicePort.add("href", null);
            devicePort.addProperty("portSpeed", entry.getValue().getSpeed());
            devicePort.add("overbookingFactor", null);
            devicePort.addProperty("utilizedBandWidth", String.valueOf(entry.getValue().getUtilPercentage()));
            devicePort.addProperty("capacity", entry.getValue().getSpeed());
            devicePort.add("ip", null);
            devicePort.add("view", null);
            devicePort.add("downTime", null);
            devicePort.add("logicalPorts", null);
            devicePort.add("additionalAttributes", null);
            log.info("Network element: {}, Device port: {}", ne.getName(), devicePort.get("name").getAsString());
            devicePorts.add(devicePort);
        }
        networkElementJson.add("devicePorts", devicePorts);
        networkElementJson.add("devicePluggables", null);

        JsonObject resourceSpecification = new JsonObject();
        resourceSpecification.add("id", null);
        resourceSpecification.add("name",null);
        resourceSpecification.add("href",null);
        resourceSpecification.add("@referredType",null);
        networkElementJson.add("resourceSpecification", resourceSpecification);

        JsonObject relatedParty = new JsonObject();
        JsonObject rack = new JsonObject();
        rack.add("id",null);
        rack.add("name",null);
        rack.add("location",null);
        rack.add("height",null);
        rack.add("width",null);
        rack.add("depth",null);
        rack.add("numOfPositionsContained",null);
        rack.add("reservedPositions",null);
        rack.add("freePositions",null);
        rack.add("href",null);
        rack.add("notes",null);
        rack.add("view",null);
        rack.add("additionalAttributes",null);
        relatedParty.add("rack", rack);
        networkElementJson.add("relatedParty", relatedParty);
        networkElementJson.add("additionalAttributes",null);
        String networkElementString = networkElementJson.toString();
//        log.info("jsonString" + networkElementString);
        return networkElementString;
    }

}
