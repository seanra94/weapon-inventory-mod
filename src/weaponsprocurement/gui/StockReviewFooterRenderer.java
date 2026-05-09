package weaponsprocurement.gui;

import com.fs.starfarer.api.ui.CustomPanelAPI;

import java.awt.Color;
import java.util.List;

final class StockReviewFooterRenderer {
    private static final WimGuiSemanticButtonFactory<StockReviewAction> BUTTON_FACTORY =
            new WimGuiSemanticButtonFactory<StockReviewAction>(StockReviewStyle.ROW_BORDER);

    private StockReviewFooterRenderer() {
    }

    static void render(CustomPanelAPI root,
                       StockReviewTradeContext tradeContext,
                       List<StockReviewPendingTrade> pendingTrades,
                       boolean reviewMode,
                       boolean filterMode,
                       boolean colorDebugMode,
                       List<WimGuiButtonBinding<StockReviewAction>> buttons) {
        if (colorDebugMode) {
            renderColorDebugFooter(root, buttons);
            return;
        }
        if (filterMode) {
            renderFilterFooter(root, buttons);
            return;
        }
        if (reviewMode) {
            renderReviewFooter(root, tradeContext, pendingTrades, buttons);
            return;
        }
        renderTradeFooter(root, pendingTrades, buttons);
    }

    private static void renderColorDebugFooter(CustomPanelAPI root,
                                               List<WimGuiButtonBinding<StockReviewAction>> buttons) {
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
    }

    private static void renderFilterFooter(CustomPanelAPI root,
                                           List<WimGuiButtonBinding<StockReviewAction>> buttons) {
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
    }

    private static void renderReviewFooter(CustomPanelAPI root,
                                           StockReviewTradeContext tradeContext,
                                           List<StockReviewPendingTrade> pendingTrades,
                                           List<WimGuiButtonBinding<StockReviewAction>> buttons) {
        WimGuiModalFooter.addLeftRowAndRightButton(
                root,
                StockReviewStyle.REVIEW_MODAL,
                StockReviewStyle.ACTION_BUTTON_HEIGHT,
                StockReviewStyle.BUTTON_GAP,
                WimGuiButtonSpecs.of(footerButton("Confirm Trades", StockReviewAction.confirmPurchase(),
                        pendingTrades != null && !pendingTrades.isEmpty() && tradeContext.canConfirm(),
                        StockReviewStyle.CONFIRM_BUTTON,
                        "Execute the queued buys and sells.")),
                footerButton("Go Back", StockReviewAction.goBack(), true, StockReviewStyle.CANCEL_BUTTON,
                        "Return to the trade screen without executing trades."),
                buttons);
    }

    private static void renderTradeFooter(CustomPanelAPI root,
                                          List<StockReviewPendingTrade> pendingTrades,
                                          List<WimGuiButtonBinding<StockReviewAction>> buttons) {
        WimGuiModalFooter.addLeftButtonRow(
                root,
                StockReviewStyle.MODAL,
                StockReviewStyle.ACTION_BUTTON_HEIGHT,
                StockReviewStyle.BUTTON_GAP,
                WimGuiButtonSpecs.of(
                        footerButton("Review Trades", StockReviewAction.reviewPurchase(),
                                pendingTrades != null && !pendingTrades.isEmpty(), StockReviewStyle.CONFIRM_BUTTON,
                                "Review the queued trades before confirming them."),
                        bulkButton("Purchase All Until Sufficient", StockReviewAction.purchaseAllUntilSufficient(), true,
                                StockReviewStyle.BUY_BUTTON, StockReviewTooltips.purchaseAllUntilSufficient()),
                        bulkButton("Sell All Until Sufficient", StockReviewAction.sellAllUntilSufficient(), true, StockReviewStyle.SELL_BUTTON,
                                StockReviewTooltips.sellAllUntilSufficient()),
                        BUTTON_FACTORY.button(
                                StockReviewStyle.RESET_ALL_BUTTON_WIDTH,
                                "Reset All Trades",
                                StockReviewAction.resetAllTrades(),
                                pendingTrades != null && !pendingTrades.isEmpty(),
                                StockReviewStyle.ACTION_BACKGROUND,
                                "Clear every queued buy and sell.")),
                buttons);
    }

    private static WimGuiButtonSpec<StockReviewAction> footerButton(String label,
                                                                    StockReviewAction action,
                                                                    boolean enabled,
                                                                    Color fill,
                                                                    String tooltip) {
        return BUTTON_FACTORY.button(StockReviewStyle.FOOTER_BUTTON_WIDTH, label, action, enabled, fill, tooltip);
    }

    private static WimGuiButtonSpec<StockReviewAction> bulkButton(String label,
                                                                  StockReviewAction action,
                                                                  boolean enabled,
                                                                  Color fill,
                                                                  String tooltip) {
        return BUTTON_FACTORY.button(StockReviewStyle.BULK_BUTTON_WIDTH, label, action, enabled, fill, tooltip);
    }
}
