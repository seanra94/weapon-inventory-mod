package weaponsprocurement.core

import com.fs.starfarer.api.campaign.econ.MarketAPI
import java.util.ArrayList
import java.util.Collections
import java.util.EnumMap
import java.util.HashMap

class WeaponStockSnapshot(
    private val market: MarketAPI?,
    private val ownedSourcePolicy: OwnedSourcePolicy,
    private val sortMode: StockSortMode,
    private val includeBlackMarket: Boolean,
    sourceMode: StockSourceMode?,
    recordsByCategory: Map<StockCategory, List<WeaponStockRecord>>,
) {
    private val sourceMode: StockSourceMode = sourceMode ?: StockSourceMode.LOCAL
    private val recordsByCategory: Map<StockCategory, List<WeaponStockRecord>> =
        immutableCategoryMap(recordsByCategory)
    private val recordsByTypeAndCategory: Map<StockItemType, Map<StockCategory, List<WeaponStockRecord>>> =
        immutableTypeCategoryMap(this.recordsByCategory)
    private val recordsByItemKey: Map<String, WeaponStockRecord> =
        immutableItemKeyMap(this.recordsByCategory)
    private val allRecords: List<WeaponStockRecord> = immutableAllRecords(this.recordsByCategory)
    private val totalRecords: Int = allRecords.size

    fun getMarket(): MarketAPI? = market

    fun getOwnedSourcePolicy(): OwnedSourcePolicy = ownedSourcePolicy

    fun getSortMode(): StockSortMode = sortMode

    fun isIncludeBlackMarket(): Boolean = includeBlackMarket

    fun getSourceMode(): StockSourceMode = sourceMode

    fun getRecords(category: StockCategory?): List<WeaponStockRecord> {
        return recordsByCategory[category] ?: Collections.emptyList()
    }

    fun getRecords(itemType: StockItemType?, category: StockCategory?): List<WeaponStockRecord> {
        val byCategory = recordsByTypeAndCategory[itemType] ?: return Collections.emptyList()
        return byCategory[category] ?: Collections.emptyList()
    }

    fun getCount(itemType: StockItemType?): Int {
        var count = 0
        val byCategory = recordsByTypeAndCategory[itemType] ?: return 0
        for (category in StockCategory.values()) {
            count += byCategory[category]?.size ?: 0
        }
        return count
    }

    fun getCount(category: StockCategory?): Int = getRecords(category).size

    fun getTotalRecords(): Int = totalRecords

    fun getRecord(itemKey: String?): WeaponStockRecord? {
        if (itemKey == null) return null
        return recordsByItemKey[itemKey]
    }

    fun getAllRecords(): List<WeaponStockRecord> = allRecords

    fun getMarketName(): String {
        if (StockSourceMode.FIXERS == sourceMode) return GlobalWeaponMarketService.FIXERS_MARKET_NAME
        if (StockSourceMode.SECTOR == sourceMode) return GlobalWeaponMarketService.SECTOR_MARKET_NAME
        return market?.name ?: "No market context"
    }

    companion object {
        private fun immutableCategoryMap(
            source: Map<StockCategory, List<WeaponStockRecord>>,
        ): Map<StockCategory, List<WeaponStockRecord>> {
            val result = EnumMap<StockCategory, List<WeaponStockRecord>>(StockCategory::class.java)
            for (category in StockCategory.values()) {
                val records = source[category]
                result[category] = if (records == null) {
                    Collections.emptyList()
                } else {
                    Collections.unmodifiableList(ArrayList(records))
                }
            }
            return Collections.unmodifiableMap(result)
        }

        private fun immutableTypeCategoryMap(
            recordsByCategory: Map<StockCategory, List<WeaponStockRecord>>,
        ): Map<StockItemType, Map<StockCategory, List<WeaponStockRecord>>> {
            val result = EnumMap<StockItemType, Map<StockCategory, List<WeaponStockRecord>>>(StockItemType::class.java)
            for (itemType in StockItemType.values()) {
                val grouped = EnumMap<StockCategory, List<WeaponStockRecord>>(StockCategory::class.java)
                for (category in StockCategory.values()) {
                    val source = recordsByCategory[category]
                    val records = ArrayList<WeaponStockRecord>()
                    if (source != null) {
                        for (record in source) {
                            if (itemType == record.itemType) records.add(record)
                        }
                    }
                    grouped[category] = Collections.unmodifiableList(records)
                }
                result[itemType] = Collections.unmodifiableMap(grouped)
            }
            return Collections.unmodifiableMap(result)
        }

        private fun immutableItemKeyMap(
            recordsByCategory: Map<StockCategory, List<WeaponStockRecord>>,
        ): Map<String, WeaponStockRecord> {
            val result = HashMap<String, WeaponStockRecord>()
            for (category in StockCategory.values()) {
                val records = recordsByCategory[category] ?: continue
                for (record in records) {
                    result[record.itemKey] = record
                }
            }
            return Collections.unmodifiableMap(result)
        }

        private fun immutableAllRecords(
            recordsByCategory: Map<StockCategory, List<WeaponStockRecord>>,
        ): List<WeaponStockRecord> {
            val result = ArrayList<WeaponStockRecord>()
            for (category in StockCategory.values()) {
                val records = recordsByCategory[category]
                if (records != null) result.addAll(records)
            }
            return Collections.unmodifiableList(result)
        }
    }
}
