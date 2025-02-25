package ai.netoai.collector.model;

public enum PrivateProtocol {
	NONE("None"), DES("DES"), AES("AES"), TripleDES("Triple Des");
	private String displayName;

	PrivateProtocol(String displayName) {
		this.displayName = displayName;
	}
}
