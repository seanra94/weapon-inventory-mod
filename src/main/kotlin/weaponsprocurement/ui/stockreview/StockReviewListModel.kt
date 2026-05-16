package weaponsprocurement.ui.stockreview

import weaponsprocurement.ui.*

import weaponsprocurement.stock.item.StockCategory
import weaponsprocurement.stock.item.StockItemType
import weaponsprocurement.stock.item.StockSourceMode
import weaponsprocurement.stock.item.WeaponStockRecord
import weaponsprocurement.stock.item.WeaponStockSnapshot
import java.awt.Color
import java.util.Locale

object StockReviewListModel {
    @JvmStatic
    fun build(
        snapshot: WeaponStockSnapshot?,
        state: StockReviewState?,
        tradeContext: StockReviewTradeContext,
    ): List<WimGuiListRow<StockReviewAction>> {
        val rows = ArrayList<WimGuiListRow<StockReviewAction>>()
        var displayed = 0
        displayed += addItemType(rows, snapshot, state!!, tradeContext, StockItemType.WEAPON, false)
        displayed += addItemType(rows, snapshot, state, tradeContext, StockItemType.WING, true)
        if (displayed == 0) {
            rows.add(StockReviewListRow.empty(emptyStateMessage(snapshot, state)))
        }
        return rows
    }

    private fun emptyStateMessage(snapshot: WeaponStockSnapshot?, state: StockReviewState?): String {
        if (snapshot != null && snapshot.getTotalRecords() > 0 && state != null && state.getActiveFilterCount() > 0) {
            return "All rows are hidden by the active filters."
        }
        val sourceMode = snapshot?.getSourceMode() ?: StockSourceMode.LOCAL
        if (StockSourceMode.SECTOR == sourceMode) {
            return "No Sector Market weapon or wing stock is currently available."
        }
        if (StockSourceMode.FIXERS == sourceMode) {
            return "Fixer's Market has no eligible theoretical or observed stock, or all eligible stock is blacklisted."
        }
        return "No local weapon or wing stock is buyable here, and no player-cargo weapons or wings are available to sell."
    }

    private fun addItemType(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        snapshot: WeaponStockSnapshot?,
        state: StockReviewState,
        tradeContext: StockReviewTradeContext,
        itemType: StockItemType,
        topGap: Boolean,
    ): Int {
        val count = snapshot?.getCount(itemType) ?: 0
        val expanded = state.isExpanded(itemType)
        rows.add(
            StockReviewListRow.filterHeading(
                WimGuiToggleHeading.countedLabel(itemType.sectionLabel, count, expanded),
                StockReviewAction.toggle(itemType),
                topGap,
                "Show or hide ${itemType.sectionLabel.lowercase(Locale.US)}.",
            ),
        )
        if (!expanded) {
            return count
        }
        var displayed = 0
        displayed += addCategory(rows, snapshot, state, tradeContext, itemType, StockCategory.NO_STOCK, StockReviewStyle.NO_STOCK, false)
        displayed += addCategory(rows, snapshot, state, tradeContext, itemType, StockCategory.INSUFFICIENT, StockReviewStyle.INSUFFICIENT, true)
        displayed += addCategory(rows, snapshot, state, tradeContext, itemType, StockCategory.SUFFICIENT, StockReviewStyle.SUFFICIENT, true)
        return displayed
    }

    private fun addCategory(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        snapshot: WeaponStockSnapshot?,
        state: StockReviewState,
        tradeContext: StockReviewTradeContext,
        itemType: StockItemType,
        category: StockCategory,
        color: Color,
        topGap: Boolean,
    ): Int {
        val records = filteredRecords(
            StockReviewTradePlanner.visibleTradeableRecords(snapshot, itemType, category),
            state.getActiveFilters(),
        )
        val expanded = state.isExpanded(itemType, category)
        val label = WimGuiToggleHeading.label(categoryHeading(itemType, category, records, tradeContext), expanded)
        rows.add(
            StockReviewListRow.categoryIndented(
                label,
                color,
                StockReviewAction.toggle(itemType, category),
                topGap,
                StockReviewTooltips.category(category),
                StockReviewStyle.WEAPON_INDENT,
            ),
        )
        if (!expanded) {
            return records.size
        }
        if (StockReviewStyle.SHOW_WIDTH_TEST_ROWS && StockItemType.WEAPON == itemType && StockCategory.NO_STOCK == category) {
            addWorstCaseTestRow(rows)
        }
        for (record in records) {
            addWeapon(rows, record, state, tradeContext)
        }
        return records.size
    }

