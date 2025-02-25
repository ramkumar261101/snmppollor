/**
 * 
 */
package ai.netoai.collector.settings;


import ai.netoai.collector.model.GenericJavaBean;


public class KafkaTopicSettings extends GenericJavaBean {

	public static final String _CATEGORY = "kafkaTopics";
	
	// Names of the topics in the system.
	public static final String COLLECTOR_OUTGOING_TOPIC = "collector_outgoing_topic";
	public static final String COLLECTOR_INCOMING_TOPIC = "collector_incoming_topic";
	public static final String INVENTORY_TOPIC = "inventory_topic";
	public static final String PERF_TOPIC = "perf_topic";
	public static final String FAULT_TOPIC = "fault_topic";
        public static final String INVENTORY_TOPIC_UPDATE = "inventory_topic_update";
        public static final String FAULT_TOPIC_UPDATE = "fault_topic_update";
	
	public static final String COLLECTOR_OG_TOPIC_PART = "collectorOgTopicPart";
	public static final String AGENT_APP_INVENTORY_TOPIC = "agent_app_inv_topic";
	public static final String AGENT_APP_PERF_TOPIC = "agent_app_perf_topic";
	public static final String DK_SNMP_TRAPS = "dk_snmp_traps";
	public static final String RETINA_STREAM_TOPIC = "retina_stream_topic";
	public static final String RETINA_CONFIG_UPDATE_TOPIC = "retina_config_update_topic";
	public static final String RETINA_IMAGE_SERVICE_TOPIC = "retina_image_service_topic";
	public static final String LAMPY_INPUT_TOPIC = "rlampy_input_topic";
	public static final String LAMPY_CONFIG_TOPIC = "rlampy_config_topic";
	public static final String RETINA_EMP_ACCEPTANCE_TOPIC = "emp_acceptance_topic";
	public static final String BOT_CONFIG_TOPIC = "bot_config_topic";
	public static final String BOT_COMMANDS_TOPIC = "bot_commands_topic";
	public static final String BOT_USER_MESSAGES_TOPIC = "bot_user_messages_topic";

	private Integer collectorOgTopicPart = new Integer(1);
	private Integer collectorOgTopicRepl = new Integer(1);
	private Integer collectorOgTopicMinIsr = new Integer(1);
	
	private Integer collectorIcTopicPart = new Integer(1);
	private Integer collectorIcTopicRepl = new Integer(1);
	private Integer collectorIcTopicMinIsr = new Integer(1);
	private Integer collectorIcMsgProcThreads = new Integer(10);
	
	private Integer invTopicPart = new Integer(1);
	private Integer invTopicRepl = new Integer(1);
	private Integer invTopicMinIsr = new Integer(1);
	private Boolean invAsyncSendEnabled = Boolean.FALSE;
	
	private Integer perfTopicPart = new Integer(1);
	private Integer perfTopicRepl = new Integer(1);
	private Integer perfTopicMinIsr = new Integer(1);
	private Boolean perfAsyncSendEnabled = Boolean.FALSE;
	
	private Integer faultTopicPart = new Integer(1);
	private Integer faultTopicRepl = new Integer(1);
	private Integer faultTopicMinIsr = new Integer(1);
	private Boolean faultAsyncSendEnabled = Boolean.FALSE;

	private Integer agentAppInvTopicPart = new Integer(1);
	private Integer agentAppInvTopicRepl = new Integer(1);
	private Integer agentAppInvTopicMinIsr = new Integer(1);

	private Integer agentAppPerfTopicPart = new Integer(1);
	private Integer agentAppPerfTopicRepl = new Integer(1);
	private Integer agentAppPerfTopicMinIsr = new Integer(1);

	private Integer retinaStreamTopicPart = new Integer(1);
	private Integer retinaStreamTopicRepl = new Integer(1);
	private Integer retinaStreamTopicMinIsr = new Integer(1);
        
        private Integer retinaConfigUpdateTopicPart = new Integer(1);
        private Integer retinaConfigUpdateTopicRepl = new Integer(1);
        private Integer retinaConfigUpdateTopicMinIsr = new Integer(1);
	
