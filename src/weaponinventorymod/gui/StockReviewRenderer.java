package weaponinventorymod.gui;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import weaponinventorymod.core.WeaponStockSnapshot;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

final class StockReviewRenderer {
    RenderResult render(CustomPanelAPI root,
                        WeaponStockSnapshot snapshot,
                        StockReviewState state,
                        List<StockReviewPendingPurchase> pendingPurchases,
                        boolean reviewMode,
                        List<StockReviewButtonBinding> buttons) {
        renderHeader(root, snapshot);
        renderActionRow(root, snapshot, buttons);
        RenderResult result = reviewMode
                ? renderReviewList(root, snapshot, pendingPurchases, state, buttons)
                : renderStockList(root, snapshot, state, pendingPurchases, buttons);
        renderFooter(root, snapshot, pendingPurchases, reviewMode, buttons);
        return result;
    }

    private void renderHeader(CustomPanelAPI root, WeaponStockSnapshot snapshot) {
        CustomPanelAPI header = root.createCustomPanel(
                StockReviewStyle.WIDTH - 2f * StockReviewStyle.PAD,
                StockReviewStyle.HEADER_HEIGHT,
                new StockReviewPanelBoxPlugin(StockReviewStyle.PANEL_BACKGROUND, StockReviewStyle.PANEL_BORDER));
        root.addComponent(header).inTL(StockReviewStyle.PAD, StockReviewStyle.PAD);

        TooltipMakerAPI title = header.createUIElement(header.getPosition().getWidth() - 2f * StockReviewStyle.PAD, 22f, false);
        title.setParaFontDefault();
        title.setParaFontColor(StockReviewStyle.TEXT);
        title.addPara("Weapon Stock Review", 0f, StockReviewStyle.TEXT);
        header.addUIElement(title).inTL(StockReviewStyle.PAD, 2f);

        TooltipMakerAPI status = header.createUIElement(header.getPosition().getWidth() - 2f * StockReviewStyle.PAD, 28f, false);
        status.setParaFontDefault();
        status.setParaFontColor(StockReviewStyle.TEXT);
        status.addPara(statusLine(snapshot), 0f, StockReviewStyle.TEXT);
        header.addUIElement(status).inTL(StockReviewStyle.PAD, 28f);
    }

    private void renderActionRow(CustomPanelAPI root,
                                 WeaponStockSnapshot snapshot,
                                 List<StockReviewButtonBinding> buttons) {
        float y = StockReviewStyle.PAD + StockReviewStyle.HEADER_HEIGHT + StockReviewStyle.SMALL_PAD;
        float x = StockReviewStyle.PAD;
        x = addActionButton(root, x, y, StockReviewStyle.REFRESH_BUTTON_WIDTH, "Refresh", StockReviewAction.refresh(), true, buttons);
        x = addActionButton(root, x, y, StockReviewStyle.MODE_BUTTON_WIDTH, "Mode: " + snapshot.getDisplayMode().getLabel(),
                StockReviewAction.cycleDisplayMode(), true, buttons);
        x = addActionButton(root, x, y, StockReviewStyle.SORT_BUTTON_WIDTH, "Sort: " + snapshot.getSortMode().getLabel(),
                StockReviewAction.cycleSortMode(), true, buttons);
        x = addActionButton(root, x, y, StockReviewStyle.STORAGE_BUTTON_WIDTH, "Market Storage: " + onOff(snapshot.getOwnedSourcePolicy().name().contains("CURRENT_MARKET_STORAGE")),
                StockReviewAction.toggleCurrentMarketStorage(), true, buttons);
        x = addActionButton(root, x, y, StockReviewStyle.BLACK_MARKET_BUTTON_WIDTH, "Black Market: " + onOff(snapshot.isIncludeBlackMarket()),
                StockReviewAction.toggleBlackMarket(), true, buttons);
        addActionButton(root, x, y, StockReviewStyle.CLOSE_BUTTON_WIDTH, "Close", StockReviewAction.close(), true, buttons);
    }

    private RenderResult renderStockList(CustomPanelAPI root,
                                         WeaponStockSnapshot snapshot,
                                         StockReviewState state,
                                         List<StockReviewPendingPurchase> pendingPurchases,
                                         List<StockReviewButtonBinding> buttons) {
        List<StockReviewListRow> rows = StockReviewListModel.build(snapshot, state, pendingPurchases);
        return renderRows(root, rows, state, buttons);
    }

