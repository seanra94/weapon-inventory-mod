package weaponsprocurement.ui.stockreview.state

import weaponsprocurement.ui.WimGuiScroll
import weaponsprocurement.ui.WimGuiScrollableListState
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeGroup
import weaponsprocurement.stock.item.StockCategory
import weaponsprocurement.stock.item.StockItemType
import weaponsprocurement.config.StockReviewConfig
import weaponsprocurement.stock.item.StockSortMode
import weaponsprocurement.stock.item.StockSourceMode

class StockReviewState : WimGuiScrollableListState {
    private val expansion: StockReviewExpansionState
    private val filters: StockReviewFilterState
    private val source: StockReviewSourceState
    private var listScrollOffset = 0
    private var tradeWarning = "None"
    private var initialCredits = -1f
    private var initialCargoCapacity = -1f
    private var contentRevision = 0

    constructor(config: StockReviewConfig) {
        expansion = StockReviewExpansionState()
        filters = StockReviewFilterState()
        source = StockReviewSourceState(config)
    }

    constructor(source: StockReviewState) {
        expansion = StockReviewExpansionState(source.expansion)
        filters = StockReviewFilterState(source.filters)
        this.source = StockReviewSourceState(source.source)
        listScrollOffset = source.listScrollOffset
        tradeWarning = source.tradeWarning
        initialCredits = source.initialCredits
        initialCargoCapacity = source.initialCargoCapacity
        contentRevision = source.contentRevision
    }

    fun isExpanded(category: StockCategory?): Boolean = expansion.isExpanded(category)
    fun toggle(category: StockCategory?) {
        markContentChangedIf(expansion.toggle(category))
    }
    fun isExpanded(itemType: StockItemType?, category: StockCategory?): Boolean = expansion.isExpanded(itemType, category)
    fun toggle(itemType: StockItemType?, category: StockCategory?) {
        markContentChangedIf(expansion.toggle(itemType, category))
    }
    fun isExpanded(itemType: StockItemType?): Boolean = expansion.isExpanded(itemType)
    fun toggle(itemType: StockItemType?) {
        markContentChangedIf(expansion.toggle(itemType))
    }
    fun isExpanded(tradeGroup: StockReviewTradeGroup?): Boolean = expansion.isExpanded(tradeGroup)
    fun toggle(tradeGroup: StockReviewTradeGroup?) {
        markContentChangedIf(expansion.toggle(tradeGroup))
    }
    fun setExpanded(tradeGroup: StockReviewTradeGroup?, value: Boolean) {
        markContentChangedIf(expansion.setExpanded(tradeGroup, value))
    }
    fun isItemExpanded(itemKey: String?): Boolean = expansion.isItemExpanded(itemKey)
    fun toggleItem(itemKey: String?) {
        markContentChangedIf(expansion.toggleItem(itemKey))
    }

    fun isFilterActive(filter: StockReviewFilter?): Boolean = filters.isFilterActive(filter)

    fun toggleFilter(filter: StockReviewFilter?) {
        if (filters.toggleFilter(filter)) {
            listScrollOffset = 0
            markContentChanged()
        }
    }

    fun getActiveFilters(): Set<StockReviewFilter> = filters.getActiveFilters()
    fun getActiveFilterCount(): Int = filters.getActiveFilterCount()

    fun clearFilters() {
        if (filters.clearFilters()) {
            listScrollOffset = 0
            markContentChanged()
        }
    }

    fun isExpanded(group: StockReviewFilterGroup?): Boolean = filters.isExpanded(group)
    fun toggle(group: StockReviewFilterGroup?) {
        markContentChangedIf(filters.toggle(group))
    }
    fun getSortMode(): StockSortMode = source.getSortMode()
    fun cycleSortMode() {
        markContentChangedIf(source.cycleSortMode())
    }
    fun isIncludeCurrentMarketStorage(): Boolean = source.isIncludeCurrentMarketStorage()
    fun isIncludeBlackMarket(): Boolean = source.isIncludeBlackMarket()
    fun toggleBlackMarket() {
        markContentChangedIf(source.toggleBlackMarket())
    }
    fun getSourceMode(): StockSourceMode = source.getSourceMode()
    fun cycleSourceMode() {
        markContentChangedIf(source.cycleSourceMode())
    }

    fun normalizeSourceMode() {
        markContentChangedIf(source.normalizeSourceMode())
    }

    fun getContentRevision(): Int = contentRevision

    override fun getListScrollOffset(): Int = listScrollOffset

    override fun setListScrollOffset(listScrollOffset: Int) {
        this.listScrollOffset = Math.max(0, listScrollOffset)
    }

    fun adjustListScrollOffset(delta: Int, maxOffset: Int) {
        listScrollOffset = WimGuiScroll.usefulOffsetByDelta(listScrollOffset, delta, Math.max(0, maxOffset))
    }

    fun getTradeWarning(): String = tradeWarning

    fun setTradeWarning(tradeWarning: String?) {
        this.tradeWarning = if (tradeWarning.isNullOrEmpty()) "None" else tradeWarning
    }

    fun getInitialCredits(): Float = initialCredits

    fun setInitialCreditsIfUnset(initialCredits: Float) {
        if (this.initialCredits < 0f) {
            this.initialCredits = Math.max(0f, initialCredits)
        }
    }

    fun getInitialCargoCapacity(): Float = initialCargoCapacity

    fun setInitialCargoCapacityIfUnset(initialCargoCapacity: Float) {
        if (this.initialCargoCapacity < 0f) {
            this.initialCargoCapacity = Math.max(0f, initialCargoCapacity)
        }
    }

    private fun markContentChanged() {
        contentRevision++
    }

    private fun markContentChangedIf(changed: Boolean) {
        if (changed) markContentChanged()
    }
}
