package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.WimGuiRowCell
import weaponsprocurement.ui.WimGuiToggleHeading
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import weaponsprocurement.ui.stockreview.state.StockReviewState
import weaponsprocurement.ui.stockreview.tooltips.StockReviewItemTooltip
import weaponsprocurement.ui.stockreview.tooltips.StockReviewTooltips
import weaponsprocurement.ui.stockreview.trade.StockReviewPendingTrade
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeContext
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeGroup
import com.fs.starfarer.api.ui.Alignment
import weaponsprocurement.stock.item.WeaponStockRecord
import weaponsprocurement.stock.item.WeaponStockSnapshot
import java.awt.Color
import java.util.ArrayList
import java.util.Locale

class StockReviewReviewListModel private constructor() {
    companion object {
        @JvmStatic
        fun build(
            snapshot: WeaponStockSnapshot,
            pendingTrades: List<StockReviewPendingTrade>?,
            state: StockReviewState,
            tradeContext: StockReviewTradeContext,
        ): List<WimGuiListRow<StockReviewAction>> {
            val rows = ArrayList<WimGuiListRow<StockReviewAction>>()
            if (pendingTrades.isNullOrEmpty()) {
                rows.add(StockReviewListRow.empty("No trades are planned."))
                return rows
            }
            val buying = reviewTradesForGroup(pendingTrades, StockReviewTradeGroup.BUYING)
            val selling = reviewTradesForGroup(pendingTrades, StockReviewTradeGroup.SELLING)
            if (buying.isEmpty() && selling.isEmpty()) {
                rows.add(StockReviewListRow.empty("No trades are planned."))
                return rows
            }
            addReviewGroup(rows, snapshot, buying, state, tradeContext, StockReviewTradeGroup.BUYING)
            addReviewGroup(rows, snapshot, selling, state, tradeContext, StockReviewTradeGroup.SELLING)
            return rows
        }

        private fun addReviewGroup(
            rows: MutableList<WimGuiListRow<StockReviewAction>>,
            snapshot: WeaponStockSnapshot,
            groupTrades: List<StockReviewPendingTrade>,
            state: StockReviewState,
            tradeContext: StockReviewTradeContext,
            tradeGroup: StockReviewTradeGroup,
        ) {
            val expanded = state.isExpanded(tradeGroup)
            val label = WimGuiToggleHeading.countedLabel(tradeGroup.label, groupTrades.size, expanded)
            val headingColor: Color = if (StockReviewTradeGroup.BUYING == tradeGroup) StockReviewStyle.CONFIRM_BUTTON else StockReviewStyle.CANCEL_BUTTON
            rows.add(
                StockReviewListRow.category(
                    label,
                    headingColor,
                    StockReviewAction.toggle(tradeGroup),
                    StockReviewTradeGroup.SELLING == tradeGroup,
                    "Show or hide queued ${tradeGroup.label.lowercase(Locale.US)} trades.",
                ),
            )
            if (!expanded) {
                return
            }
            if (StockReviewStyle.SHOW_WIDTH_TEST_ROWS && StockReviewTradeGroup.SELLING == tradeGroup) {
                addWorstCaseReviewRow(rows)
            }
            for (trade in groupTrades) {
                addReviewTrade(rows, snapshot, trade, state, tradeContext)
            }
        }

        private fun addReviewTrade(
            rows: MutableList<WimGuiListRow<StockReviewAction>>,
            snapshot: WeaponStockSnapshot,
            trade: StockReviewPendingTrade,
            state: StockReviewState,
            tradeContext: StockReviewTradeContext,
        ) {
            val record: WeaponStockRecord? = snapshot.getRecord(trade.itemKey)
            if (record == null) {
                rows.add(StockReviewListRow.review(trade.itemKey))
                return
            }
            val expanded = state.isItemExpanded(record.itemKey)
            val cost = tradeContext.transactionCostForLine(trade.itemKey, trade.submarketId)
            val cells = WimGuiRowCell.of(
                StockReviewTradeRowCells.storage(record.storageCount, trade.quantity, StockReviewStyle.REVIEW_STOCK_CELL_WIDTH),
                StockReviewTradeRowCells.plan(trade.quantity, cost),
            )
            val itemTooltip = StockReviewTooltips.itemDataToggle(record)
            rows.add(
                StockReviewListRow.item(
                    WimGuiToggleHeading.label(record.displayName, expanded),
                    cells,
                    StockReviewAction.toggleItem(record.itemKey),
                    itemTooltip,
                    StockReviewItemTooltip.forRecord(record, itemTooltip),
                    StockReviewStyle.SECTION_INDENT,
                    StockReviewRowIcon.weapon(record),
                ),
            )
            if (!expanded) {
                return
            }
            StockReviewItemInfoRows.add(
                rows,
                record,
                state,
                StockReviewStyle.REVIEW_ROW_RIGHT_BLOCK_WIDTH,
                StockReviewStyle.REVIEW_LIST_WIDTH,
                StockReviewStyle.DETAIL_INDENT,
                StockReviewStyle.DATA_INDENT,
            )
        }

        private fun addWorstCaseReviewRow(rows: MutableList<WimGuiListRow<StockReviewAction>>) {
            val cells = WimGuiRowCell.of(
                WimGuiRowCell.info<StockReviewAction>("Storage: 99+ [-99+]", StockReviewStyle.REVIEW_STOCK_CELL_WIDTH, StockReviewStyle.CELL_BACKGROUND, StockReviewStyle.TEXT, Alignment.LMID, StockReviewTooltips.STORAGE),
                WimGuiRowCell.info<StockReviewAction>("Selling: 99+ [999,999+\u00a2]", StockReviewStyle.PLAN_CELL_WIDTH, StockReviewStyle.PLAN_NEGATIVE, StockReviewStyle.TEXT, Alignment.LMID, StockReviewTooltips.PLAN),
            )
            rows.add(
                StockReviewListRow.item(
                    "Suzuki-Clapteryon Thermal Prokector... (+)",
                    cells,
                    StockReviewAction.debugNoop(),
                    "Worst-case review-row width test sample. It does not affect trades.",
                    StockReviewStyle.SECTION_INDENT,
                ),
            )
        }

        private fun reviewTradesForGroup(
            pendingTrades: List<StockReviewPendingTrade>?,
            tradeGroup: StockReviewTradeGroup,
        ): List<StockReviewPendingTrade> {
            val result = ArrayList<StockReviewPendingTrade>()
            if (pendingTrades == null) {
                return result
            }
            for (trade in pendingTrades) {
                if (StockReviewTradeGroup.BUYING == tradeGroup && trade.isBuy()) {
                    result.add(trade)
                } else if (StockReviewTradeGroup.SELLING == tradeGroup && trade.isSell()) {
                    result.add(trade)
                }
            }
            return result
        }
    }
}