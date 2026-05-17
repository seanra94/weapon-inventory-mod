package weaponsprocurement.ui.stockreview.state



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

    fun toggleFilter(filter: StockReviewFilter?): Boolean {
        if (filter == null) {
            return false
        }
        if (activeFilters.contains(filter)) {
            activeFilters.remove(filter)
        } else {
            activeFilters.add(filter)
        }
        return true
    }

    fun getActiveFilters(): Set<StockReviewFilter> = EnumSet.copyOf(activeFilters)

    fun getActiveFilterCount(): Int = activeFilters.size

    fun clearFilters(): Boolean {
        if (activeFilters.isEmpty()) return false
        activeFilters.clear()
        return true
    }

    fun isExpanded(group: StockReviewFilterGroup?): Boolean = expandedFilterGroups[group] == true

    fun toggle(group: StockReviewFilterGroup?): Boolean {
        if (group == null) return false
        expandedFilterGroups[group] = !isExpanded(group)
        return true
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
