package weaponsprocurement.stock.market

import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.CargoStackAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Submarkets
import weaponsprocurement.stock.fixer.FixerRarity
import weaponsprocurement.stock.inventory.InventoryCountService
import weaponsprocurement.stock.item.StockItemStacks
import weaponsprocurement.stock.item.StockItemType
import weaponsprocurement.stock.item.SubmarketWeaponStock
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap

class MarketStockService {
    fun collectCurrentMarketItemStock(market: MarketAPI?, includeBlackMarket: Boolean): MarketStock {
        val totals = HashMap<String, Int>()
        val byItemKey = HashMap<String, MutableList<SubmarketWeaponStock>>()
        val submarkets = market?.submarketsCopy
        if (submarkets == null) return MarketStock(totals, byItemKey)

        for (submarket in submarkets) {
            if (!isTradeSubmarket(submarket, includeBlackMarket)) continue
            val cargo: CargoAPI = submarket.cargoNullOk ?: continue
            val stacks = cargo.stacksCopy ?: continue
            for (stack: CargoStackAPI in stacks) {
                addVisibleStack(totals, byItemKey, market, submarket, stack, StockItemType.WEAPON)
                addVisibleStack(totals, byItemKey, market, submarket, stack, StockItemType.WING)
            }
        }

        return MarketStock(totals, byItemKey)
    }

    private fun addVisibleStack(
        totals: MutableMap<String, Int>,
        byItemKey: MutableMap<String, MutableList<SubmarketWeaponStock>>,
        market: MarketAPI,
        submarket: SubmarketAPI,
        stack: CargoStackAPI,
        itemType: StockItemType,
    ) {
        if (!StockItemStacks.isVisibleItemStack(stack, itemType)) return
        val itemKey = itemType.key(StockItemStacks.itemId(stack, itemType))
        val count = Math.round(stack.size)
        if (count <= 0) return
        InventoryCountService.add(totals, itemKey, count)
        var stocks = byItemKey[itemKey]
        if (stocks == null) {
            stocks = ArrayList()
            byItemKey[itemKey] = stocks
        }
        stocks.add(
            SubmarketWeaponStock(
                market.id,
                market.name,
                submarket.specId,
                submarket.nameOneLine,
                count,
                StockItemStacks.unitPrice(submarket, stack),
                StockItemStacks.baseUnitPrice(stack),
                StockItemStacks.unitCargoSpace(stack),
                StockItemStacks.isPurchasableItemStack(submarket, stack, itemType),
            )
        )
    }

    class MarketStock(
        private val totals: Map<String, Int>,
        private val byItemKey: Map<String, MutableList<SubmarketWeaponStock>>,
        private val rarityByItemKey: Map<String, FixerRarity>,
    ) {
        constructor(
            totals: Map<String, Int>,
            byItemKey: Map<String, MutableList<SubmarketWeaponStock>>,
        ) : this(totals, byItemKey, emptyMap())

        fun getTotal(itemKey: String?): Int = totals[itemKey] ?: 0

        fun getSubmarketStocks(itemKey: String?): List<SubmarketWeaponStock> {
            val stocks = byItemKey[itemKey] ?: return Collections.emptyList()
            return Collections.unmodifiableList(stocks)
        }

        fun itemKeys(): Iterable<String> = totals.keys

        fun getRarity(itemKey: String?): FixerRarity? = rarityByItemKey[itemKey]
    }

    class MarketStockBuilder {
        private val totals = HashMap<String, Int>()
        private val byItemKey = HashMap<String, MutableList<SubmarketWeaponStock>>()
        private val rarityByItemKey = HashMap<String, FixerRarity>()

        fun add(itemKey: String?, stock: SubmarketWeaponStock?) {
            add(itemKey, stock, null)
        }

        fun add(itemKey: String?, stock: SubmarketWeaponStock?, rarity: FixerRarity?) {
            if (itemKey.isNullOrEmpty() || stock == null || stock.count <= 0) return
            InventoryCountService.add(totals, itemKey, stock.count)
            var stocks = byItemKey[itemKey]
            if (stocks == null) {
                stocks = ArrayList()
                byItemKey[itemKey] = stocks
            }
            stocks.add(stock)
            if (rarity != null && !rarityByItemKey.containsKey(itemKey)) {
                rarityByItemKey[itemKey] = rarity
            }
        }

        fun addAll(stock: MarketStock?) {
            if (stock == null) return
            for (id in stock.itemKeys()) {
                val sources = stock.getSubmarketStocks(id)
                for (i in sources.indices) {
                    add(id, sources[i])
                }
            }
        }

        fun build(): MarketStock = MarketStock(totals, byItemKey, rarityByItemKey)
    }

    companion object {
        @JvmStatic
        fun isTradeSubmarket(submarket: SubmarketAPI?, includeBlackMarket: Boolean): Boolean {
            if (submarket == null) return false
            val id = submarket.specId
            if (isNonTradeSubmarket(id)) return false
            if (!includeBlackMarket && Submarkets.SUBMARKET_BLACK == id) return false
            return submarket.cargoNullOk != null
        }

        @JvmStatic
        fun isNonTradeSubmarket(submarketId: String?): Boolean {
            return Submarkets.SUBMARKET_STORAGE == submarketId || Submarkets.LOCAL_RESOURCES == submarketId
        }

        @JvmStatic
        fun isBlackMarketSubmarket(submarketId: String?): Boolean {
            return Submarkets.SUBMARKET_BLACK == submarketId
        }
    }
}
