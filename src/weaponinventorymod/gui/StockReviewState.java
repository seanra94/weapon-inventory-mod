package weaponinventorymod.gui;

import weaponinventorymod.core.StockCategory;
import weaponinventorymod.core.StockDisplayMode;
import weaponinventorymod.core.StockReviewConfig;
import weaponinventorymod.core.StockSortMode;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class StockReviewState implements WimGuiScrollableListState {
    private final Map<StockCategory, Boolean> expanded = new EnumMap<StockCategory, Boolean>(StockCategory.class);
    private final Map<StockReviewTradeGroup, Boolean> expandedTradeGroups = new EnumMap<StockReviewTradeGroup, Boolean>(StockReviewTradeGroup.class);
    private StockDisplayMode displayMode;
    private StockSortMode sortMode;
    private boolean includeCurrentMarketStorage;
    private boolean includeBlackMarket;
    private int listScrollOffset = 0;
    private final Set<String> expandedWeapons = new HashSet<String>();
    private final Set<String> expandedWeaponData = new HashSet<String>();
    private final Set<String> expandedSellers = new HashSet<String>();

    public StockReviewState(StockReviewConfig config) {
        expanded.put(StockCategory.NO_STOCK, Boolean.FALSE);
        expanded.put(StockCategory.INSUFFICIENT, Boolean.FALSE);
        expanded.put(StockCategory.SUFFICIENT, Boolean.FALSE);
        expandedTradeGroups.put(StockReviewTradeGroup.BUYING, Boolean.FALSE);
        expandedTradeGroups.put(StockReviewTradeGroup.SELLING, Boolean.FALSE);
        this.displayMode = config.getDisplayMode();
        this.sortMode = config.getSortMode();
        this.includeCurrentMarketStorage = config.isIncludeCurrentMarketStorage();
        this.includeBlackMarket = config.isIncludeBlackMarket();
    }

    public StockReviewState(StockReviewState source) {
        expanded.putAll(source.expanded);
        expandedTradeGroups.putAll(source.expandedTradeGroups);
        this.displayMode = source.displayMode;
        this.sortMode = source.sortMode;
        this.includeCurrentMarketStorage = source.includeCurrentMarketStorage;
        this.includeBlackMarket = source.includeBlackMarket;
        this.listScrollOffset = source.listScrollOffset;
        this.expandedWeapons.addAll(source.expandedWeapons);
        this.expandedWeaponData.addAll(source.expandedWeaponData);
        this.expandedSellers.addAll(source.expandedSellers);
    }

    public boolean isExpanded(StockCategory category) {
        Boolean value = expanded.get(category);
        return value != null && value.booleanValue();
    }

    public void toggle(StockCategory category) {
        expanded.put(category, Boolean.valueOf(!isExpanded(category)));
    }

    public boolean isExpanded(StockReviewTradeGroup tradeGroup) {
        Boolean value = expandedTradeGroups.get(tradeGroup);
        return value != null && value.booleanValue();
    }

    public void toggle(StockReviewTradeGroup tradeGroup) {
        expandedTradeGroups.put(tradeGroup, Boolean.valueOf(!isExpanded(tradeGroup)));
    }

    public boolean isWeaponExpanded(String weaponId) {
        return expandedWeapons.contains(weaponId);
    }

    public void toggleWeapon(String weaponId) {
        toggleSet(expandedWeapons, weaponId);
    }

    public boolean isWeaponDataExpanded(String weaponId) {
        return expandedWeaponData.contains(weaponId);
    }

    public boolean isSellersExpanded(String weaponId) {
        return expandedSellers.contains(weaponId);
    }

    public void toggleWeaponSection(String weaponId, StockReviewSection section) {
        if (StockReviewSection.WEAPON_DATA.equals(section)) {
            toggleSet(expandedWeaponData, weaponId);
        } else if (StockReviewSection.SELLERS.equals(section)) {
            toggleSet(expandedSellers, weaponId);
        }
    }

    public StockDisplayMode getDisplayMode() {
        return displayMode;
    }

    public void cycleDisplayMode() {
        displayMode = displayMode.next();
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

    public void toggleCurrentMarketStorage() {
        includeCurrentMarketStorage = !includeCurrentMarketStorage;
    }

    public boolean isIncludeBlackMarket() {
        return includeBlackMarket;
    }

    public void toggleBlackMarket() {
        includeBlackMarket = !includeBlackMarket;
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
}
