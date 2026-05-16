package weaponsprocurement.trade

import weaponsprocurement.stock.*

class StockPurchaseLine(
    @JvmField val source: StockPurchaseSource,
    @JvmField val quantity: Int,
)
