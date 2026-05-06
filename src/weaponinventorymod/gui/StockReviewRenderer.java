package weaponinventorymod.gui;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CutStyle;
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
                + " | Sort: " + snapshot.getSortMode().getLabel()
                + " | Owned source: " + ownedSourceLabel(snapshot)
                + " | Black market: " + onOff(snapshot.isIncludeBlackMarket()), StockReviewStyle.SMALL_PAD, StockReviewStyle.MUTED,
                "Market:", "Mode:", "Sort:", "Owned source:", "Black market:");

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
        ButtonAPI sort = tooltip.addButton("Sort: " + snapshot.getSortMode().getLabel(), StockReviewAction.cycleSortMode(), Misc.getBasePlayerColor(),
                Misc.getDarkPlayerColor(), StockReviewStyle.WIDE_ACTION_BUTTON_WIDTH, StockReviewStyle.ACTION_BUTTON_HEIGHT, StockReviewStyle.SMALL_PAD);
        buttons.add(new StockReviewButtonBinding(sort, StockReviewAction.cycleSortMode()));
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
        ButtonAPI button = tooltip.addButton(label, StockReviewAction.toggle(category), color,
                StockReviewStyle.HEADING_BACKGROUND, Alignment.LMID, CutStyle.NONE,
                StockReviewStyle.CATEGORY_BUTTON_WIDTH, StockReviewStyle.CATEGORY_BUTTON_HEIGHT, StockReviewStyle.CATEGORY_GAP);
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
            addRecord(tooltip, record, color, state, buttons);
            shown++;
        }
    }

    private void addRecord(TooltipMakerAPI tooltip,
                           WeaponStockRecord record,
                           Color categoryColor,
                           StockReviewState state,
                           List<StockReviewButtonBinding> buttons) {
        boolean expanded = state.isWeaponExpanded(record.getWeaponId());
        String suffix = expanded ? " (-)" : " (+)";
        String label = "    " + record.getDisplayName() + " (" + record.getCountLabel() + ")" + suffix;
        ButtonAPI toggle = tooltip.addButton(label, StockReviewAction.toggleWeapon(record.getWeaponId()), categoryColor,
                StockReviewStyle.ROW_BACKGROUND, Alignment.LMID, CutStyle.NONE,
                StockReviewStyle.WEAPON_ROW_WIDTH, StockReviewStyle.WEAPON_ROW_HEIGHT, StockReviewStyle.SMALL_PAD);
        buttons.add(new StockReviewButtonBinding(toggle, StockReviewAction.toggleWeapon(record.getWeaponId())));

        ButtonAPI buyOne = addBuyButton(tooltip, "Buy 1", StockReviewAction.buyBest(record.getWeaponId(), 1), record.getBuyableCount() > 0, buttons);
        buyOne.getPosition().rightOfTop(toggle, StockReviewStyle.SMALL_PAD);
        ButtonAPI buyTen = addBuyButton(tooltip, "Buy 10", StockReviewAction.buyBest(record.getWeaponId(), 10), record.getBuyableCount() > 0, buttons);
        buyTen.getPosition().rightOfTop(buyOne, StockReviewStyle.SMALL_PAD);

        if (!expanded) {
            return;
        }

        addWeaponDataSection(tooltip, record, state, buttons);
        addSellersSection(tooltip, record, state, buttons);
    }

    private ButtonAPI addBuyButton(TooltipMakerAPI tooltip,
                                   String label,
                                   StockReviewAction action,
                                   boolean enabled,
                                   List<StockReviewButtonBinding> buttons) {
        ButtonAPI button = tooltip.addButton(label, action, Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(),
                StockReviewStyle.BUY_BUTTON_WIDTH, StockReviewStyle.ACTION_BUTTON_HEIGHT, 0f);
        button.setEnabled(enabled);
        buttons.add(new StockReviewButtonBinding(button, action));
        return button;
    }

    private void addWeaponDataSection(TooltipMakerAPI tooltip,
                                      WeaponStockRecord record,
                                      StockReviewState state,
                                      List<StockReviewButtonBinding> buttons) {
        boolean expanded = state.isWeaponDataExpanded(record.getWeaponId());
        ButtonAPI heading = tooltip.addButton("        Weapon data " + (expanded ? "(-)" : "(+)"),
                StockReviewAction.toggleWeaponSection(record.getWeaponId(), StockReviewSection.WEAPON_DATA),
                StockReviewStyle.MUTED, StockReviewStyle.ROW_BACKGROUND_DARK, Alignment.LMID, CutStyle.NONE,
                StockReviewStyle.SECTION_ROW_WIDTH, StockReviewStyle.SECTION_ROW_HEIGHT, StockReviewStyle.SMALL_PAD);
        buttons.add(new StockReviewButtonBinding(heading,
                StockReviewAction.toggleWeaponSection(record.getWeaponId(), StockReviewSection.WEAPON_DATA)));
        if (!expanded) {
            return;
        }
        tooltip.addPara("            Size: " + record.getSizeLabel(), 0f, StockReviewStyle.MUTED);
        tooltip.addPara("            Type: " + record.getTypeLabel(), 0f, StockReviewStyle.MUTED);
        tooltip.addPara("            Damage: " + record.getDamageLabel(), 0f, StockReviewStyle.MUTED);
        tooltip.addPara("            EMP: " + record.getEmpLabel(), 0f, StockReviewStyle.MUTED);
        tooltip.addPara("            Range: " + record.getRangeLabel(), 0f, StockReviewStyle.MUTED);
        tooltip.addPara("            Flux/Second: " + record.getFluxPerSecondLabel(), 0f, StockReviewStyle.MUTED);
        tooltip.addPara("            Flux/Damage: " + record.getFluxPerDamageLabel(), 0f, StockReviewStyle.MUTED);
    }

    private void addSellersSection(TooltipMakerAPI tooltip,
                                   WeaponStockRecord record,
                                   StockReviewState state,
                                   List<StockReviewButtonBinding> buttons) {
        boolean expanded = state.isSellersExpanded(record.getWeaponId());
        ButtonAPI heading = tooltip.addButton("        Sellers " + (expanded ? "(-)" : "(+)"),
                StockReviewAction.toggleWeaponSection(record.getWeaponId(), StockReviewSection.SELLERS),
                StockReviewStyle.MUTED, StockReviewStyle.ROW_BACKGROUND_DARK, Alignment.LMID, CutStyle.NONE,
                StockReviewStyle.SECTION_ROW_WIDTH, StockReviewStyle.SECTION_ROW_HEIGHT, StockReviewStyle.SMALL_PAD);
        buttons.add(new StockReviewButtonBinding(heading,
                StockReviewAction.toggleWeaponSection(record.getWeaponId(), StockReviewSection.SELLERS)));
        if (!expanded) {
            return;
        }
        for (SubmarketWeaponStock stock : record.getSubmarketStocks()) {
            String label = "            " + stock.getSubmarketName() + ": " + stock.getCount() + " @ " + stock.getUnitPrice() + "cr"
                    + (stock.isPurchasable() ? "" : " (locked)");
            tooltip.addPara(label, StockReviewStyle.SMALL_PAD, StockReviewStyle.MUTED);
            ButtonAPI buyOne = addBuyButton(tooltip, "Buy 1",
                    StockReviewAction.buyFromSubmarket(record.getWeaponId(), stock.getSubmarketId(), 1),
                    stock.isPurchasable() && stock.getCount() > 0, buttons);
            ButtonAPI buyTen = addBuyButton(tooltip, "Buy 10",
                    StockReviewAction.buyFromSubmarket(record.getWeaponId(), stock.getSubmarketId(), 10),
                    stock.isPurchasable() && stock.getCount() > 0, buttons);
            buyTen.getPosition().rightOfTop(buyOne, StockReviewStyle.SMALL_PAD);
        }
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
