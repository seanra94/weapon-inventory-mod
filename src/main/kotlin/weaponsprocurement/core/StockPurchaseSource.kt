package weaponsprocurement.core

import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import java.util.Comparator

class StockPurchaseSource(
    @JvmField val market: MarketAPI?,
    @JvmField val submarket: SubmarketAPI?,
    @JvmField val cargo: CargoAPI?,
    @JvmField val available: Int,
    @JvmField val unitPrice: Int,
    @JvmField val unitCargoSpace: Float,
) {
    constructor(
        submarket: SubmarketAPI?,
        cargo: CargoAPI?,
        available: Int,
        unitPrice: Int,
        unitCargoSpace: Float,
    ) : this(null, submarket, cargo, available, unitPrice, unitCargoSpace)

    companion object {
        @JvmField
        val PRICE_ORDER: Comparator<StockPurchaseSource> = PurchaseSourcePriceComparator()
    }

    private class PurchaseSourcePriceComparator : Comparator<StockPurchaseSource> {
        override fun compare(left: StockPurchaseSource, right: StockPurchaseSource): Int {
            val result = left.unitPrice.compareTo(right.unitPrice)
            if (result != 0) return result
            return sourceName(left).compareTo(sourceName(right), ignoreCase = true)
        }

        private fun sourceName(source: StockPurchaseSource?): String {
            if (source == null || source.submarket == null) return ""
            return source.submarket.nameOneLine
        }
    }
}