	private Integer retinaImageServiceTopicPart = new Integer(1);
	private Integer retinaImageServiceTopicRepl = new Integer(1);
	private Integer retinaImageServiceTopicMinIsr = new Integer(1);

	/**
	 * @return the collectorOgTopicPart
	 */
	public Integer getCollectorOgTopicPart() {
		return collectorOgTopicPart;
	}
	/**
	 * @param collectorOgTopicPart the collectorOgTopicPart to set
	 */
	public void setCollectorOgTopicPart(Integer collectorOgTopicPart) {
		this.collectorOgTopicPart = collectorOgTopicPart;
	}
	/**
	 * @return the collectorOgTopicRepl
	 */
	public Integer getCollectorOgTopicRepl() {
		return collectorOgTopicRepl;
	}
	/**
	 * @param collectorOgTopicRepl the collectorOgTopicRepl to set
	 */
	public void setCollectorOgTopicRepl(Integer collectorOgTopicRepl) {
		this.collectorOgTopicRepl = collectorOgTopicRepl;
	}
	/**
	 * @return the collectorOgTopicMinIsr
	 */
	public Integer getCollectorOgTopicMinIsr() {
		return collectorOgTopicMinIsr;
	}
	/**
	 * @param collectorOgTopicMinIsr the collectorOgTopicMinIsr to set
	 */
	public void setCollectorOgTopicMinIsr(Integer collectorOgTopicMinIsr) {
		this.collectorOgTopicMinIsr = collectorOgTopicMinIsr;
	}
	/**
	 * @return the collectorIcTopicPart
	 */
	public Integer getCollectorIcTopicPart() {
		return collectorIcTopicPart;
	}
	/**
	 * @param collectorIcTopicPart the collectorIcTopicPart to set
	 */
	public void setCollectorIcTopicPart(Integer collectorIcTopicPart) {
		this.collectorIcTopicPart = collectorIcTopicPart;
	}
	/**
	 * @return the collectorIcTopicRepl
	 */
	public Integer getCollectorIcTopicRepl() {
		return collectorIcTopicRepl;
	}
	/**
	 * @param collectorIcTopicRepl the collectorIcTopicRepl to set
	 */
	public void setCollectorIcTopicRepl(Integer collectorIcTopicRepl) {
		this.collectorIcTopicRepl = collectorIcTopicRepl;
	}
	/**
	 * @return the collectorIcTopicMinIsr
	 */
	public Integer getCollectorIcTopicMinIsr() {
		return collectorIcTopicMinIsr;
	}
	/**
	 * @param collectorIcTopicMinIsr the collectorIcTopicMinIsr to set
	 */
	public void setCollectorIcTopicMinIsr(Integer collectorIcTopicMinIsr) {
		this.collectorIcTopicMinIsr = collectorIcTopicMinIsr;
	}
	/**
	 * @return the invTopicPart
	 */
	public Integer getInvTopicPart() {
		return invTopicPart;
	}
	/**
	 * @param invTopicPart the invTopicPart to set
	 */
	public void setInvTopicPart(Integer invTopicPart) {
		this.invTopicPart = invTopicPart;
	}
	/**
	 * @return the invTopicRepl
	 */
	public Integer getInvTopicRepl() {
		return invTopicRepl;
	}
	/**
	 * @param invTopicRepl the invTopicRepl to set
	 */
	public void setInvTopicRepl(Integer invTopicRepl) {
		this.invTopicRepl = invTopicRepl;
	}
	/**
	 * @return the invTopicMinIsr
	 */
	public Integer getInvTopicMinIsr() {
		return invTopicMinIsr;
	}
	/**
	 * @param invTopicMinIsr the invTopicMinIsr to set
	 */
	public void setInvTopicMinIsr(Integer invTopicMinIsr) {
		this.invTopicMinIsr = invTopicMinIsr;
	}
	/**
	 * @return the perfTopicPart
	 */
	public Integer getPerfTopicPart() {
		return perfTopicPart;
	}
	/**
	 * @param perfTopicPart the perfTopicPart to set
	 */
	public void setPerfTopicPart(Integer perfTopicPart) {
		this.perfTopicPart = perfTopicPart;
	}
	/**
	 * @return the perfTopicRepl
	 */
	public Integer getPerfTopicRepl() {
		return perfTopicRepl;
	}
	/**
	 * @param perfTopicRepl the perfTopicRepl to set
	 */
	public void setPerfTopicRepl(Integer perfTopicRepl) {
		this.perfTopicRepl = perfTopicRepl;
	}
	/**
	 * @return the perfTopicMinIsr
	 */
	public Integer getPerfTopicMinIsr() {
		return perfTopicMinIsr;
	}
	/**
	 * @param perfTopicMinIsr the perfTopicMinIsr to set
	 */
	public void setPerfTopicMinIsr(Integer perfTopicMinIsr) {
		this.perfTopicMinIsr = perfTopicMinIsr;
	}
	/**
	 * @return the faultTopicPart
	 */
	public Integer getFaultTopicPart() {
		return faultTopicPart;
	}
	/**
	 * @param faultTopicPart the faultTopicPart to set
	 */
	public void setFaultTopicPart(Integer faultTopicPart) {
		this.faultTopicPart = faultTopicPart;
	}
	/**
	 * @return the faultTopicRepl
	 */
	public Integer getFaultTopicRepl() {
		return faultTopicRepl;
	}
	/**
	 * @param faultTopicRepl the faultTopicRepl to set
	 */
	public void setFaultTopicRepl(Integer faultTopicRepl) {
		this.faultTopicRepl = faultTopicRepl;
	}
	/**
	 * @return the faultTopicMinIsr
	 */
	public Integer getFaultTopicMinIsr() {
		return faultTopicMinIsr;
	}
	/**
	 * @param faultTopicMinIsr the faultTopicMinIsr to set
	 */
	public void setFaultTopicMinIsr(Integer faultTopicMinIsr) {
		this.faultTopicMinIsr = faultTopicMinIsr;
	}
	/**
	 * @return the invAsyncSendEnabled
	 */
	public Boolean getInvAsyncSendEnabled() {
		return invAsyncSendEnabled;
	}
	/**
	 * @param invAsyncSendEnabled the invAsyncSendEnabled to set
	 */
	public void setInvAsyncSendEnabled(Boolean invAsyncSendEnabled) {
		this.invAsyncSendEnabled = invAsyncSendEnabled;
	}
	/**
	 * @return the perfAsyncSendEnabled
	 */
	public Boolean getPerfAsyncSendEnabled() {
		return perfAsyncSendEnabled;
	}
	/**
	 * @param perfAsyncSendEnabled the perfAsyncSendEnabled to set
	 */
	public void setPerfAsyncSendEnabled(Boolean perfAsyncSendEnabled) {
		this.perfAsyncSendEnabled = perfAsyncSendEnabled;
	}
	/**
	 * @return the faultAsyncSendEnabled
	 */
	public Boolean getFaultAsyncSendEnabled() {
		return faultAsyncSendEnabled;
	}
	/**
	 * @param faultAsyncSendEnabled the faultAsyncSendEnabled to set
	 */
	public void setFaultAsyncSendEnabled(Boolean faultAsyncSendEnabled) {
		this.faultAsyncSendEnabled = faultAsyncSendEnabled;
	}
	/**
	 * @return the collectorIcMsgProcThreads
	 */
	public Integer getCollectorIcMsgProcThreads() {
		return collectorIcMsgProcThreads;
	}
	/**
	 * @param collectorIcMsgProcThreads the collectorIcMsgProcThreads to set
	 */
	public void setCollectorIcMsgProcThreads(
			Integer collectorIcMsgProcThreads) {
		this.collectorIcMsgProcThreads = collectorIcMsgProcThreads;
	}

