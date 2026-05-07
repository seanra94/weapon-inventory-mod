package weaponinventorymod.core;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import weaponinventorymod.internal.WeaponInventoryConfig;

import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GlobalWeaponMarketService {
    public static final String VIRTUAL_SUBMARKET_ID = "wim_global_weapon_market";
    public static final String VIRTUAL_SUBMARKET_NAME = "Global Weapon Market";
    public static final int VIRTUAL_STOCK = 999;

    private final MarketStockService marketStockService = new MarketStockService();
    private final Map<String, MarketStockService.MarketStock> cache = new HashMap<String, MarketStockService.MarketStock>();

    public MarketStockService.MarketStock collectGlobalWeaponStock(SectorAPI sector, boolean includeBlackMarket) {
        boolean includeInferred = WeaponInventoryConfig.isGlobalMarketTagInferenceEnabled();
        float priceMultiplier = WeaponInventoryConfig.globalMarketPriceMultiplier();
        String key = includeBlackMarket + "|" + includeInferred + "|" + priceMultiplier;
        MarketStockService.MarketStock cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        MarketStockService.MarketStock result = buildGlobalWeaponStock(sector, includeBlackMarket, includeInferred, priceMultiplier);
        cache.put(key, result);
        return result;
    }

    private MarketStockService.MarketStock buildGlobalWeaponStock(SectorAPI sector,
                                                                  boolean includeBlackMarket,
                                                                  boolean includeInferred,
                                                                  float priceMultiplier) {
        MarketStockService.MarketStockBuilder builder = new MarketStockService.MarketStockBuilder();
        EconomyAPI economy = sector == null ? null : sector.getEconomy();
        List<MarketAPI> markets = economy == null ? null : economy.getMarketsCopy();
        if (markets == null) {
            return builder.build();
        }
        Map<String, SubmarketWeaponStock> cheapestByWeapon = new HashMap<String, SubmarketWeaponStock>();
        Set<String> activeFactionIds = new HashSet<String>();
        for (int i = 0; i < markets.size(); i++) {
            MarketAPI market = markets.get(i);
            if (market != null && market.getFactionId() != null && !market.getFactionId().isEmpty()) {
                activeFactionIds.add(market.getFactionId());
            }
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
        if (includeInferred) {
            addInferredFactionWeapons(sector, activeFactionIds, cheapestByWeapon, priceMultiplier);
        }
        for (Map.Entry<String, SubmarketWeaponStock> entry : cheapestByWeapon.entrySet()) {
            SubmarketWeaponStock source = entry.getValue();
            builder.add(entry.getKey(), new SubmarketWeaponStock(
                    VIRTUAL_SUBMARKET_ID,
                    VIRTUAL_SUBMARKET_NAME,
                    VIRTUAL_STOCK,
                    markedUpPrice(source.getUnitPrice(), priceMultiplier),
                    source.getUnitCargoSpace(),
                    true));
        }
        return builder.build();
    }

    private static void addInferredFactionWeapons(SectorAPI sector,
                                                  Set<String> activeFactionIds,
                                                  Map<String, SubmarketWeaponStock> cheapestByWeapon,
                                                  float priceMultiplier) {
        if (sector == null || activeFactionIds == null || activeFactionIds.isEmpty()) {
            return;
        }
        for (String factionId : activeFactionIds) {
            FactionAPI faction = sector.getFaction(factionId);
            for (String weaponId : inferredWeaponIds(faction)) {
                if (cheapestByWeapon.containsKey(weaponId) || !isSafeInferredWeapon(weaponId)) {
                    continue;
                }
                WeaponSpecAPI spec = safeWeaponSpec(weaponId);
                if (spec == null) {
                    continue;
                }
                cheapestByWeapon.put(weaponId, new SubmarketWeaponStock(
                        VIRTUAL_SUBMARKET_ID,
                        VIRTUAL_SUBMARKET_NAME,
                        VIRTUAL_STOCK,
                        markedUpPrice(Math.max(0, Math.round(spec.getBaseValue())), priceMultiplier),
                        1f,
                        true));
            }
        }
    }

    private static Set<String> inferredWeaponIds(FactionAPI faction) {
        if (faction == null) {
            return Collections.emptySet();
        }
        Map<String, Float> sellFrequency = faction.getWeaponSellFrequency();
        if (sellFrequency != null && !sellFrequency.isEmpty()) {
            return sellFrequency.keySet();
        }
        Set<String> knownWeapons = faction.getKnownWeapons();
        return knownWeapons == null ? Collections.<String>emptySet() : knownWeapons;
    }

    private static boolean isSafeInferredWeapon(String weaponId) {
        WeaponSpecAPI spec = safeWeaponSpec(weaponId);
        if (spec == null || spec.getTags() == null) {
            return false;
        }
        Set<String> tags = spec.getTags();
        return !contains(tags, "restricted")
                && !contains(tags, "no_dealer")
                && !contains(tags, "no_drop")
                && !contains(tags, "no_bp_drop")
                && !contains(tags, "omega")
                && !contains(tags, "dweller")
                && !contains(tags, "threat")
                && !contains(tags, "hide_in_codex")
                && !contains(tags, "codex_unlockable");
    }

    private static boolean contains(Set<String> tags, String tag) {
        return tags != null && tag != null && tags.contains(tag);
    }

    private static int markedUpPrice(int unitPrice, float priceMultiplier) {
        return Math.max(0, Math.round(Math.max(0, unitPrice) * Math.max(1f, priceMultiplier)));
    }

    private static WeaponSpecAPI safeWeaponSpec(String weaponId) {
        try {
            return Global.getSettings().getWeaponSpec(weaponId);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
