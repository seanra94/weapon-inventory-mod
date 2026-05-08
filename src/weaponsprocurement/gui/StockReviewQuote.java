package weaponsprocurement.gui;

import java.util.Collections;
import java.util.List;

final class StockReviewQuote {
    static final StockReviewQuote ZERO = new StockReviewQuote(0, 0f,
            0, 0, Collections.<StockReviewSellerAllocation>emptyList());

    private final int cost;
    private final float cargoSpaceDelta;
    private final int baseCost;
    private final int buyQuantity;
    private final List<StockReviewSellerAllocation> sellerAllocations;

    StockReviewQuote(int cost, float cargoSpaceDelta, List<StockReviewSellerAllocation> sellerAllocations) {
        this(cost, cargoSpaceDelta, cost, 0, sellerAllocations);
    }

    StockReviewQuote(int cost,
                     float cargoSpaceDelta,
                     int baseCost,
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

    int getCost() {
        return cost;
    }

    int getBaseCost() {
        return baseCost;
    }

    int getBuyQuantity() {
        return buyQuantity;
    }

    int getMarkupPaid() {
        if (cost == StockReviewQuoteBook.PRICE_UNAVAILABLE) {
            return 0;
        }
        return Math.max(0, cost - baseCost);
    }

    float getCargoSpaceDelta() {
        return cargoSpaceDelta;
    }

    List<StockReviewSellerAllocation> getSellerAllocations() {
        return sellerAllocations;
    }
}
