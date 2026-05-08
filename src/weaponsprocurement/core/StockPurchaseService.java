package weaponsprocurement.core;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import org.apache.log4j.Logger;

import java.util.Collections;
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
        PurchaseResult validation = StockPurchaseChecks.marketContext(sector, market);
        if (validation != null) {
            return validation;
        }
        validation = StockPurchaseChecks.selectedItem(itemType, itemId);
        if (validation != null) {
            return validation;
        }
        validation = StockPurchaseChecks.positiveQuantity(requestedQuantity, "sell");
        if (validation != null) {
            return validation;
        }

        CargoAPI playerCargo = StockPurchaseChecks.playerCargo(sector);
        validation = StockPurchaseChecks.playerCargoAvailable(playerCargo);
        if (validation != null) {
            return validation;
        }
        CargoStackAPI playerStack = StockItemCargo.itemStack(playerCargo, itemType, itemId);
        int available = StockItemCargo.itemCount(playerCargo, itemType, itemId);
        if (playerStack == null || available <= 0) {
            return PurchaseResult.failure("No player-cargo stock is available to sell.");
        }
        int quantity = Math.min(requestedQuantity, available);
        StockSellTarget target = StockPurchaseMarketSources.sellTarget(market, playerStack, includeBlackMarket);
        if (target == null) {
            return PurchaseResult.failure("No valid market buyer is available.");
        }

        return StockPurchaseExecutor.sellToMarket(LOG, market, playerCargo, target, itemType, itemId, quantity);
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
        PurchaseResult validation = StockPurchaseChecks.sectorContext(sector);
        if (validation != null) {
            return validation;
        }
        validation = StockPurchaseChecks.selectedItem(itemType, itemId);
        if (validation != null) {
            return validation;
        }
        validation = StockPurchaseChecks.positiveQuantity(requestedQuantity, "buy");
        if (validation != null) {
            return validation;
        }
        if (unitPrice < 0) {
            return PurchaseResult.failure("No fixer-market price is available.");
        }

        CargoAPI playerCargo = StockPurchaseChecks.playerCargo(sector);
        validation = StockPurchaseChecks.playerCargoAvailable(playerCargo);
        if (validation != null) {
            return validation;
        }
        int totalCost = unitPrice * requestedQuantity;
        float totalSpace = Math.max(1f, unitCargoSpace) * requestedQuantity;
        validation = StockPurchaseChecks.canCompletePurchase(playerCargo, totalCost, totalSpace);
        if (validation != null) {
            return validation;
        }

        return StockPurchaseExecutor.buyFromFixersMarket(LOG, playerCargo, itemType, itemId, requestedQuantity, totalCost);
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
        PurchaseResult validation = StockPurchaseChecks.sectorContext(sector);
        if (validation != null) {
            return validation;
        }
        validation = StockPurchaseChecks.selectedItem(itemType, itemId);
        if (validation != null) {
            return validation;
        }
        validation = StockPurchaseChecks.positiveQuantity(requestedQuantity, "buy");
        if (validation != null) {
            return validation;
        }
        if (stockSources == null || stockSources.isEmpty()) {
            return PurchaseResult.failure("No sector-market stock is available.");
        }

        CargoAPI playerCargo = StockPurchaseChecks.playerCargo(sector);
        validation = StockPurchaseChecks.playerCargoAvailable(playerCargo);
        if (validation != null) {
            return validation;
        }

        List<StockPurchaseSource> sources = StockPurchaseMarketSources.collectSectorSources(sector, itemType, itemId, stockSources);
        if (sources.isEmpty()) {
            return PurchaseResult.failure("No sector-market stock is available.");
        }
        Collections.sort(sources, StockPurchaseSource.PRICE_ORDER);

        StockPurchasePlan plan = StockPurchasePlan.build(sources, requestedQuantity);
        validation = StockPurchaseChecks.buyPlanAvailable(plan, "No sector-market stock is available.");
        if (validation != null) {
            return validation;
        }
        validation = StockPurchaseChecks.canCompletePurchase(playerCargo, plan);
        if (validation != null) {
            return validation;
        }

        return StockPurchaseExecutor.buyPlan(LOG, playerCargo, null, itemType, itemId, plan, " from the sector market", "buy from sector market");
    }

    private PurchaseResult buyItem(SectorAPI sector,
                                   MarketAPI market,
                                   StockItemType itemType,
                                   String itemId,
                                   String onlySubmarketId,
                                   int requestedQuantity,
                                   boolean includeBlackMarket) {
        PurchaseResult validation = StockPurchaseChecks.marketContext(sector, market);
        if (validation != null) {
            return validation;
        }
        validation = StockPurchaseChecks.selectedItem(itemType, itemId);
        if (validation != null) {
            return validation;
        }
        validation = StockPurchaseChecks.positiveQuantity(requestedQuantity, "buy");
        if (validation != null) {
            return validation;
        }

        CargoAPI playerCargo = StockPurchaseChecks.playerCargo(sector);
        validation = StockPurchaseChecks.playerCargoAvailable(playerCargo);
        if (validation != null) {
            return validation;
        }

        List<StockPurchaseSource> sources = StockPurchaseMarketSources.collectLocalSources(market, itemType, itemId, onlySubmarketId, includeBlackMarket);
        if (sources.isEmpty()) {
            return PurchaseResult.failure("No purchasable stock is available.");
        }
        Collections.sort(sources, StockPurchaseSource.PRICE_ORDER);

        StockPurchasePlan plan = StockPurchasePlan.build(sources, requestedQuantity);
        validation = StockPurchaseChecks.buyPlanAvailable(plan, "No purchasable stock is available.");
        if (validation != null) {
            return validation;
        }
        validation = StockPurchaseChecks.canCompletePurchase(playerCargo, plan);
        if (validation != null) {
            return validation;
        }

        return StockPurchaseExecutor.buyPlan(LOG, playerCargo, market, itemType, itemId, plan, "", "buy from local market");
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
