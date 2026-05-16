package weaponsprocurement.ui.stockreview

import weaponsprocurement.ui.*

import com.fs.starfarer.api.combat.WeaponAPI
import weaponsprocurement.stock.WeaponStockRecord

class StockReviewRowIcon private constructor(
    val spriteName: String,
    val motifType: WeaponAPI.WeaponType?,
) {
    companion object {
        @JvmStatic
        fun weapon(spriteName: String?, motifType: WeaponAPI.WeaponType?): StockReviewRowIcon? {
            if (!WimGuiTooltip.hasText(spriteName)) return null
            return StockReviewRowIcon(spriteName!!, motifType)
        }

        @JvmStatic
        fun weapon(record: WeaponStockRecord?): StockReviewRowIcon? {
            if (record == null || record.isWing() || record.spec == null) return null
            return weapon(
                StockReviewWeaponIconPlugin.spriteName(record.spec),
                StockReviewWeaponIconPlugin.motifType(record.spec),
            )
        }
    }
}
