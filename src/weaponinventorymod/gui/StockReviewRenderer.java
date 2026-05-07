package weaponinventorymod.gui;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import weaponinventorymod.core.WeaponStockSnapshot;

import java.awt.Color;
import java.util.List;

final class StockReviewRenderer implements WimGuiModalListRenderer.ScrollRowFactory<StockReviewAction>,
        WimGuiModalListRenderer.ExtraGapProvider<StockReviewAction> {
    private final WimGuiSemanticButtonFactory<StockReviewAction> buttonFactory =
            new WimGuiSemanticButtonFactory<StockReviewAction>(StockReviewStyle.ROW_BORDER);

    WimGuiListBounds render(CustomPanelAPI root,
                            WeaponStockSnapshot snapshot,
                            StockReviewState state,
                            List<StockReviewPendingPurchase> pendingPurchases,
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
        StockReviewTradeContext tradeContext = new StockReviewTradeContext(snapshot, pendingPurchases);
        WimGuiListBounds result = colorDebugMode
                ? renderColorDebugList(root, colorDebugTargetIndex, colorDebugDraft, colorDebugPersistent, state, buttons)
                : filterMode
                ? renderFilterList(root, state, buttons)
                : reviewMode
                ? renderReviewList(root, snapshot, pendingPurchases, state, tradeContext, buttons)
                : renderStockList(root, snapshot, state, tradeContext, buttons);
        if (!filterMode && !colorDebugMode) {
            renderTradeSummary(root, tradeContext, reviewMode);
        }
        renderFooter(root, tradeContext, pendingPurchases, reviewMode, filterMode, colorDebugMode, colorDebugPersistent, buttons);
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
                                StockReviewAction.cycleSortMode(), StockReviewStyle.ACTION_BACKGROUND),
                        buttonFactory.enabledButton(StockReviewStyle.GLOBAL_MARKET_BUTTON_WIDTH, "Source: " + (snapshot.isGlobalMarketMode() ? "Global" : "Local"),
                                StockReviewAction.toggleGlobalMarket(), StockReviewStyle.ACTION_BACKGROUND),
                        buttonFactory.enabledButton(StockReviewStyle.BLACK_MARKET_BUTTON_WIDTH, "Black Market: " + onOff(snapshot.isIncludeBlackMarket()),
                                StockReviewAction.toggleBlackMarket(), StockReviewStyle.ACTION_BACKGROUND),
                        buttonFactory.enabledButton(StockReviewStyle.FILTER_BUTTON_WIDTH, "Filters: " + state.getActiveFilterCount(),
                                StockReviewAction.openFilters(), StockReviewStyle.ACTION_BACKGROUND),
                        buttonFactory.enabledButton(StockReviewStyle.COLOR_BUTTON_WIDTH, "Colors",
                                StockReviewAction.openColorDebug(), StockReviewStyle.ACTION_BACKGROUND)),
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
                                              List<StockReviewPendingPurchase> pendingPurchases,
                                              StockReviewState state,
                                              StockReviewTradeContext tradeContext,
                                              List<WimGuiButtonBinding<StockReviewAction>> buttons) {
        List<WimGuiListRow<StockReviewAction>> rows = StockReviewReviewListModel.build(snapshot, pendingPurchases, state, tradeContext);
        return renderRows(root, rows, state, StockReviewStyle.REVIEW_LIST, buttons);
    }

    private void renderTradeSummary(CustomPanelAPI root,
                                    StockReviewTradeContext tradeContext,
                                    boolean reviewMode) {
        int netCost = tradeContext.totalCost();
        String netLabel = netCost < 0 ? "Total Profit" : "Total Cost";
        String netValue = netCost == StockReviewQuoteBook.PRICE_UNAVAILABLE
                ? "Price Unavailable"
                : StockReviewFormat.credits(netCost);
        Color netFill = costValueFill(netCost, tradeContext.credits());
        float purchaseVolume = Math.max(0f, tradeContext.totalCargoSpaceDelta());
        float width = reviewMode ? StockReviewStyle.REVIEW_LIST_WIDTH : StockReviewStyle.LIST_WIDTH;
        float rowY = StockReviewStyle.SUMMARY_TOP;
        addSummaryRow(
                root,
                width,
                rowY,
                netLabel,
                netValue,
                netFill);
        rowY += StockReviewStyle.ROW_HEIGHT + StockReviewStyle.SUMMARY_ROW_GAP;
        addSummaryRow(
                root,
                width,
                rowY,
                "Credits Available",
                StockReviewFormat.credits(Math.round(tradeContext.credits())),
                StockReviewStyle.CELL_BACKGROUND);
        rowY += StockReviewStyle.ROW_HEIGHT + StockReviewStyle.SUMMARY_ROW_GAP;
        addSummaryRow(
                root,
                width,
                rowY,
                "Total Purchase Volume",
                formatCargo(purchaseVolume),
                StockReviewStyle.CELL_BACKGROUND);
        rowY += StockReviewStyle.ROW_HEIGHT + StockReviewStyle.SUMMARY_ROW_GAP;
        addSummaryRow(
                root,
                width,
                rowY,
                "Cargo Space Available",
                formatCargo(tradeContext.cargoSpaceLeft()),
                cargoValueFill(purchaseVolume, tradeContext.cargoSpaceLeft()));
    }

    private void addSummaryRow(CustomPanelAPI root,
                               float width,
                               float y,
                               String label,
                               String value,
                               Color valueFill) {
        WimGuiControls.addLabelTextRow(
                root,
                StockReviewStyle.PAD,
                y,
                width,
                StockReviewStyle.ROW_HEIGHT,
                label,
                value,
                valueFill,
                StockReviewStyle.ROW_BORDER,
                StockReviewStyle.TEXT);
    }

    private static Color costValueFill(int netCost, float creditsAvailable) {
        if (netCost < 0) {
            return StockReviewStyle.CONFIRM_BUTTON;
        }
        if (netCost <= 0) {
            return StockReviewStyle.CELL_BACKGROUND;
        }
        if (isNearLimit(netCost, creditsAvailable)) {
            return StockReviewStyle.PRESET_SCOPE_BUTTON;
        }
        return StockReviewStyle.CANCEL_BUTTON;
    }

    private static Color cargoValueFill(float purchaseVolume, float cargoSpaceAvailable) {
        if (purchaseVolume <= 0f) {
            return StockReviewStyle.CELL_BACKGROUND;
        }
        if (purchaseVolume > cargoSpaceAvailable) {
            return StockReviewStyle.CANCEL_BUTTON;
        }
        if (isNearLimit(purchaseVolume, cargoSpaceAvailable)) {
            return StockReviewStyle.PRESET_SCOPE_BUTTON;
        }
        return StockReviewStyle.CELL_BACKGROUND;
    }

    private static boolean isNearLimit(float value, float limit) {
        return limit > 0f && value >= limit * 0.95f && value <= limit;
    }

    private static String formatCargo(float value) {
        float rounded = Math.round(value);
        if (Math.abs(value - rounded) < 0.05f) {
            return Integer.toString(Math.round(rounded));
        }
        return String.format(java.util.Locale.US, "%.1f", value);
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

    private void renderFooter(CustomPanelAPI root,
                              StockReviewTradeContext tradeContext,
                              List<StockReviewPendingPurchase> pendingPurchases,
                              boolean reviewMode,
                              boolean filterMode,
                              boolean colorDebugMode,
                              boolean colorDebugPersistent,
                              List<WimGuiButtonBinding<StockReviewAction>> buttons) {
        if (colorDebugMode) {
            WimGuiModalFooter.addLeftRowAndRightButton(
                    root,
                    StockReviewStyle.MODAL,
                    StockReviewStyle.ACTION_BUTTON_HEIGHT,
                    StockReviewStyle.BUTTON_GAP,
                    WimGuiButtonSpecs.of(
                            footerButton("Confirm", StockReviewAction.debugConfirm(), true, StockReviewStyle.CONFIRM_BUTTON),
                        footerButton("Apply", StockReviewAction.debugApply(), true, StockReviewStyle.SAVE_BUTTON),
                            footerButton("Restore", StockReviewAction.debugRestore(), true, StockReviewStyle.LOAD_BUTTON)),
                    footerButton("Cancel", StockReviewAction.goBack(), true, StockReviewStyle.CANCEL_BUTTON),
                    buttons);
            return;
        }
        if (filterMode) {
            WimGuiModalFooter.addLeftRowAndRightButton(
                    root,
                    StockReviewStyle.MODAL,
                    StockReviewStyle.ACTION_BUTTON_HEIGHT,
                    StockReviewStyle.BUTTON_GAP,
                    WimGuiButtonSpecs.of(footerButton("Confirm", StockReviewAction.goBack(), true, StockReviewStyle.CONFIRM_BUTTON),
                            footerButton("Reset Filters", StockReviewAction.resetFilters(),
                                    true, StockReviewStyle.LOAD_BUTTON)),
                    footerButton("Cancel", StockReviewAction.goBack(), true, StockReviewStyle.CANCEL_BUTTON),
                    buttons);
            return;
        }
        if (reviewMode) {
            WimGuiModalFooter.addLeftRowAndRightButton(
                    root,
                    StockReviewStyle.MODAL,
                    StockReviewStyle.ACTION_BUTTON_HEIGHT,
                    StockReviewStyle.BUTTON_GAP,
                    WimGuiButtonSpecs.of(footerButton("Confirm Trades", StockReviewAction.confirmPurchase(),
                            pendingPurchases != null && !pendingPurchases.isEmpty() && tradeContext.canConfirm(),
                            StockReviewStyle.CONFIRM_BUTTON)),
                    footerButton("Go Back", StockReviewAction.goBack(), true, StockReviewStyle.CANCEL_BUTTON),
                    buttons);
            return;
        }
        WimGuiModalFooter.addLeftRowAndRightButton(
                root,
                StockReviewStyle.MODAL,
                StockReviewStyle.ACTION_BUTTON_HEIGHT,
                StockReviewStyle.BUTTON_GAP,
                WimGuiButtonSpecs.of(
                        footerButton("Review Trades", StockReviewAction.reviewPurchase(),
                                pendingPurchases != null && !pendingPurchases.isEmpty(), StockReviewStyle.CONFIRM_BUTTON),
                        bulkButton("Purchase All Until Sufficient", StockReviewAction.purchaseAllUntilSufficient(), true),
                        bulkButton("Sell All Until Sufficient", StockReviewAction.sellAllUntilSufficient(), true, StockReviewStyle.SELL_BUTTON),
                        buttonFactory.button(
                                StockReviewStyle.RESET_ALL_BUTTON_WIDTH,
                                "Reset All Trades",
                                StockReviewAction.resetAllTrades(),
                                pendingPurchases != null && !pendingPurchases.isEmpty(),
                                StockReviewStyle.ACTION_BACKGROUND)),
                footerButton("Cancel", StockReviewAction.close(), true, StockReviewStyle.CANCEL_BUTTON),
                buttons);
    }

    private WimGuiButtonSpec<StockReviewAction> footerButton(String label,
                                                             StockReviewAction action,
                                                             boolean enabled,
                                                             Color fill) {
        return buttonFactory.button(StockReviewStyle.FOOTER_BUTTON_WIDTH, label, action, enabled, fill);
    }

    private WimGuiButtonSpec<StockReviewAction> bulkButton(String label,
                                                           StockReviewAction action,
                                                           boolean enabled) {
        return bulkButton(label, action, enabled, StockReviewStyle.BUY_BUTTON);
    }

    private WimGuiButtonSpec<StockReviewAction> bulkButton(String label,
                                                           StockReviewAction action,
                                                           boolean enabled,
                                                           Color fill) {
        return buttonFactory.button(StockReviewStyle.BULK_BUTTON_WIDTH, label, action, enabled, fill);
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
                + " | Stock source: " + (snapshot.isGlobalMarketMode() ? "global" : "local")
                + " | Black market: " + onOff(snapshot.isIncludeBlackMarket())
                + " | Filters: " + state.getActiveFilterCount();
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
