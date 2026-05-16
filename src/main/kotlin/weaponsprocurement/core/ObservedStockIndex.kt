package weaponsprocurement.core

import com.fs.starfarer.api.campaign.SectorAPI
import com.fs.starfarer.api.impl.campaign.ids.Submarkets
import java.util.HashMap

class ObservedStockIndex {
    private val marketStockService = MarketStockService()

    fun collect(sector: SectorAPI?, blacklist: WeaponMarketBlacklist?): Map<String, ObservedItem> {
        val economy = sector?.economy
        val markets = economy?.marketsCopy
        if (markets == null || markets.isEmpty()) return emptyMap()

        val result = HashMap<String, ObservedItem>()
        for (market in markets) {
            val stock = marketStockService.collectCurrentMarketItemStock(market, true)
            for (itemKey in stock.itemKeys()) {
                if (!isEligible(itemKey, blacklist)) continue
                val sources = stock.getSubmarketStocks(itemKey)
                for (source in sources) {
                    if (source == null || source.count <= 0 || !source.isPurchasable()) continue
                    val current = result[itemKey]
                    val vanillaSupported = isVanillaSupportedSubmarket(source.submarketId)
                    if (current == null) {
                        result[itemKey] = ObservedItem(source, vanillaSupported)
                    } else {
                        current.observe(source, vanillaSupported)
                    }
                }
            }
        }
        return result
    }

    class ObservedItem private constructor(
        private var cheapest: SubmarketWeaponStock?,
        private var vanillaSupportedSeen: Boolean,
        private var unsupportedSeen: Boolean,
    ) {
        constructor(source: SubmarketWeaponStock, vanillaSupported: Boolean) : this(null, false, false) {
            observe(source, vanillaSupported)
        }

        fun observe(source: SubmarketWeaponStock, vanillaSupported: Boolean) {
            if (vanillaSupported) {
                vanillaSupportedSeen = true
            } else {
                unsupportedSeen = true
            }
            val currentCheapest = cheapest
            if (currentCheapest == null || compareReferenceSource(source, currentCheapest) < 0) {
                cheapest = source
            }
        }

        val cheapestReferenceSource: SubmarketWeaponStock?
            get() = cheapest

        val isOnlyUnsupportedCustomSubmarket: Boolean
            get() = unsupportedSeen && !vanillaSupportedSeen

        companion object {
            private fun compareReferenceSource(left: SubmarketWeaponStock, right: SubmarketWeaponStock): Int {
                val result = left.baseUnitPrice.compareTo(right.baseUnitPrice)
                if (result != 0) return result
                return (left.displaySourceName ?: "").compareTo(right.displaySourceName ?: "", ignoreCase = true)
            }
        }
    }

    companion object {
        private fun isEligible(itemKey: String, blacklist: WeaponMarketBlacklist?): Boolean {
            return FixerMarketObservedCatalog.isSafeFixerItem(itemKey) &&
                (blacklist == null || !blacklist.isBannedFromFixers(itemKey))
        }

        @JvmStatic
        fun isVanillaSupportedSubmarket(submarketId: String?): Boolean {
            return Submarkets.SUBMARKET_OPEN == submarketId ||
                Submarkets.GENERIC_MILITARY == submarketId ||
                Submarkets.SUBMARKET_BLACK == submarketId
        }
    }
}
