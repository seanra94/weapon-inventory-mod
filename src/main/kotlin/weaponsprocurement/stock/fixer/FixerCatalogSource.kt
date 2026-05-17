package weaponsprocurement.stock.fixer

enum class FixerCatalogSource(
    val label: String,
    val shortLabel: String,
    val details: String,
) {
    LIVE_STOCK(
        "Live stock",
        "Live",
        "Currently found in real market cargo. Fixer purchases still use virtual stock and do not drain the source market.",
    ),
    CUSTOM_LIVE_STOCK(
        "Custom live stock",
        "Custom",
        "Currently found only in unsupported/custom submarket cargo. Rarity is unknown until a source adapter exists.",
    ),
    FACTION_CATALOG(
        "Faction catalog",
        "Catalog",
        "In a current market owner's vanilla-style weapon/LPC catalog. Price is estimated from item data.",
    ),
    FACTION_CATALOG_OBSERVED_REFERENCE(
        "Catalog + observed ref",
        "Catalog+ref",
        "In a current market owner's vanilla-style weapon/LPC catalog. Price and cargo space use prior observed market data.",
    ),
}
