package weaponsprocurement.ui.stockreview

import weaponsprocurement.ui.*

import com.fs.starfarer.api.campaign.SectorAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import org.apache.log4j.Logger
import weaponsprocurement.trade.execution.StockPurchaseService
import weaponsprocurement.config.StockReviewConfig
import weaponsprocurement.stock.item.WeaponStockSnapshot
import weaponsprocurement.stock.item.WeaponStockSnapshotBuilder
import weaponsprocurement.lifecycle.StockReviewHotkeyScript

class StockReviewPanelPlugin(
    private val initialMarket: MarketAPI?,
    launchState: StockReviewLaunchState?,
) : WimGuiModalPanelPlugin<StockReviewAction>(
    StockReviewAction::class.java,
    StockReviewStyle.widthFor(reviewMode(launchState)),
    StockReviewStyle.HEIGHT,
    StockReviewStyle.BUTTON_POLL_FRAMES_AFTER_MOUSE_EVENT,
    StockReviewStyle.initialListBounds(reviewMode(launchState)),
),
    StockReviewUiController.Host,
    StockReviewTradeController.Host,
    StockReviewExecutionController.Host {
    private val config: StockReviewConfig = StockReviewConfig.load()
    private val state: StockReviewState = if (launchState?.getState() == null) {
        StockReviewState(config)
    } else {
        StockReviewState(launchState.getState()!!)
    }
    private val renderer = StockReviewRenderer()
    private val snapshotBuilder = WeaponStockSnapshotBuilder()
    private val purchaseService = StockPurchaseService()
    private val pendingTrades = StockReviewPendingTrades()
    private val modes: StockReviewModeController
    private val ui: StockReviewUiController
    private val trades: StockReviewTradeController
    private val execution: StockReviewExecutionController
    private var snapshot: WeaponStockSnapshot? = null

    init {
        if (launchState != null) {
            pendingTrades.replaceWith(launchState.getPendingTrades())
        }
        modes = StockReviewModeController(reviewMode(launchState))
        ui = StockReviewUiController(state, modes, pendingTrades, this)
        trades = StockReviewTradeController(state, pendingTrades, this)
        execution = StockReviewExecutionController(state, pendingTrades, purchaseService, this)
    }

    fun isReviewMode(): Boolean = modes.isReviewMode()

    override fun onInit() {
        StockReviewTradeWarnings.initialize(state)
        rebuildSnapshot()
        updateTradeWarning(null)
    }

    override fun onCloseRequested() {
        ui.handleCloseRequested()
    }

    override fun reportDialogDismissed(option: Int) {
        StockReviewHotkeyScript.markDialogClosed()
    }

    override fun canRenderContent(): Boolean = snapshot != null

    override fun renderContent(
        content: CustomPanelAPI,
        buttonBindings: MutableList<WimGuiButtonBinding<StockReviewAction>>,
    ): WimGuiListBounds = renderer.render(
        content,
        snapshot!!,
        state,
        pendingTrades.asList(),
        modes.isReviewMode(),
        modes.isFilterMode(),
        modes.isColorDebugMode(),
        modes.getColorDebugTargetIndex(),
        modes.currentColorDebugDraft(),
        modes.isColorDebugPersistent(),
        buttonBindings,
    )

    override fun onScroll(scrollDelta: Int, maxScrollOffset: Int) {
        state.adjustListScrollOffset(scrollDelta, maxScrollOffset)
    }

    override fun onRebuildFailed(t: Throwable) {
        LOG.error("WP_STOCK_REVIEW rebuild failed", t)
    }

    override fun handle(action: StockReviewAction) {
        val type = action.getType()
        if (StockReviewAction.Type.ADJUST_PLAN == type) {
            trades.adjustPendingTrade(action)
            return
        }
        if (StockReviewAction.Type.ADJUST_TO_SUFFICIENT == type) {
            trades.adjustPendingTrade(action)
            return
        }
        if (StockReviewAction.Type.RESET_PLAN == type) {
            trades.resetPlan(action.getItemKey())
            return
        }
        if (StockReviewAction.Type.PURCHASE_ALL_UNTIL_SUFFICIENT == type) {
            trades.purchaseAllUntilSufficient()
            return
        }
        if (StockReviewAction.Type.SELL_ALL_UNTIL_SUFFICIENT == type) {
            trades.sellAllUntilSufficient()
            return
        }
        if (StockReviewAction.Type.CONFIRM_PURCHASE == type) {
            execution.confirmPendingTrades()
            return
        }
        if (ui.handle(action)) {
            return
        }
    }

    override fun snapshot(): WeaponStockSnapshot? = snapshot

    override fun updateTradeWarning(explicitWarning: String?) {
        StockReviewTradeWarnings.update(snapshot, state, pendingTrades.asList(), explicitWarning)
    }

    override fun requestContentRebuild() {
        rebuildContent()
    }

    override fun currentMaxScrollOffset(): Int = maxScrollOffset()

    override fun sector(): SectorAPI? {
        val host = WimGuiCampaignDialogHost.current()
        return host.getSector()
    }

    override fun market(): MarketAPI? {
        val host = WimGuiCampaignDialogHost.current()
        return host.getCurrentMarketOr(initialMarket)
    }

    override fun postMessage(message: String?) {
        reportMessage(message)
    }

    private fun reportMessage(message: String?) {
        WimGuiCampaignDialogHost.current().addMessage(message)
    }

    override fun rebuildSnapshot() {
        val host = WimGuiCampaignDialogHost.current()
        val sector = host.getSector()
        val market = host.getCurrentMarketOr(initialMarket)
        snapshot = snapshotBuilder.build(
            sector,
            market,
            config,
            state.getSortMode(),
            state.isIncludeCurrentMarketStorage(),
            state.isIncludeBlackMarket(),
            state.getSourceMode(),
        )
    }

    override fun exitReviewMode() {
        modes.exitReview(state)
    }

    override fun refreshVanillaCargoScreen() {
        WimGuiCampaignDialogHost.current().refreshCargoCore(
            LOG,
            "WP_STOCK_REVIEW refreshed vanilla cargo screen",
            initialMarket,
        )
    }

    override fun requestReopen(review: Boolean) {
        StockReviewHotkeyScript.requestReopen(
            market(),
            StockReviewLaunchState(state, pendingTrades.asList(), review),
        )
    }

    override fun requestClose() {
        close()
    }

    override fun reopen(review: Boolean) {
        requestReopen(review)
        close()
    }

    companion object {
        private val LOG: Logger = Logger.getLogger(StockReviewPanelPlugin::class.java)

        private fun reviewMode(launchState: StockReviewLaunchState?): Boolean = launchState != null && launchState.isReviewMode()
    }
}