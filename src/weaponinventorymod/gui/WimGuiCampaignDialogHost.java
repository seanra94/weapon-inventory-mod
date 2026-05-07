package weaponinventorymod.gui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import org.apache.log4j.Logger;

final class WimGuiCampaignDialogHost {
    private final SectorAPI sector;
    private final CampaignUIAPI ui;
    private final InteractionDialogAPI dialog;

    private WimGuiCampaignDialogHost(SectorAPI sector,
                                     CampaignUIAPI ui,
                                     InteractionDialogAPI dialog) {
        this.sector = sector;
        this.ui = ui;
        this.dialog = dialog;
    }

    static WimGuiCampaignDialogHost current() {
        SectorAPI sector = Global.getSector();
        CampaignUIAPI ui = sector == null ? null : sector.getCampaignUI();
        InteractionDialogAPI dialog = ui == null ? null : ui.getCurrentInteractionDialog();
        return new WimGuiCampaignDialogHost(sector, ui, dialog);
    }

    boolean hasDialog() {
        return dialog != null;
    }

    SectorAPI getSector() {
        return sector;
    }

    InteractionDialogAPI getDialog() {
        return dialog;
    }

    MarketAPI getCurrentMarket() {
        return sector == null ? null : sector.getCurrentlyOpenMarket();
    }

    MarketAPI getCurrentMarketOr(MarketAPI fallback) {
        MarketAPI market = getCurrentMarket();
        return market == null ? fallback : market;
    }

    CargoAPI getPlayerCargo() {
        try {
            return sector == null || sector.getPlayerFleet() == null ? null : sector.getPlayerFleet().getCargo();
        } catch (Throwable ignored) {
            return null;
        }
    }

    void addMessage(String message) {
        if (ui != null && message != null) {
            ui.addMessage(message);
        }
    }

    boolean refreshCargoCore(Logger log, String logPrefix, MarketAPI fallbackMarket) {
        if (dialog == null || dialog.getVisualPanel() == null) {
            return false;
        }
        MarketAPI market = getCurrentMarketOr(fallbackMarket);
        try {
            dialog.getVisualPanel().closeCoreUI();
            dialog.getVisualPanel().showCore(CoreUITabId.CARGO, dialog.getInteractionTarget(),
                    CampaignUIAPI.CoreUITradeMode.OPEN, WimGuiNoopCoreInteractionListener.INSTANCE);
            if (log != null) {
                log.info(logPrefix + " market=" + (market == null ? "null" : market.getId()));
            }
            return true;
        } catch (Throwable t) {
            if (log != null) {
                log.warn(logPrefix + " failed; closing stale core UI", t);
            }
            try {
                dialog.getVisualPanel().closeCoreUI();
            } catch (Throwable ignored) {
            }
            return false;
        }
    }
}
