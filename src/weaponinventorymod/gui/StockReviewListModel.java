package weaponinventorymod.gui;

import weaponinventorymod.core.StockCategory;
import weaponinventorymod.core.SubmarketWeaponStock;
import weaponinventorymod.core.WeaponStockRecord;
import weaponinventorymod.core.WeaponStockSnapshot;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

final class StockReviewListModel {
    private StockReviewListModel() {
    }

    static List<StockReviewListRow> build(WeaponStockSnapshot snapshot, StockReviewState state) {
        List<StockReviewListRow> rows = new ArrayList<StockReviewListRow>();
        int displayed = 0;
        displayed += addCategory(rows, snapshot, state, StockCategory.NO_STOCK, StockReviewStyle.NO_STOCK, false);
        displayed += addCategory(rows, snapshot, state, StockCategory.INSUFFICIENT, StockReviewStyle.INSUFFICIENT, true);
        displayed += addCategory(rows, snapshot, state, StockCategory.SUFFICIENT, StockReviewStyle.SUFFICIENT, true);
        if (displayed == 0) {
            rows.add(StockReviewListRow.empty("No currently buyable weapons were found at this market."));
        }
        return rows;
    }

    private static int addCategory(List<StockReviewListRow> rows,
                                   WeaponStockSnapshot snapshot,
                                   StockReviewState state,
                                   StockCategory category,
                                   Color color,
                                   boolean topGap) {
        List<WeaponStockRecord> records = buyableRecords(snapshot.getRecords(category));
        boolean expanded = state.isExpanded(category);
        String label = category.getLabel() + " [" + records.size() + "] " + (expanded ? "(-)" : "(+)");
        rows.add(StockReviewListRow.category(label, color, StockReviewAction.toggle(category), topGap));
        if (!expanded) {
            return records.size();
        }
        for (int i = 0; i < records.size(); i++) {
            addWeapon(rows, records.get(i), color, state);
        }
        return records.size();
    }

    private static List<WeaponStockRecord> buyableRecords(List<WeaponStockRecord> records) {
        List<WeaponStockRecord> result = new ArrayList<WeaponStockRecord>();
        for (int i = 0; i < records.size(); i++) {
            WeaponStockRecord record = records.get(i);
            if (record.getBuyableCount() > 0) {
                result.add(record);
            }
        }
        return result;
    }

    private static void addWeapon(List<StockReviewListRow> rows,
                                  WeaponStockRecord record,
                                  Color color,
                                  StockReviewState state) {
        boolean expanded = state.isWeaponExpanded(record.getWeaponId());
        String label = record.getDisplayName() + " (" + record.getOwnedCount() + "/" + record.getBuyableCount() + ") " + (expanded ? "(-)" : "(+)");
        boolean canBuy = record.getBuyableCount() > 0;
        rows.add(StockReviewListRow.weapon(
                label,
                StockReviewStyle.TEXT,
                StockReviewAction.toggleWeapon(record.getWeaponId()),
                StockReviewAction.buyBest(record.getWeaponId(), 1),
                StockReviewAction.buyBest(record.getWeaponId(), 10),
                canBuy));
        if (!expanded) {
            return;
        }
        addWeaponData(rows, record, state);
        addSellers(rows, record, state);
    }

    private static void addWeaponData(List<StockReviewListRow> rows, WeaponStockRecord record, StockReviewState state) {
        boolean expanded = state.isWeaponDataExpanded(record.getWeaponId());
        rows.add(StockReviewListRow.section(
                "Weapon data " + (expanded ? "(-)" : "(+)"),
                StockReviewAction.toggleWeaponSection(record.getWeaponId(), StockReviewSection.WEAPON_DATA)));
        if (!expanded) {
            return;
        }
        rows.add(StockReviewListRow.detail("Desired: " + record.getDesiredCount()));
        rows.add(StockReviewListRow.detail("Size: " + record.getSizeLabel()));
        rows.add(StockReviewListRow.detail("Type: " + record.getTypeLabel()));
        rows.add(StockReviewListRow.detail("Damage: " + record.getDamageLabel()));
        rows.add(StockReviewListRow.detail("EMP: " + record.getEmpLabel()));
        rows.add(StockReviewListRow.detail("Range: " + record.getRangeLabel()));
        rows.add(StockReviewListRow.detail("Flux/Second: " + record.getFluxPerSecondLabel()));
        rows.add(StockReviewListRow.detail("Flux/Damage: " + record.getFluxPerDamageLabel()));
    }

    private static void addSellers(List<StockReviewListRow> rows, WeaponStockRecord record, StockReviewState state) {
        boolean expanded = state.isSellersExpanded(record.getWeaponId());
        rows.add(StockReviewListRow.section(
                "Sellers " + (expanded ? "(-)" : "(+)"),
                StockReviewAction.toggleWeaponSection(record.getWeaponId(), StockReviewSection.SELLERS)));
        if (!expanded) {
            return;
        }
        List<SubmarketWeaponStock> stocks = record.getSubmarketStocks();
        if (stocks.isEmpty()) {
            rows.add(StockReviewListRow.detail("No seller stock found at this market."));
            return;
        }
        for (int i = 0; i < stocks.size(); i++) {
            SubmarketWeaponStock stock = stocks.get(i);
            boolean buyable = stock.isPurchasable() && stock.getCount() > 0;
            String label = stock.getSubmarketName() + ": " + stock.getCount();
            if (stock.getCount() > 0) {
                label += " @ " + stock.getUnitPrice() + "cr";
            }
            if (!stock.isPurchasable()) {
                label += " (locked)";
            }
            rows.add(StockReviewListRow.seller(
                    label,
                    buyable,
                    StockReviewAction.buyFromSubmarket(record.getWeaponId(), stock.getSubmarketId(), 1),
                    StockReviewAction.buyFromSubmarket(record.getWeaponId(), stock.getSubmarketId(), 10)));
        }
    }
}
