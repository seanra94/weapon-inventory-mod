package weaponinventorymod.gui;

import com.fs.starfarer.api.ui.Alignment;
import weaponinventorymod.core.WeaponStockRecord;
import weaponinventorymod.core.WeaponStockSnapshot;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

final class StockReviewReviewListModel {
    private StockReviewReviewListModel() {
    }

    static List<WimGuiListRow<StockReviewAction>> build(WeaponStockSnapshot snapshot,
                                          List<StockReviewPendingPurchase> pendingPurchases,
                                          StockReviewState state,
                                          StockReviewTradeContext tradeContext) {
        List<WimGuiListRow<StockReviewAction>> rows = new ArrayList<WimGuiListRow<StockReviewAction>>();
        if (pendingPurchases == null || pendingPurchases.isEmpty()) {
            rows.add(StockReviewListRow.empty("No weapon trades are planned."));
            return rows;
        }
        addReviewGroup(rows, snapshot, pendingPurchases, state, tradeContext, StockReviewTradeGroup.BUYING);
        addReviewGroup(rows, snapshot, pendingPurchases, state, tradeContext, StockReviewTradeGroup.SELLING);
        return rows;
    }

    private static void addReviewGroup(List<WimGuiListRow<StockReviewAction>> rows,
                                       WeaponStockSnapshot snapshot,
                                       List<StockReviewPendingPurchase> pendingPurchases,
                                       StockReviewState state,
                                       StockReviewTradeContext tradeContext,
                                       StockReviewTradeGroup tradeGroup) {
        List<StockReviewPendingPurchase> groupPurchases = reviewPurchasesForGroup(pendingPurchases, tradeGroup);
        boolean expanded = state.isExpanded(tradeGroup);
        String label = WimGuiToggleHeading.countedLabel(tradeGroup.getLabel(), groupPurchases.size(), expanded);
        Color headingColor = StockReviewTradeGroup.BUYING.equals(tradeGroup)
                ? StockReviewStyle.CONFIRM_BUTTON
                : StockReviewStyle.CANCEL_BUTTON;
        rows.add(StockReviewListRow.category(label, headingColor, StockReviewAction.toggle(tradeGroup),
                StockReviewTradeGroup.SELLING.equals(tradeGroup)));
        if (!expanded) {
            return;
        }
        for (int i = 0; i < groupPurchases.size(); i++) {
            addReviewTrade(rows, snapshot, groupPurchases.get(i), state, tradeContext);
        }
    }

    private static void addReviewTrade(List<WimGuiListRow<StockReviewAction>> rows,
                                       WeaponStockSnapshot snapshot,
                                       StockReviewPendingPurchase purchase,
                                       StockReviewState state,
                                       StockReviewTradeContext tradeContext) {
        WeaponStockRecord record = snapshot.getRecord(purchase.getWeaponId());
        if (record == null) {
            rows.add(StockReviewListRow.review(purchase.getWeaponId()));
            return;
        }
        boolean expanded = state.isWeaponExpanded(record.getWeaponId());
        int quantity = Math.abs(purchase.getQuantity());
        int cost = tradeContext.transactionCostForLine(purchase.getWeaponId(), purchase.getSubmarketId());
        List<WimGuiRowCell<StockReviewAction>> cells = WimGuiRowCell.of(
                WimGuiRowCell.info(StockReviewListModel.storageLabel(record.getStorageCount(), purchase.getQuantity()),
                        StockReviewStyle.STOCK_CELL_WIDTH, StockReviewStyle.CELL_BACKGROUND, StockReviewStyle.TEXT, Alignment.LMID),
                reviewCostCell(cost, purchase.isSell()),
                WimGuiRowCell.info((purchase.isSell() ? "Selling: " : "Buying: ") + quantity,
                        StockReviewStyle.PLAN_CELL_WIDTH,
                        purchase.isSell() ? StockReviewStyle.PLAN_NEGATIVE : StockReviewStyle.PLAN_POSITIVE,
                        StockReviewStyle.TEXT));
        rows.add(StockReviewListRow.weapon(WimGuiToggleHeading.label(record.getDisplayName(), expanded),
                cells, StockReviewAction.toggleWeapon(record.getWeaponId())));
        if (!expanded) {
            return;
        }
        StockReviewListModel.addWeaponData(rows, record, state, StockReviewStyle.REVIEW_ROW_RIGHT_BLOCK_WIDTH);
        if (purchase.isBuy()) {
            addReviewSellers(rows, purchase, record, state, tradeContext);
        }
    }

