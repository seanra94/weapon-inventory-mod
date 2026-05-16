package weaponsprocurement.gui

import java.util.EnumMap
import java.util.EnumSet

class StockReviewFilterState {
    private val activeFilters: EnumSet<StockReviewFilter> = EnumSet.noneOf(StockReviewFilter::class.java)
    private val expandedFilterGroups: EnumMap<StockReviewFilterGroup, Boolean> =
        EnumMap(StockReviewFilterGroup::class.java)

    constructor() {
        initializeFilterGroups()
    }

    constructor(source: StockReviewFilterState) {
        activeFilters.addAll(source.activeFilters)
        expandedFilterGroups.putAll(source.expandedFilterGroups)
        ensureFilterGroupsInitialized()
    }

    fun isFilterActive(filter: StockReviewFilter?): Boolean = activeFilters.contains(filter)

    fun toggleFilter(filter: StockReviewFilter?) {
        if (filter == null) {
            return
        }
        if (activeFilters.contains(filter)) {
            activeFilters.remove(filter)
        } else {
            activeFilters.add(filter)
        }
    }

    fun getActiveFilters(): Set<StockReviewFilter> = EnumSet.copyOf(activeFilters)

    fun getActiveFilterCount(): Int = activeFilters.size

    fun clearFilters() {
        activeFilters.clear()
    }

    fun isExpanded(group: StockReviewFilterGroup?): Boolean = expandedFilterGroups[group] == true

    fun toggle(group: StockReviewFilterGroup?) {
        if (group != null) {
            expandedFilterGroups[group] = !isExpanded(group)
        }
    }

    private fun initializeFilterGroups() {
        for (group in StockReviewFilterGroup.values()) {
            expandedFilterGroups[group] = false
        }
    }

    private fun ensureFilterGroupsInitialized() {
        for (group in StockReviewFilterGroup.values()) {
            if (!expandedFilterGroups.containsKey(group)) {
                expandedFilterGroups[group] = false
            }
        }
    }
}
