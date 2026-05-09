package weaponsprocurement.core;

import java.util.ArrayList;
import java.util.List;

final class StockPurchasePlan {
    final List<StockPurchaseLine> lines;
    final int totalQuantity;
    final long totalCost;
    final float totalSpace;

    private StockPurchasePlan(List<StockPurchaseLine> lines, int totalQuantity, long totalCost, float totalSpace) {
        this.lines = lines;
        this.totalQuantity = totalQuantity;
        this.totalCost = totalCost;
        this.totalSpace = totalSpace;
    }

    static StockPurchasePlan build(List<StockPurchaseSource> sources, int requestedQuantity) {
        int remaining = requestedQuantity;
        int totalQuantity = 0;
        long totalCost = 0L;
        float totalSpace = 0f;
        List<StockPurchaseLine> lines = new ArrayList<StockPurchaseLine>();
        for (StockPurchaseSource source : sources) {
            if (remaining <= 0) {
                break;
            }
            int quantity = Math.min(remaining, source.available);
            if (quantity <= 0) {
                continue;
            }
            lines.add(new StockPurchaseLine(source, quantity));
            remaining -= quantity;
            totalQuantity += quantity;
            totalCost = TradeMoney.safeAdd(totalCost, TradeMoney.lineTotal(source.unitPrice, quantity));
            totalSpace += source.unitCargoSpace * quantity;
        }
        return new StockPurchasePlan(lines, totalQuantity, totalCost, totalSpace);
    }
}