    private static void addReviewSellers(List<WimGuiListRow<StockReviewAction>> rows,
                                         StockReviewPendingPurchase purchase,
                                         WeaponStockRecord record,
                                         StockReviewState state,
                                         StockReviewTradeContext tradeContext) {
        boolean expanded = state.isSellersExpanded(record.getWeaponId());
        rows.add(StockReviewListRow.reviewSection(WimGuiToggleHeading.label("Sellers", expanded),
                StockReviewAction.toggleWeaponSection(record.getWeaponId(), StockReviewSection.SELLERS)));
        if (!expanded) {
            return;
        }
        List<StockReviewSellerAllocation> allocations = tradeContext.sellerAllocations(purchase);
        if (allocations.isEmpty()) {
            rows.add(StockReviewListRow.detail("No seller allocation found."));
            return;
        }
        for (int i = 0; i < allocations.size(); i++) {
            StockReviewSellerAllocation allocation = allocations.get(i);
            rows.add(StockReviewListRow.labelTextIndented("Market", allocation.getSubmarketName(),
                    StockReviewStyle.SELLER_INDENT, false, StockReviewStyle.REVIEW_ROW_RIGHT_BLOCK_WIDTH));
            rows.add(StockReviewListRow.labelTextIndented("Buying", String.valueOf(allocation.getQuantity()),
                    StockReviewStyle.SELLER_INDENT, false, StockReviewStyle.REVIEW_ROW_RIGHT_BLOCK_WIDTH));
            rows.add(StockReviewListRow.labelTextIndented("Price", StockReviewFormat.credits(allocation.getCost()),
                    StockReviewStyle.SELLER_INDENT, false, StockReviewStyle.REVIEW_ROW_RIGHT_BLOCK_WIDTH));
        }
    }

    private static WimGuiRowCell<StockReviewAction> reviewCostCell(int cost, boolean sell) {
        if (cost == StockReviewQuoteBook.PRICE_UNAVAILABLE) {
            return WimGuiRowCell.info("Price: ?", StockReviewStyle.PRICE_CELL_WIDTH,
                    StockReviewStyle.CELL_BACKGROUND, StockReviewStyle.TEXT);
        }
        if (sell) {
            return WimGuiRowCell.info("Profit: " + StockReviewFormat.credits(cost), StockReviewStyle.PRICE_CELL_WIDTH,
                    StockReviewStyle.PROFIT_BUTTON, StockReviewStyle.TEXT);
        }
        return WimGuiRowCell.info("Price: " + StockReviewFormat.credits(cost), StockReviewStyle.PRICE_CELL_WIDTH,
                StockReviewStyle.CELL_BACKGROUND, StockReviewStyle.TEXT);
    }

    private static List<StockReviewPendingPurchase> reviewPurchasesForGroup(List<StockReviewPendingPurchase> pendingPurchases,
                                                                            StockReviewTradeGroup tradeGroup) {
        List<StockReviewPendingPurchase> result = new ArrayList<StockReviewPendingPurchase>();
        if (pendingPurchases == null) {
            return result;
        }
        for (int i = 0; i < pendingPurchases.size(); i++) {
            StockReviewPendingPurchase purchase = pendingPurchases.get(i);
            if (StockReviewTradeGroup.BUYING.equals(tradeGroup) && purchase.isBuy()) {
                result.add(purchase);
            } else if (StockReviewTradeGroup.SELLING.equals(tradeGroup) && purchase.isSell()) {
                result.add(purchase);
            }
        }
        return result;
    }
}
