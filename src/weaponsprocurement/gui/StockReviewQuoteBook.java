package weaponsprocurement.gui;

import weaponsprocurement.core.SubmarketWeaponStock;
import weaponsprocurement.core.WeaponStockRecord;
import weaponsprocurement.core.WeaponStockSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

final class StockReviewQuoteBook {
    static final int PRICE_UNAVAILABLE = Integer.MIN_VALUE;

    private final WeaponStockSnapshot snapshot;
    private final Map<String, List<SubmarketWeaponStock>> sortedBuyStocksByItem = new HashMap<String, List<SubmarketWeaponStock>>();
    private final Map<String, Integer> sellUnitPriceByItem = new HashMap<String, Integer>();
    private final Map<String, Float> unitCargoSpaceByItem = new HashMap<String, Float>();
    private final Map<String, StockReviewQuote> quotesByLine = new HashMap<String, StockReviewQuote>();
    private Map<String, Integer> playerSellUnitPrices;

    StockReviewQuoteBook(WeaponStockSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    List<StockReviewSellerAllocation> sellerAllocations(StockReviewPendingPurchase purchase) {
        return quote(purchase).getSellerAllocations();
    }

    StockReviewPortfolioQuote quotePortfolio(List<StockReviewPendingPurchase> pendingPurchases) {
        StockReviewPortfolioQuote result = new StockReviewPortfolioQuote();
        if (pendingPurchases == null || pendingPurchases.isEmpty()) {
            return result;
        }
        Map<String, Integer> remainingBySource = new HashMap<String, Integer>();
        List<StockReviewPendingPurchase> ordered = StockReviewTradePlanner.executionOrder(pendingPurchases);
        for (int i = 0; i < ordered.size(); i++) {
            StockReviewPendingPurchase purchase = ordered.get(i);
            StockReviewQuote quote = purchase.isBuy()
                    ? quoteBuyWithRemaining(purchase, remainingBySource)
                    : quote(purchase);
            result.addLine(purchase, quote);
        }
        return result;
    }

    int cheapestUnitPrice(WeaponStockRecord record) {
        List<SubmarketWeaponStock> stocks = sortedBuyStocks(record);
        if (stocks.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        return stocks.get(0).getUnitPrice();
    }

    int nextBuyUnitPriceAfterPlannedBuys(WeaponStockRecord record, int plannedBuyQuantity) {
        int planned = Math.max(0, plannedBuyQuantity);
        List<SubmarketWeaponStock> stocks = sortedBuyStocks(record);
        for (int i = 0; i < stocks.size(); i++) {
            SubmarketWeaponStock stock = stocks.get(i);
            if (planned >= stock.getCount()) {
                planned -= stock.getCount();
                continue;
            }
            return stock.getUnitPrice();
        }
        return Integer.MAX_VALUE;
    }

    private StockReviewQuote quote(StockReviewPendingPurchase purchase) {
        if (purchase == null || purchase.isZero()) {
            return StockReviewQuote.ZERO;
        }
        String key = lineKey(purchase);
        StockReviewQuote cached = quotesByLine.get(key);
        if (cached != null) {
            return cached;
        }
        StockReviewQuote result = purchase.isSell() ? quoteSell(purchase) : quoteBuy(purchase);
        quotesByLine.put(key, result);
        return result;
    }

    private StockReviewQuote quoteSell(StockReviewPendingPurchase purchase) {
        int unitPrice = sellUnitPrice(purchase.getItemKey());
        if (unitPrice < 0) {
            return StockReviewQuote.priceUnavailable();
        }
        float cargo = purchase.getQuantity() * fallbackUnitCargoSpace(purchase.getItemKey());
        return new StockReviewQuote(purchase.getQuantity() * unitPrice, cargo,
                Collections.<StockReviewSellerAllocation>emptyList());
    }

    private StockReviewQuote quoteBuy(StockReviewPendingPurchase purchase) {
        return quoteBuyWithRemaining(purchase, null);
    }

    private StockReviewQuote quoteBuyWithRemaining(StockReviewPendingPurchase purchase,
                                                   Map<String, Integer> remainingBySource) {
        int remaining = purchase.getQuantity();
        int totalCost = 0;
        int totalBaseCost = 0;
        int totalQuantity = 0;
        float totalCargo = 0f;
        List<StockReviewSellerAllocation> allocations =
                new ArrayList<StockReviewSellerAllocation>();
        List<SubmarketWeaponStock> stocks = sortedBuyStocks(purchase.getItemKey());
        for (int i = 0; i < stocks.size() && remaining > 0; i++) {
            SubmarketWeaponStock stock = stocks.get(i);
            if (purchase.getSubmarketId() != null && !matchesSource(purchase.getSubmarketId(), stock)) {
                continue;
            }
            int available = remainingBySource == null ? stock.getCount() : remainingStock(purchase.getItemKey(), stock, remainingBySource);
            int quantity = Math.min(remaining, available);
            if (quantity <= 0) {
                continue;
            }
            int cost = quantity * stock.getUnitPrice();
            totalCost += cost;
            totalBaseCost += quantity * stock.getBaseUnitPrice();
            totalQuantity += quantity;
            totalCargo += quantity * stock.getUnitCargoSpace();
            allocations.add(new StockReviewSellerAllocation(stock.getDisplaySourceName(),
                    stock.getSourceId(), quantity, cost));
            remaining -= quantity;
            if (remainingBySource != null) {
                remainingBySource.put(sourceKey(purchase.getItemKey(), stock), Integer.valueOf(available - quantity));
            }
        }
        if (remaining > 0) {
            return StockReviewQuote.priceUnavailable(totalCargo, allocations);
        }
        return new StockReviewQuote(totalCost, totalCargo, totalBaseCost, totalQuantity, allocations);
    }

    private int remainingStock(String itemKey, SubmarketWeaponStock stock, Map<String, Integer> remainingBySource) {
        String key = sourceKey(itemKey, stock);
        Integer cached = remainingBySource.get(key);
        if (cached != null) {
            return cached.intValue();
        }
        remainingBySource.put(key, Integer.valueOf(stock.getCount()));
        return stock.getCount();
    }

    private List<SubmarketWeaponStock> sortedBuyStocks(String itemKey) {
        if (itemKey == null) {
            return Collections.emptyList();
        }
        List<SubmarketWeaponStock> cached = sortedBuyStocksByItem.get(itemKey);
        if (cached != null) {
            return cached;
        }
        WeaponStockRecord record = findRecord(itemKey);
        if (record == null) {
            sortedBuyStocksByItem.put(itemKey, Collections.<SubmarketWeaponStock>emptyList());
            return Collections.emptyList();
        }
        List<SubmarketWeaponStock> result = new ArrayList<SubmarketWeaponStock>();
        for (int i = 0; i < record.getSubmarketStocks().size(); i++) {
            SubmarketWeaponStock stock = record.getSubmarketStocks().get(i);
            if (stock.isPurchasable() && stock.getCount() > 0) {
                result.add(stock);
            }
        }
        sortByPrice(result);
        result = Collections.unmodifiableList(result);
        sortedBuyStocksByItem.put(itemKey, result);
        return result;
    }

    private List<SubmarketWeaponStock> sortedBuyStocks(WeaponStockRecord record) {
        if (record == null) {
            return Collections.emptyList();
        }
        String itemKey = record.getItemKey();
        List<SubmarketWeaponStock> cached = sortedBuyStocksByItem.get(itemKey);
        if (cached != null) {
            return cached;
        }
        List<SubmarketWeaponStock> result = new ArrayList<SubmarketWeaponStock>();
        for (int i = 0; i < record.getSubmarketStocks().size(); i++) {
            SubmarketWeaponStock stock = record.getSubmarketStocks().get(i);
            if (stock.isPurchasable() && stock.getCount() > 0) {
                result.add(stock);
            }
        }
        sortByPrice(result);
        result = Collections.unmodifiableList(result);
        sortedBuyStocksByItem.put(itemKey, result);
        return result;
    }

    private float fallbackUnitCargoSpace(String itemKey) {
        if (itemKey == null) {
            return 1f;
        }
        Float cached = unitCargoSpaceByItem.get(itemKey);
        if (cached != null) {
            return cached.floatValue();
        }
        float result = 1f;
        WeaponStockRecord record = findRecord(itemKey);
        if (record != null) {
            for (int i = 0; i < record.getSubmarketStocks().size(); i++) {
                SubmarketWeaponStock stock = record.getSubmarketStocks().get(i);
                if (stock.getUnitCargoSpace() > 0f) {
                    result = stock.getUnitCargoSpace();
                    break;
                }
            }
        }
        unitCargoSpaceByItem.put(itemKey, Float.valueOf(result));
        return result;
    }

    int sellUnitPrice(String itemKey) {
        if (itemKey == null) {
            return -1;
        }
        Integer cached = sellUnitPriceByItem.get(itemKey);
        if (cached != null) {
            return cached.intValue();
        }
        if (playerSellUnitPrices == null) {
            playerSellUnitPrices = StockReviewPlayerCargo.sellUnitPricesByItem(
                    snapshot == null ? null : snapshot.getMarket(),
                    snapshot != null && snapshot.isIncludeBlackMarket());
        }
        Integer price = playerSellUnitPrices.get(itemKey);
        int result = price == null ? -1 : price.intValue();
        sellUnitPriceByItem.put(itemKey, Integer.valueOf(result));
        return result;
    }

    private WeaponStockRecord findRecord(String itemKey) {
        if (snapshot == null || itemKey == null) {
            return null;
        }
        return snapshot.getRecord(itemKey);
    }

    private static void sortByPrice(List<SubmarketWeaponStock> stocks) {
        for (int i = 0; i < stocks.size(); i++) {
            for (int j = i + 1; j < stocks.size(); j++) {
                if (compareSourceOrder(stocks.get(j), stocks.get(i)) < 0) {
                    SubmarketWeaponStock temp = stocks.get(i);
                    stocks.set(i, stocks.get(j));
                    stocks.set(j, temp);
                }
            }
        }
    }

    private static int compareSourceOrder(SubmarketWeaponStock left, SubmarketWeaponStock right) {
        int result = Integer.compare(left.getUnitPrice(), right.getUnitPrice());
        if (result != 0) {
            return result;
        }
        return left.getDisplaySourceName().compareToIgnoreCase(right.getDisplaySourceName());
    }

    private static String lineKey(StockReviewPendingPurchase purchase) {
        return (purchase.getItemKey() == null ? "" : purchase.getItemKey())
                + "|" + (purchase.getSubmarketId() == null ? "" : purchase.getSubmarketId())
                + "|" + purchase.getQuantity();
    }

    private static String sourceKey(String itemKey, SubmarketWeaponStock stock) {
        return (itemKey == null ? "" : itemKey)
                + "|" + (stock == null || stock.getSourceId() == null ? "" : stock.getSourceId());
    }

    private static boolean matchesSource(String requestedSourceId, SubmarketWeaponStock stock) {
        if (stock == null || requestedSourceId == null) {
            return false;
        }
        return requestedSourceId.equals(stock.getSourceId()) || requestedSourceId.equals(stock.getSubmarketId());
    }

}
