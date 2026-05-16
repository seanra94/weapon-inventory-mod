package weaponsprocurement.ui.stockreview.state



import com.fs.starfarer.api.combat.DamageType
import com.fs.starfarer.api.combat.WeaponAPI
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
    DAMAGE_FRAGMENTATION(StockReviewFilterGroup.DAMAGE, "Fragmentation");

    fun matches(record: WeaponStockRecord?): Boolean {
        val spec = record?.spec ?: return false
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
        }
    }
}