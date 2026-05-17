package weaponsprocurement.stock.item

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.loading.FighterWingSpecAPI
import com.fs.starfarer.api.loading.WeaponSpecAPI

object StockItemSpecs {
    @JvmStatic
    fun weaponSpec(weaponId: String?): WeaponSpecAPI? {
        if (weaponId.isNullOrBlank()) return null
        return try {
            Global.getSettings().getWeaponSpec(weaponId)
        } catch (_: RuntimeException) {
            null
        }
    }

    @JvmStatic
    fun wingSpec(wingId: String?): FighterWingSpecAPI? {
        if (wingId.isNullOrBlank()) return null
        return try {
            Global.getSettings().getFighterWingSpec(wingId)
        } catch (_: RuntimeException) {
            null
        }
    }

    @JvmStatic
    fun hasSystemHint(spec: WeaponSpecAPI?): Boolean {
        if (spec == null) return false
        return try {
            spec.aiHints?.contains(WeaponAPI.AIHints.SYSTEM) == true
        } catch (_: RuntimeException) {
            false
        }
    }
}
