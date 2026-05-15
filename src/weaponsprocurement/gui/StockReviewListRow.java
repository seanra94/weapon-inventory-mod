package weaponsprocurement.gui;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.awt.Color;
import java.util.List;

final class StockReviewListRow {
    private StockReviewListRow() {
    }

    static WimGuiListRow<StockReviewAction> category(String label,
                                                     Color textColor,
                                                     StockReviewAction action,
                                                     boolean topGap) {
        return category(label, textColor, action, topGap, null);
    }

    static WimGuiListRow<StockReviewAction> category(String label,
                                                     Color textColor,
                                                     StockReviewAction action,
                                                     boolean topGap,
                                                     String tooltip) {
        return category(label, textColor, action, topGap, tooltip, 0f);
    }

    static WimGuiListRow<StockReviewAction> categoryIndented(String label,
                                                             Color textColor,
                                                             StockReviewAction action,
                                                             boolean topGap,
                                                             String tooltip,
                                                             float indent) {
        return category(label, textColor, action, topGap, tooltip, indent);
    }

    private static WimGuiListRow<StockReviewAction> category(String label,
                                                            Color textColor,
                                                            StockReviewAction action,
                                                            boolean topGap,
                                                           String tooltip,
                                                           float indent) {
        return row(label, StockReviewStyle.TEXT, null, textColor, null,
                indent, action, Alignment.LMID, null, topGap, tooltip);
    }

    static WimGuiListRow<StockReviewAction> filterHeading(String label,
                                                          StockReviewAction action,
                                                          boolean topGap) {
        return filterHeading(label, action, topGap, null);
    }

    static WimGuiListRow<StockReviewAction> filterHeading(String label,
                                                          StockReviewAction action,
                                                          boolean topGap,
                                                          String tooltip) {
        return row(label, StockReviewStyle.TEXT, StockReviewStyle.HEADING_BACKGROUND,
                StockReviewStyle.HEADING_BACKGROUND, null, 0f, action, Alignment.LMID, null, topGap, tooltip);
    }

    static WimGuiListRow<StockReviewAction> nestedHeading(String label,
                                                          float indent,
                                                          float rightReserveWidth,
                                                          StockReviewAction action,
                                                          boolean topGap,
                                                          String tooltip) {
        return row(label, StockReviewStyle.TEXT, null,
                StockReviewStyle.HEADING_BACKGROUND, null, indent, action, Alignment.LMID,
                null, topGap, null, rightReserveWidth, tooltip);
    }

    static WimGuiListRow<StockReviewAction> filter(String label,
                                                   boolean active,
                                                   StockReviewAction action,
                                                   boolean topGap) {
        return filter(label, active, action, topGap, null);
    }

    static WimGuiListRow<StockReviewAction> filter(String label,
                                                   boolean active,
                                                   StockReviewAction action,
                                                   boolean topGap,
                                                   String tooltip) {
        Color fill = active ? StockReviewStyle.FILTER_ACTIVE : StockReviewStyle.ROW_BACKGROUND;
        return row(label, StockReviewStyle.TEXT, fill, fill, StockReviewStyle.ROW_BORDER,
                active ? 0f : StockReviewStyle.WEAPON_INDENT, action, Alignment.LMID, null, topGap, tooltip);
    }

    static WimGuiListRow<StockReviewAction> item(String label,
                                                 List<WimGuiRowCell<StockReviewAction>> cells,
                                                 StockReviewAction action) {
        return item(label, cells, action, null);
    }

    static WimGuiListRow<StockReviewAction> item(String label,
                                                 List<WimGuiRowCell<StockReviewAction>> cells,
                                                 StockReviewAction action,
                                                 String tooltip) {
        return item(label, cells, action, tooltip, StockReviewStyle.WEAPON_INDENT);
    }

    static WimGuiListRow<StockReviewAction> item(String label,
                                                 List<WimGuiRowCell<StockReviewAction>> cells,
                                                 StockReviewAction action,
                                                 String tooltip,
                                                 float indent) {
        return item(label, cells, action, tooltip, null, indent);
    }

    static WimGuiListRow<StockReviewAction> item(String label,
                                                 List<WimGuiRowCell<StockReviewAction>> cells,
                                                 StockReviewAction action,
                                                 String tooltip,
                                                 TooltipMakerAPI.TooltipCreator tooltipCreator,
                                                 float indent) {
        return item(label, cells, action, tooltip, tooltipCreator, indent, null);
    }

