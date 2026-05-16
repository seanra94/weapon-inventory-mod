package weaponsprocurement.ui.stockreview.trade

import weaponsprocurement.ui.stockreview.actions.*
import weaponsprocurement.ui.stockreview.state.*
import weaponsprocurement.ui.stockreview.rows.*
import weaponsprocurement.ui.stockreview.tooltips.*
import weaponsprocurement.ui.stockreview.rendering.*
import weaponsprocurement.ui.stockreview.trade.*

import weaponsprocurement.ui.*

enum class StockReviewTradeGroup(val label: String) {
    BUYING("Buying"),
    SELLING("Selling"),
}