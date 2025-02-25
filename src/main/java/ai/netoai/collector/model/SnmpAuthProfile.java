
package ai.netoai.collector.model;

import javax.persistence.Id;
import javax.persistence.*;
import java.util.UUID;

public class SnmpAuthProfile {

	private String uuid;
	private String name;
	private SnmpVersion snmpVersion = SnmpVersion.SNMPv2C;
	private Integer port = 161;
	private String userName;
	private SecurityLevel secLevel = SecurityLevel.AUTHNOPRIV;
	private String contextName;
	private String community;
	private AuthenticationProtocol authProtocol = AuthenticationProtocol.NONE;
	private String authPassword;
	private PrivateProtocol privProtocol = PrivateProtocol.NONE;
	private String privPassword;
	private boolean defaultForNew;
	private Integer timeout = 0;
	private Integer retries = 0;
	private String userRole;
	private String createdByUser;

	public SnmpAuthProfile() {
		if (uuid == null) {
			uuid = UUID.randomUUID().toString();
		}
	}
	
	/*
	 * @Id
	 * 
	 * @GeneratedValue(strategy = GenerationType.IDENTITY)
	 */
	@Id
	@Column(name = "uuid", unique = true)
	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	/**
	 * @return the SnmpAuth Profile name
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
	 * @return the this Profile selected snmpVersion
	 */
	@Enumerated(EnumType.STRING)
	public SnmpVersion getSnmpVersion() {
		return snmpVersion;
	}

	/**
	 * @param snmpVersion
	 *            the snmpVersion to set
	 */
	public void setSnmpVersion(SnmpVersion snmpVersion) {
		this.snmpVersion = snmpVersion;
	}

	/**
	 * @return the SnmpAuth Profile port
	 */
	public Integer getPort() {
		return port;
	}

	/**
	 * @param port
	 *            the port to set
	 */
	public void setPort(Integer port) {
		this.port = port;
	}

	/**
	 * @return the SnmpAuth Profile userName
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * @param userName
	 *            the userName to set
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}
    
	/**
	 * @return the SnmpProfile selected Security Level
	 */
	public SecurityLevel getSecLevel() {
		return secLevel;
	}

	/**
	 * @param secLevel
	 *            the secLevel to set
	 */
	public void setSecLevel(SecurityLevel secLevel) {
		this.secLevel = secLevel;
	}

	/**
	 * @return the SnmpAuth Profile contextName
	 */
	public String getContextName() {
		return contextName;
	}

	/**
	 * @param contextName
	 *            the contextName to set
	 */
	public void setContextName(String contextName) {
		this.contextName = contextName;
	}

	/**
	 * @return the SnmpAuth Profile community
	 */
	public String getCommunity() {
		return community;
	}

	/**
	 * @param community
	 *            the community to set
	 */
	public void setCommunity(String community) {
		this.community = community;
	}

	/**
	 * @return the SnmpProfile selected Authentication Protocol
	 */
	@Enumerated(EnumType.STRING)
	public AuthenticationProtocol getAuthProtocol() {
		return authProtocol;
	}

	/**
	 * @param authProtocol
	 *            the authProtocol to set
	 */
	public void setAuthProtocol(AuthenticationProtocol authProtocol) {
		this.authProtocol = authProtocol;
	}

	/**
	 * @return the SnmpAuth Profile authPassword
	 */
	public String getAuthPassword() {
		return authPassword;
	}

	/**
	 * @param authPassword
	 *            the authPassword to set
	 */
	public void setAuthPassword(String authPassword) {
		this.authPassword = authPassword;
	}

	/**
	 * @return the SnmpAuth Profile privProtocol
	 */
	@Enumerated(EnumType.STRING)
	public PrivateProtocol getPrivProtocol() {
		return privProtocol;
	}

	/**
	 * @param privProtocol
	 *            the privProtocol to set
	 */
	public void setPrivProtocol(PrivateProtocol privProtocol) {
		this.privProtocol = privProtocol;
	}

	/**
	 * @return the SnmpAuth Profile is defaultForNew (or) not
	 */
	public boolean isDefaultForNew() {
		return defaultForNew;
	}

	/**
	 * @param defaultForNew
	 *            the defaultForNew to set
	 */
	public void setDefaultForNew(boolean defaultForNew) {
		this.defaultForNew = defaultForNew;
	}

	/**
	 * @return the SnmpAuth Profile timeout
	 */
	public Integer getTimeout() {
		return timeout;
	}

	/**
	 * @param timeout
	 *            the timeout to set
	 */
	public void setTimeout(Integer timeout) {
		this.timeout = timeout;
	}

	/**
	 * @return the SnmpAuth Profile retries count
	 */
	public Integer getRetries() {
		return retries;
	}

	/**
	 * @param retries
	 *            the retries to set
	 */
	public void setRetries(Integer retries) {
		this.retries = retries;
	}
	
	/**
	 * @return the privPassword
	 */
	public String getPrivPassword() {
		return privPassword;
	}

	/**
	 * @param privPassword the privPassword to set
	 */
	public void setPrivPassword(String privPassword) {
		this.privPassword = privPassword;
	}

	@Column(nullable=false)
	public String getUserRole() {
		return userRole;
	}

	public void setUserRole(String userRole) {
		this.userRole = userRole;
	}

	@Column(nullable=false)
	public String getCreatedByUser() {
		return createdByUser;
	}

	public void setCreatedByUser(String createdByUser) {
		this.createdByUser = createdByUser;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "SnmpAuthProfile [name=" + name + ", snmpVersion=" + snmpVersion
				+ ", port=" + port + ", userName=" + userName + ", contextName="
				+ contextName + ", community=" + community + ", authProtocol="
				+ authProtocol + ", authPassword=" + authPassword
				+ ", privProtocol=" + privProtocol + ", defaultForNew="
				+ defaultForNew + ", timeout=" + timeout + ", retries="
				+ retries + "]";
	}
}
