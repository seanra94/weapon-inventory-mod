package weaponsprocurement.gui;

final class StockReviewPendingPurchase {
    private final String itemKey;
    private final String submarketId;
    private int quantity;

    StockReviewPendingPurchase(String itemKey, String submarketId, int quantity) {
        this.itemKey = itemKey;
        this.submarketId = submarketId;
        this.quantity = quantity;
    }

    String getItemKey() {
        return itemKey;
    }

    String getSubmarketId() {
        return submarketId;
    }

    int getQuantity() {
        return quantity;
    }

    void addQuantity(int amount) {
        quantity += amount;
    }

    boolean isBuy() {
        return quantity > 0;
    }

    boolean isSell() {
        return quantity < 0;
    }

    boolean isZero() {
        return quantity == 0;
    }

    boolean matches(String otherItemKey, String otherSubmarketId) {
        if (itemKey == null ? otherItemKey != null : !itemKey.equals(otherItemKey)) {
            return false;
        }
        return submarketId == null ? otherSubmarketId == null : submarketId.equals(otherSubmarketId);
    }

    StockReviewPendingPurchase copy() {
        return new StockReviewPendingPurchase(itemKey, submarketId, quantity);
    }
}
