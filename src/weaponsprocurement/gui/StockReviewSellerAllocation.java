package weaponsprocurement.gui;

final class StockReviewSellerAllocation {
    private final String submarketName;
    private final String submarketId;
    private final int quantity;
    private final long cost;

    StockReviewSellerAllocation(String submarketName, String submarketId, int quantity, long cost) {
        this.submarketName = submarketName;
        this.submarketId = submarketId;
        this.quantity = quantity;
        this.cost = cost;
    }

    String getSubmarketName() {
        return submarketName;
    }

    int getQuantity() {
        return quantity;
    }

    long getCost() {
        return cost;
    }

    boolean isBlackMarket() {
        return submarketId != null && submarketId.toLowerCase().contains("black");
    }
}
