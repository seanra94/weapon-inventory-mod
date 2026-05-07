package weaponinventorymod.core;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
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
    private final GlobalWeaponMarketService globalWeaponMarketService = new GlobalWeaponMarketService();
    private final StockStatusClassifier classifier = new StockStatusClassifier();

    public WeaponStockSnapshot build(SectorAPI sector,
                                     MarketAPI market,
                                     StockReviewConfig config,
                                     StockSortMode sortMode,
                                     boolean includeCurrentMarketStorage,
                                     boolean includeBlackMarket,
                                     boolean globalMarketMode) {
        OwnedSourcePolicy ownedSourcePolicy = config.ownedSourcePolicy(includeCurrentMarketStorage);
        DesiredStockService desiredStockService = new DesiredStockService(config);
        Map<String, Integer> owned = inventoryCountService.collectOwnedWeaponCounts(sector, market, ownedSourcePolicy);
        Map<String, Integer> playerCargoCounts = InventoryCountService.collectCargoWeaponCounts(playerCargo(sector));
        MarketStockService.MarketStock marketStock = globalMarketMode
                ? globalWeaponMarketService.collectGlobalWeaponStock(sector, includeBlackMarket)
                : marketStockService.collectCurrentMarketWeaponStock(market, includeBlackMarket);

        Set<String> ids = new HashSet<String>();
        addTradeableIds(ids, playerCargoCounts, marketStock);

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
            if (!shouldInclude(getCount(playerCargoCounts, weaponId), marketStock.getSubmarketStocks(weaponId))) {
                continue;
            }
            int desiredCount = desiredStockService.desiredCount(weaponId, spec);
            StockCategory category = classifier.classify(ownedCount, desiredCount);
            grouped.get(category).add(new WeaponStockRecord(
                    weaponId,
                    spec.getWeaponName(),
                    spec,
                    ownedCount,
                    getCount(playerCargoCounts, weaponId),
                    purchasableCount,
                    desiredCount,
                    category,
                    marketStock.getSubmarketStocks(weaponId)));
        }

        for (List<WeaponStockRecord> records : grouped.values()) {
            Collections.sort(records, comparatorFor(sortMode));
        }

        return new WeaponStockSnapshot(market, ownedSourcePolicy, sortMode, includeBlackMarket, globalMarketMode, grouped);
    }

    private static void addTradeableIds(Set<String> ids,
                                        Map<String, Integer> playerCargoCounts,
                                        MarketStockService.MarketStock marketStock) {
        ids.addAll(playerCargoCounts.keySet());
        for (String id : marketStock.weaponIds()) {
            if (hasPurchasableStock(marketStock.getSubmarketStocks(id))) {
                ids.add(id);
            }
        }
    }

    private static boolean shouldInclude(int playerCargoCount, List<SubmarketWeaponStock> marketStocks) {
        return playerCargoCount > 0 || hasPurchasableStock(marketStocks);
    }

    private static boolean hasPurchasableStock(List<SubmarketWeaponStock> marketStocks) {
        if (marketStocks == null) {
            return false;
        }
        for (int i = 0; i < marketStocks.size(); i++) {
            SubmarketWeaponStock stock = marketStocks.get(i);
            if (stock != null && stock.isPurchasable() && stock.getCount() > 0) {
                return true;
            }
        }
        return false;
    }

    private static WeaponSpecAPI safeWeaponSpec(String weaponId) {
        try {
            return Global.getSettings().getWeaponSpec(weaponId);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static com.fs.starfarer.api.campaign.CargoAPI playerCargo(SectorAPI sector) {
        CampaignFleetAPI fleet = sector == null ? null : sector.getPlayerFleet();
        return fleet == null ? null : fleet.getCargo();
    }

    private static int getCount(Map<String, Integer> counts, String id) {
        Integer count = counts.get(id);
        return count == null ? 0 : count.intValue();
    }

    private static Comparator<WeaponStockRecord> comparatorFor(StockSortMode sortMode) {
        if (StockSortMode.NAME.equals(sortMode)) {
            return NameComparator.INSTANCE;
        }
        if (StockSortMode.PRICE.equals(sortMode)) {
            return PriceComparator.INSTANCE;
        }
        return NeedComparator.INSTANCE;
    }

    private static final class NeedComparator implements Comparator<WeaponStockRecord> {
        static final NeedComparator INSTANCE = new NeedComparator();

        @Override
        public int compare(WeaponStockRecord left, WeaponStockRecord right) {
            return compareByNeedPriceName(left, right);
        }
    }

    private static final class NameComparator implements Comparator<WeaponStockRecord> {
        static final NameComparator INSTANCE = new NameComparator();

        @Override
        public int compare(WeaponStockRecord left, WeaponStockRecord right) {
            int result = left.getDisplayName().compareToIgnoreCase(right.getDisplayName());
            if (result != 0) {
                return result;
            }
            result = compareByNeed(left, right);
            if (result != 0) {
                return result;
            }
            return compareByPrice(left, right);
        }
    }

    private static final class PriceComparator implements Comparator<WeaponStockRecord> {
        static final PriceComparator INSTANCE = new PriceComparator();

        @Override
        public int compare(WeaponStockRecord left, WeaponStockRecord right) {
            int result = compareByPrice(left, right);
            if (result != 0) {
                return result;
            }
            result = Integer.compare(right.getNeededCount(), left.getNeededCount());
            if (result != 0) {
                return result;
            }
            return left.getDisplayName().compareToIgnoreCase(right.getDisplayName());
        }
    }

    /**
     * Compatibility shim for games that hot-loaded an older outer class while a
     * newer jar was copied underneath it. Remove only after this class has been
     * in public builds long enough that stale runtime references are unlikely.
     */
    private static final class CostComparator implements Comparator<WeaponStockRecord> {
        static final CostComparator INSTANCE = new CostComparator();

        @Override
        public int compare(WeaponStockRecord left, WeaponStockRecord right) {
            return PriceComparator.INSTANCE.compare(left, right);
        }
    }

    private static int compareByNeedPriceName(WeaponStockRecord left, WeaponStockRecord right) {
        int result = compareByNeed(left, right);
        if (result != 0) {
            return result;
        }
        result = compareByPrice(left, right);
        if (result != 0) {
            return result;
        }
        return left.getDisplayName().compareToIgnoreCase(right.getDisplayName());
    }

    private static int compareByNeed(WeaponStockRecord left, WeaponStockRecord right) {
        return Integer.compare(left.getStoredOutsideInventoryCount(), right.getStoredOutsideInventoryCount());
    }

    private static int compareByPrice(WeaponStockRecord left, WeaponStockRecord right) {
        return Integer.compare(left.getCheapestPurchasableUnitPrice(), right.getCheapestPurchasableUnitPrice());
    }
}
