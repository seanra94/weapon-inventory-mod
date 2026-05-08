package weaponinventorymod.gui;

import weaponinventorymod.core.StockCategory;
import weaponinventorymod.core.StockReviewConfig;
import weaponinventorymod.core.StockSortMode;
import weaponinventorymod.core.StockSourceMode;
import weaponinventorymod.core.StockItemType;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class StockReviewState implements WimGuiScrollableListState {
    private final Map<StockCategory, Boolean> expanded = new EnumMap<StockCategory, Boolean>(StockCategory.class);
    private final Map<StockItemType, Boolean> expandedItemTypes = new EnumMap<StockItemType, Boolean>(StockItemType.class);
    private final Map<StockItemType, Map<StockCategory, Boolean>> expandedByItemType =
            new EnumMap<StockItemType, Map<StockCategory, Boolean>>(StockItemType.class);
    private final Map<StockReviewTradeGroup, Boolean> expandedTradeGroups = new EnumMap<StockReviewTradeGroup, Boolean>(StockReviewTradeGroup.class);
    private StockSortMode sortMode;
    private boolean includeCurrentMarketStorage;
    private boolean includeBlackMarket;
    private StockSourceMode sourceMode;
    private int listScrollOffset = 0;
    private final Set<String> expandedWeapons = new HashSet<String>();
    private final Set<StockReviewFilter> activeFilters = EnumSet.noneOf(StockReviewFilter.class);
    private final Map<StockReviewFilterGroup, Boolean> expandedFilterGroups = new EnumMap<StockReviewFilterGroup, Boolean>(StockReviewFilterGroup.class);
    private String tradeWarning = "None";
    private float initialCredits = -1f;
    private float initialCargoCapacity = -1f;

    public StockReviewState(StockReviewConfig config) {
        expanded.put(StockCategory.NO_STOCK, Boolean.FALSE);
        expanded.put(StockCategory.INSUFFICIENT, Boolean.FALSE);
        expanded.put(StockCategory.SUFFICIENT, Boolean.FALSE);
        expandedItemTypes.put(StockItemType.WEAPON, Boolean.TRUE);
        expandedItemTypes.put(StockItemType.WING, Boolean.TRUE);
        initializeItemCategoryExpansion();
        expandedTradeGroups.put(StockReviewTradeGroup.BUYING, Boolean.FALSE);
        expandedTradeGroups.put(StockReviewTradeGroup.SELLING, Boolean.FALSE);
        for (StockReviewFilterGroup group : StockReviewFilterGroup.values()) {
            expandedFilterGroups.put(group, Boolean.FALSE);
        }
        this.sortMode = config.getSortMode();
        this.includeCurrentMarketStorage = config.isIncludeCurrentMarketStorage();
        this.includeBlackMarket = config.isIncludeBlackMarket();
        this.sourceMode = StockSourceMode.LOCAL;
    }

    public StockReviewState(StockReviewState source) {
        expanded.putAll(source.expanded);
        expandedItemTypes.putAll(source.expandedItemTypes);
        copyItemCategoryExpansion(source.expandedByItemType);
        expandedTradeGroups.putAll(source.expandedTradeGroups);
        this.sortMode = source.sortMode;
        this.includeCurrentMarketStorage = source.includeCurrentMarketStorage;
        this.includeBlackMarket = source.includeBlackMarket;
        this.sourceMode = source.sourceMode;
        this.listScrollOffset = source.listScrollOffset;
        this.expandedWeapons.addAll(source.expandedWeapons);
        this.activeFilters.addAll(source.activeFilters);
        this.expandedFilterGroups.putAll(source.expandedFilterGroups);
        this.tradeWarning = source.tradeWarning;
        this.initialCredits = source.initialCredits;
        this.initialCargoCapacity = source.initialCargoCapacity;
    }

    public boolean isExpanded(StockCategory category) {
        Boolean value = expanded.get(category);
        return value != null && value.booleanValue();
    }

    public void toggle(StockCategory category) {
        expanded.put(category, Boolean.valueOf(!isExpanded(category)));
    }

    public boolean isExpanded(StockItemType itemType, StockCategory category) {
        Map<StockCategory, Boolean> byCategory = expandedByItemType.get(itemType);
        Boolean value = byCategory == null ? null : byCategory.get(category);
        return value != null && value.booleanValue();
    }

    public void toggle(StockItemType itemType, StockCategory category) {
        if (itemType == null) {
            toggle(category);
            return;
        }
        Map<StockCategory, Boolean> byCategory = expandedByItemType.get(itemType);
        if (byCategory == null) {
            byCategory = new EnumMap<StockCategory, Boolean>(StockCategory.class);
            expandedByItemType.put(itemType, byCategory);
        }
        byCategory.put(category, Boolean.valueOf(!isExpanded(itemType, category)));
    }

    public boolean isExpanded(StockItemType itemType) {
        Boolean value = expandedItemTypes.get(itemType);
        return value != null && value.booleanValue();
    }

    public void toggle(StockItemType itemType) {
        expandedItemTypes.put(itemType, Boolean.valueOf(!isExpanded(itemType)));
    }

    public boolean isExpanded(StockReviewTradeGroup tradeGroup) {
        Boolean value = expandedTradeGroups.get(tradeGroup);
        return value != null && value.booleanValue();
    }

    public void toggle(StockReviewTradeGroup tradeGroup) {
        expandedTradeGroups.put(tradeGroup, Boolean.valueOf(!isExpanded(tradeGroup)));
    }

    public void setExpanded(StockReviewTradeGroup tradeGroup, boolean value) {
        if (tradeGroup != null) {
            expandedTradeGroups.put(tradeGroup, Boolean.valueOf(value));
        }
    }

    public boolean isWeaponExpanded(String weaponId) {
        return expandedWeapons.contains(weaponId);
    }

    public void toggleWeapon(String weaponId) {
        toggleSet(expandedWeapons, weaponId);
    }

    public boolean isFilterActive(StockReviewFilter filter) {
        return activeFilters.contains(filter);
    }

    public void toggleFilter(StockReviewFilter filter) {
        if (filter == null) {
            return;
        }
        if (activeFilters.contains(filter)) {
            activeFilters.remove(filter);
        } else {
            activeFilters.add(filter);
        }
        listScrollOffset = 0;
    }

    public Set<StockReviewFilter> getActiveFilters() {
        return EnumSet.copyOf(activeFilters);
    }

    public int getActiveFilterCount() {
        return activeFilters.size();
    }

    public void clearFilters() {
        activeFilters.clear();
        listScrollOffset = 0;
    }

    public boolean isExpanded(StockReviewFilterGroup group) {
        Boolean value = expandedFilterGroups.get(group);
        return value != null && value.booleanValue();
    }

    public void toggle(StockReviewFilterGroup group) {
        expandedFilterGroups.put(group, Boolean.valueOf(!isExpanded(group)));
    }

    public StockSortMode getSortMode() {
        return sortMode;
    }

    public void cycleSortMode() {
        sortMode = sortMode.next();
    }

    public boolean isIncludeCurrentMarketStorage() {
        return includeCurrentMarketStorage;
    }

    public boolean isIncludeBlackMarket() {
        return !getSourceMode().isRemote() && includeBlackMarket;
    }

    public void toggleBlackMarket() {
        if (getSourceMode().isRemote()) {
            includeBlackMarket = false;
            return;
        }
        includeBlackMarket = !includeBlackMarket;
    }

    public StockSourceMode getSourceMode() {
        StockSourceMode resolved = sourceMode == null ? StockSourceMode.LOCAL : sourceMode;
        if (!resolved.isEnabled()) {
            sourceMode = StockSourceMode.LOCAL;
            includeBlackMarket = false;
            return sourceMode;
        }
        return resolved;
    }

    public void cycleSourceMode() {
        sourceMode = getSourceMode().next();
        if (sourceMode.isRemote()) {
            includeBlackMarket = false;
        }
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

    private static void toggleSet(Set<String> set, String key) {
        if (key == null || key.isEmpty()) {
            return;
        }
        if (set.contains(key)) {
            set.remove(key);
        } else {
            set.add(key);
        }
    }

    private void initializeItemCategoryExpansion() {
        for (StockItemType itemType : StockItemType.values()) {
            Map<StockCategory, Boolean> byCategory = new EnumMap<StockCategory, Boolean>(StockCategory.class);
            for (StockCategory category : StockCategory.values()) {
                byCategory.put(category, Boolean.FALSE);
            }
            expandedByItemType.put(itemType, byCategory);
        }
    }

    private void copyItemCategoryExpansion(Map<StockItemType, Map<StockCategory, Boolean>> source) {
        if (source == null || source.isEmpty()) {
            initializeItemCategoryExpansion();
            return;
        }
        for (StockItemType itemType : StockItemType.values()) {
            Map<StockCategory, Boolean> sourceByCategory = source.get(itemType);
            Map<StockCategory, Boolean> byCategory = new EnumMap<StockCategory, Boolean>(StockCategory.class);
            for (StockCategory category : StockCategory.values()) {
                Boolean value = sourceByCategory == null ? null : sourceByCategory.get(category);
                byCategory.put(category, Boolean.valueOf(value != null && value.booleanValue()));
            }
            expandedByItemType.put(itemType, byCategory);
        }
    }
}
