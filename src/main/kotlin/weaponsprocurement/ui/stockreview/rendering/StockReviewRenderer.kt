package weaponsprocurement.ui.stockreview.rendering

import weaponsprocurement.ui.WimGuiButtonBinding
import weaponsprocurement.ui.WimGuiButtonSpecs
import weaponsprocurement.ui.WimGuiColorDebug
import weaponsprocurement.ui.WimGuiListBounds
import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.WimGuiModalActionRow
import weaponsprocurement.ui.WimGuiModalHeader
import weaponsprocurement.ui.WimGuiModalListRenderer
import weaponsprocurement.ui.WimGuiModalListSpec
import weaponsprocurement.ui.WimGuiSemanticButtonFactory
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.rows.StockReviewColorDebugRows
import weaponsprocurement.ui.stockreview.rows.StockReviewFooterRenderer
import weaponsprocurement.ui.stockreview.rows.StockReviewListModel
import weaponsprocurement.ui.stockreview.rows.StockReviewListRow
import weaponsprocurement.ui.stockreview.rows.StockReviewReviewListModel
import weaponsprocurement.ui.stockreview.rows.StockReviewTradeSummaryRenderer
import weaponsprocurement.ui.stockreview.state.StockReviewFilterListModel
import weaponsprocurement.ui.stockreview.state.StockReviewState
import weaponsprocurement.ui.stockreview.tooltips.StockReviewTooltips
import weaponsprocurement.ui.stockreview.trade.StockReviewPendingTrade
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeContext
import com.fs.starfarer.api.ui.CustomPanelAPI
import weaponsprocurement.stock.item.StockSourceMode
import weaponsprocurement.stock.item.WeaponStockSnapshot
import java.awt.Color

