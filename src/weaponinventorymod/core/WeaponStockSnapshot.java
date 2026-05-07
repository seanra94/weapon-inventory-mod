package weaponinventorymod.core;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class WeaponStockSnapshot {
    private final MarketAPI market;
    private final OwnedSourcePolicy ownedSourcePolicy;
    private final StockDisplayMode displayMode;
    private final StockSortMode sortMode;
    private final boolean includeBlackMarket;
    private final boolean globalMarketMode;
    private final Map<StockCategory, List<WeaponStockRecord>> recordsByCategory;
    private final Map<String, WeaponStockRecord> recordsByWeaponId;
    private final List<WeaponStockRecord> allRecords;
    private final int totalRecords;

    public WeaponStockSnapshot(MarketAPI market,
                               OwnedSourcePolicy ownedSourcePolicy,
                               StockDisplayMode displayMode,
                               StockSortMode sortMode,
                               boolean includeBlackMarket,
                               boolean globalMarketMode,
                               Map<StockCategory, List<WeaponStockRecord>> recordsByCategory) {
        this.market = market;
        this.ownedSourcePolicy = ownedSourcePolicy;
        this.displayMode = displayMode;
        this.sortMode = sortMode;
        this.includeBlackMarket = includeBlackMarket;
        this.globalMarketMode = globalMarketMode;
        this.recordsByCategory = immutableCategoryMap(recordsByCategory);
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

    public StockDisplayMode getDisplayMode() {
        return displayMode;
    }

    public StockSortMode getSortMode() {
        return sortMode;
    }

    public boolean isIncludeBlackMarket() {
        return includeBlackMarket;
    }

    public boolean isGlobalMarketMode() {
        return globalMarketMode;
    }

    public List<WeaponStockRecord> getRecords(StockCategory category) {
        List<WeaponStockRecord> records = recordsByCategory.get(category);
        return records == null ? Collections.<WeaponStockRecord>emptyList() : records;
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
        if (globalMarketMode) {
            return GlobalWeaponMarketService.VIRTUAL_SUBMARKET_NAME;
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