	public Integer getAgentAppInvTopicPart() {
		return agentAppInvTopicPart;
	}

	public void setAgentAppInvTopicPart(Integer agentAppInvTopicPart) {
		this.agentAppInvTopicPart = agentAppInvTopicPart;
	}

	public Integer getAgentAppInvTopicRepl() {
		return agentAppInvTopicRepl;
	}

	public void setAgentAppInvTopicRepl(Integer agentAppInvTopicRepl) {
		this.agentAppInvTopicRepl = agentAppInvTopicRepl;
	}

	public Integer getAgentAppInvTopicMinIsr() {
		return agentAppInvTopicMinIsr;
	}

	public void setAgentAppInvTopicMinIsr(Integer agentAppInvTopicMinIsr) {
		this.agentAppInvTopicMinIsr = agentAppInvTopicMinIsr;
	}

	public Integer getAgentAppPerfTopicPart() {
		return agentAppPerfTopicPart;
	}

	public void setAgentAppPerfTopicPart(Integer agentAppPerfTopicPart) {
		this.agentAppPerfTopicPart = agentAppPerfTopicPart;
	}

	public Integer getAgentAppPerfTopicRepl() {
		return agentAppPerfTopicRepl;
	}

	public void setAgentAppPerfTopicRepl(Integer agentAppPerfTopicRepl) {
		this.agentAppPerfTopicRepl = agentAppPerfTopicRepl;
	}

