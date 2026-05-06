package weaponinventorymod.gui;

import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import weaponinventorymod.core.WeaponStockSnapshot;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

final class StockReviewRenderer {
    RenderResult render(CustomPanelAPI root,
                        WeaponStockSnapshot snapshot,
                        StockReviewState state,
                        List<StockReviewButtonBinding> buttons) {
        buttons.clear();
        renderHeader(root, snapshot);
        renderActionRow(root, snapshot, buttons);
        return renderList(root, snapshot, state, buttons);
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
        title.addTitle("Weapon Stock Review");
        header.addUIElement(title).inTL(StockReviewStyle.PAD, 2f);

        TooltipMakerAPI status = header.createUIElement(header.getPosition().getWidth() - 2f * StockReviewStyle.PAD, 28f, false);
        status.setParaFontDefault();
        status.setParaFontColor(StockReviewStyle.MUTED);
        status.addPara(statusLine(snapshot), 0f, StockReviewStyle.MUTED);
        header.addUIElement(status).inTL(StockReviewStyle.PAD, 28f);
    }

    private void renderActionRow(CustomPanelAPI root, WeaponStockSnapshot snapshot, List<StockReviewButtonBinding> buttons) {
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

    private RenderResult renderList(CustomPanelAPI root,
                                    WeaponStockSnapshot snapshot,
                                    StockReviewState state,
                                    List<StockReviewButtonBinding> buttons) {
        CustomPanelAPI listPanel = root.createCustomPanel(
                StockReviewStyle.LIST_WIDTH,
                StockReviewStyle.LIST_HEIGHT,
                new StockReviewPanelBoxPlugin(StockReviewStyle.PANEL_BACKGROUND, StockReviewStyle.PANEL_BORDER));
        root.addComponent(listPanel).inTL(StockReviewStyle.PAD, StockReviewStyle.LIST_TOP);

        List<StockReviewListRow> rows = StockReviewListModel.build(snapshot, state);
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

    private void renderRow(CustomPanelAPI parent,
                           StockReviewListRow row,
                           float y,
                           List<StockReviewButtonBinding> buttons) {
        float width = parent.getPosition().getWidth() - 2f * StockReviewStyle.SMALL_PAD;
        CustomPanelAPI rowPanel = parent.createCustomPanel(
                width,
                StockReviewStyle.ROW_HEIGHT,
                new StockReviewPanelBoxPlugin(row.getFillColor(), null));
        parent.addComponent(rowPanel).inTL(StockReviewStyle.SMALL_PAD, y);

        float buyBlockWidth = row.getBuyOneAction() == null ? 0f :
                2f * StockReviewStyle.BUY_BUTTON_WIDTH + StockReviewStyle.BUTTON_GAP + StockReviewStyle.BUTTON_GAP;
        float labelLeft = row.getIndent();
        float labelWidth = Math.max(80f, width - labelLeft - buyBlockWidth - StockReviewStyle.TEXT_LEFT_PAD);
        if (row.getMainAction() != null) {
            addInvisibleButton(rowPanel, labelLeft, 0f, labelWidth, row.getMainAction(), true, buttons);
        }
        addLabel(rowPanel, row.getLabel(), row.getTextColor(), labelLeft + StockReviewStyle.TEXT_LEFT_PAD, labelWidth);

        if (row.getBuyOneAction() != null) {
            float buttonX = width - 2f * StockReviewStyle.BUY_BUTTON_WIDTH - StockReviewStyle.BUTTON_GAP;
            addSmallButton(rowPanel, buttonX, 0f, "Buy 1", row.getBuyOneAction(), row.isBuyEnabled(), buttons);
            addSmallButton(rowPanel, buttonX + StockReviewStyle.BUY_BUTTON_WIDTH + StockReviewStyle.BUTTON_GAP, 0f,
                    "Buy 10", row.getBuyTenAction(), row.isBuyEnabled(), buttons);
        }
    }

    private float addActionButton(CustomPanelAPI parent,
                                  float x,
                                  float y,
                                  float width,
                                  String label,
                                  StockReviewAction action,
                                  boolean enabled,
                                  List<StockReviewButtonBinding> buttons) {
        addSmallButton(parent, x, y, label, action, enabled, buttons, width);
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
        Color fill = enabled ? StockReviewStyle.ACTION_BACKGROUND : StockReviewStyle.DISABLED_BACKGROUND;
        Color text = enabled ? StockReviewStyle.TEXT : StockReviewStyle.DISABLED_TEXT;
        CustomPanelAPI buttonPanel = parent.createCustomPanel(
                width,
                StockReviewStyle.ACTION_BUTTON_HEIGHT,
                new StockReviewPanelBoxPlugin(fill, null));
        parent.addComponent(buttonPanel).inTL(x, y);
        addInvisibleButton(buttonPanel, 0f, 0f, width, action, enabled, buttons);
        addCenteredLabel(buttonPanel, label, text, width);
    }

    private ButtonAPI addInvisibleButton(CustomPanelAPI parent,
                                         float x,
                                         float y,
                                         float width,
                                         StockReviewAction action,
                                         boolean enabled,
                                         List<StockReviewButtonBinding> buttons) {
        TooltipMakerAPI element = parent.createUIElement(width, StockReviewStyle.ACTION_BUTTON_HEIGHT, false);
        ButtonAPI button = element.addAreaCheckbox(
                "",
                action,
                StockReviewStyle.TEXT,
                StockReviewStyle.ACTION_BACKGROUND,
                StockReviewStyle.ACTION_HOVER,
                width,
                StockReviewStyle.ACTION_BUTTON_HEIGHT,
                0f);
        button.setEnabled(enabled);
        button.setQuickMode(true);
        parent.addUIElement(element).inTL(x, y);
        buttons.add(new StockReviewButtonBinding(button, action));
        return button;
    }

    private void addLabel(CustomPanelAPI parent, String text, Color color, float x, float width) {
        TooltipMakerAPI label = parent.createUIElement(width, StockReviewStyle.ROW_HEIGHT, false);
        label.setParaFontDefault();
        label.setParaFontColor(color);
        int maxChars = Math.max(8, (int) (width / 6.2f));
        label.addPara(StockReviewText.fit(text, maxChars), 0f, color);
        parent.addUIElement(label).inTL(x, StockReviewStyle.TEXT_TOP_PAD);
    }

    private void addCenteredLabel(CustomPanelAPI parent, String text, Color color, float width) {
        int maxChars = Math.max(4, (int) (width / 6.2f));
        String fitted = StockReviewText.fit(text, maxChars);
        float estimatedWidth = Math.min(width - 4f, fitted.length() * 6.2f);
        float x = Math.max(2f, (width - estimatedWidth) / 2f);
        TooltipMakerAPI label = parent.createUIElement(width - x, StockReviewStyle.ACTION_BUTTON_HEIGHT, false);
        label.setParaFontDefault();
        label.setParaFontColor(color);
        label.addPara(fitted, 0f, color);
        parent.addUIElement(label).inTL(x, StockReviewStyle.TEXT_TOP_PAD);
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
