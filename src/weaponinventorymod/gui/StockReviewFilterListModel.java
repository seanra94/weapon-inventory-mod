package weaponinventorymod.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class StockReviewFilterListModel {
    private StockReviewFilterListModel() {
    }

    static List<WimGuiListRow<StockReviewAction>> build(StockReviewState state) {
        List<WimGuiListRow<StockReviewAction>> rows = new ArrayList<WimGuiListRow<StockReviewAction>>();
        Set<StockReviewFilter> active = state.getActiveFilters();
        if (!active.isEmpty()) {
            rows.add(StockReviewListRow.filterHeading(
                    WimGuiToggleHeading.countedLabel("Active Filters", active.size(), true),
                    StockReviewAction.resetFilters(),
                    false));
            for (StockReviewFilter filter : StockReviewFilter.values()) {
                if (active.contains(filter)) {
                    rows.add(StockReviewListRow.filter(filter.getLabel(), true,
                            StockReviewAction.toggleFilter(filter), false));
                }
            }
        }
        for (StockReviewFilterGroup group : StockReviewFilterGroup.values()) {
            addGroup(rows, state, group, !active.isEmpty() || group.ordinal() > 0);
        }
        return rows;
    }

    private static void addGroup(List<WimGuiListRow<StockReviewAction>> rows,
                                 StockReviewState state,
                                 StockReviewFilterGroup group,
                                 boolean topGap) {
        boolean expanded = state.isExpanded(group);
        Set<StockReviewFilter> activeInGroup = StockReviewFilters.activeInGroup(state.getActiveFilters(), group);
        String label = WimGuiToggleHeading.countedLabel(group.getLabel(), activeInGroup.size(), expanded);
        rows.add(StockReviewListRow.filterHeading(label, StockReviewAction.toggle(group), topGap));
        if (!expanded) {
            return;
        }
        for (StockReviewFilter filter : StockReviewFilter.values()) {
            if (!group.equals(filter.getGroup())) {
                continue;
            }
            boolean active = state.isFilterActive(filter);
            rows.add(StockReviewListRow.filter(filter.getLabel(), active,
                    StockReviewAction.toggleFilter(filter), false));
        }
    }
}
