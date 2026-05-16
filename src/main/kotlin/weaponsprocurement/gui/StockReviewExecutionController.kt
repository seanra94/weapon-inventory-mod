package weaponsprocurement.gui

import com.fs.starfarer.api.campaign.SectorAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import org.apache.log4j.Logger
import weaponsprocurement.trade.StockPurchaseService
import weaponsprocurement.stock.StockSourceMode
import weaponsprocurement.stock.SubmarketWeaponStock
import weaponsprocurement.trade.TradeMoney
import weaponsprocurement.stock.WeaponStockSnapshot
import java.util.Collections

class StockReviewExecutionController(
    private val state: StockReviewState,
    private val pendingTrades: StockReviewPendingTrades,
    private val purchaseService: StockPurchaseService,
    private val host: Host,
) {
    interface Host {
        fun snapshot(): WeaponStockSnapshot?
        fun sector(): SectorAPI?
        fun market(): MarketAPI?
        fun updateTradeWarning(explicitWarning: String?)
        fun rebuildSnapshot()
        fun requestContentRebuild()
        fun exitReviewMode()
        fun requestReopen(review: Boolean)
        fun reopen(review: Boolean)
        fun requestClose()
        fun refreshVanillaCargoScreen()
        fun postMessage(message: String?)
    }

    fun confirmPendingTrades() {
        if (pendingTrades.isEmpty()) {
            host.reopen(false)
            return
        }
        val snapshot = host.snapshot()
        val tradeContext = StockReviewTradeContext(snapshot, pendingTrades.asList())
        val estimatedCost = tradeContext.totalCost()
        if (estimatedCost == StockReviewQuoteBook.PRICE_UNAVAILABLE.toLong()) {
            host.postMessage("Could not price every queued item. Adjust the plan and try again.")
            host.requestContentRebuild()
            return
        }
        if (estimatedCost > TradeMoney.MAX_EXECUTABLE_CREDITS) {
            host.postMessage("Order value is too large.")
            host.requestContentRebuild()
            return
        }
        val credits = tradeContext.credits()
        if (estimatedCost > 0 && credits + 0.01f < estimatedCost) {
            host.postMessage("Need ${StockReviewFormat.credits(estimatedCost)} for these trades.")
            host.updateTradeWarning(StockReviewTradeWarnings.NOT_ENOUGH_CREDITS)
            host.requestContentRebuild()
            return
        }
        val cargoDelta = tradeContext.totalCargoSpaceDelta()
        if (cargoDelta > tradeContext.cargoSpaceLeft() + 0.01f) {
            host.postMessage("Need ${Math.round(cargoDelta)} cargo space for these trades.")
            host.updateTradeWarning(StockReviewTradeWarnings.NO_CARGO_CAPACITY)
            host.requestContentRebuild()
            return
        }

        val sector = host.sector()
        val market = host.market()
        val executionOrder = StockReviewTradePlanner.executionOrder(pendingTrades.asList())
        val sourceMode = snapshot?.getSourceMode() ?: StockSourceMode.LOCAL
        for (i in executionOrder.indices) {
            val trade = executionOrder[i]
            val result = executePendingTradeSafely(sector, market, trade, sourceMode)
            if (result == null || !result.isSuccess()) {
                if (result != null) {
                    reportPurchaseFailure(result)
                }
                pendingTrades.removeExecuted(executionOrder, i)
                host.rebuildSnapshot()
                host.requestContentRebuild()
                return
            }
        }
        pendingTrades.clear()
        host.updateTradeWarning(null)
        host.exitReviewMode()
        host.rebuildSnapshot()
        if (StockReviewStyle.REFRESH_VANILLA_CORE_AFTER_PURCHASE) {
            host.requestReopen(false)
            host.refreshVanillaCargoScreen()
            host.requestClose()
            return
        }
        host.reopen(false)
    }

    private fun executePendingTradeSafely(
        sector: SectorAPI?,
        market: MarketAPI?,
        trade: StockReviewPendingTrade?,
        sourceMode: StockSourceMode?,
    ): StockPurchaseService.PurchaseResult? {
        return try {
            executePendingTrade(sector, market, trade, sourceMode)
        } catch (t: Throwable) {
            LOG.error(
                "WP_STOCK_REVIEW queued trade execution crashed item=" +
                    (trade?.itemKey ?: "null") +
                    " source=" + (trade?.submarketId ?: "null") +
                    " quantity=" + (trade?.quantity ?: 0) +
                    " sourceMode=" + sourceMode,
                t,
            )
            StockPurchaseService.PurchaseResult.failure("Trade failed during execution. Check starsector.log.")
        }
    }

    private fun executePendingTrade(
        sector: SectorAPI?,
        market: MarketAPI?,
        trade: StockReviewPendingTrade?,
        sourceMode: StockSourceMode?,
    ): StockPurchaseService.PurchaseResult {
        if (trade == null) {
            return StockPurchaseService.PurchaseResult.failure("No queued trade is available.")
        }
        val snapshot = host.snapshot()
        val record = snapshot?.getRecord(trade.itemKey)
        if (record == null) {
            return StockPurchaseService.PurchaseResult.failure("No queued item record is available.")
        }
        if (trade.isSell()) {
            if (sourceMode != null && sourceMode.isRemote()) {
                return purchaseService.sellItemToMarket(sector, market, record.itemType, record.itemId, -trade.quantity, false)
            }
            return purchaseService.sellItemToMarket(sector, market, record.itemType, record.itemId, -trade.quantity, state.isIncludeBlackMarket())
        }
        if (StockSourceMode.FIXERS == sourceMode) {
            return purchaseService.buyItemFromFixersMarket(
                sector,
                record.itemType,
                record.itemId,
                trade.quantity,
                virtualUnitPrice(trade.itemKey),
                virtualUnitCargoSpace(trade.itemKey),
            )
        }
        if (StockSourceMode.SECTOR == sourceMode) {
            return purchaseService.buyItemFromSectorSources(
                sector,
                record.itemType,
                record.itemId,
                trade.quantity,
                stockSources(trade.itemKey, trade.submarketId),
            )
        }
        return purchaseService.buyCheapestItem(
            sector,
            market,
            record.itemType,
            record.itemId,
            trade.quantity,
            state.isIncludeBlackMarket(),
        )
    }

    private fun reportPurchaseFailure(result: StockPurchaseService.PurchaseResult) {
        host.postMessage(result.message)
        LOG.info("WP_STOCK_REVIEW trade blocked: ${result.message}")
    }

    private fun stockSources(itemKey: String?, sourceId: String?): List<SubmarketWeaponStock> {
        val snapshot = host.snapshot()
        val record = snapshot?.getRecord(itemKey)
        if (record == null) {
            return Collections.emptyList()
        }
        if (sourceId.isNullOrEmpty()) {
            return record.submarketStocks
        }
        val result = ArrayList<SubmarketWeaponStock>()
        for (stock in record.submarketStocks) {
            if (sourceId == stock.sourceId || sourceId == stock.submarketId) {
                result.add(stock)
            }
        }
        return result
    }

    private fun virtualUnitPrice(itemKey: String?): Int {
        val snapshot = host.snapshot()
        val record = snapshot?.getRecord(itemKey)
        return record?.cheapestPurchasableUnitPrice ?: 0
    }

    private fun virtualUnitCargoSpace(itemKey: String?): Float {
        val snapshot = host.snapshot()
        val record = snapshot?.getRecord(itemKey)
        if (record == null || record.submarketStocks.isEmpty()) {
            return 1f
        }
        return maxOf(1f, record.submarketStocks[0].unitCargoSpace)
    }

    companion object {
        private val LOG: Logger = Logger.getLogger(StockReviewExecutionController::class.java)
    }
}
