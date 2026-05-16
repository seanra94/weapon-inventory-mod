package weaponsprocurement.core

import java.util.ArrayList

class StockPurchasePlan private constructor(
    @JvmField val lines: List<StockPurchaseLine>,
    @JvmField val totalQuantity: Int,
    @JvmField val totalCost: Long,
    @JvmField val totalSpace: Float,
) {
    companion object {
        @JvmStatic
        fun build(sources: List<StockPurchaseSource>, requestedQuantity: Int): StockPurchasePlan {
            var remaining = requestedQuantity
            var totalQuantity = 0
            var totalCost = 0L
            var totalSpace = 0f
            val lines = ArrayList<StockPurchaseLine>()
            for (source in sources) {
                if (remaining <= 0) break
                val quantity = remaining.coerceAtMost(source.available)
                if (quantity <= 0) continue
                lines.add(StockPurchaseLine(source, quantity))
                remaining -= quantity
                totalQuantity += quantity
                totalCost = TradeMoney.safeAdd(totalCost, TradeMoney.lineTotal(source.unitPrice, quantity))
                totalSpace += source.unitCargoSpace * quantity
            }
            return StockPurchasePlan(lines, totalQuantity, totalCost, totalSpace)
        }
    }
}
