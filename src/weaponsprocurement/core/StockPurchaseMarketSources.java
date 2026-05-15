package weaponsprocurement.core;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SubmarketPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;

import java.util.ArrayList;
import java.util.List;

final class StockPurchaseMarketSources {
    private StockPurchaseMarketSources() {
    }

    static List<StockPurchaseSource> collectLocalSources(MarketAPI market,
                                                         StockItemType itemType,
                                                         String itemId,
                                                         String onlySubmarketId,
                                                         boolean includeBlackMarket) {
        List<StockPurchaseSource> result = new ArrayList<StockPurchaseSource>();
        if (market.getSubmarketsCopy() == null) {
            return result;
        }
        for (SubmarketAPI submarket : market.getSubmarketsCopy()) {
            if (submarket == null) {
                continue;
            }
            if (onlySubmarketId != null && !onlySubmarketId.equals(submarket.getSpecId())) {
                continue;
            }
            if (!MarketStockService.isTradeSubmarket(submarket, includeBlackMarket)) {
                continue;
            }
            CargoAPI cargo = submarket.getCargoNullOk();
            if (cargo == null || cargo.getStacksCopy() == null) {
                continue;
            }
            for (CargoStackAPI stack : cargo.getStacksCopy()) {
                if (!StockItemStacks.isPurchasableItemStack(submarket, stack, itemType)) {
                    continue;
                }
                if (!itemId.equals(StockItemStacks.itemId(stack, itemType))) {
                    continue;
                }
                int available = Math.round(stack.getSize());
                if (available > 0) {
                    result.add(new StockPurchaseSource(submarket, cargo, available,
                            StockItemStacks.unitPrice(submarket, stack),
                            StockItemStacks.unitCargoSpace(stack)));
                }
            }
        }
        return result;
    }

    static List<StockPurchaseSource> collectSectorSources(SectorAPI sector,
                                                          StockItemType itemType,
                                                          String itemId,
                                                          List<SubmarketWeaponStock> stockSources) {
        List<StockPurchaseSource> result = new ArrayList<StockPurchaseSource>();
        if (stockSources == null) {
            return result;
        }
        for (int i = 0; i < stockSources.size(); i++) {
            SubmarketWeaponStock stock = stockSources.get(i);
            if (stock == null || !stock.isPurchasable() || stock.getCount() <= 0) {
                continue;
            }
            MarketAPI market = findMarket(sector, stock.getMarketId());
            SubmarketAPI submarket = market == null ? null : market.getSubmarket(stock.getSubmarketId());
            CargoAPI cargo = submarket == null ? null : submarket.getCargoNullOk();
            CargoStackAPI stack = StockItemCargo.itemStack(cargo, itemType, itemId);
            int liveAvailable = stack == null ? 0 : Math.round(stack.getSize());
            int available = Math.min(stock.getCount(), liveAvailable);
            if (available <= 0) {
                continue;
            }
            result.add(new StockPurchaseSource(market, submarket, cargo, available,
                    stock.getUnitPrice(),
                    StockItemStacks.unitCargoSpace(stack)));
        }
        return result;
    }

    static StockSellTarget sellTarget(MarketAPI market, CargoStackAPI playerStack, boolean includeBlackMarket) {
        if (market == null || market.getSubmarketsCopy() == null || playerStack == null) {
            return null;
        }
        StockSellTarget bestBlackMarket = null;
        StockSellTarget bestLegalMarket = null;
        for (SubmarketAPI submarket : market.getSubmarketsCopy()) {
            if (submarket == null) {
                continue;
            }
            if (!MarketStockService.isTradeSubmarket(submarket, includeBlackMarket)) {
                continue;
            }
            CargoAPI cargo = submarket.getCargoNullOk();
            if (cargo == null) {
                continue;
            }
            SubmarketPlugin plugin = submarket.getPlugin();
            if (plugin != null && plugin.isIllegalOnSubmarket(playerStack, SubmarketPlugin.TransferAction.PLAYER_SELL)) {
                continue;
            }
            StockSellTarget candidate = new StockSellTarget(submarket, cargo, StockItemStacks.sellUnitPrice(submarket, playerStack));
            if (plugin != null && plugin.isBlackMarket()) {
                bestBlackMarket = betterSellTarget(bestBlackMarket, candidate);
            } else {
                bestLegalMarket = betterSellTarget(bestLegalMarket, candidate);
            }
        }
        return includeBlackMarket && bestBlackMarket != null ? bestBlackMarket : bestLegalMarket;
    }

    private static StockSellTarget betterSellTarget(StockSellTarget current, StockSellTarget candidate) {
        if (current == null || candidate.unitPrice > current.unitPrice) {
            return candidate;
        }
        return current;
    }

    private static MarketAPI findMarket(SectorAPI sector, String marketId) {
        if (sector == null || sector.getEconomy() == null || marketId == null || marketId.isEmpty()) {
            return null;
        }
        List<MarketAPI> markets = sector.getEconomy().getMarketsCopy();
        if (markets == null) {
            return null;
        }
        for (int i = 0; i < markets.size(); i++) {
            MarketAPI market = markets.get(i);
            if (market != null && marketId.equals(market.getId())) {
                return market;
            }
        }
        return null;
    }
}
