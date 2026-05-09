package weaponsprocurement.gui;

import java.util.Collections;
import java.util.List;

final class StockReviewQuote {
    static final StockReviewQuote ZERO = new StockReviewQuote(0, 0f,
            0, 0, Collections.<StockReviewSellerAllocation>emptyList());

    private final long cost;
    private final float cargoSpaceDelta;
    private final long baseCost;
    private final int buyQuantity;
    private final List<StockReviewSellerAllocation> sellerAllocations;

    StockReviewQuote(long cost, float cargoSpaceDelta, List<StockReviewSellerAllocation> sellerAllocations) {
        this(cost, cargoSpaceDelta, cost, 0, sellerAllocations);
    }

    StockReviewQuote(long cost,
                     float cargoSpaceDelta,
                     long baseCost,
                     int buyQuantity,
                     List<StockReviewSellerAllocation> sellerAllocations) {
        this.cost = cost;
        this.cargoSpaceDelta = cargoSpaceDelta;
        this.baseCost = baseCost;
        this.buyQuantity = buyQuantity;
        this.sellerAllocations = Collections.unmodifiableList(sellerAllocations);
    }

    static StockReviewQuote priceUnavailable() {
        return new StockReviewQuote(StockReviewQuoteBook.PRICE_UNAVAILABLE, 0f,
                Collections.<StockReviewSellerAllocation>emptyList());
    }

    static StockReviewQuote priceUnavailable(float cargoSpaceDelta,
                                             List<StockReviewSellerAllocation> sellerAllocations) {
        return new StockReviewQuote(StockReviewQuoteBook.PRICE_UNAVAILABLE, cargoSpaceDelta, sellerAllocations);
    }

    long getCost() {
        return cost;
    }

    long getBaseCost() {
        return baseCost;
    }

    int getBuyQuantity() {
        return buyQuantity;
    }

    long getMarkupPaid() {
        if (cost == StockReviewQuoteBook.PRICE_UNAVAILABLE) {
            return 0L;
        }
        return Math.max(0L, cost - baseCost);
    }

    float getCargoSpaceDelta() {
        return cargoSpaceDelta;
    }

    List<StockReviewSellerAllocation> getSellerAllocations() {
        return sellerAllocations;
    }
}
