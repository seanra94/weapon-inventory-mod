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
                                          List<StockReviewPendingTrade> pendingTrades,
                                          StockReviewState state,
                                          StockReviewTradeContext tradeContext) {
        List<WimGuiListRow<StockReviewAction>> rows = new ArrayList<WimGuiListRow<StockReviewAction>>();
        if (pendingTrades == null || pendingTrades.isEmpty()) {
            rows.add(StockReviewListRow.empty("No trades are planned."));
            return rows;
        }
        List<StockReviewPendingTrade> buying = reviewTradesForGroup(pendingTrades, StockReviewTradeGroup.BUYING);
        List<StockReviewPendingTrade> selling = reviewTradesForGroup(pendingTrades, StockReviewTradeGroup.SELLING);
        if (buying.isEmpty() && selling.isEmpty()) {
            rows.add(StockReviewListRow.empty("No trades are planned."));
            return rows;
        }
        addReviewGroup(rows, snapshot, buying, state, tradeContext, StockReviewTradeGroup.BUYING);
        addReviewGroup(rows, snapshot, selling, state, tradeContext, StockReviewTradeGroup.SELLING);
        return rows;
    }

    private static void addReviewGroup(List<WimGuiListRow<StockReviewAction>> rows,
                                       WeaponStockSnapshot snapshot,
                                       List<StockReviewPendingTrade> groupTrades,
                                       StockReviewState state,
                                       StockReviewTradeContext tradeContext,
                                       StockReviewTradeGroup tradeGroup) {
        boolean expanded = state.isExpanded(tradeGroup);
        String label = WimGuiToggleHeading.countedLabel(tradeGroup.getLabel(), groupTrades.size(), expanded);
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
        for (int i = 0; i < groupTrades.size(); i++) {
            addReviewTrade(rows, snapshot, groupTrades.get(i), state, tradeContext);
        }
    }

    private static void addReviewTrade(List<WimGuiListRow<StockReviewAction>> rows,
                                       WeaponStockSnapshot snapshot,
                                       StockReviewPendingTrade trade,
                                       StockReviewState state,
                                       StockReviewTradeContext tradeContext) {
        WeaponStockRecord record = snapshot.getRecord(trade.getItemKey());
        if (record == null) {
            rows.add(StockReviewListRow.review(trade.getItemKey()));
            return;
        }
        boolean expanded = state.isItemExpanded(record.getItemKey());
        int cost = tradeContext.transactionCostForLine(trade.getItemKey(), trade.getSubmarketId());
        List<WimGuiRowCell<StockReviewAction>> cells = WimGuiRowCell.of(
                StockReviewTradeRowCells.storage(record.getStorageCount(), trade.getQuantity(), StockReviewStyle.REVIEW_STOCK_CELL_WIDTH),
                StockReviewTradeRowCells.plan(trade.getQuantity(), cost));
        rows.add(StockReviewListRow.item(WimGuiToggleHeading.label(record.getDisplayName(), expanded),
                cells, StockReviewAction.toggleItem(record.getItemKey()), StockReviewTooltips.itemDataToggle(record),
                StockReviewStyle.SECTION_INDENT));
        if (!expanded) {
            return;
        }
        StockReviewItemInfoRows.add(rows, record, state,
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
        rows.add(StockReviewListRow.item("Suzuki-Clapteryon Thermal Prokector... (+)",
                cells,
                StockReviewAction.debugNoop(),
                "Worst-case review-row width test sample. It does not affect trades.",
                StockReviewStyle.SECTION_INDENT));
    }

    private static List<StockReviewPendingTrade> reviewTradesForGroup(List<StockReviewPendingTrade> pendingTrades,
                                                                            StockReviewTradeGroup tradeGroup) {
        List<StockReviewPendingTrade> result = new ArrayList<StockReviewPendingTrade>();
        if (pendingTrades == null) {
            return result;
        }
        for (int i = 0; i < pendingTrades.size(); i++) {
            StockReviewPendingTrade trade = pendingTrades.get(i);
            if (StockReviewTradeGroup.BUYING.equals(tradeGroup) && trade.isBuy()) {
                result.add(trade);
            } else if (StockReviewTradeGroup.SELLING.equals(tradeGroup) && trade.isSell()) {
                result.add(trade);
            }
        }
        return result;
    }
}
