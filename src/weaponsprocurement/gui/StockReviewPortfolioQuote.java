package weaponsprocurement.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class StockReviewPortfolioQuote {
    private final Map<String, StockReviewQuote> quotesByLine = new HashMap<String, StockReviewQuote>();
    private final Map<String, Integer> costByWeapon = new HashMap<String, Integer>();
    private int totalCost = 0;
    private int totalBuyCost = 0;
    private int totalBaseBuyCost = 0;
    private int totalBuyQuantity = 0;
    private float totalCargoSpaceDelta = 0f;
    private boolean priceUnavailable = false;

    void addLine(StockReviewPendingPurchase purchase, StockReviewQuote quote) {
        String lineKey = lineKey(purchase.getWeaponId(), purchase.getSubmarketId());
        quotesByLine.put(lineKey, quote);
        if (quote.getCost() == StockReviewQuoteBook.PRICE_UNAVAILABLE) {
            priceUnavailable = true;
            costByWeapon.put(purchase.getWeaponId(), Integer.valueOf(StockReviewQuoteBook.PRICE_UNAVAILABLE));
        } else {
            add(costByWeapon, purchase.getWeaponId(), quote.getCost());
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

    int costForWeapon(String weaponId) {
        return get(costByWeapon, weaponId);
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

    int costForLine(String weaponId, String submarketId) {
        return quoteForLine(weaponId, submarketId).getCost();
    }

    float cargoSpaceForLine(String weaponId, String submarketId) {
        return quoteForLine(weaponId, submarketId).getCargoSpaceDelta();
    }

    List<StockReviewSellerAllocation> sellerAllocations(String weaponId, String submarketId) {
        return quoteForLine(weaponId, submarketId).getSellerAllocations();
    }

    private StockReviewQuote quoteForLine(String weaponId, String submarketId) {
        StockReviewQuote quote = quotesByLine.get(lineKey(weaponId, submarketId));
        return quote == null ? StockReviewQuote.ZERO : quote;
    }

    static String lineKey(String weaponId, String submarketId) {
        return (weaponId == null ? "" : weaponId) + "|" + (submarketId == null ? "" : submarketId);
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
