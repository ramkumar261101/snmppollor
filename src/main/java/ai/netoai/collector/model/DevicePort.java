package ai.netoai.collector.model;

public class DevicePort {

    private int id;
    private String name;
    private String positionOnCard;
    private String portType;
    private String opState;
    private String adminState;
    private String portSpeed;
    private String utilizedBandwidth;
    private String parentId;

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

    public String getPositionOnCard() {
        return positionOnCard;
    }

    public void setPositionOnCard(String positionOnCard) {
        this.positionOnCard = positionOnCard;
    }

    public String getPortType() {
        return portType;
    }

    public void setPortType(String portType) {
        this.portType = portType;
    }

    public String getOpState() {
        return opState;
    }

    public void setOpState(String opState) {
        this.opState = opState;
    }

    public String getAdminState() {
        return adminState;
    }

    public void setAdminState(String adminState) {
        this.adminState = adminState;
    }

    public String getPortSpeed() {
        return portSpeed;
    }

    public void setPortSpeed(String portSpeed) {
        this.portSpeed = portSpeed;
    }

    public String getUtilizedBandwidth() {
        return utilizedBandwidth;
    }

    public void setUtilizedBandwidth(String utilizedBandwidth) {
        this.utilizedBandwidth = utilizedBandwidth;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }
}
