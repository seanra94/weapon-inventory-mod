package weaponsprocurement.trade.plan


import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI

class StockSellTarget(
    @JvmField val submarket: SubmarketAPI,
    @JvmField val cargo: CargoAPI,
    @JvmField val unitPrice: Int,
)