package weaponsprocurement.core;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;

final class StockSellTarget {
    final SubmarketAPI submarket;
    final CargoAPI cargo;
    final int unitPrice;

    StockSellTarget(SubmarketAPI submarket, CargoAPI cargo, int unitPrice) {
        this.submarket = submarket;
        this.cargo = cargo;
        this.unitPrice = unitPrice;
    }
}
