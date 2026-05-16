package weaponsprocurement.gui

class StockReviewPendingTrade private constructor(
    val itemKey: String,
    val submarketId: String?,
    var quantity: Int,
) {
    fun addQuantity(amount: Int) {
        quantity += amount
    }

    fun isBuy(): Boolean = quantity > 0

    fun isSell(): Boolean = quantity < 0

    fun isZero(): Boolean = quantity == 0

    fun matches(otherItemKey: String?, otherSubmarketId: String?): Boolean {
        if (itemKey != otherItemKey) return false
        return submarketId == otherSubmarketId
    }

    fun copy(): StockReviewPendingTrade? = create(itemKey, submarketId, quantity)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StockReviewPendingTrade) return false
        return quantity == other.quantity && matches(other.itemKey, other.submarketId)
    }

    override fun hashCode(): Int {
        var result = itemKey.hashCode()
        result = 31 * result + (submarketId?.hashCode() ?: 0)
        result = 31 * result + quantity
        return result
    }

    companion object {
        @JvmStatic
        fun create(itemKey: String?, submarketId: String?, quantity: Int): StockReviewPendingTrade? {
            return if (itemKey.isNullOrEmpty() || quantity == 0) {
                null
            } else {
                StockReviewPendingTrade(itemKey, submarketId, quantity)
            }
        }
    }
}
