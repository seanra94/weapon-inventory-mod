package weaponsprocurement.ui.stockreview.rendering

import weaponsprocurement.ui.stockreview.actions.*
import weaponsprocurement.ui.stockreview.state.*
import weaponsprocurement.ui.stockreview.rows.*
import weaponsprocurement.ui.stockreview.tooltips.*
import weaponsprocurement.ui.stockreview.rendering.*
import weaponsprocurement.ui.stockreview.trade.*

import weaponsprocurement.ui.*

class StockReviewUiController(
    private val state: StockReviewState,
    private val modes: StockReviewModeController,
    private val pendingTrades: StockReviewPendingTrades,
    private val host: Host,
) {
    interface Host {
        fun currentMaxScrollOffset(): Int
        fun updateTradeWarning(explicitWarning: String?)
        fun rebuildSnapshot()
        fun requestContentRebuild()
        fun reopen(review: Boolean)
        fun requestClose()
    }

    fun handleCloseRequested() {
        if (modes.isColorDebugMode()) {
            modes.leaveColorDebug(state)
            host.requestContentRebuild()
            return
        }
        if (modes.isFilterMode()) {
            modes.leaveFilters(state)
            host.requestContentRebuild()
            return
        }
        if (modes.isReviewMode()) {
            state.setListScrollOffset(0)
            host.reopen(false)
            return
        }
        host.requestClose()
    }

    fun handle(action: StockReviewAction?): Boolean {
        if (action == null) {
            return true
        }
        val type = action.getType()
        if (StockReviewAction.Type.TOGGLE_CATEGORY == type) {
            state.toggle(action.getItemType(), action.getCategory())
            host.requestContentRebuild()
            return true
        }
        if (StockReviewAction.Type.TOGGLE_ITEM_TYPE == type) {
            state.toggle(action.getItemType())
            host.requestContentRebuild()
            return true
        }
        if (StockReviewAction.Type.TOGGLE_TRADE_GROUP == type) {
            state.toggle(action.getTradeGroup())
            host.requestContentRebuild()
            return true
        }
        if (StockReviewAction.Type.TOGGLE_ITEM == type) {
            state.toggleItem(action.getItemKey())
            host.requestContentRebuild()
            return true
        }
        if (StockReviewAction.Type.CYCLE_SORT_MODE == type) {
            state.cycleSortMode()
            host.rebuildSnapshot()
            host.requestContentRebuild()
            return true
        }
        if (StockReviewAction.Type.CYCLE_SOURCE_MODE == type) {
            state.cycleSourceMode()
            resetTradeStateForSourceChange()
            state.setListScrollOffset(0)
            host.rebuildSnapshot()
            host.requestContentRebuild()
            return true
        }
        if (StockReviewAction.Type.TOGGLE_BLACK_MARKET == type) {
            if (state.getSourceMode().isRemote()) {
                host.requestContentRebuild()
                return true
            }
            state.toggleBlackMarket()
            resetTradeStateForSourceChange()
            host.rebuildSnapshot()
            host.requestContentRebuild()
            return true
        }
        if (StockReviewAction.Type.SCROLL_LIST == type) {
            state.adjustListScrollOffset(action.getQuantity(), host.currentMaxScrollOffset())
            host.requestContentRebuild()
            return true
        }
        if (StockReviewAction.Type.RESET_ALL_TRADES == type) {
            pendingTrades.clear()
            host.updateTradeWarning(null)
            host.requestContentRebuild()
            return true
        }
        if (StockReviewAction.Type.OPEN_FILTERS == type) {
            modes.enterFilters(state)
            host.requestContentRebuild()
            return true
        }
        if (StockReviewAction.Type.TOGGLE_FILTER_GROUP == type) {
            state.toggle(action.getFilterGroup())
            host.requestContentRebuild()
            return true
        }
        if (StockReviewAction.Type.TOGGLE_FILTER == type) {
            state.toggleFilter(action.getFilter())
            host.requestContentRebuild()
            return true
        }
        if (StockReviewAction.Type.RESET_FILTERS == type) {
            state.clearFilters()
            host.requestContentRebuild()
            return true
        }
        if (StockReviewAction.Type.OPEN_COLOR_DEBUG == type) {
            modes.enterColorDebug(state)
            host.requestContentRebuild()
            return true
        }
        if (handleColorDebugAction(action)) {
            return true
        }
        if (StockReviewAction.Type.REVIEW_PURCHASE == type) {
            if (!pendingTrades.isEmpty()) {
                state.setListScrollOffset(0)
                state.setExpanded(StockReviewTradeGroup.BUYING, true)
                state.setExpanded(StockReviewTradeGroup.SELLING, true)
                host.reopen(true)
            }
            return true
        }
        if (StockReviewAction.Type.GO_BACK == type) {
            handleGoBack()
            return true
        }
        return false
    }

    private fun handleColorDebugAction(action: StockReviewAction): Boolean {
        val type = action.getType()
        if (StockReviewAction.Type.DEBUG_CYCLE_TARGET == type) {
            modes.cycleColorDebugTarget(action.getQuantity())
            host.requestContentRebuild()
            return true
        }
        if (StockReviewAction.Type.DEBUG_TOGGLE_PERSISTENCE == type) {
            modes.toggleColorDebugPersistence()
            host.requestContentRebuild()
            return true
        }
        if (StockReviewAction.Type.DEBUG_ADJUST_RED == type) {
            modes.adjustColorDebugDraft(action.getQuantity(), 0, 0)
            host.requestContentRebuild()
            return true
        }
        if (StockReviewAction.Type.DEBUG_ADJUST_GREEN == type) {
            modes.adjustColorDebugDraft(0, action.getQuantity(), 0)
            host.requestContentRebuild()
            return true
        }
        if (StockReviewAction.Type.DEBUG_ADJUST_BLUE == type) {
            modes.adjustColorDebugDraft(0, 0, action.getQuantity())
            host.requestContentRebuild()
            return true
        }
        if (StockReviewAction.Type.DEBUG_APPLY == type) {
            modes.applyColorDebugDraft()
            host.requestContentRebuild()
            return true
        }
        if (StockReviewAction.Type.DEBUG_CONFIRM == type) {
            modes.applyColorDebugDraft()
            modes.leaveColorDebug(state)
            host.requestContentRebuild()
            return true
        }
        if (StockReviewAction.Type.DEBUG_RESTORE == type) {
            modes.restoreColorDebugDraft()
            host.requestContentRebuild()
            return true
        }
        if (StockReviewAction.Type.DEBUG_NOOP == type) {
            host.requestContentRebuild()
            return true
        }
        return false
    }

    private fun handleGoBack() {
        if (modes.isColorDebugMode()) {
            modes.leaveColorDebug(state)
            host.requestContentRebuild()
            return
        }
        if (modes.isFilterMode()) {
            modes.leaveFilters(state)
            host.requestContentRebuild()
            return
        }
        state.setListScrollOffset(0)
        host.reopen(false)
    }

    private fun resetTradeStateForSourceChange() {
        pendingTrades.clear()
        StockReviewTradeWarnings.clear(state)
        modes.setReviewMode(false)
    }
}