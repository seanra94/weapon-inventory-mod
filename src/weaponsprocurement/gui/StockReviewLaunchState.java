package weaponsprocurement.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class StockReviewLaunchState {
    private final StockReviewState state;
    private final List<StockReviewPendingTrade> pendingTrades;
    private final boolean reviewMode;

    StockReviewLaunchState(StockReviewState state,
                           List<StockReviewPendingTrade> pendingTrades,
                           boolean reviewMode) {
        this.state = state == null ? null : new StockReviewState(state);
        this.pendingTrades = immutableCopy(pendingTrades);
        this.reviewMode = reviewMode;
    }

    StockReviewState getState() {
        return state;
    }

    List<StockReviewPendingTrade> getPendingTrades() {
        return pendingTrades;
    }

    boolean isReviewMode() {
        return reviewMode;
    }

    private static List<StockReviewPendingTrade> immutableCopy(List<StockReviewPendingTrade> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        List<StockReviewPendingTrade> result = new ArrayList<StockReviewPendingTrade>();
        for (int i = 0; i < source.size(); i++) {
            StockReviewPendingTrade trade = source.get(i);
            if (trade != null) {
                result.add(trade.copy());
            }
        }
        return Collections.unmodifiableList(result);
    }
}
