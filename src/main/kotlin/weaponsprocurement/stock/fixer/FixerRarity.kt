package weaponsprocurement.stock.fixer

enum class FixerRarity(
    val label: String,
    val details: String,
) {
    COMMON("Common", "Low-tier or high-frequency catalog item. This only affects sorting/filtering; it does not change price or availability."),
    UNCOMMON("Uncommon", "Mid-tier catalog item. This only affects sorting/filtering; it does not change price or availability."),
    RARE("Rare", "Higher-tier catalog item. This only affects sorting/filtering; it does not change price or availability."),
    VERY_RARE("Very rare", "Top-tier or very low-frequency catalog item. This only affects sorting/filtering; it does not change price or availability."),
    UNKNOWN_CUSTOM_SUBMARKET("Unknown", "Observed only in unsupported/custom submarket cargo, so WP does not have a vanilla rarity estimate."),
}
