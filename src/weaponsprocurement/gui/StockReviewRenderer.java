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
            renderTradeSummary(root, tradeContext, state, reviewMode);
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
                                              List<StockReviewPendingPurchase> pendingPurchases,
                                              StockReviewState state,
                                              StockReviewTradeContext tradeContext,
                                              List<WimGuiButtonBinding<StockReviewAction>> buttons) {
        List<WimGuiListRow<StockReviewAction>> rows = StockReviewReviewListModel.build(snapshot, pendingPurchases, state, tradeContext);
        return renderRows(root, rows, state, StockReviewStyle.REVIEW_LIST, buttons);
    }

    private void renderTradeSummary(CustomPanelAPI root,
                                    StockReviewTradeContext tradeContext,
                                    StockReviewState state,
                                    boolean reviewMode) {
        int netCost = tradeContext.totalCost();
        float cargoDelta = tradeContext.totalCargoSpaceDelta();
        float width = reviewMode ? StockReviewStyle.REVIEW_LIST_WIDTH : StockReviewStyle.LIST_WIDTH;
        float rowY = StockReviewStyle.SUMMARY_TOP;
        String warning = state == null ? "None" : state.getTradeWarning();
        addSummaryRow(
                root,
                width,
                rowY,
                "Warning",
                warning,
                "None".equals(warning) ? StockReviewStyle.CELL_BACKGROUND : StockReviewStyle.PRESET_SCOPE_BUTTON,
                "Most recent trade warning for credits or cargo capacity.");
        rowY += StockReviewStyle.ROW_HEIGHT + StockReviewStyle.SUMMARY_ROW_GAP;
        addSummaryRow(
                root,
                width,
                rowY,
                "Tariffs Paid",
                tariffsPaidLabel(tradeContext),
                tradeContext.totalMarkupPaid() > 0 ? StockReviewStyle.CANCEL_BUTTON : StockReviewStyle.CELL_BACKGROUND,
                StockReviewTooltips.tariffs());
        rowY += StockReviewStyle.ROW_HEIGHT + StockReviewStyle.SUMMARY_ROW_GAP;
        addSummaryRow(
                root,
                width,
                rowY,
                "Credits Available",
                creditsAvailableLabel(tradeContext.credits(), netCost),
                creditDeltaFill(netCost),
                "Current credits plus the signed change from queued trades.");
        rowY += StockReviewStyle.ROW_HEIGHT + StockReviewStyle.SUMMARY_ROW_GAP;
        addSummaryRow(
                root,
                width,
                rowY,
                "Cargo Space Available",
                cargoAvailableLabel(tradeContext.cargoSpaceLeft(), cargoDelta),
                cargoDeltaFill(cargoDelta),
                "Current cargo space plus the signed cargo change from queued trades.");
    }

    private void addSummaryRow(CustomPanelAPI root,
                               float width,
                               float y,
                               String label,
                               String value,
                               Color valueFill,
                               String tooltip) {
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
                StockReviewStyle.TEXT,
                tooltip);
    }

    private static String formatCargo(float value) {
        float rounded = Math.round(value);
        if (Math.abs(value - rounded) < 0.05f) {
            return Integer.toString(Math.round(rounded));
        }
        return String.format(java.util.Locale.US, "%.1f", value);
    }

    private static String creditsAvailableLabel(float creditsAvailable, int netCost) {
        if (netCost == StockReviewQuoteBook.PRICE_UNAVAILABLE) {
            return StockReviewFormat.credits(Math.round(creditsAvailable)) + " [?]";
        }
        return StockReviewFormat.credits(Math.round(creditsAvailable)) + " [" + signedCredits(-netCost) + "]";
    }

    private static String cargoAvailableLabel(float cargoSpaceAvailable, float cargoDelta) {
        return formatCargo(cargoSpaceAvailable) + " [" + signedCargo(-cargoDelta) + "]";
    }

    private static String tariffsPaidLabel(StockReviewTradeContext tradeContext) {
        int markup = tradeContext.totalMarkupPaid();
        float multiplier = tradeContext.averageBuyMultiplier();
        if (markup <= 0) {
            return StockReviewFormat.credits(0) + " [avg 1.0x]";
        }
        return StockReviewFormat.credits(markup) + " [avg " + String.format(java.util.Locale.US, "%.1fx", multiplier) + "]";
    }

    private static String signedCredits(int delta) {
        return (delta >= 0 ? "+" : "-") + StockReviewFormat.credits(delta);
    }

    private static String signedCargo(float delta) {
        return (delta >= 0f ? "+" : "-") + formatCargo(Math.abs(delta));
    }

    private static Color creditDeltaFill(int netCost) {
        if (netCost == StockReviewQuoteBook.PRICE_UNAVAILABLE) {
            return StockReviewStyle.CANCEL_BUTTON;
        }
        if (netCost > 0) {
            return StockReviewStyle.CANCEL_BUTTON;
        }
        if (netCost < 0) {
            return StockReviewStyle.CONFIRM_BUTTON;
        }
        return StockReviewStyle.CELL_BACKGROUND;
    }

    private static Color cargoDeltaFill(float cargoDelta) {
        if (cargoDelta > 0.01f) {
            return StockReviewStyle.CANCEL_BUTTON;
        }
        if (cargoDelta < -0.01f) {
            return StockReviewStyle.CONFIRM_BUTTON;
        }
        return StockReviewStyle.CELL_BACKGROUND;
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
                            footerButton("Confirm", StockReviewAction.debugConfirm(), true, StockReviewStyle.CONFIRM_BUTTON,
                                    "Apply the color and return to the trade screen."),
                        footerButton("Apply", StockReviewAction.debugApply(), true, StockReviewStyle.SAVE_BUTTON,
                                "Apply the color without closing the debug menu."),
                            footerButton("Restore", StockReviewAction.debugRestore(), true, StockReviewStyle.LOAD_BUTTON,
                                    "Restore the selected color to its default value.")),
                    footerButton("Cancel", StockReviewAction.goBack(), true, StockReviewStyle.CANCEL_BUTTON,
                            "Return without applying additional changes."),
                    buttons);
            return;
        }
        if (filterMode) {
            WimGuiModalFooter.addLeftRowAndRightButton(
                    root,
                    StockReviewStyle.MODAL,
                    StockReviewStyle.ACTION_BUTTON_HEIGHT,
                    StockReviewStyle.BUTTON_GAP,
                    WimGuiButtonSpecs.of(footerButton("Confirm", StockReviewAction.goBack(), true, StockReviewStyle.CONFIRM_BUTTON,
                                    "Return to the trade screen with the current filters."),
                            footerButton("Reset Filters", StockReviewAction.resetFilters(),
                                    true, StockReviewStyle.LOAD_BUTTON,
                                    "Clear every active filter.")),
                    footerButton("Cancel", StockReviewAction.goBack(), true, StockReviewStyle.CANCEL_BUTTON,
                            "Return to the trade screen."),
                    buttons);
            return;
        }
        if (reviewMode) {
            WimGuiModalFooter.addLeftRowAndRightButton(
                    root,
                    StockReviewStyle.REVIEW_MODAL,
                    StockReviewStyle.ACTION_BUTTON_HEIGHT,
                    StockReviewStyle.BUTTON_GAP,
                    WimGuiButtonSpecs.of(footerButton("Confirm Trades", StockReviewAction.confirmPurchase(),
                            pendingPurchases != null && !pendingPurchases.isEmpty() && tradeContext.canConfirm(),
                            StockReviewStyle.CONFIRM_BUTTON,
                            "Execute the queued buys and sells.")),
                    footerButton("Go Back", StockReviewAction.goBack(), true, StockReviewStyle.CANCEL_BUTTON,
                            "Return to the trade screen without executing trades."),
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
                                pendingPurchases != null && !pendingPurchases.isEmpty(), StockReviewStyle.CONFIRM_BUTTON,
                                "Review the queued trades before confirming them."),
                        bulkButton("Purchase All Until Sufficient", StockReviewAction.purchaseAllUntilSufficient(), true,
                                StockReviewStyle.BUY_BUTTON, StockReviewTooltips.purchaseAllUntilSufficient()),
                        bulkButton("Sell All Until Sufficient", StockReviewAction.sellAllUntilSufficient(), true, StockReviewStyle.SELL_BUTTON,
                                StockReviewTooltips.sellAllUntilSufficient()),
                        buttonFactory.button(
                                StockReviewStyle.RESET_ALL_BUTTON_WIDTH,
                                "Reset All Trades",
                                StockReviewAction.resetAllTrades(),
                                pendingPurchases != null && !pendingPurchases.isEmpty(),
                                StockReviewStyle.ACTION_BACKGROUND,
                                "Clear every queued buy and sell.")),
                footerButton("Cancel", StockReviewAction.close(), true, StockReviewStyle.CANCEL_BUTTON,
                        "Close Weapon Stock Review."),
                buttons);
    }

    private WimGuiButtonSpec<StockReviewAction> footerButton(String label,
                                                             StockReviewAction action,
                                                             boolean enabled,
                                                             Color fill) {
        return footerButton(label, action, enabled, fill, null);
    }

    private WimGuiButtonSpec<StockReviewAction> footerButton(String label,
                                                             StockReviewAction action,
                                                             boolean enabled,
                                                             Color fill,
                                                             String tooltip) {
        return buttonFactory.button(StockReviewStyle.FOOTER_BUTTON_WIDTH, label, action, enabled, fill, tooltip);
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
        return bulkButton(label, action, enabled, fill, null);
    }

    private WimGuiButtonSpec<StockReviewAction> bulkButton(String label,
                                                           StockReviewAction action,
                                                           boolean enabled,
                                                           Color fill,
                                                           String tooltip) {
        return buttonFactory.button(StockReviewStyle.BULK_BUTTON_WIDTH, label, action, enabled, fill, tooltip);
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
