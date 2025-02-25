package ai.netoai.collector.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ConfigMessage extends GenericJavaBean {

	private static final long serialVersionUID = 6883624358063073937L;
	
	public enum MsgType {
		START_DISCOVERY,
		STOP_DISCOVERY,
		REDISCOVER_NODE,
		DELETE_NODE,
		HANDSHAKE,
		SETTINGS_MODIFIED,
		ENDPOINT_MODIFY;
	}
	
	private MsgType msgType;
	private List<Serializable> payload;
	
	public ConfigMessage(MsgType msgType, Serializable... args) {
		this.msgType = msgType;
		this.payload = new ArrayList<>();
		for(Serializable arg : args) {
			this.payload.add(arg);
		}
	}

	/**
	 * @return the msgType
	 */
	public MsgType getMsgType() {
		return msgType;
	}

	/**
	 * @param msgType the msgType to set
	 */
	public void setMsgType(MsgType msgType) {
		this.msgType = msgType;
	}

	/**
	 * @return the payload
	 */
	public List<Serializable> getPayload() {
		return payload;
	}

	/**
	 * @param payload the payload to set
	 */
	public void setPayload(List<Serializable> payload) {
		this.payload = payload;
	}
	


}
