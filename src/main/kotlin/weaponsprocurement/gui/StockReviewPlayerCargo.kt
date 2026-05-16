package weaponsprocurement.gui

import com.fs.starfarer.api.campaign.CargoStackAPI
import com.fs.starfarer.api.campaign.SubmarketPlugin
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import weaponsprocurement.stock.MarketStockService
import weaponsprocurement.stock.StockItemStacks
import weaponsprocurement.stock.StockItemType
import java.util.HashMap

class StockReviewPlayerCargo private constructor() {
    companion object {
        @JvmStatic
        fun currentCredits(): Float {
            val cargo = WimGuiCampaignDialogHost.current().getPlayerCargo()
            return cargo?.credits?.get() ?: 0f
        }

        @JvmStatic
        fun currentCargoSpaceLeft(): Float {
            val cargo = WimGuiCampaignDialogHost.current().getPlayerCargo()
            return cargo?.spaceLeft ?: 0f
        }

        @JvmStatic
        fun currentCargoCapacity(): Float {
            val cargo = WimGuiCampaignDialogHost.current().getPlayerCargo()
            return cargo?.maxCapacity ?: 0f
        }

        @JvmStatic
        fun sellUnitPricesByItem(market: MarketAPI?, includeBlackMarket: Boolean): Map<String, Int> {
            val result = HashMap<String, Int>()
            val cargo = WimGuiCampaignDialogHost.current().getPlayerCargo()
            val stacks = cargo?.getStacksCopy()
            if (stacks == null) {
                return result
            }
            for (stack in stacks) {
                val itemType = if (StockItemStacks.isVisibleWingStack(stack)) StockItemType.WING else StockItemType.WEAPON
                if (!StockItemStacks.isVisibleItemStack(stack, itemType)) {
                    continue
                }
                val itemKey = itemType.key(StockItemStacks.itemId(stack, itemType))
                val unitPrice = localSellUnitPrice(market, stack, includeBlackMarket)
                if (unitPrice < 0) {
                    continue
                }
                val current = result[itemKey]
                if (current == null || unitPrice > current) {
                    result[itemKey] = unitPrice
                }
            }
            return result
        }

        private fun localSellUnitPrice(
            market: MarketAPI?,
            stack: CargoStackAPI?,
            includeBlackMarket: Boolean,
        ): Int {
            if (market == null || market.submarketsCopy == null || stack == null) {
                return -1
            }
            var bestBlackMarket = -1
            var bestLegalMarket = -1
            for (submarket in market.submarketsCopy) {
                if (submarket == null) {
                    continue
                }
                if (!MarketStockService.isTradeSubmarket(submarket, includeBlackMarket)) {
                    continue
                }
                val plugin = submarket.plugin
                if (plugin != null && plugin.isIllegalOnSubmarket(stack, SubmarketPlugin.TransferAction.PLAYER_SELL)) {
                    continue
                }
                val unitPrice = StockItemStacks.sellUnitPrice(submarket, stack)
                if (plugin != null && plugin.isBlackMarket) {
                    bestBlackMarket = Math.max(bestBlackMarket, unitPrice)
                } else {
                    bestLegalMarket = Math.max(bestLegalMarket, unitPrice)
                }
            }
            return if (includeBlackMarket && bestBlackMarket >= 0) bestBlackMarket else bestLegalMarket
        }
    }
}
