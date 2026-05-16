package weaponsprocurement.gui

import weaponsprocurement.stock.StockCategory
import weaponsprocurement.stock.StockSortMode
import weaponsprocurement.stock.StockSourceMode
import weaponsprocurement.stock.WeaponStockRecord
import weaponsprocurement.config.WeaponsProcurementConfig
import java.util.Locale

class StockReviewTooltips private constructor() {
    companion object {
        const val STORAGE = "Combined item stock across all storage locations and fleet cargo. Brackets show the queued trade delta."
        const val PRICE = "Cheapest eligible unit price for this item. The unit price may rise as cheaper stock is exhausted and purchases move to more expensive sources. Sale price is a fraction of this price and is higher when selling on the black market."
        const val PLAN = "Queued trade quantity and total value for this item."

        @JvmStatic
        fun sort(mode: StockSortMode?): String {
            if (StockSortMode.NAME == mode) {
                return "Cycle item sorting. Sorts by name first, then lowest stock, then price."
            }
            if (StockSortMode.PRICE == mode) {
                return "Cycle item sorting. Sorts by price first, then lowest stock, then name."
            }
            return "Cycle item sorting. Sorts by lowest stock first, then price, then name."
        }

        @JvmStatic
        fun source(mode: StockSourceMode?): String {
            val resolved = mode ?: StockSourceMode.LOCAL
            if (StockSourceMode.SECTOR == resolved) {
                return "Buy from the Sector Market. Includes weapons and fighter LPCs currently being sold by any market in the sector. Purchased items are removed from the appropriate market inventory. Selling while this source is active uses the current local legal buyer; black-market selling is disabled in remote source modes. Prices are " +
                    oneDecimal(WeaponsProcurementConfig.sectorMarketPriceMultiplier()) +
                    "x normal due to the difficulty of procuring items from across the sector."
            }
            if (StockSourceMode.FIXERS == resolved) {
                return "Buy from the Fixer's Market. Includes every weapon or fighter LPC that could normally appear for sale in the sector, excluding REDACTED items and similar special cases. Selling while this source is active uses the current local legal buyer; black-market selling is disabled in remote source modes. Prices are " +
                    oneDecimal(WeaponsProcurementConfig.fixersMarketPriceMultiplier()) +
                    "x normal due to the difficulty of procuring items that may not be in stock anywhere."
            }
            return "Buy from local markets. Includes weapons and fighter LPCs from the local open market, black market, and other eligible local markets, such as faction markets, if you meet their requirements. Purchased items are removed from the appropriate market inventory. Sold items are deposited into either the open market or black market, depending on whether black-market transactions are enabled."
        }

        @JvmStatic
        fun category(category: StockCategory?): String {
            val summary = " Heading totals show item types in this section, queued selling units, and queued buying units. Buying and selling are counted separately and do not cancel each other out."
            if (StockCategory.NO_STOCK == category) {
                return "Items with no owned stock that are buyable here or present in inventory.$summary"
            }
            if (StockCategory.INSUFFICIENT == category) {
                return "Items below their desired stock threshold.$summary"
            }
            if (StockCategory.SUFFICIENT == category) {
                return "Items already at or above their desired stock threshold.$summary"
            }
            return "Show or hide items in this stock category.$summary"
        }

        @JvmStatic
        fun itemDataToggle(record: WeaponStockRecord?): String =
            if (record != null && record.isWing()) "Toggle the wing data display." else "Toggle the weapon data display."

        @JvmStatic
        fun decreasePlan(quantity: Int): String = "Decrease the queued trade quantity by ${Math.max(1, quantity)}, selling if applicable."

        @JvmStatic
        fun increasePlan(quantity: Int): String = "Increase the queued trade quantity by ${Math.max(1, quantity)}, buying if applicable."

        @JvmStatic
        fun sufficient(record: WeaponStockRecord?): String {
            val threshold = record?.desiredCount ?: 0
            val item = if (record != null && record.isWing()) "wing" else "weapon"
            val thresholdKind = if (record != null && record.isWing()) "fighter LPC" else "weapon mount size"
            return "Adjust the queued trade quantity so that your stock of this $item just meets the sufficiency threshold for this $thresholdKind ($threshold)."
        }

        @JvmStatic
        fun resetPlan(): String = "Reset the queued trade quantity to 0."

        @JvmStatic
        fun tariffs(): String =
            "Extra credits paid above base item value due to source markup. Tariffs are much higher on the Sector Market (" +
                oneDecimal(WeaponsProcurementConfig.sectorMarketPriceMultiplier()) +
                "x) and Fixer's Market (" +
                oneDecimal(WeaponsProcurementConfig.fixersMarketPriceMultiplier()) +
                "x)."

        @JvmStatic
        fun purchaseAllUntilSufficient(): String =
            "Queue purchases from cheapest to most expensive until your stock reaches the sufficiency threshold, or until you run out of money, cargo space, or available items to buy."

        @JvmStatic
        fun sellAllUntilSufficient(): String =
            "Queue sales until you have sold as many items as possible without reducing stock below the sufficiency threshold."

        @JvmStatic
        fun filterHeading(group: StockReviewFilterGroup): String =
            "Show or hide ${group.label.lowercase(Locale.US)} filters."

        @JvmStatic
        fun filter(filter: StockReviewFilter, active: Boolean): String =
            (if (active) "Remove" else "Apply") + " the " + filter.label + " filter."

        private fun oneDecimal(value: Float): String = String.format(Locale.US, "%.1f", value)
    }
}
