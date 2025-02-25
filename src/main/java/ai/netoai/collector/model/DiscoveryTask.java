package ai.netoai.collector.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Filter;

import javax.persistence.Id;
import javax.persistence.Transient;
import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "dType")
@JsonSubTypes({
        @Type(value = DiscoveryTask.class, name = "ai.netoai.collector.model.DiscoveryTask")
})
public class DiscoveryTask implements Serializable {

    private static final long serialVersionUID = 7869936380351365093L;

    public DiscoveryTask() {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }
    }

    private String uuid;
    private String name;
    private List<String> ipRange;
    private String schedule;
    private List<SnmpAuthProfile> authProfiles;
    private List<String> credentials;
    private boolean enableIcmp;
    private boolean active;
    private DiscoveryStatus status;
    private long lastRun;
    private String discoveryType;
    private boolean enableSchedule;


	@Id
    @Column(name = "uuid", unique = true)
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the ipRange
     */
    public List<String> getIpRange() {
        if (ipRange == null) {
            ipRange = new ArrayList<>();
        }
        return ipRange;
    }

    /**
     * @param ipRange the ipRange to set
     */
    public void setIpRange(List<String> ipRange) {
        this.ipRange = ipRange;
    }

    /**
     * @return the schedule
     */
    public String getSchedule() {
        return schedule;
    }

    /**
     * @param schedule the schedule to set
     */
    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    /**
     * @return the authProfiles
     */
    public List<SnmpAuthProfile> getAuthProfiles() {
        if (authProfiles == null) {
            authProfiles = new ArrayList<>();
        }
        return authProfiles;
    }

    /**
     * @param authProfiles the authProfiles to set
     */
    public void setAuthProfiles(List<SnmpAuthProfile> authProfiles) {
        this.authProfiles = authProfiles;
    }

    public List<String> getCredentials() {
		return credentials;
	}

	public void setCredentials(List<String> credentials) {
		this.credentials = credentials;
	}

	public void addAuthProfile(SnmpAuthProfile profile) {
        if (authProfiles == null) {
            authProfiles = new ArrayList<>();
        }
        authProfiles.add(profile);
    }

    public void removeAuthProfile(SnmpAuthProfile profile) {
        if (authProfiles != null) {
            authProfiles.remove(profile);
        }
    }

    /**
     * @return the enableIcmp
     */
    public boolean isEnableIcmp() {
        return enableIcmp;
    }

    /**
     * @param enableIcmp the enableIcmp to set
     */
    public void setEnableIcmp(boolean enableIcmp) {
        this.enableIcmp = enableIcmp;
    }

    
    public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	/**
     * @return the status
     */
    @Enumerated(EnumType.STRING)
    public DiscoveryStatus getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(DiscoveryStatus status) {
        this.status = status;
    }

    /**
     * @return the lastRun
     */
    public long getLastRun() {
        return lastRun;
    }

    /**
     * @param lastRun the lastRun to set
     */
    public void setLastRun(long lastRun) {
        this.lastRun = lastRun;
    }

    public String getDiscoveryType() {
        return discoveryType;
    }

    public void setDiscoveryType(String discoveryType) {
        this.discoveryType = discoveryType;
    }



    public boolean isEnableSchedule() {
		return enableSchedule;
	}
		
    public void setEnableSchedule(boolean enableSchedule) {
		this.enableSchedule = enableSchedule;
	}

}
