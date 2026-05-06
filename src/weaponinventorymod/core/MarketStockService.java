package weaponinventorymod.core;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MarketStockService {
    public MarketStock collectCurrentMarketWeaponStock(MarketAPI market, boolean includeBlackMarket) {
        Map<String, Integer> totals = new HashMap<String, Integer>();
        Map<String, List<SubmarketWeaponStock>> byWeapon = new HashMap<String, List<SubmarketWeaponStock>>();
        if (market == null || market.getSubmarketsCopy() == null) {
            return new MarketStock(totals, byWeapon);
        }

        for (SubmarketAPI submarket : market.getSubmarketsCopy()) {
            if (!isPurchasableSubmarket(submarket, includeBlackMarket)) {
                continue;
            }
            CargoAPI cargo = submarket.getCargoNullOk();
            Map<String, Integer> counts = InventoryCountService.collectCargoWeaponCounts(cargo);
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                int count = entry.getValue().intValue();
                if (count <= 0) {
                    continue;
                }
                InventoryCountService.add(totals, entry.getKey(), count);
                List<SubmarketWeaponStock> stocks = byWeapon.get(entry.getKey());
                if (stocks == null) {
                    stocks = new ArrayList<SubmarketWeaponStock>();
                    byWeapon.put(entry.getKey(), stocks);
                }
                stocks.add(new SubmarketWeaponStock(submarket.getSpecId(), submarket.getNameOneLine(), count));
            }
        }

        return new MarketStock(totals, byWeapon);
    }

    private static boolean isPurchasableSubmarket(SubmarketAPI submarket, boolean includeBlackMarket) {
        if (submarket == null) {
            return false;
        }
        String id = submarket.getSpecId();
        if (Submarkets.SUBMARKET_STORAGE.equals(id) || Submarkets.LOCAL_RESOURCES.equals(id)) {
            return false;
        }
        if (!includeBlackMarket && Submarkets.SUBMARKET_BLACK.equals(id)) {
            return false;
        }
        return submarket.getCargoNullOk() != null;
    }

    public static final class MarketStock {
        private final Map<String, Integer> totals;
        private final Map<String, List<SubmarketWeaponStock>> byWeapon;

        private MarketStock(Map<String, Integer> totals, Map<String, List<SubmarketWeaponStock>> byWeapon) {
            this.totals = totals;
            this.byWeapon = byWeapon;
        }

        public int getTotal(String weaponId) {
            Integer count = totals.get(weaponId);
            return count == null ? 0 : count.intValue();
        }

        public List<SubmarketWeaponStock> getSubmarketStocks(String weaponId) {
            List<SubmarketWeaponStock> stocks = byWeapon.get(weaponId);
            if (stocks == null) {
                return Collections.emptyList();
            }
            return Collections.unmodifiableList(stocks);
        }

        public Iterable<String> weaponIds() {
            return totals.keySet();
        }
    }
}
