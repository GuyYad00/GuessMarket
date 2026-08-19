package engine.core;

public enum CommissionType {
    ON_PURCHASE("on-purchase"),
    ON_CLOSE("on-close");

    private final String xmlValue;

    CommissionType(String xmlValue) {
        this.xmlValue = xmlValue;
    }

    public String getDisplayName() {
        return xmlValue;
    }

    public static CommissionType fromXmlValue(String value) {
        for (CommissionType type : values()) {
            if (type.xmlValue.equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown commission type: '" + value + "'");
    }
}
