package weaponsprocurement.core;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import org.apache.log4j.Logger;

final class StockPurchaseExecutor {
    private StockPurchaseExecutor() {
    }

    static StockPurchaseService.PurchaseResult sellToMarket(Logger log,
                                                            MarketAPI market,
                                                            CargoAPI playerCargo,
                                                            StockSellTarget target,
                                                            StockItemType itemType,
                                                            String itemId,
                                                            int quantity) {
        int credits = target.unitPrice * quantity;
        int expectedMarketCount = StockItemCargo.itemCount(target.cargo, itemType, itemId) + quantity;
        try {
            StockItemCargo.removeItem(playerCargo, itemType, itemId, quantity);
            playerCargo.getCredits().add(credits);
            StockItemCargo.tidyCargo(playerCargo);
            StockItemCargo.addItem(target.cargo, itemType, itemId, quantity);
            StockItemCargo.tidyCargo(target.cargo);
            StockMarketTransactionReporter.reportItemTransaction(log, market, target.submarket, itemType, itemId, quantity, target.unitPrice, false);
            StockItemCargo.reconcileItemCount(target.cargo, itemType, itemId, expectedMarketCount);

            String message = "Sold " + quantity + " " + StockItemCargo.itemDisplayName(itemType, itemId)
                    + " for " + CreditFormat.creditsLong(credits) + ".";
            StockPurchaseChecks.addCampaignMessage(message);
            return StockPurchaseService.PurchaseResult.success(message, quantity, -credits);
        } catch (Throwable t) {
            return executionFailure(log, "sell to market", itemType, itemId, quantity, t);
        }
    }

    static StockPurchaseService.PurchaseResult buyFromFixersMarket(Logger log,
                                                                   CargoAPI playerCargo,
                                                                   StockItemType itemType,
                                                                   String itemId,
                                                                   int quantity,
                                                                   int totalCost) {
        try {
            StockItemCargo.addItem(playerCargo, itemType, itemId, quantity);
            playerCargo.getCredits().subtract(totalCost);
            StockItemCargo.tidyCargo(playerCargo);

            String message = "Bought " + quantity + " " + StockItemCargo.itemDisplayName(itemType, itemId)
                    + " from the fixer's market for " + CreditFormat.creditsLong(totalCost) + ".";
            StockPurchaseChecks.addCampaignMessage(message);
            return StockPurchaseService.PurchaseResult.success(message, quantity, totalCost);
        } catch (Throwable t) {
            return executionFailure(log, "buy from fixer's market", itemType, itemId, quantity, t);
        }
    }

    static StockPurchaseService.PurchaseResult buyPlan(Logger log,
                                                       CargoAPI playerCargo,
                                                       MarketAPI fallbackMarket,
                                                       StockItemType itemType,
                                                       String itemId,
                                                       StockPurchasePlan plan,
                                                       String sourceLabel,
                                                       String operation) {
        try {
            for (StockPurchaseLine line : plan.lines) {
                StockItemCargo.removeItem(line.source.cargo, itemType, itemId, line.quantity);
                StockItemCargo.tidyCargo(line.source.cargo);
                MarketAPI reportMarket = line.source.market == null ? fallbackMarket : line.source.market;
                StockMarketTransactionReporter.reportItemTransaction(log, reportMarket, line.source.submarket, itemType, itemId, line.quantity, line.source.unitPrice, true);
            }
            StockItemCargo.addItem(playerCargo, itemType, itemId, plan.totalQuantity);
            playerCargo.getCredits().subtract(plan.totalCost);
            StockItemCargo.tidyCargo(playerCargo);

            String message = "Bought " + plan.totalQuantity + " " + StockItemCargo.itemDisplayName(itemType, itemId)
                    + sourceLabel + " for " + CreditFormat.creditsLong(plan.totalCost) + ".";
            StockPurchaseChecks.addCampaignMessage(message);
            return StockPurchaseService.PurchaseResult.success(message, plan.totalQuantity, plan.totalCost);
        } catch (Throwable t) {
            return executionFailure(log, operation, itemType, itemId, plan.totalQuantity, t);
        }
    }

    private static StockPurchaseService.PurchaseResult executionFailure(Logger log,
                                                                        String operation,
                                                                        StockItemType itemType,
                                                                        String itemId,
                                                                        int quantity,
                                                                        Throwable t) {
        log.error("WP_STOCK_REVIEW trade execution failed operation=" + operation
                + " item=" + itemType.key(itemId)
                + " quantity=" + quantity, t);
        return StockPurchaseService.PurchaseResult.failure("Trade failed during execution. Check starsector.log.");
    }
}
