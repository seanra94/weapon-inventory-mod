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
        List<WimGuiRowCell<StockReviewAction>> cells = WimGuiRowCell.of(
                WimGuiRowCell.info("Storage: " + Math.max(0, record.getOwnedCount() - record.getPlayerCargoCount()),
                        StockReviewStyle.STOCK_CELL_WIDTH, StockReviewStyle.CELL_BACKGROUND, StockReviewStyle.TEXT),
                WimGuiRowCell.info("Inventory: " + record.getPlayerCargoCount(),
                        StockReviewStyle.INVENTORY_CELL_WIDTH, StockReviewStyle.CELL_BACKGROUND, StockReviewStyle.TEXT),
                WimGuiRowCell.info("Stocked: " + record.getOwnedCount(),
                        StockReviewStyle.STOCK_CELL_WIDTH, StockReviewStyle.CELL_BACKGROUND, StockReviewStyle.TEXT),
                planCell(planQuantity),
                costCell(transactionCost),
                WimGuiRowCell.standardAction("-S", StockReviewStyle.TRADE_STEP_BUTTON_WIDTH, StockReviewStyle.SELL_BUTTON,
                        StockReviewAction.adjustPlan(record.getWeaponId(), -sellUntilQuantity), sellUntilQuantity > 0),
                WimGuiRowCell.standardAction("-10", StockReviewStyle.TRADE_STEP_BUTTON_WIDTH, StockReviewStyle.SELL_BUTTON,
                        StockReviewAction.adjustPlan(record.getWeaponId(), -10), sellRemaining >= 10),
                WimGuiRowCell.standardAction("-1", StockReviewStyle.TRADE_STEP_BUTTON_WIDTH, StockReviewStyle.SELL_BUTTON,
                        StockReviewAction.adjustPlan(record.getWeaponId(), -1), sellRemaining >= 1),
                WimGuiRowCell.standardAction("+1", StockReviewStyle.TRADE_STEP_BUTTON_WIDTH, StockReviewStyle.BUY_BUTTON,
                        StockReviewAction.adjustPlan(record.getWeaponId(), 1),
                        tradeContext.affordableBuyQuantity(record, null, 1) >= 1),
                WimGuiRowCell.standardAction("+10", StockReviewStyle.TRADE_STEP_BUTTON_WIDTH, StockReviewStyle.BUY_BUTTON,
                        StockReviewAction.adjustPlan(record.getWeaponId(), 10),
                        tradeContext.affordableBuyQuantity(record, null, 10) >= 10),
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

    private static WimGuiRowCell<StockReviewAction> planCell(int planQuantity) {
        String label = planQuantity > 0
                ? "Buying: " + planQuantity
                : planQuantity < 0 ? "Selling: " + (-planQuantity) : "Buying: 0";
        Color fill = planQuantity > 0
                ? StockReviewStyle.PLAN_POSITIVE
                : planQuantity < 0 ? StockReviewStyle.PLAN_NEGATIVE : StockReviewStyle.PLAN_ZERO;
        return WimGuiRowCell.info(label, StockReviewStyle.PLAN_CELL_WIDTH, fill, StockReviewStyle.TEXT);
    }

    private static WimGuiRowCell<StockReviewAction> costCell(int transactionCost) {
        if (transactionCost == StockReviewQuoteBook.PRICE_UNAVAILABLE) {
            return WimGuiRowCell.info("Cost: ?", StockReviewStyle.COST_CELL_WIDTH, StockReviewStyle.COST_BUTTON, StockReviewStyle.TEXT);
        }
        if (transactionCost < 0) {
            return WimGuiRowCell.info("Profit: $" + (-transactionCost), StockReviewStyle.COST_CELL_WIDTH,
                    StockReviewStyle.PROFIT_BUTTON, StockReviewStyle.TEXT);
        }
        return WimGuiRowCell.info("Cost: $" + transactionCost, StockReviewStyle.COST_CELL_WIDTH,
                transactionCost == 0 ? StockReviewStyle.PLAN_ZERO : StockReviewStyle.COST_BUTTON, StockReviewStyle.TEXT);
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
            int buyableTen = tradeContext.affordableBuyQuantity(record, stock.getSubmarketId(), 10);
            rows.add(StockReviewListRow.seller(
                    stock.getSubmarketName(),
                    stock.getCount() + (stock.getCount() > 0 ? " @ " + stock.getUnitPrice() + "cr" : "") + (!stock.isPurchasable() ? " (locked)" : ""),
                    buyableOne >= 1,
                    buyableTen >= 10,
                    StockReviewAction.buyFromSubmarket(record.getWeaponId(), stock.getSubmarketId(), 1),
                    StockReviewAction.buyFromSubmarket(record.getWeaponId(), stock.getSubmarketId(), 10)));
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
