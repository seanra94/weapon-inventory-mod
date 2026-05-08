package weaponsprocurement.core;

public enum StockCategory {
    NO_STOCK("No Stock"),
    INSUFFICIENT("Insufficient Stock"),
    SUFFICIENT("Sufficient Stock");

    private final String label;

    StockCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
