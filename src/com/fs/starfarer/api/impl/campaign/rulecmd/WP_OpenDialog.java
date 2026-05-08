package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc;
import weaponsprocurement.gui.StockReviewHotkeyScript;
import weaponsprocurement.internal.WeaponsProcurementConfig;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WP_OpenDialog extends BaseCommandPlugin {
    private static final String MODE_CAN_SHOW = "canshow";
    private static final String MODE_OPEN = "open";

    @Override
    public boolean execute(String ruleId,
                           InteractionDialogAPI dialog,
                           List<Misc.Token> params,
                           Map<String, MemoryAPI> memoryMap) {
        String mode = params == null || params.isEmpty()
                ? MODE_OPEN
                : params.get(0).getString(memoryMap).toLowerCase(Locale.US);

        WeaponsProcurementConfig.refreshAndPublishSettings();
        if (MODE_CAN_SHOW.equals(mode)) {
            return WeaponsProcurementConfig.isDialogueOptionEnabled()
                    && dialog != null
                    && StockReviewHotkeyScript.canOpenAtCurrentMarket(currentMarket());
        }
        if (MODE_OPEN.equals(mode)) {
            StockReviewHotkeyScript.openFromCurrentDialog("dialog-option");
            return true;
        }
        return false;
    }

    private static MarketAPI currentMarket() {
        return Global.getSector() == null ? null : Global.getSector().getCurrentlyOpenMarket();
    }
}
