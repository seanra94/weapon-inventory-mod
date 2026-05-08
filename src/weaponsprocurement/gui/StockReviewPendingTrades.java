package weaponsprocurement.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class StockReviewPendingTrades {
    private final List<StockReviewPendingTrade> trades = new ArrayList<StockReviewPendingTrade>();

    List<StockReviewPendingTrade> asList() {
        return Collections.unmodifiableList(trades);
    }

    boolean isEmpty() {
        return trades.isEmpty();
    }

    void clear() {
        trades.clear();
    }

    void replaceWith(List<StockReviewPendingTrade> source) {
        trades.clear();
        if (source == null) {
            return;
        }
        for (int i = 0; i < source.size(); i++) {
            StockReviewPendingTrade trade = source.get(i);
            if (trade != null && !trade.isZero()) {
                StockReviewPendingTrade copy = trade.copy();
                if (copy != null) {
                    trades.add(copy);
                }
            }
        }
    }

    void add(String itemKey, String submarketId, int quantity) {
        if (itemKey == null || itemKey.isEmpty() || quantity == 0) {
            return;
        }
        StockReviewPendingTrade existing = find(itemKey, submarketId);
        if (existing == null) {
            StockReviewPendingTrade trade = StockReviewPendingTrade.create(itemKey, submarketId, quantity);
            if (trade != null) {
                trades.add(trade);
            }
            return;
        }
        existing.addQuantity(quantity);
        if (existing.isZero()) {
            trades.remove(existing);
        }
    }

    void adjustItemNet(String itemKey, int delta) {
        if (itemKey == null || itemKey.isEmpty() || delta == 0) {
            return;
        }
        int remaining = delta;
        if (delta < 0) {
            remaining = reduceExistingBuys(itemKey, -delta);
            if (remaining > 0) {
                add(itemKey, null, -remaining);
            }
            return;
        }
        remaining = reduceExistingSells(itemKey, delta);
        if (remaining > 0) {
            add(itemKey, null, remaining);
        }
    }

    void resetItem(String itemKey) {
        for (int i = trades.size() - 1; i >= 0; i--) {
            if (itemKey != null && itemKey.equals(trades.get(i).getItemKey())) {
                trades.remove(i);
            }
        }
    }

    void removeExecuted(List<StockReviewPendingTrade> executionOrder, int failedIndex) {
        if (executionOrder == null) {
            return;
        }
        for (int i = 0; i < failedIndex; i++) {
            removeMatching(executionOrder.get(i));
        }
    }

    private void removeMatching(StockReviewPendingTrade executed) {
        if (executed == null) {
            return;
        }
        for (int i = trades.size() - 1; i >= 0; i--) {
            StockReviewPendingTrade trade = trades.get(i);
            if (trade.matches(executed.getItemKey(), executed.getSubmarketId())
                    && trade.getQuantity() == executed.getQuantity()) {
                trades.remove(i);
                return;
            }
        }
    }

    private StockReviewPendingTrade find(String itemKey, String submarketId) {
        for (int i = 0; i < trades.size(); i++) {
            StockReviewPendingTrade trade = trades.get(i);
            if (trade.matches(itemKey, submarketId)) {
                return trade;
            }
        }
        return null;
    }

    private int reduceExistingBuys(String itemKey, int quantity) {
        int remaining = quantity;
        for (int i = trades.size() - 1; i >= 0 && remaining > 0; i--) {
            StockReviewPendingTrade trade = trades.get(i);
            if (!itemKey.equals(trade.getItemKey()) || !trade.isBuy()) {
                continue;
            }
            int reduced = Math.min(remaining, trade.getQuantity());
            trade.addQuantity(-reduced);
            remaining -= reduced;
            if (trade.isZero()) {
                trades.remove(i);
            }
        }
        return remaining;
    }

    private int reduceExistingSells(String itemKey, int quantity) {
        int remaining = quantity;
        for (int i = trades.size() - 1; i >= 0 && remaining > 0; i--) {
            StockReviewPendingTrade trade = trades.get(i);
            if (!itemKey.equals(trade.getItemKey()) || !trade.isSell()) {
                continue;
            }
            int reduced = Math.min(remaining, -trade.getQuantity());
            trade.addQuantity(reduced);
            remaining -= reduced;
            if (trade.isZero()) {
                trades.remove(i);
            }
        }
        return remaining;
    }
}
