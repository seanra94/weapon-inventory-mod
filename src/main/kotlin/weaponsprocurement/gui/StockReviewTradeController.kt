package weaponsprocurement.gui

import weaponsprocurement.core.WeaponStockSnapshot
import kotlin.math.abs

class StockReviewTradeController(
    private val state: StockReviewState,
    private val pendingTrades: StockReviewPendingTrades,
    private val host: Host,
) {
    interface Host {
        fun snapshot(): WeaponStockSnapshot?
        fun updateTradeWarning(explicitWarning: String?)
        fun requestContentRebuild()
        fun postMessage(message: String?)
    }

    fun adjustPendingTrade(action: StockReviewAction) {
        val available = availableFor(action)
        if (available <= 0) {
            host.postMessage(
                if (action.getQuantity() < 0) {
                    "No more queued or player-cargo stock is available to remove from the plan."
                } else {
                    "No more queued sales or buyable stock is available for that plan."
                },
            )
            host.updateTradeWarning(null)
            host.requestContentRebuild()
            return
        }
        val requested = action.getQuantity()
        val quantity = if (requested > 0) Math.min(requested, available) else -Math.min(-requested, available)
        pendingTrades.adjustItemNet(action.getItemKey(), quantity)
        if (abs(quantity) < abs(requested)) {
            host.postMessage("Only ${abs(quantity)} more can be planned for that item.")
        }
        host.updateTradeWarning(null)
        host.requestContentRebuild()
    }

    fun resetPlan(itemKey: String?) {
        pendingTrades.resetItem(itemKey)
        host.updateTradeWarning(null)
        host.requestContentRebuild()
    }

    fun purchaseAllUntilSufficient() {
        val snapshot = host.snapshot() ?: return
        var added = 0
        var explicitWarning: String? = null
        val quoteBook = StockReviewQuoteBook(snapshot)
        val records = StockReviewTradePlanner.cheapestFirstVisibleBuyableRecords(snapshot)
        var tradeContext = StockReviewTradeContext(snapshot, pendingTrades.asList())
        for (record in records) {
            val needed = tradeContext.buyNeededForSufficiency(record)
            val quantity = tradeContext.affordableBuyQuantity(record, null, needed)
            explicitWarning = StockReviewTradeWarnings.purchaseAllLimitWarning(
                quoteBook,
                pendingTrades.asList(),
                record,
                tradeContext,
                needed,
                quantity,
                explicitWarning,
            )
            if (quantity <= 0) {
                continue
            }
            pendingTrades.add(record.itemKey, null, quantity)
            tradeContext = StockReviewTradeContext(snapshot, pendingTrades.asList())
            added += quantity
        }
        if (added <= 0) {
            host.postMessage("No additional sufficient-stock purchases are available.")
            host.updateTradeWarning(explicitWarning)
            host.requestContentRebuild()
            return
        }
        host.updateTradeWarning(explicitWarning)
        host.requestContentRebuild()
    }

    fun sellAllUntilSufficient() {
        val snapshot = host.snapshot() ?: return
        var removed = 0
        val records = StockReviewTradePlanner.visibleTradeableRecords(snapshot)
        var tradeContext = StockReviewTradeContext(snapshot, pendingTrades.asList())
        for (record in records) {
            val quantity = tradeContext.sellableUntilSufficient(record)
            if (quantity <= 0) {
                continue
            }
            pendingTrades.add(record.itemKey, null, -quantity)
            tradeContext = StockReviewTradeContext(snapshot, pendingTrades.asList())
            removed += quantity
        }
        if (removed <= 0) {
            host.postMessage("No sufficient-stock sales are available.")
            host.updateTradeWarning(null)
            host.requestContentRebuild()
            return
        }
        host.updateTradeWarning(null)
        host.requestContentRebuild()
    }

    private fun availableFor(action: StockReviewAction): Int {
        val snapshot = host.snapshot()
        val record = snapshot?.getRecord(action.getItemKey()) ?: return 0
        val tradeContext = StockReviewTradeContext(snapshot, pendingTrades.asList())
        if (action.getQuantity() < 0) {
            return tradeContext.negativeAdjustmentRemaining(record, -action.getQuantity())
        }
        if (action.getSubmarketId() == null) {
            return tradeContext.positiveAdjustmentRemaining(record, action.getQuantity())
        }
        return tradeContext.affordableBuyQuantity(record, action.getSubmarketId(), action.getQuantity())
    }
}
