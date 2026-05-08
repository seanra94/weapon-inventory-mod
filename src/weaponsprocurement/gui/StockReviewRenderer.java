package weaponsprocurement.gui;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import weaponsprocurement.core.StockSourceMode;
import weaponsprocurement.core.WeaponStockSnapshot;

import java.awt.Color;
import java.util.List;

final class StockReviewRenderer implements WimGuiModalListRenderer.ScrollRowFactory<StockReviewAction>,
        WimGuiModalListRenderer.ExtraGapProvider<StockReviewAction> {
    private final WimGuiSemanticButtonFactory<StockReviewAction> buttonFactory =
            new WimGuiSemanticButtonFactory<StockReviewAction>(StockReviewStyle.ROW_BORDER);

    WimGuiListBounds render(CustomPanelAPI root,
                            WeaponStockSnapshot snapshot,
                            StockReviewState state,
                            List<StockReviewPendingTrade> pendingTrades,
                            boolean reviewMode,
                            boolean filterMode,
                            boolean colorDebugMode,
                            int colorDebugTargetIndex,
                            Color colorDebugDraft,
                            boolean colorDebugPersistent,
                            List<WimGuiButtonBinding<StockReviewAction>> buttons) {
        if (filterMode || colorDebugMode) {
            renderHeader(root, snapshot, state, reviewMode, filterMode, colorDebugMode, colorDebugTargetIndex, colorDebugDraft);
        }
        if (!reviewMode && !filterMode && !colorDebugMode) {
            renderActionRow(root, snapshot, state, buttons);
        }
        StockReviewTradeContext tradeContext = new StockReviewTradeContext(snapshot, pendingTrades);
        WimGuiListBounds result = colorDebugMode
                ? renderColorDebugList(root, colorDebugTargetIndex, colorDebugDraft, colorDebugPersistent, state, buttons)
                : filterMode
                ? renderFilterList(root, state, buttons)
                : reviewMode
                ? renderReviewList(root, snapshot, pendingTrades, state, tradeContext, buttons)
                : renderStockList(root, snapshot, state, tradeContext, buttons);
        if (!filterMode && !colorDebugMode) {
            StockReviewTradeSummaryRenderer.render(root, tradeContext, state, reviewMode);
        }
        StockReviewFooterRenderer.render(root, tradeContext, pendingTrades, reviewMode, filterMode,
                colorDebugMode, buttons);
        return result;
    }

    private void renderHeader(CustomPanelAPI root,
                              WeaponStockSnapshot snapshot,
                              StockReviewState state,
                              boolean reviewMode,
                              boolean filterMode,
                              boolean colorDebugMode,
                              int colorDebugTargetIndex,
                              Color colorDebugDraft) {
        String title = colorDebugMode ? "Debug Colors" : filterMode ? "Filters" : reviewMode ? "Review Trades" : "Make Trades";
        String status = colorDebugMode ? colorStatusLine(colorDebugTargetIndex, colorDebugDraft)
                : filterMode ? filterStatusLine(state) : statusLine(snapshot, state);
        WimGuiModalHeader.addTitleStatusHeader(
                root,
                StockReviewStyle.MODAL,
                StockReviewStyle.HEADER_HEIGHT,
                title,
                status,
                StockReviewStyle.PANEL_BACKGROUND,
                StockReviewStyle.PANEL_BORDER,
                StockReviewStyle.TEXT);
    }

    private void renderActionRow(CustomPanelAPI root,
                                 WeaponStockSnapshot snapshot,
                                 StockReviewState state,
                                 List<WimGuiButtonBinding<StockReviewAction>> buttons) {
        WimGuiModalActionRow.add(
                root,
                StockReviewStyle.MODAL,
                0f,
                0f,
                StockReviewStyle.ACTION_BUTTON_HEIGHT,
                StockReviewStyle.BUTTON_GAP,
                WimGuiButtonSpecs.of(
                        buttonFactory.enabledButton(StockReviewStyle.SORT_BUTTON_WIDTH, "Sort: " + snapshot.getSortMode().getLabel(),
                                StockReviewAction.cycleSortMode(), StockReviewStyle.ACTION_BACKGROUND,
                                StockReviewTooltips.sort(snapshot.getSortMode())),
                        buttonFactory.enabledButton(StockReviewStyle.SOURCE_BUTTON_WIDTH, "Source: " + snapshot.getSourceMode().getLabel(),
                                StockReviewAction.cycleSourceMode(), StockReviewStyle.ACTION_BACKGROUND,
                                StockReviewTooltips.source(snapshot.getSourceMode())),
                        buttonFactory.button(StockReviewStyle.BLACK_MARKET_BUTTON_WIDTH, "Black Market: " + onOff(snapshot.isIncludeBlackMarket()),
                                StockReviewAction.toggleBlackMarket(), !snapshot.getSourceMode().isRemote(), StockReviewStyle.ACTION_BACKGROUND,
                                "Include black-market stock for Local source mode. Remote source modes control their own stock."),
                        buttonFactory.enabledButton(StockReviewStyle.FILTER_BUTTON_WIDTH, "Filters: " + state.getActiveFilterCount(),
                                StockReviewAction.openFilters(), StockReviewStyle.ACTION_BACKGROUND,
                                "Open the weapon filter list."),
                        buttonFactory.enabledButton(StockReviewStyle.COLOR_BUTTON_WIDTH, "Colors",
                                StockReviewAction.openColorDebug(), StockReviewStyle.ACTION_BACKGROUND,
                                "Open the color debug menu.")),
                buttons);
    }

    private WimGuiListBounds renderStockList(CustomPanelAPI root,
                                             WeaponStockSnapshot snapshot,
                                             StockReviewState state,
                                             StockReviewTradeContext tradeContext,
                                             List<WimGuiButtonBinding<StockReviewAction>> buttons) {
        List<WimGuiListRow<StockReviewAction>> rows = StockReviewListModel.build(snapshot, state, tradeContext);
        return renderRows(root, rows, state, StockReviewStyle.TRADE_LIST, buttons);
    }

    private WimGuiListBounds renderReviewList(CustomPanelAPI root,
                                              WeaponStockSnapshot snapshot,
                                              List<StockReviewPendingTrade> pendingTrades,
                                              StockReviewState state,
                                              StockReviewTradeContext tradeContext,
                                              List<WimGuiButtonBinding<StockReviewAction>> buttons) {
        List<WimGuiListRow<StockReviewAction>> rows = StockReviewReviewListModel.build(snapshot, pendingTrades, state, tradeContext);
        return renderRows(root, rows, state, StockReviewStyle.REVIEW_LIST, buttons);
    }

    private WimGuiListBounds renderColorDebugList(CustomPanelAPI root,
                                                  int targetIndex,
                                                  Color draft,
                                                  boolean persistent,
                                                  StockReviewState state,
                                                  List<WimGuiButtonBinding<StockReviewAction>> buttons) {
        List<WimGuiListRow<StockReviewAction>> rows = StockReviewColorDebugRows.build(targetIndex, draft, persistent);
        return renderRows(root, rows, state, StockReviewStyle.LIST, buttons);
    }

    private WimGuiListBounds renderFilterList(CustomPanelAPI root,
                                              StockReviewState state,
                                              List<WimGuiButtonBinding<StockReviewAction>> buttons) {
        List<WimGuiListRow<StockReviewAction>> rows = StockReviewFilterListModel.build(state);
        return renderRows(root, rows, state, StockReviewStyle.FILTER_LIST, buttons);
    }

    private WimGuiListBounds renderRows(CustomPanelAPI root,
                                        List<WimGuiListRow<StockReviewAction>> rows,
                                        StockReviewState state,
                                        WimGuiModalListSpec spec,
                                        List<WimGuiButtonBinding<StockReviewAction>> buttons) {
        return WimGuiModalListRenderer.renderAndStoreOffset(
                root,
                rows,
                state,
                spec,
                this,
                this,
                buttons);
    }

    @Override
    public WimGuiListRow<StockReviewAction> createScrollRow(String label, int scrollDelta) {
        return StockReviewListRow.scroll(label, StockReviewAction.scrollList(scrollDelta));
    }

    @Override
    public float extraGapBefore(WimGuiListRow<StockReviewAction> row) {
        return row != null && row.hasTopGap() ? StockReviewStyle.CATEGORY_TOP_GAP : 0f;
    }

    private static String statusLine(WeaponStockSnapshot snapshot, StockReviewState state) {
        return "Market: " + snapshot.getMarketName()
                + " | Sort: " + snapshot.getSortMode().getLabel()
                + " | Owned source: " + ownedSourceLabel(snapshot)
                + " | Stock source: " + sourceLabel(snapshot.getSourceMode())
                + " | Black market: " + onOff(snapshot.isIncludeBlackMarket())
                + " | Filters: " + state.getActiveFilterCount();
    }

    private static String sourceLabel(StockSourceMode sourceMode) {
        return sourceMode == null ? "local" : sourceMode.getLabel();
    }

    private static String filterStatusLine(StockReviewState state) {
        return "Active filters: " + state.getActiveFilterCount()
                + " | Active filter rows are shown first";
    }

    private static String colorStatusLine(int targetIndex, Color draft) {
        WimGuiColorDebug.Target target = WimGuiColorDebug.targetAt(targetIndex);
        Color color = draft == null ? WimGuiColorDebug.currentColor(target) : draft;
        return target.getLabel() + " | RGB("
                + color.getRed() + ", "
                + color.getGreen() + ", "
                + color.getBlue() + ")";
    }

    private static String ownedSourceLabel(WeaponStockSnapshot snapshot) {
        if (snapshot.getOwnedSourcePolicy().name().contains("ACCESSIBLE_STORAGE")) {
            return "fleet + all accessible storage";
        }
        if (snapshot.getOwnedSourcePolicy().name().contains("CURRENT_MARKET_STORAGE")) {
            return "fleet + current market storage";
        }
        return "fleet only";
    }

    private static String onOff(boolean enabled) {
        return enabled ? "On" : "Off";
    }

}
