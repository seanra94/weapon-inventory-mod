package weaponsprocurement.gui

import weaponsprocurement.core.TradeMoney
import java.util.HashMap

class StockReviewPortfolioQuote {
    private val quotesByLine = HashMap<String, StockReviewQuote>()
    private val costByItem = HashMap<String, Long>()

    private var totalCostValue = 0L
    private var totalBuyCost = 0L
    private var totalBaseBuyCost = 0L
    private var totalBuyQuantityValue = 0
    private var totalCargoSpaceDeltaValue = 0f
    private var priceUnavailable = false

    fun addLine(trade: StockReviewPendingTrade, quote: StockReviewQuote) {
        val lineKey = lineKey(trade.itemKey, trade.submarketId)
        quotesByLine[lineKey] = quote
        if (quote.cost == StockReviewQuoteBook.PRICE_UNAVAILABLE.toLong()) {
            priceUnavailable = true
            costByItem[trade.itemKey] = StockReviewQuoteBook.PRICE_UNAVAILABLE.toLong()
        } else {
            add(costByItem, trade.itemKey, quote.cost)
            totalCostValue = TradeMoney.safeAdd(totalCostValue, quote.cost)
            if (trade.isBuy()) {
                totalBuyCost = TradeMoney.safeAdd(totalBuyCost, quote.cost)
                totalBaseBuyCost = TradeMoney.safeAdd(totalBaseBuyCost, quote.baseCost)
                totalBuyQuantityValue += quote.buyQuantity
            }
        }
        totalCargoSpaceDeltaValue += quote.cargoSpaceDelta
    }

    fun totalCost(): Long = if (priceUnavailable) StockReviewQuoteBook.PRICE_UNAVAILABLE.toLong() else totalCostValue

    fun totalCargoSpaceDelta(): Float = totalCargoSpaceDeltaValue

    fun costForItem(itemKey: String?): Long = get(costByItem, itemKey)

    fun totalMarkupPaid(): Long {
        if (priceUnavailable) return 0L
        return Math.max(0L, totalBuyCost - totalBaseBuyCost)
    }

    fun averageBuyMultiplier(): Float {
        if (totalBaseBuyCost <= 0) return 1f
        return totalBuyCost.toFloat() / totalBaseBuyCost.toFloat()
    }

    fun totalBuyQuantity(): Int = totalBuyQuantityValue

    fun costForLine(itemKey: String?, submarketId: String?): Long {
        return quoteForLine(itemKey, submarketId).cost
    }

    fun cargoSpaceForLine(itemKey: String?, submarketId: String?): Float {
        return quoteForLine(itemKey, submarketId).cargoSpaceDelta
    }

    fun sellerAllocations(itemKey: String?, submarketId: String?): List<StockReviewSellerAllocation> {
        return quoteForLine(itemKey, submarketId).sellerAllocations
    }

    private fun quoteForLine(itemKey: String?, submarketId: String?): StockReviewQuote {
        return quotesByLine[lineKey(itemKey, submarketId)] ?: StockReviewQuote.ZERO
    }

    companion object {
        @JvmStatic
        fun lineKey(itemKey: String?, submarketId: String?): String {
            return (itemKey ?: "") + "|" + (submarketId ?: "")
        }

        private fun add(counts: MutableMap<String, Long>, itemKey: String?, quantity: Long) {
            if (itemKey == null || quantity == 0L) return
            counts[itemKey] = TradeMoney.safeAdd(get(counts, itemKey), quantity)
        }

        private fun get(counts: Map<String, Long>, itemKey: String?): Long {
            return counts[itemKey] ?: 0L
        }
    }
}
