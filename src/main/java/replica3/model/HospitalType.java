package replica3.model;

public enum HospitalType {
    MTL("Montreal", "MTL", new UDPServerInfo("MontrealServerAddress", 5052)),
    QUE("Quebec", "QUE", new UDPServerInfo("QuebecServerAddress", 5051)),
    SHE("Sherbrooke", "SHE", new UDPServerInfo("SherbrookeServerAddress", 5053));

    private final String _hospitalName;
    private final String _hospitalCode;

    private final UDPServerInfo _hospitalServerAddress;
    HospitalType(final String hospitalName, final String hospitalCode, final UDPServerInfo serverAddress) {
        _hospitalName = hospitalName;
        _hospitalCode = hospitalCode;
        _hospitalServerAddress = serverAddress;
    }

    public String getHospitalName() {
        return _hospitalName;
    }

    public String getHospitalCode() {
        return _hospitalCode;
    }

    public UDPServerInfo getHospitalServerAddress() {
        return _hospitalServerAddress;
    }

    public static HospitalType findHospital(String hospitalCode) {
        for(HospitalType hospitalType : HospitalType.values()) {
            if (hospitalType._hospitalCode.equalsIgnoreCase(hospitalCode)) {
                return hospitalType;
            }
        }

        return null;
    }
}
