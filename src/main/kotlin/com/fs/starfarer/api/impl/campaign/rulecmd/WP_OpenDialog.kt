package com.fs.starfarer.api.impl.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.util.Misc
import weaponsprocurement.lifecycle.StockReviewHotkeyScript
import weaponsprocurement.config.WeaponsProcurementConfig
import java.util.Locale

class WP_OpenDialog : BaseCommandPlugin() {
    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: List<Misc.Token>?,
        memoryMap: Map<String, MemoryAPI>?,
    ): Boolean {
        val rawMode = if (params.isNullOrEmpty()) {
            MODE_OPEN
        } else {
            params[0].getString(memoryMap)
        }
        val mode = rawMode?.lowercase(Locale.US) ?: MODE_OPEN

        WeaponsProcurementConfig.refreshAndPublishSettings()
        if (MODE_CAN_SHOW == mode) {
            return WeaponsProcurementConfig.isDialogueOptionEnabled() &&
                dialog != null &&
                StockReviewHotkeyScript.canOpenAtCurrentMarket(currentMarket())
        }
        if (MODE_OPEN == mode) {
            StockReviewHotkeyScript.openFromCurrentDialog("dialog-option")
            return true
        }
        return false
    }

    companion object {
        private const val MODE_CAN_SHOW = "canshow"
        private const val MODE_OPEN = "open"

        private fun currentMarket(): MarketAPI? {
            val sector = Global.getSector() ?: return null
            return sector.currentlyOpenMarket
        }
    }
}
