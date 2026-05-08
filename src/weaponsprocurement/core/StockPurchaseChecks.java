package weaponsprocurement.core;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

final class StockPurchaseChecks {
    private StockPurchaseChecks() {
    }

    static StockPurchaseService.PurchaseResult marketContext(SectorAPI sector, MarketAPI market) {
        return sector == null || market == null
                ? StockPurchaseService.PurchaseResult.failure("No active market context.")
                : null;
    }

    static StockPurchaseService.PurchaseResult sectorContext(SectorAPI sector) {
        return sector == null
                ? StockPurchaseService.PurchaseResult.failure("No active sector context.")
                : null;
    }

    static StockPurchaseService.PurchaseResult selectedItem(StockItemType itemType, String itemId) {
        return itemId == null || itemId.isEmpty()
                ? StockPurchaseService.PurchaseResult.failure("No " + itemType.getSingularLabel().toLowerCase(java.util.Locale.US) + " selected.")
                : null;
    }

    static StockPurchaseService.PurchaseResult positiveQuantity(int quantity, String action) {
        return quantity <= 0
                ? StockPurchaseService.PurchaseResult.failure("Nothing to " + action + ".")
                : null;
    }

    static CargoAPI playerCargo(SectorAPI sector) {
        CampaignFleetAPI fleet = sector == null ? null : sector.getPlayerFleet();
        return fleet == null ? null : fleet.getCargo();
    }

    static StockPurchaseService.PurchaseResult playerCargoAvailable(CargoAPI playerCargo) {
        return playerCargo == null
                ? StockPurchaseService.PurchaseResult.failure("Player cargo is unavailable.")
                : null;
    }

    static StockPurchaseService.PurchaseResult canAfford(CargoAPI playerCargo, int totalCost) {
        return playerCargo.getCredits().get() + 0.01f < totalCost
                ? StockPurchaseService.PurchaseResult.failure("Need " + CreditFormat.creditsLong(totalCost) + " for this order.")
                : null;
    }

    static StockPurchaseService.PurchaseResult hasCargoSpace(CargoAPI playerCargo, float totalSpace) {
        return playerCargo.getSpaceLeft() + 0.01f < totalSpace
                ? StockPurchaseService.PurchaseResult.failure("Need " + Math.round(totalSpace) + " cargo space for this order.")
                : null;
    }

    static StockPurchaseService.PurchaseResult canCompletePurchase(CargoAPI playerCargo, int totalCost, float totalSpace) {
        StockPurchaseService.PurchaseResult validation = canAfford(playerCargo, totalCost);
        return validation != null ? validation : hasCargoSpace(playerCargo, totalSpace);
    }

    static StockPurchaseService.PurchaseResult canCompletePurchase(CargoAPI playerCargo, StockPurchasePlan plan) {
        return canCompletePurchase(playerCargo, plan.totalCost, plan.totalSpace);
    }

    static StockPurchaseService.PurchaseResult buyPlanAvailable(StockPurchasePlan plan, String message) {
        return plan.totalQuantity <= 0
                ? StockPurchaseService.PurchaseResult.failure(message)
                : null;
    }

    static void addCampaignMessage(String message) {
        if (Global.getSector() != null && Global.getSector().getCampaignUI() != null) {
            Global.getSector().getCampaignUI().addMessage(message);
        }
    }
}
