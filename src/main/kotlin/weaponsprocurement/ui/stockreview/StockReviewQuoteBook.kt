package weaponsprocurement.ui.stockreview

import weaponsprocurement.ui.*

import weaponsprocurement.stock.SubmarketWeaponStock
import weaponsprocurement.trade.TradeMoney
import weaponsprocurement.stock.WeaponStockRecord
import weaponsprocurement.stock.WeaponStockSnapshot
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.HashMap

class StockReviewQuoteBook(private val snapshot: WeaponStockSnapshot?) {
    private val sortedBuyStocksByItem = HashMap<String, List<SubmarketWeaponStock>>()
    private val sellUnitPriceByItem = HashMap<String, Int>()
    private val unitCargoSpaceByItem = HashMap<String, Float>()
    private val quotesByLine = HashMap<String, StockReviewQuote>()
    private var playerSellUnitPrices: Map<String, Int>? = null

    fun sellerAllocations(trade: StockReviewPendingTrade?): List<StockReviewSellerAllocation> {
        return quote(trade).sellerAllocations
    }

    fun quotePortfolio(pendingTrades: List<StockReviewPendingTrade>?): StockReviewPortfolioQuote {
        val result = StockReviewPortfolioQuote()
        if (pendingTrades == null || pendingTrades.isEmpty()) return result

        val remainingBySource = HashMap<String, Int>()
        val ordered = StockReviewTradePlanner.executionOrder(pendingTrades)
        for (trade in ordered) {
            val quote = if (trade.isBuy()) {
                quoteBuyWithRemaining(trade, remainingBySource)
            } else {
                quote(trade)
            }
            result.addLine(trade, quote)
        }
        return result
    }

    fun cheapestUnitPrice(record: WeaponStockRecord?): Int {
        val stocks = sortedBuyStocks(record)
        if (stocks.isEmpty()) return Int.MAX_VALUE
        return stocks[0].unitPrice
    }

    fun nextBuyUnitPriceAfterPlannedBuys(record: WeaponStockRecord?, plannedBuyQuantity: Int): Int {
        var planned = Math.max(0, plannedBuyQuantity)
        val stocks = sortedBuyStocks(record)
        for (stock in stocks) {
            if (planned >= stock.count) {
                planned -= stock.count
                continue
            }
            return stock.unitPrice
        }
        return Int.MAX_VALUE
    }

    private fun quote(trade: StockReviewPendingTrade?): StockReviewQuote {
        if (trade == null || trade.isZero()) return StockReviewQuote.ZERO
        val key = lineKey(trade)
        val cached = quotesByLine[key]
        if (cached != null) return cached
        val result = if (trade.isSell()) quoteSell(trade) else quoteBuy(trade)
        quotesByLine[key] = result
        return result
    }

    private fun quoteSell(trade: StockReviewPendingTrade): StockReviewQuote {
        val unitPrice = sellUnitPrice(trade.itemKey)
        if (unitPrice < 0) return StockReviewQuote.priceUnavailable()
        if (trade.quantity == Int.MIN_VALUE) return StockReviewQuote.priceUnavailable()

        val sellQuantity = -trade.quantity
        val credits = TradeMoney.lineTotal(unitPrice, sellQuantity)
        if (!TradeMoney.canExecuteCreditMutation(credits)) return StockReviewQuote.priceUnavailable()

        val cargo = -sellQuantity * fallbackUnitCargoSpace(trade.itemKey)
        return StockReviewQuote(-credits, cargo, Collections.emptyList())
    }

    private fun quoteBuy(trade: StockReviewPendingTrade): StockReviewQuote {
        return quoteBuyWithRemaining(trade, null)
    }

    private fun quoteBuyWithRemaining(
        trade: StockReviewPendingTrade,
        remainingBySource: MutableMap<String, Int>?,
    ): StockReviewQuote {
        var remaining = trade.quantity
        var totalCost = 0L
        var totalBaseCost = 0L
        var totalQuantity = 0
        var totalCargo = 0f
        val allocations = ArrayList<StockReviewSellerAllocation>()
        val stocks = sortedBuyStocks(trade.itemKey)

        for (stock in stocks) {
            if (remaining <= 0) break
            if (trade.submarketId != null && !matchesSource(trade.submarketId, stock)) continue

            val available = if (remainingBySource == null) {
                stock.count
            } else {
                remainingStock(trade.itemKey, stock, remainingBySource)
            }
            val quantity = Math.min(remaining, available)
            if (quantity <= 0) continue

            val cost = TradeMoney.lineTotal(stock.unitPrice, quantity)
            totalCost = TradeMoney.safeAdd(totalCost, cost)
            totalBaseCost = TradeMoney.safeAdd(totalBaseCost, TradeMoney.lineTotal(stock.baseUnitPrice, quantity))
            totalQuantity += quantity
            totalCargo += quantity * stock.unitCargoSpace
            allocations.add(StockReviewSellerAllocation(stock.displaySourceName, stock.sourceId, quantity, cost))
            remaining -= quantity
            if (remainingBySource != null) {
                remainingBySource[sourceKey(trade.itemKey, stock)] = available - quantity
            }
        }

        if (remaining > 0) {
            return StockReviewQuote.priceUnavailable(totalCargo, allocations)
        }
        return StockReviewQuote(totalCost, totalCargo, totalBaseCost, totalQuantity, allocations)
    }

