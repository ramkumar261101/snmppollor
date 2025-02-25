package ai.netoai.collector.snmp;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;

public class PerfPollValue implements Serializable {
	
	private Double value;
	private long timeStamp;
	
	/**
	 * 
	 */
	public PerfPollValue() {
		super();
	}
	
	/**
         * Creates a PerfPollValue instance
	 * @param value the metric value received
	 * @param timeStamp the timestamp of the request
	 */
	public PerfPollValue(Double value, long timeStamp) {
		super();
		this.value = value;
		this.timeStamp = timeStamp;
	}
	
	public Double getValue() {
		return value;
	}
	
	public void setValue(Double value) {
		this.value = value;
	}
	
	public long getTimeStamp() {
		return timeStamp;
	}
	
	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (timeStamp ^ (timeStamp >>> 32));
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PerfPollValue other = (PerfPollValue) obj;
		if (timeStamp != other.timeStamp)
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "PerfPollValue [value=" + value + ", timeStamp=" + Instant.ofEpochMilli(timeStamp).atZone(ZoneId.systemDefault()).toString()
				+ "]";
	}
	
}
