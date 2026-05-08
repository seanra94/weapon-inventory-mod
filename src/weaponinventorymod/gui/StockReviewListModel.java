package weaponinventorymod.gui;

import com.fs.starfarer.api.ui.Alignment;
import weaponinventorymod.core.CreditFormat;
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
        rows.add(StockReviewListRow.category(label, color, StockReviewAction.toggle(category), topGap,
                StockReviewTooltips.category(category)));
        if (!expanded) {
            return records.size();
        }
        if (StockCategory.NO_STOCK.equals(category)) {
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
                        StockReviewStyle.STOCK_CELL_WIDTH, StockReviewStyle.CELL_BACKGROUND, StockReviewStyle.TEXT,
                        Alignment.LMID, StockReviewTooltips.STORAGE),
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
        rows.add(StockReviewListRow.weapon(label, cells, StockReviewAction.toggleWeapon(record.getWeaponId()),
                StockReviewTooltips.weapon(record)));
        if (!expanded) {
            return;
        }
        addWeaponData(rows, record, state);
    }

    private static void addWorstCaseTestRow(List<WimGuiListRow<StockReviewAction>> rows) {
        List<WimGuiRowCell<StockReviewAction>> cells = WimGuiRowCell.of(
                WimGuiRowCell.info("Storage: 99+", StockReviewStyle.STOCK_CELL_WIDTH,
                        StockReviewStyle.CELL_BACKGROUND, StockReviewStyle.TEXT,
                        Alignment.LMID, StockReviewTooltips.STORAGE),
                WimGuiRowCell.info("Price: 99,999+" + CreditFormat.CREDIT_SYMBOL,
                        StockReviewStyle.PRICE_CELL_WIDTH, StockReviewStyle.CELL_BACKGROUND,
                        StockReviewStyle.TEXT, Alignment.LMID, StockReviewTooltips.PRICE),
                WimGuiRowCell.info("Selling: 99+ [999,999+" + CreditFormat.CREDIT_SYMBOL + "]",
                        StockReviewStyle.PLAN_CELL_WIDTH, StockReviewStyle.PLAN_NEGATIVE,
                        StockReviewStyle.TEXT, Alignment.LMID, StockReviewTooltips.PLAN),
                WimGuiRowCell.standardAction("-10", StockReviewStyle.TRADE_STEP_BUTTON_WIDTH,
                        StockReviewStyle.SELL_BUTTON, StockReviewAction.debugNoop(), true,
                        "Worst-case sell-step width sample."),
                WimGuiRowCell.standardAction("-1", StockReviewStyle.TRADE_STEP_BUTTON_WIDTH,
                        StockReviewStyle.SELL_BUTTON, StockReviewAction.debugNoop(), true,
                        "Worst-case single-sell width sample."),
                WimGuiRowCell.standardAction("+1", StockReviewStyle.TRADE_STEP_BUTTON_WIDTH,
                        StockReviewStyle.BUY_BUTTON, StockReviewAction.debugNoop(), true,
                        "Worst-case single-buy width sample."),
                WimGuiRowCell.standardAction("+10", StockReviewStyle.TRADE_STEP_BUTTON_WIDTH,
                        StockReviewStyle.BUY_BUTTON, StockReviewAction.debugNoop(), true,
                        "Worst-case buy-step width sample."),
                WimGuiRowCell.standardAction("Sufficient", StockReviewStyle.SUFFICIENT_BUTTON_WIDTH,
                        StockReviewStyle.SELL_BUTTON, StockReviewAction.debugNoop(), true,
                        "Worst-case sufficient button width sample."),
                WimGuiRowCell.standardAction("Reset", StockReviewStyle.RESET_BUTTON_WIDTH,
                        StockReviewStyle.ACTION_BACKGROUND, StockReviewAction.debugNoop(), true,
                        "Worst-case reset button width sample."));
        rows.add(StockReviewListRow.weapon("Suzuki-Clapteryon Thermal Prokector... (+)",
                cells,
                StockReviewAction.debugNoop(),
                "Worst-case row-width test sample. It does not affect trades."));
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
        String quantity = cappedCount(Math.abs(planQuantity));
        String total = cappedCredits(transactionCost, 999999);
        String label = planQuantity > 0
                ? "Buying: " + quantity + " [" + total + "]"
                : planQuantity < 0 ? "Selling: " + quantity + " [" + total + "]" : "Buying: 0 [" + StockReviewFormat.credits(0) + "]";
        Color fill = planQuantity > 0
                ? StockReviewStyle.PLAN_POSITIVE
                : planQuantity < 0 ? StockReviewStyle.PLAN_NEGATIVE : StockReviewStyle.CELL_BACKGROUND;
        return WimGuiRowCell.info(label, StockReviewStyle.PLAN_CELL_WIDTH, fill, StockReviewStyle.TEXT,
                Alignment.LMID, StockReviewTooltips.PLAN);
    }

    static String storageLabel(int ownedCount, int planQuantity) {
        if (planQuantity == 0) {
            return "Storage: " + cappedCount(ownedCount);
        }
        return "Storage: " + cappedCount(ownedCount) + " [" + signedCappedCount(planQuantity) + "]";
    }

    private static WimGuiRowCell<StockReviewAction> unitPriceCell(int unitPrice) {
        if (unitPrice == StockReviewQuoteBook.PRICE_UNAVAILABLE) {
            return WimGuiRowCell.info("Price: ?", StockReviewStyle.PRICE_CELL_WIDTH, StockReviewStyle.CELL_BACKGROUND,
                    StockReviewStyle.TEXT, Alignment.LMID, StockReviewTooltips.PRICE);
        }
        return WimGuiRowCell.info("Price: " + cappedCredits(unitPrice, 99999), StockReviewStyle.PRICE_CELL_WIDTH,
                StockReviewStyle.CELL_BACKGROUND, StockReviewStyle.TEXT, Alignment.LMID, StockReviewTooltips.PRICE);
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
        float dataIndent = StockReviewStyle.SECTION_INDENT;
        rows.add(StockReviewListRow.labelTextIndented("Desired", String.valueOf(record.getDesiredCount()), dataIndent, false, rightReserveWidth, listWidth));
        rows.add(StockReviewListRow.labelTextIndented("Size", record.getSizeLabel(), dataIndent, false, rightReserveWidth, listWidth));
        rows.add(StockReviewListRow.labelTextIndented("Type", record.getTypeLabel(), dataIndent, false, rightReserveWidth, listWidth));
        rows.add(StockReviewListRow.labelTextIndented("Damage", record.getDamageLabel(), dataIndent, false, rightReserveWidth, listWidth));
        rows.add(StockReviewListRow.labelTextIndented("EMP", record.getEmpLabel(), dataIndent, false, rightReserveWidth, listWidth));
        rows.add(StockReviewListRow.labelTextIndented("Range", record.getRangeLabel(), dataIndent, false, rightReserveWidth, listWidth));
        rows.add(StockReviewListRow.labelTextIndented("Flux/Second", record.getFluxPerSecondLabel(), dataIndent, false, rightReserveWidth, listWidth));
        rows.add(StockReviewListRow.labelTextIndented("Flux/Damage", record.getFluxPerDamageLabel(), dataIndent, false, rightReserveWidth, listWidth));
    }

    private static String cappedCount(int value) {
        return value >= 99 ? "99+" : String.valueOf(Math.max(0, value));
    }

    private static String signedCappedCount(int value) {
        String sign = value > 0 ? "+" : value < 0 ? "-" : "";
        int absolute = Math.abs(value);
        return sign + (absolute >= 99 ? "99+" : String.valueOf(absolute));
    }

    private static String cappedCredits(int credits, int cap) {
        if (credits == StockReviewQuoteBook.PRICE_UNAVAILABLE) {
            return "?";
        }
        int absolute = Math.abs(credits);
        if (absolute >= cap) {
            return CreditFormat.grouped(cap) + "+" + CreditFormat.CREDIT_SYMBOL;
        }
        return StockReviewFormat.credits(absolute);
    }
}
