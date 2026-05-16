package weaponsprocurement.gui

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.CampaignUIAPI
import com.fs.starfarer.api.campaign.CoreUITabId
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.SectorAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import org.apache.log4j.Logger

class WimGuiCampaignDialogHost private constructor(
    private val sector: SectorAPI?,
    private val ui: CampaignUIAPI?,
    private val dialog: InteractionDialogAPI?,
) {
    fun hasDialog(): Boolean = dialog != null

    fun getSector(): SectorAPI? = sector

    fun getDialog(): InteractionDialogAPI? = dialog

    fun getCurrentMarket(): MarketAPI? = sector?.currentlyOpenMarket

    fun getCurrentMarketOr(fallback: MarketAPI?): MarketAPI? = getCurrentMarket() ?: fallback

    fun getPlayerCargo(): CargoAPI? {
        return try {
            if (sector == null || sector.playerFleet == null) null else sector.playerFleet.cargo
        } catch (_: Throwable) {
            null
        }
    }

    fun addMessage(message: String?) {
        if (ui != null && message != null) {
            ui.addMessage(message)
        }
    }

    fun refreshCargoCore(log: Logger?, logPrefix: String, fallbackMarket: MarketAPI?): Boolean {
        if (dialog == null || dialog.visualPanel == null) {
            return false
        }
        val market = getCurrentMarketOr(fallbackMarket)
        return try {
            dialog.visualPanel.closeCoreUI()
            dialog.visualPanel.showCore(
                CoreUITabId.CARGO,
                dialog.interactionTarget,
                CampaignUIAPI.CoreUITradeMode.OPEN,
                WimGuiNoopCoreInteractionListener.INSTANCE,
            )
            log?.info("$logPrefix market=${market?.id ?: "null"}")
            true
        } catch (t: Throwable) {
            log?.warn("$logPrefix failed; closing stale core UI", t)
            try {
                dialog.visualPanel.closeCoreUI()
            } catch (_: Throwable) {
            }
            false
        }
    }

    companion object {
        @JvmStatic
        fun current(): WimGuiCampaignDialogHost {
            val sector = Global.getSector()
            val ui = sector?.campaignUI
            val dialog = ui?.currentInteractionDialog
            return WimGuiCampaignDialogHost(sector, ui, dialog)
        }
    }
}
