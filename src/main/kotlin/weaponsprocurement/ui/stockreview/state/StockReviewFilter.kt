package weaponsprocurement.ui.stockreview.state



import com.fs.starfarer.api.combat.DamageType
import com.fs.starfarer.api.combat.WeaponAPI
import weaponsprocurement.stock.fixer.FixerCatalogSource
import weaponsprocurement.stock.fixer.FixerRarity
import weaponsprocurement.stock.item.WeaponStockRecord

enum class StockReviewFilter(
    val group: StockReviewFilterGroup,
    val label: String,
) {
    SIZE_SMALL(StockReviewFilterGroup.SIZE, "Small"),
    SIZE_MEDIUM(StockReviewFilterGroup.SIZE, "Medium"),
    SIZE_LARGE(StockReviewFilterGroup.SIZE, "Large"),
    TYPE_BALLISTIC(StockReviewFilterGroup.TYPE, "Ballistic"),
    TYPE_ENERGY(StockReviewFilterGroup.TYPE, "Energy"),
    TYPE_MISSILE(StockReviewFilterGroup.TYPE, "Missile"),
    DAMAGE_KINETIC(StockReviewFilterGroup.DAMAGE, "Kinetic"),
    DAMAGE_HIGH_EXPLOSIVE(StockReviewFilterGroup.DAMAGE, "High Explosive"),
    DAMAGE_ENERGY(StockReviewFilterGroup.DAMAGE, "Energy"),
    DAMAGE_FRAGMENTATION(StockReviewFilterGroup.DAMAGE, "Fragmentation"),
    AVAILABILITY_LIVE(StockReviewFilterGroup.AVAILABILITY, "Live Stock"),
    AVAILABILITY_CUSTOM(StockReviewFilterGroup.AVAILABILITY, "Custom Live"),
    AVAILABILITY_CATALOG(StockReviewFilterGroup.AVAILABILITY, "Catalog"),
    AVAILABILITY_OBSERVED_REFERENCE(StockReviewFilterGroup.AVAILABILITY, "Catalog + Ref"),
    RARITY_COMMON(StockReviewFilterGroup.RARITY, "Common"),
    RARITY_UNCOMMON(StockReviewFilterGroup.RARITY, "Uncommon"),
    RARITY_RARE(StockReviewFilterGroup.RARITY, "Rare"),
    RARITY_VERY_RARE(StockReviewFilterGroup.RARITY, "Very Rare"),
    RARITY_UNKNOWN(StockReviewFilterGroup.RARITY, "Unknown");

    fun matches(record: WeaponStockRecord?): Boolean {
        if (record == null) return false
        if (group.weaponOnly) {
            val spec = record.spec ?: return false
            return when (this) {
                SIZE_SMALL -> WeaponAPI.WeaponSize.SMALL == spec.size
                SIZE_MEDIUM -> WeaponAPI.WeaponSize.MEDIUM == spec.size
                SIZE_LARGE -> WeaponAPI.WeaponSize.LARGE == spec.size
                TYPE_BALLISTIC -> WeaponAPI.WeaponType.BALLISTIC == spec.type
                TYPE_ENERGY -> WeaponAPI.WeaponType.ENERGY == spec.type
                TYPE_MISSILE -> WeaponAPI.WeaponType.MISSILE == spec.type
                DAMAGE_KINETIC -> DamageType.KINETIC == spec.damageType
                DAMAGE_HIGH_EXPLOSIVE -> DamageType.HIGH_EXPLOSIVE == spec.damageType
                DAMAGE_ENERGY -> DamageType.ENERGY == spec.damageType
                DAMAGE_FRAGMENTATION -> DamageType.FRAGMENTATION == spec.damageType
                else -> false
            }
        }
        return when (this) {
            AVAILABILITY_LIVE -> FixerCatalogSource.LIVE_STOCK == record.fixerCatalogSource
            AVAILABILITY_CUSTOM -> FixerCatalogSource.CUSTOM_LIVE_STOCK == record.fixerCatalogSource
            AVAILABILITY_CATALOG -> FixerCatalogSource.FACTION_CATALOG == record.fixerCatalogSource
            AVAILABILITY_OBSERVED_REFERENCE -> FixerCatalogSource.FACTION_CATALOG_OBSERVED_REFERENCE == record.fixerCatalogSource
            RARITY_COMMON -> FixerRarity.COMMON == record.fixerRarity
            RARITY_UNCOMMON -> FixerRarity.UNCOMMON == record.fixerRarity
            RARITY_RARE -> FixerRarity.RARE == record.fixerRarity
            RARITY_VERY_RARE -> FixerRarity.VERY_RARE == record.fixerRarity
            RARITY_UNKNOWN -> FixerRarity.UNKNOWN_CUSTOM_SUBMARKET == record.fixerRarity
            else -> false
        }
    }
}
