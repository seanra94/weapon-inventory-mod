package weaponsprocurement.ui.stockreview

import weaponsprocurement.ui.*

import java.util.Collections

class StockReviewQuote(
    val cost: Long,
    val cargoSpaceDelta: Float,
    val baseCost: Long,
    val buyQuantity: Int,
    sellerAllocations: List<StockReviewSellerAllocation>,
) {
    constructor(
        cost: Long,
        cargoSpaceDelta: Float,
        sellerAllocations: List<StockReviewSellerAllocation>,
    ) : this(cost, cargoSpaceDelta, cost, 0, sellerAllocations)

    val sellerAllocations: List<StockReviewSellerAllocation> = Collections.unmodifiableList(sellerAllocations)

    val markupPaid: Long
        get() {
            if (cost == StockReviewQuoteBook.PRICE_UNAVAILABLE.toLong()) return 0L
            return Math.max(0L, cost - baseCost)
        }

    companion object {
        @JvmField
        val ZERO = StockReviewQuote(0L, 0f, 0L, 0, Collections.emptyList())

        @JvmStatic
        fun priceUnavailable(): StockReviewQuote {
            return StockReviewQuote(StockReviewQuoteBook.PRICE_UNAVAILABLE.toLong(), 0f, Collections.emptyList())
        }

        @JvmStatic
        fun priceUnavailable(
            cargoSpaceDelta: Float,
            sellerAllocations: List<StockReviewSellerAllocation>,
        ): StockReviewQuote {
            return StockReviewQuote(StockReviewQuoteBook.PRICE_UNAVAILABLE.toLong(), cargoSpaceDelta, sellerAllocations)
        }
    }
}
