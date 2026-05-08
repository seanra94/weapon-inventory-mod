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

    static List<StockReviewPendingPurchase> withAdjustment(List<StockReviewPendingPurchase> pendingPurchases,
                                                           String itemKey,
                                                           String submarketId,
                                                           int delta) {
        List<StockReviewPendingPurchase> result = new ArrayList<StockReviewPendingPurchase>();
        boolean adjusted = false;
        if (pendingPurchases != null) {
            for (int i = 0; i < pendingPurchases.size(); i++) {
                StockReviewPendingPurchase purchase = pendingPurchases.get(i);
                int quantity = purchase.getQuantity();
                if (purchase.matches(itemKey, submarketId)) {
                    quantity += delta;
                    adjusted = true;
                }
                if (quantity != 0) {
                    result.add(new StockReviewPendingPurchase(purchase.getItemKey(), purchase.getSubmarketId(), quantity));
                }
            }
        }
        if (!adjusted && delta != 0) {
            result.add(new StockReviewPendingPurchase(itemKey, submarketId, delta));
        }
        return result;
    }

    static List<StockReviewPendingPurchase> executionOrder(List<StockReviewPendingPurchase> pendingPurchases) {
        List<StockReviewPendingPurchase> result = new ArrayList<StockReviewPendingPurchase>();
        if (pendingPurchases == null || pendingPurchases.isEmpty()) {
            return result;
        }
        addMatching(result, pendingPurchases, MATCH_SELL);
        addMatching(result, pendingPurchases, MATCH_SELLER_BUY);
        addMatching(result, pendingPurchases, MATCH_GENERIC_BUY);
        return result;
    }

    private static void addMatching(List<StockReviewPendingPurchase> result,
                                    List<StockReviewPendingPurchase> pendingPurchases,
                                    int match) {
        for (int i = 0; i < pendingPurchases.size(); i++) {
            StockReviewPendingPurchase purchase = pendingPurchases.get(i);
            if (matches(purchase, match)) {
                result.add(purchase);
            }
        }
    }

    private static boolean matches(StockReviewPendingPurchase purchase, int match) {
        if (match == MATCH_SELL) {
            return purchase.isSell();
        }
        if (match == MATCH_SELLER_BUY) {
            return purchase.isBuy() && purchase.getSubmarketId() != null;
        }
        return purchase.isBuy() && purchase.getSubmarketId() == null;
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
