package weaponsprocurement.ui.stockreview.state

import weaponsprocurement.ui.stockreview.actions.*
import weaponsprocurement.ui.stockreview.state.*
import weaponsprocurement.ui.stockreview.rows.*
import weaponsprocurement.ui.stockreview.tooltips.*
import weaponsprocurement.ui.stockreview.rendering.*
import weaponsprocurement.ui.stockreview.trade.*

import weaponsprocurement.ui.*

import weaponsprocurement.stock.item.StockCategory
import weaponsprocurement.stock.item.StockItemType
import java.util.EnumMap
import java.util.HashSet

class StockReviewExpansionState {
    private val expanded: MutableMap<StockCategory, Boolean> = EnumMap(StockCategory::class.java)
    private val expandedItemTypes: MutableMap<StockItemType, Boolean> = EnumMap(StockItemType::class.java)
    private val expandedByItemType: MutableMap<StockItemType, MutableMap<StockCategory, Boolean>> =
        EnumMap(StockItemType::class.java)
    private val expandedTradeGroups: MutableMap<StockReviewTradeGroup, Boolean> = EnumMap(StockReviewTradeGroup::class.java)
    private val expandedItems: MutableSet<String> = HashSet()

    constructor() {
        expanded[StockCategory.NO_STOCK] = false
        expanded[StockCategory.INSUFFICIENT] = false
        expanded[StockCategory.SUFFICIENT] = false
        expandedItemTypes[StockItemType.WEAPON] = true
        expandedItemTypes[StockItemType.WING] = true
        initializeItemCategoryExpansion()
        expandedTradeGroups[StockReviewTradeGroup.BUYING] = false
        expandedTradeGroups[StockReviewTradeGroup.SELLING] = false
    }

    constructor(source: StockReviewExpansionState) {
        expanded.putAll(source.expanded)
        expandedItemTypes.putAll(source.expandedItemTypes)
        copyItemCategoryExpansion(source.expandedByItemType)
        expandedTradeGroups.putAll(source.expandedTradeGroups)
        expandedItems.addAll(source.expandedItems)
    }

    fun isExpanded(category: StockCategory?): Boolean = expanded[category] == true

    fun toggle(category: StockCategory?) {
        if (category != null) {
            expanded[category] = !isExpanded(category)
        }
    }

    fun isExpanded(itemType: StockItemType?, category: StockCategory?): Boolean {
        val byCategory = expandedByItemType[itemType]
        return byCategory?.get(category) == true
    }

    fun toggle(itemType: StockItemType?, category: StockCategory?) {
        if (itemType == null) {
            toggle(category)
            return
        }
        if (category == null) {
            return
        }
        val byCategory = expandedByItemType.getOrPut(itemType) { EnumMap(StockCategory::class.java) }
        byCategory[category] = !isExpanded(itemType, category)
    }

    fun isExpanded(itemType: StockItemType?): Boolean = expandedItemTypes[itemType] == true

    fun toggle(itemType: StockItemType?) {
        if (itemType != null) {
            expandedItemTypes[itemType] = !isExpanded(itemType)
        }
    }

    fun isExpanded(tradeGroup: StockReviewTradeGroup?): Boolean = expandedTradeGroups[tradeGroup] == true

    fun toggle(tradeGroup: StockReviewTradeGroup?) {
        if (tradeGroup != null) {
            expandedTradeGroups[tradeGroup] = !isExpanded(tradeGroup)
        }
    }

    fun setExpanded(tradeGroup: StockReviewTradeGroup?, value: Boolean) {
        if (tradeGroup != null) {
            expandedTradeGroups[tradeGroup] = value
        }
    }

    fun isItemExpanded(itemKey: String?): Boolean = expandedItems.contains(itemKey)

    fun toggleItem(itemKey: String?) {
        if (itemKey.isNullOrEmpty()) {
            return
        }
        if (expandedItems.contains(itemKey)) {
            expandedItems.remove(itemKey)
        } else {
            expandedItems.add(itemKey)
        }
    }

    private fun initializeItemCategoryExpansion() {
        for (itemType in StockItemType.values()) {
            val byCategory: MutableMap<StockCategory, Boolean> = EnumMap(StockCategory::class.java)
            for (category in StockCategory.values()) {
                byCategory[category] = false
            }
            expandedByItemType[itemType] = byCategory
        }
    }

    private fun copyItemCategoryExpansion(source: Map<StockItemType, Map<StockCategory, Boolean>>?) {
        if (source.isNullOrEmpty()) {
            initializeItemCategoryExpansion()
            return
        }
        for (itemType in StockItemType.values()) {
            val sourceByCategory = source[itemType]
            val byCategory: MutableMap<StockCategory, Boolean> = EnumMap(StockCategory::class.java)
            for (category in StockCategory.values()) {
                byCategory[category] = sourceByCategory?.get(category) == true
            }
            expandedByItemType[itemType] = byCategory
        }
    }
}