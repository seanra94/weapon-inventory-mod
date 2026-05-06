package weaponinventorymod.gui;

import weaponinventorymod.core.StockCategory;

final class StockReviewAction {
    enum Type {
        TOGGLE_CATEGORY,
        TOGGLE_WEAPON,
        TOGGLE_WEAPON_SECTION,
        BUY_BEST,
        BUY_FROM_SUBMARKET,
        CYCLE_DISPLAY_MODE,
        CYCLE_SORT_MODE,
        TOGGLE_CURRENT_MARKET_STORAGE,
        TOGGLE_BLACK_MARKET,
        SCROLL_LIST,
        REVIEW_PURCHASE,
        CONFIRM_PURCHASE,
        GO_BACK,
        REFRESH,
        CLOSE
    }

    private final Type type;
    private final StockCategory category;
    private final StockReviewSection section;
    private final String weaponId;
    private final String submarketId;
    private final int quantity;

    private StockReviewAction(Type type,
                              StockCategory category,
                              StockReviewSection section,
                              String weaponId,
                              String submarketId,
                              int quantity) {
        this.type = type;
        this.category = category;
        this.section = section;
        this.weaponId = weaponId;
        this.submarketId = submarketId;
        this.quantity = quantity;
    }

    static StockReviewAction toggle(StockCategory category) {
        return new StockReviewAction(Type.TOGGLE_CATEGORY, category, null, null, null, 0);
    }

    static StockReviewAction toggleWeapon(String weaponId) {
        return new StockReviewAction(Type.TOGGLE_WEAPON, null, null, weaponId, null, 0);
    }

    static StockReviewAction toggleWeaponSection(String weaponId, StockReviewSection section) {
        return new StockReviewAction(Type.TOGGLE_WEAPON_SECTION, null, section, weaponId, null, 0);
    }

    static StockReviewAction buyBest(String weaponId, int quantity) {
        return new StockReviewAction(Type.BUY_BEST, null, null, weaponId, null, quantity);
    }

    static StockReviewAction buyFromSubmarket(String weaponId, String submarketId, int quantity) {
        return new StockReviewAction(Type.BUY_FROM_SUBMARKET, null, null, weaponId, submarketId, quantity);
    }

    static StockReviewAction refresh() {
        return new StockReviewAction(Type.REFRESH, null, null, null, null, 0);
    }

    static StockReviewAction cycleDisplayMode() {
        return new StockReviewAction(Type.CYCLE_DISPLAY_MODE, null, null, null, null, 0);
    }

    static StockReviewAction cycleSortMode() {
        return new StockReviewAction(Type.CYCLE_SORT_MODE, null, null, null, null, 0);
    }

    static StockReviewAction toggleCurrentMarketStorage() {
        return new StockReviewAction(Type.TOGGLE_CURRENT_MARKET_STORAGE, null, null, null, null, 0);
    }

    static StockReviewAction toggleBlackMarket() {
        return new StockReviewAction(Type.TOGGLE_BLACK_MARKET, null, null, null, null, 0);
    }

    static StockReviewAction scrollList(int delta) {
        return new StockReviewAction(Type.SCROLL_LIST, null, null, null, null, delta);
    }

    static StockReviewAction reviewPurchase() {
        return new StockReviewAction(Type.REVIEW_PURCHASE, null, null, null, null, 0);
    }

    static StockReviewAction confirmPurchase() {
        return new StockReviewAction(Type.CONFIRM_PURCHASE, null, null, null, null, 0);
    }

    static StockReviewAction goBack() {
        return new StockReviewAction(Type.GO_BACK, null, null, null, null, 0);
    }

    static StockReviewAction close() {
        return new StockReviewAction(Type.CLOSE, null, null, null, null, 0);
    }

    Type getType() {
        return type;
    }

    StockCategory getCategory() {
        return category;
    }

    StockReviewSection getSection() {
        return section;
    }

    String getWeaponId() {
        return weaponId;
    }

    String getSubmarketId() {
        return submarketId;
    }

    int getQuantity() {
        return quantity;
    }
}
