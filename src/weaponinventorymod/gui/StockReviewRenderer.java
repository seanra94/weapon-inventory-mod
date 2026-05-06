package weaponinventorymod.gui;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import weaponinventorymod.core.StockCategory;
import weaponinventorymod.core.SubmarketWeaponStock;
import weaponinventorymod.core.WeaponStockRecord;
import weaponinventorymod.core.WeaponStockSnapshot;

import java.awt.Color;
import java.util.List;

final class StockReviewRenderer {
    void render(TooltipMakerAPI tooltip,
                WeaponStockSnapshot snapshot,
                StockReviewState state,
                List<StockReviewButtonBinding> buttons) {
        tooltip.setParaFontDefault();
        tooltip.setParaFontColor(StockReviewStyle.TEXT);
        tooltip.addTitle("Weapon Stock Review");
        tooltip.addPara("Market: " + snapshot.getMarketName()
                + " | Mode: " + snapshot.getDisplayMode().getLabel()
                + " | Owned source: " + ownedSourceLabel(snapshot)
                + " | Black market: " + onOff(snapshot.isIncludeBlackMarket()), StockReviewStyle.SMALL_PAD, StockReviewStyle.MUTED,
                "Market:", "Mode:", "Owned source:", "Black market:");

        addActionRow(tooltip, snapshot, buttons);
        addCategory(tooltip, snapshot, state, buttons, StockCategory.NO_STOCK, StockReviewStyle.NO_STOCK);
        addCategory(tooltip, snapshot, state, buttons, StockCategory.INSUFFICIENT, StockReviewStyle.INSUFFICIENT);
        addCategory(tooltip, snapshot, state, buttons, StockCategory.SUFFICIENT, StockReviewStyle.SUFFICIENT);

        if (snapshot.getTotalRecords() == 0) {
            tooltip.addPara("No owned or currently purchasable weapons were found for this market context.", StockReviewStyle.PAD, StockReviewStyle.MUTED);
        }
    }

    private void addActionRow(TooltipMakerAPI tooltip, WeaponStockSnapshot snapshot, List<StockReviewButtonBinding> buttons) {
        ButtonAPI refresh = tooltip.addButton("Refresh", StockReviewAction.refresh(), Misc.getBasePlayerColor(),
                Misc.getDarkPlayerColor(), StockReviewStyle.ACTION_BUTTON_WIDTH, StockReviewStyle.ACTION_BUTTON_HEIGHT, StockReviewStyle.PAD);
        buttons.add(new StockReviewButtonBinding(refresh, StockReviewAction.refresh()));
        ButtonAPI mode = tooltip.addButton("Mode: " + snapshot.getDisplayMode().getLabel(), StockReviewAction.cycleDisplayMode(), Misc.getBasePlayerColor(),
                Misc.getDarkPlayerColor(), StockReviewStyle.WIDE_ACTION_BUTTON_WIDTH, StockReviewStyle.ACTION_BUTTON_HEIGHT, StockReviewStyle.SMALL_PAD);
        buttons.add(new StockReviewButtonBinding(mode, StockReviewAction.cycleDisplayMode()));
        ButtonAPI storage = tooltip.addButton("Market Storage: " + onOff(snapshot.getOwnedSourcePolicy().name().contains("CURRENT_MARKET_STORAGE")),
                StockReviewAction.toggleCurrentMarketStorage(), Misc.getBasePlayerColor(),
                Misc.getDarkPlayerColor(), StockReviewStyle.WIDE_ACTION_BUTTON_WIDTH, StockReviewStyle.ACTION_BUTTON_HEIGHT, StockReviewStyle.SMALL_PAD);
        buttons.add(new StockReviewButtonBinding(storage, StockReviewAction.toggleCurrentMarketStorage()));
        ButtonAPI blackMarket = tooltip.addButton("Black Market: " + onOff(snapshot.isIncludeBlackMarket()), StockReviewAction.toggleBlackMarket(), Misc.getBasePlayerColor(),
                Misc.getDarkPlayerColor(), StockReviewStyle.WIDE_ACTION_BUTTON_WIDTH, StockReviewStyle.ACTION_BUTTON_HEIGHT, StockReviewStyle.SMALL_PAD);
        buttons.add(new StockReviewButtonBinding(blackMarket, StockReviewAction.toggleBlackMarket()));
        ButtonAPI close = tooltip.addButton("Close", StockReviewAction.close(), Misc.getBasePlayerColor(),
                Misc.getDarkPlayerColor(), StockReviewStyle.ACTION_BUTTON_WIDTH, StockReviewStyle.ACTION_BUTTON_HEIGHT, StockReviewStyle.SMALL_PAD);
        buttons.add(new StockReviewButtonBinding(close, StockReviewAction.close()));
    }

    private void addCategory(TooltipMakerAPI tooltip,
                             WeaponStockSnapshot snapshot,
                             StockReviewState state,
                             List<StockReviewButtonBinding> buttons,
                             StockCategory category,
                             Color color) {
        List<WeaponStockRecord> records = snapshot.getRecords(category);
        boolean expanded = state.isExpanded(category);
        String suffix = expanded ? " (-)" : " (+)";
        String label = category.getLabel() + " [" + records.size() + "]" + suffix;
        ButtonAPI button = tooltip.addAreaCheckbox(label, StockReviewAction.toggle(category), color, new Color(12, 12, 12), color,
                StockReviewStyle.CATEGORY_BUTTON_WIDTH, StockReviewStyle.CATEGORY_BUTTON_HEIGHT, StockReviewStyle.PAD);
        buttons.add(new StockReviewButtonBinding(button, StockReviewAction.toggle(category)));

        if (!expanded) {
            return;
        }
        int shown = 0;
        for (WeaponStockRecord record : records) {
            if (shown >= StockReviewStyle.MAX_VISIBLE_ROWS_PER_CATEGORY) {
                tooltip.addPara("... " + (records.size() - shown) + " more", StockReviewStyle.SMALL_PAD, StockReviewStyle.MUTED);
                break;
            }
            addRecord(tooltip, record, color);
            shown++;
        }
    }

    private void addRecord(TooltipMakerAPI tooltip, WeaponStockRecord record, Color categoryColor) {
        String row = "    " + record.getDisplayName() + " (" + record.getCountLabel() + ")";
        tooltip.addPara(row, StockReviewStyle.SMALL_PAD, categoryColor, record.getDisplayName(), record.getCountLabel());
        tooltip.addPara("        " + record.getDetailLine(), 0f, StockReviewStyle.MUTED);
        String sourceLine = sourceLine(record.getSubmarketStocks());
        if (!sourceLine.isEmpty()) {
            tooltip.addPara("        Sold by: " + sourceLine, 0f, StockReviewStyle.MUTED);
        }
    }

    private String sourceLine(List<SubmarketWeaponStock> stocks) {
        if (stocks.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < stocks.size(); i++) {
            if (i > 0) {
                result.append(", ");
            }
            SubmarketWeaponStock stock = stocks.get(i);
            result.append(stock.getSubmarketName()).append(": ").append(stock.getCount());
        }
        return result.toString();
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
}
