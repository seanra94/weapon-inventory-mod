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
    private final StockStatusClassifier classifier = new StockStatusClassifier();

    public WeaponStockSnapshot build(SectorAPI sector,
                                     MarketAPI market,
                                     StockReviewConfig config,
                                     StockDisplayMode displayMode,
                                     StockSortMode sortMode,
                                     boolean includeCurrentMarketStorage,
                                     boolean includeBlackMarket) {
        OwnedSourcePolicy ownedSourcePolicy = config.ownedSourcePolicy(includeCurrentMarketStorage);
        DesiredStockService desiredStockService = new DesiredStockService(config);
        Map<String, Integer> owned = inventoryCountService.collectOwnedWeaponCounts(sector, market, ownedSourcePolicy);
        Map<String, Integer> playerCargoCounts = InventoryCountService.collectCargoWeaponCounts(playerCargo(sector));
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
                    getCount(playerCargoCounts, weaponId),
                    purchasableCount,
                    desiredCount,
                    category,
                    marketStock.getSubmarketStocks(weaponId)));
        }

        for (List<WeaponStockRecord> records : grouped.values()) {
            Collections.sort(records, comparatorFor(sortMode));
        }

        return new WeaponStockSnapshot(market, ownedSourcePolicy, displayMode, sortMode, includeBlackMarket, grouped);
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
        if (StockSortMode.PURCHASABLE.equals(sortMode)) {
            return PurchasableComparator.INSTANCE;
        }
        if (StockSortMode.OWNED.equals(sortMode)) {
            return OwnedComparator.INSTANCE;
        }
        return NeedComparator.INSTANCE;
    }

    private static final class NeedComparator implements Comparator<WeaponStockRecord> {
        static final NeedComparator INSTANCE = new NeedComparator();

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

    private static final class NameComparator implements Comparator<WeaponStockRecord> {
        static final NameComparator INSTANCE = new NameComparator();

        @Override
        public int compare(WeaponStockRecord left, WeaponStockRecord right) {
            return left.getDisplayName().compareToIgnoreCase(right.getDisplayName());
        }
    }

    private static final class PurchasableComparator implements Comparator<WeaponStockRecord> {
        static final PurchasableComparator INSTANCE = new PurchasableComparator();

        @Override
        public int compare(WeaponStockRecord left, WeaponStockRecord right) {
            int result = Integer.compare(right.getPurchasableCount(), left.getPurchasableCount());
            if (result != 0) {
                return result;
            }
            return left.getDisplayName().compareToIgnoreCase(right.getDisplayName());
        }
    }

    private static final class OwnedComparator implements Comparator<WeaponStockRecord> {
        static final OwnedComparator INSTANCE = new OwnedComparator();

        @Override
        public int compare(WeaponStockRecord left, WeaponStockRecord right) {
            int result = Integer.compare(right.getOwnedCount(), left.getOwnedCount());
            if (result != 0) {
                return result;
            }
            return left.getDisplayName().compareToIgnoreCase(right.getDisplayName());
        }
    }
}
