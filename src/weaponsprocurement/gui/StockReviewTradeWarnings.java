package weaponsprocurement.gui;

import weaponsprocurement.core.WeaponStockRecord;
import weaponsprocurement.core.WeaponStockSnapshot;

import java.util.List;

final class StockReviewTradeWarnings {
    static final String NONE = "None";
    static final String NO_CARGO_CAPACITY = "Not enough cargo capacity";
    static final String NOT_ENOUGH_CREDITS = "Not enough credits";
    static final String LOW_CREDIT_BALANCE = "Credit balance at <5% of initial balance";
    static final String LOW_CARGO_CAPACITY = "Cargo capacity at <5% of total capacity";

    private StockReviewTradeWarnings() {
    }

    static void initialize(StockReviewState state) {
        if (state == null) {
            return;
        }
        state.setInitialCreditsIfUnset(StockReviewPlayerCargo.currentCredits());
        state.setInitialCargoCapacityIfUnset(StockReviewPlayerCargo.currentCargoCapacity());
    }

    static void clear(StockReviewState state) {
        if (state != null) {
            state.setTradeWarning(NONE);
        }
    }

    static void update(WeaponStockSnapshot snapshot,
                       StockReviewState state,
                       List<StockReviewPendingPurchase> pendingPurchases,
                       String explicitWarning) {
        if (state == null) {
            return;
        }
        if (explicitWarning != null && !explicitWarning.isEmpty()) {
            state.setTradeWarning(explicitWarning);
            return;
        }
        StockReviewTradeContext tradeContext = new StockReviewTradeContext(snapshot, pendingPurchases);
        if (tradeContext.cargoSpaceLeft() <= 0.01f
                || tradeContext.totalCargoSpaceDelta() > tradeContext.cargoSpaceLeft() + 0.01f) {
            state.setTradeWarning(NO_CARGO_CAPACITY);
            return;
        }
        int netCost = tradeContext.totalCost();
        if (netCost != StockReviewQuoteBook.PRICE_UNAVAILABLE
                && netCost > 0
                && remainingCreditsAfterTrade(tradeContext) < state.getInitialCredits() * 0.05f) {
            state.setTradeWarning(LOW_CREDIT_BALANCE);
            return;
        }
        float purchaseVolume = Math.max(0f, tradeContext.totalCargoSpaceDelta());
        if (purchaseVolume > 0f
                && remainingCargoAfterTrade(tradeContext) < state.getInitialCargoCapacity() * 0.05f) {
            state.setTradeWarning(LOW_CARGO_CAPACITY);
            return;
        }
        state.setTradeWarning(NONE);
    }

    static String purchaseAllLimitWarning(StockReviewQuoteBook quoteBook,
                                          List<StockReviewPendingPurchase> pendingPurchases,
                                          WeaponStockRecord record,
                                          StockReviewTradeContext tradeContext,
                                          int needed,
                                          int quantity,
                                          String currentWarning) {
        int target = Math.min(Math.max(0, needed), tradeContext.buyableRemaining(record));
        if (target <= 0 || quantity >= target) {
            return currentWarning;
        }
        StockReviewPortfolioQuote fullQuote = quoteBook.quotePortfolio(StockReviewTradePlanner.withAdjustment(
                pendingPurchases,
                record.getWeaponId(),
                null,
                target));
        int fullCost = fullQuote.totalCost();
        if (fullCost != StockReviewQuoteBook.PRICE_UNAVAILABLE && fullCost > tradeContext.credits()) {
            return NOT_ENOUGH_CREDITS;
        }
        if (fullQuote.totalCargoSpaceDelta() > tradeContext.cargoSpaceLeft() + 0.01f) {
            return NO_CARGO_CAPACITY;
        }
        return currentWarning;
    }

    private static float remainingCreditsAfterTrade(StockReviewTradeContext tradeContext) {
        int netCost = tradeContext.totalCost();
        return tradeContext.credits() - Math.max(0, netCost);
    }

    private static float remainingCargoAfterTrade(StockReviewTradeContext tradeContext) {
        return tradeContext.cargoSpaceLeft() - Math.max(0f, tradeContext.totalCargoSpaceDelta());
    }
}
