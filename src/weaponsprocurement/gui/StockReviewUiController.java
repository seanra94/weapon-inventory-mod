package weaponsprocurement.gui;

final class StockReviewUiController {
    interface Host {
        int currentMaxScrollOffset();

        void updateTradeWarning(String explicitWarning);

        void rebuildSnapshot();

        void requestContentRebuild();

        void reopen(boolean review);

        void requestClose();
    }

    private final StockReviewState state;
    private final StockReviewModeController modes;
    private final StockReviewPendingTrades pendingTrades;
    private final Host host;

    StockReviewUiController(StockReviewState state,
                            StockReviewModeController modes,
                            StockReviewPendingTrades pendingTrades,
                            Host host) {
        this.state = state;
        this.modes = modes;
        this.pendingTrades = pendingTrades;
        this.host = host;
    }

    void handleCloseRequested() {
        if (modes.isColorDebugMode()) {
            modes.leaveColorDebug(state);
            host.requestContentRebuild();
            return;
        }
        if (modes.isFilterMode()) {
            modes.leaveFilters(state);
            host.requestContentRebuild();
            return;
        }
        if (modes.isReviewMode()) {
            state.setListScrollOffset(0);
            host.reopen(false);
            return;
        }
        host.requestClose();
    }

    boolean handle(StockReviewAction action) {
        if (action == null) {
            return true;
        }
        StockReviewAction.Type type = action.getType();
        if (StockReviewAction.Type.TOGGLE_CATEGORY.equals(type)) {
            state.toggle(action.getItemType(), action.getCategory());
            host.requestContentRebuild();
            return true;
        }
        if (StockReviewAction.Type.TOGGLE_ITEM_TYPE.equals(type)) {
            state.toggle(action.getItemType());
            host.requestContentRebuild();
            return true;
        }
        if (StockReviewAction.Type.TOGGLE_TRADE_GROUP.equals(type)) {
            state.toggle(action.getTradeGroup());
            host.requestContentRebuild();
            return true;
        }
        if (StockReviewAction.Type.TOGGLE_ITEM.equals(type)) {
            state.toggleItem(action.getItemKey());
            host.requestContentRebuild();
            return true;
        }
        if (StockReviewAction.Type.CYCLE_SORT_MODE.equals(type)) {
            state.cycleSortMode();
            host.rebuildSnapshot();
            host.requestContentRebuild();
            return true;
        }
        if (StockReviewAction.Type.CYCLE_SOURCE_MODE.equals(type)) {
            state.cycleSourceMode();
            resetTradeStateForSourceChange();
            state.setListScrollOffset(0);
            host.rebuildSnapshot();
            host.requestContentRebuild();
            return true;
        }
        if (StockReviewAction.Type.TOGGLE_BLACK_MARKET.equals(type)) {
            if (state.getSourceMode().isRemote()) {
                host.requestContentRebuild();
                return true;
            }
            state.toggleBlackMarket();
            resetTradeStateForSourceChange();
            host.rebuildSnapshot();
            host.requestContentRebuild();
            return true;
        }
        if (StockReviewAction.Type.SCROLL_LIST.equals(type)) {
            state.adjustListScrollOffset(action.getQuantity(), host.currentMaxScrollOffset());
            host.requestContentRebuild();
            return true;
        }
        if (StockReviewAction.Type.RESET_ALL_TRADES.equals(type)) {
            pendingTrades.clear();
            host.updateTradeWarning(null);
            host.requestContentRebuild();
            return true;
        }
        if (StockReviewAction.Type.OPEN_FILTERS.equals(type)) {
            modes.enterFilters(state);
            host.requestContentRebuild();
            return true;
        }
        if (StockReviewAction.Type.TOGGLE_FILTER_GROUP.equals(type)) {
            state.toggle(action.getFilterGroup());
            host.requestContentRebuild();
            return true;
        }
        if (StockReviewAction.Type.TOGGLE_FILTER.equals(type)) {
            state.toggleFilter(action.getFilter());
            host.requestContentRebuild();
            return true;
        }
        if (StockReviewAction.Type.RESET_FILTERS.equals(type)) {
            state.clearFilters();
            host.requestContentRebuild();
            return true;
        }
        if (StockReviewAction.Type.OPEN_COLOR_DEBUG.equals(type)) {
            modes.enterColorDebug(state);
            host.requestContentRebuild();
            return true;
        }
        if (handleColorDebugAction(action)) {
            return true;
        }
        if (StockReviewAction.Type.REVIEW_PURCHASE.equals(type)) {
            if (!pendingTrades.isEmpty()) {
                state.setListScrollOffset(0);
                state.setExpanded(StockReviewTradeGroup.BUYING, true);
                state.setExpanded(StockReviewTradeGroup.SELLING, true);
                host.reopen(true);
            }
            return true;
        }
        if (StockReviewAction.Type.GO_BACK.equals(type)) {
            handleGoBack();
            return true;
        }
        return false;
    }

    private boolean handleColorDebugAction(StockReviewAction action) {
        StockReviewAction.Type type = action.getType();
        if (StockReviewAction.Type.DEBUG_CYCLE_TARGET.equals(type)) {
            modes.cycleColorDebugTarget(action.getQuantity());
            host.requestContentRebuild();
            return true;
        }
        if (StockReviewAction.Type.DEBUG_TOGGLE_PERSISTENCE.equals(type)) {
            modes.toggleColorDebugPersistence();
            host.requestContentRebuild();
            return true;
        }
        if (StockReviewAction.Type.DEBUG_ADJUST_RED.equals(type)) {
            modes.adjustColorDebugDraft(action.getQuantity(), 0, 0);
            host.requestContentRebuild();
            return true;
        }
        if (StockReviewAction.Type.DEBUG_ADJUST_GREEN.equals(type)) {
            modes.adjustColorDebugDraft(0, action.getQuantity(), 0);
            host.requestContentRebuild();
            return true;
        }
        if (StockReviewAction.Type.DEBUG_ADJUST_BLUE.equals(type)) {
            modes.adjustColorDebugDraft(0, 0, action.getQuantity());
            host.requestContentRebuild();
            return true;
        }
        if (StockReviewAction.Type.DEBUG_APPLY.equals(type)) {
            modes.applyColorDebugDraft();
            host.requestContentRebuild();
            return true;
        }
        if (StockReviewAction.Type.DEBUG_CONFIRM.equals(type)) {
            modes.applyColorDebugDraft();
            modes.leaveColorDebug(state);
            host.requestContentRebuild();
            return true;
        }
        if (StockReviewAction.Type.DEBUG_RESTORE.equals(type)) {
            modes.restoreColorDebugDraft();
            host.requestContentRebuild();
            return true;
        }
        if (StockReviewAction.Type.DEBUG_NOOP.equals(type)) {
            host.requestContentRebuild();
            return true;
        }
        return false;
    }

    private void handleGoBack() {
        if (modes.isColorDebugMode()) {
            modes.leaveColorDebug(state);
            host.requestContentRebuild();
            return;
        }
        if (modes.isFilterMode()) {
            modes.leaveFilters(state);
            host.requestContentRebuild();
            return;
        }
        state.setListScrollOffset(0);
        host.reopen(false);
    }

    private void resetTradeStateForSourceChange() {
        pendingTrades.clear();
        StockReviewTradeWarnings.clear(state);
        modes.setReviewMode(false);
    }
}
