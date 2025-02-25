package ai.netoai.collector.model;

public enum SecurityLevel {
	NOAUTHNOPRIV("NOAUTH_NOPRIV"), AUTHPRIV("AUTH_PRIV"), AUTHNOPRIV("AUTH_NOPRIV");
	private String displayName;
	
	SecurityLevel(String displayName){
		this.displayName = displayName;
	}
}
