package weaponsprocurement.gui;

import weaponsprocurement.core.StockCategory;
import weaponsprocurement.core.StockItemType;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class StockReviewExpansionState {
    private final Map<StockCategory, Boolean> expanded = new EnumMap<StockCategory, Boolean>(StockCategory.class);
    private final Map<StockItemType, Boolean> expandedItemTypes = new EnumMap<StockItemType, Boolean>(StockItemType.class);
    private final Map<StockItemType, Map<StockCategory, Boolean>> expandedByItemType =
            new EnumMap<StockItemType, Map<StockCategory, Boolean>>(StockItemType.class);
    private final Map<StockReviewTradeGroup, Boolean> expandedTradeGroups = new EnumMap<StockReviewTradeGroup, Boolean>(StockReviewTradeGroup.class);
    private final Set<String> expandedItems = new HashSet<String>();

    StockReviewExpansionState() {
        expanded.put(StockCategory.NO_STOCK, Boolean.FALSE);
        expanded.put(StockCategory.INSUFFICIENT, Boolean.FALSE);
        expanded.put(StockCategory.SUFFICIENT, Boolean.FALSE);
        expandedItemTypes.put(StockItemType.WEAPON, Boolean.TRUE);
        expandedItemTypes.put(StockItemType.WING, Boolean.TRUE);
        initializeItemCategoryExpansion();
        expandedTradeGroups.put(StockReviewTradeGroup.BUYING, Boolean.FALSE);
        expandedTradeGroups.put(StockReviewTradeGroup.SELLING, Boolean.FALSE);
    }

    StockReviewExpansionState(StockReviewExpansionState source) {
        expanded.putAll(source.expanded);
        expandedItemTypes.putAll(source.expandedItemTypes);
        copyItemCategoryExpansion(source.expandedByItemType);
        expandedTradeGroups.putAll(source.expandedTradeGroups);
        expandedItems.addAll(source.expandedItems);
    }

    boolean isExpanded(StockCategory category) {
        Boolean value = expanded.get(category);
        return value != null && value.booleanValue();
    }

    void toggle(StockCategory category) {
        expanded.put(category, Boolean.valueOf(!isExpanded(category)));
    }

    boolean isExpanded(StockItemType itemType, StockCategory category) {
        Map<StockCategory, Boolean> byCategory = expandedByItemType.get(itemType);
        Boolean value = byCategory == null ? null : byCategory.get(category);
        return value != null && value.booleanValue();
    }

    void toggle(StockItemType itemType, StockCategory category) {
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

    boolean isExpanded(StockItemType itemType) {
        Boolean value = expandedItemTypes.get(itemType);
        return value != null && value.booleanValue();
    }

    void toggle(StockItemType itemType) {
        expandedItemTypes.put(itemType, Boolean.valueOf(!isExpanded(itemType)));
    }

    boolean isExpanded(StockReviewTradeGroup tradeGroup) {
        Boolean value = expandedTradeGroups.get(tradeGroup);
        return value != null && value.booleanValue();
    }

    void toggle(StockReviewTradeGroup tradeGroup) {
        expandedTradeGroups.put(tradeGroup, Boolean.valueOf(!isExpanded(tradeGroup)));
    }

    void setExpanded(StockReviewTradeGroup tradeGroup, boolean value) {
        if (tradeGroup != null) {
            expandedTradeGroups.put(tradeGroup, Boolean.valueOf(value));
        }
    }

    boolean isItemExpanded(String itemKey) {
        return expandedItems.contains(itemKey);
    }

    void toggleItem(String itemKey) {
        if (itemKey == null || itemKey.isEmpty()) {
            return;
        }
        if (expandedItems.contains(itemKey)) {
            expandedItems.remove(itemKey);
        } else {
            expandedItems.add(itemKey);
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
