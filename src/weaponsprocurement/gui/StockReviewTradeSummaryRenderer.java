package weaponsprocurement.gui;

import com.fs.starfarer.api.ui.CustomPanelAPI;

import java.awt.Color;

final class StockReviewTradeSummaryRenderer {
    private StockReviewTradeSummaryRenderer() {
    }

    static void render(CustomPanelAPI root,
                       StockReviewTradeContext tradeContext,
                       StockReviewState state,
                       boolean reviewMode) {
        long netCost = tradeContext.totalCost();
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

    private static void addSummaryRow(CustomPanelAPI root,
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

    private static String creditsAvailableLabel(float creditsAvailable, long netCost) {
        if (netCost == StockReviewQuoteBook.PRICE_UNAVAILABLE) {
            return StockReviewFormat.credits(Math.round(creditsAvailable)) + " [?]";
        }
        return StockReviewFormat.credits(Math.round(creditsAvailable)) + " [" + signedCredits(-netCost) + "]";
    }

    private static String cargoAvailableLabel(float cargoSpaceAvailable, float cargoDelta) {
        return formatCargo(cargoSpaceAvailable) + " [" + signedCargo(-cargoDelta) + "]";
    }

    private static String tariffsPaidLabel(StockReviewTradeContext tradeContext) {
        long markup = tradeContext.totalMarkupPaid();
        float multiplier = tradeContext.averageBuyMultiplier();
        if (markup <= 0) {
            return StockReviewFormat.credits(0) + " [avg 1.0x]";
        }
        return StockReviewFormat.credits(markup) + " [avg " + String.format(java.util.Locale.US, "%.1fx", multiplier) + "]";
    }

    private static String signedCredits(long delta) {
        return (delta >= 0 ? "+" : "-") + StockReviewFormat.credits(delta);
    }

    private static String signedCargo(float delta) {
        return (delta >= 0f ? "+" : "-") + formatCargo(Math.abs(delta));
    }

    private static Color creditDeltaFill(long netCost) {
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
}
