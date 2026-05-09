package weaponsprocurement.core;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import com.fs.starfarer.api.campaign.SubmarketPlugin;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction.LineItemType;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import org.apache.log4j.Logger;

final class StockMarketTransactionReporter {
    private StockMarketTransactionReporter() {
    }

    static void reportItemTransaction(Logger log,
                                      MarketAPI market,
                                      SubmarketAPI submarket,
                                      StockItemType itemType,
                                      String itemId,
                                      int quantity,
                                      int unitPrice,
                                      boolean bought) {
        if (market == null || submarket == null || itemId == null || itemId.isEmpty() || quantity <= 0) {
            return;
        }
        SubmarketPlugin plugin = submarket.getPlugin();
        if (plugin == null) {
            return;
        }
        long creditValue = TradeMoney.lineTotal(unitPrice, quantity);
        if (!TradeMoney.canExecuteCreditMutation(creditValue)) {
            log.warn("WP_STOCK_REVIEW skipped transaction report with oversized credit value item="
                    + itemId + " quantity=" + quantity + " unitPrice=" + unitPrice);
            return;
        }
        try {
            PlayerMarketTransaction transaction = new PlayerMarketTransaction(market, submarket, tradeMode(submarket));
            CargoAPI cargo = Global.getFactory() == null ? null : Global.getFactory().createCargo(false);
            if (cargo != null) {
                StockItemCargo.addItem(cargo, itemType, itemId, quantity);
                cargo.sort();
                if (bought) {
                    transaction.setBought(cargo);
                } else {
                    transaction.setSold(cargo);
                }
            }
            LineItemType lineType = bought ? LineItemType.BOUGHT : LineItemType.SOLD;
            transaction.getLineItems().add(new PlayerMarketTransaction.TransactionLineItem(
                    itemId,
                    lineType,
                    StockItemType.WING.equals(itemType) ? CargoItemType.FIGHTER_CHIP : CargoItemType.WEAPONS,
                    submarket,
                    quantity,
                    unitPrice,
                    unitPrice,
                    timestamp()));
            transaction.setCreditValue((int) (bought ? creditValue : -creditValue));
            plugin.reportPlayerMarketTransaction(transaction);
        } catch (Throwable t) {
            // Transaction callbacks are best-effort; cargo mutation has already succeeded.
            log.warn("WP_STOCK_REVIEW transaction report failed for " + itemId
                    + " at " + submarket.getSpecId(), t);
        }
    }

    private static CampaignUIAPI.CoreUITradeMode tradeMode(SubmarketAPI submarket) {
        SubmarketPlugin plugin = submarket == null ? null : submarket.getPlugin();
        return plugin != null && plugin.isBlackMarket()
                ? CampaignUIAPI.CoreUITradeMode.SNEAK
                : CampaignUIAPI.CoreUITradeMode.OPEN;
    }

    private static long timestamp() {
        return Global.getSector() == null || Global.getSector().getClock() == null
                ? 0L
                : Global.getSector().getClock().getTimestamp();
    }
}
