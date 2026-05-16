package weaponsprocurement.ui.stockreview.rendering

import weaponsprocurement.ui.stockreview.trade.StockReviewQuoteBook
import weaponsprocurement.trade.quote.CreditFormat
import kotlin.math.abs

class StockReviewFormat private constructor() {
    companion object {
        @JvmStatic
        fun credits(credits: Long): String {
            if (credits == StockReviewQuoteBook.PRICE_UNAVAILABLE.toLong()) return "?"
            return CreditFormat.credits(abs(credits))
        }
    }
}