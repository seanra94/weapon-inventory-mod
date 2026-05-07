package weaponinventorymod.gui;

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
}
