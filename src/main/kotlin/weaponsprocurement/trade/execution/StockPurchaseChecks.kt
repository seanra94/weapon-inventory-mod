package weaponsprocurement.trade.execution

import weaponsprocurement.stock.item.*
import weaponsprocurement.stock.market.*
import weaponsprocurement.stock.fixer.*
import weaponsprocurement.trade.plan.StockPurchasePlan
import weaponsprocurement.trade.plan.TradeMoney
import weaponsprocurement.trade.quote.CreditFormat

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.SectorAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import java.util.Locale

class StockPurchaseChecks private constructor() {
    companion object {
        @JvmStatic
        fun marketContext(sector: SectorAPI?, market: MarketAPI?): StockPurchaseService.PurchaseResult? {
            return if (sector == null || market == null) {
                StockPurchaseService.PurchaseResult.failure("No active market context.")
            } else {
                null
            }
        }

        @JvmStatic
        fun sectorContext(sector: SectorAPI?): StockPurchaseService.PurchaseResult? {
            return if (sector == null) StockPurchaseService.PurchaseResult.failure("No active sector context.") else null
        }

        @JvmStatic
        fun selectedItem(itemType: StockItemType, itemId: String?): StockPurchaseService.PurchaseResult? {
            return if (itemId.isNullOrEmpty()) {
                StockPurchaseService.PurchaseResult.failure(
                    "No " + itemType.singularLabel.lowercase(Locale.US) + " selected."
                )
            } else {
                null
            }
        }

        @JvmStatic
        fun positiveQuantity(quantity: Int, action: String): StockPurchaseService.PurchaseResult? {
            return if (quantity <= 0) StockPurchaseService.PurchaseResult.failure("Nothing to $action.") else null
        }

        @JvmStatic
        fun playerCargo(sector: SectorAPI?): CargoAPI? {
            val fleet = sector?.playerFleet
            return fleet?.cargo
        }

        @JvmStatic
        fun playerCargoAvailable(playerCargo: CargoAPI?): StockPurchaseService.PurchaseResult? {
            return if (playerCargo == null) {
                StockPurchaseService.PurchaseResult.failure("Player cargo is unavailable.")
            } else {
                null
            }
        }

        @JvmStatic
        fun canAfford(playerCargo: CargoAPI, totalCost: Long): StockPurchaseService.PurchaseResult? {
            return if (playerCargo.credits.get() + 0.01f < totalCost) {
                StockPurchaseService.PurchaseResult.failure(
                    "Need " + CreditFormat.creditsLong(totalCost) + " for this order."
                )
            } else {
                null
            }
        }

        @JvmStatic
        fun canMutateCredits(credits: Long): StockPurchaseService.PurchaseResult? {
            return if (TradeMoney.canExecuteCreditMutation(credits)) {
                null
            } else {
                StockPurchaseService.PurchaseResult.failure("Order value is too large.")
            }
        }

        @JvmStatic
        fun hasCargoSpace(playerCargo: CargoAPI, totalSpace: Float): StockPurchaseService.PurchaseResult? {
            return if (playerCargo.spaceLeft + 0.01f < totalSpace) {
                StockPurchaseService.PurchaseResult.failure(
                    "Need " + Math.round(totalSpace) + " cargo space for this order."
                )
            } else {
                null
            }
        }

        @JvmStatic
        fun canCompletePurchase(
            playerCargo: CargoAPI,
            totalCost: Long,
            totalSpace: Float,
        ): StockPurchaseService.PurchaseResult? {
            var validation = canMutateCredits(totalCost)
            if (validation != null) return validation
            validation = canAfford(playerCargo, totalCost)
            return validation ?: hasCargoSpace(playerCargo, totalSpace)
        }

        @JvmStatic
        fun canCompletePurchase(playerCargo: CargoAPI, plan: StockPurchasePlan): StockPurchaseService.PurchaseResult? {
            return canCompletePurchase(playerCargo, plan.totalCost, plan.totalSpace)
        }

        @JvmStatic
        fun buyPlanAvailable(plan: StockPurchasePlan, message: String): StockPurchaseService.PurchaseResult? {
            return if (plan.totalQuantity <= 0) StockPurchaseService.PurchaseResult.failure(message) else null
        }

        @JvmStatic
        fun addCampaignMessage(message: String?) {
            val ui = Global.getSector()?.campaignUI
            if (ui != null) ui.addMessage(message)
        }
    }
}
