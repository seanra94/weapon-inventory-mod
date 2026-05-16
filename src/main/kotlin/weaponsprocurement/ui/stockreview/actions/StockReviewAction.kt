package weaponsprocurement.ui.stockreview.actions

import weaponsprocurement.ui.stockreview.state.StockReviewFilter
import weaponsprocurement.ui.stockreview.state.StockReviewFilterGroup
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeGroup
import weaponsprocurement.stock.item.StockCategory
import weaponsprocurement.stock.item.StockItemType

class StockReviewAction private constructor(
    private val type: Type,
    private val category: StockCategory?,
    private val itemType: StockItemType?,
    private val tradeGroup: StockReviewTradeGroup?,
    private val filterGroup: StockReviewFilterGroup?,
    private val filter: StockReviewFilter?,
    private val itemKey: String?,
    private val submarketId: String?,
    private val quantity: Int,
) {
    enum class Type {
        TOGGLE_CATEGORY,
        TOGGLE_ITEM_TYPE,
        TOGGLE_TRADE_GROUP,
        TOGGLE_ITEM,
        ADJUST_PLAN,
        ADJUST_TO_SUFFICIENT,
        RESET_PLAN,
        CYCLE_SORT_MODE,
        CYCLE_SOURCE_MODE,
        TOGGLE_BLACK_MARKET,
        SCROLL_LIST,
        PURCHASE_ALL_UNTIL_SUFFICIENT,
        SELL_ALL_UNTIL_SUFFICIENT,
        RESET_ALL_TRADES,
        OPEN_FILTERS,
        TOGGLE_FILTER_GROUP,
        TOGGLE_FILTER,
        RESET_FILTERS,
        OPEN_COLOR_DEBUG,
        DEBUG_CYCLE_TARGET,
        DEBUG_TOGGLE_PERSISTENCE,
        DEBUG_ADJUST_RED,
        DEBUG_ADJUST_GREEN,
        DEBUG_ADJUST_BLUE,
        DEBUG_APPLY,
        DEBUG_CONFIRM,
        DEBUG_RESTORE,
        DEBUG_NOOP,
        REVIEW_PURCHASE,
        CONFIRM_PURCHASE,
        GO_BACK,
    }

    fun getType(): Type = type
    fun getCategory(): StockCategory? = category
    fun getItemType(): StockItemType? = itemType
    fun getTradeGroup(): StockReviewTradeGroup? = tradeGroup
    fun getFilterGroup(): StockReviewFilterGroup? = filterGroup
    fun getFilter(): StockReviewFilter? = filter
    fun getItemKey(): String? = itemKey
    fun getSubmarketId(): String? = submarketId
    fun getQuantity(): Int = quantity

    companion object {
        @JvmStatic
        fun toggle(category: StockCategory?): StockReviewAction =
            StockReviewAction(Type.TOGGLE_CATEGORY, category, null, null, null, null, null, null, 0)

        @JvmStatic
        fun toggle(itemType: StockItemType?, category: StockCategory?): StockReviewAction =
            StockReviewAction(Type.TOGGLE_CATEGORY, category, itemType, null, null, null, null, null, 0)

        @JvmStatic
        fun toggle(itemType: StockItemType?): StockReviewAction =
            StockReviewAction(Type.TOGGLE_ITEM_TYPE, null, itemType, null, null, null, null, null, 0)

        @JvmStatic
        fun toggle(tradeGroup: StockReviewTradeGroup?): StockReviewAction =
            StockReviewAction(Type.TOGGLE_TRADE_GROUP, null, null, tradeGroup, null, null, null, null, 0)

        @JvmStatic
        fun toggle(filterGroup: StockReviewFilterGroup?): StockReviewAction =
            StockReviewAction(Type.TOGGLE_FILTER_GROUP, null, null, null, filterGroup, null, null, null, 0)

        @JvmStatic
        fun toggleItem(itemKey: String?): StockReviewAction =
            StockReviewAction(Type.TOGGLE_ITEM, null, null, null, null, null, itemKey, null, 0)

        @JvmStatic
        fun buyBest(itemKey: String?, quantity: Int): StockReviewAction = adjustPlan(itemKey, quantity)

        @JvmStatic
        fun adjustPlan(itemKey: String?, delta: Int): StockReviewAction =
            StockReviewAction(Type.ADJUST_PLAN, null, null, null, null, null, itemKey, null, delta)

        @JvmStatic
        fun resetPlan(itemKey: String?): StockReviewAction =
            StockReviewAction(Type.RESET_PLAN, null, null, null, null, null, itemKey, null, 0)

        @JvmStatic
        fun adjustToSufficient(itemKey: String?, delta: Int): StockReviewAction =
            StockReviewAction(Type.ADJUST_TO_SUFFICIENT, null, null, null, null, null, itemKey, null, delta)

        @JvmStatic
        fun cycleSortMode(): StockReviewAction =
            StockReviewAction(Type.CYCLE_SORT_MODE, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun toggleBlackMarket(): StockReviewAction =
            StockReviewAction(Type.TOGGLE_BLACK_MARKET, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun cycleSourceMode(): StockReviewAction =
            StockReviewAction(Type.CYCLE_SOURCE_MODE, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun scrollList(delta: Int): StockReviewAction =
            StockReviewAction(Type.SCROLL_LIST, null, null, null, null, null, null, null, delta)

        @JvmStatic
        fun reviewPurchase(): StockReviewAction =
            StockReviewAction(Type.REVIEW_PURCHASE, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun purchaseAllUntilSufficient(): StockReviewAction =
            StockReviewAction(Type.PURCHASE_ALL_UNTIL_SUFFICIENT, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun sellAllUntilSufficient(): StockReviewAction =
            StockReviewAction(Type.SELL_ALL_UNTIL_SUFFICIENT, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun resetAllTrades(): StockReviewAction =
            StockReviewAction(Type.RESET_ALL_TRADES, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun openFilters(): StockReviewAction =
            StockReviewAction(Type.OPEN_FILTERS, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun toggleFilter(filter: StockReviewFilter?): StockReviewAction =
            StockReviewAction(Type.TOGGLE_FILTER, null, null, null, null, filter, null, null, 0)

        @JvmStatic
        fun resetFilters(): StockReviewAction =
            StockReviewAction(Type.RESET_FILTERS, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun openColorDebug(): StockReviewAction =
            StockReviewAction(Type.OPEN_COLOR_DEBUG, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun debugCycleTarget(delta: Int): StockReviewAction =
            StockReviewAction(Type.DEBUG_CYCLE_TARGET, null, null, null, null, null, null, null, delta)

        @JvmStatic
        fun debugTogglePersistence(): StockReviewAction =
            StockReviewAction(Type.DEBUG_TOGGLE_PERSISTENCE, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun debugAdjustRed(delta: Int): StockReviewAction =
            StockReviewAction(Type.DEBUG_ADJUST_RED, null, null, null, null, null, null, null, delta)

        @JvmStatic
        fun debugAdjustGreen(delta: Int): StockReviewAction =
            StockReviewAction(Type.DEBUG_ADJUST_GREEN, null, null, null, null, null, null, null, delta)

        @JvmStatic
        fun debugAdjustBlue(delta: Int): StockReviewAction =
            StockReviewAction(Type.DEBUG_ADJUST_BLUE, null, null, null, null, null, null, null, delta)

        @JvmStatic
        fun debugApply(): StockReviewAction =
            StockReviewAction(Type.DEBUG_APPLY, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun debugConfirm(): StockReviewAction =
            StockReviewAction(Type.DEBUG_CONFIRM, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun debugRestore(): StockReviewAction =
            StockReviewAction(Type.DEBUG_RESTORE, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun debugNoop(): StockReviewAction =
            StockReviewAction(Type.DEBUG_NOOP, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun confirmPurchase(): StockReviewAction =
            StockReviewAction(Type.CONFIRM_PURCHASE, null, null, null, null, null, null, null, 0)

        @JvmStatic
        fun goBack(): StockReviewAction =
            StockReviewAction(Type.GO_BACK, null, null, null, null, null, null, null, 0)
    }
}