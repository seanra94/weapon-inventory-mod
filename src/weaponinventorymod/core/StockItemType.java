package weaponinventorymod.core;

public enum StockItemType {
    WEAPON("Weapons", "Weapon", "W:"),
    WING("Wings", "Wing", "F:");

    private final String sectionLabel;
    private final String singularLabel;
    private final String keyPrefix;

    StockItemType(String sectionLabel, String singularLabel, String keyPrefix) {
        this.sectionLabel = sectionLabel;
        this.singularLabel = singularLabel;
        this.keyPrefix = keyPrefix;
    }

    public String getSectionLabel() {
        return sectionLabel;
    }

    public String getSingularLabel() {
        return singularLabel;
    }

    public String key(String itemId) {
        return keyPrefix + (itemId == null ? "" : itemId);
    }

    public static StockItemType fromKey(String itemKey) {
        if (itemKey != null && itemKey.startsWith(WING.keyPrefix)) {
            return WING;
        }
        return WEAPON;
    }

    public static String rawId(String itemKey) {
        if (itemKey == null) {
            return null;
        }
        for (StockItemType type : values()) {
            if (itemKey.startsWith(type.keyPrefix)) {
                return itemKey.substring(type.keyPrefix.length());
            }
        }
        return itemKey;
    }
}
