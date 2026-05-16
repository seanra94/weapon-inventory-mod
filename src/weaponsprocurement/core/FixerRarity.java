package weaponsprocurement.core;

public enum FixerRarity {
    COMMON("Common"),
    UNCOMMON("Uncommon"),
    RARE("Rare"),
    VERY_RARE("Very rare"),
    UNKNOWN_CUSTOM_SUBMARKET("Unknown");

    private final String label;

    FixerRarity(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