    static WimGuiListRow<StockReviewAction> item(String label,
                                                 List<WimGuiRowCell<StockReviewAction>> cells,
                                                 StockReviewAction action,
                                                 String tooltip,
                                                 TooltipMakerAPI.TooltipCreator tooltipCreator,
                                                 float indent,
                                                 StockReviewRowIcon icon) {
        return row(label, StockReviewStyle.TEXT, StockReviewStyle.ROW_BACKGROUND,
                StockReviewStyle.CELL_BACKGROUND, StockReviewStyle.ROW_BORDER,
                indent, action, Alignment.LMID, cells, false, tooltip, tooltipCreator, icon);
    }

    static WimGuiListRow<StockReviewAction> labelTextIndented(String label, String value, float indent) {
        return labelTextIndented(label, value, indent, false);
    }

    static WimGuiListRow<StockReviewAction> labelTextIndented(String label, String value, float indent, boolean topGap) {
        return labelTextIndented(label, value, indent, topGap, StockReviewStyle.TRADE_ROW_RIGHT_BLOCK_WIDTH);
    }

    static WimGuiListRow<StockReviewAction> labelTextIndented(String label,
                                                              String value,
                                                              float indent,
                                                              boolean topGap,
                                                              float rightReserveWidth) {
        return labelTextIndented(label, value, indent, topGap, rightReserveWidth, StockReviewStyle.LIST_WIDTH);
    }

    static WimGuiListRow<StockReviewAction> labelTextIndented(String label,
                                                              String value,
                                                              float indent,
                                                              boolean topGap,
                                                              float rightReserveWidth,
                                                              float listWidth) {
        float componentWidth = Math.max(40f,
                listWidth - indent - rightReserveWidth - 2f * StockReviewStyle.SMALL_PAD);
        return labelTextRow(label, value, indent, topGap, componentWidth, rightReserveWidth, StockReviewStyle.TEXT);
    }

    private static WimGuiListRow<StockReviewAction> labelTextRow(String label,
                                                                 String value,
                                                                 float indent,
                                                                 boolean topGap,
                                                                 float componentWidth,
                                                                 float rightReserveWidth,
                                                                 Color valueColor) {
        float labelWidth = componentWidth * 0.65f;
        float valueWidth = componentWidth - labelWidth;
        List<WimGuiRowCell<StockReviewAction>> cells = WimGuiRowCell.of(
                WimGuiRowCell.infoWithBorder(label, labelWidth,
                        null, StockReviewStyle.TEXT, Alignment.LMID, StockReviewStyle.ROW_BORDER),
                WimGuiRowCell.infoWithBorder(value, valueWidth,
                        StockReviewStyle.CELL_BACKGROUND, valueColor, Alignment.MID, StockReviewStyle.ROW_BORDER));
        return row("", StockReviewStyle.TEXT, null,
                null, null,
                indent, null, Alignment.LMID, cells, topGap, 0f, rightReserveWidth);
    }

    static WimGuiListRow<StockReviewAction> form(String label,
                                                 List<WimGuiRowCell<StockReviewAction>> cells) {
        return row(label, StockReviewStyle.TEXT, StockReviewStyle.ROW_BACKGROUND,
                StockReviewStyle.ROW_BACKGROUND, StockReviewStyle.ROW_BORDER,
                0f, null, Alignment.LMID, cells, false);
    }

    static WimGuiListRow<StockReviewAction> empty(String label) {
        return row(label, StockReviewStyle.MUTED, null, null, null,
                0f, null, Alignment.LMID, null, false);
    }

    static WimGuiListRow<StockReviewAction> scroll(String label, StockReviewAction action) {
        return row(label, StockReviewStyle.SCROLL, StockReviewStyle.HEADING_BACKGROUND,
                StockReviewStyle.HEADING_BACKGROUND, null, 0f, action, Alignment.MID, null, false,
                "Move the list by one visible page.");
    }

    static WimGuiListRow<StockReviewAction> review(String label, Color fillColor) {
        return row(label, StockReviewStyle.TEXT, fillColor, fillColor,
                StockReviewStyle.ROW_BORDER, StockReviewStyle.WEAPON_INDENT,
                null, Alignment.LMID, null, false);
    }

