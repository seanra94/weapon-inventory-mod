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
    private final Color buttonFillColor;
    private final Color borderColor;
    private final float indent;
    private final StockReviewAction mainAction;
    private final StockReviewAction sellTenAction;
    private final StockReviewAction sellOneAction;
    private final StockReviewAction buyOneAction;
    private final StockReviewAction buyTenAction;
    private final StockReviewAction buyUntilAction;
    private final int currentCount;
    private final int forSaleCount;
    private final int planQuantity;
    private final int transactionCost;
    private final boolean sellEnabled;
    private final boolean buyEnabled;
    private final boolean buyUntilEnabled;
    private final boolean topGap;

    private StockReviewListRow(Kind kind,
                               String label,
                               Color textColor,
                               Color fillColor,
                               Color buttonFillColor,
                               Color borderColor,
                               float indent,
                               StockReviewAction mainAction,
                               StockReviewAction sellTenAction,
                               StockReviewAction sellOneAction,
                               StockReviewAction buyOneAction,
                               StockReviewAction buyTenAction,
                               StockReviewAction buyUntilAction,
                               int currentCount,
                               int forSaleCount,
                               int planQuantity,
                               int transactionCost,
                               boolean sellEnabled,
                               boolean buyEnabled,
                               boolean buyUntilEnabled,
                               boolean topGap) {
        this.kind = kind;
        this.label = label;
        this.textColor = textColor;
        this.fillColor = fillColor;
        this.buttonFillColor = buttonFillColor;
        this.borderColor = borderColor;
        this.indent = indent;
        this.mainAction = mainAction;
        this.sellTenAction = sellTenAction;
        this.sellOneAction = sellOneAction;
        this.buyOneAction = buyOneAction;
        this.buyTenAction = buyTenAction;
        this.buyUntilAction = buyUntilAction;
        this.currentCount = currentCount;
        this.forSaleCount = forSaleCount;
        this.planQuantity = planQuantity;
        this.transactionCost = transactionCost;
        this.sellEnabled = sellEnabled;
        this.buyEnabled = buyEnabled;
        this.buyUntilEnabled = buyUntilEnabled;
        this.topGap = topGap;
    }

    static StockReviewListRow category(String label, Color textColor, StockReviewAction action, boolean topGap) {
        return new StockReviewListRow(Kind.CATEGORY, label, StockReviewStyle.TEXT, textColor, textColor, null,
                0f, action, null, null, null, null, null, 0, 0, 0, 0, false, false, false, topGap);
    }

    static StockReviewListRow weapon(String label,
                                     Color textColor,
                                     StockReviewAction action,
                                     StockReviewAction sellTenAction,
                                     StockReviewAction sellOneAction,
                                     StockReviewAction buyOneAction,
                                     StockReviewAction buyTenAction,
                                     StockReviewAction buyUntilAction,
                                     int currentCount,
                                     int forSaleCount,
                                     int planQuantity,
                                     int transactionCost,
                                     boolean sellEnabled,
                                     boolean buyEnabled,
                                     boolean buyUntilEnabled) {
        return new StockReviewListRow(Kind.WEAPON, label, textColor, StockReviewStyle.ROW_BACKGROUND, StockReviewStyle.ROW_BACKGROUND, StockReviewStyle.ROW_BORDER,
                StockReviewStyle.WEAPON_INDENT, action, sellTenAction, sellOneAction, buyOneAction, buyTenAction, buyUntilAction,
                currentCount, forSaleCount, planQuantity, transactionCost, sellEnabled, buyEnabled, buyUntilEnabled, false);
    }

    static StockReviewListRow section(String label, StockReviewAction action) {
        return new StockReviewListRow(Kind.SECTION, label, StockReviewStyle.TEXT, StockReviewStyle.ROW_BACKGROUND, StockReviewStyle.HEADING_BACKGROUND, null,
                StockReviewStyle.SECTION_INDENT, action, null, null, null, null, null, 0, 0, 0, 0, false, false, false, false);
    }

    static StockReviewListRow detail(String label) {
        return new StockReviewListRow(Kind.DETAIL, label, StockReviewStyle.MUTED, null, null, null,
                StockReviewStyle.DETAIL_INDENT, null, null, null, null, null, null, 0, 0, 0, 0, false, false, false, false);
    }

    static StockReviewListRow seller(String label,
                                     boolean buyEnabled,
                                     StockReviewAction buyOneAction,
                                     StockReviewAction buyTenAction) {
        Color textColor = buyEnabled ? StockReviewStyle.MUTED : StockReviewStyle.DISABLED_TEXT;
        return new StockReviewListRow(Kind.SELLER, label, textColor, StockReviewStyle.ROW_BACKGROUND_DARK, StockReviewStyle.ROW_BACKGROUND_DARK, StockReviewStyle.ROW_BORDER,
                StockReviewStyle.SELLER_INDENT, null, null, null, buyOneAction, buyTenAction, null, 0, 0, 0, 0, false, buyEnabled, false, false);
    }

    static StockReviewListRow empty(String label) {
        return new StockReviewListRow(Kind.EMPTY, label, StockReviewStyle.MUTED, null, null, null,
                0f, null, null, null, null, null, null, 0, 0, 0, 0, false, false, false, false);
    }

    static StockReviewListRow scroll(String label, StockReviewAction action) {
        return new StockReviewListRow(Kind.SCROLL, label, StockReviewStyle.SCROLL, StockReviewStyle.HEADING_BACKGROUND, StockReviewStyle.HEADING_BACKGROUND, null,
                0f, action, null, null, null, null, null, 0, 0, 0, 0, false, false, false, false);
    }

    static StockReviewListRow review(String label) {
        return new StockReviewListRow(Kind.WEAPON, label, StockReviewStyle.TEXT, StockReviewStyle.ROW_BACKGROUND, StockReviewStyle.ROW_BACKGROUND, StockReviewStyle.ROW_BORDER,
                StockReviewStyle.WEAPON_INDENT, null, null, null, null, null, null, 0, 0, 0, 0, false, false, false, false);
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

    Color getButtonFillColor() {
        return buttonFillColor == null ? fillColor : buttonFillColor;
    }

    Color getBorderColor() {
        return borderColor;
    }

    float getIndent() {
        return indent;
    }

    StockReviewAction getMainAction() {
        return mainAction;
    }

    StockReviewAction getSellTenAction() {
        return sellTenAction;
    }

    StockReviewAction getSellOneAction() {
        return sellOneAction;
    }

    StockReviewAction getBuyOneAction() {
        return buyOneAction;
    }

    StockReviewAction getBuyTenAction() {
        return buyTenAction;
    }

    StockReviewAction getBuyUntilAction() {
        return buyUntilAction;
    }

    int getPlanQuantity() {
        return planQuantity;
    }

    int getCurrentCount() {
        return currentCount;
    }

    int getForSaleCount() {
        return forSaleCount;
    }

    int getTransactionCost() {
        return transactionCost;
    }

    boolean isSellEnabled() {
        return sellEnabled;
    }

    boolean isBuyEnabled() {
        return buyEnabled;
    }

    boolean isBuyUntilEnabled() {
        return buyUntilEnabled;
    }

    boolean hasTopGap() {
        return topGap;
    }
}
