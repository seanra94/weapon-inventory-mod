package weaponsprocurement.gui

import weaponsprocurement.config.StockReviewConfig
import weaponsprocurement.stock.StockSortMode
import weaponsprocurement.stock.StockSourceMode

class StockReviewSourceState {
    private var sortMode: StockSortMode
    private var includeCurrentMarketStorage: Boolean
    private var includeBlackMarket: Boolean
    private var sourceMode: StockSourceMode?

    constructor(config: StockReviewConfig) {
        sortMode = config.getSortMode()
        includeCurrentMarketStorage = config.isIncludeCurrentMarketStorage()
        includeBlackMarket = config.isIncludeBlackMarket()
        sourceMode = StockSourceMode.LOCAL
    }

    constructor(source: StockReviewSourceState) {
        sortMode = source.sortMode
        includeCurrentMarketStorage = source.includeCurrentMarketStorage
        includeBlackMarket = source.includeBlackMarket
        sourceMode = source.sourceMode
    }

    fun getSortMode(): StockSortMode = sortMode

    fun cycleSortMode() {
        sortMode = sortMode.next()
    }

    fun isIncludeCurrentMarketStorage(): Boolean = includeCurrentMarketStorage

    fun isIncludeBlackMarket(): Boolean = !getSourceMode().isRemote() && includeBlackMarket

    fun toggleBlackMarket() {
        if (getSourceMode().isRemote()) {
            includeBlackMarket = false
            return
        }
        includeBlackMarket = !includeBlackMarket
    }

    fun getSourceMode(): StockSourceMode {
        val resolved = sourceMode ?: StockSourceMode.LOCAL
        if (!resolved.isEnabled) {
            sourceMode = StockSourceMode.LOCAL
            includeBlackMarket = false
            return sourceMode as StockSourceMode
        }
        return resolved
    }

    fun cycleSourceMode() {
        sourceMode = getSourceMode().next()
        if (sourceMode!!.isRemote()) {
            includeBlackMarket = false
        }
    }
}
