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

    void add(String weaponId, String submarketId, int quantity) {
        StockReviewPendingPurchase existing = find(weaponId, submarketId);
        if (existing == null) {
            trades.add(new StockReviewPendingPurchase(weaponId, submarketId, quantity));
            return;
        }
        existing.addQuantity(quantity);
        if (existing.isZero()) {
            trades.remove(existing);
        }
    }

    void adjustWeaponNet(String weaponId, int delta) {
        if (weaponId == null || weaponId.isEmpty() || delta == 0) {
            return;
        }
        int remaining = delta;
        if (delta < 0) {
            remaining = reduceExistingBuys(weaponId, -delta);
            if (remaining > 0) {
                add(weaponId, null, -remaining);
            }
            return;
        }
        remaining = reduceExistingSells(weaponId, delta);
        if (remaining > 0) {
            add(weaponId, null, remaining);
        }
    }

    void resetWeapon(String weaponId) {
        for (int i = trades.size() - 1; i >= 0; i--) {
            if (weaponId != null && weaponId.equals(trades.get(i).getWeaponId())) {
                trades.remove(i);
            }
        }
    }

    void removeExecuted(List<StockReviewPendingPurchase> executionOrder, int failedIndex) {
        if (executionOrder == null) {
            return;
        }
        for (int i = 0; i < failedIndex; i++) {
            trades.remove(executionOrder.get(i));
        }
    }

    private StockReviewPendingPurchase find(String weaponId, String submarketId) {
        for (int i = 0; i < trades.size(); i++) {
            StockReviewPendingPurchase purchase = trades.get(i);
            if (purchase.matches(weaponId, submarketId)) {
                return purchase;
            }
        }
        return null;
    }

    private int reduceExistingBuys(String weaponId, int quantity) {
        int remaining = quantity;
        for (int i = trades.size() - 1; i >= 0 && remaining > 0; i--) {
            StockReviewPendingPurchase trade = trades.get(i);
            if (!weaponId.equals(trade.getWeaponId()) || !trade.isBuy()) {
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

    private int reduceExistingSells(String weaponId, int quantity) {
        int remaining = quantity;
        for (int i = trades.size() - 1; i >= 0 && remaining > 0; i--) {
            StockReviewPendingPurchase trade = trades.get(i);
            if (!weaponId.equals(trade.getWeaponId()) || !trade.isSell()) {
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
