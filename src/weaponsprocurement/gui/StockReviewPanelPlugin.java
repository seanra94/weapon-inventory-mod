package weaponsprocurement.gui;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import org.apache.log4j.Logger;
import weaponsprocurement.core.StockReviewConfig;
import weaponsprocurement.core.StockPurchaseService;
import weaponsprocurement.core.WeaponStockSnapshot;
import weaponsprocurement.core.WeaponStockSnapshotBuilder;

import java.util.List;

public final class StockReviewPanelPlugin extends WimGuiModalPanelPlugin<StockReviewAction>
        implements StockReviewTradeController.Host, StockReviewExecutionController.Host {
    private static final Logger LOG = Logger.getLogger(StockReviewPanelPlugin.class);

    private static boolean reviewMode(StockReviewLaunchState launchState) {
        return launchState != null && launchState.isReviewMode();
    }

    private final StockReviewConfig config = StockReviewConfig.load();
    private final StockReviewState state;
    private final StockReviewRenderer renderer = new StockReviewRenderer();
    private final WeaponStockSnapshotBuilder snapshotBuilder = new WeaponStockSnapshotBuilder();
    private final StockPurchaseService purchaseService = new StockPurchaseService();
    private final StockReviewPendingTrades pendingTrades = new StockReviewPendingTrades();
    private final MarketAPI initialMarket;
    private final StockReviewModeController modes;
    private final StockReviewTradeController trades;
    private final StockReviewExecutionController execution;

    private WeaponStockSnapshot snapshot;

    public StockReviewPanelPlugin(MarketAPI initialMarket, StockReviewLaunchState launchState) {
        super(
                StockReviewAction.class,
                StockReviewStyle.widthFor(reviewMode(launchState)),
                StockReviewStyle.HEIGHT,
                StockReviewStyle.BUTTON_POLL_FRAMES_AFTER_MOUSE_EVENT,
                StockReviewStyle.initialListBounds(reviewMode(launchState)));
        this.initialMarket = initialMarket;
        this.state = launchState == null || launchState.getState() == null
                ? new StockReviewState(config)
                : new StockReviewState(launchState.getState());
        if (launchState != null) {
            this.pendingTrades.replaceWith(launchState.getPendingTrades());
        }
        this.modes = new StockReviewModeController(reviewMode(launchState));
        this.trades = new StockReviewTradeController(state, pendingTrades, this);
        this.execution = new StockReviewExecutionController(state, pendingTrades, purchaseService, this);
    }

    boolean isReviewMode() {
        return modes.isReviewMode();
    }

    @Override
    protected void onInit() {
        StockReviewTradeWarnings.initialize(state);
        rebuildSnapshot();
        updateTradeWarning(null);
    }

    @Override
    protected void onCloseRequested() {
        if (modes.isColorDebugMode()) {
            modes.leaveColorDebug(state);
            rebuildContent();
            return;
        }
        if (modes.isFilterMode()) {
            modes.leaveFilters(state);
            rebuildContent();
            return;
        }
        if (modes.isReviewMode()) {
            state.setListScrollOffset(0);
            reopen(false);
            return;
        }
        close();
    }

    @Override
    public void reportDialogDismissed(int option) {
        StockReviewHotkeyScript.markDialogClosed();
    }

    @Override
    protected boolean canRenderContent() {
        return snapshot != null;
    }

    @Override
    protected WimGuiListBounds renderContent(CustomPanelAPI content,
                                             List<WimGuiButtonBinding<StockReviewAction>> buttonBindings) {
        return renderer.render(content, snapshot, state, pendingTrades.asList(), modes.isReviewMode(),
                modes.isFilterMode(),
                modes.isColorDebugMode(),
                modes.getColorDebugTargetIndex(),
                modes.currentColorDebugDraft(),
                modes.isColorDebugPersistent(),
                buttonBindings);
    }

    @Override
    protected void onScroll(int scrollDelta, int maxScrollOffset) {
        state.adjustListScrollOffset(scrollDelta, maxScrollOffset);
    }

    @Override
    protected void onRebuildFailed(Throwable t) {
        LOG.error("WP_STOCK_REVIEW rebuild failed", t);
    }

    @Override
    public void handle(StockReviewAction action) {
        if (action == null) {
            return;
        }
        StockReviewAction.Type type = action.getType();
        if (StockReviewAction.Type.TOGGLE_CATEGORY.equals(type)) {
            state.toggle(action.getItemType(), action.getCategory());
            rebuildContent();
            return;
        }
        if (StockReviewAction.Type.TOGGLE_ITEM_TYPE.equals(type)) {
            state.toggle(action.getItemType());
            rebuildContent();
            return;
        }
        if (StockReviewAction.Type.TOGGLE_TRADE_GROUP.equals(type)) {
            state.toggle(action.getTradeGroup());
            rebuildContent();
            return;
        }
        if (StockReviewAction.Type.TOGGLE_ITEM.equals(type)) {
            state.toggleItem(action.getItemKey());
            rebuildContent();
            return;
        }
        if (StockReviewAction.Type.ADJUST_PLAN.equals(type)) {
            trades.adjustPendingTrade(action);
            return;
        }
        if (StockReviewAction.Type.ADJUST_TO_SUFFICIENT.equals(type)) {
            trades.adjustPendingTrade(action);
            return;
        }
        if (StockReviewAction.Type.BUY_FROM_SUBMARKET.equals(type)) {
            trades.addPendingTrade(action);
            return;
        }
        if (StockReviewAction.Type.CYCLE_SORT_MODE.equals(type)) {
            state.cycleSortMode();
            rebuildSnapshot();
            rebuildContent();
            return;
        }
        if (StockReviewAction.Type.CYCLE_SOURCE_MODE.equals(type)) {
            state.cycleSourceMode();
            pendingTrades.clear();
            StockReviewTradeWarnings.clear(state);
            modes.setReviewMode(false);
            state.setListScrollOffset(0);
            rebuildSnapshot();
            rebuildContent();
            return;
        }
        if (StockReviewAction.Type.TOGGLE_BLACK_MARKET.equals(type)) {
            if (state.getSourceMode().isRemote()) {
                rebuildContent();
                return;
            }
            state.toggleBlackMarket();
            pendingTrades.clear();
            StockReviewTradeWarnings.clear(state);
            modes.setReviewMode(false);
            rebuildSnapshot();
            rebuildContent();
            return;
        }
        if (StockReviewAction.Type.SCROLL_LIST.equals(type)) {
            state.adjustListScrollOffset(action.getQuantity(), maxScrollOffset());
            rebuildContent();
            return;
        }
        if (StockReviewAction.Type.RESET_PLAN.equals(type)) {
            trades.resetPlan(action.getItemKey());
            return;
        }
        if (StockReviewAction.Type.PURCHASE_ALL_UNTIL_SUFFICIENT.equals(type)) {
            trades.purchaseAllUntilSufficient();
            return;
        }
        if (StockReviewAction.Type.SELL_ALL_UNTIL_SUFFICIENT.equals(type)) {
            trades.sellAllUntilSufficient();
            return;
        }
        if (StockReviewAction.Type.RESET_ALL_TRADES.equals(type)) {
            pendingTrades.clear();
            updateTradeWarning(null);
            rebuildContent();
            return;
        }
        if (StockReviewAction.Type.OPEN_FILTERS.equals(type)) {
            modes.enterFilters(state);
            rebuildContent();
            return;
        }
        if (StockReviewAction.Type.TOGGLE_FILTER_GROUP.equals(type)) {
            state.toggle(action.getFilterGroup());
            rebuildContent();
            return;
        }
        if (StockReviewAction.Type.TOGGLE_FILTER.equals(type)) {
            state.toggleFilter(action.getFilter());
            rebuildContent();
            return;
        }
        if (StockReviewAction.Type.RESET_FILTERS.equals(type)) {
            state.clearFilters();
            rebuildContent();
            return;
        }
        if (StockReviewAction.Type.OPEN_COLOR_DEBUG.equals(type)) {
            modes.enterColorDebug(state);
            rebuildContent();
            return;
        }
        if (StockReviewAction.Type.DEBUG_CYCLE_TARGET.equals(type)) {
            modes.cycleColorDebugTarget(action.getQuantity());
            rebuildContent();
            return;
        }
        if (StockReviewAction.Type.DEBUG_TOGGLE_PERSISTENCE.equals(type)) {
            modes.toggleColorDebugPersistence();
            rebuildContent();
            return;
        }
        if (StockReviewAction.Type.DEBUG_ADJUST_RED.equals(type)) {
            modes.adjustColorDebugDraft(action.getQuantity(), 0, 0);
            rebuildContent();
            return;
        }
        if (StockReviewAction.Type.DEBUG_ADJUST_GREEN.equals(type)) {
            modes.adjustColorDebugDraft(0, action.getQuantity(), 0);
            rebuildContent();
            return;
        }
        if (StockReviewAction.Type.DEBUG_ADJUST_BLUE.equals(type)) {
            modes.adjustColorDebugDraft(0, 0, action.getQuantity());
            rebuildContent();
            return;
        }
        if (StockReviewAction.Type.DEBUG_APPLY.equals(type)) {
            modes.applyColorDebugDraft();
            rebuildContent();
            return;
        }
        if (StockReviewAction.Type.DEBUG_CONFIRM.equals(type)) {
            modes.applyColorDebugDraft();
            modes.leaveColorDebug(state);
            rebuildContent();
            return;
        }
        if (StockReviewAction.Type.DEBUG_RESTORE.equals(type)) {
            modes.restoreColorDebugDraft();
            rebuildContent();
            return;
        }
        if (StockReviewAction.Type.DEBUG_NOOP.equals(type)) {
            rebuildContent();
            return;
        }
        if (StockReviewAction.Type.REVIEW_PURCHASE.equals(type)) {
            if (!pendingTrades.isEmpty()) {
                state.setListScrollOffset(0);
                state.setExpanded(StockReviewTradeGroup.BUYING, true);
                state.setExpanded(StockReviewTradeGroup.SELLING, true);
                reopen(true);
            }
            return;
        }
        if (StockReviewAction.Type.GO_BACK.equals(type)) {
            if (modes.isColorDebugMode()) {
                modes.leaveColorDebug(state);
                rebuildContent();
                return;
            }
            if (modes.isFilterMode()) {
                modes.leaveFilters(state);
                rebuildContent();
                return;
            }
            state.setListScrollOffset(0);
            reopen(false);
            return;
        }
        if (StockReviewAction.Type.CONFIRM_PURCHASE.equals(type)) {
            execution.confirmPendingPurchases();
            return;
        }
        if (StockReviewAction.Type.CLOSE.equals(type)) {
            close();
        }
    }

    public WeaponStockSnapshot snapshot() {
        return snapshot;
    }

    public void updateTradeWarning(String explicitWarning) {
        StockReviewTradeWarnings.update(snapshot, state, pendingTrades.asList(), explicitWarning);
    }

    public void requestContentRebuild() {
        rebuildContent();
    }

    public SectorAPI sector() {
        WimGuiCampaignDialogHost host = WimGuiCampaignDialogHost.current();
        return host.getSector();
    }

    public MarketAPI market() {
        WimGuiCampaignDialogHost host = WimGuiCampaignDialogHost.current();
        return host.getCurrentMarketOr(initialMarket);
    }

    public void postMessage(String message) {
        reportMessage(message);
    }

    private void reportMessage(String message) {
        WimGuiCampaignDialogHost.current().addMessage(message);
    }

    public void rebuildSnapshot() {
        WimGuiCampaignDialogHost host = WimGuiCampaignDialogHost.current();
        SectorAPI sector = host.getSector();
        MarketAPI market = host.getCurrentMarketOr(initialMarket);
        snapshot = snapshotBuilder.build(sector, market, config, state.getSortMode(),
                state.isIncludeCurrentMarketStorage(), state.isIncludeBlackMarket(),
                state.getSourceMode());
    }

    public void exitReviewMode() {
        modes.exitReview(state);
    }

    public void refreshVanillaCargoScreen() {
        WimGuiCampaignDialogHost.current().refreshCargoCore(
                LOG,
                "WP_STOCK_REVIEW refreshed vanilla cargo screen",
                initialMarket);
    }

    public void requestReopen(boolean review) {
        StockReviewHotkeyScript.requestReopen(market(),
                new StockReviewLaunchState(state, pendingTrades.asList(), review));
    }

    public void requestClose() {
        close();
    }

    public void reopen(boolean review) {
        requestReopen(review);
        close();
    }
}
