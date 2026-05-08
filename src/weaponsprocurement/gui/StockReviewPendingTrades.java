package weaponsprocurement.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class StockReviewPendingTrades {
    private final List<StockReviewPendingPurchase> trades = new ArrayList<StockReviewPendingPurchase>();

    List<StockReviewPendingPurchase> asList() {
        return Collections.unmodifiableList(trades);
    }

    boolean isEmpty() {
        return trades.isEmpty();
    }

    void clear() {
        trades.clear();
    }

    void replaceWith(List<StockReviewPendingPurchase> source) {
        trades.clear();
        if (source == null) {
            return;
        }
        for (int i = 0; i < source.size(); i++) {
            StockReviewPendingPurchase purchase = source.get(i);
            if (purchase != null && !purchase.isZero()) {
                trades.add(purchase.copy());
            }
        }
    }

    void add(String itemKey, String submarketId, int quantity) {
        if (itemKey == null || itemKey.isEmpty() || quantity == 0) {
            return;
        }
        StockReviewPendingPurchase existing = find(itemKey, submarketId);
        if (existing == null) {
            trades.add(new StockReviewPendingPurchase(itemKey, submarketId, quantity));
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

    void removeExecuted(List<StockReviewPendingPurchase> executionOrder, int failedIndex) {
        if (executionOrder == null) {
            return;
        }
        for (int i = 0; i < failedIndex; i++) {
            removeMatching(executionOrder.get(i));
        }
    }

    private void removeMatching(StockReviewPendingPurchase executed) {
        if (executed == null) {
            return;
        }
        for (int i = trades.size() - 1; i >= 0; i--) {
            StockReviewPendingPurchase trade = trades.get(i);
            if (trade.matches(executed.getItemKey(), executed.getSubmarketId())
                    && trade.getQuantity() == executed.getQuantity()) {
                trades.remove(i);
                return;
            }
        }
    }

    private StockReviewPendingPurchase find(String itemKey, String submarketId) {
        for (int i = 0; i < trades.size(); i++) {
            StockReviewPendingPurchase purchase = trades.get(i);
            if (purchase.matches(itemKey, submarketId)) {
                return purchase;
            }
        }
        return null;
    }

    private int reduceExistingBuys(String itemKey, int quantity) {
        int remaining = quantity;
        for (int i = trades.size() - 1; i >= 0 && remaining > 0; i--) {
            StockReviewPendingPurchase trade = trades.get(i);
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
            StockReviewPendingPurchase trade = trades.get(i);
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
