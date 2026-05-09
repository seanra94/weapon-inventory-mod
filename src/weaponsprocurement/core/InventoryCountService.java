package weaponsprocurement.core;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Misc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class InventoryCountService {
    public Map<String, Integer> collectOwnedItemCounts(SectorAPI sector, MarketAPI market, OwnedSourcePolicy policy) {
        Map<String, Integer> counts = collectOwnedCounts(sector, market, policy, StockItemType.WEAPON);
        merge(counts, collectOwnedCounts(sector, market, policy, StockItemType.WING));
        return counts;
    }

    private Map<String, Integer> collectOwnedCounts(SectorAPI sector,
                                                    MarketAPI market,
                                                    OwnedSourcePolicy policy,
                                                    StockItemType itemType) {
        Map<String, Integer> counts = new HashMap<String, Integer>();
        if (sector == null) {
            return counts;
        }

        CampaignFleetAPI fleet = sector.getPlayerFleet();
        merge(counts, collectCargoCounts(fleet == null ? null : fleet.getCargo(), itemType));

        if (OwnedSourcePolicy.FLEET_AND_ACCESSIBLE_STORAGE.equals(policy)) {
            mergeAccessibleStorage(counts, sector, itemType);
        } else if (OwnedSourcePolicy.FLEET_AND_CURRENT_MARKET_STORAGE.equals(policy) && market != null && Misc.playerHasStorageAccess(market)) {
            merge(counts, collectCargoCounts(Misc.getStorageCargo(market), itemType));
        }

        return counts;
    }

    private static void mergeAccessibleStorage(Map<String, Integer> counts, SectorAPI sector, StockItemType itemType) {
        EconomyAPI economy = sector.getEconomy();
        List<MarketAPI> markets = economy == null ? null : economy.getMarketsCopy();
        if (markets == null) {
            return;
        }
        for (MarketAPI storageMarket : markets) {
            if (storageMarket == null || !Misc.playerHasStorageAccess(storageMarket)) {
                continue;
            }
            merge(counts, collectCargoCounts(Misc.getStorageCargo(storageMarket), itemType));
        }
    }

    public static Map<String, Integer> collectCargoWeaponCounts(CargoAPI cargo) {
        Map<String, Integer> counts = new HashMap<String, Integer>();
        if (cargo == null || cargo.getWeapons() == null) {
            return counts;
        }
        List<CargoAPI.CargoItemQuantity<String>> weapons = cargo.getWeapons();
        for (CargoAPI.CargoItemQuantity<String> quantity : weapons) {
            if (quantity != null) {
                add(counts, quantity.getItem(), quantity.getCount());
            }
        }
        return counts;
    }

    public static Map<String, Integer> collectCargoFighterCounts(CargoAPI cargo) {
        Map<String, Integer> counts = new HashMap<String, Integer>();
        if (cargo == null || cargo.getFighters() == null) {
            return counts;
        }
        List<CargoAPI.CargoItemQuantity<String>> fighters = cargo.getFighters();
        for (CargoAPI.CargoItemQuantity<String> quantity : fighters) {
            if (quantity != null) {
                add(counts, quantity.getItem(), quantity.getCount());
            }
        }
        return counts;
    }

    public static Map<String, Integer> collectCargoItemCounts(CargoAPI cargo) {
        Map<String, Integer> counts = new HashMap<String, Integer>();
        mergeWithPrefix(counts, collectCargoWeaponCounts(cargo), StockItemType.WEAPON);
        mergeWithPrefix(counts, collectCargoFighterCounts(cargo), StockItemType.WING);
        return counts;
    }

    private static Map<String, Integer> collectCargoCounts(CargoAPI cargo, StockItemType itemType) {
        if (StockItemType.WING.equals(itemType)) {
            Map<String, Integer> raw = collectCargoFighterCounts(cargo);
            Map<String, Integer> keyed = new HashMap<String, Integer>();
            mergeWithPrefix(keyed, raw, StockItemType.WING);
            return keyed;
        }
        Map<String, Integer> raw = collectCargoWeaponCounts(cargo);
        Map<String, Integer> keyed = new HashMap<String, Integer>();
        mergeWithPrefix(keyed, raw, StockItemType.WEAPON);
        return keyed;
    }

    static void merge(Map<String, Integer> target, Map<String, Integer> source) {
        for (Map.Entry<String, Integer> entry : source.entrySet()) {
            add(target, entry.getKey(), entry.getValue().intValue());
        }
    }

    static void add(Map<String, Integer> counts, String id, int count) {
        if (id == null || id.isEmpty() || count == 0) {
            return;
        }
        Integer existing = counts.get(id);
        counts.put(id, Integer.valueOf((existing == null ? 0 : existing.intValue()) + count));
    }

    private static void mergeWithPrefix(Map<String, Integer> target, Map<String, Integer> source, StockItemType itemType) {
        for (Map.Entry<String, Integer> entry : source.entrySet()) {
            add(target, itemType.key(entry.getKey()), entry.getValue().intValue());
        }
    }
}
