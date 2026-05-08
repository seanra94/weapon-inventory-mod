package weaponsprocurement.gui;

final class StockReviewPendingPurchase {
    private final String itemKey;
    private final String submarketId;
    private int quantity;

    private StockReviewPendingPurchase(String itemKey, String submarketId, int quantity) {
        this.itemKey = itemKey;
        this.submarketId = submarketId;
        this.quantity = quantity;
    }

    static StockReviewPendingPurchase create(String itemKey, String submarketId, int quantity) {
        return itemKey == null || itemKey.isEmpty() || quantity == 0
                ? null
                : new StockReviewPendingPurchase(itemKey, submarketId, quantity);
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
        return create(itemKey, submarketId, quantity);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof StockReviewPendingPurchase)) {
            return false;
        }
        StockReviewPendingPurchase purchase = (StockReviewPendingPurchase) other;
        return quantity == purchase.quantity && matches(purchase.itemKey, purchase.submarketId);
    }

    @Override
    public int hashCode() {
        int result = itemKey == null ? 0 : itemKey.hashCode();
        result = 31 * result + (submarketId == null ? 0 : submarketId.hashCode());
        result = 31 * result + quantity;
        return result;
    }
}
