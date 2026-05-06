package weaponinventorymod.core;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WeaponStockSnapshotBuilder {
    private final InventoryCountService inventoryCountService = new InventoryCountService();
    private final MarketStockService marketStockService = new MarketStockService();
    private final DesiredStockService desiredStockService = new DesiredStockService();
    private final StockStatusClassifier classifier = new StockStatusClassifier();

    public WeaponStockSnapshot build(SectorAPI sector, MarketAPI market, OwnedSourcePolicy ownedSourcePolicy) {
        Map<String, Integer> owned = inventoryCountService.collectOwnedWeaponCounts(sector, market, ownedSourcePolicy);
        MarketStockService.MarketStock marketStock = marketStockService.collectCurrentMarketWeaponStock(market);

        Set<String> ids = new HashSet<String>();
        ids.addAll(owned.keySet());
        for (String id : marketStock.weaponIds()) {
            ids.add(id);
        }

        Map<StockCategory, List<WeaponStockRecord>> grouped = new EnumMap<StockCategory, List<WeaponStockRecord>>(StockCategory.class);
        for (StockCategory category : StockCategory.values()) {
            grouped.put(category, new ArrayList<WeaponStockRecord>());
        }

        for (String weaponId : ids) {
            WeaponSpecAPI spec = safeWeaponSpec(weaponId);
            if (spec == null) {
                continue;
            }
            int ownedCount = getCount(owned, weaponId);
            int purchasableCount = marketStock.getTotal(weaponId);
            if (ownedCount <= 0 && purchasableCount <= 0) {
                continue;
            }
            int desiredCount = desiredStockService.desiredCount(spec);
            StockCategory category = classifier.classify(ownedCount, desiredCount);
            grouped.get(category).add(new WeaponStockRecord(
                    weaponId,
                    spec.getWeaponName(),
                    spec,
                    ownedCount,
                    purchasableCount,
                    desiredCount,
                    category,
                    marketStock.getSubmarketStocks(weaponId)));
        }

        for (List<WeaponStockRecord> records : grouped.values()) {
            Collections.sort(records, RECORD_ORDER);
        }

        return new WeaponStockSnapshot(market, ownedSourcePolicy, grouped);
    }

    private static WeaponSpecAPI safeWeaponSpec(String weaponId) {
        try {
            return Global.getSettings().getWeaponSpec(weaponId);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static int getCount(Map<String, Integer> counts, String id) {
        Integer count = counts.get(id);
        return count == null ? 0 : count.intValue();
    }

    private static final Comparator<WeaponStockRecord> RECORD_ORDER = new WeaponStockRecordComparator();

    private static final class WeaponStockRecordComparator implements Comparator<WeaponStockRecord> {
        @Override
        public int compare(WeaponStockRecord left, WeaponStockRecord right) {
            int desiredGap = Integer.compare(
                    right.getDesiredCount() - right.getOwnedCount(),
                    left.getDesiredCount() - left.getOwnedCount());
            if (desiredGap != 0) {
                return desiredGap;
            }
            return left.getDisplayName().compareToIgnoreCase(right.getDisplayName());
        }
    }
}
