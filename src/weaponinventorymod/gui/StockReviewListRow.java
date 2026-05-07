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

    static WimGuiListRow<StockReviewAction> weapon(String label,
                                                   List<WimGuiRowCell<StockReviewAction>> cells,
                                                   StockReviewAction action) {
        return row(label, StockReviewStyle.TEXT, StockReviewStyle.ROW_BACKGROUND,
                StockReviewStyle.CELL_BACKGROUND, StockReviewStyle.ROW_BORDER,
                StockReviewStyle.WEAPON_INDENT, action, Alignment.LMID, cells, false);
    }

    static WimGuiListRow<StockReviewAction> section(String label, StockReviewAction action) {
        return row(label, StockReviewStyle.TEXT, StockReviewStyle.ROW_BACKGROUND,
                StockReviewStyle.HEADING_BACKGROUND, null, StockReviewStyle.SECTION_INDENT,
                action, Alignment.LMID, null, false);
    }

    static WimGuiListRow<StockReviewAction> detail(String label) {
        return row(label, StockReviewStyle.MUTED, null, null, null,
                StockReviewStyle.DETAIL_INDENT, null, Alignment.LMID, null, false);
    }

    static WimGuiListRow<StockReviewAction> labelText(String label, String value) {
        List<WimGuiRowCell<StockReviewAction>> cells = WimGuiRowCell.of(
                WimGuiRowCell.info(label, StockReviewStyle.LABEL_TEXT_CELL_WIDTH,
                        StockReviewStyle.CELL_BACKGROUND, StockReviewStyle.TEXT, Alignment.LMID),
                WimGuiRowCell.info(value, StockReviewStyle.LABEL_TEXT_CELL_WIDTH,
                        StockReviewStyle.CELL_BACKGROUND, StockReviewStyle.TEXT, Alignment.MID));
        return row("", StockReviewStyle.TEXT, StockReviewStyle.ROW_BACKGROUND,
                StockReviewStyle.ROW_BACKGROUND, StockReviewStyle.ROW_BORDER,
                0f, null, Alignment.LMID, cells, false, 0f);
    }

    static WimGuiListRow<StockReviewAction> form(String label,
                                                 List<WimGuiRowCell<StockReviewAction>> cells) {
        return row(label, StockReviewStyle.TEXT, StockReviewStyle.ROW_BACKGROUND,
                StockReviewStyle.ROW_BACKGROUND, StockReviewStyle.ROW_BORDER,
                0f, null, Alignment.LMID, cells, false);
    }

    static WimGuiListRow<StockReviewAction> seller(String label,
                                                   boolean buyOneEnabled,
                                                   boolean buyTenEnabled,
                                                   StockReviewAction buyOneAction,
                                                   StockReviewAction buyTenAction) {
        List<WimGuiRowCell<StockReviewAction>> cells = WimGuiRowCell.of(
                WimGuiRowCell.standardAction("+1", StockReviewStyle.SELLER_BUY_BUTTON_WIDTH, StockReviewStyle.BUY_BUTTON,
                        buyOneAction, buyOneEnabled),
                WimGuiRowCell.standardAction("+10", StockReviewStyle.SELLER_BUY_BUTTON_WIDTH, StockReviewStyle.BUY_BUTTON,
                        buyTenAction, buyTenEnabled));
        Color textColor = buyOneEnabled || buyTenEnabled ? StockReviewStyle.MUTED : StockReviewStyle.DISABLED_TEXT;
        return row(label, textColor, StockReviewStyle.ROW_BACKGROUND_DARK,
                StockReviewStyle.ROW_BACKGROUND_DARK, StockReviewStyle.ROW_BORDER,
                StockReviewStyle.SELLER_INDENT, null, Alignment.LMID, cells, false);
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

    static WimGuiListRow<StockReviewAction> reviewTable(String label,
                                                        List<WimGuiRowCell<StockReviewAction>> cells,
                                                        float indent) {
        return row(label, StockReviewStyle.TEXT, StockReviewStyle.ROW_BACKGROUND,
                StockReviewStyle.ROW_BACKGROUND, StockReviewStyle.ROW_BORDER,
                indent, null, Alignment.LMID, cells, false);
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
                                                        Float cellGapOverride) {
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
                cellGapOverride);
    }
}
