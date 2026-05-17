package weaponsprocurement.stock.item

import weaponsprocurement.stock.market.MarketStockService.MarketStock
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.SectorAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.loading.FighterWingSpecAPI
import com.fs.starfarer.api.loading.WeaponSpecAPI
import weaponsprocurement.config.StockReviewConfig
import weaponsprocurement.stock.inventory.InventoryCountService
import weaponsprocurement.stock.market.GlobalWeaponMarketService
import weaponsprocurement.stock.market.MarketStockService
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.EnumMap
import java.util.HashSet

class WeaponStockSnapshotBuilder {
    private val inventoryCountService = InventoryCountService()
    private val marketStockService = MarketStockService()
    private val globalWeaponMarketService = GlobalWeaponMarketService()

    fun build(
        sector: SectorAPI?,
        market: MarketAPI?,
        config: StockReviewConfig,
        sortMode: StockSortMode,
        includeCurrentMarketStorage: Boolean,
        includeBlackMarket: Boolean,
        sourceMode: StockSourceMode?,
    ): WeaponStockSnapshot {
        var resolvedSourceMode = sourceMode ?: StockSourceMode.LOCAL
        if (!resolvedSourceMode.isEnabled) {
            resolvedSourceMode = StockSourceMode.LOCAL
        }
        val ownedSourcePolicy = config.ownedSourcePolicy(includeCurrentMarketStorage)
        val desiredStockService = DesiredStockService(config)
        val owned = inventoryCountService.collectOwnedItemCounts(sector, market, ownedSourcePolicy)
        val playerCargoCounts = InventoryCountService.collectCargoItemCounts(playerCargo(sector))
        val marketStock = marketStock(sector, market, includeBlackMarket, resolvedSourceMode)

        val ids = HashSet<String>()
        addTradeableIds(ids, playerCargoCounts, marketStock)

        val grouped = EnumMap<StockCategory, MutableList<WeaponStockRecord>>(StockCategory::class.java)
        for (category in StockCategory.values()) {
            grouped[category] = ArrayList()
        }

        for (itemKey in ids) {
            val itemType = StockItemType.fromKey(itemKey)
            val itemId = StockItemType.rawId(itemKey)
            val spec = if (StockItemType.WEAPON == itemType) safeWeaponSpec(itemId) else null
            val wingSpec = if (StockItemType.WING == itemType) safeWingSpec(itemId) else null
            if (spec == null && wingSpec == null) continue
            val displayName = displayName(itemType, spec, wingSpec) ?: continue
            if (config.isIgnored(itemKey) || config.isIgnored(itemId)) continue

            val ownedCount = getCount(owned, itemKey)
            val purchasableCount = marketStock.getTotal(itemKey)
            if (!shouldInclude(getCount(playerCargoCounts, itemKey), marketStock.getSubmarketStocks(itemKey))) continue

            val desiredCount = if (StockItemType.WING == itemType) {
                desiredStockService.desiredWingCount(itemId, wingSpec)
            } else {
                desiredStockService.desiredCount(itemId, spec)
            }
            val category = classifyStock(ownedCount, desiredCount)
            grouped[category]?.add(
                WeaponStockRecord(
                    itemType,
                    itemId,
                    displayName,
                    spec,
                    wingSpec,
                    ownedCount,
                    getCount(playerCargoCounts, itemKey),
                    purchasableCount,
                    desiredCount,
                    category,
                    marketStock.getSubmarketStocks(itemKey),
                    marketStock.getRarity(itemKey),
                )
            )
        }

        for (records in grouped.values) {
            Collections.sort(records, comparatorFor(sortMode))
        }

        return WeaponStockSnapshot(
            market,
            ownedSourcePolicy,
            sortMode,
            if (resolvedSourceMode.isRemote()) false else includeBlackMarket,
            resolvedSourceMode,
            grouped,
        )
    }

    private fun marketStock(
        sector: SectorAPI?,
        market: MarketAPI?,
        includeBlackMarket: Boolean,
        sourceMode: StockSourceMode,
    ): MarketStockService.MarketStock {
        if (StockSourceMode.FIXERS == sourceMode) {
            return globalWeaponMarketService.collectFixersWeaponStock(sector)
        }
        if (StockSourceMode.SECTOR == sourceMode) {
            return globalWeaponMarketService.collectSectorWeaponStock(sector)
        }
        return marketStockService.collectCurrentMarketItemStock(market, includeBlackMarket)
    }

