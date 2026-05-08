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
        int cost = tradeContext.transactionCostForLine(purchase.getWeaponId(), purchase.getSubmarketId());
        List<WimGuiRowCell<StockReviewAction>> cells = WimGuiRowCell.of(
                WimGuiRowCell.info(StockReviewListModel.storageLabel(record.getStorageCount(), purchase.getQuantity()),
                        StockReviewStyle.STOCK_CELL_WIDTH, StockReviewStyle.CELL_BACKGROUND, StockReviewStyle.TEXT, Alignment.LMID),
                StockReviewListModel.planCell(purchase.getQuantity(), cost));
        rows.add(StockReviewListRow.weapon(WimGuiToggleHeading.label(record.getDisplayName(), expanded),
                cells, StockReviewAction.toggleWeapon(record.getWeaponId())));
        if (!expanded) {
            return;
        }
        StockReviewListModel.addWeaponData(rows, record, state,
                StockReviewStyle.REVIEW_ROW_RIGHT_BLOCK_WIDTH,
                StockReviewStyle.REVIEW_LIST_WIDTH);
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
