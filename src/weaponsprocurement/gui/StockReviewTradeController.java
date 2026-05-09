package weaponsprocurement.gui;

import weaponsprocurement.core.WeaponStockRecord;
import weaponsprocurement.core.WeaponStockSnapshot;

import java.util.List;

final class StockReviewTradeController {
    interface Host {
        WeaponStockSnapshot snapshot();

        void updateTradeWarning(String explicitWarning);

        void requestContentRebuild();

        void postMessage(String message);
    }

    private final StockReviewState state;
    private final StockReviewPendingTrades pendingTrades;
    private final Host host;

    StockReviewTradeController(StockReviewState state,
                               StockReviewPendingTrades pendingTrades,
                               Host host) {
        this.state = state;
        this.pendingTrades = pendingTrades;
        this.host = host;
    }

    void addPendingTrade(StockReviewAction action) {
        int available = availableFor(action);
        if (available <= 0) {
            host.postMessage(action.getQuantity() < 0
                    ? "No more player-cargo stock is available to sell."
                    : "No more buyable stock is available for that plan.");
            host.updateTradeWarning(null);
            host.requestContentRebuild();
            return;
        }
        int requested = action.getQuantity();
        int quantity = requested > 0 ? Math.min(requested, available) : -Math.min(-requested, available);
        pendingTrades.add(action.getItemKey(), action.getSubmarketId(), quantity);
        if (Math.abs(quantity) < Math.abs(requested)) {
            host.postMessage("Only " + Math.abs(quantity) + " more can be planned for that item.");
        }
        host.updateTradeWarning(null);
        host.requestContentRebuild();
    }

    void adjustPendingTrade(StockReviewAction action) {
        int available = availableFor(action);
        if (available <= 0) {
            host.postMessage(action.getQuantity() < 0
                    ? "No more queued or player-cargo stock is available to remove from the plan."
                    : "No more queued sales or buyable stock is available for that plan.");
            host.updateTradeWarning(null);
            host.requestContentRebuild();
            return;
        }
        int requested = action.getQuantity();
        int quantity = requested > 0 ? Math.min(requested, available) : -Math.min(-requested, available);
        pendingTrades.adjustItemNet(action.getItemKey(), quantity);
        if (Math.abs(quantity) < Math.abs(requested)) {
            host.postMessage("Only " + Math.abs(quantity) + " more can be planned for that item.");
        }
        host.updateTradeWarning(null);
        host.requestContentRebuild();
    }

    void resetPlan(String itemKey) {
        pendingTrades.resetItem(itemKey);
        host.updateTradeWarning(null);
        host.requestContentRebuild();
    }

    void purchaseAllUntilSufficient() {
        WeaponStockSnapshot snapshot = host.snapshot();
        if (snapshot == null) {
            return;
        }
        int added = 0;
        String explicitWarning = null;
        StockReviewQuoteBook quoteBook = new StockReviewQuoteBook(snapshot);
        List<WeaponStockRecord> records = StockReviewTradePlanner.cheapestFirstVisibleBuyableRecords(snapshot);
        StockReviewTradeContext tradeContext = new StockReviewTradeContext(snapshot, pendingTrades.asList());
        for (int i = 0; i < records.size(); i++) {
            WeaponStockRecord record = records.get(i);
            int needed = tradeContext.buyNeededForSufficiency(record);
            int quantity = tradeContext.affordableBuyQuantity(record, null, needed);
            explicitWarning = StockReviewTradeWarnings.purchaseAllLimitWarning(
                    quoteBook,
                    pendingTrades.asList(),
                    record,
                    tradeContext,
                    needed,
                    quantity,
                    explicitWarning);
            if (quantity <= 0) {
                continue;
            }
            pendingTrades.add(record.getItemKey(), null, quantity);
            tradeContext = new StockReviewTradeContext(snapshot, pendingTrades.asList());
            added += quantity;
        }
        if (added <= 0) {
            host.postMessage("No additional sufficient-stock purchases are available.");
            host.updateTradeWarning(explicitWarning);
            host.requestContentRebuild();
            return;
        }
        host.updateTradeWarning(explicitWarning);
        host.requestContentRebuild();
    }

    void sellAllUntilSufficient() {
        WeaponStockSnapshot snapshot = host.snapshot();
        if (snapshot == null) {
            return;
        }
        int removed = 0;
        List<WeaponStockRecord> records = StockReviewTradePlanner.visibleTradeableRecords(snapshot);
        StockReviewTradeContext tradeContext = new StockReviewTradeContext(snapshot, pendingTrades.asList());
        for (int i = 0; i < records.size(); i++) {
            WeaponStockRecord record = records.get(i);
            int quantity = tradeContext.sellableUntilSufficient(record);
            if (quantity <= 0) {
                continue;
            }
            pendingTrades.add(record.getItemKey(), null, -quantity);
            tradeContext = new StockReviewTradeContext(snapshot, pendingTrades.asList());
            removed += quantity;
        }
        if (removed <= 0) {
            host.postMessage("No sufficient-stock sales are available.");
            host.updateTradeWarning(null);
            host.requestContentRebuild();
            return;
        }
        host.updateTradeWarning(null);
        host.requestContentRebuild();
    }

    private int availableFor(StockReviewAction action) {
        WeaponStockSnapshot snapshot = host.snapshot();
        WeaponStockRecord record = snapshot == null ? null : snapshot.getRecord(action.getItemKey());
        if (record == null) {
            return 0;
        }
        StockReviewTradeContext tradeContext = new StockReviewTradeContext(snapshot, pendingTrades.asList());
        if (action.getQuantity() < 0) {
            return tradeContext.negativeAdjustmentRemaining(record, -action.getQuantity());
        }
        if (action.getSubmarketId() == null) {
            return tradeContext.positiveAdjustmentRemaining(record, action.getQuantity());
        }
        return tradeContext.affordableBuyQuantity(record, action.getSubmarketId(), action.getQuantity());
    }
}
