package weaponsprocurement.stock.market

import weaponsprocurement.stock.fixer.TheoreticalSaleIndex.Candidate
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.SectorAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import weaponsprocurement.config.WeaponMarketBlacklist
import weaponsprocurement.config.WeaponsProcurementConfig
import weaponsprocurement.stock.fixer.FixerMarketObservedCatalog
import weaponsprocurement.stock.fixer.FixerRarity
import weaponsprocurement.stock.fixer.ObservedStockIndex
import weaponsprocurement.stock.fixer.RarityClassifier
import weaponsprocurement.stock.fixer.TheoreticalSaleIndex
import weaponsprocurement.stock.item.SubmarketWeaponStock
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.HashSet

class GlobalWeaponMarketService {
    private val marketStockService = MarketStockService()
    private val observedStockIndex = ObservedStockIndex()
    private val theoreticalSaleIndex = TheoreticalSaleIndex()
    private val observedCatalog = FixerMarketObservedCatalog()

    private var theoreticalCacheKey: String? = null
    private var theoreticalCache: Map<String, TheoreticalSaleIndex.Candidate>? = null

    fun collectSectorWeaponStock(sector: SectorAPI?): MarketStockService.MarketStock {
        return buildSectorWeaponStock(sector, WeaponsProcurementConfig.sectorMarketPriceMultiplier())
    }

    fun collectFixersWeaponStock(sector: SectorAPI?): MarketStockService.MarketStock {
        val priceMultiplier = WeaponsProcurementConfig.fixersMarketPriceMultiplier()
        val blacklist = WeaponMarketBlacklist.load()
        return buildFixersWeaponStock(sector, priceMultiplier, blacklist)
    }

    /**
     * Sector Market is deliberately built from real market cargo entries. The
     * stock rows are marked up for WP pricing, but keep market/submarket ids so
     * confirmation can drain the actual remote cargo stacks.
     */
    private fun buildSectorWeaponStock(sector: SectorAPI?, priceMultiplier: Float): MarketStockService.MarketStock {
        val blacklist = WeaponMarketBlacklist.load()
        val builder = MarketStockService.MarketStockBuilder()
        val markets: List<MarketAPI> = sector?.economy?.marketsCopy ?: return builder.build()

        for (market in markets) {
            val stock = marketStockService.collectCurrentMarketItemStock(market, true)
            for (itemKey in stock.itemKeys()) {
                if (blacklist.isBannedFromSector(itemKey)) continue
                val sources = stock.getSubmarketStocks(itemKey)
                for (source in sources) {
                    if (source.count <= 0) continue
                    builder.add(
                        itemKey,
                        SubmarketWeaponStock(
                            source.marketId,
                            source.marketName,
                            source.submarketId,
                            source.submarketName,
                            source.count,
                            markedUpPrice(source.baseUnitPrice, priceMultiplier),
                            source.baseUnitPrice,
                            source.unitCargoSpace,
                            source.isPurchasable(),
                        )
                    )
                }
            }
        }
        return builder.build()
    }

    private fun buildFixersWeaponStock(
        sector: SectorAPI?,
        priceMultiplier: Float,
        blacklist: WeaponMarketBlacklist?,
    ): MarketStockService.MarketStock {
        val builder = MarketStockService.MarketStockBuilder()
        val references = HashMap<String, ReferenceItem>()
        addLiveObservedReferences(sector, references, blacklist)
        addPersistentObservedReferences(sector, references, blacklist)
        addTheoreticalCandidates(sector, references, blacklist)

        for ((itemKey, reference) in references) {
            builder.add(
                itemKey,
                SubmarketWeaponStock(
                    VIRTUAL_SUBMARKET_ID,
                    FIXERS_MARKET_NAME,
                    VIRTUAL_STOCK,
                    markedUpPrice(reference.baseUnitPrice, priceMultiplier),
                    reference.baseUnitPrice,
                    reference.unitCargoSpace,
                    true,
                ),
                reference.rarity,
            )
        }
        return builder.build()
    }

    private fun addLiveObservedReferences(
        sector: SectorAPI?,
        references: MutableMap<String, ReferenceItem>,
        blacklist: WeaponMarketBlacklist?,
    ) {
        val observed = observedStockIndex.collect(sector, blacklist)
        for ((itemKey, item) in observed) {
            val source = item.cheapestReferenceSource ?: continue
            references[itemKey] = ReferenceItem(
                source.baseUnitPrice,
                source.unitCargoSpace,
                RarityClassifier.observedOnly(item),
            )
        }
    }

