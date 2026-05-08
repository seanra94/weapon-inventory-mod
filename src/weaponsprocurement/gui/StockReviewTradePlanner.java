package weaponsprocurement.gui;

import weaponsprocurement.core.StockCategory;
import weaponsprocurement.core.StockItemType;
import weaponsprocurement.core.WeaponStockRecord;
import weaponsprocurement.core.WeaponStockSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

final class StockReviewTradePlanner {
    private static final int MATCH_SELL = 0;
    private static final int MATCH_SELLER_BUY = 1;
    private static final int MATCH_GENERIC_BUY = 2;

    private StockReviewTradePlanner() {
    }

    static List<WeaponStockRecord> visibleTradeableRecords(WeaponStockSnapshot snapshot, StockCategory category) {
        List<WeaponStockRecord> result = new ArrayList<WeaponStockRecord>();
        if (snapshot == null || category == null) {
            return result;
        }
        addVisibleTradeableRecords(result, snapshot.getRecords(category));
        return result;
    }

    static List<WeaponStockRecord> visibleTradeableRecords(WeaponStockSnapshot snapshot, StockItemType itemType, StockCategory category) {
        List<WeaponStockRecord> result = new ArrayList<WeaponStockRecord>();
        if (snapshot == null || category == null) {
            return result;
        }
        addVisibleTradeableRecords(result, snapshot.getRecords(itemType, category));
        return result;
    }

    static List<WeaponStockRecord> visibleTradeableRecords(WeaponStockSnapshot snapshot) {
        List<WeaponStockRecord> result = new ArrayList<WeaponStockRecord>();
        if (snapshot == null) {
            return result;
        }
        for (StockCategory category : StockCategory.values()) {
            addVisibleTradeableRecords(result, snapshot.getRecords(category));
        }
        return result;
    }

    static List<WeaponStockRecord> visibleBuyableRecords(WeaponStockSnapshot snapshot) {
        List<WeaponStockRecord> result = new ArrayList<WeaponStockRecord>();
        if (snapshot == null) {
            return result;
        }
        for (StockCategory category : StockCategory.values()) {
            addVisibleBuyableRecords(result, snapshot.getRecords(category));
        }
        return result;
    }

    static List<WeaponStockRecord> cheapestFirstVisibleBuyableRecords(WeaponStockSnapshot snapshot) {
        List<WeaponStockRecord> result = visibleBuyableRecords(snapshot);
        Collections.sort(result, new CheapestBuyRecordComparator(snapshot));
        return result;
    }

    static List<StockReviewPendingTrade> withAdjustment(List<StockReviewPendingTrade> pendingTrades,
                                                           String itemKey,
                                                           String submarketId,
                                                           int delta) {
        List<StockReviewPendingTrade> result = new ArrayList<StockReviewPendingTrade>();
        boolean adjusted = false;
        if (pendingTrades != null) {
            for (int i = 0; i < pendingTrades.size(); i++) {
                StockReviewPendingTrade trade = pendingTrades.get(i);
                int quantity = trade.getQuantity();
                if (trade.matches(itemKey, submarketId)) {
                    quantity += delta;
                    adjusted = true;
                }
                if (quantity != 0) {
                    addPending(result, trade.getItemKey(), trade.getSubmarketId(), quantity);
                }
            }
        }
        if (!adjusted && delta != 0) {
            addPending(result, itemKey, submarketId, delta);
        }
        return result;
    }

    private static void addPending(List<StockReviewPendingTrade> result,
                                   String itemKey,
                                   String submarketId,
                                   int quantity) {
        StockReviewPendingTrade trade = StockReviewPendingTrade.create(itemKey, submarketId, quantity);
        if (trade != null) {
            result.add(trade);
        }
    }

    static List<StockReviewPendingTrade> executionOrder(List<StockReviewPendingTrade> pendingTrades) {
        List<StockReviewPendingTrade> result = new ArrayList<StockReviewPendingTrade>();
        if (pendingTrades == null || pendingTrades.isEmpty()) {
            return result;
        }
        addMatching(result, pendingTrades, MATCH_SELL);
        addMatching(result, pendingTrades, MATCH_SELLER_BUY);
        addMatching(result, pendingTrades, MATCH_GENERIC_BUY);
        return result;
    }

    private static void addMatching(List<StockReviewPendingTrade> result,
                                    List<StockReviewPendingTrade> pendingTrades,
                                    int match) {
        for (int i = 0; i < pendingTrades.size(); i++) {
            StockReviewPendingTrade trade = pendingTrades.get(i);
            if (matches(trade, match)) {
                result.add(trade);
            }
        }
    }

    private static boolean matches(StockReviewPendingTrade trade, int match) {
        if (match == MATCH_SELL) {
            return trade.isSell();
        }
        if (match == MATCH_SELLER_BUY) {
            return trade.isBuy() && trade.getSubmarketId() != null;
        }
        return trade.isBuy() && trade.getSubmarketId() == null;
    }

    private static void addVisibleBuyableRecords(List<WeaponStockRecord> result, List<WeaponStockRecord> records) {
        if (records == null) {
            return;
        }
        for (int i = 0; i < records.size(); i++) {
            WeaponStockRecord record = records.get(i);
            if (record.getBuyableCount() > 0) {
                result.add(record);
            }
        }
    }

    private static void addVisibleTradeableRecords(List<WeaponStockRecord> result, List<WeaponStockRecord> records) {
        if (records == null) {
            return;
        }
        for (int i = 0; i < records.size(); i++) {
            WeaponStockRecord record = records.get(i);
            if (record.getBuyableCount() > 0 || record.getPlayerCargoCount() > 0) {
                result.add(record);
            }
        }
    }

    private static final class CheapestBuyRecordComparator implements Comparator<WeaponStockRecord> {
        private final StockReviewQuoteBook quoteBook;

        CheapestBuyRecordComparator(WeaponStockSnapshot snapshot) {
            this.quoteBook = new StockReviewQuoteBook(snapshot);
        }

        @Override
        public int compare(WeaponStockRecord left, WeaponStockRecord right) {
            int result = Integer.compare(quoteBook.cheapestUnitPrice(left), quoteBook.cheapestUnitPrice(right));
            if (result != 0) {
                return result;
            }
            return left.getDisplayName().compareToIgnoreCase(right.getDisplayName());
        }
    }

}
