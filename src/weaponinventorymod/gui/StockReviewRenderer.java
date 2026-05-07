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
                            boolean colorDebugMode,
                            int colorDebugTargetIndex,
                            Color colorDebugDraft,
                            boolean colorDebugPersistent,
                            List<WimGuiButtonBinding<StockReviewAction>> buttons) {
        renderHeader(root, snapshot, reviewMode, colorDebugMode, colorDebugTargetIndex, colorDebugDraft);
        if (!reviewMode) {
            renderActionRow(root, snapshot, buttons);
        }
        StockReviewTradeContext tradeContext = new StockReviewTradeContext(snapshot, pendingPurchases);
        WimGuiListBounds result = colorDebugMode
                ? renderColorDebugList(root, colorDebugTargetIndex, colorDebugDraft, colorDebugPersistent, state, buttons)
                : reviewMode
                ? renderReviewList(root, snapshot, pendingPurchases, state, tradeContext, buttons)
                : renderStockList(root, snapshot, state, tradeContext, buttons);
        renderFooter(root, tradeContext, pendingPurchases, reviewMode, colorDebugMode, colorDebugPersistent, buttons);
        return result;
    }

    private void renderHeader(CustomPanelAPI root,
                              WeaponStockSnapshot snapshot,
                              boolean reviewMode,
                              boolean colorDebugMode,
                              int colorDebugTargetIndex,
                              Color colorDebugDraft) {
        String title = colorDebugMode ? "Debug Colors" : reviewMode ? "Review Trades" : "Make Trades";
        String status = colorDebugMode ? colorStatusLine(colorDebugTargetIndex, colorDebugDraft) : statusLine(snapshot);
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
                                 List<WimGuiButtonBinding<StockReviewAction>> buttons) {
        WimGuiModalActionRow.add(
                root,
                StockReviewStyle.MODAL,
                StockReviewStyle.HEADER_HEIGHT,
                StockReviewStyle.SMALL_PAD,
                StockReviewStyle.ACTION_BUTTON_HEIGHT,
                StockReviewStyle.BUTTON_GAP,
                WimGuiButtonSpecs.of(
                        buttonFactory.enabledButton(StockReviewStyle.SORT_BUTTON_WIDTH, "Sort: " + snapshot.getSortMode().getLabel(),
                                StockReviewAction.cycleSortMode(), StockReviewStyle.ACTION_BACKGROUND),
                        buttonFactory.enabledButton(StockReviewStyle.GLOBAL_MARKET_BUTTON_WIDTH, "Source: " + (snapshot.isGlobalMarketMode() ? "Global" : "Local"),
                                StockReviewAction.toggleGlobalMarket(), snapshot.isGlobalMarketMode() ? StockReviewStyle.SAVE_BUTTON : StockReviewStyle.ACTION_BACKGROUND),
                        buttonFactory.enabledButton(StockReviewStyle.BLACK_MARKET_BUTTON_WIDTH, "Black Market: " + onOff(snapshot.isIncludeBlackMarket()),
                                StockReviewAction.toggleBlackMarket(), StockReviewStyle.ACTION_BACKGROUND),
                        buttonFactory.enabledButton(StockReviewStyle.COLOR_BUTTON_WIDTH, "Colors",
                                StockReviewAction.openColorDebug(), StockReviewStyle.SAVE_BUTTON)),
                buttons);
    }

    private WimGuiListBounds renderStockList(CustomPanelAPI root,
                                             WeaponStockSnapshot snapshot,
                                             StockReviewState state,
                                             StockReviewTradeContext tradeContext,
                                             List<WimGuiButtonBinding<StockReviewAction>> buttons) {
        List<WimGuiListRow<StockReviewAction>> rows = StockReviewListModel.build(snapshot, state, tradeContext);
        return renderRows(root, rows, state, StockReviewStyle.LIST, buttons);
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

    private WimGuiListBounds renderColorDebugList(CustomPanelAPI root,
                                                  int targetIndex,
                                                  Color draft,
                                                  boolean persistent,
                                                  StockReviewState state,
                                                  List<WimGuiButtonBinding<StockReviewAction>> buttons) {
        List<WimGuiListRow<StockReviewAction>> rows = StockReviewColorDebugRows.build(targetIndex, draft, persistent);
        return renderRows(root, rows, state, StockReviewStyle.LIST, buttons);
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
                        bulkButton("Sell All Until Sufficient", StockReviewAction.sellAllUntilSufficient(), true),
                        buttonFactory.button(
                                StockReviewStyle.RESET_ALL_BUTTON_WIDTH,
                                "Reset All Trades",
                                StockReviewAction.resetAllTrades(),
                                pendingPurchases != null && !pendingPurchases.isEmpty(),
                                StockReviewStyle.BULK_BUTTON)),
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
        return buttonFactory.button(StockReviewStyle.BULK_BUTTON_WIDTH, label, action, enabled, StockReviewStyle.BULK_BUTTON);
    }

    @Override
    public WimGuiListRow<StockReviewAction> createScrollRow(String label, int scrollDelta) {
        return StockReviewListRow.scroll(label, StockReviewAction.scrollList(scrollDelta));
    }

    @Override
    public float extraGapBefore(WimGuiListRow<StockReviewAction> row) {
        return row != null && row.hasTopGap() ? StockReviewStyle.CATEGORY_TOP_GAP : 0f;
    }

    private static String statusLine(WeaponStockSnapshot snapshot) {
        return "Market: " + snapshot.getMarketName()
                + " | Sort: " + snapshot.getSortMode().getLabel()
                + " | Owned source: " + ownedSourceLabel(snapshot)
                + " | Stock source: " + (snapshot.isGlobalMarketMode() ? "global" : "local")
                + " | Black market: " + onOff(snapshot.isIncludeBlackMarket());
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
