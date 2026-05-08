package weaponinventorymod.core;

public enum StockSourceMode {
    LOCAL("Local", false),
    SECTOR("Sector Market", true),
    FIXERS("Fixer's Market", true);

    private final String label;
    private final boolean remote;

    StockSourceMode(String label, boolean remote) {
        this.label = label;
        this.remote = remote;
    }

    public String getLabel() {
        return label;
    }

    public boolean isRemote() {
        return remote;
    }

    public StockSourceMode next() {
        StockSourceMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