    private RenderResult renderReviewList(CustomPanelAPI root,
                                          WeaponStockSnapshot snapshot,
                                          List<StockReviewPendingPurchase> pendingPurchases,
                                          StockReviewState state,
                                          List<StockReviewButtonBinding> buttons) {
        List<StockReviewListRow> rows = buildReviewRows(snapshot, pendingPurchases);
        return renderRows(root, rows, state, buttons);
    }

    private RenderResult renderRows(CustomPanelAPI root,
                                    List<StockReviewListRow> rows,
                                    StockReviewState state,
                                    List<StockReviewButtonBinding> buttons) {
        CustomPanelAPI listPanel = root.createCustomPanel(
                StockReviewStyle.LIST_WIDTH,
                StockReviewStyle.LIST_HEIGHT,
                new StockReviewPanelBoxPlugin(StockReviewStyle.PANEL_BACKGROUND, StockReviewStyle.PANEL_BORDER));
        root.addComponent(listPanel).inTL(StockReviewStyle.PAD, StockReviewStyle.LIST_TOP);

        ScrollSlice slice = ScrollSlice.compute(rows, state.getListScrollOffset());
        state.setListScrollOffset(slice.offset);

        float y = StockReviewStyle.SMALL_PAD;
        if (slice.hasAbove) {
            renderRow(listPanel, StockReviewListRow.scroll("^     ^     ^     ^     ^",
                    StockReviewAction.scrollList(-StockReviewStyle.SCROLL_STEP)), y, buttons);
            y += StockReviewStyle.ROW_HEIGHT + StockReviewStyle.ROW_GAP;
        }
        for (int i = 0; i < slice.visibleRows.size(); i++) {
            StockReviewListRow row = slice.visibleRows.get(i);
            if (row.hasTopGap()) {
                y += StockReviewStyle.CATEGORY_TOP_GAP;
            }
            renderRow(listPanel, row, y, buttons);
            y += StockReviewStyle.ROW_HEIGHT + StockReviewStyle.ROW_GAP;
        }
        if (slice.hasBelow) {
            renderRow(listPanel, StockReviewListRow.scroll("v     v     v     v     v",
                    StockReviewAction.scrollList(StockReviewStyle.SCROLL_STEP)), y, buttons);
        }
        return new RenderResult(slice.maxOffset);
    }

    private List<StockReviewListRow> buildReviewRows(WeaponStockSnapshot snapshot,
                                                     List<StockReviewPendingPurchase> pendingPurchases) {
        List<StockReviewListRow> rows = new ArrayList<StockReviewListRow>();
        if (pendingPurchases == null || pendingPurchases.isEmpty()) {
            rows.add(StockReviewListRow.empty("No weapons queued for purchase."));
            return rows;
        }
        for (int i = 0; i < pendingPurchases.size(); i++) {
            StockReviewPendingPurchase purchase = pendingPurchases.get(i);
            int cost = StockReviewPurchasePreview.quoteCost(snapshot, purchase);
            int quantity = Math.abs(purchase.getQuantity());
            String verb = purchase.isSell() ? "Sell " : "Buy ";
            String costText = cost == StockReviewPurchasePreview.PRICE_UNAVAILABLE
                    ? "price unavailable"
                    : (cost < 0 ? "+" + (-cost) + "cr" : (cost == 0 ? "0cr" : "-" + cost + "cr"));
            rows.add(StockReviewListRow.review(verb + StockReviewPurchasePreview.displayName(snapshot, purchase.getWeaponId())
                    + " x" + quantity
                    + StockReviewPurchasePreview.sourceSuffix(snapshot, purchase)
                    + " - " + costText));
        }
        int netCost = StockReviewPurchasePreview.totalCost(snapshot, pendingPurchases);
        String netText = netCost == StockReviewPurchasePreview.PRICE_UNAVAILABLE
                ? "Total cost: price unavailable"
                : (netCost < 0 ? "Net credits gained: " + (-netCost) + "cr" : "Total cost: " + netCost + "cr");
        rows.add(StockReviewListRow.detail(netText));
        rows.add(StockReviewListRow.detail("Credits available: " + Math.round(StockReviewPurchasePreview.currentCredits()) + "cr"));
        return rows;
    }

