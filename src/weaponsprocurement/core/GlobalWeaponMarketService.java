package weaponsprocurement.core;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import weaponsprocurement.internal.WeaponsProcurementConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GlobalWeaponMarketService {
    public static final String VIRTUAL_SUBMARKET_ID = "wp_fixers_market";
    public static final String SECTOR_MARKET_NAME = "Sector Market";
    public static final String FIXERS_MARKET_NAME = "Fixer's Market";
    public static final int VIRTUAL_STOCK = 999;

    private final MarketStockService marketStockService = new MarketStockService();
    private final ObservedStockIndex observedStockIndex = new ObservedStockIndex();
    private final TheoreticalSaleIndex theoreticalSaleIndex = new TheoreticalSaleIndex();
    private final FixerMarketObservedCatalog observedCatalog = new FixerMarketObservedCatalog();

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
        Map<String, TheoreticalSaleIndex.Candidate> candidates = theoreticalSaleIndex.collect(sector, blacklist);
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
