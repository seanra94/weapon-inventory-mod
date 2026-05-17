package weaponsprocurement.stock.fixer

enum class FixerCatalogSource(
    val label: String,
    val details: String,
) {
    LIVE_STOCK(
        "Live stock",
        "Currently found in real market cargo. Fixer purchases still use virtual stock and do not drain the source market.",
    ),
    CUSTOM_LIVE_STOCK(
        "Custom live stock",
        "Currently found only in unsupported/custom submarket cargo. Rarity is unknown until a source adapter exists.",
    ),
    FACTION_CATALOG(
        "Faction catalog",
        "In a current market owner's vanilla-style weapon/LPC catalog. Price is estimated from item data.",
    ),
    FACTION_CATALOG_OBSERVED_REFERENCE(
        "Catalog + observed ref",
        "In a current market owner's vanilla-style weapon/LPC catalog. Price and cargo space use prior observed market data.",
    ),
}
