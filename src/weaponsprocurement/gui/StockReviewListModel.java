package weaponsprocurement.gui;

import weaponsprocurement.core.StockCategory;
import weaponsprocurement.core.StockItemType;
import weaponsprocurement.core.StockSourceMode;
import weaponsprocurement.core.WeaponStockRecord;
import weaponsprocurement.core.WeaponStockSnapshot;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class StockReviewListModel {
    private StockReviewListModel() {
    }

    static List<WimGuiListRow<StockReviewAction>> build(WeaponStockSnapshot snapshot,
                                          StockReviewState state,
                                          StockReviewTradeContext tradeContext) {
        List<WimGuiListRow<StockReviewAction>> rows = new ArrayList<WimGuiListRow<StockReviewAction>>();
        int displayed = 0;
        displayed += addItemType(rows, snapshot, state, tradeContext, StockItemType.WEAPON, false);
        displayed += addItemType(rows, snapshot, state, tradeContext, StockItemType.WING, true);
        if (displayed == 0) {
            rows.add(StockReviewListRow.empty(emptyStateMessage(snapshot, state)));
        }
        return rows;
    }

    private static String emptyStateMessage(WeaponStockSnapshot snapshot, StockReviewState state) {
        if (snapshot != null
                && snapshot.getTotalRecords() > 0
                && state != null
                && state.getActiveFilterCount() > 0) {
            return "All rows are hidden by the active filters.";
        }
        StockSourceMode sourceMode = snapshot == null ? StockSourceMode.LOCAL : snapshot.getSourceMode();
        if (StockSourceMode.SECTOR.equals(sourceMode)) {
            return "No Sector Market weapon or wing stock is currently available.";
        }
        if (StockSourceMode.FIXERS.equals(sourceMode)) {
            return "Fixer's Market has not observed any eligible stock yet, or all eligible stock is blacklisted.";
        }
        return "No local weapon or wing stock is buyable here, and no player-cargo weapons or wings are available to sell.";
    }

    private static int addItemType(List<WimGuiListRow<StockReviewAction>> rows,
                                   WeaponStockSnapshot snapshot,
                                   StockReviewState state,
                                   StockReviewTradeContext tradeContext,
                                   StockItemType itemType,
                                   boolean topGap) {
        int count = snapshot == null ? 0 : snapshot.getCount(itemType);
        boolean expanded = state.isExpanded(itemType);
        rows.add(StockReviewListRow.filterHeading(
                WimGuiToggleHeading.countedLabel(itemType.getSectionLabel(), count, expanded),
                StockReviewAction.toggle(itemType),
                topGap,
                "Show or hide " + itemType.getSectionLabel().toLowerCase(java.util.Locale.US) + "."));
        if (!expanded) {
            return count;
        }
        int displayed = 0;
        displayed += addCategory(rows, snapshot, state, tradeContext, itemType, StockCategory.NO_STOCK, StockReviewStyle.NO_STOCK, false);
        displayed += addCategory(rows, snapshot, state, tradeContext, itemType, StockCategory.INSUFFICIENT, StockReviewStyle.INSUFFICIENT, true);
        displayed += addCategory(rows, snapshot, state, tradeContext, itemType, StockCategory.SUFFICIENT, StockReviewStyle.SUFFICIENT, true);
        return displayed;
    }

    private static int addCategory(List<WimGuiListRow<StockReviewAction>> rows,
                                   WeaponStockSnapshot snapshot,
                                   StockReviewState state,
                                   StockReviewTradeContext tradeContext,
                                   StockItemType itemType,
                                   StockCategory category,
                                   Color color,
                                   boolean topGap) {
        List<WeaponStockRecord> records = filteredRecords(
                StockReviewTradePlanner.visibleTradeableRecords(snapshot, itemType, category),
                state.getActiveFilters());
        boolean expanded = state.isExpanded(itemType, category);
        String label = WimGuiToggleHeading.label(categoryHeading(itemType, category, records, tradeContext), expanded);
        rows.add(StockReviewListRow.categoryIndented(label, color, StockReviewAction.toggle(itemType, category), topGap,
                StockReviewTooltips.category(category), StockReviewStyle.WEAPON_INDENT));
        if (!expanded) {
            return records.size();
        }
        if (StockReviewStyle.SHOW_WIDTH_TEST_ROWS
                && StockItemType.WEAPON.equals(itemType)
                && StockCategory.NO_STOCK.equals(category)) {
            addWorstCaseTestRow(rows);
        }
        for (int i = 0; i < records.size(); i++) {
            addWeapon(rows, records.get(i), state, tradeContext);
        }
        return records.size();
    }

    private static void addWeapon(List<WimGuiListRow<StockReviewAction>> rows,
                                  WeaponStockRecord record,
                                  StockReviewState state,
                                  StockReviewTradeContext tradeContext) {
        boolean expanded = state.isItemExpanded(record.getItemKey());
        String label = WimGuiToggleHeading.label(record.getDisplayName(), expanded);
        int planQuantity = tradeContext.netQuantityForItem(record.getItemKey());
        int sellRemaining = tradeContext.negativeAdjustmentRemaining(record, Integer.MAX_VALUE);
        long transactionCost = tradeContext.transactionCostForItem(record.getItemKey());
        int buyStepQuantity = tradeContext.positiveAdjustmentRemaining(record, 10);
        int sellStepQuantity = Math.min(10, sellRemaining);
        int sufficientDelta = tradeContext.deltaToSufficient(record);
        List<WimGuiRowCell<StockReviewAction>> cells = WimGuiRowCell.of(
                StockReviewTradeRowCells.storage(record.getStorageCount(), planQuantity, StockReviewStyle.STOCK_CELL_WIDTH),
                StockReviewTradeRowCells.unitPrice(tradeContext.unitPriceForItem(record)),
                StockReviewTradeRowCells.plan(planQuantity, transactionCost),
                StockReviewTradeRowCells.step("-", sellStepQuantity, StockReviewStyle.SELL_BUTTON,
                        StockReviewAction.adjustPlan(record.getItemKey(), -sellStepQuantity),
                        StockReviewTooltips.decreasePlan(sellStepQuantity)),
                WimGuiRowCell.standardAction("-1", StockReviewStyle.TRADE_STEP_BUTTON_WIDTH, StockReviewStyle.SELL_BUTTON,
                        StockReviewAction.adjustPlan(record.getItemKey(), -1), sellRemaining >= 1,
                        StockReviewTooltips.decreasePlan(1)),
                WimGuiRowCell.standardAction("+1", StockReviewStyle.TRADE_STEP_BUTTON_WIDTH, StockReviewStyle.BUY_BUTTON,
                        StockReviewAction.adjustPlan(record.getItemKey(), 1),
                        tradeContext.positiveAdjustmentRemaining(record, 1) >= 1,
                        StockReviewTooltips.increasePlan(1)),
                StockReviewTradeRowCells.step("+", buyStepQuantity, StockReviewStyle.BUY_BUTTON,
                        StockReviewAction.adjustPlan(record.getItemKey(), buyStepQuantity),
                        StockReviewTooltips.increasePlan(buyStepQuantity)),
                WimGuiRowCell.standardAction("Sufficient", StockReviewStyle.SUFFICIENT_BUTTON_WIDTH,
                        sufficientDelta < 0 ? StockReviewStyle.SELL_BUTTON : StockReviewStyle.BUY_BUTTON,
                        StockReviewAction.adjustToSufficient(record.getItemKey(), sufficientDelta), sufficientDelta != 0,
                        StockReviewTooltips.sufficient(record)),
                WimGuiRowCell.standardAction("Reset", StockReviewStyle.RESET_BUTTON_WIDTH, StockReviewStyle.ACTION_BACKGROUND,
                        StockReviewAction.resetPlan(record.getItemKey()), planQuantity != 0,
                        StockReviewTooltips.resetPlan()));
        String itemTooltip = StockReviewTooltips.itemDataToggle(record);
        rows.add(StockReviewListRow.item(label, cells, StockReviewAction.toggleItem(record.getItemKey()),
                itemTooltip, StockReviewItemTooltip.forRecord(record, itemTooltip), StockReviewStyle.SECTION_INDENT));
        if (!expanded) {
            return;
        }
        StockReviewItemInfoRows.add(rows, record, state, StockReviewStyle.TRADE_ROW_RIGHT_BLOCK_WIDTH,
                StockReviewStyle.LIST_WIDTH, StockReviewStyle.DETAIL_INDENT, StockReviewStyle.DATA_INDENT);
    }

    private static void addWorstCaseTestRow(List<WimGuiListRow<StockReviewAction>> rows) {
        StockReviewTradeRowCells.addWorstCaseTradeRow(rows);
    }

    private static List<WeaponStockRecord> filteredRecords(List<WeaponStockRecord> records,
                                                           Set<StockReviewFilter> activeFilters) {
        if (records == null || records.isEmpty() || StockReviewFilters.count(activeFilters) <= 0) {
            return records;
        }
        List<WeaponStockRecord> result = new ArrayList<WeaponStockRecord>();
        for (int i = 0; i < records.size(); i++) {
            WeaponStockRecord record = records.get(i);
            if (StockReviewFilters.matches(record, activeFilters)) {
                result.add(record);
            }
        }
        return result;
    }

    private static String categoryHeading(StockItemType itemType,
                                          StockCategory category,
                                          List<WeaponStockRecord> records,
                                          StockReviewTradeContext tradeContext) {
        int itemTypes = records == null ? 0 : records.size();
        int selling = 0;
        int buying = 0;
        if (records != null && tradeContext != null) {
            for (int i = 0; i < records.size(); i++) {
                WeaponStockRecord record = records.get(i);
                selling += tradeContext.pendingSellQuantityForItem(record.getItemKey());
                buying += tradeContext.pendingBuyQuantityForItem(record.getItemKey());
            }
        }
        String typeLabel = StockItemType.WING.equals(itemType) ? "Wing Types" : "Weapon Types";
        return category.getLabel()
                + " [" + typeLabel + ": " + itemTypes + "]"
                + "[Selling: " + Math.max(0, selling) + "]"
                + "[Buying: " + Math.max(0, buying) + "]";
    }

}
