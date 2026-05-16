package weaponsprocurement.trade

import weaponsprocurement.stock.item.*
import weaponsprocurement.stock.market.*
import weaponsprocurement.stock.fixer.*

import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.CargoStackAPI
import com.fs.starfarer.api.campaign.SectorAPI
import com.fs.starfarer.api.campaign.SubmarketPlugin
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import java.util.ArrayList

class StockPurchaseMarketSources private constructor() {
    companion object {
        @JvmStatic
        fun collectLocalSources(
            market: MarketAPI,
            itemType: StockItemType,
            itemId: String,
            onlySubmarketId: String?,
            includeBlackMarket: Boolean,
        ): List<StockPurchaseSource> {
            val result = ArrayList<StockPurchaseSource>()
            val submarkets = market.submarketsCopy ?: return result
            for (submarket in submarkets) {
                if (submarket == null) continue
                if (onlySubmarketId != null && onlySubmarketId != submarket.specId) continue
                if (!MarketStockService.isTradeSubmarket(submarket, includeBlackMarket)) continue
                val cargo = submarket.cargoNullOk ?: continue
                val stacks = cargo.stacksCopy ?: continue
                for (stack: CargoStackAPI in stacks) {
                    if (!StockItemStacks.isPurchasableItemStack(submarket, stack, itemType)) continue
                    if (itemId != StockItemStacks.itemId(stack, itemType)) continue
                    val available = Math.round(stack.size)
                    if (available > 0) {
                        result.add(
                            StockPurchaseSource(
                                submarket,
                                cargo,
                                available,
                                StockItemStacks.unitPrice(submarket, stack),
                                StockItemStacks.unitCargoSpace(stack),
                            )
                        )
                    }
                }
            }
            return result
        }

        @JvmStatic
        fun collectSectorSources(
            sector: SectorAPI?,
            itemType: StockItemType,
            itemId: String,
            stockSources: List<SubmarketWeaponStock>?,
        ): List<StockPurchaseSource> {
            val result = ArrayList<StockPurchaseSource>()
            if (stockSources == null) return result
            for (stock in stockSources) {
                if (!stock.isPurchasable() || stock.count <= 0) continue
                val market = findMarket(sector, stock.marketId)
                val submarket = market?.getSubmarket(stock.submarketId)
                val cargo = submarket?.cargoNullOk
                val stack = StockItemCargo.itemStack(cargo, itemType, itemId)
                val liveAvailable = if (stack == null) 0 else Math.round(stack.size)
                val available = Math.min(stock.count, liveAvailable)
                if (available <= 0) continue
                result.add(
                    StockPurchaseSource(
                        market,
                        submarket,
                        cargo,
                        available,
                        stock.unitPrice,
                        StockItemStacks.unitCargoSpace(stack),
                    )
                )
            }
            return result
        }

        @JvmStatic
        fun sellTarget(
            market: MarketAPI?,
            playerStack: CargoStackAPI?,
            includeBlackMarket: Boolean,
        ): StockSellTarget? {
            val submarkets = market?.submarketsCopy
            if (submarkets == null || playerStack == null) return null

            var bestBlackMarket: StockSellTarget? = null
            var bestLegalMarket: StockSellTarget? = null
            for (submarket in submarkets) {
                if (submarket == null) continue
                if (!MarketStockService.isTradeSubmarket(submarket, includeBlackMarket)) continue
                val cargo: CargoAPI = submarket.cargoNullOk ?: continue
                val plugin = submarket.plugin
                if (plugin != null && plugin.isIllegalOnSubmarket(playerStack, SubmarketPlugin.TransferAction.PLAYER_SELL)) {
                    continue
                }
                val candidate = StockSellTarget(submarket, cargo, StockItemStacks.sellUnitPrice(submarket, playerStack))
                if (plugin != null && plugin.isBlackMarket) {
                    bestBlackMarket = betterSellTarget(bestBlackMarket, candidate)
                } else {
                    bestLegalMarket = betterSellTarget(bestLegalMarket, candidate)
                }
            }
            return if (includeBlackMarket && bestBlackMarket != null) bestBlackMarket else bestLegalMarket
        }

        private fun betterSellTarget(current: StockSellTarget?, candidate: StockSellTarget): StockSellTarget {
            if (current == null || candidate.unitPrice > current.unitPrice) return candidate
            return current
        }

        private fun findMarket(sector: SectorAPI?, marketId: String?): MarketAPI? {
            if (sector?.economy == null || marketId.isNullOrEmpty()) return null
            val markets = sector.economy.marketsCopy ?: return null
            for (market in markets) {
                if (market != null && marketId == market.id) return market
            }
            return null
        }
    }
}