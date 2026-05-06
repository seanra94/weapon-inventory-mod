package weaponinventorymod.gui;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import org.apache.log4j.Logger;
import org.lwjgl.input.Keyboard;
import weaponinventorymod.core.MarketStockService;
import weaponinventorymod.core.StockReviewConfig;
import weaponinventorymod.core.SubmarketWeaponStock;

public final class StockReviewHotkeyScript implements EveryFrameScript {
    private static final Logger LOG = Logger.getLogger(StockReviewHotkeyScript.class);
    private static final int HOTKEY = Keyboard.KEY_F8;
    private static boolean dialogOpen = false;
    private static MarketAPI pendingMarket;
    private static StockReviewState pendingState;

    private boolean wasDown = false;

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public void advance(float amount) {
        if (!dialogOpen && pendingState != null) {
            StockReviewState state = pendingState;
            MarketAPI market = pendingMarket;
            pendingState = null;
            pendingMarket = null;
            openDialog(market, state);
            return;
        }
        boolean down = Keyboard.isKeyDown(HOTKEY);
        if (!down) {
            wasDown = false;
            return;
        }
        if (wasDown) {
            return;
        }
        wasDown = true;
        openDialog();
    }

    static void markDialogClosed() {
        dialogOpen = false;
    }

    static void requestReopen(MarketAPI market, StockReviewState state) {
        pendingMarket = market;
        pendingState = new StockReviewState(state);
        dialogOpen = false;
    }

    private void openDialog() {
        if (dialogOpen) {
            return;
        }
        SectorAPI sector = Global.getSector();
        CampaignUIAPI ui = sector == null ? null : sector.getCampaignUI();
        InteractionDialogAPI dialog = ui == null ? null : ui.getCurrentInteractionDialog();
        if (dialog == null) {
            if (ui != null) {
                ui.addMessage("Weapon Stock Review requires an active market/storage dialog.");
            }
            return;
        }

        MarketAPI market = sector.getCurrentlyOpenMarket();
        if (!canOpenAtCurrentMarket(market)) {
            ui.addMessage("Weapon Stock Review is only available while shopping at a market with weapons for sale.");
            return;
        }
        try {
            openDialog(market, null);
        } catch (Throwable t) {
            dialogOpen = false;
            LOG.error("WIM_STOCK_REVIEW open failed", t);
            ui.addMessage("Weapon Stock Review failed to open. Check starsector.log.");
        }
    }

    private static boolean canOpenAtCurrentMarket(MarketAPI market) {
        if (market == null) {
            return false;
        }
        try {
            StockReviewConfig config = StockReviewConfig.load();
            MarketStockService.MarketStock stock = new MarketStockService().collectCurrentMarketWeaponStock(market, config.isIncludeBlackMarket());
            for (String weaponId : stock.weaponIds()) {
                for (SubmarketWeaponStock submarketStock : stock.getSubmarketStocks(weaponId)) {
                    if (submarketStock.isPurchasable() && submarketStock.getCount() > 0) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void openDialog(MarketAPI market, StockReviewState initialState) {
        SectorAPI sector = Global.getSector();
        CampaignUIAPI ui = sector == null ? null : sector.getCampaignUI();
        InteractionDialogAPI dialog = ui == null ? null : ui.getCurrentInteractionDialog();
        if (dialog == null) {
            dialogOpen = false;
            return;
        }
        try {
            StockReviewPanelPlugin panelPlugin = new StockReviewPanelPlugin(market, initialState);
            dialog.showCustomVisualDialog(StockReviewStyle.WIDTH, StockReviewStyle.HEIGHT, new StockReviewDialogDelegate(panelPlugin));
            dialogOpen = true;
            LOG.info("WIM_STOCK_REVIEW opened hotkey=F8 market=" + (market == null ? "null" : market.getId()));
        } catch (Throwable t) {
            dialogOpen = false;
            LOG.error("WIM_STOCK_REVIEW open failed", t);
            ui.addMessage("Weapon Stock Review failed to open. Check starsector.log.");
        }
    }
}
