package weaponsprocurement.gui;

import weaponsprocurement.core.WeaponStockRecord;

import java.util.EnumSet;
import java.util.Set;

final class StockReviewFilters {
    private StockReviewFilters() {
    }

    static boolean matches(WeaponStockRecord record, Set<StockReviewFilter> activeFilters) {
        if (activeFilters == null || activeFilters.isEmpty()) {
            return true;
        }
        if (record != null && record.isWing()) {
            return true;
        }
        for (StockReviewFilterGroup group : StockReviewFilterGroup.values()) {
            if (!matchesGroup(record, activeFilters, group)) {
                return false;
            }
        }
        return true;
    }

    static int count(Set<StockReviewFilter> activeFilters) {
        return activeFilters == null ? 0 : activeFilters.size();
    }

    static Set<StockReviewFilter> activeInGroup(Set<StockReviewFilter> activeFilters, StockReviewFilterGroup group) {
        Set<StockReviewFilter> result = EnumSet.noneOf(StockReviewFilter.class);
        if (activeFilters == null || group == null) {
            return result;
        }
        for (StockReviewFilter filter : activeFilters) {
            if (filter != null && group.equals(filter.getGroup())) {
                result.add(filter);
            }
        }
        return result;
    }

    private static boolean matchesGroup(WeaponStockRecord record,
                                        Set<StockReviewFilter> activeFilters,
                                        StockReviewFilterGroup group) {
        Set<StockReviewFilter> groupFilters = activeInGroup(activeFilters, group);
        if (groupFilters.isEmpty()) {
            return true;
        }
        for (StockReviewFilter filter : groupFilters) {
            if (filter.matches(record)) {
                return true;
            }
        }
        return false;
    }
}
