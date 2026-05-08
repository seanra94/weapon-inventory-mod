package weaponsprocurement.gui;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

final class StockReviewFilterState {
    private final Set<StockReviewFilter> activeFilters = EnumSet.noneOf(StockReviewFilter.class);
    private final Map<StockReviewFilterGroup, Boolean> expandedFilterGroups =
            new EnumMap<StockReviewFilterGroup, Boolean>(StockReviewFilterGroup.class);

    StockReviewFilterState() {
        initializeFilterGroups();
    }

    StockReviewFilterState(StockReviewFilterState source) {
        activeFilters.addAll(source.activeFilters);
        expandedFilterGroups.putAll(source.expandedFilterGroups);
        ensureFilterGroupsInitialized();
    }

    boolean isFilterActive(StockReviewFilter filter) {
        return activeFilters.contains(filter);
    }

    void toggleFilter(StockReviewFilter filter) {
        if (filter == null) {
            return;
        }
        if (activeFilters.contains(filter)) {
            activeFilters.remove(filter);
        } else {
            activeFilters.add(filter);
        }
    }

    Set<StockReviewFilter> getActiveFilters() {
        return EnumSet.copyOf(activeFilters);
    }

    int getActiveFilterCount() {
        return activeFilters.size();
    }

    void clearFilters() {
        activeFilters.clear();
    }

    boolean isExpanded(StockReviewFilterGroup group) {
        Boolean value = expandedFilterGroups.get(group);
        return value != null && value.booleanValue();
    }

    void toggle(StockReviewFilterGroup group) {
        if (group != null) {
            expandedFilterGroups.put(group, Boolean.valueOf(!isExpanded(group)));
        }
    }

    private void initializeFilterGroups() {
        for (StockReviewFilterGroup group : StockReviewFilterGroup.values()) {
            expandedFilterGroups.put(group, Boolean.FALSE);
        }
    }

    private void ensureFilterGroupsInitialized() {
        for (StockReviewFilterGroup group : StockReviewFilterGroup.values()) {
            if (!expandedFilterGroups.containsKey(group)) {
                expandedFilterGroups.put(group, Boolean.FALSE);
            }
        }
    }
}