class StockReviewRenderer :
    WimGuiModalListRenderer.ScrollRowFactory<StockReviewAction>,
    WimGuiModalListRenderer.ExtraGapProvider<StockReviewAction> {
    private val buttonFactory = WimGuiSemanticButtonFactory<StockReviewAction>(StockReviewStyle.ROW_BORDER)

    fun render(
        root: CustomPanelAPI,
        snapshot: WeaponStockSnapshot,
        state: StockReviewState,
        pendingTrades: List<StockReviewPendingTrade>,
        reviewMode: Boolean,
        filterMode: Boolean,
        colorDebugMode: Boolean,
        colorDebugTargetIndex: Int,
        colorDebugDraft: Color?,
        colorDebugPersistent: Boolean,
        buttons: MutableList<WimGuiButtonBinding<StockReviewAction>>,
    ): WimGuiListBounds {
        if (filterMode || colorDebugMode) {
            renderHeader(root, snapshot, state, reviewMode, filterMode, colorDebugMode, colorDebugTargetIndex, colorDebugDraft)
        }
        if (!reviewMode && !filterMode && !colorDebugMode) {
            renderActionRow(root, snapshot, state, buttons)
        }
        val tradeContext = StockReviewTradeContext(snapshot, pendingTrades)
        val result = if (colorDebugMode) {
            renderColorDebugList(root, colorDebugTargetIndex, colorDebugDraft, colorDebugPersistent, state, buttons)
        } else if (filterMode) {
            renderFilterList(root, state, buttons)
        } else if (reviewMode) {
            renderReviewList(root, snapshot, pendingTrades, state, tradeContext, buttons)
        } else {
            renderStockList(root, snapshot, state, tradeContext, buttons)
        }
        if (!filterMode && !colorDebugMode) {
            StockReviewTradeSummaryRenderer.render(root, tradeContext, state, reviewMode)
        }
        StockReviewFooterRenderer.render(root, tradeContext, pendingTrades, reviewMode, filterMode, colorDebugMode, buttons)
        return result
    }

    private fun renderHeader(
        root: CustomPanelAPI,
        snapshot: WeaponStockSnapshot,
        state: StockReviewState,
        reviewMode: Boolean,
        filterMode: Boolean,
        colorDebugMode: Boolean,
        colorDebugTargetIndex: Int,
        colorDebugDraft: Color?,
    ) {
        val title = if (colorDebugMode) {
            "Debug Colors"
        } else if (filterMode) {
            "Filters"
        } else if (reviewMode) {
            "Review Trades"
        } else {
            "Make Trades"
        }
        val status = if (colorDebugMode) {
            colorStatusLine(colorDebugTargetIndex, colorDebugDraft)
        } else if (filterMode) {
            filterStatusLine(state)
        } else {
            statusLine(snapshot, state)
        }
        WimGuiModalHeader.addTitleStatusHeader(
            root,
            StockReviewStyle.MODAL,
            StockReviewStyle.HEADER_HEIGHT,
            title,
            status,
            StockReviewStyle.PANEL_BACKGROUND,
            StockReviewStyle.PANEL_BORDER,
            StockReviewStyle.TEXT,
        )
    }

    private fun renderActionRow(
        root: CustomPanelAPI,
        snapshot: WeaponStockSnapshot,
        state: StockReviewState,
        buttons: MutableList<WimGuiButtonBinding<StockReviewAction>>,
    ) {
        WimGuiModalActionRow.add(
            root,
            StockReviewStyle.MODAL,
            0f,
            0f,
            StockReviewStyle.ACTION_BUTTON_HEIGHT,
            StockReviewStyle.BUTTON_GAP,
            WimGuiButtonSpecs.of(
                buttonFactory.enabledButton(
                    StockReviewStyle.SORT_BUTTON_WIDTH,
                    "Sort: ${snapshot.getSortMode().label}",
                    StockReviewAction.cycleSortMode(),
                    StockReviewStyle.ACTION_BACKGROUND,
                    StockReviewTooltips.sort(snapshot.getSortMode()),
                ),
                buttonFactory.enabledButton(
                    StockReviewStyle.SOURCE_BUTTON_WIDTH,
                    "Source: ${snapshot.getSourceMode().label}",
                    StockReviewAction.cycleSourceMode(),
                    StockReviewStyle.ACTION_BACKGROUND,
                    StockReviewTooltips.source(snapshot.getSourceMode()),
                ),
                buttonFactory.button(
                    StockReviewStyle.BLACK_MARKET_BUTTON_WIDTH,
                    "Black Market: ${onOff(snapshot.isIncludeBlackMarket())}",
                    StockReviewAction.toggleBlackMarket(),
                    !snapshot.getSourceMode().isRemote(),
                    StockReviewStyle.ACTION_BACKGROUND,
                    "Include black-market stock for Local source mode. Remote source modes control their own stock.",
                ),
                buttonFactory.enabledButton(
                    StockReviewStyle.FILTER_BUTTON_WIDTH,
                    "Filters: ${state.getActiveFilterCount()}",
                    StockReviewAction.openFilters(),
                    StockReviewStyle.ACTION_BACKGROUND,
                    "Open the weapon filter list.",
                ),
                buttonFactory.enabledButton(
                    StockReviewStyle.COLOR_BUTTON_WIDTH,
                    "Colors",
                    StockReviewAction.openColorDebug(),
                    StockReviewStyle.ACTION_BACKGROUND,
                    "Open the color debug menu.",
                ),
            ),
            buttons,
        )
    }

    private fun renderStockList(
        root: CustomPanelAPI,
        snapshot: WeaponStockSnapshot,
        state: StockReviewState,
        tradeContext: StockReviewTradeContext,
        buttons: MutableList<WimGuiButtonBinding<StockReviewAction>>,
    ): WimGuiListBounds {
        val rows = StockReviewListModel.build(snapshot, state, tradeContext)
        return renderRows(root, rows, state, StockReviewStyle.TRADE_LIST, buttons)
    }

    private fun renderReviewList(
        root: CustomPanelAPI,
        snapshot: WeaponStockSnapshot,
        pendingTrades: List<StockReviewPendingTrade>,
        state: StockReviewState,
        tradeContext: StockReviewTradeContext,
        buttons: MutableList<WimGuiButtonBinding<StockReviewAction>>,
    ): WimGuiListBounds {
        val rows = StockReviewReviewListModel.build(snapshot, pendingTrades, state, tradeContext)
        return renderRows(root, rows, state, StockReviewStyle.REVIEW_LIST, buttons)
    }

    private fun renderColorDebugList(
        root: CustomPanelAPI,
        targetIndex: Int,
        draft: Color?,
        persistent: Boolean,
        state: StockReviewState,
        buttons: MutableList<WimGuiButtonBinding<StockReviewAction>>,
    ): WimGuiListBounds {
        val rows = StockReviewColorDebugRows.build(targetIndex, draft, persistent)
        return renderRows(root, rows, state, StockReviewStyle.LIST, buttons)
    }

    private fun renderFilterList(
        root: CustomPanelAPI,
        state: StockReviewState,
        buttons: MutableList<WimGuiButtonBinding<StockReviewAction>>,
    ): WimGuiListBounds {
        val rows = StockReviewFilterListModel.build(state)
        return renderRows(root, rows, state, StockReviewStyle.FILTER_LIST, buttons)
    }

    private fun renderRows(
        root: CustomPanelAPI,
        rows: List<WimGuiListRow<StockReviewAction>>,
        state: StockReviewState,
        spec: WimGuiModalListSpec,
        buttons: MutableList<WimGuiButtonBinding<StockReviewAction>>,
    ): WimGuiListBounds = WimGuiModalListRenderer.renderAndStoreOffset(
        root,
        rows,
        state,
        spec,
        this,
        this,
        buttons,
    )

    override fun createScrollRow(label: String, scrollDelta: Int): WimGuiListRow<StockReviewAction> =
        StockReviewListRow.scroll(label, StockReviewAction.scrollList(scrollDelta))

    override fun extraGapBefore(row: WimGuiListRow<StockReviewAction>): Float =
        if (row.hasTopGap()) StockReviewStyle.CATEGORY_TOP_GAP else 0f

    companion object {
        private fun statusLine(snapshot: WeaponStockSnapshot, state: StockReviewState): String =
            "Market: ${snapshot.getMarketName()}" +
                " | Sort: ${snapshot.getSortMode().label}" +
                " | Owned source: ${ownedSourceLabel(snapshot)}" +
                " | Stock source: ${sourceLabel(snapshot.getSourceMode())}" +
                " | Black market: ${onOff(snapshot.isIncludeBlackMarket())}" +
                " | Filters: ${state.getActiveFilterCount()}"

        private fun sourceLabel(sourceMode: StockSourceMode?): String = sourceMode?.label ?: "local"

        private fun filterStatusLine(state: StockReviewState): String =
            "Active filters: ${state.getActiveFilterCount()} | Active filter rows are shown first"

        private fun colorStatusLine(targetIndex: Int, draft: Color?): String {
            val target = WimGuiColorDebug.targetAt(targetIndex)
            val color = draft ?: WimGuiColorDebug.currentColor(target)
            return (target?.label ?: "Unknown") + " | RGB(" +
                color.red + ", " +
                color.green + ", " +
                color.blue + ")"
        }

        private fun ownedSourceLabel(snapshot: WeaponStockSnapshot): String {
            if (snapshot.getOwnedSourcePolicy().name.contains("ACCESSIBLE_STORAGE")) {
                return "fleet + all accessible storage"
            }
            if (snapshot.getOwnedSourcePolicy().name.contains("CURRENT_MARKET_STORAGE")) {
                return "fleet + current market storage"
            }
            return "fleet only"
        }

        private fun onOff(enabled: Boolean): String = if (enabled) "On" else "Off"
    }
}