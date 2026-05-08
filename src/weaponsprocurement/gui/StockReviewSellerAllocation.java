package weaponsprocurement.gui;

final class StockReviewSellerAllocation {
    private final String submarketName;
    private final String submarketId;
    private final int quantity;
    private final int cost;

    StockReviewSellerAllocation(String submarketName, String submarketId, int quantity, int cost) {
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

    int getCost() {
        return cost;
    }

    boolean isBlackMarket() {
        return submarketId != null && submarketId.toLowerCase().contains("black");
    }
}
