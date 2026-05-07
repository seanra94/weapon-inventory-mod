package weaponinventorymod.gui;

import weaponinventorymod.core.StockCategory;

final class StockReviewAction {
    enum Type {
        TOGGLE_CATEGORY,
        TOGGLE_TRADE_GROUP,
        TOGGLE_WEAPON,
        TOGGLE_WEAPON_SECTION,
        ADJUST_PLAN,
        BUY_FROM_SUBMARKET,
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
        CLOSE
    }

    private final Type type;
    private final StockCategory category;
    private final StockReviewTradeGroup tradeGroup;
    private final StockReviewSection section;
    private final StockReviewFilterGroup filterGroup;
    private final StockReviewFilter filter;
    private final String weaponId;
    private final String submarketId;
    private final int quantity;

    private StockReviewAction(Type type,
                              StockCategory category,
                              StockReviewTradeGroup tradeGroup,
                              StockReviewSection section,
                              StockReviewFilterGroup filterGroup,
                              StockReviewFilter filter,
                              String weaponId,
                              String submarketId,
                              int quantity) {
        this.type = type;
        this.category = category;
        this.tradeGroup = tradeGroup;
        this.section = section;
        this.filterGroup = filterGroup;
        this.filter = filter;
        this.weaponId = weaponId;
        this.submarketId = submarketId;
        this.quantity = quantity;
    }

    static StockReviewAction toggle(StockCategory category) {
        return new StockReviewAction(Type.TOGGLE_CATEGORY, category, null, null, null, null, null, null, 0);
    }

    static StockReviewAction toggle(StockReviewTradeGroup tradeGroup) {
        return new StockReviewAction(Type.TOGGLE_TRADE_GROUP, null, tradeGroup, null, null, null, null, null, 0);
    }

    static StockReviewAction toggle(StockReviewFilterGroup filterGroup) {
        return new StockReviewAction(Type.TOGGLE_FILTER_GROUP, null, null, null, filterGroup, null, null, null, 0);
    }

    static StockReviewAction toggleWeapon(String weaponId) {
        return new StockReviewAction(Type.TOGGLE_WEAPON, null, null, null, null, null, weaponId, null, 0);
    }

    static StockReviewAction toggleWeaponSection(String weaponId, StockReviewSection section) {
        return new StockReviewAction(Type.TOGGLE_WEAPON_SECTION, null, null, section, null, null, weaponId, null, 0);
    }

    static StockReviewAction buyBest(String weaponId, int quantity) {
        return adjustPlan(weaponId, quantity);
    }

    static StockReviewAction adjustPlan(String weaponId, int delta) {
        return new StockReviewAction(Type.ADJUST_PLAN, null, null, null, null, null, weaponId, null, delta);
    }

    static StockReviewAction resetPlan(String weaponId) {
        return new StockReviewAction(Type.RESET_PLAN, null, null, null, null, null, weaponId, null, 0);
    }

    static StockReviewAction buyFromSubmarket(String weaponId, String submarketId, int quantity) {
        return new StockReviewAction(Type.BUY_FROM_SUBMARKET, null, null, null, null, null, weaponId, submarketId, quantity);
    }

    static StockReviewAction adjustToSufficient(String weaponId, int delta) {
        return new StockReviewAction(Type.ADJUST_TO_SUFFICIENT, null, null, null, null, null, weaponId, null, delta);
    }

    static StockReviewAction cycleSortMode() {
        return new StockReviewAction(Type.CYCLE_SORT_MODE, null, null, null, null, null, null, null, 0);
    }

    static StockReviewAction toggleBlackMarket() {
        return new StockReviewAction(Type.TOGGLE_BLACK_MARKET, null, null, null, null, null, null, null, 0);
    }

    static StockReviewAction cycleSourceMode() {
        return new StockReviewAction(Type.CYCLE_SOURCE_MODE, null, null, null, null, null, null, null, 0);
    }

