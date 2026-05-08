package weaponinventorymod.gui;

final class StockReviewPendingPurchase {
    private final String weaponId;
    private final String submarketId;
    private int quantity;

    StockReviewPendingPurchase(String weaponId, String submarketId, int quantity) {
        this.weaponId = weaponId;
        this.submarketId = submarketId;
        this.quantity = quantity;
    }

    String getWeaponId() {
        return weaponId;
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

    boolean matches(String otherWeaponId, String otherSubmarketId) {
        if (weaponId == null ? otherWeaponId != null : !weaponId.equals(otherWeaponId)) {
            return false;
        }
        return submarketId == null ? otherSubmarketId == null : submarketId.equals(otherSubmarketId);
    }

    StockReviewPendingPurchase copy() {
        return new StockReviewPendingPurchase(weaponId, submarketId, quantity);
    }
}
