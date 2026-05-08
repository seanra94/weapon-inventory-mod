package weaponsprocurement.core;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;

import java.util.Comparator;

final class StockPurchaseSource {
    static final Comparator<StockPurchaseSource> PRICE_ORDER = PurchaseSourcePriceComparator.INSTANCE;

    final MarketAPI market;
    final SubmarketAPI submarket;
    final CargoAPI cargo;
    final int available;
    final int unitPrice;
    final float unitCargoSpace;

    StockPurchaseSource(SubmarketAPI submarket, CargoAPI cargo, int available, int unitPrice, float unitCargoSpace) {
        this(null, submarket, cargo, available, unitPrice, unitCargoSpace);
    }

    StockPurchaseSource(MarketAPI market,
                        SubmarketAPI submarket,
                        CargoAPI cargo,
                        int available,
                        int unitPrice,
                        float unitCargoSpace) {
        this.market = market;
        this.submarket = submarket;
        this.cargo = cargo;
        this.available = available;
        this.unitPrice = unitPrice;
        this.unitCargoSpace = unitCargoSpace;
    }

    private static final class PurchaseSourcePriceComparator implements Comparator<StockPurchaseSource> {
        static final PurchaseSourcePriceComparator INSTANCE = new PurchaseSourcePriceComparator();

        @Override
        public int compare(StockPurchaseSource left, StockPurchaseSource right) {
            int result = Integer.compare(left.unitPrice, right.unitPrice);
            if (result != 0) {
                return result;
            }
            return sourceName(left).compareToIgnoreCase(sourceName(right));
        }

        private static String sourceName(StockPurchaseSource source) {
            if (source == null || source.submarket == null) {
                return "";
            }
            return source.submarket.getNameOneLine();
        }
    }
}
