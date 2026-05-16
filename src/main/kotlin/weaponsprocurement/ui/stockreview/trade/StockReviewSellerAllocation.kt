package weaponsprocurement.ui.stockreview.trade



class StockReviewSellerAllocation(
    val submarketName: String?,
    private val submarketId: String?,
    val quantity: Int,
    val cost: Long,
) {
    fun isBlackMarket(): Boolean = submarketId?.lowercase()?.contains("black") == true
}