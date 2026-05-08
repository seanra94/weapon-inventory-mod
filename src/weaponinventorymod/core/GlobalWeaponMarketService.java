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
    public static final String SECTOR_MARKET_NAME = "Sector Market";
    public static final String FIXERS_MARKET_NAME = "Fixer's Market";
    public static final int VIRTUAL_STOCK = 999;

    private final MarketStockService marketStockService = new MarketStockService();
    private final Map<String, MarketStockService.MarketStock> cache = new HashMap<String, MarketStockService.MarketStock>();

    public MarketStockService.MarketStock collectSectorWeaponStock(SectorAPI sector) {
        return buildSectorWeaponStock(sector, WeaponInventoryConfig.sectorMarketPriceMultiplier());
    }

    public MarketStockService.MarketStock collectFixersWeaponStock(SectorAPI sector) {
        boolean includeInferred = WeaponInventoryConfig.isGlobalMarketTagInferenceEnabled();
        float priceMultiplier = WeaponInventoryConfig.secretMarketPriceMultiplier();
        String key = "secret|" + includeInferred + "|" + priceMultiplier;
        MarketStockService.MarketStock cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        MarketStockService.MarketStock result = buildFixersWeaponStock(sector, includeInferred, priceMultiplier);
        cache.put(key, result);
        return result;
    }

    /**
     * Sector Market is deliberately built from real market cargo entries. The
     * stock rows are marked up for WIM pricing, but keep market/submarket ids so
     * confirmation can drain the actual remote cargo stacks.
     */
    private MarketStockService.MarketStock buildSectorWeaponStock(SectorAPI sector, float priceMultiplier) {
        MarketStockService.MarketStockBuilder builder = new MarketStockService.MarketStockBuilder();
        EconomyAPI economy = sector == null ? null : sector.getEconomy();
        List<MarketAPI> markets = economy == null ? null : economy.getMarketsCopy();
        if (markets == null) {
            return builder.build();
        }
        for (int i = 0; i < markets.size(); i++) {
            MarketAPI market = markets.get(i);
            MarketStockService.MarketStock stock = marketStockService.collectCurrentMarketItemStock(market, true);
            for (String itemKey : stock.itemKeys()) {
                List<SubmarketWeaponStock> sources = stock.getSubmarketStocks(itemKey);
                for (int j = 0; j < sources.size(); j++) {
                    SubmarketWeaponStock source = sources.get(j);
                    if (source.getCount() <= 0) {
                        continue;
                    }
                    builder.add(itemKey, new SubmarketWeaponStock(
                            source.getMarketId(),
                            source.getMarketName(),
                            source.getSubmarketId(),
                            source.getSubmarketName(),
                            source.getCount(),
                            markedUpPrice(source.getBaseUnitPrice(), priceMultiplier),
                            source.getBaseUnitPrice(),
                            source.getUnitCargoSpace(),
                            source.isPurchasable()));
                }
            }
        }
        return builder.build();
    }

    private MarketStockService.MarketStock buildFixersWeaponStock(SectorAPI sector,
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
            MarketStockService.MarketStock stock = marketStockService.collectCurrentMarketItemStock(market, true);
            for (String itemKey : stock.itemKeys()) {
                List<SubmarketWeaponStock> sources = stock.getSubmarketStocks(itemKey);
                for (int j = 0; j < sources.size(); j++) {
                    SubmarketWeaponStock source = sources.get(j);
                    if (source.getCount() <= 0) {
                        continue;
                    }
                    SubmarketWeaponStock current = cheapestByWeapon.get(itemKey);
                    if (current == null || compareReferenceSource(source, current) < 0) {
                        cheapestByWeapon.put(itemKey, source);
                    }
                }
            }
        }
        if (includeInferred) {
            addInferredFactionWeapons(sector, activeFactionIds, cheapestByWeapon);
        }
        for (Map.Entry<String, SubmarketWeaponStock> entry : cheapestByWeapon.entrySet()) {
            SubmarketWeaponStock source = entry.getValue();
            builder.add(entry.getKey(), new SubmarketWeaponStock(
                    VIRTUAL_SUBMARKET_ID,
                    FIXERS_MARKET_NAME,
                    VIRTUAL_STOCK,
                    markedUpPrice(source.getBaseUnitPrice(), priceMultiplier),
                    source.getBaseUnitPrice(),
                    source.getUnitCargoSpace(),
                    true));
        }
        return builder.build();
    }

    private static void addInferredFactionWeapons(SectorAPI sector,
                                                  Set<String> activeFactionIds,
                                                  Map<String, SubmarketWeaponStock> cheapestByWeapon) {
        if (sector == null || activeFactionIds == null || activeFactionIds.isEmpty()) {
            return;
        }
        for (String factionId : activeFactionIds) {
            FactionAPI faction = sector.getFaction(factionId);
            for (String weaponId : inferredWeaponIds(faction)) {
                String itemKey = StockItemType.WEAPON.key(weaponId);
                if (cheapestByWeapon.containsKey(itemKey) || !isSafeInferredWeapon(weaponId)) {
                    continue;
                }
                WeaponSpecAPI spec = safeWeaponSpec(weaponId);
                if (spec == null) {
                    continue;
                }
                cheapestByWeapon.put(itemKey, new SubmarketWeaponStock(
                        VIRTUAL_SUBMARKET_ID,
                        FIXERS_MARKET_NAME,
                        VIRTUAL_STOCK,
                        Math.max(0, Math.round(spec.getBaseValue())),
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

    private static int compareReferenceSource(SubmarketWeaponStock left, SubmarketWeaponStock right) {
        int result = Integer.compare(left.getBaseUnitPrice(), right.getBaseUnitPrice());
        if (result != 0) {
            return result;
        }
        return left.getDisplaySourceName().compareToIgnoreCase(right.getDisplaySourceName());
    }

    private static WeaponSpecAPI safeWeaponSpec(String weaponId) {
        try {
            return Global.getSettings().getWeaponSpec(weaponId);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
