package ai.netoai.collector.model;

public enum AuthenticationProtocol {
	NONE("None"), MD5("MD 5"), SHA("SHA");
	private String displayName;

	AuthenticationProtocol(String displayName) {
		this.displayName = displayName;
	}
}
