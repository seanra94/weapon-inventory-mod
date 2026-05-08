package weaponinventorymod.gui;

import com.fs.starfarer.api.ui.Alignment;

import java.awt.Color;
import java.util.List;

final class StockReviewListRow {
    private StockReviewListRow() {
    }

    static WimGuiListRow<StockReviewAction> category(String label,
                                                     Color textColor,
                                                     StockReviewAction action,
                                                     boolean topGap) {
        return row(label, StockReviewStyle.TEXT, textColor, textColor, null,
                0f, action, Alignment.LMID, null, topGap);
    }

    static WimGuiListRow<StockReviewAction> filterHeading(String label,
                                                          StockReviewAction action,
                                                          boolean topGap) {
        return row(label, StockReviewStyle.TEXT, StockReviewStyle.HEADING_BACKGROUND,
                StockReviewStyle.HEADING_BACKGROUND, null, 0f, action, Alignment.LMID, null, topGap);
    }

    static WimGuiListRow<StockReviewAction> filter(String label,
                                                   boolean active,
                                                   StockReviewAction action,
                                                   boolean topGap) {
        Color fill = active ? StockReviewStyle.FILTER_ACTIVE : StockReviewStyle.ROW_BACKGROUND;
        return row(label, StockReviewStyle.TEXT, fill, fill, StockReviewStyle.ROW_BORDER,
                active ? 0f : StockReviewStyle.WEAPON_INDENT, action, Alignment.LMID, null, topGap);
    }

    static WimGuiListRow<StockReviewAction> weapon(String label,
                                                   List<WimGuiRowCell<StockReviewAction>> cells,
                                                   StockReviewAction action) {
        return row(label, StockReviewStyle.TEXT, StockReviewStyle.ROW_BACKGROUND,
                StockReviewStyle.CELL_BACKGROUND, StockReviewStyle.ROW_BORDER,
                StockReviewStyle.WEAPON_INDENT, action, Alignment.LMID, cells, false);
    }

    static WimGuiListRow<StockReviewAction> section(String label, StockReviewAction action) {
        return section(label, action, StockReviewStyle.TRADE_ROW_RIGHT_BLOCK_WIDTH);
    }

    static WimGuiListRow<StockReviewAction> reviewSection(String label, StockReviewAction action) {
        return section(label, action, StockReviewStyle.REVIEW_ROW_RIGHT_BLOCK_WIDTH);
    }

    static WimGuiListRow<StockReviewAction> section(String label, StockReviewAction action, float rightReserveWidth) {
        return row(label, StockReviewStyle.TEXT, StockReviewStyle.ROW_BACKGROUND,
                StockReviewStyle.HEADING_BACKGROUND, null, StockReviewStyle.SECTION_INDENT,
                action, Alignment.LMID, null, false, null, rightReserveWidth);
    }

    static WimGuiListRow<StockReviewAction> detail(String label) {
        return row(label, StockReviewStyle.MUTED, null, null, null,
                StockReviewStyle.DETAIL_INDENT, null, Alignment.LMID, null, false);
    }

    static WimGuiListRow<StockReviewAction> labelText(String label, String value) {
        return labelText(label, value, false);
    }

    static WimGuiListRow<StockReviewAction> labelText(String label, String value, boolean topGap) {
        return labelText(label, value, topGap, StockReviewStyle.TEXT);
    }

    static WimGuiListRow<StockReviewAction> labelText(String label, String value, boolean topGap, Color valueColor) {
        return labelTextRow(label, value, 0f, topGap,
                StockReviewStyle.REVIEW_LIST_WIDTH - 2f * StockReviewStyle.SMALL_PAD,
                0f,
                valueColor);
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
        float cellWidth = componentWidth / 2f;
        List<WimGuiRowCell<StockReviewAction>> cells = WimGuiRowCell.of(
                WimGuiRowCell.infoWithBorder(label, cellWidth,
                        null, StockReviewStyle.TEXT, Alignment.LMID, StockReviewStyle.ROW_BORDER),
                WimGuiRowCell.infoWithBorder(value, cellWidth,
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
                StockReviewStyle.HEADING_BACKGROUND, null, 0f, action, Alignment.MID, null, false);
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
                0f);
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
                rightReserveWidth);
    }
}
