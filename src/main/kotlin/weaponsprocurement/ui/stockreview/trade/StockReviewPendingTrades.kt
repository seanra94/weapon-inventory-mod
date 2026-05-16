package weaponsprocurement.ui.stockreview.trade

import weaponsprocurement.ui.stockreview.actions.*
import weaponsprocurement.ui.stockreview.state.*
import weaponsprocurement.ui.stockreview.rows.*
import weaponsprocurement.ui.stockreview.tooltips.*
import weaponsprocurement.ui.stockreview.rendering.*
import weaponsprocurement.ui.stockreview.trade.*

import weaponsprocurement.ui.*

import java.util.ArrayList
import java.util.Collections

class StockReviewPendingTrades {
    private val trades = ArrayList<StockReviewPendingTrade>()

    fun asList(): List<StockReviewPendingTrade> = Collections.unmodifiableList(trades)

    fun isEmpty(): Boolean = trades.isEmpty()

    fun clear() {
        trades.clear()
    }

    fun replaceWith(source: List<StockReviewPendingTrade>?) {
        trades.clear()
        if (source == null) return
        for (trade in source) {
            if (!trade.isZero()) {
                val copy = trade.copy()
                if (copy != null) trades.add(copy)
            }
        }
    }

    fun add(itemKey: String?, submarketId: String?, quantity: Int) {
        if (itemKey.isNullOrEmpty() || quantity == 0) return
        val existing = find(itemKey, submarketId)
        if (existing == null) {
            val trade = StockReviewPendingTrade.create(itemKey, submarketId, quantity)
            if (trade != null) trades.add(trade)
            return
        }
        existing.addQuantity(quantity)
        if (existing.isZero()) trades.remove(existing)
    }

    fun adjustItemNet(itemKey: String?, delta: Int) {
        if (itemKey.isNullOrEmpty() || delta == 0) return
        if (delta < 0) {
            val remaining = reduceExistingBuys(itemKey, -delta)
            if (remaining > 0) add(itemKey, null, -remaining)
            return
        }
        val remaining = reduceExistingSells(itemKey, delta)
        if (remaining > 0) add(itemKey, null, remaining)
    }

    fun resetItem(itemKey: String?) {
        for (i in trades.size - 1 downTo 0) {
            if (itemKey != null && itemKey == trades[i].itemKey) {
                trades.removeAt(i)
            }
        }
    }

    fun removeExecuted(executionOrder: List<StockReviewPendingTrade>?, failedIndex: Int) {
        if (executionOrder == null) return
        for (i in 0 until failedIndex) {
            removeMatching(executionOrder[i])
        }
    }

    private fun removeMatching(executed: StockReviewPendingTrade?) {
        if (executed == null) return
        for (i in trades.size - 1 downTo 0) {
            val trade = trades[i]
            if (trade.matches(executed.itemKey, executed.submarketId) &&
                trade.quantity == executed.quantity
            ) {
                trades.removeAt(i)
                return
            }
        }
    }

    private fun find(itemKey: String, submarketId: String?): StockReviewPendingTrade? {
        for (trade in trades) {
            if (trade.matches(itemKey, submarketId)) return trade
        }
        return null
    }

    private fun reduceExistingBuys(itemKey: String, quantity: Int): Int {
        var remaining = quantity
        var i = trades.size - 1
        while (i >= 0 && remaining > 0) {
            val trade = trades[i]
            if (itemKey != trade.itemKey || !trade.isBuy()) {
                i--
                continue
            }
            val reduced = Math.min(remaining, trade.quantity)
            trade.addQuantity(-reduced)
            remaining -= reduced
            if (trade.isZero()) trades.removeAt(i)
            i--
        }
        return remaining
    }

    private fun reduceExistingSells(itemKey: String, quantity: Int): Int {
        var remaining = quantity
        var i = trades.size - 1
        while (i >= 0 && remaining > 0) {
            val trade = trades[i]
            if (itemKey != trade.itemKey || !trade.isSell()) {
                i--
                continue
            }
            val reduced = Math.min(remaining, -trade.quantity)
            trade.addQuantity(reduced)
            remaining -= reduced
            if (trade.isZero()) trades.removeAt(i)
            i--
        }
        return remaining
    }
}