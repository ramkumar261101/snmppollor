package ai.netoai.collector.model;



public class PhysicalLink extends GenericJavaBean {

    private String sourceNodeIP;
    private String sourcePort;
    private String sourceNodeName;
    private String destinationNodeIP;
    private String destinationPort;
    private String destinationNodeName;
    
    public PhysicalLink() {
        setBeanType(BeanType.PHYSICALLINK);
    }
    
    public String getSourceNodeIP() {
        return sourceNodeIP;
    }
    
    public void setSourceNodeIP(String sourceNodeIP) {
        this.sourceNodeIP = sourceNodeIP;
    }
    
    public String getSourcePort() {
        return sourcePort;
    }

    public void setSourcePort(String sourcePort) {
        this.sourcePort = sourcePort;
    }

    public String getDestinationNodeIP() {
        return destinationNodeIP;
    }

    public void setDestinationNodeIP(String destinationNodeIP) {
        this.destinationNodeIP = destinationNodeIP;
    }

    public String getDestinationPort() {
        return destinationPort;
    }

    public void setDestinationPort(String destinationPort) {
        this.destinationPort = destinationPort;
    }

    public String getSourceNodeName() {
        return sourceNodeName;
    }

    public void setSourceNodeName(String sourceNodeName) {
        this.sourceNodeName = sourceNodeName;
    }

    public String getDestinationNodeName() {
        return destinationNodeName;
    }

    public void setDestinationNodeName(String destinationNodeName) {
        this.destinationNodeName = destinationNodeName;
    }

    @Override
    public String toString() {
        return "["+sourceNodeIP+","+sourcePort+","+destinationNodeIP+","+destinationPort+"]";
    }
    
}
