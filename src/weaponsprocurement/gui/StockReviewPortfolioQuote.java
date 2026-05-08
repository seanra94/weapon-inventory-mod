package weaponsprocurement.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class StockReviewPortfolioQuote {
    private final Map<String, StockReviewQuote> quotesByLine = new HashMap<String, StockReviewQuote>();
    private final Map<String, Integer> costByItem = new HashMap<String, Integer>();
    private int totalCost = 0;
    private int totalBuyCost = 0;
    private int totalBaseBuyCost = 0;
    private int totalBuyQuantity = 0;
    private float totalCargoSpaceDelta = 0f;
    private boolean priceUnavailable = false;

    void addLine(StockReviewPendingPurchase purchase, StockReviewQuote quote) {
        String lineKey = lineKey(purchase.getItemKey(), purchase.getSubmarketId());
        quotesByLine.put(lineKey, quote);
        if (quote.getCost() == StockReviewQuoteBook.PRICE_UNAVAILABLE) {
            priceUnavailable = true;
            costByItem.put(purchase.getItemKey(), Integer.valueOf(StockReviewQuoteBook.PRICE_UNAVAILABLE));
        } else {
            add(costByItem, purchase.getItemKey(), quote.getCost());
            totalCost += quote.getCost();
            if (purchase.isBuy()) {
                totalBuyCost += quote.getCost();
                totalBaseBuyCost += quote.getBaseCost();
                totalBuyQuantity += quote.getBuyQuantity();
            }
        }
        totalCargoSpaceDelta += quote.getCargoSpaceDelta();
    }

    int totalCost() {
        return priceUnavailable ? StockReviewQuoteBook.PRICE_UNAVAILABLE : totalCost;
    }

    float totalCargoSpaceDelta() {
        return totalCargoSpaceDelta;
    }

    int costForItem(String itemKey) {
        return get(costByItem, itemKey);
    }

    int totalMarkupPaid() {
        if (priceUnavailable) {
            return 0;
        }
        return Math.max(0, totalBuyCost - totalBaseBuyCost);
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

    int costForLine(String itemKey, String submarketId) {
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
