package weaponsprocurement.ui.stockreview

import weaponsprocurement.ui.*

import weaponsprocurement.stock.item.WeaponStockRecord
import weaponsprocurement.stock.item.WeaponStockSnapshot

class StockReviewTradeWarnings private constructor() {
    companion object {
        const val NONE = "None"
        const val NO_CARGO_CAPACITY = "Not enough cargo capacity"
        const val NOT_ENOUGH_CREDITS = "Not enough credits"
        const val LOW_CREDIT_BALANCE = "Credit balance at <5% of initial balance"
        const val LOW_CARGO_CAPACITY = "Cargo capacity at <5% of total capacity"

        @JvmStatic
        fun initialize(state: StockReviewState?) {
            if (state == null) {
                return
            }
            state.setInitialCreditsIfUnset(StockReviewPlayerCargo.currentCredits())
            state.setInitialCargoCapacityIfUnset(StockReviewPlayerCargo.currentCargoCapacity())
        }

        @JvmStatic
        fun clear(state: StockReviewState?) {
            state?.setTradeWarning(NONE)
        }

        @JvmStatic
        fun update(
            snapshot: WeaponStockSnapshot?,
            state: StockReviewState?,
            pendingTrades: List<StockReviewPendingTrade>?,
            explicitWarning: String?,
        ) {
            if (state == null) {
                return
            }
            if (!explicitWarning.isNullOrEmpty()) {
                state.setTradeWarning(explicitWarning)
                return
            }
            val tradeContext = StockReviewTradeContext(snapshot, pendingTrades)
            if (tradeContext.cargoSpaceLeft() <= 0.01f ||
                tradeContext.totalCargoSpaceDelta() > tradeContext.cargoSpaceLeft() + 0.01f
            ) {
                state.setTradeWarning(NO_CARGO_CAPACITY)
                return
            }
            val netCost = tradeContext.totalCost()
            if (netCost != StockReviewQuoteBook.PRICE_UNAVAILABLE.toLong() &&
                netCost > 0 &&
                remainingCreditsAfterTrade(tradeContext) < state.getInitialCredits() * 0.05f
            ) {
                state.setTradeWarning(LOW_CREDIT_BALANCE)
                return
            }
            val purchaseVolume = Math.max(0f, tradeContext.totalCargoSpaceDelta())
            if (purchaseVolume > 0f &&
                remainingCargoAfterTrade(tradeContext) < state.getInitialCargoCapacity() * 0.05f
            ) {
                state.setTradeWarning(LOW_CARGO_CAPACITY)
                return
            }
            state.setTradeWarning(NONE)
        }

        @JvmStatic
        fun purchaseAllLimitWarning(
            quoteBook: StockReviewQuoteBook,
            pendingTrades: List<StockReviewPendingTrade>?,
            record: WeaponStockRecord,
            tradeContext: StockReviewTradeContext,
            needed: Int,
            quantity: Int,
            currentWarning: String?,
        ): String? {
            val target = Math.min(Math.max(0, needed), tradeContext.buyableRemaining(record))
            if (target <= 0 || quantity >= target) {
                return currentWarning
            }
            val fullQuote = quoteBook.quotePortfolio(
                StockReviewTradePlanner.withAdjustment(
                    pendingTrades,
                    record.itemKey,
                    null,
                    target,
                ),
            )
            val fullCost = fullQuote.totalCost()
            if (fullCost != StockReviewQuoteBook.PRICE_UNAVAILABLE.toLong() && fullCost > tradeContext.credits()) {
                return NOT_ENOUGH_CREDITS
            }
            if (fullQuote.totalCargoSpaceDelta() > tradeContext.cargoSpaceLeft() + 0.01f) {
                return NO_CARGO_CAPACITY
            }
            return currentWarning
        }

        private fun remainingCreditsAfterTrade(tradeContext: StockReviewTradeContext): Float {
            val netCost = tradeContext.totalCost()
            return tradeContext.credits() - Math.max(0, netCost).toFloat()
        }

        private fun remainingCargoAfterTrade(tradeContext: StockReviewTradeContext): Float =
            tradeContext.cargoSpaceLeft() - Math.max(0f, tradeContext.totalCargoSpaceDelta())
    }
}