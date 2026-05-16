package weaponsprocurement.ui.stockreview.state

import weaponsprocurement.ui.stockreview.actions.*
import weaponsprocurement.ui.stockreview.state.*
import weaponsprocurement.ui.stockreview.rows.*
import weaponsprocurement.ui.stockreview.tooltips.*
import weaponsprocurement.ui.stockreview.rendering.*
import weaponsprocurement.ui.stockreview.trade.*

import weaponsprocurement.ui.*

import java.util.ArrayList

class StockReviewFilterListModel private constructor() {
    companion object {
        @JvmStatic
        fun build(state: StockReviewState): List<WimGuiListRow<StockReviewAction>> {
            val rows = ArrayList<WimGuiListRow<StockReviewAction>>()
            val active = state.getActiveFilters()
            if (active.isNotEmpty()) {
                for (filter in StockReviewFilter.values()) {
                    if (active.contains(filter)) {
                        rows.add(
                            StockReviewListRow.filter(
                                filter.label,
                                true,
                                StockReviewAction.toggleFilter(filter),
                                false,
                                StockReviewTooltips.filter(filter, true),
                            ),
                        )
                    }
                }
            }
            for (group in StockReviewFilterGroup.values()) {
                addGroup(rows, state, group, active.isNotEmpty() || group.ordinal > 0)
            }
            return rows
        }

        private fun addGroup(
            rows: MutableList<WimGuiListRow<StockReviewAction>>,
            state: StockReviewState,
            group: StockReviewFilterGroup,
            topGap: Boolean,
        ) {
            val expanded = state.isExpanded(group)
            val activeInGroup = StockReviewFilters.activeInGroup(state.getActiveFilters(), group)
            val label = WimGuiToggleHeading.countedLabel(group.label, activeInGroup.size, expanded)
            rows.add(
                StockReviewListRow.filterHeading(
                    label,
                    StockReviewAction.toggle(group),
                    topGap,
                    StockReviewTooltips.filterHeading(group),
                ),
            )
            if (!expanded) {
                return
            }
            for (filter in StockReviewFilter.values()) {
                if (group != filter.group) {
                    continue
                }
                val active = state.isFilterActive(filter)
                if (active) {
                    continue
                }
                rows.add(
                    StockReviewListRow.filter(
                        filter.label,
                        false,
                        StockReviewAction.toggleFilter(filter),
                        false,
                        StockReviewTooltips.filter(filter, false),
                    ),
                )
            }
        }
    }
}