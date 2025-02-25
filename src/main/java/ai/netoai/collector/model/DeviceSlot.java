package ai.netoai.collector.model;

import java.util.ArrayList;
import java.util.List;

public class DeviceSlot {

    private int id;
    private String name;
    private int position;
    private String parentId;
    private List<DeviceCard> cards = new ArrayList<>();

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

    public List<DeviceCard> getCards() {
        return cards;
    }

    public void setCards(List<DeviceCard> cards) {
        this.cards = cards;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }
}
