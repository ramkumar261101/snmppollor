package ai.netoai.collector.model;

import java.util.ArrayList;
import java.util.List;

public class DeviceEntity {

    private int id;
    private String name;
    private int position;
    private List<DeviceShelf> shelves = new ArrayList<>();

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

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }


    public List<DeviceShelf> getShelves() {
        return shelves;
    }

    public void setShelves(List<DeviceShelf> shelves) {
        this.shelves = shelves;
    }
}
