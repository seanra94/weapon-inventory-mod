package weaponinventorymod.core;

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
    public Map<String, Integer> collectOwnedWeaponCounts(SectorAPI sector, MarketAPI market, OwnedSourcePolicy policy) {
        Map<String, Integer> counts = new HashMap<String, Integer>();
        if (sector == null) {
            return counts;
        }

        CampaignFleetAPI fleet = sector.getPlayerFleet();
        merge(counts, collectCargoWeaponCounts(fleet == null ? null : fleet.getCargo()));

        if (OwnedSourcePolicy.FLEET_AND_ACCESSIBLE_STORAGE.equals(policy)) {
            mergeAccessibleStorage(counts, sector);
        } else if (OwnedSourcePolicy.FLEET_AND_CURRENT_MARKET_STORAGE.equals(policy) && market != null && Misc.playerHasStorageAccess(market)) {
            merge(counts, collectCargoWeaponCounts(Misc.getStorageCargo(market)));
        }

        return counts;
    }

    private static void mergeAccessibleStorage(Map<String, Integer> counts, SectorAPI sector) {
        EconomyAPI economy = sector.getEconomy();
        List<MarketAPI> markets = economy == null ? null : economy.getMarketsCopy();
        if (markets == null) {
            return;
        }
        for (MarketAPI storageMarket : markets) {
            if (storageMarket == null || !Misc.playerHasStorageAccess(storageMarket)) {
                continue;
            }
            merge(counts, collectCargoWeaponCounts(Misc.getStorageCargo(storageMarket)));
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
}
