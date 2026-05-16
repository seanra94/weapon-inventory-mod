package weaponsprocurement.gui

import weaponsprocurement.core.CreditFormat
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
