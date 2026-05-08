package weaponsprocurement.gui;

import weaponsprocurement.core.StockCategory;
import weaponsprocurement.core.StockReviewConfig;
import weaponsprocurement.core.StockSortMode;
import weaponsprocurement.core.StockSourceMode;
import weaponsprocurement.core.StockItemType;

import java.util.Set;

public final class StockReviewState implements WimGuiScrollableListState {
    private final StockReviewExpansionState expansion;
    private final StockReviewFilterState filters;
    private final StockReviewSourceState source;
    private int listScrollOffset = 0;
    private String tradeWarning = "None";
    private float initialCredits = -1f;
    private float initialCargoCapacity = -1f;

    public StockReviewState(StockReviewConfig config) {
        this.expansion = new StockReviewExpansionState();
        this.filters = new StockReviewFilterState();
        this.source = new StockReviewSourceState(config);
    }

    public StockReviewState(StockReviewState source) {
        this.expansion = new StockReviewExpansionState(source.expansion);
        this.filters = new StockReviewFilterState(source.filters);
        this.source = new StockReviewSourceState(source.source);
        this.listScrollOffset = source.listScrollOffset;
        this.tradeWarning = source.tradeWarning;
        this.initialCredits = source.initialCredits;
        this.initialCargoCapacity = source.initialCargoCapacity;
    }

    public boolean isExpanded(StockCategory category) {
        return expansion.isExpanded(category);
    }

    public void toggle(StockCategory category) {
        expansion.toggle(category);
    }

    public boolean isExpanded(StockItemType itemType, StockCategory category) {
        return expansion.isExpanded(itemType, category);
    }

    public void toggle(StockItemType itemType, StockCategory category) {
        expansion.toggle(itemType, category);
    }

    public boolean isExpanded(StockItemType itemType) {
        return expansion.isExpanded(itemType);
    }

    public void toggle(StockItemType itemType) {
        expansion.toggle(itemType);
    }

    public boolean isExpanded(StockReviewTradeGroup tradeGroup) {
        return expansion.isExpanded(tradeGroup);
    }

    public void toggle(StockReviewTradeGroup tradeGroup) {
        expansion.toggle(tradeGroup);
    }

    public void setExpanded(StockReviewTradeGroup tradeGroup, boolean value) {
        expansion.setExpanded(tradeGroup, value);
    }

    public boolean isItemExpanded(String itemKey) {
        return expansion.isItemExpanded(itemKey);
    }

    public void toggleItem(String itemKey) {
        expansion.toggleItem(itemKey);
    }

    public boolean isFilterActive(StockReviewFilter filter) {
        return filters.isFilterActive(filter);
    }

    public void toggleFilter(StockReviewFilter filter) {
        filters.toggleFilter(filter);
        listScrollOffset = 0;
    }

    public Set<StockReviewFilter> getActiveFilters() {
        return filters.getActiveFilters();
    }

    public int getActiveFilterCount() {
        return filters.getActiveFilterCount();
    }

    public void clearFilters() {
        filters.clearFilters();
        listScrollOffset = 0;
    }

    public boolean isExpanded(StockReviewFilterGroup group) {
        return filters.isExpanded(group);
    }

    public void toggle(StockReviewFilterGroup group) {
        filters.toggle(group);
    }

    public StockSortMode getSortMode() {
        return source.getSortMode();
    }

    public void cycleSortMode() {
        source.cycleSortMode();
    }

    public boolean isIncludeCurrentMarketStorage() {
        return source.isIncludeCurrentMarketStorage();
    }

    public boolean isIncludeBlackMarket() {
        return source.isIncludeBlackMarket();
    }

    public void toggleBlackMarket() {
        source.toggleBlackMarket();
    }

    public StockSourceMode getSourceMode() {
        return source.getSourceMode();
    }

    public void cycleSourceMode() {
        source.cycleSourceMode();
    }

    public int getListScrollOffset() {
        return listScrollOffset;
    }

    @Override
    public void setListScrollOffset(int listScrollOffset) {
        this.listScrollOffset = Math.max(0, listScrollOffset);
    }

    public void adjustListScrollOffset(int delta, int maxOffset) {
        listScrollOffset = WimGuiScroll.usefulOffsetByDelta(listScrollOffset, delta, Math.max(0, maxOffset));
    }

    public String getTradeWarning() {
        return tradeWarning;
    }

    public void setTradeWarning(String tradeWarning) {
        this.tradeWarning = tradeWarning == null || tradeWarning.isEmpty() ? "None" : tradeWarning;
    }

    public float getInitialCredits() {
        return initialCredits;
    }

    public void setInitialCreditsIfUnset(float initialCredits) {
        if (this.initialCredits < 0f) {
            this.initialCredits = Math.max(0f, initialCredits);
        }
    }

    public float getInitialCargoCapacity() {
        return initialCargoCapacity;
    }

    public void setInitialCargoCapacityIfUnset(float initialCargoCapacity) {
        if (this.initialCargoCapacity < 0f) {
            this.initialCargoCapacity = Math.max(0f, initialCargoCapacity);
        }
    }

}
