package weaponsprocurement.core;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class WeaponStockSnapshot {
    private final MarketAPI market;
    private final OwnedSourcePolicy ownedSourcePolicy;
    private final StockSortMode sortMode;
    private final boolean includeBlackMarket;
    private final StockSourceMode sourceMode;
    private final Map<StockCategory, List<WeaponStockRecord>> recordsByCategory;
    private final Map<StockItemType, Map<StockCategory, List<WeaponStockRecord>>> recordsByTypeAndCategory;
    private final Map<String, WeaponStockRecord> recordsByWeaponId;
    private final List<WeaponStockRecord> allRecords;
    private final int totalRecords;

    public WeaponStockSnapshot(MarketAPI market,
                               OwnedSourcePolicy ownedSourcePolicy,
                               StockSortMode sortMode,
                               boolean includeBlackMarket,
                               StockSourceMode sourceMode,
                               Map<StockCategory, List<WeaponStockRecord>> recordsByCategory) {
        this.market = market;
        this.ownedSourcePolicy = ownedSourcePolicy;
        this.sortMode = sortMode;
        this.includeBlackMarket = includeBlackMarket;
        this.sourceMode = sourceMode == null ? StockSourceMode.LOCAL : sourceMode;
        this.recordsByCategory = immutableCategoryMap(recordsByCategory);
        this.recordsByTypeAndCategory = immutableTypeCategoryMap(this.recordsByCategory);
        this.recordsByWeaponId = immutableWeaponMap(this.recordsByCategory);
        this.allRecords = immutableAllRecords(this.recordsByCategory);
        this.totalRecords = allRecords.size();
    }

    public MarketAPI getMarket() {
        return market;
    }

    public OwnedSourcePolicy getOwnedSourcePolicy() {
        return ownedSourcePolicy;
    }

    public StockSortMode getSortMode() {
        return sortMode;
    }

    public boolean isIncludeBlackMarket() {
        return includeBlackMarket;
    }

    public StockSourceMode getSourceMode() {
        return sourceMode;
    }

    public List<WeaponStockRecord> getRecords(StockCategory category) {
        List<WeaponStockRecord> records = recordsByCategory.get(category);
        return records == null ? Collections.<WeaponStockRecord>emptyList() : records;
    }

    public List<WeaponStockRecord> getRecords(StockItemType itemType, StockCategory category) {
        Map<StockCategory, List<WeaponStockRecord>> byCategory = recordsByTypeAndCategory.get(itemType);
        if (byCategory == null) {
            return Collections.emptyList();
        }
        List<WeaponStockRecord> records = byCategory.get(category);
        return records == null ? Collections.<WeaponStockRecord>emptyList() : records;
    }

    public int getCount(StockItemType itemType) {
        int count = 0;
        Map<StockCategory, List<WeaponStockRecord>> byCategory = recordsByTypeAndCategory.get(itemType);
        if (byCategory == null) {
            return 0;
        }
        for (StockCategory category : StockCategory.values()) {
            List<WeaponStockRecord> records = byCategory.get(category);
            count += records == null ? 0 : records.size();
        }
        return count;
    }

    public int getCount(StockCategory category) {
        return getRecords(category).size();
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public WeaponStockRecord getRecord(String weaponId) {
        if (weaponId == null) {
            return null;
        }
        return recordsByWeaponId.get(weaponId);
    }

    public List<WeaponStockRecord> getAllRecords() {
        return allRecords;
    }

    public String getMarketName() {
        if (StockSourceMode.FIXERS.equals(sourceMode)) {
            return GlobalWeaponMarketService.FIXERS_MARKET_NAME;
        }
        if (StockSourceMode.SECTOR.equals(sourceMode)) {
            return GlobalWeaponMarketService.SECTOR_MARKET_NAME;
        }
        return market == null ? "No market context" : market.getName();
    }

    private static Map<StockCategory, List<WeaponStockRecord>> immutableCategoryMap(Map<StockCategory, List<WeaponStockRecord>> source) {
        Map<StockCategory, List<WeaponStockRecord>> result = new EnumMap<StockCategory, List<WeaponStockRecord>>(StockCategory.class);
        for (StockCategory category : StockCategory.values()) {
            List<WeaponStockRecord> records = source.get(category);
            if (records == null) {
                records = Collections.emptyList();
            } else {
                records = Collections.unmodifiableList(new ArrayList<WeaponStockRecord>(records));
            }
            result.put(category, records);
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<StockItemType, Map<StockCategory, List<WeaponStockRecord>>> immutableTypeCategoryMap(Map<StockCategory, List<WeaponStockRecord>> recordsByCategory) {
        Map<StockItemType, Map<StockCategory, List<WeaponStockRecord>>> result =
                new EnumMap<StockItemType, Map<StockCategory, List<WeaponStockRecord>>>(StockItemType.class);
        for (StockItemType itemType : StockItemType.values()) {
            Map<StockCategory, List<WeaponStockRecord>> grouped =
                    new EnumMap<StockCategory, List<WeaponStockRecord>>(StockCategory.class);
            for (StockCategory category : StockCategory.values()) {
                List<WeaponStockRecord> source = recordsByCategory.get(category);
                List<WeaponStockRecord> records = new ArrayList<WeaponStockRecord>();
                if (source != null) {
                    for (int i = 0; i < source.size(); i++) {
                        WeaponStockRecord record = source.get(i);
                        if (record != null && itemType.equals(record.getItemType())) {
                            records.add(record);
                        }
                    }
                }
                grouped.put(category, Collections.unmodifiableList(records));
            }
            result.put(itemType, Collections.unmodifiableMap(grouped));
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, WeaponStockRecord> immutableWeaponMap(Map<StockCategory, List<WeaponStockRecord>> recordsByCategory) {
        Map<String, WeaponStockRecord> result = new java.util.HashMap<String, WeaponStockRecord>();
        for (StockCategory category : StockCategory.values()) {
            List<WeaponStockRecord> records = recordsByCategory.get(category);
            if (records == null) {
                continue;
            }
            for (int i = 0; i < records.size(); i++) {
                WeaponStockRecord record = records.get(i);
                result.put(record.getWeaponId(), record);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static List<WeaponStockRecord> immutableAllRecords(Map<StockCategory, List<WeaponStockRecord>> recordsByCategory) {
        List<WeaponStockRecord> result = new ArrayList<WeaponStockRecord>();
        for (StockCategory category : StockCategory.values()) {
            List<WeaponStockRecord> records = recordsByCategory.get(category);
            if (records != null) {
                result.addAll(records);
            }
        }
        return Collections.unmodifiableList(result);
    }
}
