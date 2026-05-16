package weaponsprocurement.trade

import weaponsprocurement.stock.item.*
import weaponsprocurement.stock.market.*
import weaponsprocurement.stock.fixer.*

class StockPurchaseLine(
    @JvmField val source: StockPurchaseSource,
    @JvmField val quantity: Int,
)