package weaponsprocurement.ui.stockreview.trade

import weaponsprocurement.ui.stockreview.actions.*
import weaponsprocurement.ui.stockreview.state.*
import weaponsprocurement.ui.stockreview.rows.*
import weaponsprocurement.ui.stockreview.tooltips.*
import weaponsprocurement.ui.stockreview.rendering.*
import weaponsprocurement.ui.stockreview.trade.*

import weaponsprocurement.ui.*

class StockReviewSellerAllocation(
    val submarketName: String?,
    private val submarketId: String?,
    val quantity: Int,
    val cost: Long,
) {
    fun isBlackMarket(): Boolean = submarketId?.lowercase()?.contains("black") == true
}