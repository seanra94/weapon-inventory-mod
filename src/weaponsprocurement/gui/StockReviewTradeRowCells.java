package weaponsprocurement.gui;

import com.fs.starfarer.api.ui.Alignment;
import weaponsprocurement.core.CreditFormat;

import java.awt.Color;
import java.util.List;

final class StockReviewTradeRowCells {
    private StockReviewTradeRowCells() {
    }

    static WimGuiRowCell<StockReviewAction> storage(int ownedCount, int planQuantity, float width) {
        return WimGuiRowCell.info(storageLabel(ownedCount, planQuantity),
                width, StockReviewStyle.CELL_BACKGROUND, StockReviewStyle.TEXT,
                Alignment.LMID, StockReviewTooltips.STORAGE);
    }

    static WimGuiRowCell<StockReviewAction> plan(int planQuantity, long transactionCost) {
        String quantity = cappedCount(Math.abs(planQuantity));
        String total = cappedCredits(transactionCost, 999999);
        String label = planQuantity > 0
                ? "Buying: " + quantity + " [" + total + "]"
                : planQuantity < 0 ? "Selling: " + quantity + " [" + total + "]" : "Buying: 0 [" + StockReviewFormat.credits(0) + "]";
        Color fill = planQuantity > 0
                ? StockReviewStyle.PLAN_POSITIVE
                : planQuantity < 0 ? StockReviewStyle.PLAN_NEGATIVE : StockReviewStyle.CELL_BACKGROUND;
        return WimGuiRowCell.info(label, StockReviewStyle.PLAN_CELL_WIDTH, fill, StockReviewStyle.TEXT,
                Alignment.LMID, StockReviewTooltips.PLAN);
    }

    static WimGuiRowCell<StockReviewAction> unitPrice(int unitPrice) {
        if (unitPrice == StockReviewQuoteBook.PRICE_UNAVAILABLE) {
            return WimGuiRowCell.info("Price: ?", StockReviewStyle.PRICE_CELL_WIDTH, StockReviewStyle.CELL_BACKGROUND,
                    StockReviewStyle.TEXT, Alignment.LMID, StockReviewTooltips.PRICE);
        }
        return WimGuiRowCell.info("Price: " + cappedCredits(unitPrice, 99999), StockReviewStyle.PRICE_CELL_WIDTH,
                StockReviewStyle.CELL_BACKGROUND, StockReviewStyle.TEXT, Alignment.LMID, StockReviewTooltips.PRICE);
    }

    static WimGuiRowCell<StockReviewAction> step(String sign,
                                                 int quantity,
                                                 Color fill,
                                                 StockReviewAction action,
                                                 String tooltip) {
        boolean enabled = quantity > 1;
        String label = enabled ? sign + quantity : sign + "10";
        return WimGuiRowCell.standardAction(label, StockReviewStyle.TRADE_STEP_BUTTON_WIDTH, fill, action, enabled, tooltip);
    }

    static void addWorstCaseTradeRow(List<WimGuiListRow<StockReviewAction>> rows) {
        List<WimGuiRowCell<StockReviewAction>> cells = WimGuiRowCell.of(
                WimGuiRowCell.info("Storage: 99+", StockReviewStyle.STOCK_CELL_WIDTH,
                        StockReviewStyle.CELL_BACKGROUND, StockReviewStyle.TEXT,
                        Alignment.LMID, StockReviewTooltips.STORAGE),
                WimGuiRowCell.info("Price: 99,999+" + CreditFormat.CREDIT_SYMBOL,
                        StockReviewStyle.PRICE_CELL_WIDTH, StockReviewStyle.CELL_BACKGROUND,
                        StockReviewStyle.TEXT, Alignment.LMID, StockReviewTooltips.PRICE),
                WimGuiRowCell.info("Selling: 99+ [999,999+" + CreditFormat.CREDIT_SYMBOL + "]",
                        StockReviewStyle.PLAN_CELL_WIDTH, StockReviewStyle.PLAN_NEGATIVE,
                        StockReviewStyle.TEXT, Alignment.LMID, StockReviewTooltips.PLAN),
                WimGuiRowCell.standardAction("-10", StockReviewStyle.TRADE_STEP_BUTTON_WIDTH,
                        StockReviewStyle.SELL_BUTTON, StockReviewAction.debugNoop(), true,
                        StockReviewTooltips.decreasePlan(10)),
                WimGuiRowCell.standardAction("-1", StockReviewStyle.TRADE_STEP_BUTTON_WIDTH,
                        StockReviewStyle.SELL_BUTTON, StockReviewAction.debugNoop(), true,
                        StockReviewTooltips.decreasePlan(1)),
                WimGuiRowCell.standardAction("+1", StockReviewStyle.TRADE_STEP_BUTTON_WIDTH,
                        StockReviewStyle.BUY_BUTTON, StockReviewAction.debugNoop(), true,
                        StockReviewTooltips.increasePlan(1)),
                WimGuiRowCell.standardAction("+10", StockReviewStyle.TRADE_STEP_BUTTON_WIDTH,
                        StockReviewStyle.BUY_BUTTON, StockReviewAction.debugNoop(), true,
                        StockReviewTooltips.increasePlan(10)),
                WimGuiRowCell.standardAction("Sufficient", StockReviewStyle.SUFFICIENT_BUTTON_WIDTH,
                        StockReviewStyle.SELL_BUTTON, StockReviewAction.debugNoop(), true,
                        "Adjust the queued trade quantity so that your stock of this item just meets the sufficiency threshold (99)."),
                WimGuiRowCell.standardAction("Reset", StockReviewStyle.RESET_BUTTON_WIDTH,
                        StockReviewStyle.ACTION_BACKGROUND, StockReviewAction.debugNoop(), true,
                        StockReviewTooltips.resetPlan()));
        rows.add(StockReviewListRow.item("Suzuki-Clapteryon Thermal Prokector... (+)",
                cells,
                StockReviewAction.debugNoop(),
                "Worst-case row-width test sample. It does not affect trades.",
                StockReviewStyle.SECTION_INDENT));
    }

    private static String storageLabel(int ownedCount, int planQuantity) {
        if (planQuantity == 0) {
            return "Storage: " + cappedCount(ownedCount);
        }
        return "Storage: " + cappedCount(ownedCount) + " [" + signedCappedCount(planQuantity) + "]";
    }

    private static String cappedCount(int value) {
        return value >= 99 ? "99+" : String.valueOf(Math.max(0, value));
    }

    private static String signedCappedCount(int value) {
        String sign = value > 0 ? "+" : value < 0 ? "-" : "";
        int absolute = Math.abs(value);
        return sign + (absolute >= 99 ? "99+" : String.valueOf(absolute));
    }

    private static String cappedCredits(long credits, int cap) {
        if (credits == StockReviewQuoteBook.PRICE_UNAVAILABLE) {
            return "?";
        }
        long absolute = Math.abs(credits);
        if (absolute >= cap) {
            return CreditFormat.grouped(cap) + "+" + CreditFormat.CREDIT_SYMBOL;
        }
        return StockReviewFormat.credits(absolute);
    }
}
