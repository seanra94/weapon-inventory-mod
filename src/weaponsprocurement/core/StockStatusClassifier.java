package weaponsprocurement.core;

public final class StockStatusClassifier {
    public StockCategory classify(int ownedCount, int desiredCount) {
        if (desiredCount <= 0 || ownedCount >= desiredCount) {
            return StockCategory.SUFFICIENT;
        }
        if (ownedCount <= 0) {
            return StockCategory.NO_STOCK;
        }
        return StockCategory.INSUFFICIENT;
    }
}
