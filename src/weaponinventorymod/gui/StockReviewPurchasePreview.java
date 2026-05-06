package weaponinventorymod.gui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import weaponinventorymod.core.MarketStockService;
import weaponinventorymod.core.SubmarketWeaponStock;
import weaponinventorymod.core.WeaponStockRecord;
import weaponinventorymod.core.WeaponStockSnapshot;

import java.util.ArrayList;
import java.util.List;

final class StockReviewPurchasePreview {
    static final int PRICE_UNAVAILABLE = Integer.MIN_VALUE;

    private StockReviewPurchasePreview() {
    }

    static int totalCost(WeaponStockSnapshot snapshot, List<StockReviewPendingPurchase> pendingPurchases) {
        int total = 0;
        if (pendingPurchases == null) {
            return total;
        }
        for (int i = 0; i < pendingPurchases.size(); i++) {
            int cost = quoteCost(snapshot, pendingPurchases.get(i));
            if (cost == PRICE_UNAVAILABLE) {
                return PRICE_UNAVAILABLE;
            }
            total += cost;
        }
        return total;
    }

    static int quoteCost(WeaponStockSnapshot snapshot, StockReviewPendingPurchase purchase) {
        if (purchase.isSell()) {
            int unitPrice = playerSellUnitPrice(purchase.getWeaponId());
            return unitPrice < 0 ? PRICE_UNAVAILABLE : purchase.getQuantity() * unitPrice;
        }
        WeaponStockRecord record = findRecord(snapshot, purchase.getWeaponId());
        if (record == null) {
            return PRICE_UNAVAILABLE;
        }
        int remaining = purchase.getQuantity();
        int total = 0;
        List<SubmarketWeaponStock> stocks = new ArrayList<SubmarketWeaponStock>(record.getSubmarketStocks());
        sortByPrice(stocks);
        for (int i = 0; i < stocks.size() && remaining > 0; i++) {
            SubmarketWeaponStock stock = stocks.get(i);
            if (!stock.isPurchasable() || stock.getCount() <= 0) {
                continue;
            }
            if (purchase.getSubmarketId() != null && !purchase.getSubmarketId().equals(stock.getSubmarketId())) {
                continue;
            }
            int quantity = Math.min(remaining, stock.getCount());
            total += quantity * stock.getUnitPrice();
            remaining -= quantity;
        }
        return remaining > 0 ? PRICE_UNAVAILABLE : total;
    }

    static String displayName(WeaponStockSnapshot snapshot, String weaponId) {
        WeaponStockRecord record = findRecord(snapshot, weaponId);
        return record == null ? weaponId : record.getDisplayName();
    }

    static String sourceSuffix(WeaponStockSnapshot snapshot, StockReviewPendingPurchase purchase) {
        if (purchase.getSubmarketId() == null) {
            return "";
        }
        WeaponStockRecord record = findRecord(snapshot, purchase.getWeaponId());
        if (record == null) {
            return "";
        }
        for (int i = 0; i < record.getSubmarketStocks().size(); i++) {
            SubmarketWeaponStock stock = record.getSubmarketStocks().get(i);
            if (purchase.getSubmarketId().equals(stock.getSubmarketId())) {
                return " from " + stock.getSubmarketName();
            }
        }
        return "";
    }

    static WeaponStockRecord findRecord(WeaponStockSnapshot snapshot, String weaponId) {
        if (snapshot == null || weaponId == null) {
            return null;
        }
        for (int i = 0; i < snapshot.getAllRecords().size(); i++) {
            WeaponStockRecord record = snapshot.getAllRecords().get(i);
            if (weaponId.equals(record.getWeaponId())) {
                return record;
            }
        }
        return null;
    }

    static float currentCredits() {
        try {
            return Global.getSector().getPlayerFleet().getCargo().getCredits().get();
        } catch (Throwable ignored) {
            return 0f;
        }
    }

    private static int playerSellUnitPrice(String weaponId) {
        CargoAPI cargo;
        try {
            cargo = Global.getSector().getPlayerFleet().getCargo();
        } catch (Throwable ignored) {
            return -1;
        }
        if (cargo == null || cargo.getStacksCopy() == null) {
            return -1;
        }
        for (CargoStackAPI stack : cargo.getStacksCopy()) {
            if (MarketStockService.isVisibleWeaponStack(stack) && weaponId.equals(stack.getWeaponSpecIfWeapon().getWeaponId())) {
                return Math.max(0, Math.round(stack.getBaseValuePerUnit()));
            }
        }
        return -1;
    }

    private static void sortByPrice(List<SubmarketWeaponStock> stocks) {
        for (int i = 0; i < stocks.size(); i++) {
            for (int j = i + 1; j < stocks.size(); j++) {
                if (stocks.get(j).getUnitPrice() < stocks.get(i).getUnitPrice()) {
                    SubmarketWeaponStock temp = stocks.get(i);
                    stocks.set(i, stocks.get(j));
                    stocks.set(j, temp);
                }
            }
        }
    }
}
