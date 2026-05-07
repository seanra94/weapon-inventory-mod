package weaponinventorymod.gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

final class StockReviewColorDebugRows {
    private StockReviewColorDebugRows() {
    }

    static List<WimGuiListRow<StockReviewAction>> build(int targetIndex, Color draft, boolean persistent) {
        List<WimGuiListRow<StockReviewAction>> rows = new ArrayList<WimGuiListRow<StockReviewAction>>();
        List<WimGuiColorDebug.Target> targets = WimGuiColorDebug.targets();
        WimGuiColorDebug.Target target = WimGuiColorDebug.targetAt(targetIndex);
        Color color = draft == null ? WimGuiColorDebug.currentColor(target) : draft;
        String count = "[" + (Math.max(0, Math.min(targetIndex, targets.size() - 1)) + 1) + "/" + targets.size() + "]";

        rows.add(StockReviewListRow.form("Samples", WimGuiRowCell.of(
                WimGuiRowCell.info("Container", StockReviewStyle.DEBUG_SAMPLE_WIDTH, color, StockReviewStyle.TEXT),
                WimGuiRowCell.standardAction("Button", StockReviewStyle.DEBUG_SAMPLE_WIDTH, color,
                        StockReviewAction.debugNoop(), true),
                WimGuiRowCell.standardAction("Toggle", StockReviewStyle.DEBUG_SAMPLE_WIDTH, color,
                        StockReviewAction.debugNoop(), true))));
        rows.add(StockReviewListRow.form("Variable", WimGuiRowCell.of(
                WimGuiRowCell.standardAction(target.getLabel() + " " + count, StockReviewStyle.DEBUG_VALUE_WIDTH,
                        StockReviewStyle.ACTION_BACKGROUND, StockReviewAction.debugCycleTarget(1), true))));
        rows.add(StockReviewListRow.form("Mode", WimGuiRowCell.of(
                WimGuiRowCell.standardAction(persistent ? "Permanent" : "Temporary", StockReviewStyle.DEBUG_VALUE_WIDTH,
                        persistent ? StockReviewStyle.CONFIRM_BUTTON : StockReviewStyle.ACTION_BACKGROUND,
                        StockReviewAction.debugTogglePersistence(), true))));
        rows.add(StockReviewListRow.form("Preview", WimGuiRowCell.of(
                WimGuiRowCell.info("Color(" + color.getRed() + ", " + color.getGreen() + ", " + color.getBlue() + ")",
                        StockReviewStyle.DEBUG_VALUE_WIDTH, color, StockReviewStyle.TEXT))));
        rows.add(channelRow("Red: " + color.getRed(),
                StockReviewAction.debugAdjustRed(-10),
                StockReviewAction.debugAdjustRed(-1),
                StockReviewAction.debugAdjustRed(1),
                StockReviewAction.debugAdjustRed(10)));
        rows.add(channelRow("Green: " + color.getGreen(),
                StockReviewAction.debugAdjustGreen(-10),
                StockReviewAction.debugAdjustGreen(-1),
                StockReviewAction.debugAdjustGreen(1),
                StockReviewAction.debugAdjustGreen(10)));
        rows.add(channelRow("Blue: " + color.getBlue(),
                StockReviewAction.debugAdjustBlue(-10),
                StockReviewAction.debugAdjustBlue(-1),
                StockReviewAction.debugAdjustBlue(1),
                StockReviewAction.debugAdjustBlue(10)));
        return rows;
    }

    private static WimGuiListRow<StockReviewAction> channelRow(String label,
                                                               StockReviewAction minusTen,
                                                               StockReviewAction minusOne,
                                                               StockReviewAction plusOne,
                                                               StockReviewAction plusTen) {
        return StockReviewListRow.form(label, WimGuiRowCell.of(
                WimGuiRowCell.standardAction("-10", StockReviewStyle.DEBUG_DELTA_BUTTON_WIDTH,
                        StockReviewStyle.CANCEL_BUTTON, minusTen, true),
                WimGuiRowCell.standardAction("-1", StockReviewStyle.DEBUG_DELTA_BUTTON_WIDTH,
                        StockReviewStyle.CANCEL_BUTTON, minusOne, true),
                WimGuiRowCell.standardAction("+1", StockReviewStyle.DEBUG_DELTA_BUTTON_WIDTH,
                        StockReviewStyle.CONFIRM_BUTTON, plusOne, true),
                WimGuiRowCell.standardAction("+10", StockReviewStyle.DEBUG_DELTA_BUTTON_WIDTH,
                        StockReviewStyle.CONFIRM_BUTTON, plusTen, true)));
    }
}
