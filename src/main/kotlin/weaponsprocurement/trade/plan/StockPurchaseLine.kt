package weaponsprocurement.trade.plan


class StockPurchaseLine(
    @JvmField val source: StockPurchaseSource,
    @JvmField val quantity: Int,
)