package weaponsprocurement.gui;

import com.fs.starfarer.api.ui.Alignment;
import weaponsprocurement.core.WeaponStockRecord;
import weaponsprocurement.core.WeaponStockSnapshot;

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
        List<StockReviewPendingPurchase> buying = reviewPurchasesForGroup(pendingPurchases, StockReviewTradeGroup.BUYING);
        List<StockReviewPendingPurchase> selling = reviewPurchasesForGroup(pendingPurchases, StockReviewTradeGroup.SELLING);
        if (buying.isEmpty() && selling.isEmpty()) {
            rows.add(StockReviewListRow.empty("No weapon trades are planned."));
            return rows;
        }
        addReviewGroup(rows, snapshot, buying, state, tradeContext, StockReviewTradeGroup.BUYING);
        addReviewGroup(rows, snapshot, selling, state, tradeContext, StockReviewTradeGroup.SELLING);
        return rows;
    }

    private static void addReviewGroup(List<WimGuiListRow<StockReviewAction>> rows,
                                       WeaponStockSnapshot snapshot,
                                       List<StockReviewPendingPurchase> groupPurchases,
                                       StockReviewState state,
                                       StockReviewTradeContext tradeContext,
                                       StockReviewTradeGroup tradeGroup) {
        boolean expanded = state.isExpanded(tradeGroup);
        String label = WimGuiToggleHeading.countedLabel(tradeGroup.getLabel(), groupPurchases.size(), expanded);
        Color headingColor = StockReviewTradeGroup.BUYING.equals(tradeGroup)
                ? StockReviewStyle.CONFIRM_BUTTON
                : StockReviewStyle.CANCEL_BUTTON;
        rows.add(StockReviewListRow.category(label, headingColor, StockReviewAction.toggle(tradeGroup),
                StockReviewTradeGroup.SELLING.equals(tradeGroup),
                "Show or hide queued " + tradeGroup.getLabel().toLowerCase(java.util.Locale.US) + " trades."));
        if (!expanded) {
            return;
        }
        if (StockReviewTradeGroup.SELLING.equals(tradeGroup)) {
            addWorstCaseReviewRow(rows);
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
        WeaponStockRecord record = snapshot.getRecord(purchase.getItemKey());
        if (record == null) {
            rows.add(StockReviewListRow.review(purchase.getItemKey()));
            return;
        }
        boolean expanded = state.isItemExpanded(record.getItemKey());
        int cost = tradeContext.transactionCostForLine(purchase.getItemKey(), purchase.getSubmarketId());
        List<WimGuiRowCell<StockReviewAction>> cells = WimGuiRowCell.of(
                WimGuiRowCell.info(StockReviewListModel.storageLabel(record.getStorageCount(), purchase.getQuantity()),
                        StockReviewStyle.REVIEW_STOCK_CELL_WIDTH, StockReviewStyle.CELL_BACKGROUND, StockReviewStyle.TEXT,
                        Alignment.LMID, StockReviewTooltips.STORAGE),
                StockReviewListModel.planCell(purchase.getQuantity(), cost));
        rows.add(StockReviewListRow.weapon(WimGuiToggleHeading.label(record.getDisplayName(), expanded),
                cells, StockReviewAction.toggleItem(record.getItemKey()), StockReviewTooltips.weapon(record),
                StockReviewStyle.SECTION_INDENT));
        if (!expanded) {
            return;
        }
        StockReviewListModel.addWeaponData(rows, record, state,
                StockReviewStyle.REVIEW_ROW_RIGHT_BLOCK_WIDTH,
                StockReviewStyle.REVIEW_LIST_WIDTH,
                StockReviewStyle.DETAIL_INDENT,
                StockReviewStyle.DATA_INDENT);
    }

    private static void addWorstCaseReviewRow(List<WimGuiListRow<StockReviewAction>> rows) {
        List<WimGuiRowCell<StockReviewAction>> cells = WimGuiRowCell.of(
                WimGuiRowCell.info("Storage: 99+ [-99+]",
                        StockReviewStyle.REVIEW_STOCK_CELL_WIDTH,
                        StockReviewStyle.CELL_BACKGROUND,
                        StockReviewStyle.TEXT,
                        Alignment.LMID,
                        StockReviewTooltips.STORAGE),
                WimGuiRowCell.info("Selling: 99+ [999,999+\u00a2]",
                        StockReviewStyle.PLAN_CELL_WIDTH,
                        StockReviewStyle.PLAN_NEGATIVE,
                        StockReviewStyle.TEXT,
                        Alignment.LMID,
                        StockReviewTooltips.PLAN));
        rows.add(StockReviewListRow.weapon("Suzuki-Clapteryon Thermal Prokector... (+)",
                cells,
                StockReviewAction.debugNoop(),
                "Worst-case review-row width test sample. It does not affect trades.",
                StockReviewStyle.SECTION_INDENT));
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
