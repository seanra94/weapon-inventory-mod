package weaponsprocurement.ui.stockreview.state

import weaponsprocurement.ui.stockreview.actions.*
import weaponsprocurement.ui.stockreview.state.*
import weaponsprocurement.ui.stockreview.rows.*
import weaponsprocurement.ui.stockreview.tooltips.*
import weaponsprocurement.ui.stockreview.rendering.*
import weaponsprocurement.ui.stockreview.trade.*

import weaponsprocurement.ui.*

import weaponsprocurement.stock.item.WeaponStockRecord
import java.util.EnumSet

class StockReviewFilters private constructor() {
    companion object {
        @JvmStatic
        fun matches(record: WeaponStockRecord?, activeFilters: Set<StockReviewFilter>?): Boolean {
            if (activeFilters.isNullOrEmpty()) {
                return true
            }
            if (record != null && record.isWing()) {
                return true
            }
            for (group in StockReviewFilterGroup.values()) {
                if (!matchesGroup(record, activeFilters, group)) {
                    return false
                }
            }
            return true
        }

        @JvmStatic
        fun count(activeFilters: Set<StockReviewFilter>?): Int = activeFilters?.size ?: 0

        @JvmStatic
        fun activeInGroup(activeFilters: Set<StockReviewFilter>?, group: StockReviewFilterGroup?): Set<StockReviewFilter> {
            val result = EnumSet.noneOf(StockReviewFilter::class.java)
            if (activeFilters == null || group == null) {
                return result
            }
            for (filter in activeFilters) {
                if (group == filter.group) {
                    result.add(filter)
                }
            }
            return result
        }

        private fun matchesGroup(
            record: WeaponStockRecord?,
            activeFilters: Set<StockReviewFilter>,
            group: StockReviewFilterGroup,
        ): Boolean {
            val groupFilters = activeInGroup(activeFilters, group)
            if (groupFilters.isEmpty()) {
                return true
            }
            for (filter in groupFilters) {
                if (filter.matches(record)) {
                    return true
                }
            }
            return false
        }
    }
}