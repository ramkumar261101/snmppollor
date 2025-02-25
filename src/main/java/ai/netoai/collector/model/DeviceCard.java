package ai.netoai.collector.model;

import java.util.ArrayList;
import java.util.List;

public class DeviceCard {

    private int id;
    private String name;
    private int slotPosition;
    private String parentId;
    private List<DevicePort> ports = new ArrayList<>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSlotPosition() {
        return slotPosition;
    }

    public void setSlotPosition(int slotPosition) {
        this.slotPosition = slotPosition;
    }

    public List<DevicePort> getPorts() {
        return ports;
    }

    public void setPorts(List<DevicePort> ports) {
        this.ports = ports;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }
}
