package weaponsprocurement.core

import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.loading.FighterWingSpecAPI
import com.fs.starfarer.api.loading.WeaponSpecAPI

class DesiredStockService(private val config: StockReviewConfig) {
    fun desiredCount(weaponId: String?, spec: WeaponSpecAPI?): Int {
        if (spec == null || spec.size == null) {
            return config.desiredCount(weaponId, WeaponAPI.WeaponSize.MEDIUM)
        }
        return config.desiredCount(weaponId, spec.size)
    }

    fun desiredWingCount(wingId: String?, spec: FighterWingSpecAPI?): Int {
        return config.desiredFighterWingCount(wingId)
    }
}
