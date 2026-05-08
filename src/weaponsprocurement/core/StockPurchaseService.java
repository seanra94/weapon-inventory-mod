package weaponsprocurement.core;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SubmarketPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class StockPurchaseService {
    private static final Logger LOG = Logger.getLogger(StockPurchaseService.class);

    public PurchaseResult buyCheapest(SectorAPI sector,
                                      MarketAPI market,
                                      String weaponId,
                                      int requestedQuantity,
                                      boolean includeBlackMarket) {
        return buyItem(sector, market, StockItemType.WEAPON, weaponId, null, requestedQuantity, includeBlackMarket);
    }

    public PurchaseResult buyCheapestItem(SectorAPI sector,
                                          MarketAPI market,
                                          StockItemType itemType,
                                          String itemId,
                                          int requestedQuantity,
                                          boolean includeBlackMarket) {
        return buyItem(sector, market, itemType, itemId, null, requestedQuantity, includeBlackMarket);
    }

    public PurchaseResult buyFromSubmarket(SectorAPI sector,
                                           MarketAPI market,
                                           String weaponId,
                                           String submarketId,
                                           int requestedQuantity,
                                           boolean includeBlackMarket) {
        return buyItem(sector, market, StockItemType.WEAPON, weaponId, submarketId, requestedQuantity, includeBlackMarket);
    }

    public PurchaseResult sellToMarket(SectorAPI sector,
                                       MarketAPI market,
                                       String weaponId,
                                       int requestedQuantity,
                                       boolean includeBlackMarket) {
        return sellItemToMarket(sector, market, StockItemType.WEAPON, weaponId, requestedQuantity, includeBlackMarket);
    }

    public PurchaseResult sellItemToMarket(SectorAPI sector,
                                           MarketAPI market,
                                           StockItemType itemType,
                                           String itemId,
                                           int requestedQuantity,
                                           boolean includeBlackMarket) {
        if (sector == null || market == null) {
            return PurchaseResult.failure("No active market context.");
        }
        if (itemId == null || itemId.isEmpty()) {
            return PurchaseResult.failure("No " + itemType.getSingularLabel().toLowerCase(java.util.Locale.US) + " selected.");
        }
        if (requestedQuantity <= 0) {
            return PurchaseResult.failure("Nothing to sell.");
        }

        CampaignFleetAPI fleet = sector.getPlayerFleet();
        CargoAPI playerCargo = fleet == null ? null : fleet.getCargo();
        if (playerCargo == null) {
            return PurchaseResult.failure("Player cargo is unavailable.");
        }
        CargoStackAPI playerStack = StockItemCargo.itemStack(playerCargo, itemType, itemId);
        int available = StockItemCargo.itemCount(playerCargo, itemType, itemId);
        if (playerStack == null || available <= 0) {
            return PurchaseResult.failure("No player-cargo stock is available to sell.");
        }
        int quantity = Math.min(requestedQuantity, available);
        SellTarget target = sellTarget(market, playerStack, includeBlackMarket);
        if (target == null) {
            return PurchaseResult.failure("No valid market buyer is available.");
        }

        int credits = target.unitPrice * quantity;
        int expectedMarketCount = StockItemCargo.itemCount(target.cargo, itemType, itemId) + quantity;
        try {
            StockItemCargo.removeItem(playerCargo, itemType, itemId, quantity);
            playerCargo.getCredits().add(credits);
            StockItemCargo.tidyCargo(playerCargo);
            StockItemCargo.addItem(target.cargo, itemType, itemId, quantity);
            StockItemCargo.tidyCargo(target.cargo);
            StockMarketTransactionReporter.reportItemTransaction(LOG, market, target.submarket, itemType, itemId, quantity, target.unitPrice, false);
            StockItemCargo.reconcileItemCount(target.cargo, itemType, itemId, expectedMarketCount);

            String message = "Sold " + quantity + " " + StockItemCargo.itemDisplayName(itemType, itemId) + " for " + CreditFormat.creditsLong(credits) + ".";
            if (Global.getSector() != null && Global.getSector().getCampaignUI() != null) {
                Global.getSector().getCampaignUI().addMessage(message);
            }
            return PurchaseResult.success(message, quantity, -credits);
        } catch (Throwable t) {
            return executionFailure("sell to market", itemType, itemId, quantity, t);
        }
    }

    public PurchaseResult buyFromFixersMarket(SectorAPI sector,
                                              String weaponId,
                                              int requestedQuantity,
                                              int unitPrice,
                                              float unitCargoSpace) {
        return buyItemFromFixersMarket(sector, StockItemType.WEAPON, weaponId, requestedQuantity, unitPrice, unitCargoSpace);
    }

    public PurchaseResult buyItemFromFixersMarket(SectorAPI sector,
                                                  StockItemType itemType,
                                                  String itemId,
                                                  int requestedQuantity,
                                                  int unitPrice,
                                                  float unitCargoSpace) {
        if (sector == null) {
            return PurchaseResult.failure("No active sector context.");
        }
        if (itemId == null || itemId.isEmpty()) {
            return PurchaseResult.failure("No " + itemType.getSingularLabel().toLowerCase(java.util.Locale.US) + " selected.");
        }
        if (requestedQuantity <= 0) {
            return PurchaseResult.failure("Nothing to buy.");
        }
        if (unitPrice < 0) {
            return PurchaseResult.failure("No fixer-market price is available.");
        }

        CampaignFleetAPI fleet = sector.getPlayerFleet();
        CargoAPI playerCargo = fleet == null ? null : fleet.getCargo();
        if (playerCargo == null) {
            return PurchaseResult.failure("Player cargo is unavailable.");
        }
        int totalCost = unitPrice * requestedQuantity;
        float totalSpace = Math.max(1f, unitCargoSpace) * requestedQuantity;
        if (playerCargo.getCredits().get() + 0.01f < totalCost) {
            return PurchaseResult.failure("Need " + CreditFormat.creditsLong(totalCost) + " for this order.");
        }
        if (playerCargo.getSpaceLeft() + 0.01f < totalSpace) {
            return PurchaseResult.failure("Need " + Math.round(totalSpace) + " cargo space for this order.");
        }

        try {
            StockItemCargo.addItem(playerCargo, itemType, itemId, requestedQuantity);
            playerCargo.getCredits().subtract(totalCost);
            StockItemCargo.tidyCargo(playerCargo);

            String message = "Bought " + requestedQuantity + " " + StockItemCargo.itemDisplayName(itemType, itemId)
                    + " from the fixer's market for " + CreditFormat.creditsLong(totalCost) + ".";
            if (Global.getSector() != null && Global.getSector().getCampaignUI() != null) {
                Global.getSector().getCampaignUI().addMessage(message);
            }
            return PurchaseResult.success(message, requestedQuantity, totalCost);
        } catch (Throwable t) {
            return executionFailure("buy from fixer's market", itemType, itemId, requestedQuantity, t);
        }
    }

    public PurchaseResult buyFromSectorSources(SectorAPI sector,
                                               String weaponId,
                                               int requestedQuantity,
                                               List<SubmarketWeaponStock> stockSources) {
        return buyItemFromSectorSources(sector, StockItemType.WEAPON, weaponId, requestedQuantity, stockSources);
    }

    public PurchaseResult buyItemFromSectorSources(SectorAPI sector,
                                                   StockItemType itemType,
                                                   String itemId,
                                                   int requestedQuantity,
                                                   List<SubmarketWeaponStock> stockSources) {
        if (sector == null) {
            return PurchaseResult.failure("No active sector context.");
        }
        if (itemId == null || itemId.isEmpty()) {
            return PurchaseResult.failure("No " + itemType.getSingularLabel().toLowerCase(java.util.Locale.US) + " selected.");
        }
        if (requestedQuantity <= 0) {
            return PurchaseResult.failure("Nothing to buy.");
        }
        if (stockSources == null || stockSources.isEmpty()) {
            return PurchaseResult.failure("No sector-market stock is available.");
        }

        CampaignFleetAPI fleet = sector.getPlayerFleet();
        CargoAPI playerCargo = fleet == null ? null : fleet.getCargo();
        if (playerCargo == null) {
            return PurchaseResult.failure("Player cargo is unavailable.");
        }

        List<PurchaseSource> sources = collectSectorSources(sector, itemType, itemId, stockSources);
        if (sources.isEmpty()) {
            return PurchaseResult.failure("No sector-market stock is available.");
        }
        Collections.sort(sources, PurchaseSource.PRICE_ORDER);

        PurchasePlan plan = PurchasePlan.build(sources, requestedQuantity);
        if (plan.totalQuantity <= 0) {
            return PurchaseResult.failure("No sector-market stock is available.");
        }
        if (playerCargo.getCredits().get() + 0.01f < plan.totalCost) {
            return PurchaseResult.failure("Need " + CreditFormat.creditsLong(plan.totalCost) + " for this order.");
        }
        if (playerCargo.getSpaceLeft() + 0.01f < plan.totalSpace) {
            return PurchaseResult.failure("Need " + Math.round(plan.totalSpace) + " cargo space for this order.");
        }

        try {
            for (PurchaseLine line : plan.lines) {
                StockItemCargo.removeItem(line.source.cargo, itemType, itemId, line.quantity);
                StockItemCargo.tidyCargo(line.source.cargo);
                StockMarketTransactionReporter.reportItemTransaction(LOG, line.source.market, line.source.submarket, itemType, itemId, line.quantity, line.source.unitPrice, true);
            }
            StockItemCargo.addItem(playerCargo, itemType, itemId, plan.totalQuantity);
            playerCargo.getCredits().subtract(plan.totalCost);
            StockItemCargo.tidyCargo(playerCargo);

            String message = "Bought " + plan.totalQuantity + " " + StockItemCargo.itemDisplayName(itemType, itemId)
                    + " from the sector market for " + CreditFormat.creditsLong(plan.totalCost) + ".";
            if (Global.getSector() != null && Global.getSector().getCampaignUI() != null) {
                Global.getSector().getCampaignUI().addMessage(message);
            }
            return PurchaseResult.success(message, plan.totalQuantity, plan.totalCost);
        } catch (Throwable t) {
            return executionFailure("buy from sector market", itemType, itemId, plan.totalQuantity, t);
        }
    }

    private PurchaseResult buyItem(SectorAPI sector,
                                   MarketAPI market,
                                   StockItemType itemType,
                                   String itemId,
                                   String onlySubmarketId,
                                   int requestedQuantity,
                                   boolean includeBlackMarket) {
        if (sector == null || market == null) {
            return PurchaseResult.failure("No active market context.");
        }
        if (itemId == null || itemId.isEmpty()) {
            return PurchaseResult.failure("No " + itemType.getSingularLabel().toLowerCase(java.util.Locale.US) + " selected.");
        }
        if (requestedQuantity <= 0) {
            return PurchaseResult.failure("Nothing to buy.");
        }

        CampaignFleetAPI fleet = sector.getPlayerFleet();
        CargoAPI playerCargo = fleet == null ? null : fleet.getCargo();
        if (playerCargo == null) {
            return PurchaseResult.failure("Player cargo is unavailable.");
        }

        List<PurchaseSource> sources = collectSources(market, itemType, itemId, onlySubmarketId, includeBlackMarket);
        if (sources.isEmpty()) {
            return PurchaseResult.failure("No purchasable stock is available.");
        }
        Collections.sort(sources, PurchaseSource.PRICE_ORDER);

        PurchasePlan plan = PurchasePlan.build(sources, requestedQuantity);
        if (plan.totalQuantity <= 0) {
            return PurchaseResult.failure("No purchasable stock is available.");
        }
        if (playerCargo.getCredits().get() + 0.01f < plan.totalCost) {
            return PurchaseResult.failure("Need " + CreditFormat.creditsLong(plan.totalCost) + " for this order.");
        }
        if (playerCargo.getSpaceLeft() + 0.01f < plan.totalSpace) {
            return PurchaseResult.failure("Need " + Math.round(plan.totalSpace) + " cargo space for this order.");
        }

        try {
            for (PurchaseLine line : plan.lines) {
                StockItemCargo.removeItem(line.source.cargo, itemType, itemId, line.quantity);
                StockItemCargo.tidyCargo(line.source.cargo);
                StockMarketTransactionReporter.reportItemTransaction(LOG, market, line.source.submarket, itemType, itemId, line.quantity, line.source.unitPrice, true);
            }
            StockItemCargo.addItem(playerCargo, itemType, itemId, plan.totalQuantity);
            playerCargo.getCredits().subtract(plan.totalCost);
            StockItemCargo.tidyCargo(playerCargo);

            String message = "Bought " + plan.totalQuantity + " " + StockItemCargo.itemDisplayName(itemType, itemId) + " for " + CreditFormat.creditsLong(plan.totalCost) + ".";
            if (Global.getSector() != null && Global.getSector().getCampaignUI() != null) {
                Global.getSector().getCampaignUI().addMessage(message);
            }
            return PurchaseResult.success(message, plan.totalQuantity, plan.totalCost);
        } catch (Throwable t) {
            return executionFailure("buy from local market", itemType, itemId, plan.totalQuantity, t);
        }
    }

    private static List<PurchaseSource> collectSources(MarketAPI market,
                                                       StockItemType itemType,
                                                       String itemId,
                                                       String onlySubmarketId,
                                                       boolean includeBlackMarket) {
        List<PurchaseSource> result = new ArrayList<PurchaseSource>();
        if (market.getSubmarketsCopy() == null) {
            return result;
        }
        for (SubmarketAPI submarket : market.getSubmarketsCopy()) {
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
                if (!MarketStockService.isPurchasableItemStack(submarket, stack, itemType)) {
                    continue;
                }
                if (!itemId.equals(MarketStockService.itemId(stack, itemType))) {
                    continue;
                }
                int available = Math.round(stack.getSize());
                if (available > 0) {
                    result.add(new PurchaseSource(submarket, cargo, available,
                            MarketStockService.unitPrice(submarket, stack),
                            MarketStockService.unitCargoSpace(stack)));
                }
            }
        }
        return result;
    }

    private static List<PurchaseSource> collectSectorSources(SectorAPI sector,
                                                             StockItemType itemType,
                                                             String itemId,
                                                             List<SubmarketWeaponStock> stockSources) {
        List<PurchaseSource> result = new ArrayList<PurchaseSource>();
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
            result.add(new PurchaseSource(market, submarket, cargo, available,
                    stock.getUnitPrice(),
                    MarketStockService.unitCargoSpace(stack)));
        }
        return result;
    }

    private static SellTarget sellTarget(MarketAPI market, CargoStackAPI playerStack, boolean includeBlackMarket) {
        if (market == null || market.getSubmarketsCopy() == null || playerStack == null) {
            return null;
        }
        SellTarget bestBlackMarket = null;
        SellTarget bestLegalMarket = null;
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
            SellTarget candidate = new SellTarget(submarket, cargo, MarketStockService.sellUnitPrice(submarket, playerStack));
            if (plugin != null && plugin.isBlackMarket()) {
                bestBlackMarket = betterSellTarget(bestBlackMarket, candidate);
            } else {
                bestLegalMarket = betterSellTarget(bestLegalMarket, candidate);
            }
        }
        return includeBlackMarket && bestBlackMarket != null ? bestBlackMarket : bestLegalMarket;
    }

    private static SellTarget betterSellTarget(SellTarget current, SellTarget candidate) {
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

    private static PurchaseResult executionFailure(String operation,
                                                   StockItemType itemType,
                                                   String itemId,
                                                   int quantity,
                                                   Throwable t) {
        LOG.error("WP_STOCK_REVIEW trade execution failed operation=" + operation
                + " item=" + itemType.key(itemId)
                + " quantity=" + quantity, t);
        return PurchaseResult.failure("Trade failed during execution. Check starsector.log.");
    }

    private static final class PurchaseSource {
        static final Comparator<PurchaseSource> PRICE_ORDER = PurchaseSourcePriceComparator.INSTANCE;

        final MarketAPI market;
        final SubmarketAPI submarket;
        final CargoAPI cargo;
        final int available;
        final int unitPrice;
        final float unitCargoSpace;

        PurchaseSource(SubmarketAPI submarket, CargoAPI cargo, int available, int unitPrice, float unitCargoSpace) {
            this(null, submarket, cargo, available, unitPrice, unitCargoSpace);
        }

        PurchaseSource(MarketAPI market,
                       SubmarketAPI submarket,
                       CargoAPI cargo,
                       int available,
                       int unitPrice,
                       float unitCargoSpace) {
            this.market = market;
            this.submarket = submarket;
            this.cargo = cargo;
            this.available = available;
            this.unitPrice = unitPrice;
            this.unitCargoSpace = unitCargoSpace;
        }
    }

    private static final class PurchaseSourcePriceComparator implements Comparator<PurchaseSource> {
        static final PurchaseSourcePriceComparator INSTANCE = new PurchaseSourcePriceComparator();

        @Override
        public int compare(PurchaseSource left, PurchaseSource right) {
            int result = Integer.compare(left.unitPrice, right.unitPrice);
            if (result != 0) {
                return result;
            }
            return sourceName(left).compareToIgnoreCase(sourceName(right));
        }

        private static String sourceName(PurchaseSource source) {
            if (source == null || source.submarket == null) {
                return "";
            }
            return source.submarket.getNameOneLine();
        }
    }

    private static final class PurchaseLine {
        final PurchaseSource source;
        final int quantity;

        PurchaseLine(PurchaseSource source, int quantity) {
            this.source = source;
            this.quantity = quantity;
        }
    }

    private static final class PurchasePlan {
        final List<PurchaseLine> lines;
        final int totalQuantity;
        final int totalCost;
        final float totalSpace;

        private PurchasePlan(List<PurchaseLine> lines, int totalQuantity, int totalCost, float totalSpace) {
            this.lines = lines;
            this.totalQuantity = totalQuantity;
            this.totalCost = totalCost;
            this.totalSpace = totalSpace;
        }

        static PurchasePlan build(List<PurchaseSource> sources, int requestedQuantity) {
            int remaining = requestedQuantity;
            int totalQuantity = 0;
            int totalCost = 0;
            float totalSpace = 0f;
            List<PurchaseLine> lines = new ArrayList<PurchaseLine>();
            for (PurchaseSource source : sources) {
                if (remaining <= 0) {
                    break;
                }
                int quantity = Math.min(remaining, source.available);
                if (quantity <= 0) {
                    continue;
                }
                lines.add(new PurchaseLine(source, quantity));
                remaining -= quantity;
                totalQuantity += quantity;
                totalCost += source.unitPrice * quantity;
                totalSpace += source.unitCargoSpace * quantity;
            }
            return new PurchasePlan(lines, totalQuantity, totalCost, totalSpace);
        }
    }

    private static final class SellTarget {
        final SubmarketAPI submarket;
        final CargoAPI cargo;
        final int unitPrice;

        SellTarget(SubmarketAPI submarket, CargoAPI cargo, int unitPrice) {
            this.submarket = submarket;
            this.cargo = cargo;
            this.unitPrice = unitPrice;
        }
    }

    public static final class PurchaseResult {
        private final boolean success;
        private final String message;
        private final int quantity;
        private final int credits;

        private PurchaseResult(boolean success, String message, int quantity, int credits) {
            this.success = success;
            this.message = message;
            this.quantity = quantity;
            this.credits = credits;
        }

        public static PurchaseResult success(String message, int quantity, int credits) {
            return new PurchaseResult(true, message, quantity, credits);
        }

        public static PurchaseResult failure(String message) {
            return new PurchaseResult(false, message, 0, 0);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public int getQuantity() {
            return quantity;
        }

        public int getCredits() {
            return credits;
        }
    }
}