    companion object {
        @JvmStatic
        private fun classifyStock(ownedCount: Int, desiredCount: Int): StockCategory {
            if (desiredCount <= 0 || ownedCount >= desiredCount) return StockCategory.SUFFICIENT
            if (ownedCount <= 0) return StockCategory.NO_STOCK
            return StockCategory.INSUFFICIENT
        }

        private fun addTradeableIds(
            ids: MutableSet<String>,
            playerCargoCounts: Map<String, Int>,
            marketStock: MarketStockService.MarketStock,
        ) {
            ids.addAll(playerCargoCounts.keys)
            for (id in marketStock.itemKeys()) {
                if (hasPurchasableStock(marketStock.getSubmarketStocks(id))) {
                    ids.add(id)
                }
            }
        }

        private fun shouldInclude(playerCargoCount: Int, marketStocks: List<SubmarketWeaponStock>?): Boolean {
            return playerCargoCount > 0 || hasPurchasableStock(marketStocks)
        }

        private fun hasPurchasableStock(marketStocks: List<SubmarketWeaponStock>?): Boolean {
            if (marketStocks == null) return false
            for (stock in marketStocks) {
                if (stock.isPurchasable() && stock.count > 0) return true
            }
            return false
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

        private fun displayName(
            itemType: StockItemType,
            weaponSpec: WeaponSpecAPI?,
            wingSpec: FighterWingSpecAPI?,
        ): String? {
            return if (StockItemType.WING == itemType) {
                wingSpec?.wingName
            } else {
                weaponSpec?.weaponName
            }
        }

        private fun playerCargo(sector: SectorAPI?): CargoAPI? {
            val fleet = sector?.playerFleet
            return fleet?.cargo
        }

        private fun getCount(counts: Map<String, Int>, id: String?): Int {
            return counts[id] ?: 0
        }

        private fun comparatorFor(sortMode: StockSortMode?): Comparator<WeaponStockRecord> {
            if (StockSortMode.NAME == sortMode) return NameComparator.INSTANCE
            if (StockSortMode.PRICE == sortMode) return PriceComparator.INSTANCE
            return NeedComparator.INSTANCE
        }

        private fun compareByNeedPriceName(left: WeaponStockRecord, right: WeaponStockRecord): Int {
            var result = compareByNeed(left, right)
            if (result != 0) return result
            result = compareByPrice(left, right)
            if (result != 0) return result
            return left.displayName.orEmpty().compareTo(right.displayName.orEmpty(), ignoreCase = true)
        }

        private fun compareByNeed(left: WeaponStockRecord, right: WeaponStockRecord): Int {
            return left.storageCount.compareTo(right.storageCount)
        }

        private fun compareByPrice(left: WeaponStockRecord, right: WeaponStockRecord): Int {
            return left.cheapestPurchasableUnitPrice.compareTo(right.cheapestPurchasableUnitPrice)
        }
    }

    private class NeedComparator private constructor() : Comparator<WeaponStockRecord> {
        override fun compare(left: WeaponStockRecord, right: WeaponStockRecord): Int {
            return compareByNeedPriceName(left, right)
        }

        companion object {
            @JvmField
            val INSTANCE: NeedComparator = NeedComparator()
        }
    }

    private class NameComparator private constructor() : Comparator<WeaponStockRecord> {
        override fun compare(left: WeaponStockRecord, right: WeaponStockRecord): Int {
            var result = left.displayName.orEmpty().compareTo(right.displayName.orEmpty(), ignoreCase = true)
            if (result != 0) return result
            result = compareByNeed(left, right)
            if (result != 0) return result
            return compareByPrice(left, right)
        }

        companion object {
            @JvmField
            val INSTANCE: NameComparator = NameComparator()
        }
    }

    private class PriceComparator private constructor() : Comparator<WeaponStockRecord> {
        override fun compare(left: WeaponStockRecord, right: WeaponStockRecord): Int {
            var result = compareByPrice(left, right)
            if (result != 0) return result
            result = right.neededCount.compareTo(left.neededCount)
            if (result != 0) return result
            return left.displayName.orEmpty().compareTo(right.displayName.orEmpty(), ignoreCase = true)
        }

        companion object {
            @JvmField
            val INSTANCE: PriceComparator = PriceComparator()
        }
    }

    /**
     * Compatibility shim for games that hot-loaded an older outer class while a
     * newer jar was copied underneath it. Remove only after this class has been
     * in public builds long enough that stale runtime references are unlikely.
     */
    private class CostComparator private constructor() : Comparator<WeaponStockRecord> {
        override fun compare(left: WeaponStockRecord, right: WeaponStockRecord): Int {
            return PriceComparator.INSTANCE.compare(left, right)
        }

        companion object {
            @JvmField
            val INSTANCE: CostComparator = CostComparator()
        }
    }
}
