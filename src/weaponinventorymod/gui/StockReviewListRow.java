package weaponinventorymod.gui;

import java.awt.Color;

final class StockReviewListRow {
    enum Kind {
        CATEGORY,
        WEAPON,
        SECTION,
        DETAIL,
        SELLER,
        EMPTY,
        SCROLL
    }

    private final Kind kind;
    private final String label;
    private final Color textColor;
    private final Color fillColor;
    private final float indent;
    private final StockReviewAction mainAction;
    private final StockReviewAction buyOneAction;
    private final StockReviewAction buyTenAction;
    private final boolean buyEnabled;
    private final boolean topGap;

    private StockReviewListRow(Kind kind,
                               String label,
                               Color textColor,
                               Color fillColor,
                               float indent,
                               StockReviewAction mainAction,
                               StockReviewAction buyOneAction,
                               StockReviewAction buyTenAction,
                               boolean buyEnabled,
                               boolean topGap) {
        this.kind = kind;
        this.label = label;
        this.textColor = textColor;
        this.fillColor = fillColor;
        this.indent = indent;
        this.mainAction = mainAction;
        this.buyOneAction = buyOneAction;
        this.buyTenAction = buyTenAction;
        this.buyEnabled = buyEnabled;
        this.topGap = topGap;
    }

    static StockReviewListRow category(String label, Color textColor, StockReviewAction action, boolean topGap) {
        return new StockReviewListRow(Kind.CATEGORY, label, textColor, StockReviewStyle.HEADING_BACKGROUND,
                0f, action, null, null, false, topGap);
    }

    static StockReviewListRow weapon(String label,
                                     Color textColor,
                                     StockReviewAction action,
                                     StockReviewAction buyOneAction,
                                     StockReviewAction buyTenAction,
                                     boolean buyEnabled) {
        return new StockReviewListRow(Kind.WEAPON, label, textColor, StockReviewStyle.ROW_BACKGROUND,
                StockReviewStyle.WEAPON_INDENT, action, buyOneAction, buyTenAction, buyEnabled, false);
    }

    static StockReviewListRow section(String label, StockReviewAction action) {
        return new StockReviewListRow(Kind.SECTION, label, StockReviewStyle.MUTED, StockReviewStyle.ROW_BACKGROUND_DARK,
                StockReviewStyle.SECTION_INDENT, action, null, null, false, false);
    }

    static StockReviewListRow detail(String label) {
        return new StockReviewListRow(Kind.DETAIL, label, StockReviewStyle.MUTED, null,
                StockReviewStyle.DETAIL_INDENT, null, null, null, false, false);
    }

    static StockReviewListRow seller(String label,
                                     boolean buyEnabled,
                                     StockReviewAction buyOneAction,
                                     StockReviewAction buyTenAction) {
        Color textColor = buyEnabled ? StockReviewStyle.MUTED : StockReviewStyle.DISABLED_TEXT;
        return new StockReviewListRow(Kind.SELLER, label, textColor, StockReviewStyle.ROW_BACKGROUND_DARK,
                StockReviewStyle.SELLER_INDENT, null, buyOneAction, buyTenAction, buyEnabled, false);
    }

    static StockReviewListRow empty(String label) {
        return new StockReviewListRow(Kind.EMPTY, label, StockReviewStyle.MUTED, null,
                0f, null, null, null, false, false);
    }

    static StockReviewListRow scroll(String label, StockReviewAction action) {
        return new StockReviewListRow(Kind.SCROLL, label, StockReviewStyle.SCROLL, StockReviewStyle.HEADING_BACKGROUND,
                0f, action, null, null, false, false);
    }

    Kind getKind() {
        return kind;
    }

    String getLabel() {
        return label;
    }

    Color getTextColor() {
        return textColor;
    }

    Color getFillColor() {
        return fillColor;
    }

    float getIndent() {
        return indent;
    }

    StockReviewAction getMainAction() {
        return mainAction;
    }

    StockReviewAction getBuyOneAction() {
        return buyOneAction;
    }

    StockReviewAction getBuyTenAction() {
        return buyTenAction;
    }

    boolean isBuyEnabled() {
        return buyEnabled;
    }

    boolean hasTopGap() {
        return topGap;
    }
}