	public Integer getAgentAppPerfTopicMinIsr() {
		return agentAppPerfTopicMinIsr;
	}

	public void setAgentAppPerfTopicMinIsr(Integer agentAppPerfTopicMinIsr) {
		this.agentAppPerfTopicMinIsr = agentAppPerfTopicMinIsr;
	}

	public Integer getRetinaStreamTopicPart() {
		return retinaStreamTopicPart;
	}

	public void setRetinaStreamTopicPart(Integer retinaStreamTopicPart) {
		this.retinaStreamTopicPart = retinaStreamTopicPart;
	}

	public Integer getRetinaStreamTopicRepl() {
		return retinaStreamTopicRepl;
	}

	public void setRetinaStreamTopicRepl(Integer retinaStreamTopicRepl) {
		this.retinaStreamTopicRepl = retinaStreamTopicRepl;
	}

	public Integer getRetinaStreamTopicMinIsr() {
		return retinaStreamTopicMinIsr;
	}

	public void setRetinaStreamTopicMinIsr(Integer retinaStreamTopicMinIsr) {
		this.retinaStreamTopicMinIsr = retinaStreamTopicMinIsr;
	}
        
        public Integer getRetinaConfigUpdateTopicPart() {
            return retinaConfigUpdateTopicPart;
        }

        public void setRetinaConfigUpdateTopicPart(Integer retinaConfigUpdateTopicPart) {
            this.retinaConfigUpdateTopicPart = retinaConfigUpdateTopicPart;
        }

        public Integer getRetinaConfigUpdateTopicRepl() {
            return retinaConfigUpdateTopicRepl;
        }

        public void setRetinaConfigUpdateTopicRepl(Integer retinaConfigUpdateTopicRepl) {
            this.retinaConfigUpdateTopicRepl = retinaConfigUpdateTopicRepl;
        }

        public Integer getRetinaConfigUpdateTopicMinIsr() {
            return retinaConfigUpdateTopicMinIsr;
        }

        public void setRetinaConfigUpdateTopicMinIsr(Integer retinaConfigUpdateTopicMinIsr) {
            this.retinaConfigUpdateTopicMinIsr = retinaConfigUpdateTopicMinIsr;
        }
        

	public Integer getRetinaImageServiceTopicPart() {
		return retinaImageServiceTopicPart;
	}

	public void setRetinaImageServiceTopicPart(Integer retinaStreamTopicPart) {
		this.retinaImageServiceTopicPart = retinaImageServiceTopicPart;
	}

	public Integer getRetinaImageServiceTopicRepl() {
		return retinaImageServiceTopicRepl;
	}

	public void setRetinaImageServiceTopicRepl(Integer retinaImageServiceTopicRepl) {
		this.retinaImageServiceTopicRepl = retinaImageServiceTopicRepl;
	}

	public Integer getRetinaImageServiceTopicMinIsr() {
		return retinaImageServiceTopicMinIsr;
	}

	public void setRetinaImageServiceTopicMinIsr(Integer retinaImageServiceTopicMinIsr) {
		this.retinaImageServiceTopicMinIsr = retinaImageServiceTopicMinIsr;
	}

}
