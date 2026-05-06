package weaponinventorymod.gui;

import weaponinventorymod.core.StockCategory;

final class StockReviewAction {
    enum Type {
        TOGGLE_CATEGORY,
        CYCLE_DISPLAY_MODE,
        CYCLE_SORT_MODE,
        TOGGLE_CURRENT_MARKET_STORAGE,
        TOGGLE_BLACK_MARKET,
        REFRESH,
        CLOSE
    }

    private final Type type;
    private final StockCategory category;

    private StockReviewAction(Type type, StockCategory category) {
        this.type = type;
        this.category = category;
    }

    static StockReviewAction toggle(StockCategory category) {
        return new StockReviewAction(Type.TOGGLE_CATEGORY, category);
    }

    static StockReviewAction refresh() {
        return new StockReviewAction(Type.REFRESH, null);
    }

    static StockReviewAction cycleDisplayMode() {
        return new StockReviewAction(Type.CYCLE_DISPLAY_MODE, null);
    }

    static StockReviewAction cycleSortMode() {
        return new StockReviewAction(Type.CYCLE_SORT_MODE, null);
    }

    static StockReviewAction toggleCurrentMarketStorage() {
        return new StockReviewAction(Type.TOGGLE_CURRENT_MARKET_STORAGE, null);
    }

    static StockReviewAction toggleBlackMarket() {
        return new StockReviewAction(Type.TOGGLE_BLACK_MARKET, null);
    }

    static StockReviewAction close() {
        return new StockReviewAction(Type.CLOSE, null);
    }

    Type getType() {
        return type;
    }

    StockCategory getCategory() {
        return category;
    }
}
