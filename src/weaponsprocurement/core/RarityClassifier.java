package weaponsprocurement.core;

public final class RarityClassifier {
    private static final float HIGH_SELL_FREQUENCY = 1f;
    private static final float VERY_LOW_SELL_FREQUENCY = 0.25f;

    private RarityClassifier() {
    }

    public static FixerRarity classify(int tier, Float sellFrequency) {
        if (sellFrequency != null && sellFrequency.floatValue() <= VERY_LOW_SELL_FREQUENCY) {
            return FixerRarity.VERY_RARE;
        }
        if (tier <= 0 || (sellFrequency != null && sellFrequency.floatValue() >= HIGH_SELL_FREQUENCY)) {
            return FixerRarity.COMMON;
        }
        if (tier == 1) {
            return FixerRarity.UNCOMMON;
        }
        if (tier == 2) {
            return FixerRarity.RARE;
        }
        return FixerRarity.VERY_RARE;
    }

    public static FixerRarity observedOnly(ObservedStockIndex.ObservedItem item) {
        return item != null && item.isOnlyUnsupportedCustomSubmarket()
                ? FixerRarity.UNKNOWN_CUSTOM_SUBMARKET
                : null;
    }
}
