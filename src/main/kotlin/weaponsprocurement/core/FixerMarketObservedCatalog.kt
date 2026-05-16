package weaponsprocurement.core

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.SectorAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.loading.FighterWingSpecAPI
import com.fs.starfarer.api.loading.WeaponSpecAPI
import org.apache.log4j.Logger
import java.util.Collections
import java.util.HashMap
import java.util.HashSet
import java.util.Locale

class FixerMarketObservedCatalog {
    private val marketStockService = MarketStockService()

    fun observeSectorStock(sector: SectorAPI?, blacklist: WeaponMarketBlacklist?): Int {
        val catalog = rawCatalog(sector) ?: return 0
        val markets: List<MarketAPI> = sector?.economy?.marketsCopy ?: return 0

        var added = 0
        for (market in markets) {
            val stock = marketStockService.collectCurrentMarketItemStock(market, true)
            for (itemKey in stock.itemKeys()) {
                if (!isSafeFixerItem(itemKey) || isBanned(blacklist, itemKey)) continue
                val source = cheapestReferenceSource(stock.getSubmarketStocks(itemKey)) ?: continue
                if (!catalog.containsKey(itemKey)) added++
                catalog[itemKey] = encode(source.baseUnitPrice, source.unitCargoSpace)
            }
        }
        return added
    }

    fun observedItems(sector: SectorAPI?, blacklist: WeaponMarketBlacklist?): Map<String, ObservedItem> {
        val catalog = rawCatalog(sector)
        if (catalog == null || catalog.isEmpty()) return Collections.emptyMap()

        val result = HashMap<String, ObservedItem>()
        for ((itemKey, encoded) in catalog) {
            if (!isSafeFixerItem(itemKey) || isBanned(blacklist, itemKey)) continue
            val item = decode(encoded) ?: continue
            result[itemKey] = item
        }
        return result
    }

    fun cacheKey(sector: SectorAPI?): String {
        val catalog = rawCatalog(sector)
        return if (catalog == null) "observed=none" else "observed=${catalog.size}:${catalog.hashCode()}"
    }

    class ObservedItem private constructor(
        @get:JvmName("getBaseUnitPrice")
        val baseUnitPrice: Int,
        @get:JvmName("getUnitCargoSpace")
        val unitCargoSpace: Float,
    ) {
        companion object {
            fun create(baseUnitPrice: Int, unitCargoSpace: Float): ObservedItem {
                return ObservedItem(baseUnitPrice, unitCargoSpace)
            }
        }
    }

    companion object {
        private val LOG: Logger = Logger.getLogger(FixerMarketObservedCatalog::class.java)
        private const val PERSISTENT_KEY = "weaponsProcurement.fixerObservedCatalog.v1"
        private const val VALUE_SEPARATOR = "|"

        private val SPOILER_TAGS: Set<String> = tags(
            "restricted",
            "no_dealer",
            "no_drop",
            "no_bp_drop",
            "omega",
            "remnant",
            "dweller",
            "threat",
            "hide_in_codex",
            "invisible_in_codex",
            "codex_unlockable",
        )

        private var migrationLogged = false

        @JvmStatic
        fun isSafeFixerItem(itemKey: String?): Boolean {
            val itemType = StockItemType.fromKey(itemKey)
            val itemId = StockItemType.rawId(itemKey)
            return if (StockItemType.WING == itemType) {
                isSafeWing(itemId)
            } else {
                isSafeWeapon(itemId)
            }
        }

        private fun isSafeWeapon(weaponId: String?): Boolean {
            val spec = safeWeaponSpec(weaponId) ?: return false
            try {
                if (spec.aiHints != null && spec.aiHints.contains(WeaponAPI.AIHints.SYSTEM)) return false
            } catch (_: Throwable) {
            }
            val tags = lowerTags(spec.tags)
            return !tags.contains("no_sell") &&
                !tags.contains("weapon_no_sell") &&
                !intersects(tags, SPOILER_TAGS)
        }

        private fun isSafeWing(wingId: String?): Boolean {
            val spec = safeWingSpec(wingId) ?: return false
            val tags = lowerTags(spec.tags)
            return !tags.contains("no_sell") &&
                !tags.contains("wing_no_sell") &&
                !intersects(tags, SPOILER_TAGS)
        }

        private fun isBanned(blacklist: WeaponMarketBlacklist?, itemKey: String?): Boolean {
            return blacklist != null && blacklist.isBannedFromFixers(itemKey)
        }

        private fun cheapestReferenceSource(sources: List<SubmarketWeaponStock>?): SubmarketWeaponStock? {
            var best: SubmarketWeaponStock? = null
            if (sources == null) return null
            for (source in sources) {
                if (source.count <= 0 || !source.isPurchasable()) continue
                if (best == null || compareReferenceSource(source, best!!) < 0) {
                    best = source
                }
            }
            return best
        }

        private fun rawCatalog(sector: SectorAPI?): MutableMap<String, String>? {
            val persistentData = sector?.persistentData ?: return null
            val existing = persistentData[PERSISTENT_KEY]
            if (existing is Map<*, *>) {
                return sanitizedCatalog(sector, existing)
            }
            val catalog = HashMap<String, String>()
            persistentData[PERSISTENT_KEY] = catalog
            return catalog
        }

        private fun sanitizedCatalog(sector: SectorAPI, existing: Map<*, *>): MutableMap<String, String> {
            val catalog = HashMap<String, String>()
            var discarded = 0
            for ((key, value) in existing) {
                if (key is String && value is String) {
                    catalog[key] = value
                } else {
                    discarded++
                }
            }
            if (discarded > 0 && !migrationLogged) {
                migrationLogged = true
                LOG.warn("WP_FIXER_CATALOG discarded $discarded malformed persistent entries.")
            }
            sector.persistentData[PERSISTENT_KEY] = catalog
            return catalog
        }

        private fun encode(baseUnitPrice: Int, unitCargoSpace: Float): String {
            return "${Math.max(0, baseUnitPrice)}$VALUE_SEPARATOR${Math.max(0.01f, unitCargoSpace)}"
        }

        private fun decode(value: String?): ObservedItem? {
            if (value == null) return null
            val parts = value.split(VALUE_SEPARATOR, limit = 2)
            return try {
                val baseUnitPrice = if (parts.isNotEmpty()) parts[0].toInt() else 0
                val unitCargoSpace = if (parts.size > 1) parts[1].toFloat() else 1f
                ObservedItem.create(Math.max(0, baseUnitPrice), Math.max(0.01f, unitCargoSpace))
            } catch (_: Throwable) {
                null
            }
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
            for (tag in tags) {
                result.add(tag)
            }
            return Collections.unmodifiableSet(result)
        }

        private fun compareReferenceSource(left: SubmarketWeaponStock, right: SubmarketWeaponStock): Int {
            val result = left.baseUnitPrice.compareTo(right.baseUnitPrice)
            return if (result != 0) {
                result
            } else {
                left.displaySourceName.orEmpty().compareTo(right.displaySourceName.orEmpty(), ignoreCase = true)
            }
        }

        private fun safeWeaponSpec(weaponId: String?): WeaponSpecAPI? {
            return try {
                Global.getSettings().getWeaponSpec(weaponId)
            } catch (_: Throwable) {
                null
            }
        }

        private fun safeWingSpec(wingId: String?): FighterWingSpecAPI? {
            return try {
                Global.getSettings().getFighterWingSpec(wingId)
            } catch (_: Throwable) {
                null
            }
        }
    }
}
