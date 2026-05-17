package weaponsprocurement.stock.item

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.loading.FighterWingSpecAPI
import com.fs.starfarer.api.loading.WeaponSpecAPI
import java.util.HashMap
import java.util.HashSet

object StockItemSpecs {
    private val weaponSpecs = HashMap<String, WeaponSpecAPI>()
    private val missingWeaponSpecs = HashSet<String>()
    private val wingSpecs = HashMap<String, FighterWingSpecAPI>()
    private val missingWingSpecs = HashSet<String>()

    @JvmStatic
    fun weaponSpec(weaponId: String?): WeaponSpecAPI? {
        val id = normalizedId(weaponId) ?: return null
        val cached = weaponSpecs[id]
        if (cached != null) return cached
        if (missingWeaponSpecs.contains(id)) return null
        return try {
            val spec = Global.getSettings().getWeaponSpec(id)
            if (spec == null) {
                missingWeaponSpecs.add(id)
            } else {
                weaponSpecs[id] = spec
            }
            spec
        } catch (_: RuntimeException) {
            missingWeaponSpecs.add(id)
            null
        }
    }

    @JvmStatic
    fun wingSpec(wingId: String?): FighterWingSpecAPI? {
        val id = normalizedId(wingId) ?: return null
        val cached = wingSpecs[id]
        if (cached != null) return cached
        if (missingWingSpecs.contains(id)) return null
        return try {
            val spec = Global.getSettings().getFighterWingSpec(id)
            if (spec == null) {
                missingWingSpecs.add(id)
            } else {
                wingSpecs[id] = spec
            }
            spec
        } catch (_: RuntimeException) {
            missingWingSpecs.add(id)
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

    private fun normalizedId(id: String?): String? {
        if (id.isNullOrBlank()) return null
        return id.trim()
    }
}