    private void renderRow(CustomPanelAPI parent,
                           StockReviewListRow row,
                           float y,
                           List<StockReviewButtonBinding> buttons) {
        float width = parent.getPosition().getWidth() - 2f * StockReviewStyle.SMALL_PAD;
        CustomPanelAPI rowPanel = parent.createCustomPanel(
                width,
                StockReviewStyle.ROW_HEIGHT,
                new StockReviewPanelBoxPlugin(row.getFillColor(), row.getBorderColor()));
        parent.addComponent(rowPanel).inTL(StockReviewStyle.SMALL_PAD, y);

        float actionBlockWidth = actionBlockWidth(row);
        float labelLeft = row.getIndent();
        float labelWidth = Math.max(80f, width - labelLeft - actionBlockWidth - StockReviewStyle.TEXT_LEFT_PAD);
        if (row.getMainAction() != null) {
            Alignment alignment = StockReviewListRow.Kind.SCROLL.equals(row.getKind()) ? Alignment.MID : Alignment.LMID;
            addButton(rowPanel, labelLeft, 0f, labelWidth, row.getLabel(), row.getTextColor(),
                    row.getMainAction(), true, alignment, buttons, row.getButtonFillColor());
        } else {
            addLabel(rowPanel, row.getLabel(), row.getTextColor(), labelLeft + StockReviewStyle.TEXT_LEFT_PAD, labelWidth);
        }

        if (row.getSellOneAction() != null) {
            float x = width - actionBlockWidth;
            addTallyLabel(rowPanel, row.getTally(), x, StockReviewStyle.TALLY_WIDTH);
            x += StockReviewStyle.TALLY_WIDTH + StockReviewStyle.BUTTON_GAP;
            addSmallButton(rowPanel, x, 0f, "Sell 10", row.getSellTenAction(), row.isSellEnabled(), buttons,
                    StockReviewStyle.SELL_BUTTON_WIDTH, StockReviewStyle.SELL_BUTTON);
            x += StockReviewStyle.SELL_BUTTON_WIDTH + StockReviewStyle.BUTTON_GAP;
            addSmallButton(rowPanel, x, 0f, "Sell 1", row.getSellOneAction(), row.isSellEnabled(), buttons,
                    StockReviewStyle.SELL_BUTTON_WIDTH, StockReviewStyle.SELL_BUTTON);
            x += StockReviewStyle.SELL_BUTTON_WIDTH + StockReviewStyle.BUTTON_GAP;
            addSmallButton(rowPanel, x, 0f, "Buy 1", row.getBuyOneAction(), row.isBuyEnabled(), buttons,
                    StockReviewStyle.BUY_BUTTON_WIDTH, StockReviewStyle.BUY_BUTTON);
            x += StockReviewStyle.BUY_BUTTON_WIDTH + StockReviewStyle.BUTTON_GAP;
            addSmallButton(rowPanel, x, 0f, "Buy 10", row.getBuyTenAction(), row.isBuyEnabled(), buttons,
                    StockReviewStyle.BUY_BUTTON_WIDTH, StockReviewStyle.BUY_BUTTON);
            x += StockReviewStyle.BUY_BUTTON_WIDTH + StockReviewStyle.BUTTON_GAP;
            addSmallButton(rowPanel, x, 0f, "Buy Until Sufficient", row.getBuyUntilAction(), row.isBuyUntilEnabled(), buttons,
                    StockReviewStyle.BUY_UNTIL_BUTTON_WIDTH, StockReviewStyle.BUY_BUTTON);
        } else if (row.getBuyOneAction() != null) {
            float buttonX = width - 2f * StockReviewStyle.BUY_BUTTON_WIDTH - StockReviewStyle.BUTTON_GAP;
            addSmallButton(rowPanel, buttonX, 0f, "Buy 1", row.getBuyOneAction(), row.isBuyEnabled(), buttons,
                    StockReviewStyle.BUY_BUTTON_WIDTH, StockReviewStyle.BUY_BUTTON);
            addSmallButton(rowPanel, buttonX + StockReviewStyle.BUY_BUTTON_WIDTH + StockReviewStyle.BUTTON_GAP, 0f,
                    "Buy 10", row.getBuyTenAction(), row.isBuyEnabled(), buttons, StockReviewStyle.BUY_BUTTON_WIDTH, StockReviewStyle.BUY_BUTTON);
        }
    }

