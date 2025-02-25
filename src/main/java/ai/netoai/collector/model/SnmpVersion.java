package ai.netoai.collector.model;

public enum SnmpVersion {
	SNMPv1(0), SNMPv2C(1), SNMPv3(3);
	private int versionId;

	private SnmpVersion(int versionId) {
		this.versionId = versionId;
	}

	public int getVersionId() {
		return versionId;
	}

	public static NetworkProtocol getNetworkProtocol(SnmpVersion protocol) {
		if (protocol != null) {
			if (protocol.equals(SnmpVersion.SNMPv1)) {
				return NetworkProtocol.SNMPv1;
			} else if (protocol.equals(SnmpVersion.SNMPv2C)) {
				return NetworkProtocol.SNMPv2c;
			} else if (protocol.equals(SnmpVersion.SNMPv3)) {
				return NetworkProtocol.SNMPv3;
			}
		}
		return null;
	}
}
