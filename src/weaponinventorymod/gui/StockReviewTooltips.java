package weaponinventorymod.gui;

import weaponinventorymod.core.StockCategory;
import weaponinventorymod.core.StockSortMode;
import weaponinventorymod.core.StockSourceMode;
import weaponinventorymod.core.WeaponStockRecord;
import weaponinventorymod.internal.WeaponInventoryConfig;

final class StockReviewTooltips {
    static final String STORAGE = "Combined weapon stock across all storage locations and fleet cargo. Brackets show the queued trade delta.";
    static final String PRICE = "Cheapest eligible unit price for this weapon. The unit price may rise as cheaper stock is exhausted and purchases move to more expensive sources. Sale price is a fraction of this price and is higher when selling on the black market.";
    static final String PLAN = "Queued trade quantity and total value for this weapon.";

    private StockReviewTooltips() {
    }

    static String sort(StockSortMode mode) {
        if (StockSortMode.NAME.equals(mode)) {
            return "Cycle weapon sorting. Sorts by name first, then lowest stock, then price.";
        }
        if (StockSortMode.PRICE.equals(mode)) {
            return "Cycle weapon sorting. Sorts by price first, then lowest stock, then name.";
        }
        return "Cycle weapon sorting. Sorts by lowest stock first, then price, then name.";
    }

    static String source(StockSourceMode mode) {
        StockSourceMode resolved = mode == null ? StockSourceMode.LOCAL : mode;
        if (StockSourceMode.SECTOR.equals(resolved)) {
            return "Buy from the Sector Market. Includes weapons currently being sold by any market in the sector. Purchased weapons are removed from the appropriate market inventory. Sold weapons are deposited into either the local open market or black market, depending on whether black-market transactions are enabled. Prices are "
                    + oneDecimal(WeaponInventoryConfig.sectorMarketPriceMultiplier())
                    + "x normal due to the difficulty of procuring weapons from across the sector.";
        }
        if (StockSourceMode.FIXERS.equals(resolved)) {
            return "Buy from the Fixer Market. Includes every weapon that could normally appear for sale in the sector, excluding REDACTED weapons and similar special cases. Sold weapons are deposited into either the local open market or black market, depending on whether black-market transactions are enabled. Prices are "
                    + oneDecimal(WeaponInventoryConfig.fixersMarketPriceMultiplier())
                    + "x normal due to the difficulty of procuring weapons that may not be in stock anywhere.";
        }
        return "Buy from local markets. Includes weapons from the local open market, black market, and other eligible local markets, such as faction markets, if you meet their requirements. Purchased weapons are removed from the appropriate market inventory. Sold weapons are deposited into either the open market or black market, depending on whether black-market transactions are enabled.";
    }

    static String category(StockCategory category) {
        String summary = " Heading totals show weapon types in this section, queued selling units, and queued buying units. Buying and selling are counted separately and do not cancel each other out.";
        if (StockCategory.NO_STOCK.equals(category)) {
            return "Weapons with no owned stock that are buyable here or present in inventory." + summary;
        }
        if (StockCategory.INSUFFICIENT.equals(category)) {
            return "Weapons below their desired stock threshold." + summary;
        }
        if (StockCategory.SUFFICIENT.equals(category)) {
            return "Weapons already at or above their desired stock threshold." + summary;
        }
        return "Show or hide weapons in this stock category." + summary;
    }

    static String weapon(WeaponStockRecord record) {
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
        return "Adjust the queued trade quantity so that your stock of this weapon just meets the sufficiency threshold for this weapon mount size (" + threshold + ").";
    }

    static String resetPlan() {
        return "Reset the queued trade quantity to 0.";
    }

    static String tariffs() {
        return "Extra credits paid above base weapon value due to source markup. Tariffs are much higher on the Sector Market ("
                + oneDecimal(WeaponInventoryConfig.sectorMarketPriceMultiplier())
                + "x) and Fixer Market ("
                + oneDecimal(WeaponInventoryConfig.fixersMarketPriceMultiplier())
                + "x).";
    }

    static String purchaseAllUntilSufficient() {
        return "Queue purchases from cheapest to most expensive until your stock reaches the sufficiency threshold, or until you run out of money, cargo space, or available weapons to buy.";
    }

    static String sellAllUntilSufficient() {
        return "Queue sales until you have sold as many weapons as possible without reducing stock below the sufficiency threshold.";
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
