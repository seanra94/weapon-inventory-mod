package weaponsprocurement.ui.stockreview.rendering

import weaponsprocurement.ui.stockreview.actions.*
import weaponsprocurement.ui.stockreview.state.*
import weaponsprocurement.ui.stockreview.rows.*
import weaponsprocurement.ui.stockreview.tooltips.*
import weaponsprocurement.ui.stockreview.rendering.*
import weaponsprocurement.ui.stockreview.trade.*

import weaponsprocurement.ui.*

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