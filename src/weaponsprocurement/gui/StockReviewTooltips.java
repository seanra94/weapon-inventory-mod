package weaponsprocurement.gui;

import weaponsprocurement.core.StockCategory;
import weaponsprocurement.core.StockSortMode;
import weaponsprocurement.core.StockSourceMode;
import weaponsprocurement.core.WeaponStockRecord;
import weaponsprocurement.internal.WeaponsProcurementConfig;

final class StockReviewTooltips {
    static final String STORAGE = "Combined item stock across all storage locations and fleet cargo. Brackets show the queued trade delta.";
    static final String PRICE = "Cheapest eligible unit price for this item. The unit price may rise as cheaper stock is exhausted and purchases move to more expensive sources. Sale price is a fraction of this price and is higher when selling on the black market.";
    static final String PLAN = "Queued trade quantity and total value for this item.";

    private StockReviewTooltips() {
    }

    static String sort(StockSortMode mode) {
        if (StockSortMode.NAME.equals(mode)) {
            return "Cycle item sorting. Sorts by name first, then lowest stock, then price.";
        }
        if (StockSortMode.PRICE.equals(mode)) {
            return "Cycle item sorting. Sorts by price first, then lowest stock, then name.";
        }
        return "Cycle item sorting. Sorts by lowest stock first, then price, then name.";
    }

    static String source(StockSourceMode mode) {
        StockSourceMode resolved = mode == null ? StockSourceMode.LOCAL : mode;
        if (StockSourceMode.SECTOR.equals(resolved)) {
            return "Buy from the Sector Market. Includes weapons and fighter LPCs currently being sold by any market in the sector. Purchased items are removed from the appropriate market inventory. Selling while this source is active uses the current local legal buyer; black-market selling is disabled in remote source modes. Prices are "
                    + oneDecimal(WeaponsProcurementConfig.sectorMarketPriceMultiplier())
                    + "x normal due to the difficulty of procuring items from across the sector.";
        }
        if (StockSourceMode.FIXERS.equals(resolved)) {
            return "Buy from the Fixer's Market. Includes every weapon or fighter LPC that could normally appear for sale in the sector, excluding REDACTED items and similar special cases. Selling while this source is active uses the current local legal buyer; black-market selling is disabled in remote source modes. Prices are "
                    + oneDecimal(WeaponsProcurementConfig.fixersMarketPriceMultiplier())
                    + "x normal due to the difficulty of procuring items that may not be in stock anywhere.";
        }
        return "Buy from local markets. Includes weapons and fighter LPCs from the local open market, black market, and other eligible local markets, such as faction markets, if you meet their requirements. Purchased items are removed from the appropriate market inventory. Sold items are deposited into either the open market or black market, depending on whether black-market transactions are enabled.";
    }

    static String category(StockCategory category) {
        String summary = " Heading totals show item types in this section, queued selling units, and queued buying units. Buying and selling are counted separately and do not cancel each other out.";
        if (StockCategory.NO_STOCK.equals(category)) {
            return "Items with no owned stock that are buyable here or present in inventory." + summary;
        }
        if (StockCategory.INSUFFICIENT.equals(category)) {
            return "Items below their desired stock threshold." + summary;
        }
        if (StockCategory.SUFFICIENT.equals(category)) {
            return "Items already at or above their desired stock threshold." + summary;
        }
        return "Show or hide items in this stock category." + summary;
    }

    static String itemDataToggle(WeaponStockRecord record) {
        if (record != null && record.isWing()) {
            return "Toggle the wing data display.";
        }
        return "Toggle the weapon data display.";
    }

    static String decreasePlan(int quantity) {
        return "Decrease the queued trade quantity by " + Math.max(1, quantity) + ", selling if applicable.";
    }

    static String increasePlan(int quantity) {
        return "Increase the queued trade quantity by " + Math.max(1, quantity) + ", buying if applicable.";
    }

    static String sufficient(WeaponStockRecord record) {
        int threshold = record == null ? 0 : record.getDesiredCount();
        String item = record != null && record.isWing() ? "wing" : "weapon";
        String thresholdKind = record != null && record.isWing() ? "fighter LPC" : "weapon mount size";
        return "Adjust the queued trade quantity so that your stock of this " + item + " just meets the sufficiency threshold for this " + thresholdKind + " (" + threshold + ").";
    }

    static String resetPlan() {
        return "Reset the queued trade quantity to 0.";
    }

    static String tariffs() {
        return "Extra credits paid above base item value due to source markup. Tariffs are much higher on the Sector Market ("
                + oneDecimal(WeaponsProcurementConfig.sectorMarketPriceMultiplier())
                + "x) and Fixer's Market ("
                + oneDecimal(WeaponsProcurementConfig.fixersMarketPriceMultiplier())
                + "x).";
    }

    static String purchaseAllUntilSufficient() {
        return "Queue purchases from cheapest to most expensive until your stock reaches the sufficiency threshold, or until you run out of money, cargo space, or available items to buy.";
    }

    static String sellAllUntilSufficient() {
        return "Queue sales until you have sold as many items as possible without reducing stock below the sufficiency threshold.";
    }

    static String filterHeading(StockReviewFilterGroup group) {
        return "Show or hide " + group.getLabel().toLowerCase(java.util.Locale.US) + " filters.";
    }

    static String filter(StockReviewFilter filter, boolean active) {
        return (active ? "Remove" : "Apply") + " the " + filter.getLabel() + " filter.";
    }

    private static String oneDecimal(float value) {
        return String.format(java.util.Locale.US, "%.1f", value);
    }
}
