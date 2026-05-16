package weaponsprocurement.core;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ObservedStockIndex {
    private final MarketStockService marketStockService = new MarketStockService();

    public Map<String, ObservedItem> collect(SectorAPI sector, WeaponMarketBlacklist blacklist) {
        EconomyAPI economy = sector == null ? null : sector.getEconomy();
        List<MarketAPI> markets = economy == null ? null : economy.getMarketsCopy();
        if (markets == null || markets.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, ObservedItem> result = new HashMap<String, ObservedItem>();
        for (int i = 0; i < markets.size(); i++) {
            MarketAPI market = markets.get(i);
            MarketStockService.MarketStock stock = marketStockService.collectCurrentMarketItemStock(market, true);
            for (String itemKey : stock.itemKeys()) {
                if (!isEligible(itemKey, blacklist)) {
                    continue;
                }
                List<SubmarketWeaponStock> sources = stock.getSubmarketStocks(itemKey);
                for (int j = 0; j < sources.size(); j++) {
                    SubmarketWeaponStock source = sources.get(j);
                    if (source == null || source.getCount() <= 0 || !source.isPurchasable()) {
                        continue;
                    }
                    ObservedItem current = result.get(itemKey);
                    boolean vanillaSupported = isVanillaSupportedSubmarket(source.getSubmarketId());
                    if (current == null) {
                        result.put(itemKey, new ObservedItem(source, vanillaSupported));
                    } else {
                        current.observe(source, vanillaSupported);
                    }
                }
            }
        }
        return result;
    }

    private static boolean isEligible(String itemKey, WeaponMarketBlacklist blacklist) {
        return FixerMarketObservedCatalog.isSafeFixerItem(itemKey)
                && (blacklist == null || !blacklist.isBannedFromFixers(itemKey));
    }

    static boolean isVanillaSupportedSubmarket(String submarketId) {
        return Submarkets.SUBMARKET_OPEN.equals(submarketId)
                || Submarkets.GENERIC_MILITARY.equals(submarketId)
                || Submarkets.SUBMARKET_BLACK.equals(submarketId);
    }

    public static final class ObservedItem {
        private SubmarketWeaponStock cheapest;
        private boolean vanillaSupportedSeen;
        private boolean unsupportedSeen;

        private ObservedItem(SubmarketWeaponStock source, boolean vanillaSupported) {
            observe(source, vanillaSupported);
        }

        private void observe(SubmarketWeaponStock source, boolean vanillaSupported) {
            if (vanillaSupported) {
                vanillaSupportedSeen = true;
            } else {
                unsupportedSeen = true;
            }
            if (cheapest == null || compareReferenceSource(source, cheapest) < 0) {
                cheapest = source;
            }
        }

        public SubmarketWeaponStock getCheapestReferenceSource() {
            return cheapest;
        }

        public boolean isOnlyUnsupportedCustomSubmarket() {
            return unsupportedSeen && !vanillaSupportedSeen;
        }

        private static int compareReferenceSource(SubmarketWeaponStock left, SubmarketWeaponStock right) {
            int result = Integer.compare(left.getBaseUnitPrice(), right.getBaseUnitPrice());
            if (result != 0) {
                return result;
            }
            return left.getDisplaySourceName().compareToIgnoreCase(right.getDisplaySourceName());
        }
    }
}