    private fun addPersistentObservedReferences(
        sector: SectorAPI?,
        references: MutableMap<String, ReferenceItem>,
        blacklist: WeaponMarketBlacklist?,
    ) {
        val observed = observedCatalog.observedItems(sector, blacklist)
        for ((itemKey, item) in observed) {
            if (references.containsKey(itemKey)) continue
            references[itemKey] = ReferenceItem(item.baseUnitPrice, item.unitCargoSpace, null)
        }
    }

    private fun addTheoreticalCandidates(
        sector: SectorAPI?,
        references: MutableMap<String, ReferenceItem>,
        blacklist: WeaponMarketBlacklist?,
    ) {
        val candidates = theoreticalCandidates(sector, blacklist)
        for ((itemKey, candidate) in candidates) {
            val current = references[itemKey]
            if (current == null) {
                references[itemKey] = ReferenceItem(
                    candidate.baseUnitPrice,
                    candidate.unitCargoSpace,
                    candidate.rarity,
                )
            } else if (current.rarity == null) {
                current.rarity = candidate.rarity
            }
        }
    }

    private fun theoreticalCandidates(
        sector: SectorAPI?,
        blacklist: WeaponMarketBlacklist?,
    ): Map<String, TheoreticalSaleIndex.Candidate> {
        val key = theoreticalCacheKey(sector, blacklist)
        val cache = theoreticalCache
        if (key == theoreticalCacheKey && cache != null) return cache

        theoreticalCacheKey = key
        val collected = theoreticalSaleIndex.collect(sector, blacklist)
        val immutable = Collections.unmodifiableMap(HashMap(collected))
        theoreticalCache = immutable
        return immutable
    }

    private class ReferenceItem(
        baseUnitPrice: Int,
        unitCargoSpace: Float,
        var rarity: FixerRarity?,
    ) {
        val baseUnitPrice: Int = Math.max(0, baseUnitPrice)
        val unitCargoSpace: Float = if (!unitCargoSpace.isNaN() && !unitCargoSpace.isInfinite()) {
            Math.max(0.01f, unitCargoSpace)
        } else {
            1f
        }
    }

    companion object {
        @JvmField
        val VIRTUAL_SUBMARKET_ID: String = "wp_fixers_market"

        @JvmField
        val SECTOR_MARKET_NAME: String = "Sector Market"

        @JvmField
        val FIXERS_MARKET_NAME: String = "Fixer's Market"

        @JvmField
        val VIRTUAL_STOCK: Int = 999

        private fun theoreticalCacheKey(sector: SectorAPI?, blacklist: WeaponMarketBlacklist?): String {
            val result = StringBuilder()
            result.append("blacklist=").append(blacklist?.cacheKey() ?: "none")
            val factionIds = HashSet<String>()
            val markets = sector?.economy?.marketsCopy
            if (markets != null) {
                for (market in markets) {
                    append(result, "m", market?.id)
                    append(result, "mf", market?.factionId)
                    addFactionId(factionIds, market?.factionId)
                    val submarkets: List<SubmarketAPI> = market?.submarketsCopy ?: continue
                    for (submarket in submarkets) {
                        append(result, "s", submarket?.specId)
                        val submarketFactionId = submarket?.faction?.id
                        append(result, "sf", submarketFactionId)
                        addFactionId(factionIds, submarketFactionId)
                    }
                }
            }
            val sortedFactionIds = ArrayList(factionIds)
            Collections.sort(sortedFactionIds)
            for (factionId in sortedFactionIds) {
                appendFactionSignature(result, sector, factionId)
            }
            return result.toString()
        }

        private fun appendFactionSignature(result: StringBuilder, sector: SectorAPI?, factionId: String?) {
            val faction = safeFaction(sector, factionId)
            append(result, "f", factionId)
            if (faction == null) {
                append(result, "missing", "true")
                return
            }
            append(result, "kw", hash(faction.knownWeapons).toString())
            append(result, "kf", hash(faction.knownFighters).toString())
            append(result, "wsf", hash(faction.weaponSellFrequency).toString())
            append(result, "fsf", hash(faction.fighterSellFrequency).toString())
        }

        private fun safeFaction(sector: SectorAPI?, factionId: String?): FactionAPI? {
            return try {
                if (sector == null || factionId == null) null else sector.getFaction(factionId)
            } catch (_: RuntimeException) {
                null
            }
        }

        private fun hash(value: Any?): Int = value?.hashCode() ?: 0

        private fun append(result: StringBuilder, key: String, value: String?) {
            result.append('|').append(key).append('=').append(value ?: "")
        }

        private fun addFactionId(result: MutableSet<String>, factionId: String?) {
            if (!factionId.isNullOrBlank()) result.add(factionId)
        }

        private fun markedUpPrice(unitPrice: Int, priceMultiplier: Float): Int {
            return Math.max(0, Math.round(Math.max(0, unitPrice) * Math.max(1f, priceMultiplier)))
        }
    }
}