    private float actionBlockWidth(StockReviewListRow row) {
        if (row.getSellOneAction() != null) {
            return StockReviewStyle.TALLY_WIDTH
                    + 2f * StockReviewStyle.SELL_BUTTON_WIDTH
                    + 2f * StockReviewStyle.BUY_BUTTON_WIDTH
                    + StockReviewStyle.BUY_UNTIL_BUTTON_WIDTH
                    + 5f * StockReviewStyle.BUTTON_GAP;
        }
        if (row.getBuyOneAction() != null) {
            return 2f * StockReviewStyle.BUY_BUTTON_WIDTH + StockReviewStyle.BUTTON_GAP;
        }
        return 0f;
    }

    private float addActionButton(CustomPanelAPI parent,
                                  float x,
                                  float y,
                                  float width,
                                  String label,
                                  StockReviewAction action,
                                  boolean enabled,
                                  List<StockReviewButtonBinding> buttons) {
        Color fill = enabled ? StockReviewStyle.ACTION_BACKGROUND : StockReviewStyle.DISABLED_BACKGROUND;
        Color text = enabled ? StockReviewStyle.TEXT : StockReviewStyle.DISABLED_TEXT;
        addButton(parent, x, y, width, label, text, action, enabled, Alignment.MID, buttons, fill);
        return x + width + StockReviewStyle.BUTTON_GAP;
    }

    private void addSmallButton(CustomPanelAPI parent,
                                float x,
                                float y,
                                String label,
                                StockReviewAction action,
                                boolean enabled,
                                List<StockReviewButtonBinding> buttons) {
        addSmallButton(parent, x, y, label, action, enabled, buttons, StockReviewStyle.BUY_BUTTON_WIDTH);
    }

    private void addSmallButton(CustomPanelAPI parent,
                                float x,
                                float y,
                                String label,
                                StockReviewAction action,
                                boolean enabled,
                                List<StockReviewButtonBinding> buttons,
                                float width) {
        addSmallButton(parent, x, y, label, action, enabled, buttons, width, StockReviewStyle.BUY_BUTTON);
    }

    private void addSmallButton(CustomPanelAPI parent,
                                float x,
                                float y,
                                String label,
                                StockReviewAction action,
                                boolean enabled,
                                List<StockReviewButtonBinding> buttons,
                                float width,
                                Color enabledFill) {
        Color fill = enabled ? enabledFill : StockReviewStyle.DISABLED_BACKGROUND;
        Color text = enabled ? StockReviewStyle.TEXT : StockReviewStyle.DISABLED_TEXT;
        addButton(parent, x, y, width, label, text, action, enabled, Alignment.MID, buttons, fill);
    }

    private void addTallyLabel(CustomPanelAPI parent, int tally, float x, float width) {
        float labelWidth = 38f;
        addLabel(parent, "Tally:", StockReviewStyle.TEXT, x, labelWidth);
        String value = tally > 0 ? "+" + tally : String.valueOf(tally);
        Color color = tally > 0 ? StockReviewStyle.TALLY_POSITIVE : (tally < 0 ? StockReviewStyle.TALLY_NEGATIVE : StockReviewStyle.TALLY_ZERO);
        addLabel(parent, value, color, x + labelWidth, Math.max(8f, width - labelWidth));
    }

    private ButtonAPI addButton(CustomPanelAPI parent,
                                float x,
                                float y,
                                float width,
                                String label,
                                Color textColor,
                                StockReviewAction action,
                                boolean enabled,
                                Alignment alignment,
                                List<StockReviewButtonBinding> buttons) {
        return addButton(parent, x, y, width, label, textColor, action, enabled, alignment, buttons, StockReviewStyle.ACTION_BACKGROUND);
    }

