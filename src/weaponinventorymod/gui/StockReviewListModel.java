package weaponinventorymod.gui;

import com.fs.starfarer.api.ui.Alignment;
import weaponinventorymod.core.StockCategory;
import weaponinventorymod.core.WeaponStockRecord;
import weaponinventorymod.core.WeaponStockSnapshot;

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
        displayed += addCategory(rows, snapshot, state, tradeContext, StockCategory.NO_STOCK, StockReviewStyle.NO_STOCK, false);
        displayed += addCategory(rows, snapshot, state, tradeContext, StockCategory.INSUFFICIENT, StockReviewStyle.INSUFFICIENT, true);
        displayed += addCategory(rows, snapshot, state, tradeContext, StockCategory.SUFFICIENT, StockReviewStyle.SUFFICIENT, true);
        if (displayed == 0) {
            rows.add(StockReviewListRow.empty("No tradeable weapons were found at this market."));
        }
        return rows;
    }

    private static int addCategory(List<WimGuiListRow<StockReviewAction>> rows,
                                   WeaponStockSnapshot snapshot,
                                   StockReviewState state,
                                   StockReviewTradeContext tradeContext,
                                   StockCategory category,
                                   Color color,
                                   boolean topGap) {
        List<WeaponStockRecord> records = filteredRecords(
                StockReviewTradePlanner.visibleTradeableRecords(snapshot, category),
                state.getActiveFilters());
        boolean expanded = state.isExpanded(category);
        String label = WimGuiToggleHeading.countedLabel(category.getLabel(), records.size(), expanded);
        rows.add(StockReviewListRow.category(label, color, StockReviewAction.toggle(category), topGap));
        if (!expanded) {
            return records.size();
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
        boolean expanded = state.isWeaponExpanded(record.getWeaponId());
        String label = WimGuiToggleHeading.label(record.getDisplayName(), expanded);
        int planQuantity = tradeContext.netQuantityForWeapon(record.getWeaponId());
        int sellRemaining = tradeContext.negativeAdjustmentRemaining(record, Integer.MAX_VALUE);
        int transactionCost = tradeContext.transactionCostForWeapon(record.getWeaponId());
        int buyStepQuantity = tradeContext.positiveAdjustmentRemaining(record, 10);
        int sellStepQuantity = Math.min(10, sellRemaining);
        int sufficientDelta = tradeContext.deltaToSufficient(record);
        List<WimGuiRowCell<StockReviewAction>> cells = WimGuiRowCell.of(
                WimGuiRowCell.info(storageLabel(record.getStorageCount(), planQuantity),
                        StockReviewStyle.STOCK_CELL_WIDTH, StockReviewStyle.CELL_BACKGROUND, StockReviewStyle.TEXT, Alignment.LMID),
                unitPriceCell(tradeContext.unitPriceForWeapon(record)),
                planCell(planQuantity, transactionCost),
                stepCell("-", sellStepQuantity, StockReviewStyle.SELL_BUTTON,
                        StockReviewAction.adjustPlan(record.getWeaponId(), -sellStepQuantity),
                        "Reduce this weapon's plan by the shown amount."),
                WimGuiRowCell.standardAction("-1", StockReviewStyle.TRADE_STEP_BUTTON_WIDTH, StockReviewStyle.SELL_BUTTON,
                        StockReviewAction.adjustPlan(record.getWeaponId(), -1), sellRemaining >= 1,
                        "Reduce this weapon's plan by 1."),
                WimGuiRowCell.standardAction("+1", StockReviewStyle.TRADE_STEP_BUTTON_WIDTH, StockReviewStyle.BUY_BUTTON,
                        StockReviewAction.adjustPlan(record.getWeaponId(), 1),
                        tradeContext.positiveAdjustmentRemaining(record, 1) >= 1,
                        "Increase this weapon's plan by 1."),
                stepCell("+", buyStepQuantity, StockReviewStyle.BUY_BUTTON,
                        StockReviewAction.adjustPlan(record.getWeaponId(), buyStepQuantity),
                        "Increase this weapon's plan by the shown amount."),
                WimGuiRowCell.standardAction("Sufficient", StockReviewStyle.SUFFICIENT_BUTTON_WIDTH,
                        sufficientDelta < 0 ? StockReviewStyle.SELL_BUTTON : StockReviewStyle.BUY_BUTTON,
                        StockReviewAction.adjustToSufficient(record.getWeaponId(), sufficientDelta), sufficientDelta != 0,
                        "Adjust this weapon until it is barely sufficient."),
                WimGuiRowCell.standardAction("Reset", StockReviewStyle.RESET_BUTTON_WIDTH, StockReviewStyle.ACTION_BACKGROUND,
                        StockReviewAction.resetPlan(record.getWeaponId()), planQuantity != 0,
                        "Clear the planned trade for this weapon."));
        rows.add(StockReviewListRow.weapon(label, cells, StockReviewAction.toggleWeapon(record.getWeaponId())));
        if (!expanded) {
            return;
        }
        addWeaponData(rows, record, state);
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

    static WimGuiRowCell<StockReviewAction> planCell(int planQuantity, int transactionCost) {
        String quantity = String.valueOf(Math.abs(planQuantity));
        String total = StockReviewFormat.credits(transactionCost);
        String label = planQuantity > 0
                ? "Buying: " + quantity + " [" + total + "]"
                : planQuantity < 0 ? "Selling: " + quantity + " [" + total + "]" : "Buying: 0 [" + StockReviewFormat.credits(0) + "]";
        Color fill = planQuantity > 0
                ? StockReviewStyle.PLAN_POSITIVE
                : planQuantity < 0 ? StockReviewStyle.PLAN_NEGATIVE : StockReviewStyle.CELL_BACKGROUND;
        return WimGuiRowCell.info(label, StockReviewStyle.PLAN_CELL_WIDTH, fill, StockReviewStyle.TEXT, Alignment.LMID);
    }

    static String storageLabel(int ownedCount, int planQuantity) {
        if (planQuantity == 0) {
            return "Storage: " + ownedCount;
        }
        return "Storage: " + ownedCount + " [" + (planQuantity > 0 ? "+" : "") + planQuantity + "]";
    }

    private static WimGuiRowCell<StockReviewAction> unitPriceCell(int unitPrice) {
        if (unitPrice == StockReviewQuoteBook.PRICE_UNAVAILABLE) {
            return WimGuiRowCell.info("Price: ?", StockReviewStyle.PRICE_CELL_WIDTH, StockReviewStyle.CELL_BACKGROUND, StockReviewStyle.TEXT, Alignment.LMID);
        }
        return WimGuiRowCell.info("Price: " + StockReviewFormat.credits(unitPrice), StockReviewStyle.PRICE_CELL_WIDTH,
                StockReviewStyle.CELL_BACKGROUND, StockReviewStyle.TEXT, Alignment.LMID);
    }

    private static WimGuiRowCell<StockReviewAction> stepCell(String sign,
                                                            int quantity,
                                                            Color fill,
                                                            StockReviewAction action,
                                                            String tooltip) {
        boolean enabled = quantity > 1;
        String label = enabled ? sign + quantity : sign + "10";
        return WimGuiRowCell.standardAction(label, StockReviewStyle.TRADE_STEP_BUTTON_WIDTH, fill, action, enabled, tooltip);
    }

    static void addWeaponData(List<WimGuiListRow<StockReviewAction>> rows, WeaponStockRecord record, StockReviewState state) {
        addWeaponData(rows, record, state, StockReviewStyle.TRADE_ROW_RIGHT_BLOCK_WIDTH, StockReviewStyle.LIST_WIDTH);
    }

    static void addWeaponData(List<WimGuiListRow<StockReviewAction>> rows,
                              WeaponStockRecord record,
                              StockReviewState state,
                              float rightReserveWidth) {
        addWeaponData(rows, record, state, rightReserveWidth, StockReviewStyle.LIST_WIDTH);
    }

    static void addWeaponData(List<WimGuiListRow<StockReviewAction>> rows,
                              WeaponStockRecord record,
                              StockReviewState state,
                              float rightReserveWidth,
                              float listWidth) {
        boolean expanded = state.isWeaponDataExpanded(record.getWeaponId());
        float nestedRightReserveWidth = nestedRightReserveWidth(rightReserveWidth);
        rows.add(sectionRow(
                WimGuiToggleHeading.label("Weapon Data", expanded),
                StockReviewAction.toggleWeaponSection(record.getWeaponId(), StockReviewSection.WEAPON_DATA),
                nestedRightReserveWidth));
        if (!expanded) {
            return;
        }
        rows.add(StockReviewListRow.labelTextIndented("Desired", String.valueOf(record.getDesiredCount()), StockReviewStyle.DETAIL_INDENT, false, nestedRightReserveWidth, listWidth));
        rows.add(StockReviewListRow.labelTextIndented("Size", record.getSizeLabel(), StockReviewStyle.DETAIL_INDENT, false, nestedRightReserveWidth, listWidth));
        rows.add(StockReviewListRow.labelTextIndented("Type", record.getTypeLabel(), StockReviewStyle.DETAIL_INDENT, false, nestedRightReserveWidth, listWidth));
        rows.add(StockReviewListRow.labelTextIndented("Damage", record.getDamageLabel(), StockReviewStyle.DETAIL_INDENT, false, nestedRightReserveWidth, listWidth));
        rows.add(StockReviewListRow.labelTextIndented("EMP", record.getEmpLabel(), StockReviewStyle.DETAIL_INDENT, false, nestedRightReserveWidth, listWidth));
        rows.add(StockReviewListRow.labelTextIndented("Range", record.getRangeLabel(), StockReviewStyle.DETAIL_INDENT, false, nestedRightReserveWidth, listWidth));
        rows.add(StockReviewListRow.labelTextIndented("Flux/Second", record.getFluxPerSecondLabel(), StockReviewStyle.DETAIL_INDENT, false, nestedRightReserveWidth, listWidth));
        rows.add(StockReviewListRow.labelTextIndented("Flux/Damage", record.getFluxPerDamageLabel(), StockReviewStyle.DETAIL_INDENT, false, nestedRightReserveWidth, listWidth));
    }

    private static WimGuiListRow<StockReviewAction> sectionRow(String label,
                                                               StockReviewAction action,
                                                               float rightReserveWidth) {
        return StockReviewListRow.section(label, action, rightReserveWidth);
    }

    private static float nestedRightReserveWidth(float rightReserveWidth) {
        return rightReserveWidth;
    }
}
