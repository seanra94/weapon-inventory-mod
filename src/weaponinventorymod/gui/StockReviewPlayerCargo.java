package weaponinventorymod.gui;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SubmarketPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import weaponinventorymod.core.MarketStockService;

import java.util.HashMap;
import java.util.Map;

final class StockReviewPlayerCargo {
    private StockReviewPlayerCargo() {
    }

    static float currentCredits() {
        CargoAPI cargo = WimGuiCampaignDialogHost.current().getPlayerCargo();
        return cargo == null ? 0f : cargo.getCredits().get();
    }

    static float currentCargoSpaceLeft() {
        CargoAPI cargo = WimGuiCampaignDialogHost.current().getPlayerCargo();
        return cargo == null ? 0f : cargo.getSpaceLeft();
    }

    static float currentCargoCapacity() {
        CargoAPI cargo = WimGuiCampaignDialogHost.current().getPlayerCargo();
        return cargo == null ? 0f : cargo.getMaxCapacity();
    }

    static Map<String, Integer> sellUnitPricesByWeapon(MarketAPI market,
                                                       boolean includeBlackMarket) {
        Map<String, Integer> result = new HashMap<String, Integer>();
        CargoAPI cargo = WimGuiCampaignDialogHost.current().getPlayerCargo();
        if (cargo == null || cargo.getStacksCopy() == null) {
            return result;
        }
        for (CargoStackAPI stack : cargo.getStacksCopy()) {
            if (!MarketStockService.isVisibleWeaponStack(stack)) {
                continue;
            }
            String weaponId = stack.getWeaponSpecIfWeapon().getWeaponId();
            int unitPrice = bestLocalSellUnitPrice(market, stack, includeBlackMarket);
            if (unitPrice < 0) {
                continue;
            }
            Integer current = result.get(weaponId);
            if (current == null || unitPrice > current.intValue()) {
                result.put(weaponId, Integer.valueOf(unitPrice));
            }
        }
        return result;
    }

    private static int bestLocalSellUnitPrice(MarketAPI market,
                                              CargoStackAPI stack,
                                              boolean includeBlackMarket) {
        if (market == null || market.getSubmarketsCopy() == null || stack == null) {
            return -1;
        }
        int best = -1;
        for (SubmarketAPI submarket : market.getSubmarketsCopy()) {
            if (submarket == null) {
                continue;
            }
            if (!MarketStockService.isTradeSubmarket(submarket, includeBlackMarket)) {
                continue;
            }
            SubmarketPlugin plugin = submarket.getPlugin();
            if (plugin != null && plugin.isIllegalOnSubmarket(stack, SubmarketPlugin.TransferAction.PLAYER_SELL)) {
                continue;
            }
            best = Math.max(best, MarketStockService.sellUnitPrice(submarket, stack));
        }
        return best;
    }
}
