package weaponsprocurement.stock.fixer

enum class ShipCatalogRarity(
    val label: String,
    val details: String,
) {
    COMMON("Common", "Low-cost or strongly supported hull for at least one vanilla-supported market faction."),
    UNCOMMON("Uncommon", "Moderate-cost hull or softer faction support; plausible but less routine than common stock."),
    RARE("Rare", "High-cost hull, weak faction support, or doctrine friction makes regular market appearance unlikely."),
    VERY_RARE("Very rare", "Very high-cost hull or low faction frequency makes normal market appearance exceptional."),
    UNKNOWN_CUSTOM_SUBMARKET("Unknown", "Observed only in unsupported/custom submarket cargo, so WP does not have a vanilla ship rarity estimate."),
}