    private ButtonAPI addButton(CustomPanelAPI parent,
                                float x,
                                float y,
                                float width,
                                String label,
                                Color textColor,
                                StockReviewAction action,
                                boolean enabled,
                                Alignment alignment,
                                List<StockReviewButtonBinding> buttons,
                                Color backgroundColor) {
        TooltipMakerAPI element = parent.createUIElement(width, StockReviewStyle.ACTION_BUTTON_HEIGHT, false);
        element.setButtonFontDefault();
        Color buttonBackground = enabled ? dimForButton(backgroundColor) : StockReviewStyle.DISABLED_DARK;
        CustomPanelAPI border = parent.createCustomPanel(width, StockReviewStyle.ACTION_BUTTON_HEIGHT,
                new StockReviewPanelBoxPlugin(null, StockReviewStyle.ROW_BORDER));
        parent.addComponent(border).inTL(x, y);
        ButtonAPI button = element.addButton(
                "",
                action,
                backgroundColor,
                buttonBackground,
                alignment,
                CutStyle.NONE,
                width,
                StockReviewStyle.ACTION_BUTTON_HEIGHT,
                0f);
        button.setEnabled(enabled);
        button.setQuickMode(true);
        parent.addUIElement(element).inTL(x, y);
        addButtonLabel(parent, label, textColor, x, y, width, alignment);
        if (enabled) {
            buttons.add(new StockReviewButtonBinding(button, action));
        }
        return button;
    }

    private void renderFooter(CustomPanelAPI root,
                              WeaponStockSnapshot snapshot,
                              List<StockReviewPendingPurchase> pendingPurchases,
                              boolean reviewMode,
                              List<StockReviewButtonBinding> buttons) {
        float y = StockReviewStyle.HEIGHT - StockReviewStyle.PAD - StockReviewStyle.ACTION_BUTTON_HEIGHT;
        if (reviewMode) {
            addFooterButton(root, StockReviewStyle.PAD, y, "Confirm Purchase", StockReviewAction.confirmPurchase(),
                    canConfirmReview(snapshot, pendingPurchases),
                    StockReviewStyle.CONFIRM_BUTTON, buttons);
            addFooterButton(root, StockReviewStyle.WIDTH - StockReviewStyle.PAD - StockReviewStyle.FOOTER_BUTTON_WIDTH, y, "Go Back",
                    StockReviewAction.goBack(), true, StockReviewStyle.CANCEL_BUTTON, buttons);
            return;
        }
        addFooterButton(root, StockReviewStyle.PAD, y, "Review Purchase", StockReviewAction.reviewPurchase(),
                pendingPurchases != null && !pendingPurchases.isEmpty(), StockReviewStyle.CONFIRM_BUTTON, buttons);
        addFooterButton(root, StockReviewStyle.WIDTH - StockReviewStyle.PAD - StockReviewStyle.FOOTER_BUTTON_WIDTH, y, "Cancel",
                StockReviewAction.close(), true, StockReviewStyle.CANCEL_BUTTON, buttons);
    }

    private void addFooterButton(CustomPanelAPI parent,
                                 float x,
                                 float y,
                                 String label,
                                 StockReviewAction action,
                                 boolean enabled,
                                 Color fill,
                                 List<StockReviewButtonBinding> buttons) {
        addButton(parent, x, y, StockReviewStyle.FOOTER_BUTTON_WIDTH, label,
                enabled ? StockReviewStyle.TEXT : StockReviewStyle.DISABLED_TEXT,
                action, enabled, Alignment.MID, buttons, enabled ? fill : StockReviewStyle.DISABLED_BACKGROUND);
    }

    private static boolean canConfirmReview(WeaponStockSnapshot snapshot, List<StockReviewPendingPurchase> pendingPurchases) {
        if (pendingPurchases == null || pendingPurchases.isEmpty()) {
            return false;
        }
        int totalCost = StockReviewPurchasePreview.totalCost(snapshot, pendingPurchases);
        return totalCost != StockReviewPurchasePreview.PRICE_UNAVAILABLE
                && totalCost <= StockReviewPurchasePreview.currentCredits();
    }

    private void addButtonLabel(CustomPanelAPI parent,
                                String text,
                                Color color,
                                float x,
                                float y,
                                float width,
                                Alignment alignment) {
        float labelX = x;
        float labelWidth = width;
        if (Alignment.LMID.equals(alignment)) {
            labelX += StockReviewStyle.TEXT_LEFT_PAD;
            labelWidth = Math.max(8f, width - StockReviewStyle.TEXT_LEFT_PAD);
        }
        TooltipMakerAPI label = parent.createUIElement(labelWidth, StockReviewStyle.ACTION_BUTTON_HEIGHT, false);
        label.setParaFontDefault();
        label.setParaFontColor(color);
        int maxChars = Math.max(4, (int) (labelWidth / 6.2f));
        LabelAPI line = label.addPara(StockReviewText.fit(text, maxChars), 0f, color);
        line.setAlignment(alignment);
        parent.addUIElement(label).inTL(labelX, y + StockReviewStyle.TEXT_TOP_PAD);
    }

