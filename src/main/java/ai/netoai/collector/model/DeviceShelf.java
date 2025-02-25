package ai.netoai.collector.model;

import java.util.ArrayList;
import java.util.List;

public class DeviceShelf {

    private int id;
    private String name;
    private String position;
    private List<DeviceSlot> slots = new ArrayList<>();

    public List<DeviceSlot> getSlots() {
        return slots;
    }

    public void setSlots(List<DeviceSlot> slots) {
        this.slots = slots;
    }

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

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }
}
