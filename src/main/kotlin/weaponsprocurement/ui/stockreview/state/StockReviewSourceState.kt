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

    fun cycleSortMode(): Boolean {
        val previous = sortMode
        sortMode = sortMode.next()
        return previous != sortMode
    }

    fun isIncludeCurrentMarketStorage(): Boolean = includeCurrentMarketStorage

    fun isIncludeBlackMarket(): Boolean = !getSourceMode().isRemote() && includeBlackMarket

    fun toggleBlackMarket(): Boolean {
        val previous = includeBlackMarket
        if (getSourceMode().isRemote()) {
            includeBlackMarket = false
            return previous != includeBlackMarket
        }
        includeBlackMarket = !includeBlackMarket
        return previous != includeBlackMarket
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

    fun cycleSourceMode(): Boolean {
        val previousSource = getSourceMode()
        val previousBlackMarket = includeBlackMarket
        val next = getSourceMode().next()
        sourceMode = next
        if (next.isRemote()) {
            includeBlackMarket = false
        }
        return previousSource != next || previousBlackMarket != includeBlackMarket
    }

    fun normalizeSourceMode(): Boolean {
        val previousSource = sourceMode
        val previousBlackMarket = includeBlackMarket
        getSourceMode()
        return previousSource != sourceMode || previousBlackMarket != includeBlackMarket
    }
}
