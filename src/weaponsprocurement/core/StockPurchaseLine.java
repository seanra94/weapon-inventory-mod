package weaponsprocurement.core;

final class StockPurchaseLine {
    final StockPurchaseSource source;
    final int quantity;

    StockPurchaseLine(StockPurchaseSource source, int quantity) {
        this.source = source;
        this.quantity = quantity;
    }
}
