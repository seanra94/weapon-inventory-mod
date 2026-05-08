package weaponinventorymod.core;

import weaponinventorymod.internal.WeaponInventoryConfig;

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
        StockSourceMode current = this;
        for (int i = 0; i < values.length; i++) {
            current = values[(current.ordinal() + 1) % values.length];
            if (current.isEnabled()) {
                return current;
            }
        }
        return LOCAL;
    }

    public boolean isEnabled() {
        if (SECTOR.equals(this)) {
            return WeaponInventoryConfig.isSectorMarketEnabled();
        }
        if (FIXERS.equals(this)) {
            return WeaponInventoryConfig.isFixersMarketEnabled();
        }
        return true;
    }
}
