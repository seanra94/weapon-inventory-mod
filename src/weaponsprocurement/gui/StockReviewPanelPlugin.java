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
        implements StockReviewUiController.Host,
        StockReviewTradeController.Host,
        StockReviewExecutionController.Host {
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
    private final StockReviewUiController ui;
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
        this.ui = new StockReviewUiController(state, modes, pendingTrades, this);
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
        ui.handleCloseRequested();
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
        if (StockReviewAction.Type.CONFIRM_PURCHASE.equals(type)) {
            execution.confirmPendingTrades();
            return;
        }
        if (ui.handle(action)) {
            return;
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

    public int currentMaxScrollOffset() {
        return maxScrollOffset();
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
