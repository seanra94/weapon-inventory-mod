package weaponsprocurement.trade.execution

import weaponsprocurement.stock.item.StockItemCargo
import weaponsprocurement.stock.item.StockItemType
import weaponsprocurement.trade.plan.TradeMoney

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignUIAPI
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType
import com.fs.starfarer.api.campaign.PlayerMarketTransaction
import com.fs.starfarer.api.campaign.PlayerMarketTransaction.LineItemType
import com.fs.starfarer.api.campaign.SubmarketPlugin
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import org.apache.log4j.Logger

class StockMarketTransactionReporter private constructor() {
    companion object {
        @JvmStatic
        fun reportItemTransaction(
            log: Logger,
            market: MarketAPI?,
            submarket: SubmarketAPI?,
            itemType: StockItemType,
            itemId: String?,
            quantity: Int,
            unitPrice: Int,
            bought: Boolean,
        ) {
            if (market == null || submarket == null || itemId.isNullOrEmpty() || quantity <= 0) return
            val plugin = submarket.plugin ?: return
            val creditValue = TradeMoney.lineTotal(unitPrice, quantity)
            if (!TradeMoney.canExecuteCreditMutation(creditValue)) {
                log.warn(
                    "WP_STOCK_REVIEW skipped transaction report with oversized credit value item=" +
                        itemId + " quantity=" + quantity + " unitPrice=" + unitPrice
                )
                return
            }
            try {
                val transaction = PlayerMarketTransaction(market, submarket, tradeMode(submarket))
                val cargo = if (Global.getFactory() == null) null else Global.getFactory().createCargo(false)
                if (cargo != null) {
                    StockItemCargo.addItem(cargo, itemType, itemId, quantity)
                    cargo.sort()
                    if (bought) {
                        transaction.setBought(cargo)
                    } else {
                        transaction.setSold(cargo)
                    }
                }
                val lineType = if (bought) LineItemType.BOUGHT else LineItemType.SOLD
                transaction.lineItems.add(
                    PlayerMarketTransaction.TransactionLineItem(
                        itemId,
                        lineType,
                        if (StockItemType.WING == itemType) CargoItemType.FIGHTER_CHIP else CargoItemType.WEAPONS,
                        submarket,
                        quantity.toFloat(),
                        unitPrice.toFloat(),
                        unitPrice.toFloat(),
                        timestamp(),
                    )
                )
                transaction.creditValue = (if (bought) creditValue else -creditValue).toFloat()
                plugin.reportPlayerMarketTransaction(transaction)
            } catch (t: RuntimeException) {
                // Transaction callbacks are best-effort; cargo mutation has already succeeded.
                log.warn("WP_STOCK_REVIEW transaction report failed for $itemId at ${submarket.specId}", t)
            }
        }

        private fun tradeMode(submarket: SubmarketAPI?): CampaignUIAPI.CoreUITradeMode {
            val plugin = submarket?.plugin
            return if (plugin != null && plugin.isBlackMarket) {
                CampaignUIAPI.CoreUITradeMode.SNEAK
            } else {
                CampaignUIAPI.CoreUITradeMode.OPEN
            }
        }

        private fun timestamp(): Long {
            val clock = Global.getSector()?.clock
            return clock?.timestamp ?: 0L
        }
    }
}
