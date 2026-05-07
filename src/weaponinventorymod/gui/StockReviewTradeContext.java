package weaponinventorymod.gui;

import weaponinventorymod.core.SubmarketWeaponStock;
import weaponinventorymod.core.WeaponStockRecord;
import weaponinventorymod.core.WeaponStockSnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class StockReviewTradeContext {
    private final List<StockReviewPendingPurchase> pendingPurchases;
    private final StockReviewQuoteBook quoteBook;
    private final Map<String, Integer> netByWeapon = new HashMap<String, Integer>();
    private final Map<String, Integer> buyByWeapon = new HashMap<String, Integer>();
    private final Map<String, Integer> sellByWeapon = new HashMap<String, Integer>();
    private final Map<String, Integer> affordableCache = new HashMap<String, Integer>();
    private final StockReviewPortfolioQuote portfolioQuote;
    private final int totalCost;
    private final float totalCargoSpaceDelta;
    private final float credits;
    private final float cargoSpaceLeft;

    StockReviewTradeContext(WeaponStockSnapshot snapshot, List<StockReviewPendingPurchase> pendingPurchases) {
        this.pendingPurchases = pendingPurchases;
        this.quoteBook = new StockReviewQuoteBook(snapshot);
        if (pendingPurchases != null) {
            for (int i = 0; i < pendingPurchases.size(); i++) {
                StockReviewPendingPurchase purchase = pendingPurchases.get(i);
                add(netByWeapon, purchase.getWeaponId(), purchase.getQuantity());
                if (purchase.getQuantity() > 0) {
                    add(buyByWeapon, purchase.getWeaponId(), purchase.getQuantity());
                } else if (purchase.getQuantity() < 0) {
                    add(sellByWeapon, purchase.getWeaponId(), -purchase.getQuantity());
                }
            }
        }
        portfolioQuote = quoteBook.quotePortfolio(pendingPurchases);
        totalCost = portfolioQuote.totalCost();
        totalCargoSpaceDelta = portfolioQuote.totalCargoSpaceDelta();
        credits = StockReviewPlayerCargo.currentCredits();
        cargoSpaceLeft = StockReviewPlayerCargo.currentCargoSpaceLeft();
    }

    int netQuantityForWeapon(String weaponId) {
        return get(netByWeapon, weaponId);
    }

    int pendingBuyQuantityForWeapon(String weaponId) {
        return get(buyByWeapon, weaponId);
    }

    int pendingSellQuantityForWeapon(String weaponId) {
        return get(sellByWeapon, weaponId);
    }

    int buyableRemaining(WeaponStockRecord record) {
        return Math.max(0, record.getBuyableCount() - pendingBuyQuantityForWeapon(record.getWeaponId()));
    }

    int sellableRemaining(WeaponStockRecord record) {
        return Math.max(0, record.getPlayerCargoCount() - pendingSellQuantityForWeapon(record.getWeaponId()));
    }

    int buyNeededForSufficiency(WeaponStockRecord record) {
        return Math.max(0, record.getDesiredCount() - (record.getOwnedCount() + netQuantityForWeapon(record.getWeaponId())));
    }

    int sellableUntilSufficient(WeaponStockRecord record) {
        int stockAfterPlan = record.getOwnedCount() + netQuantityForWeapon(record.getWeaponId());
        int excess = Math.max(0, stockAfterPlan - record.getDesiredCount());
        return Math.min(excess, sellableRemaining(record));
    }

    int transactionCostForWeapon(String weaponId) {
        return portfolioQuote.costForWeapon(weaponId);
    }

    int transactionCostForLine(String weaponId, String submarketId) {
        return portfolioQuote.costForLine(weaponId, submarketId);
    }

    List<StockReviewSellerAllocation> sellerAllocations(StockReviewPendingPurchase purchase) {
        if (purchase == null) {
            return StockReviewQuote.ZERO.getSellerAllocations();
        }
        return portfolioQuote.sellerAllocations(purchase.getWeaponId(), purchase.getSubmarketId());
    }

    int totalCost() {
        return totalCost;
    }

    float totalCargoSpaceDelta() {
        return totalCargoSpaceDelta;
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
        String key = record.getWeaponId() + "|" + (submarketId == null ? "" : submarketId) + "|" + maxByStock;
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
                pendingPurchases, record.getWeaponId(), submarketId, quantity));
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
            if (submarketId.equals(stock.getSubmarketId()) && stock.isPurchasable()) {
                sourceCount += stock.getCount();
            }
        }
        int pendingFromSource = 0;
        if (pendingPurchases != null) {
            for (int i = 0; i < pendingPurchases.size(); i++) {
                StockReviewPendingPurchase purchase = pendingPurchases.get(i);
                if (purchase.matches(record.getWeaponId(), submarketId) && purchase.getQuantity() > 0) {
                    pendingFromSource += purchase.getQuantity();
                }
            }
        }
        return Math.max(0, sourceCount - pendingFromSource);
    }

    private static void add(Map<String, Integer> counts, String weaponId, int quantity) {
        if (weaponId == null || quantity == 0) {
            return;
        }
        counts.put(weaponId, Integer.valueOf(get(counts, weaponId) + quantity));
    }

    private static int get(Map<String, Integer> counts, String weaponId) {
        Integer value = counts.get(weaponId);
        return value == null ? 0 : value.intValue();
    }

}
