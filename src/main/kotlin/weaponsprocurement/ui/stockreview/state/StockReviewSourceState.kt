package weaponsprocurement.ui.stockreview.state



import weaponsprocurement.config.StockReviewConfig
import weaponsprocurement.stock.item.StockSortMode
import weaponsprocurement.stock.item.StockSourceMode

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
            return StockSourceMode.LOCAL
        }
        return resolved
    }

    fun cycleSourceMode() {
        val next = getSourceMode().next()
        sourceMode = next
        if (next.isRemote()) {
            includeBlackMarket = false
        }
    }
}
