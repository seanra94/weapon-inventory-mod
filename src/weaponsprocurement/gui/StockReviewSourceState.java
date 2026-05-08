package weaponsprocurement.gui;

import weaponsprocurement.core.StockReviewConfig;
import weaponsprocurement.core.StockSortMode;
import weaponsprocurement.core.StockSourceMode;

final class StockReviewSourceState {
    private StockSortMode sortMode;
    private boolean includeCurrentMarketStorage;
    private boolean includeBlackMarket;
    private StockSourceMode sourceMode;

    StockReviewSourceState(StockReviewConfig config) {
        this.sortMode = config.getSortMode();
        this.includeCurrentMarketStorage = config.isIncludeCurrentMarketStorage();
        this.includeBlackMarket = config.isIncludeBlackMarket();
        this.sourceMode = StockSourceMode.LOCAL;
    }

    StockReviewSourceState(StockReviewSourceState source) {
        this.sortMode = source.sortMode;
        this.includeCurrentMarketStorage = source.includeCurrentMarketStorage;
        this.includeBlackMarket = source.includeBlackMarket;
        this.sourceMode = source.sourceMode;
    }

    StockSortMode getSortMode() {
        return sortMode;
    }

    void cycleSortMode() {
        sortMode = sortMode.next();
    }

    boolean isIncludeCurrentMarketStorage() {
        return includeCurrentMarketStorage;
    }

    boolean isIncludeBlackMarket() {
        return !getSourceMode().isRemote() && includeBlackMarket;
    }

    void toggleBlackMarket() {
        if (getSourceMode().isRemote()) {
            includeBlackMarket = false;
            return;
        }
        includeBlackMarket = !includeBlackMarket;
    }

    StockSourceMode getSourceMode() {
        StockSourceMode resolved = sourceMode == null ? StockSourceMode.LOCAL : sourceMode;
        if (!resolved.isEnabled()) {
            sourceMode = StockSourceMode.LOCAL;
            includeBlackMarket = false;
            return sourceMode;
        }
        return resolved;
    }

    void cycleSourceMode() {
        sourceMode = getSourceMode().next();
        if (sourceMode.isRemote()) {
            includeBlackMarket = false;
        }
    }
}
