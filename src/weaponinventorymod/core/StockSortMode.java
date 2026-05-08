package weaponinventorymod.core;

public enum StockSortMode {
    NEED("Stock"),
    NAME("Name"),
    PRICE("Price");

    private final String label;

    StockSortMode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public StockSortMode next() {
        int nextOrdinal = ordinal() + 1;
        StockSortMode[] values = values();
        return values[nextOrdinal >= values.length ? 0 : nextOrdinal];
    }

    public static StockSortMode fromConfig(String value) {
        if (value == null) {
            return NEED;
        }
        String normalized = value.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        if ("PURCHASABLE".equals(normalized) || "FOR_SALE".equals(normalized) || "OWNED".equals(normalized)) {
            return NEED;
        }
        if ("COST".equals(normalized)) {
            return PRICE;
        }
        for (StockSortMode mode : values()) {
            if (mode.name().equals(normalized)) {
                return mode;
            }
        }
        return NEED;
    }
}
