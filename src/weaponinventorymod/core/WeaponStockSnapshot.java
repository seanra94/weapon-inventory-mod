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
    private final Map<StockCategory, List<WeaponStockRecord>> recordsByCategory;
    private final int totalRecords;

    public WeaponStockSnapshot(MarketAPI market,
                               OwnedSourcePolicy ownedSourcePolicy,
                               StockDisplayMode displayMode,
                               StockSortMode sortMode,
                               boolean includeBlackMarket,
                               Map<StockCategory, List<WeaponStockRecord>> recordsByCategory) {
        this.market = market;
        this.ownedSourcePolicy = ownedSourcePolicy;
        this.displayMode = displayMode;
        this.sortMode = sortMode;
        this.includeBlackMarket = includeBlackMarket;
        this.recordsByCategory = immutableCategoryMap(recordsByCategory);
        int total = 0;
        for (List<WeaponStockRecord> records : this.recordsByCategory.values()) {
            total += records.size();
        }
        this.totalRecords = total;
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

    public String getMarketName() {
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
}
