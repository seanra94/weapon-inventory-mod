package weaponsprocurement.gui;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import org.apache.log4j.Logger;
import weaponsprocurement.core.StockReviewConfig;
import weaponsprocurement.core.StockSourceMode;
import weaponsprocurement.core.StockPurchaseService;
import weaponsprocurement.core.SubmarketWeaponStock;
import weaponsprocurement.core.WeaponStockRecord;
import weaponsprocurement.core.WeaponStockSnapshot;
import weaponsprocurement.core.WeaponStockSnapshotBuilder;

import java.util.List;

public final class StockReviewPanelPlugin extends WimGuiModalPanelPlugin<StockReviewAction> {
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
            adjustPendingTrade(action);
            return;
        }
        if (StockReviewAction.Type.ADJUST_TO_SUFFICIENT.equals(type)) {
            adjustPendingTrade(action);
            return;
        }
        if (StockReviewAction.Type.BUY_FROM_SUBMARKET.equals(type)) {
            addPendingTrade(action);
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
            resetPlan(action.getItemKey());
            return;
        }
        if (StockReviewAction.Type.PURCHASE_ALL_UNTIL_SUFFICIENT.equals(type)) {
            purchaseAllUntilSufficient();
            return;
        }
        if (StockReviewAction.Type.SELL_ALL_UNTIL_SUFFICIENT.equals(type)) {
            sellAllUntilSufficient();
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
            confirmPendingPurchases();
            return;
        }
        if (StockReviewAction.Type.CLOSE.equals(type)) {
            close();
        }
    }

    private void addPendingTrade(StockReviewAction action) {
        int available = availableFor(action);
        if (available <= 0) {
            reportMessage(action.getQuantity() < 0 ? "No more player-cargo stock is available to sell." : "No more buyable stock is available for that plan.");
            updateTradeWarning(null);
            rebuildContent();
            return;
        }
        int requested = action.getQuantity();
        int quantity = requested > 0 ? Math.min(requested, available) : -Math.min(-requested, available);
        pendingTrades.add(action.getItemKey(), action.getSubmarketId(), quantity);
        if (Math.abs(quantity) < Math.abs(requested)) {
            reportMessage("Only " + Math.abs(quantity) + " more can be planned for that weapon.");
        }
        updateTradeWarning(null);
        rebuildContent();
    }

    private void adjustPendingTrade(StockReviewAction action) {
        int available = availableFor(action);
        if (available <= 0) {
            reportMessage(action.getQuantity() < 0 ? "No more queued or player-cargo stock is available to remove from the plan." : "No more queued sales or buyable stock is available for that plan.");
            updateTradeWarning(null);
            rebuildContent();
            return;
        }
        int requested = action.getQuantity();
        int quantity = requested > 0 ? Math.min(requested, available) : -Math.min(-requested, available);
        pendingTrades.adjustItemNet(action.getItemKey(), quantity);
        if (Math.abs(quantity) < Math.abs(requested)) {
            reportMessage("Only " + Math.abs(quantity) + " more can be planned for that weapon.");
        }
        updateTradeWarning(null);
        rebuildContent();
    }

    private void resetPlan(String itemKey) {
        pendingTrades.resetItem(itemKey);
        updateTradeWarning(null);
        rebuildContent();
    }

    private void purchaseAllUntilSufficient() {
        if (snapshot == null) {
            return;
        }
        int added = 0;
        String explicitWarning = null;
        StockReviewQuoteBook quoteBook = new StockReviewQuoteBook(snapshot);
        List<WeaponStockRecord> records = StockReviewTradePlanner.cheapestFirstVisibleBuyableRecords(snapshot);
        StockReviewTradeContext tradeContext = new StockReviewTradeContext(snapshot, pendingTrades.asList());
        for (int i = 0; i < records.size(); i++) {
            WeaponStockRecord record = records.get(i);
            int needed = tradeContext.buyNeededForSufficiency(record);
            int quantity = tradeContext.affordableBuyQuantity(record, null, needed);
            explicitWarning = StockReviewTradeWarnings.purchaseAllLimitWarning(
                    quoteBook,
                    pendingTrades.asList(),
                    record,
                    tradeContext,
                    needed,
                    quantity,
                    explicitWarning);
            if (quantity <= 0) {
                continue;
            }
            pendingTrades.add(record.getItemKey(), null, quantity);
            tradeContext = new StockReviewTradeContext(snapshot, pendingTrades.asList());
            added += quantity;
        }
        if (added <= 0) {
            reportMessage("No additional sufficient-stock purchases are available.");
            updateTradeWarning(explicitWarning);
            rebuildContent();
            return;
        }
        updateTradeWarning(explicitWarning);
        rebuildContent();
    }

    private void sellAllUntilSufficient() {
        if (snapshot == null) {
            return;
        }
        int removed = 0;
        List<WeaponStockRecord> records = StockReviewTradePlanner.visibleTradeableRecords(snapshot);
        StockReviewTradeContext tradeContext = new StockReviewTradeContext(snapshot, pendingTrades.asList());
        for (int i = 0; i < records.size(); i++) {
            WeaponStockRecord record = records.get(i);
            int quantity = tradeContext.sellableUntilSufficient(record);
            if (quantity <= 0) {
                continue;
            }
            pendingTrades.add(record.getItemKey(), null, -quantity);
            tradeContext = new StockReviewTradeContext(snapshot, pendingTrades.asList());
            removed += quantity;
        }
        if (removed <= 0) {
            reportMessage("No sufficient-stock sales are available.");
            updateTradeWarning(null);
            rebuildContent();
            return;
        }
        updateTradeWarning(null);
        rebuildContent();
    }

    private void updateTradeWarning(String explicitWarning) {
        StockReviewTradeWarnings.update(snapshot, state, pendingTrades.asList(), explicitWarning);
    }

    private int availableFor(StockReviewAction action) {
        WeaponStockRecord record = snapshot == null ? null : snapshot.getRecord(action.getItemKey());
        if (record == null) {
            return 0;
        }
        StockReviewTradeContext tradeContext = new StockReviewTradeContext(snapshot, pendingTrades.asList());
        if (action.getQuantity() < 0) {
            return tradeContext.negativeAdjustmentRemaining(record, -action.getQuantity());
        }
        if (action.getSubmarketId() == null) {
            return tradeContext.positiveAdjustmentRemaining(record, action.getQuantity());
        }
        return tradeContext.affordableBuyQuantity(record, action.getSubmarketId(), action.getQuantity());
    }

    private void confirmPendingPurchases() {
        if (pendingTrades.isEmpty()) {
            reopen(false);
            return;
        }
        StockReviewTradeContext tradeContext = new StockReviewTradeContext(snapshot, pendingTrades.asList());
        int estimatedCost = tradeContext.totalCost();
        if (estimatedCost == StockReviewQuoteBook.PRICE_UNAVAILABLE) {
            reportMessage("Could not price every queued weapon. Adjust the plan and try again.");
            rebuildContent();
            return;
        }
        float credits = tradeContext.credits();
        if (estimatedCost > 0 && credits + 0.01f < estimatedCost) {
            reportMessage("Need " + StockReviewFormat.credits(estimatedCost) + " for these trades.");
            updateTradeWarning(StockReviewTradeWarnings.NOT_ENOUGH_CREDITS);
            rebuildContent();
            return;
        }
        float cargoDelta = tradeContext.totalCargoSpaceDelta();
        if (cargoDelta > tradeContext.cargoSpaceLeft() + 0.01f) {
            reportMessage("Need " + Math.round(cargoDelta) + " cargo space for these trades.");
            updateTradeWarning(StockReviewTradeWarnings.NO_CARGO_CAPACITY);
            rebuildContent();
            return;
        }

        WimGuiCampaignDialogHost host = WimGuiCampaignDialogHost.current();
        SectorAPI sector = host.getSector();
        MarketAPI market = host.getCurrentMarketOr(initialMarket);
        List<StockReviewPendingPurchase> executionOrder = StockReviewTradePlanner.executionOrder(pendingTrades.asList());
        StockSourceMode sourceMode = snapshot == null ? StockSourceMode.LOCAL : snapshot.getSourceMode();
        for (int i = 0; i < executionOrder.size(); i++) {
            StockReviewPendingPurchase purchase = executionOrder.get(i);
            StockPurchaseService.PurchaseResult result = executePendingPurchaseSafely(sector, market, purchase, sourceMode);
            if (result == null || !result.isSuccess()) {
                if (result != null) {
                    reportPurchaseFailure(result);
                }
                pendingTrades.removeExecuted(executionOrder, i);
                rebuildSnapshot();
                rebuildContent();
                return;
            }
        }
        pendingTrades.clear();
        updateTradeWarning(null);
        modes.exitReview(state);
        rebuildSnapshot();
        if (StockReviewStyle.REFRESH_VANILLA_CORE_AFTER_PURCHASE) {
            StockReviewHotkeyScript.requestReopen(market, state);
            refreshVanillaCargoScreen();
            close();
            return;
        }
        reopen(false);
    }

    private StockPurchaseService.PurchaseResult executePendingPurchaseSafely(SectorAPI sector,
                                                                             MarketAPI market,
                                                                             StockReviewPendingPurchase purchase,
                                                                             StockSourceMode sourceMode) {
        try {
            return executePendingPurchase(sector, market, purchase, sourceMode);
        } catch (Throwable t) {
            LOG.error("WP_STOCK_REVIEW queued trade execution crashed item="
                    + (purchase == null ? "null" : purchase.getItemKey())
                    + " source=" + (purchase == null ? "null" : purchase.getSubmarketId())
                    + " quantity=" + (purchase == null ? 0 : purchase.getQuantity())
                    + " sourceMode=" + sourceMode, t);
            return StockPurchaseService.PurchaseResult.failure("Trade failed during execution. Check starsector.log.");
        }
    }

    private StockPurchaseService.PurchaseResult executePendingPurchase(SectorAPI sector,
                                                                       MarketAPI market,
                                                                       StockReviewPendingPurchase purchase,
                                                                       StockSourceMode sourceMode) {
        WeaponStockRecord record = snapshot == null ? null : snapshot.getRecord(purchase.getItemKey());
        if (record == null) {
            return StockPurchaseService.PurchaseResult.failure("No queued item record is available.");
        }
        if (purchase.isSell()) {
            if (sourceMode != null && sourceMode.isRemote()) {
                return purchaseService.sellItemToMarket(sector, market, record.getItemType(), record.getItemId(), -purchase.getQuantity(), false);
            }
            return purchaseService.sellItemToMarket(sector, market, record.getItemType(), record.getItemId(), -purchase.getQuantity(), state.isIncludeBlackMarket());
        }
        if (StockSourceMode.FIXERS.equals(sourceMode)) {
            return purchaseService.buyItemFromFixersMarket(sector, record.getItemType(), record.getItemId(), purchase.getQuantity(),
                    virtualUnitPrice(purchase.getItemKey()), virtualUnitCargoSpace(purchase.getItemKey()));
        }
        if (StockSourceMode.SECTOR.equals(sourceMode)) {
            return purchaseService.buyItemFromSectorSources(sector, record.getItemType(), record.getItemId(), purchase.getQuantity(),
                    stockSources(purchase.getItemKey(), purchase.getSubmarketId()));
        }
        if (purchase.getSubmarketId() == null) {
            return purchaseService.buyCheapestItem(sector, market, record.getItemType(), record.getItemId(), purchase.getQuantity(), state.isIncludeBlackMarket());
        }
        return purchaseService.buyCheapestItem(sector, market, record.getItemType(), record.getItemId(),
                purchase.getQuantity(), state.isIncludeBlackMarket());
    }

    private void reportPurchaseFailure(StockPurchaseService.PurchaseResult result) {
        reportMessage(result.getMessage());
        LOG.info("WP_STOCK_REVIEW trade blocked: " + result.getMessage());
    }

    private void reportMessage(String message) {
        WimGuiCampaignDialogHost.current().addMessage(message);
    }

    private void rebuildSnapshot() {
        WimGuiCampaignDialogHost host = WimGuiCampaignDialogHost.current();
        SectorAPI sector = host.getSector();
        MarketAPI market = host.getCurrentMarketOr(initialMarket);
        snapshot = snapshotBuilder.build(sector, market, config, state.getSortMode(),
                state.isIncludeCurrentMarketStorage(), state.isIncludeBlackMarket(),
                state.getSourceMode());
    }

    private List<SubmarketWeaponStock> stockSources(String itemKey, String sourceId) {
        WeaponStockRecord record = snapshot == null ? null : snapshot.getRecord(itemKey);
        if (record == null) {
            return java.util.Collections.emptyList();
        }
        if (sourceId == null || sourceId.isEmpty()) {
            return record.getSubmarketStocks();
        }
        java.util.List<SubmarketWeaponStock> result = new java.util.ArrayList<SubmarketWeaponStock>();
        for (int i = 0; i < record.getSubmarketStocks().size(); i++) {
            SubmarketWeaponStock stock = record.getSubmarketStocks().get(i);
            if (sourceId.equals(stock.getSourceId()) || sourceId.equals(stock.getSubmarketId())) {
                result.add(stock);
            }
        }
        return result;
    }

    private int virtualUnitPrice(String itemKey) {
        WeaponStockRecord record = snapshot == null ? null : snapshot.getRecord(itemKey);
        return record == null ? 0 : record.getCheapestPurchasableUnitPrice();
    }

    private float virtualUnitCargoSpace(String itemKey) {
        WeaponStockRecord record = snapshot == null ? null : snapshot.getRecord(itemKey);
        if (record == null || record.getSubmarketStocks().isEmpty()) {
            return 1f;
        }
        return Math.max(1f, record.getSubmarketStocks().get(0).getUnitCargoSpace());
    }

    private void refreshVanillaCargoScreen() {
        WimGuiCampaignDialogHost.current().refreshCargoCore(
                LOG,
                "WP_STOCK_REVIEW refreshed vanilla cargo screen",
                initialMarket);
    }

    private void reopen(boolean review) {
        StockReviewHotkeyScript.requestReopen(initialMarket,
                new StockReviewLaunchState(state, pendingTrades.asList(), review));
        close();
    }
}
