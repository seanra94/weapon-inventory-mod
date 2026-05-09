package weaponsprocurement.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import weaponsprocurement.core.TradeMoney;

final class StockReviewPortfolioQuote {
    private final Map<String, StockReviewQuote> quotesByLine = new HashMap<String, StockReviewQuote>();
    private final Map<String, Long> costByItem = new HashMap<String, Long>();
    private long totalCost = 0L;
    private long totalBuyCost = 0L;
    private long totalBaseBuyCost = 0L;
    private int totalBuyQuantity = 0;
    private float totalCargoSpaceDelta = 0f;
    private boolean priceUnavailable = false;

    void addLine(StockReviewPendingTrade trade, StockReviewQuote quote) {
        String lineKey = lineKey(trade.getItemKey(), trade.getSubmarketId());
        quotesByLine.put(lineKey, quote);
        if (quote.getCost() == StockReviewQuoteBook.PRICE_UNAVAILABLE) {
            priceUnavailable = true;
            costByItem.put(trade.getItemKey(), Long.valueOf(StockReviewQuoteBook.PRICE_UNAVAILABLE));
        } else {
            add(costByItem, trade.getItemKey(), quote.getCost());
            totalCost = TradeMoney.safeAdd(totalCost, quote.getCost());
            if (trade.isBuy()) {
                totalBuyCost = TradeMoney.safeAdd(totalBuyCost, quote.getCost());
                totalBaseBuyCost = TradeMoney.safeAdd(totalBaseBuyCost, quote.getBaseCost());
                totalBuyQuantity += quote.getBuyQuantity();
            }
        }
        totalCargoSpaceDelta += quote.getCargoSpaceDelta();
    }

    long totalCost() {
        return priceUnavailable ? StockReviewQuoteBook.PRICE_UNAVAILABLE : totalCost;
    }

    float totalCargoSpaceDelta() {
        return totalCargoSpaceDelta;
    }

    long costForItem(String itemKey) {
        return get(costByItem, itemKey);
    }

    long totalMarkupPaid() {
        if (priceUnavailable) {
            return 0L;
        }
        return Math.max(0L, totalBuyCost - totalBaseBuyCost);
    }

    float averageBuyMultiplier() {
        if (totalBaseBuyCost <= 0) {
            return 1f;
        }
        return (float) totalBuyCost / (float) totalBaseBuyCost;
    }

    int totalBuyQuantity() {
        return totalBuyQuantity;
    }

    long costForLine(String itemKey, String submarketId) {
        return quoteForLine(itemKey, submarketId).getCost();
    }

    float cargoSpaceForLine(String itemKey, String submarketId) {
        return quoteForLine(itemKey, submarketId).getCargoSpaceDelta();
    }

    List<StockReviewSellerAllocation> sellerAllocations(String itemKey, String submarketId) {
        return quoteForLine(itemKey, submarketId).getSellerAllocations();
    }

    private StockReviewQuote quoteForLine(String itemKey, String submarketId) {
        StockReviewQuote quote = quotesByLine.get(lineKey(itemKey, submarketId));
        return quote == null ? StockReviewQuote.ZERO : quote;
    }

    static String lineKey(String itemKey, String submarketId) {
        return (itemKey == null ? "" : itemKey) + "|" + (submarketId == null ? "" : submarketId);
    }

    private static void add(Map<String, Long> counts, String itemKey, long quantity) {
        if (itemKey == null || quantity == 0L) {
            return;
        }
        counts.put(itemKey, Long.valueOf(TradeMoney.safeAdd(get(counts, itemKey), quantity)));
    }

    private static long get(Map<String, Long> counts, String itemKey) {
        Long value = counts.get(itemKey);
        return value == null ? 0L : value.longValue();
    }
}
