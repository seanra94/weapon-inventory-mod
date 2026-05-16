package weaponsprocurement.trade

import weaponsprocurement.stock.*

import com.fs.starfarer.api.campaign.SectorAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import org.apache.log4j.Logger
import java.util.Collections

class StockPurchaseService {
    fun buyCheapestItem(
        sector: SectorAPI?,
        market: MarketAPI?,
        itemType: StockItemType,
        itemId: String?,
        requestedQuantity: Int,
        includeBlackMarket: Boolean,
    ): PurchaseResult {
        return buyItem(sector, market, itemType, itemId, null, requestedQuantity, includeBlackMarket)
    }

    fun sellItemToMarket(
        sector: SectorAPI?,
        market: MarketAPI?,
        itemType: StockItemType,
        itemId: String?,
        requestedQuantity: Int,
        includeBlackMarket: Boolean,
    ): PurchaseResult {
        var validation = StockPurchaseChecks.marketContext(sector, market)
        if (validation != null) return validation
        validation = StockPurchaseChecks.selectedItem(itemType, itemId)
        if (validation != null) return validation
        validation = StockPurchaseChecks.positiveQuantity(requestedQuantity, "sell")
        if (validation != null) return validation

        val playerCargo = StockPurchaseChecks.playerCargo(sector)
        validation = StockPurchaseChecks.playerCargoAvailable(playerCargo)
        if (validation != null) return validation

        val cargo = playerCargo!!
        val checkedItemId = itemId!!
        val playerStack = StockItemCargo.itemStack(cargo, itemType, checkedItemId)
        val available = StockItemCargo.itemCount(cargo, itemType, checkedItemId)
        if (playerStack == null || available <= 0) {
            return PurchaseResult.failure("No player-cargo stock is available to sell.")
        }
        val quantity = Math.min(requestedQuantity, available)
        val target = StockPurchaseMarketSources.sellTarget(market, playerStack, includeBlackMarket)
            ?: return PurchaseResult.failure("No valid market buyer is available.")

        return StockPurchaseExecutor.sellToMarket(LOG, market, cargo, target, itemType, checkedItemId, quantity)
    }

    fun buyItemFromFixersMarket(
        sector: SectorAPI?,
        itemType: StockItemType,
        itemId: String?,
        requestedQuantity: Int,
        unitPrice: Int,
        unitCargoSpace: Float,
    ): PurchaseResult {
        var validation = StockPurchaseChecks.sectorContext(sector)
        if (validation != null) return validation
        validation = StockPurchaseChecks.selectedItem(itemType, itemId)
        if (validation != null) return validation
        validation = StockPurchaseChecks.positiveQuantity(requestedQuantity, "buy")
        if (validation != null) return validation
        if (unitPrice < 0) return PurchaseResult.failure("No fixer-market price is available.")

        val playerCargo = StockPurchaseChecks.playerCargo(sector)
        validation = StockPurchaseChecks.playerCargoAvailable(playerCargo)
        if (validation != null) return validation

        val totalCost = TradeMoney.lineTotal(unitPrice, requestedQuantity)
        val totalSpace = Math.max(1f, unitCargoSpace) * requestedQuantity
        validation = StockPurchaseChecks.canCompletePurchase(playerCargo!!, totalCost, totalSpace)
        if (validation != null) return validation

        return StockPurchaseExecutor.buyFromFixersMarket(LOG, playerCargo, itemType, itemId!!, requestedQuantity, totalCost)
    }

    fun buyItemFromSectorSources(
        sector: SectorAPI?,
        itemType: StockItemType,
        itemId: String?,
        requestedQuantity: Int,
        stockSources: List<SubmarketWeaponStock>?,
    ): PurchaseResult {
        var validation = StockPurchaseChecks.sectorContext(sector)
        if (validation != null) return validation
        validation = StockPurchaseChecks.selectedItem(itemType, itemId)
        if (validation != null) return validation
        validation = StockPurchaseChecks.positiveQuantity(requestedQuantity, "buy")
        if (validation != null) return validation
        if (stockSources == null || stockSources.isEmpty()) return PurchaseResult.failure("No sector-market stock is available.")

        val playerCargo = StockPurchaseChecks.playerCargo(sector)
        validation = StockPurchaseChecks.playerCargoAvailable(playerCargo)
        if (validation != null) return validation

        val checkedItemId = itemId!!
        val sources = StockPurchaseMarketSources.collectSectorSources(sector, itemType, checkedItemId, stockSources)
        if (sources.isEmpty()) return PurchaseResult.failure("No sector-market stock is available.")
        Collections.sort(sources, StockPurchaseSource.PRICE_ORDER)

        val plan = StockPurchasePlan.build(sources, requestedQuantity)
        validation = StockPurchaseChecks.buyPlanAvailable(plan, "No sector-market stock is available.")
        if (validation != null) return validation
        validation = StockPurchaseChecks.canCompletePurchase(playerCargo!!, plan)
        if (validation != null) return validation

        return StockPurchaseExecutor.buyPlan(
            LOG,
            playerCargo,
            null,
            itemType,
            checkedItemId,
            plan,
            " from the sector market",
            "buy from sector market",
        )
    }

    private fun buyItem(
        sector: SectorAPI?,
        market: MarketAPI?,
        itemType: StockItemType,
        itemId: String?,
        onlySubmarketId: String?,
        requestedQuantity: Int,
        includeBlackMarket: Boolean,
    ): PurchaseResult {
        var validation = StockPurchaseChecks.marketContext(sector, market)
        if (validation != null) return validation
        validation = StockPurchaseChecks.selectedItem(itemType, itemId)
        if (validation != null) return validation
        validation = StockPurchaseChecks.positiveQuantity(requestedQuantity, "buy")
        if (validation != null) return validation

        val playerCargo = StockPurchaseChecks.playerCargo(sector)
        validation = StockPurchaseChecks.playerCargoAvailable(playerCargo)
        if (validation != null) return validation

        val checkedItemId = itemId!!
        val sources = StockPurchaseMarketSources.collectLocalSources(
            market!!,
            itemType,
            checkedItemId,
            onlySubmarketId,
            includeBlackMarket,
        )
        if (sources.isEmpty()) return PurchaseResult.failure("No purchasable stock is available.")
        Collections.sort(sources, StockPurchaseSource.PRICE_ORDER)

        val plan = StockPurchasePlan.build(sources, requestedQuantity)
        validation = StockPurchaseChecks.buyPlanAvailable(plan, "No purchasable stock is available.")
        if (validation != null) return validation
        validation = StockPurchaseChecks.canCompletePurchase(playerCargo!!, plan)
        if (validation != null) return validation

        return StockPurchaseExecutor.buyPlan(LOG, playerCargo, market, itemType, checkedItemId, plan, "", "buy from local market")
    }

    class PurchaseResult private constructor(
        private val successValue: Boolean,
        val message: String?,
        val quantity: Int,
        val credits: Long,
    ) {
        fun isSuccess(): Boolean = successValue

        companion object {
            @JvmStatic
            fun success(message: String?, quantity: Int, credits: Long): PurchaseResult {
                return PurchaseResult(true, message, quantity, credits)
            }

            @JvmStatic
            fun failure(message: String?): PurchaseResult {
                return PurchaseResult(false, message, 0, 0L)
            }
        }
    }

    companion object {
        private val LOG: Logger = Logger.getLogger(StockPurchaseService::class.java)
    }
}