    static WimGuiListRow<StockReviewAction> review(String label) {
        return review(label, StockReviewStyle.ROW_BACKGROUND);
    }

    private static WimGuiListRow<StockReviewAction> row(String label,
                                                        Color textColor,
                                                        Color fillColor,
                                                        Color buttonFillColor,
                                                        Color borderColor,
                                                        float indent,
                                                        StockReviewAction action,
                                                        Alignment alignment,
                                                        List<WimGuiRowCell<StockReviewAction>> cells,
                                                        boolean topGap) {
        return new WimGuiListRow<StockReviewAction>(
                label,
                textColor,
                fillColor,
                buttonFillColor,
                borderColor,
                indent,
                action,
                alignment,
                cells,
                topGap,
                null,
                0f,
                null);
    }

    private static WimGuiListRow<StockReviewAction> row(String label,
                                                        Color textColor,
                                                        Color fillColor,
                                                        Color buttonFillColor,
                                                        Color borderColor,
                                                        float indent,
                                                        StockReviewAction action,
                                                        Alignment alignment,
                                                        List<WimGuiRowCell<StockReviewAction>> cells,
                                                        boolean topGap,
                                                        String tooltip) {
        return row(label, textColor, fillColor, buttonFillColor, borderColor, indent, action,
                alignment, cells, topGap, tooltip, null);
    }

    private static WimGuiListRow<StockReviewAction> row(String label,
                                                        Color textColor,
                                                        Color fillColor,
                                                        Color buttonFillColor,
                                                        Color borderColor,
                                                        float indent,
                                                        StockReviewAction action,
                                                        Alignment alignment,
                                                        List<WimGuiRowCell<StockReviewAction>> cells,
                                                        boolean topGap,
                                                        String tooltip,
                                                        TooltipMakerAPI.TooltipCreator tooltipCreator) {
        return row(label, textColor, fillColor, buttonFillColor, borderColor, indent,
                action, alignment, cells, topGap, tooltip, tooltipCreator, null);
    }

    private static WimGuiListRow<StockReviewAction> row(String label,
                                                        Color textColor,
                                                        Color fillColor,
                                                        Color buttonFillColor,
                                                        Color borderColor,
                                                        float indent,
                                                        StockReviewAction action,
                                                        Alignment alignment,
                                                        List<WimGuiRowCell<StockReviewAction>> cells,
                                                        boolean topGap,
                                                        String tooltip,
                                                        TooltipMakerAPI.TooltipCreator tooltipCreator,
                                                        StockReviewRowIcon icon) {
        return new WimGuiListRow<StockReviewAction>(
                label,
                textColor,
                fillColor,
                buttonFillColor,
                borderColor,
                indent,
                action,
                alignment,
                cells,
                topGap,
                null,
                0f,
                tooltip,
                tooltipCreator,
                icon);
    }

    private static WimGuiListRow<StockReviewAction> row(String label,
                                                        Color textColor,
                                                        Color fillColor,
                                                        Color buttonFillColor,
                                                        Color borderColor,
                                                        float indent,
                                                        StockReviewAction action,
                                                        Alignment alignment,
                                                        List<WimGuiRowCell<StockReviewAction>> cells,
                                                        boolean topGap,
                                                        Float cellGapOverride,
                                                        float rightReserveWidth) {
        return row(label, textColor, fillColor, buttonFillColor, borderColor, indent, action,
                alignment, cells, topGap, cellGapOverride, rightReserveWidth, null);
    }

    private static WimGuiListRow<StockReviewAction> row(String label,
                                                        Color textColor,
                                                        Color fillColor,
                                                        Color buttonFillColor,
                                                        Color borderColor,
                                                        float indent,
                                                        StockReviewAction action,
                                                        Alignment alignment,
                                                        List<WimGuiRowCell<StockReviewAction>> cells,
                                                        boolean topGap,
                                                        Float cellGapOverride,
                                                        float rightReserveWidth,
                                                        String tooltip) {
        return new WimGuiListRow<StockReviewAction>(
                label,
                textColor,
                fillColor,
                buttonFillColor,
                borderColor,
                indent,
                action,
                alignment,
                cells,
                topGap,
                cellGapOverride,
                rightReserveWidth,
                tooltip);
    }
}
