package weaponsprocurement.core;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import weaponsprocurement.internal.WeaponsProcurementConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GlobalWeaponMarketService {
    public static final String VIRTUAL_SUBMARKET_ID = "wp_fixers_market";
    public static final String SECTOR_MARKET_NAME = "Sector Market";
    public static final String FIXERS_MARKET_NAME = "Fixer's Market";
    public static final int VIRTUAL_STOCK = 999;

    private final MarketStockService marketStockService = new MarketStockService();
    private final ObservedStockIndex observedStockIndex = new ObservedStockIndex();
    private final TheoreticalSaleIndex theoreticalSaleIndex = new TheoreticalSaleIndex();
    private final FixerMarketObservedCatalog observedCatalog = new FixerMarketObservedCatalog();
    private String theoreticalCacheKey;
    private Map<String, TheoreticalSaleIndex.Candidate> theoreticalCache;

    public MarketStockService.MarketStock collectSectorWeaponStock(SectorAPI sector) {
        return buildSectorWeaponStock(sector, WeaponsProcurementConfig.sectorMarketPriceMultiplier());
    }

    public MarketStockService.MarketStock collectFixersWeaponStock(SectorAPI sector) {
        float priceMultiplier = WeaponsProcurementConfig.fixersMarketPriceMultiplier();
        WeaponMarketBlacklist blacklist = WeaponMarketBlacklist.load();
        return buildFixersWeaponStock(sector, priceMultiplier, blacklist);
    }

    /**
     * Sector Market is deliberately built from real market cargo entries. The
     * stock rows are marked up for WP pricing, but keep market/submarket ids so
     * confirmation can drain the actual remote cargo stacks.
     */
    private MarketStockService.MarketStock buildSectorWeaponStock(SectorAPI sector, float priceMultiplier) {
        WeaponMarketBlacklist blacklist = WeaponMarketBlacklist.load();
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
                if (blacklist.isBannedFromSector(itemKey)) {
                    continue;
                }
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
                                                                  float priceMultiplier,
                                                                  WeaponMarketBlacklist blacklist) {
        MarketStockService.MarketStockBuilder builder = new MarketStockService.MarketStockBuilder();
        Map<String, ReferenceItem> references = new HashMap<String, ReferenceItem>();
        addLiveObservedReferences(sector, references, blacklist);
        addPersistentObservedReferences(sector, references, blacklist);
        addTheoreticalCandidates(sector, references, blacklist);

        for (Map.Entry<String, ReferenceItem> entry : references.entrySet()) {
            ReferenceItem reference = entry.getValue();
            builder.add(entry.getKey(), new SubmarketWeaponStock(
                    VIRTUAL_SUBMARKET_ID,
                    FIXERS_MARKET_NAME,
                    VIRTUAL_STOCK,
                    markedUpPrice(reference.baseUnitPrice, priceMultiplier),
                    reference.baseUnitPrice,
                    reference.unitCargoSpace,
                    true),
                    reference.rarity);
        }
        return builder.build();
    }

    private void addLiveObservedReferences(SectorAPI sector,
                                           Map<String, ReferenceItem> references,
                                           WeaponMarketBlacklist blacklist) {
        Map<String, ObservedStockIndex.ObservedItem> observed = observedStockIndex.collect(sector, blacklist);
        for (Map.Entry<String, ObservedStockIndex.ObservedItem> entry : observed.entrySet()) {
            ObservedStockIndex.ObservedItem item = entry.getValue();
            SubmarketWeaponStock source = item == null ? null : item.getCheapestReferenceSource();
            if (source == null) {
                continue;
            }
            references.put(entry.getKey(), new ReferenceItem(
                    source.getBaseUnitPrice(),
                    source.getUnitCargoSpace(),
                    RarityClassifier.observedOnly(item)));
        }
    }

    private void addPersistentObservedReferences(SectorAPI sector,
                                                 Map<String, ReferenceItem> references,
                                                 WeaponMarketBlacklist blacklist) {
        Map<String, FixerMarketObservedCatalog.ObservedItem> observed = observedCatalog.observedItems(sector, blacklist);
        for (Map.Entry<String, FixerMarketObservedCatalog.ObservedItem> entry : observed.entrySet()) {
            String itemKey = entry.getKey();
            if (references.containsKey(itemKey)) {
                continue;
            }
            FixerMarketObservedCatalog.ObservedItem item = entry.getValue();
            references.put(itemKey, new ReferenceItem(
                    item.getBaseUnitPrice(),
                    item.getUnitCargoSpace(),
                    null));
        }
    }

    private void addTheoreticalCandidates(SectorAPI sector,
                                          Map<String, ReferenceItem> references,
                                          WeaponMarketBlacklist blacklist) {
        Map<String, TheoreticalSaleIndex.Candidate> candidates = theoreticalCandidates(sector, blacklist);
        for (Map.Entry<String, TheoreticalSaleIndex.Candidate> entry : candidates.entrySet()) {
            TheoreticalSaleIndex.Candidate candidate = entry.getValue();
            ReferenceItem current = references.get(entry.getKey());
            if (current == null) {
                references.put(entry.getKey(), new ReferenceItem(
                        candidate.getBaseUnitPrice(),
                        candidate.getUnitCargoSpace(),
                        candidate.getRarity()));
            } else if (current.rarity == null) {
                current.rarity = candidate.getRarity();
            }
        }
    }

    private Map<String, TheoreticalSaleIndex.Candidate> theoreticalCandidates(SectorAPI sector,
                                                                              WeaponMarketBlacklist blacklist) {
        String key = theoreticalCacheKey(sector, blacklist);
        if (key.equals(theoreticalCacheKey) && theoreticalCache != null) {
            return theoreticalCache;
        }
        theoreticalCacheKey = key;
        theoreticalCache = theoreticalSaleIndex.collect(sector, blacklist);
        return theoreticalCache;
    }

    private static String theoreticalCacheKey(SectorAPI sector, WeaponMarketBlacklist blacklist) {
        StringBuilder result = new StringBuilder();
        result.append("blacklist=").append(blacklist == null ? "none" : blacklist.cacheKey());
        Set<String> factionIds = new HashSet<String>();
        EconomyAPI economy = sector == null ? null : sector.getEconomy();
        List<MarketAPI> markets = economy == null ? null : economy.getMarketsCopy();
        if (markets != null) {
            for (int i = 0; i < markets.size(); i++) {
                MarketAPI market = markets.get(i);
                if (market == null) {
                    continue;
                }
                append(result, "m", market.getId());
                append(result, "mf", market.getFactionId());
                addFactionId(factionIds, market.getFactionId());
                List<SubmarketAPI> submarkets = market.getSubmarketsCopy();
                if (submarkets == null) {
                    continue;
                }
                for (int j = 0; j < submarkets.size(); j++) {
                    SubmarketAPI submarket = submarkets.get(j);
                    if (submarket == null) {
                        continue;
                    }
                    append(result, "s", submarket.getSpecId());
                    String submarketFactionId = submarket.getFaction() == null ? null : submarket.getFaction().getId();
                    append(result, "sf", submarketFactionId);
                    addFactionId(factionIds, submarketFactionId);
                }
            }
        }
        List<String> sortedFactionIds = new ArrayList<String>(factionIds);
        Collections.sort(sortedFactionIds);
        for (String factionId : sortedFactionIds) {
            appendFactionSignature(result, sector, factionId);
        }
        return result.toString();
    }

    private static void appendFactionSignature(StringBuilder result, SectorAPI sector, String factionId) {
        FactionAPI faction = safeFaction(sector, factionId);
        append(result, "f", factionId);
        if (faction == null) {
            append(result, "missing", "true");
            return;
        }
        append(result, "kw", Integer.toString(hash(faction.getKnownWeapons())));
        append(result, "kf", Integer.toString(hash(faction.getKnownFighters())));
        append(result, "wsf", Integer.toString(hash(faction.getWeaponSellFrequency())));
        append(result, "fsf", Integer.toString(hash(faction.getFighterSellFrequency())));
    }

    private static FactionAPI safeFaction(SectorAPI sector, String factionId) {
        try {
            return sector == null || factionId == null ? null : sector.getFaction(factionId);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static int hash(Object value) {
        return value == null ? 0 : value.hashCode();
    }

    private static void append(StringBuilder result, String key, String value) {
        result.append('|').append(key).append('=').append(value == null ? "" : value);
    }

    private static void addFactionId(Set<String> result, String factionId) {
        if (factionId != null && factionId.trim().length() > 0) {
            result.add(factionId);
        }
    }

    private static int markedUpPrice(int unitPrice, float priceMultiplier) {
        return Math.max(0, Math.round(Math.max(0, unitPrice) * Math.max(1f, priceMultiplier)));
    }

    private static final class ReferenceItem {
        private final int baseUnitPrice;
        private final float unitCargoSpace;
        private FixerRarity rarity;

        private ReferenceItem(int baseUnitPrice, float unitCargoSpace, FixerRarity rarity) {
            this.baseUnitPrice = Math.max(0, baseUnitPrice);
            this.unitCargoSpace = Math.max(0.01f, unitCargoSpace);
            this.rarity = rarity;
        }
    }
}
