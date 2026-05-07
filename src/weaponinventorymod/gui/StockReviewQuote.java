package weaponinventorymod.gui;

import java.util.Collections;
import java.util.List;

final class StockReviewQuote {
    static final StockReviewQuote ZERO = new StockReviewQuote(0, 0f,
            Collections.<StockReviewSellerAllocation>emptyList());

    private final int cost;
    private final float cargoSpaceDelta;
    private final List<StockReviewSellerAllocation> sellerAllocations;

    StockReviewQuote(int cost, float cargoSpaceDelta, List<StockReviewSellerAllocation> sellerAllocations) {
        this.cost = cost;
        this.cargoSpaceDelta = cargoSpaceDelta;
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

    float getCargoSpaceDelta() {
        return cargoSpaceDelta;
    }

    List<StockReviewSellerAllocation> getSellerAllocations() {
        return sellerAllocations;
    }
}
