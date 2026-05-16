package weaponsprocurement.core

object RarityClassifier {
    private const val HIGH_SELL_FREQUENCY = 1f
    private const val VERY_LOW_SELL_FREQUENCY = 0.25f

    @JvmStatic
    fun classify(tier: Int, sellFrequency: Float?): FixerRarity {
        if (tier <= 0 || (sellFrequency != null && sellFrequency >= HIGH_SELL_FREQUENCY)) {
            return FixerRarity.COMMON
        }
        if (sellFrequency != null && sellFrequency <= VERY_LOW_SELL_FREQUENCY) {
            return FixerRarity.VERY_RARE
        }
        if (tier == 1) return FixerRarity.UNCOMMON
        if (tier == 2) return FixerRarity.RARE
        return FixerRarity.VERY_RARE
    }

    @JvmStatic
    fun observedOnly(item: ObservedStockIndex.ObservedItem?): FixerRarity? {
        return if (item != null && item.isOnlyUnsupportedCustomSubmarket) {
            FixerRarity.UNKNOWN_CUSTOM_SUBMARKET
        } else {
            null
        }
    }
}
