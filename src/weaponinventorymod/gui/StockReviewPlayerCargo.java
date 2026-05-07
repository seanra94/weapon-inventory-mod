package weaponinventorymod.gui;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
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

    static Map<String, Integer> sellUnitPricesByWeapon() {
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
            int unitPrice = Math.max(0, Math.round(stack.getBaseValuePerUnit()));
            Integer current = result.get(weaponId);
            if (current == null || unitPrice > current.intValue()) {
                result.put(weaponId, Integer.valueOf(unitPrice));
            }
        }
        return result;
    }
}
