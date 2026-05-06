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
    private final StockStatusClassifier classifier = new StockStatusClassifier();

    public WeaponStockSnapshot build(SectorAPI sector,
                                     MarketAPI market,
                                     StockReviewConfig config,
                                     StockDisplayMode displayMode,
                                     boolean includeCurrentMarketStorage,
                                     boolean includeBlackMarket) {
        OwnedSourcePolicy ownedSourcePolicy = config.ownedSourcePolicy(includeCurrentMarketStorage);
        DesiredStockService desiredStockService = new DesiredStockService(config);
        Map<String, Integer> owned = inventoryCountService.collectOwnedWeaponCounts(sector, market, ownedSourcePolicy);
        MarketStockService.MarketStock marketStock = marketStockService.collectCurrentMarketWeaponStock(market, includeBlackMarket);

        Set<String> ids = new HashSet<String>();
        addIdsForDisplayMode(ids, sector, owned, marketStock, displayMode);

        Map<StockCategory, List<WeaponStockRecord>> grouped = new EnumMap<StockCategory, List<WeaponStockRecord>>(StockCategory.class);
        for (StockCategory category : StockCategory.values()) {
            grouped.put(category, new ArrayList<WeaponStockRecord>());
        }

        for (String weaponId : ids) {
            WeaponSpecAPI spec = safeWeaponSpec(weaponId);
            if (spec == null) {
                continue;
            }
            if (config.isIgnored(weaponId)) {
                continue;
            }
            int ownedCount = getCount(owned, weaponId);
            int purchasableCount = marketStock.getTotal(weaponId);
            if (!shouldInclude(displayMode, ownedCount, purchasableCount)) {
                continue;
            }
            int desiredCount = desiredStockService.desiredCount(weaponId, spec);
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

        return new WeaponStockSnapshot(market, ownedSourcePolicy, displayMode, includeBlackMarket, grouped);
    }

    private static void addIdsForDisplayMode(Set<String> ids,
                                             SectorAPI sector,
                                             Map<String, Integer> owned,
                                             MarketStockService.MarketStock marketStock,
                                             StockDisplayMode displayMode) {
        if (StockDisplayMode.CURRENTLY_FOR_SALE.equals(displayMode)) {
            for (String id : marketStock.weaponIds()) {
                ids.add(id);
            }
            return;
        }
        if (StockDisplayMode.OWNED_ONLY.equals(displayMode)) {
            ids.addAll(owned.keySet());
            return;
        }
        if (StockDisplayMode.ALL_TRACKED.equals(displayMode)) {
            if (sector != null && sector.getAllWeaponIds() != null) {
                ids.addAll(sector.getAllWeaponIds());
            }
            return;
        }
        ids.addAll(owned.keySet());
        for (String id : marketStock.weaponIds()) {
            ids.add(id);
        }
    }

    private static boolean shouldInclude(StockDisplayMode displayMode, int ownedCount, int purchasableCount) {
        if (StockDisplayMode.CURRENTLY_FOR_SALE.equals(displayMode)) {
            return purchasableCount > 0;
        }
        if (StockDisplayMode.OWNED_ONLY.equals(displayMode)) {
            return ownedCount > 0;
        }
        if (StockDisplayMode.ALL_TRACKED.equals(displayMode)) {
            return true;
        }
        return ownedCount > 0 || purchasableCount > 0;
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
