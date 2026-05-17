package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.ui.WimGuiTooltip
import weaponsprocurement.ui.stockreview.rendering.StockReviewWeaponIconPlugin
import com.fs.starfarer.api.combat.WeaponAPI
import weaponsprocurement.stock.item.WeaponStockRecord

class StockReviewRowIcon private constructor(
    val spriteName: String,
    val motifType: WeaponAPI.WeaponType?,
) {
    companion object {
        @JvmStatic
        fun weapon(spriteName: String?, motifType: WeaponAPI.WeaponType?): StockReviewRowIcon? {
            val resolvedSpriteName = spriteName?.takeIf { WimGuiTooltip.hasText(it) } ?: return null
            return StockReviewRowIcon(resolvedSpriteName, motifType)
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
