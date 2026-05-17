package weaponsprocurement.stock.fixer

class FixerCatalogMetadata private constructor(
    val rarity: FixerRarity?,
    val source: FixerCatalogSource?,
) {
    companion object {
        @JvmStatic
        fun create(rarity: FixerRarity?, source: FixerCatalogSource?): FixerCatalogMetadata? {
            if (rarity == null && source == null) return null
            return FixerCatalogMetadata(rarity, source)
        }
    }
}