    static StockReviewAction scrollList(int delta) {
        return new StockReviewAction(Type.SCROLL_LIST, null, null, null, null, null, null, null, delta);
    }

    static StockReviewAction reviewPurchase() {
        return new StockReviewAction(Type.REVIEW_PURCHASE, null, null, null, null, null, null, null, 0);
    }

    static StockReviewAction purchaseAllUntilSufficient() {
        return new StockReviewAction(Type.PURCHASE_ALL_UNTIL_SUFFICIENT, null, null, null, null, null, null, null, 0);
    }

    static StockReviewAction sellAllUntilSufficient() {
        return new StockReviewAction(Type.SELL_ALL_UNTIL_SUFFICIENT, null, null, null, null, null, null, null, 0);
    }

    static StockReviewAction resetAllTrades() {
        return new StockReviewAction(Type.RESET_ALL_TRADES, null, null, null, null, null, null, null, 0);
    }

    static StockReviewAction openFilters() {
        return new StockReviewAction(Type.OPEN_FILTERS, null, null, null, null, null, null, null, 0);
    }

    static StockReviewAction toggleFilter(StockReviewFilter filter) {
        return new StockReviewAction(Type.TOGGLE_FILTER, null, null, null, null, filter, null, null, 0);
    }

    static StockReviewAction resetFilters() {
        return new StockReviewAction(Type.RESET_FILTERS, null, null, null, null, null, null, null, 0);
    }

    static StockReviewAction openColorDebug() {
        return new StockReviewAction(Type.OPEN_COLOR_DEBUG, null, null, null, null, null, null, null, 0);
    }

    static StockReviewAction debugCycleTarget(int delta) {
        return new StockReviewAction(Type.DEBUG_CYCLE_TARGET, null, null, null, null, null, null, null, delta);
    }

    static StockReviewAction debugTogglePersistence() {
        return new StockReviewAction(Type.DEBUG_TOGGLE_PERSISTENCE, null, null, null, null, null, null, null, 0);
    }

    static StockReviewAction debugAdjustRed(int delta) {
        return new StockReviewAction(Type.DEBUG_ADJUST_RED, null, null, null, null, null, null, null, delta);
    }

    static StockReviewAction debugAdjustGreen(int delta) {
        return new StockReviewAction(Type.DEBUG_ADJUST_GREEN, null, null, null, null, null, null, null, delta);
    }

    static StockReviewAction debugAdjustBlue(int delta) {
        return new StockReviewAction(Type.DEBUG_ADJUST_BLUE, null, null, null, null, null, null, null, delta);
    }

    static StockReviewAction debugApply() {
        return new StockReviewAction(Type.DEBUG_APPLY, null, null, null, null, null, null, null, 0);
    }

    static StockReviewAction debugConfirm() {
        return new StockReviewAction(Type.DEBUG_CONFIRM, null, null, null, null, null, null, null, 0);
    }

    static StockReviewAction debugRestore() {
        return new StockReviewAction(Type.DEBUG_RESTORE, null, null, null, null, null, null, null, 0);
    }

    static StockReviewAction debugNoop() {
        return new StockReviewAction(Type.DEBUG_NOOP, null, null, null, null, null, null, null, 0);
    }

    static StockReviewAction confirmPurchase() {
        return new StockReviewAction(Type.CONFIRM_PURCHASE, null, null, null, null, null, null, null, 0);
    }

    static StockReviewAction goBack() {
        return new StockReviewAction(Type.GO_BACK, null, null, null, null, null, null, null, 0);
    }

    static StockReviewAction close() {
        return new StockReviewAction(Type.CLOSE, null, null, null, null, null, null, null, 0);
    }

    Type getType() {
        return type;
    }

    StockCategory getCategory() {
        return category;
    }

    StockReviewTradeGroup getTradeGroup() {
        return tradeGroup;
    }

    StockReviewSection getSection() {
        return section;
    }

    StockReviewFilterGroup getFilterGroup() {
        return filterGroup;
    }

    StockReviewFilter getFilter() {
        return filter;
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
