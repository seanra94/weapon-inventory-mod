package weaponsprocurement.stock

class SubmarketWeaponStock(
    val marketId: String?,
    val marketName: String?,
    val submarketId: String?,
    val submarketName: String?,
    val count: Int,
    val unitPrice: Int,
    val baseUnitPrice: Int,
    val unitCargoSpace: Float,
    private val purchasable: Boolean,
) {
    constructor(
        submarketId: String?,
        submarketName: String?,
        count: Int,
        unitPrice: Int,
        baseUnitPrice: Int,
        unitCargoSpace: Float,
        purchasable: Boolean,
    ) : this(null, null, submarketId, submarketName, count, unitPrice, baseUnitPrice, unitCargoSpace, purchasable)

    constructor(
        submarketId: String?,
        submarketName: String?,
        count: Int,
        unitPrice: Int,
        unitCargoSpace: Float,
        purchasable: Boolean,
    ) : this(null, null, submarketId, submarketName, count, unitPrice, unitPrice, unitCargoSpace, purchasable)

    constructor(
        marketId: String?,
        marketName: String?,
        submarketId: String?,
        submarketName: String?,
        count: Int,
        unitPrice: Int,
        unitCargoSpace: Float,
        purchasable: Boolean,
    ) : this(marketId, marketName, submarketId, submarketName, count, unitPrice, unitPrice, unitCargoSpace, purchasable)

    val sourceId: String?
        get() = if (marketId.isNullOrEmpty()) {
            submarketId
        } else {
            "$marketId|$submarketId"
        }

    val displaySourceName: String?
        get() = if (marketName.isNullOrEmpty()) {
            submarketName
        } else {
            "$marketName / $submarketName"
        }

    fun isPurchasable(): Boolean = purchasable
}
