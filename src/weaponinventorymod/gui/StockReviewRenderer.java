package weaponinventorymod.gui;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import weaponinventorymod.core.OwnedSourcePolicy;
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
                            List<WimGuiButtonBinding<StockReviewAction>> buttons) {
        renderHeader(root, snapshot);
        renderActionRow(root, snapshot, buttons);
        StockReviewTradeContext tradeContext = new StockReviewTradeContext(snapshot, pendingPurchases);
        WimGuiListBounds result = reviewMode
                ? renderReviewList(root, snapshot, pendingPurchases, state, tradeContext, buttons)
                : renderStockList(root, snapshot, state, tradeContext, buttons);
        renderFooter(root, tradeContext, pendingPurchases, reviewMode, buttons);
        return result;
    }

    private void renderHeader(CustomPanelAPI root, WeaponStockSnapshot snapshot) {
        WimGuiModalHeader.addTitleStatusHeader(
                root,
                StockReviewStyle.MODAL,
                StockReviewStyle.HEADER_HEIGHT,
                "Weapon Stock Review",
                statusLine(snapshot),
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
                        buttonFactory.enabledButton(StockReviewStyle.MODE_BUTTON_WIDTH, "Mode: " + snapshot.getDisplayMode().getLabel(),
                                StockReviewAction.cycleDisplayMode(), StockReviewStyle.ACTION_BACKGROUND),
                        buttonFactory.enabledButton(StockReviewStyle.SORT_BUTTON_WIDTH, "Sort: " + snapshot.getSortMode().getLabel(),
                                StockReviewAction.cycleSortMode(), StockReviewStyle.ACTION_BACKGROUND),
                        buttonFactory.enabledButton(StockReviewStyle.STORAGE_BUTTON_WIDTH, "Storage: " + onOff(!OwnedSourcePolicy.FLEET_ONLY.equals(snapshot.getOwnedSourcePolicy())),
                                StockReviewAction.toggleCurrentMarketStorage(), StockReviewStyle.ACTION_BACKGROUND),
                        buttonFactory.enabledButton(StockReviewStyle.BLACK_MARKET_BUTTON_WIDTH, "Black Market: " + onOff(snapshot.isIncludeBlackMarket()),
                                StockReviewAction.toggleBlackMarket(), StockReviewStyle.ACTION_BACKGROUND),
                        buttonFactory.enabledButton(StockReviewStyle.CLOSE_BUTTON_WIDTH, "Close",
                                StockReviewAction.close(), StockReviewStyle.ACTION_BACKGROUND)),
                buttons);
    }

    private WimGuiListBounds renderStockList(CustomPanelAPI root,
                                             WeaponStockSnapshot snapshot,
                                             StockReviewState state,
                                             StockReviewTradeContext tradeContext,
                                             List<WimGuiButtonBinding<StockReviewAction>> buttons) {
        List<WimGuiListRow<StockReviewAction>> rows = StockReviewListModel.build(snapshot, state, tradeContext);
        return renderRows(root, rows, state, buttons);
    }

    private WimGuiListBounds renderReviewList(CustomPanelAPI root,
                                              WeaponStockSnapshot snapshot,
                                              List<StockReviewPendingPurchase> pendingPurchases,
                                              StockReviewState state,
                                              StockReviewTradeContext tradeContext,
                                              List<WimGuiButtonBinding<StockReviewAction>> buttons) {
        List<WimGuiListRow<StockReviewAction>> rows = StockReviewReviewListModel.build(snapshot, pendingPurchases, state, tradeContext);
        return renderRows(root, rows, state, buttons);
    }

    private WimGuiListBounds renderRows(CustomPanelAPI root,
                                        List<WimGuiListRow<StockReviewAction>> rows,
                                        StockReviewState state,
                                        List<WimGuiButtonBinding<StockReviewAction>> buttons) {
        return WimGuiModalListRenderer.renderAndStoreOffset(
                root,
                rows,
                state,
                StockReviewStyle.LIST,
                this,
                this,
                buttons);
    }

    private void renderFooter(CustomPanelAPI root,
                              StockReviewTradeContext tradeContext,
                              List<StockReviewPendingPurchase> pendingPurchases,
                              boolean reviewMode,
                              List<WimGuiButtonBinding<StockReviewAction>> buttons) {
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
                + " | Mode: " + snapshot.getDisplayMode().getLabel()
                + " | Sort: " + snapshot.getSortMode().getLabel()
                + " | Owned source: " + ownedSourceLabel(snapshot)
                + " | Black market: " + onOff(snapshot.isIncludeBlackMarket());
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
