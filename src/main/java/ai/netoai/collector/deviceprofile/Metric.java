package ai.netoai.collector.deviceprofile;

import ai.netoai.collector.deviceprofile.MetricFamily.ProtoCol;

import java.util.ArrayList;
import java.util.List;

public class Metric {

    public enum Units {
        PERCENTAGE, BPS, BYTES, DATAGRAMS, OCTETS, DEGREES, NONE;

        public static Units asEnum(String value) {
            if (value.equalsIgnoreCase("percentage")) {
                return PERCENTAGE;
            } else if (value.equalsIgnoreCase("bps")) {
                return BPS;
            } else if (value.equalsIgnoreCase("bytes")) {
                return BYTES;
            } else if (value.equalsIgnoreCase("datagrams")) {
                return DATAGRAMS;
            } else if (value.equalsIgnoreCase("octets")) {
                return OCTETS;
            } else if (value.equalsIgnoreCase("degrees")) {
                return DEGREES;
            } else if (value.equalsIgnoreCase("none")) {
                return NONE;
            }
            return null;
        }
    }

    public enum ConversionFunction {
        PER_POLL_PER_SECOND, PER_POLL, NONE;

        public static ConversionFunction asEnum(String value) {
            if (value.equalsIgnoreCase("per_poll_per_second")) {
                return PER_POLL_PER_SECOND;
            } else if (value.equalsIgnoreCase("per_poll")) {
                return PER_POLL;
            } else if (value.equalsIgnoreCase("none")) {
                return NONE;
            }
            return null;
        }

        @Override
        public String toString() {
            switch (this) {
                case NONE:
                    return "none";
                case PER_POLL:
                    return "per_poll";
                case PER_POLL_PER_SECOND:
                    return "per_poll_per_second";
                default:
                    return "na";
            }
        }

    }

    public enum ConsolidationFunction {
        SUM, AVG, NONE;

        public static ConsolidationFunction asEnum(String value) {
            if (value.equalsIgnoreCase("sum")) {
                return SUM;
            } else if (value.equalsIgnoreCase("avg")) {
                return AVG;
            } else if (value.equalsIgnoreCase("none")) {
                return NONE;
            }
            return null;
        }
    }

    public enum MetricValueType {
        BOOLEAN, DOUBLE, NONE;

        public static MetricValueType asEnum(String value) {
            if (value.equalsIgnoreCase("boolean")) {
                return BOOLEAN;
            } else if (value.equalsIgnoreCase("double")) {
                return DOUBLE;
            } else if (value.equalsIgnoreCase("none")) {
                return NONE;
            }
            return null;
        }
    }

    private String id;
    private String name;
    private String descr;
    private MetricValueType metricValueType;
    private ProtoCol protocol;
    private Units units;
    private ConversionFunction conversionFunction;
    private ConsolidationFunction consolidation;
    private List<Param> paramList = new ArrayList<Param>();
    private String value;
    private String scriptVariables;
    private String evalType;
    private String plotType;
    private String color;
    private MetricType type;

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @return the plotType
     */
    public String getPlotType() {
        return plotType;
    }

    /**
     * @param plotType
     *            the plotType to set
     */
    public void setPlotType(String plotType) {
        this.plotType = plotType;
    }

    /**
     * @return the color
     */
    public String getColor() {
        return color;
    }

    /**
     * @param color
     *            the color to set
     */
    public void setColor(String color) {
        this.color = color;
    }

    /**
     * @param id
     *            the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the descr
     */
    public String getDescr() {
        return descr;
    }

    /**
     * @param descr
     *            the descr to set
     */
    public void setDescr(String descr) {
        this.descr = descr;
    }

    /**
     * @return the protocol
     */
    public ProtoCol getProtocol() {
        return protocol;
    }

    /**
     * @param protocol
     *            the protocol to set
     */
    public void setProtocol(ProtoCol protocol) {
        this.protocol = protocol;
    }

    /**
     * @return the units
     */
    public Units getUnits() {
        return units;
    }

    /**
     * @param units
     *            the units to set
     */
    public void setUnits(Units units) {
        this.units = units;
    }

    /**
     * @return the conversionFunction
     */
    public ConversionFunction getConversionFunction() {
        return conversionFunction;
    }

    /**
     * @param conversionFunction
     *            the conversionFunction to set
     */
    public void setConversionFunction(ConversionFunction conversionFunction) {
        this.conversionFunction = conversionFunction;
    }

    /**
     * @return the consolidation
     */
    public ConsolidationFunction getConsolidation() {
        return consolidation;
    }

    /**
     * @param consolidation
     *            the consolidation to set
     */
    public void setConsolidation(ConsolidationFunction consolidation) {
        this.consolidation = consolidation;
    }

    /**
     * @return the paramList
     */
    public List<Param> getParamList() {
        return paramList;
    }

    /**
     * @param paramList
     *            the paramList to set
     */
    public void setParamList(List<Param> paramList) {
        this.paramList = paramList;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value
     *            the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    public MetricValueType getMetricValueType() {
        return metricValueType;
    }

    public void setMetricValueType(MetricValueType metricValueType) {
        this.metricValueType = metricValueType;
    }

    public String getEvalType() {
        return evalType;
    }

    public void setEvalType(String evalType) {
        this.evalType = evalType;
    }

    public String getScriptVariables() {
        return scriptVariables;
    }

    public void setScriptVariables(String scriptVariables) {
        this.scriptVariables = scriptVariables;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Metric)) {
            return false;
        }
        Metric m = (Metric) o;
        return m.id.equals(id);
    }

    private volatile int hashCode; // (See Item 71)

    @Override
    public int hashCode() {
        int result = hashCode;
        if (result == 0) {
            result = 17;
            result = 31 * result + id.hashCode();
            hashCode = result;
        }
        return result;
    }

    @Override
    public String toString() {
        return this.name + "(" + this.id + ")";
    }

	public MetricType getType() {
		return type;
	}

	public void setType(MetricType type) {
		this.type = type;
	}
}
