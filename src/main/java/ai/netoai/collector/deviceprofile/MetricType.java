package ai.netoai.collector.deviceprofile;

public enum MetricType {

    GAUGE,
    COUNTER;

    public static MetricType asEnum(String str) {
        if ( str == null || str.trim().isEmpty() ) {
            return null;
        }
        switch(str.toLowerCase()) {
            case "gauge":
                return GAUGE;
            case "counter":
                return COUNTER;
            default:
                return null;
        }
    }
}
