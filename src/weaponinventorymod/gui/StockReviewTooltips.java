package weaponinventorymod.gui;

import weaponinventorymod.core.StockCategory;
import weaponinventorymod.core.WeaponStockRecord;

final class StockReviewTooltips {
    static final String STORAGE = "Total owned stock under the current storage policy. Brackets show the queued trade delta.";
    static final String PRICE = "Cheapest eligible unit price for this weapon. Sell-only rows use the best current sell value.";
    static final String PLAN = "Queued trade quantity and total value for this weapon.";
    static final String WEAPON_DATA = "Show or hide weapon size, type, damage, range, and flux data.";

    private StockReviewTooltips() {
    }

    static String category(StockCategory category) {
        if (StockCategory.NO_STOCK.equals(category)) {
            return "Weapons with no owned stock that are buyable here or present in inventory.";
        }
        if (StockCategory.INSUFFICIENT.equals(category)) {
            return "Weapons below their desired stock threshold.";
        }
        if (StockCategory.SUFFICIENT.equals(category)) {
            return "Weapons already at or above their desired stock threshold.";
        }
        return "Show or hide weapons in this stock category.";
    }

    static String weapon(WeaponStockRecord record) {
        if (record == null) {
            return "Show or hide details for this weapon.";
        }
        return "Show or hide " + record.getDisplayName() + " details and trade controls.";
    }

    static String filterHeading(StockReviewFilterGroup group) {
        return "Show or hide " + group.getLabel().toLowerCase(java.util.Locale.US) + " filters.";
    }

    static String filter(StockReviewFilter filter, boolean active) {
        return (active ? "Remove" : "Apply") + " the " + filter.getLabel() + " filter.";
    }
}
