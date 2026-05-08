package weaponsprocurement.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class StockReviewLaunchState {
    private final StockReviewState state;
    private final List<StockReviewPendingPurchase> pendingTrades;
    private final boolean reviewMode;

    StockReviewLaunchState(StockReviewState state,
                           List<StockReviewPendingPurchase> pendingTrades,
                           boolean reviewMode) {
        this.state = state == null ? null : new StockReviewState(state);
        this.pendingTrades = immutableCopy(pendingTrades);
        this.reviewMode = reviewMode;
    }

    StockReviewState getState() {
        return state;
    }

    List<StockReviewPendingPurchase> getPendingTrades() {
        return pendingTrades;
    }

    boolean isReviewMode() {
        return reviewMode;
    }

    private static List<StockReviewPendingPurchase> immutableCopy(List<StockReviewPendingPurchase> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        List<StockReviewPendingPurchase> result = new ArrayList<StockReviewPendingPurchase>();
        for (int i = 0; i < source.size(); i++) {
            StockReviewPendingPurchase purchase = source.get(i);
            if (purchase != null) {
                result.add(purchase.copy());
            }
        }
        return Collections.unmodifiableList(result);
    }
}
