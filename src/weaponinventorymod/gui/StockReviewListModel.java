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
        List<WeaponStockRecord> records = StockReviewTradePlanner.visibleTradeableRecords(snapshot, category);
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
        int buyRemaining = tradeContext.buyableRemaining(record);
        int sellRemaining = tradeContext.sellableRemaining(record);
        int buyUntilQuantity = Math.min(buyRemaining, tradeContext.buyNeededForSufficiency(record));
        buyUntilQuantity = tradeContext.affordableBuyQuantity(record, null, buyUntilQuantity);
        int sellUntilQuantity = tradeContext.sellableUntilSufficient(record);
        int transactionCost = tradeContext.transactionCostForWeapon(record.getWeaponId());
        int buyStepQuantity = tradeContext.affordableBuyQuantity(record, null, 10);
        int sellStepQuantity = Math.min(10, sellRemaining);
        List<WimGuiRowCell<StockReviewAction>> cells = WimGuiRowCell.of(
                WimGuiRowCell.info("Storage: " + record.getOwnedCount(),
                        StockReviewStyle.STOCK_CELL_WIDTH, StockReviewStyle.CELL_BACKGROUND, StockReviewStyle.TEXT),
                unitCostCell(tradeContext.unitCostForWeapon(record)),
                planCell(planQuantity, transactionCost),
                WimGuiRowCell.standardAction("-S", StockReviewStyle.TRADE_STEP_BUTTON_WIDTH, StockReviewStyle.SELL_BUTTON,
                        StockReviewAction.adjustPlan(record.getWeaponId(), -sellUntilQuantity), sellUntilQuantity > 0),
                stepCell("-", sellStepQuantity, StockReviewStyle.SELL_BUTTON,
                        StockReviewAction.adjustPlan(record.getWeaponId(), -sellStepQuantity)),
                WimGuiRowCell.standardAction("-1", StockReviewStyle.TRADE_STEP_BUTTON_WIDTH, StockReviewStyle.SELL_BUTTON,
                        StockReviewAction.adjustPlan(record.getWeaponId(), -1), sellRemaining >= 1),
                WimGuiRowCell.standardAction("+1", StockReviewStyle.TRADE_STEP_BUTTON_WIDTH, StockReviewStyle.BUY_BUTTON,
                        StockReviewAction.adjustPlan(record.getWeaponId(), 1),
                        tradeContext.affordableBuyQuantity(record, null, 1) >= 1),
                stepCell("+", buyStepQuantity, StockReviewStyle.BUY_BUTTON,
                        StockReviewAction.adjustPlan(record.getWeaponId(), buyStepQuantity)),
                WimGuiRowCell.standardAction("+S", StockReviewStyle.TRADE_STEP_BUTTON_WIDTH, StockReviewStyle.CONFIRM_BUTTON,
                        StockReviewAction.adjustPlan(record.getWeaponId(), buyUntilQuantity), buyUntilQuantity > 0),
                WimGuiRowCell.standardAction("Reset", StockReviewStyle.RESET_BUTTON_WIDTH, StockReviewStyle.ACTION_BACKGROUND,
                        StockReviewAction.resetPlan(record.getWeaponId()), planQuantity != 0));
        rows.add(StockReviewListRow.weapon(label, cells, StockReviewAction.toggleWeapon(record.getWeaponId())));
        if (!expanded) {
            return;
        }
        addWeaponData(rows, record, state);
        addSellers(rows, record, state, tradeContext);
    }

    private static WimGuiRowCell<StockReviewAction> planCell(int planQuantity, int transactionCost) {
        String quantity = String.valueOf(Math.abs(planQuantity));
        String total = StockReviewFormat.credits(transactionCost);
        String label = planQuantity > 0
                ? "Buying: " + quantity + " [" + total + "]"
                : planQuantity < 0 ? "Selling: " + quantity + " [" + total + "]" : "Buying: 0 [0cr]";
        Color fill = planQuantity > 0
                ? StockReviewStyle.PLAN_POSITIVE
                : planQuantity < 0 ? StockReviewStyle.PLAN_NEGATIVE : StockReviewStyle.PLAN_ZERO;
        return WimGuiRowCell.info(label, StockReviewStyle.PLAN_CELL_WIDTH, fill, StockReviewStyle.TEXT);
    }

    private static WimGuiRowCell<StockReviewAction> unitCostCell(int unitCost) {
        if (unitCost == StockReviewQuoteBook.PRICE_UNAVAILABLE) {
            return WimGuiRowCell.info("Cost: ?", StockReviewStyle.COST_CELL_WIDTH, StockReviewStyle.COST_BUTTON, StockReviewStyle.TEXT);
        }
        return WimGuiRowCell.info("Cost: " + StockReviewFormat.credits(unitCost), StockReviewStyle.COST_CELL_WIDTH,
                StockReviewStyle.COST_BUTTON, StockReviewStyle.TEXT);
    }

    private static WimGuiRowCell<StockReviewAction> stepCell(String sign,
                                                            int quantity,
                                                            Color fill,
                                                            StockReviewAction action) {
        boolean enabled = quantity > 1;
        String label = enabled ? sign + quantity : sign + "10";
        return WimGuiRowCell.standardAction(label, StockReviewStyle.TRADE_STEP_BUTTON_WIDTH, fill, action, enabled);
    }

    static void addWeaponData(List<WimGuiListRow<StockReviewAction>> rows, WeaponStockRecord record, StockReviewState state) {
        addWeaponData(rows, record, state, StockReviewStyle.TRADE_ROW_RIGHT_BLOCK_WIDTH);
    }

    static void addWeaponData(List<WimGuiListRow<StockReviewAction>> rows,
                              WeaponStockRecord record,
                              StockReviewState state,
                              float rightReserveWidth) {
        boolean expanded = state.isWeaponDataExpanded(record.getWeaponId());
        rows.add(sectionRow(
                WimGuiToggleHeading.label("Weapon Data", expanded),
                StockReviewAction.toggleWeaponSection(record.getWeaponId(), StockReviewSection.WEAPON_DATA),
                rightReserveWidth));
        if (!expanded) {
            return;
        }
        rows.add(StockReviewListRow.labelTextIndented("Desired", String.valueOf(record.getDesiredCount()), StockReviewStyle.DETAIL_INDENT, false, rightReserveWidth));
        rows.add(StockReviewListRow.labelTextIndented("Size", record.getSizeLabel(), StockReviewStyle.DETAIL_INDENT, false, rightReserveWidth));
        rows.add(StockReviewListRow.labelTextIndented("Type", record.getTypeLabel(), StockReviewStyle.DETAIL_INDENT, false, rightReserveWidth));
        rows.add(StockReviewListRow.labelTextIndented("Damage", record.getDamageLabel(), StockReviewStyle.DETAIL_INDENT, false, rightReserveWidth));
        rows.add(StockReviewListRow.labelTextIndented("EMP", record.getEmpLabel(), StockReviewStyle.DETAIL_INDENT, false, rightReserveWidth));
        rows.add(StockReviewListRow.labelTextIndented("Range", record.getRangeLabel(), StockReviewStyle.DETAIL_INDENT, false, rightReserveWidth));
        rows.add(StockReviewListRow.labelTextIndented("Flux/Second", record.getFluxPerSecondLabel(), StockReviewStyle.DETAIL_INDENT, false, rightReserveWidth));
        rows.add(StockReviewListRow.labelTextIndented("Flux/Damage", record.getFluxPerDamageLabel(), StockReviewStyle.DETAIL_INDENT, false, rightReserveWidth));
    }

    private static void addSellers(List<WimGuiListRow<StockReviewAction>> rows,
                                   WeaponStockRecord record,
                                   StockReviewState state,
                                   StockReviewTradeContext tradeContext) {
        boolean expanded = state.isSellersExpanded(record.getWeaponId());
        rows.add(StockReviewListRow.section(
                WimGuiToggleHeading.label("Sellers", expanded),
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
            int buyableOne = tradeContext.affordableBuyQuantity(record, stock.getSubmarketId(), 1);
            int buyStepQuantity = tradeContext.affordableBuyQuantity(record, stock.getSubmarketId(), 10);
            rows.add(StockReviewListRow.seller(
                    stock.getSubmarketName(),
                    stock.getCount() + (stock.getCount() > 0 ? " @ " + StockReviewFormat.credits(stock.getUnitPrice()) : "") + (!stock.isPurchasable() ? " (locked)" : ""),
                    buyableOne >= 1,
                    buyStepQuantity,
                    StockReviewAction.buyFromSubmarket(record.getWeaponId(), stock.getSubmarketId(), 1),
                    StockReviewAction.buyFromSubmarket(record.getWeaponId(), stock.getSubmarketId(), buyStepQuantity)));
        }
    }

    private static WimGuiListRow<StockReviewAction> sectionRow(String label,
                                                               StockReviewAction action,
                                                               float rightReserveWidth) {
        if (rightReserveWidth == StockReviewStyle.REVIEW_ROW_RIGHT_BLOCK_WIDTH) {
            return StockReviewListRow.reviewSection(label, action);
        }
        return StockReviewListRow.section(label, action);
    }
}
