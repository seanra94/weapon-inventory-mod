package weaponsprocurement.ui.stockreview.state

import weaponsprocurement.ui.stockreview.trade.StockReviewPendingTrade
import java.util.ArrayList
import java.util.Collections

class StockReviewLaunchState(
    state: StockReviewState?,
    pendingTrades: List<StockReviewPendingTrade>?,
    private val reviewMode: Boolean,
) {
    private val state: StockReviewState? = if (state == null) null else StockReviewState(state)
    private val pendingTrades: List<StockReviewPendingTrade> = immutableCopy(pendingTrades)

    fun getState(): StockReviewState? = state

    fun getPendingTrades(): List<StockReviewPendingTrade> = pendingTrades

    fun isReviewMode(): Boolean = reviewMode

    companion object {
        private fun immutableCopy(source: List<StockReviewPendingTrade>?): List<StockReviewPendingTrade> {
            if (source.isNullOrEmpty()) {
                return emptyList()
            }
            val result = ArrayList<StockReviewPendingTrade>()
            for (trade in source) {
                val copy = trade?.copy()
                if (copy != null) {
                    result.add(copy)
                }
            }
            return Collections.unmodifiableList(result)
        }
    }
}