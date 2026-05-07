package weaponinventorymod.core;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GlobalWeaponMarketService {
    public static final String VIRTUAL_SUBMARKET_ID = "wim_global_weapon_market";
    public static final String VIRTUAL_SUBMARKET_NAME = "Global Weapon Market";
    public static final int VIRTUAL_STOCK = 999;

    private final MarketStockService marketStockService = new MarketStockService();
    private final Map<Boolean, MarketStockService.MarketStock> cache = new HashMap<Boolean, MarketStockService.MarketStock>();

    public MarketStockService.MarketStock collectGlobalWeaponStock(SectorAPI sector, boolean includeBlackMarket) {
        Boolean key = Boolean.valueOf(includeBlackMarket);
        MarketStockService.MarketStock cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        MarketStockService.MarketStock result = buildGlobalWeaponStock(sector, includeBlackMarket);
        cache.put(key, result);
        return result;
    }

    private MarketStockService.MarketStock buildGlobalWeaponStock(SectorAPI sector, boolean includeBlackMarket) {
        MarketStockService.MarketStockBuilder builder = new MarketStockService.MarketStockBuilder();
        EconomyAPI economy = sector == null ? null : sector.getEconomy();
        List<MarketAPI> markets = economy == null ? null : economy.getMarketsCopy();
        if (markets == null) {
            return builder.build();
        }
        Map<String, SubmarketWeaponStock> cheapestByWeapon = new HashMap<String, SubmarketWeaponStock>();
        for (int i = 0; i < markets.size(); i++) {
            MarketAPI market = markets.get(i);
            MarketStockService.MarketStock stock = marketStockService.collectCurrentMarketWeaponStock(market, includeBlackMarket);
            for (String weaponId : stock.weaponIds()) {
                List<SubmarketWeaponStock> sources = stock.getSubmarketStocks(weaponId);
                for (int j = 0; j < sources.size(); j++) {
                    SubmarketWeaponStock source = sources.get(j);
                    if (source.getCount() <= 0) {
                        continue;
                    }
                    SubmarketWeaponStock current = cheapestByWeapon.get(weaponId);
                    if (current == null || source.getUnitPrice() < current.getUnitPrice()) {
                        cheapestByWeapon.put(weaponId, source);
                    }
                }
            }
        }
        for (Map.Entry<String, SubmarketWeaponStock> entry : cheapestByWeapon.entrySet()) {
            SubmarketWeaponStock source = entry.getValue();
            builder.add(entry.getKey(), new SubmarketWeaponStock(
                    VIRTUAL_SUBMARKET_ID,
                    VIRTUAL_SUBMARKET_NAME,
                    VIRTUAL_STOCK,
                    source.getUnitPrice(),
                    source.getUnitCargoSpace(),
                    true));
        }
        return builder.build();
    }
}
