package weaponsprocurement.gui;

import weaponsprocurement.core.SubmarketWeaponStock;
import weaponsprocurement.core.WeaponStockRecord;
import weaponsprocurement.core.WeaponStockSnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class StockReviewTradeContext {
    private final List<StockReviewPendingTrade> pendingTrades;
    private final StockReviewQuoteBook quoteBook;
    private final Map<String, Integer> netByItem = new HashMap<String, Integer>();
    private final Map<String, Integer> buyByItem = new HashMap<String, Integer>();
    private final Map<String, Integer> sellByItem = new HashMap<String, Integer>();
    private final Map<String, Integer> affordableCache = new HashMap<String, Integer>();
    private final StockReviewPortfolioQuote portfolioQuote;
    private final int totalCost;
    private final float totalCargoSpaceDelta;
    private final float credits;
    private final float cargoSpaceLeft;

    StockReviewTradeContext(WeaponStockSnapshot snapshot, List<StockReviewPendingTrade> pendingTrades) {
        this.pendingTrades = pendingTrades;
        this.quoteBook = new StockReviewQuoteBook(snapshot);
        if (pendingTrades != null) {
            for (int i = 0; i < pendingTrades.size(); i++) {
                StockReviewPendingTrade trade = pendingTrades.get(i);
                add(netByItem, trade.getItemKey(), trade.getQuantity());
                if (trade.getQuantity() > 0) {
                    add(buyByItem, trade.getItemKey(), trade.getQuantity());
                } else if (trade.getQuantity() < 0) {
                    add(sellByItem, trade.getItemKey(), -trade.getQuantity());
                }
            }
        }
        portfolioQuote = quoteBook.quotePortfolio(pendingTrades);
        totalCost = portfolioQuote.totalCost();
        totalCargoSpaceDelta = portfolioQuote.totalCargoSpaceDelta();
        credits = StockReviewPlayerCargo.currentCredits();
        cargoSpaceLeft = StockReviewPlayerCargo.currentCargoSpaceLeft();
    }

    int netQuantityForItem(String itemKey) {
        return get(netByItem, itemKey);
    }

    int pendingBuyQuantityForItem(String itemKey) {
        return get(buyByItem, itemKey);
    }

    int pendingSellQuantityForItem(String itemKey) {
        return get(sellByItem, itemKey);
    }

    int buyableRemaining(WeaponStockRecord record) {
        return Math.max(0, record.getBuyableCount() - pendingBuyQuantityForItem(record.getItemKey()));
    }

    int sellableRemaining(WeaponStockRecord record) {
        return Math.max(0, record.getPlayerCargoCount() - pendingSellQuantityForItem(record.getItemKey()));
    }

    int positiveAdjustmentRemaining(WeaponStockRecord record, int requestedQuantity) {
        int requested = Math.max(0, requestedQuantity);
        if (requested <= 0) {
            return 0;
        }
        int sellCancellation = Math.min(requested, pendingSellQuantityForItem(record.getItemKey()));
        int remainingRequest = requested - sellCancellation;
        if (remainingRequest <= 0) {
            return sellCancellation;
        }
        return sellCancellation + affordableBuyQuantity(record, null, remainingRequest);
    }

    int negativeAdjustmentRemaining(WeaponStockRecord record, int requestedQuantity) {
        int requested = Math.max(0, requestedQuantity);
        if (requested <= 0) {
            return 0;
        }
        int buyCancellation = Math.min(requested, pendingBuyQuantityForItem(record.getItemKey()));
        int remainingRequest = requested - buyCancellation;
        if (remainingRequest <= 0) {
            return buyCancellation;
        }
        return buyCancellation + Math.min(remainingRequest, sellableRemaining(record));
    }

    int buyNeededForSufficiency(WeaponStockRecord record) {
        return Math.max(0, record.getDesiredCount() - (record.getOwnedCount() + netQuantityForItem(record.getItemKey())));
    }

    int sellableUntilSufficient(WeaponStockRecord record) {
        int stockAfterPlan = record.getOwnedCount() + netQuantityForItem(record.getItemKey());
        int excess = Math.max(0, stockAfterPlan - record.getDesiredCount());
        return Math.min(excess, sellableRemaining(record));
    }

    int transactionCostForItem(String itemKey) {
        return portfolioQuote.costForItem(itemKey);
    }

    int transactionCostForLine(String itemKey, String submarketId) {
        return portfolioQuote.costForLine(itemKey, submarketId);
    }

    int unitPriceForItem(WeaponStockRecord record) {
        if (record == null) {
            return StockReviewQuoteBook.PRICE_UNAVAILABLE;
        }
        int unitCost = quoteBook.nextBuyUnitPriceAfterPlannedBuys(record, pendingBuyQuantityForItem(record.getItemKey()));
        return unitCost == Integer.MAX_VALUE ? quoteBook.sellUnitPrice(record.getItemKey()) : unitCost;
    }

    int deltaToSufficient(WeaponStockRecord record) {
        int targetNet = record.getDesiredCount() - record.getOwnedCount();
        int delta = targetNet - netQuantityForItem(record.getItemKey());
        if (delta > 0) {
            return positiveAdjustmentRemaining(record, delta);
        }
        if (delta < 0) {
            return -negativeAdjustmentRemaining(record, -delta);
        }
        return 0;
    }

    List<StockReviewSellerAllocation> sellerAllocations(StockReviewPendingTrade trade) {
        if (trade == null) {
            return StockReviewQuote.ZERO.getSellerAllocations();
        }
        return portfolioQuote.sellerAllocations(trade.getItemKey(), trade.getSubmarketId());
    }

    int totalCost() {
        return totalCost;
    }

    float totalCargoSpaceDelta() {
        return totalCargoSpaceDelta;
    }

    int totalMarkupPaid() {
        return portfolioQuote.totalMarkupPaid();
    }

    float averageBuyMultiplier() {
        return portfolioQuote.averageBuyMultiplier();
    }

    float credits() {
        return credits;
    }

    float cargoSpaceLeft() {
        return cargoSpaceLeft;
    }

    boolean canConfirm() {
        return totalCost != StockReviewQuoteBook.PRICE_UNAVAILABLE
                && totalCost <= credits
                && totalCargoSpaceDelta <= cargoSpaceLeft + 0.01f;
    }

    int affordableBuyQuantity(WeaponStockRecord record, String submarketId, int requestedQuantity) {
        int maxByStock = Math.min(Math.max(0, requestedQuantity), buyableRemaining(record));
        if (submarketId != null) {
            maxByStock = Math.min(maxByStock, submarketRemaining(record, submarketId));
        }
        if (maxByStock <= 0) {
            return 0;
        }
        String key = record.getItemKey() + "|" + (submarketId == null ? "" : submarketId) + "|" + maxByStock;
        Integer cached = affordableCache.get(key);
        if (cached != null) {
            return cached.intValue();
        }
        int low = 0;
        int high = maxByStock;
        while (low < high) {
            int candidate = (low + high + 1) / 2;
            if (canAffordAdjustment(record, submarketId, candidate)) {
                low = candidate;
            } else {
                high = candidate - 1;
            }
        }
        affordableCache.put(key, Integer.valueOf(low));
        return low;
    }

    private boolean canAffordAdjustment(WeaponStockRecord record, String submarketId, int quantity) {
        StockReviewPortfolioQuote adjusted = quoteBook.quotePortfolio(StockReviewTradePlanner.withAdjustment(
                pendingTrades, record.getItemKey(), submarketId, quantity));
        int adjustedCost = adjusted.totalCost();
        if (adjustedCost == StockReviewQuoteBook.PRICE_UNAVAILABLE) {
            return false;
        }
        if (adjustedCost > credits) {
            return false;
        }
        return adjusted.totalCargoSpaceDelta() <= cargoSpaceLeft + 0.01f;
    }

    private int submarketRemaining(WeaponStockRecord record, String submarketId) {
        int sourceCount = 0;
        for (int i = 0; i < record.getSubmarketStocks().size(); i++) {
            SubmarketWeaponStock stock = record.getSubmarketStocks().get(i);
            if ((submarketId.equals(stock.getSourceId()) || submarketId.equals(stock.getSubmarketId())) && stock.isPurchasable()) {
                sourceCount += stock.getCount();
            }
        }
        int pendingFromSource = 0;
        if (pendingTrades != null) {
            for (int i = 0; i < pendingTrades.size(); i++) {
                StockReviewPendingTrade trade = pendingTrades.get(i);
                if (trade.matches(record.getItemKey(), submarketId) && trade.getQuantity() > 0) {
                    pendingFromSource += trade.getQuantity();
                }
            }
        }
        return Math.max(0, sourceCount - pendingFromSource);
    }

    private static void add(Map<String, Integer> counts, String itemKey, int quantity) {
        if (itemKey == null || quantity == 0) {
            return;
        }
        counts.put(itemKey, Integer.valueOf(get(counts, itemKey) + quantity));
    }

    private static int get(Map<String, Integer> counts, String itemKey) {
        Integer value = counts.get(itemKey);
        return value == null ? 0 : value.intValue();
    }

}
