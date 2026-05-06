package weaponinventorymod.core;

public enum StockDisplayMode {
    OWNED_OR_FOR_SALE("Owned or For Sale"),
    CURRENTLY_FOR_SALE("Currently For Sale"),
    OWNED_ONLY("Owned Only"),
    ALL_TRACKED("All Tracked");

    private final String label;

    StockDisplayMode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public StockDisplayMode next() {
        int nextOrdinal = ordinal() + 1;
        StockDisplayMode[] values = values();
        return values[nextOrdinal >= values.length ? 0 : nextOrdinal];
    }

    public static StockDisplayMode fromConfig(String value) {
        if (value == null) {
            return OWNED_OR_FOR_SALE;
        }
        String normalized = value.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        for (StockDisplayMode mode : values()) {
            if (mode.name().equals(normalized)) {
                return mode;
            }
        }
        return OWNED_OR_FOR_SALE;
    }
}