    private fun addWeapon(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        record: WeaponStockRecord,
        state: StockReviewState,
        tradeContext: StockReviewTradeContext,
    ) {
        val expanded = state.isItemExpanded(record.itemKey)
        val label = WimGuiToggleHeading.label(record.displayName, expanded)
        val planQuantity = tradeContext.netQuantityForItem(record.itemKey)
        val sellRemaining = tradeContext.negativeAdjustmentRemaining(record, Int.MAX_VALUE)
        val transactionCost = tradeContext.transactionCostForItem(record.itemKey)
        val buyStepQuantity = tradeContext.positiveAdjustmentRemaining(record, 10)
        val sellStepQuantity = minOf(10, sellRemaining)
        val sufficientDelta = tradeContext.deltaToSufficient(record)
        val cells = WimGuiRowCell.of(
            StockReviewTradeRowCells.storage(record.storageCount, planQuantity, StockReviewStyle.STOCK_CELL_WIDTH),
            StockReviewTradeRowCells.unitPrice(tradeContext.unitPriceForItem(record)),
            StockReviewTradeRowCells.plan(planQuantity, transactionCost),
            StockReviewTradeRowCells.step(
                "-",
                sellStepQuantity,
                StockReviewStyle.SELL_BUTTON,
                StockReviewAction.adjustPlan(record.itemKey, -sellStepQuantity),
                StockReviewTooltips.decreasePlan(sellStepQuantity),
            ),
            WimGuiRowCell.standardAction(
                "-1",
                StockReviewStyle.TRADE_STEP_BUTTON_WIDTH,
                StockReviewStyle.SELL_BUTTON,
                StockReviewAction.adjustPlan(record.itemKey, -1),
                sellRemaining >= 1,
                StockReviewTooltips.decreasePlan(1),
            ),
            WimGuiRowCell.standardAction(
                "+1",
                StockReviewStyle.TRADE_STEP_BUTTON_WIDTH,
                StockReviewStyle.BUY_BUTTON,
                StockReviewAction.adjustPlan(record.itemKey, 1),
                tradeContext.positiveAdjustmentRemaining(record, 1) >= 1,
                StockReviewTooltips.increasePlan(1),
            ),
            StockReviewTradeRowCells.step(
                "+",
                buyStepQuantity,
                StockReviewStyle.BUY_BUTTON,
                StockReviewAction.adjustPlan(record.itemKey, buyStepQuantity),
                StockReviewTooltips.increasePlan(buyStepQuantity),
            ),
            WimGuiRowCell.standardAction(
                "Sufficient",
                StockReviewStyle.SUFFICIENT_BUTTON_WIDTH,
                if (sufficientDelta < 0) StockReviewStyle.SELL_BUTTON else StockReviewStyle.BUY_BUTTON,
                StockReviewAction.adjustToSufficient(record.itemKey, sufficientDelta),
                sufficientDelta != 0,
                StockReviewTooltips.sufficient(record),
            ),
            WimGuiRowCell.standardAction(
                "Reset",
                StockReviewStyle.RESET_BUTTON_WIDTH,
                StockReviewStyle.ACTION_BACKGROUND,
                StockReviewAction.resetPlan(record.itemKey),
                planQuantity != 0,
                StockReviewTooltips.resetPlan(),
            ),
        )
        val itemTooltip = StockReviewTooltips.itemDataToggle(record)
        rows.add(
            StockReviewListRow.item(
                label,
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
            StockReviewStyle.TRADE_ROW_RIGHT_BLOCK_WIDTH,
            StockReviewStyle.LIST_WIDTH,
            StockReviewStyle.DETAIL_INDENT,
            StockReviewStyle.DATA_INDENT,
        )
    }

    private fun addWorstCaseTestRow(rows: MutableList<WimGuiListRow<StockReviewAction>>) {
        StockReviewTradeRowCells.addWorstCaseTradeRow(rows)
    }

    private fun filteredRecords(
        records: List<WeaponStockRecord>?,
        activeFilters: Set<StockReviewFilter>?,
    ): List<WeaponStockRecord> {
        if (records == null || records.isEmpty() || StockReviewFilters.count(activeFilters) <= 0) {
            return records ?: emptyList()
        }
        val result = ArrayList<WeaponStockRecord>()
        for (record in records) {
            if (StockReviewFilters.matches(record, activeFilters)) {
                result.add(record)
            }
        }
        return result
    }

    private fun categoryHeading(
        itemType: StockItemType,
        category: StockCategory,
        records: List<WeaponStockRecord>?,
        tradeContext: StockReviewTradeContext?,
    ): String {
        val itemTypes = records?.size ?: 0
        var selling = 0
        var buying = 0
        if (records != null && tradeContext != null) {
            for (record in records) {
                selling += tradeContext.pendingSellQuantityForItem(record.itemKey)
                buying += tradeContext.pendingBuyQuantityForItem(record.itemKey)
            }
        }
        val typeLabel = if (StockItemType.WING == itemType) "Wing Types" else "Weapon Types"
        return category.label +
            " [$typeLabel: $itemTypes]" +
            "[Selling: ${maxOf(0, selling)}]" +
            "[Buying: ${maxOf(0, buying)}]"
    }
}