    private fun remainingStock(
        itemKey: String?,
        stock: SubmarketWeaponStock,
        remainingBySource: MutableMap<String, Int>,
    ): Int {
        val key = sourceKey(itemKey, stock)
        val cached = remainingBySource[key]
        if (cached != null) return cached
        remainingBySource[key] = stock.count
        return stock.count
    }

    private fun sortedBuyStocks(itemKey: String?): List<SubmarketWeaponStock> {
        if (itemKey == null) return Collections.emptyList()
        val cached = sortedBuyStocksByItem[itemKey]
        if (cached != null) return cached
        val record = findRecord(itemKey)
        if (record == null) {
            sortedBuyStocksByItem[itemKey] = Collections.emptyList()
            return Collections.emptyList()
        }
        return sortedBuyStocks(record)
    }

    private fun sortedBuyStocks(record: WeaponStockRecord?): List<SubmarketWeaponStock> {
        if (record == null) return Collections.emptyList()
        val itemKey = record.itemKey
        val cached = sortedBuyStocksByItem[itemKey]
        if (cached != null) return cached

        var result = ArrayList<SubmarketWeaponStock>()
        for (stock in record.submarketStocks) {
            if (stock.isPurchasable() && stock.count > 0) result.add(stock)
        }
        Collections.sort(result, SubmarketStockPriceComparator.INSTANCE)
        val immutable = Collections.unmodifiableList(result)
        sortedBuyStocksByItem[itemKey] = immutable
        return immutable
    }

    private fun fallbackUnitCargoSpace(itemKey: String?): Float {
        if (itemKey == null) return 1f
        val cached = unitCargoSpaceByItem[itemKey]
        if (cached != null) return cached

        var result = 1f
        val record = findRecord(itemKey)
        if (record != null) {
            for (stock in record.submarketStocks) {
                if (stock.unitCargoSpace > 0f) {
                    result = stock.unitCargoSpace
                    break
                }
            }
        }
        unitCargoSpaceByItem[itemKey] = result
        return result
    }

    fun sellUnitPrice(itemKey: String?): Int {
        if (itemKey == null) return -1
        val cached = sellUnitPriceByItem[itemKey]
        if (cached != null) return cached
        if (playerSellUnitPrices == null) {
            playerSellUnitPrices = StockReviewPlayerCargo.sellUnitPricesByItem(
                snapshot?.getMarket(),
                snapshot != null && snapshot.isIncludeBlackMarket(),
            )
        }
        val price = playerSellUnitPrices?.get(itemKey)
        val result = price ?: -1
        sellUnitPriceByItem[itemKey] = result
        return result
    }

    private fun findRecord(itemKey: String?): WeaponStockRecord? {
        if (snapshot == null || itemKey == null) return null
        return snapshot.getRecord(itemKey)
    }

    private class SubmarketStockPriceComparator private constructor() : Comparator<SubmarketWeaponStock> {
        override fun compare(left: SubmarketWeaponStock, right: SubmarketWeaponStock): Int {
            val result = left.unitPrice.compareTo(right.unitPrice)
            return if (result != 0) {
                result
            } else {
                left.displaySourceName.orEmpty().compareTo(right.displaySourceName.orEmpty(), ignoreCase = true)
            }
        }

        companion object {
            @JvmField
            val INSTANCE: SubmarketStockPriceComparator = SubmarketStockPriceComparator()
        }
    }

    companion object {
        const val PRICE_UNAVAILABLE: Int = Int.MIN_VALUE

        private fun lineKey(trade: StockReviewPendingTrade): String {
            return trade.itemKey + "|" + (trade.submarketId ?: "") + "|" + trade.quantity
        }

        private fun sourceKey(itemKey: String?, stock: SubmarketWeaponStock?): String {
            return (itemKey ?: "") + "|" + (stock?.sourceId ?: "")
        }

        private fun matchesSource(requestedSourceId: String?, stock: SubmarketWeaponStock?): Boolean {
            if (stock == null || requestedSourceId == null) return false
            return requestedSourceId == stock.sourceId || requestedSourceId == stock.submarketId
        }
    }
}
