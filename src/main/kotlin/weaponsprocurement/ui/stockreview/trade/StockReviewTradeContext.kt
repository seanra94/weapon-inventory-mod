package weaponsprocurement.ui.stockreview.trade

import weaponsprocurement.ui.stockreview.actions.*
import weaponsprocurement.ui.stockreview.state.*
import weaponsprocurement.ui.stockreview.rows.*
import weaponsprocurement.ui.stockreview.tooltips.*
import weaponsprocurement.ui.stockreview.rendering.*
import weaponsprocurement.ui.stockreview.trade.*

import weaponsprocurement.ui.*

import weaponsprocurement.stock.item.SubmarketWeaponStock
import weaponsprocurement.stock.item.WeaponStockRecord
import weaponsprocurement.stock.item.WeaponStockSnapshot
import java.util.HashMap

class StockReviewTradeContext(
    snapshot: WeaponStockSnapshot?,
    private val pendingTrades: List<StockReviewPendingTrade>?,
) {
    private val quoteBook = StockReviewQuoteBook(snapshot)
    private val netByItem = HashMap<String, Int>()
    private val buyByItem = HashMap<String, Int>()
    private val sellByItem = HashMap<String, Int>()
    private val affordableCache = HashMap<String, Int>()
    private val portfolioQuote: StockReviewPortfolioQuote
    private val totalCostValue: Long
    private val totalCargoSpaceDeltaValue: Float
    private val creditsValue: Float
    private val cargoSpaceLeftValue: Float

    init {
        if (pendingTrades != null) {
            for (trade in pendingTrades) {
                add(netByItem, trade.itemKey, trade.quantity)
                if (trade.quantity > 0) {
                    add(buyByItem, trade.itemKey, trade.quantity)
                } else if (trade.quantity < 0) {
                    add(sellByItem, trade.itemKey, -trade.quantity)
                }
            }
        }
        portfolioQuote = quoteBook.quotePortfolio(pendingTrades)
        totalCostValue = portfolioQuote.totalCost()
        totalCargoSpaceDeltaValue = portfolioQuote.totalCargoSpaceDelta()
        creditsValue = StockReviewPlayerCargo.currentCredits()
        cargoSpaceLeftValue = StockReviewPlayerCargo.currentCargoSpaceLeft()
    }

    fun netQuantityForItem(itemKey: String?): Int = get(netByItem, itemKey)

    fun pendingBuyQuantityForItem(itemKey: String?): Int = get(buyByItem, itemKey)

    fun pendingSellQuantityForItem(itemKey: String?): Int = get(sellByItem, itemKey)

    fun buyableRemaining(record: WeaponStockRecord): Int {
        return Math.max(0, record.buyableCount - pendingBuyQuantityForItem(record.itemKey))
    }

    fun sellableRemaining(record: WeaponStockRecord): Int {
        return Math.max(0, record.playerCargoCount - pendingSellQuantityForItem(record.itemKey))
    }

    fun positiveAdjustmentRemaining(record: WeaponStockRecord, requestedQuantity: Int): Int {
        val requested = Math.max(0, requestedQuantity)
        if (requested <= 0) return 0
        val sellCancellation = Math.min(requested, pendingSellQuantityForItem(record.itemKey))
        val remainingRequest = requested - sellCancellation
        if (remainingRequest <= 0) return sellCancellation
        return sellCancellation + affordableBuyQuantity(record, null, remainingRequest)
    }

    fun negativeAdjustmentRemaining(record: WeaponStockRecord, requestedQuantity: Int): Int {
        val requested = Math.max(0, requestedQuantity)
        if (requested <= 0) return 0
        val buyCancellation = Math.min(requested, pendingBuyQuantityForItem(record.itemKey))
        val remainingRequest = requested - buyCancellation
        if (remainingRequest <= 0) return buyCancellation
        return buyCancellation + Math.min(remainingRequest, sellableRemaining(record))
    }

    fun buyNeededForSufficiency(record: WeaponStockRecord): Int {
        return Math.max(0, record.desiredCount - (record.ownedCount + netQuantityForItem(record.itemKey)))
    }

    fun sellableUntilSufficient(record: WeaponStockRecord): Int {
        val stockAfterPlan = record.ownedCount + netQuantityForItem(record.itemKey)
        val excess = Math.max(0, stockAfterPlan - record.desiredCount)
        return Math.min(excess, sellableRemaining(record))
    }

    fun transactionCostForItem(itemKey: String?): Long = portfolioQuote.costForItem(itemKey)

    fun transactionCostForLine(itemKey: String?, submarketId: String?): Long {
        return portfolioQuote.costForLine(itemKey, submarketId)
    }

    fun unitPriceForItem(record: WeaponStockRecord?): Int {
        if (record == null) return StockReviewQuoteBook.PRICE_UNAVAILABLE
        val unitCost = quoteBook.nextBuyUnitPriceAfterPlannedBuys(record, pendingBuyQuantityForItem(record.itemKey))
        return if (unitCost == Int.MAX_VALUE) quoteBook.sellUnitPrice(record.itemKey) else unitCost
    }

    fun deltaToSufficient(record: WeaponStockRecord): Int {
        val targetNet = record.desiredCount - record.ownedCount
        val delta = targetNet - netQuantityForItem(record.itemKey)
        if (delta > 0) return positiveAdjustmentRemaining(record, delta)
        if (delta < 0) return -negativeAdjustmentRemaining(record, -delta)
        return 0
    }

    fun sellerAllocations(trade: StockReviewPendingTrade?): List<StockReviewSellerAllocation> {
        if (trade == null) return StockReviewQuote.ZERO.sellerAllocations
        return portfolioQuote.sellerAllocations(trade.itemKey, trade.submarketId)
    }

    fun totalCost(): Long = totalCostValue

    fun totalCargoSpaceDelta(): Float = totalCargoSpaceDeltaValue

    fun totalMarkupPaid(): Long = portfolioQuote.totalMarkupPaid()

    fun averageBuyMultiplier(): Float = portfolioQuote.averageBuyMultiplier()

    fun credits(): Float = creditsValue

    fun cargoSpaceLeft(): Float = cargoSpaceLeftValue

    fun canConfirm(): Boolean {
        return totalCostValue != StockReviewQuoteBook.PRICE_UNAVAILABLE.toLong() &&
            totalCostValue <= creditsValue &&
            totalCargoSpaceDeltaValue <= cargoSpaceLeftValue + 0.01f
    }

    fun affordableBuyQuantity(record: WeaponStockRecord, submarketId: String?, requestedQuantity: Int): Int {
        var maxByStock = Math.min(Math.max(0, requestedQuantity), buyableRemaining(record))
        if (submarketId != null) {
            maxByStock = Math.min(maxByStock, submarketRemaining(record, submarketId))
        }
        if (maxByStock <= 0) return 0

        val key = record.itemKey + "|" + (submarketId ?: "") + "|" + maxByStock
        val cached = affordableCache[key]
        if (cached != null) return cached

        var low = 0
        var high = maxByStock
        while (low < high) {
            val candidate = (low + high + 1) / 2
            if (canAffordAdjustment(record, submarketId, candidate)) {
                low = candidate
            } else {
                high = candidate - 1
            }
        }
        affordableCache[key] = low
        return low
    }

    private fun canAffordAdjustment(record: WeaponStockRecord, submarketId: String?, quantity: Int): Boolean {
        val adjusted = quoteBook.quotePortfolio(
            StockReviewTradePlanner.withAdjustment(pendingTrades, record.itemKey, submarketId, quantity)
        )
        val adjustedCost = adjusted.totalCost()
        if (adjustedCost == StockReviewQuoteBook.PRICE_UNAVAILABLE.toLong()) return false
        if (adjustedCost > creditsValue) return false
        return adjusted.totalCargoSpaceDelta() <= cargoSpaceLeftValue + 0.01f
    }

    private fun submarketRemaining(record: WeaponStockRecord, submarketId: String): Int {
        var sourceCount = 0
        for (stock: SubmarketWeaponStock in record.submarketStocks) {
            if ((submarketId == stock.sourceId || submarketId == stock.submarketId) && stock.isPurchasable()) {
                sourceCount += stock.count
            }
        }

        var pendingFromSource = 0
        if (pendingTrades != null) {
            for (trade in pendingTrades) {
                if (trade.matches(record.itemKey, submarketId) && trade.quantity > 0) {
                    pendingFromSource += trade.quantity
                }
            }
        }
        return Math.max(0, sourceCount - pendingFromSource)
    }

    companion object {
        private fun add(counts: MutableMap<String, Int>, itemKey: String?, quantity: Int) {
            if (itemKey == null || quantity == 0) return
            counts[itemKey] = get(counts, itemKey) + quantity
        }

        private fun get(counts: Map<String, Int>, itemKey: String?): Int {
            return counts[itemKey] ?: 0
        }
    }
}