    private void addLabel(CustomPanelAPI parent, String text, Color color, float x, float width) {
        TooltipMakerAPI label = parent.createUIElement(width, StockReviewStyle.ROW_HEIGHT, false);
        label.setParaFontDefault();
        label.setParaFontColor(color);
        int maxChars = Math.max(8, (int) (width / 6.2f));
        label.addPara(StockReviewText.fit(text, maxChars), 0f, color);
        parent.addUIElement(label).inTL(x, StockReviewStyle.TEXT_TOP_PAD);
    }

    private static Color dimForButton(Color color) {
        if (color == null) {
            return StockReviewStyle.DISABLED_DARK;
        }
        return new Color(
                Math.max(0, Math.round(color.getRed() * 0.55f)),
                Math.max(0, Math.round(color.getGreen() * 0.55f)),
                Math.max(0, Math.round(color.getBlue() * 0.55f)),
                color.getAlpha());
    }

    private static String statusLine(WeaponStockSnapshot snapshot) {
        return "Market: " + snapshot.getMarketName()
                + " | Mode: " + snapshot.getDisplayMode().getLabel()
                + " | Sort: " + snapshot.getSortMode().getLabel()
                + " | Owned source: " + ownedSourceLabel(snapshot)
                + " | Black market: " + onOff(snapshot.isIncludeBlackMarket());
    }

    private static String ownedSourceLabel(WeaponStockSnapshot snapshot) {
        if (snapshot.getOwnedSourcePolicy().name().contains("CURRENT_MARKET_STORAGE")) {
            return "fleet + current market storage";
        }
        return "fleet only";
    }

    private static String onOff(boolean enabled) {
        return enabled ? "On" : "Off";
    }

    static final class RenderResult {
        private final int maxScrollOffset;

        RenderResult(int maxScrollOffset) {
            this.maxScrollOffset = maxScrollOffset;
        }

        int getMaxScrollOffset() {
            return maxScrollOffset;
        }
    }

    private static final class ScrollSlice {
        final int offset;
        final List<StockReviewListRow> visibleRows;
        final boolean hasAbove;
        final boolean hasBelow;
        final int maxOffset;

        private ScrollSlice(int offset,
                            List<StockReviewListRow> visibleRows,
                            boolean hasAbove,
                            boolean hasBelow,
                            int maxOffset) {
            this.offset = offset;
            this.visibleRows = visibleRows;
            this.hasAbove = hasAbove;
            this.hasBelow = hasBelow;
            this.maxOffset = maxOffset;
        }

        static ScrollSlice compute(List<StockReviewListRow> rows, int requestedOffset) {
            int totalSlots = Math.max(1, (int) Math.floor((StockReviewStyle.LIST_HEIGHT - 2f * StockReviewStyle.SMALL_PAD + StockReviewStyle.ROW_GAP)
                    / (StockReviewStyle.ROW_HEIGHT + StockReviewStyle.ROW_GAP)));
            if (rows.size() <= totalSlots) {
                return new ScrollSlice(0, rows, false, false, 0);
            }

            int offset = Math.max(0, Math.min(requestedOffset, rows.size() - 1));
            int visibleSlots = totalSlots;
            boolean hasAbove = false;
            boolean hasBelow = false;
            for (int i = 0; i < 3; i++) {
                hasAbove = offset > 0;
                visibleSlots = totalSlots - (hasAbove ? 1 : 0);
                hasBelow = offset + visibleSlots < rows.size();
                if (hasBelow) {
                    visibleSlots -= 1;
                }
                visibleSlots = Math.max(1, visibleSlots);
                int maxOffsetForSlots = Math.max(0, rows.size() - visibleSlots);
                int clamped = Math.min(offset, maxOffsetForSlots);
                if (clamped == offset) {
                    break;
                }
                offset = clamped;
            }
            int maxOffset = Math.max(0, rows.size() - visibleSlots);
            offset = Math.min(offset, maxOffset);
            hasAbove = offset > 0;
            int end = Math.min(rows.size(), offset + visibleSlots);
            hasBelow = end < rows.size();
            return new ScrollSlice(offset, new ArrayList<StockReviewListRow>(rows.subList(offset, end)), hasAbove, hasBelow, maxOffset);
        }
    }
}
