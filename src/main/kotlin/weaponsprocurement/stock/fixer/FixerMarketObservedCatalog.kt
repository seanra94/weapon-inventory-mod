package weaponsprocurement.stock.fixer

import com.fs.starfarer.api.campaign.SectorAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import org.apache.log4j.Logger
import weaponsprocurement.config.WeaponMarketBlacklist
import weaponsprocurement.stock.item.SubmarketWeaponStock
import weaponsprocurement.stock.market.MarketStockService
import java.util.Collections
import java.util.HashMap

class FixerMarketObservedCatalog {
    private val marketStockService = MarketStockService()

    fun observeSectorStock(sector: SectorAPI?, blacklist: WeaponMarketBlacklist?): Int {
        val catalog = rawCatalog(sector) ?: return 0
        val markets: List<MarketAPI> = sector?.economy?.marketsCopy ?: return 0

        pruneInvalidPersistentEntries(catalog)
        var added = 0
        for (market in markets) {
            val stock = marketStockService.collectCurrentMarketItemStock(market, true)
            for (itemKey in stock.itemKeys()) {
                if (!FixerCatalogPolicy.isEligibleObservedItem(itemKey, blacklist)) continue
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
        var pruned = 0
        val iterator = catalog.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val itemKey = entry.key
            val encoded = entry.value
            if (!FixerCatalogPolicy.isSafeItem(itemKey)) {
                iterator.remove()
                pruned++
                continue
            }
            val item = decode(encoded)
            if (item == null) {
                iterator.remove()
                pruned++
                continue
            }
            if (!FixerCatalogPolicy.isBanned(blacklist, itemKey)) {
                result[itemKey] = item
            }
        }
        logPrunedEntries(pruned)
        return Collections.unmodifiableMap(result)
    }

    fun cacheKey(sector: SectorAPI?): String {
        val catalog = rawCatalog(sector)
        if (catalog == null) return "observed=none"
        pruneInvalidPersistentEntries(catalog)
        return "observed=${catalog.size}:${catalog.hashCode()}"
    }

    private fun pruneInvalidPersistentEntries(catalog: MutableMap<String, String>) {
        if (catalog.isEmpty()) return
        var pruned = 0
        for ((itemKey, encoded) in catalog) {
            if (!FixerCatalogPolicy.isSafeItem(itemKey) || decode(encoded) == null) {
                pruned++
            }
        }
        if (pruned <= 0) return
        val iterator = catalog.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!FixerCatalogPolicy.isSafeItem(entry.key) || decode(entry.value) == null) {
                iterator.remove()
            }
        }
        logPrunedEntries(pruned)
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

        private var migrationLogged = false
        private var pruneLogged = false

        @JvmStatic
        fun isSafeFixerItem(itemKey: String?): Boolean {
            return FixerCatalogPolicy.isSafeItem(itemKey)
        }

        private fun cheapestReferenceSource(sources: List<SubmarketWeaponStock>?): SubmarketWeaponStock? {
            var best: SubmarketWeaponStock? = null
            if (sources == null) return null
            for (source in sources) {
                if (source.count <= 0 || !source.isPurchasable()) continue
                val currentBest = best
                if (currentBest == null || compareReferenceSource(source, currentBest) < 0) {
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
            return "${Math.max(0, baseUnitPrice)}$VALUE_SEPARATOR${sanitizeUnitCargoSpace(unitCargoSpace)}"
        }

        private fun decode(value: String?): ObservedItem? {
            if (value == null) return null
            val parts = value.split(VALUE_SEPARATOR, limit = 2)
            return try {
                val baseUnitPrice = if (parts.isNotEmpty()) parts[0].toInt() else 0
                val unitCargoSpace = if (parts.size > 1) parts[1].toFloat() else 1f
                if (!isFinite(unitCargoSpace)) return null
                ObservedItem.create(Math.max(0, baseUnitPrice), sanitizeUnitCargoSpace(unitCargoSpace))
            } catch (_: RuntimeException) {
                null
            }
        }

        private fun sanitizeUnitCargoSpace(unitCargoSpace: Float): Float {
            return if (isFinite(unitCargoSpace)) Math.max(0.01f, unitCargoSpace) else 1f
        }

        private fun isFinite(value: Float): Boolean {
            return !value.isNaN() && !value.isInfinite()
        }

        private fun logPrunedEntries(pruned: Int) {
            if (pruned > 0 && !pruneLogged) {
                pruneLogged = true
                LOG.warn("WP_FIXER_CATALOG pruned $pruned invalid or unsafe persistent entries.")
            }
        }

        private fun compareReferenceSource(left: SubmarketWeaponStock, right: SubmarketWeaponStock): Int {
            val result = left.baseUnitPrice.compareTo(right.baseUnitPrice)
            return if (result != 0) {
                result
            } else {
                left.displaySourceName.orEmpty().compareTo(right.displaySourceName.orEmpty(), ignoreCase = true)
            }
        }

    }
}
