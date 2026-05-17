package weaponsprocurement.stock.fixer

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipHullSpecAPI
import com.fs.starfarer.api.impl.campaign.ids.Submarkets
import java.util.Collections
import java.util.HashSet
import java.util.Locale

object ShipCatalogPolicy {
    private const val BLACK_MARKET_CRUISER_CAP = 3
    private const val NORMAL_MARKET_CAPITAL_CAP = 4

    private val excludedTags: Set<String> = tags(
        "no_sell",
        "no_dealer",
        "no_drop",
        "no_bp_drop",
        "restricted",
        "hide_in_codex",
        "invisible_in_codex",
        "codex_unlockable",
        "omega",
        "remnant",
        "dweller",
        "threat",
        "station",
        "module",
    )

    @JvmStatic
    fun hullSpec(hullId: String?): ShipHullSpecAPI? {
        val id = normalizedId(hullId) ?: return null
        return try {
            Global.getSettings().getHullSpec(id)
        } catch (_: RuntimeException) {
            null
        }
    }

    @JvmStatic
    fun isSafeHull(spec: ShipHullSpecAPI?): Boolean {
        if (spec == null) return false
        if (!isCombatHullSize(spec.hullSize)) return false
        if (hasExcludedHint(spec)) return false
        return !intersects(lowerTags(spec.tags), excludedTags)
    }

    @JvmStatic
    fun isVanillaSupportedSubmarket(submarketId: String?): Boolean {
        return Submarkets.SUBMARKET_OPEN == submarketId ||
            Submarkets.GENERIC_MILITARY == submarketId ||
            Submarkets.SUBMARKET_BLACK == submarketId
    }

    @JvmStatic
    fun maxShipSizeRank(submarketId: String?, marketFactionId: String?, submarketFactionId: String?): Int {
        if (Submarkets.SUBMARKET_BLACK != submarketId) return NORMAL_MARKET_CAPITAL_CAP
        if (marketFactionId != null && marketFactionId == submarketFactionId) return NORMAL_MARKET_CAPITAL_CAP
        return BLACK_MARKET_CRUISER_CAP
    }

    @JvmStatic
    fun hullSizeRank(hullSize: ShipAPI.HullSize?): Int {
        return when (hullSize) {
            ShipAPI.HullSize.FIGHTER -> 0
            ShipAPI.HullSize.FRIGATE -> 1
            ShipAPI.HullSize.DESTROYER -> 2
            ShipAPI.HullSize.CRUISER -> 3
            ShipAPI.HullSize.CAPITAL_SHIP -> 4
            else -> 99
        }
    }

    @JvmStatic
    fun isWithinSizeCap(spec: ShipHullSpecAPI?, maxRank: Int): Boolean {
        return hullSizeRank(spec?.hullSize) <= maxRank
    }

    @JvmStatic
    fun hullSizeLabel(hullSize: ShipAPI.HullSize?): String {
        return when (hullSize) {
            ShipAPI.HullSize.FRIGATE -> "Frigate"
            ShipAPI.HullSize.DESTROYER -> "Destroyer"
            ShipAPI.HullSize.CRUISER -> "Cruiser"
            ShipAPI.HullSize.CAPITAL_SHIP -> "Capital"
            ShipAPI.HullSize.FIGHTER -> "Fighter"
            else -> "Unknown"
        }
    }

    private fun normalizedId(id: String?): String? {
        if (id.isNullOrBlank()) return null
        return id.trim()
    }

    private fun isCombatHullSize(hullSize: ShipAPI.HullSize?): Boolean {
        return hullSize == ShipAPI.HullSize.FRIGATE ||
            hullSize == ShipAPI.HullSize.DESTROYER ||
            hullSize == ShipAPI.HullSize.CRUISER ||
            hullSize == ShipAPI.HullSize.CAPITAL_SHIP
    }

    private fun hasExcludedHint(spec: ShipHullSpecAPI): Boolean {
        val hints = spec.hints ?: return false
        for (hint in hints) {
            val name = hint.name.lowercase(Locale.US)
            if (name.contains("station") || name.contains("module") || name.contains("unboardable")) {
                return true
            }
        }
        return false
    }

    private fun lowerTags(tags: Set<String>?): Set<String> {
        if (tags == null || tags.isEmpty()) return Collections.emptySet()
        val result = HashSet<String>()
        for (tag in tags) result.add(tag.lowercase(Locale.US))
        return result
    }

    private fun intersects(left: Set<String>?, right: Set<String>?): Boolean {
        if (left == null || right == null) return false
        for (value in left) {
            if (right.contains(value)) return true
        }
        return false
    }

    private fun tags(vararg tags: String): Set<String> {
        val result = HashSet<String>()
        for (tag in tags) result.add(tag)
        return Collections.unmodifiableSet(result)
    }
}
