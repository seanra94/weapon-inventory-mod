package weaponinventorymod.core;

public enum StockCategory {
    NO_STOCK("No stock"),
    INSUFFICIENT("Insufficient stock"),
    SUFFICIENT("Sufficient stock");

    private final String label;

    StockCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
