package weaponinventorymod.gui;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import org.apache.log4j.Logger;
import org.lwjgl.input.Keyboard;
import weaponinventorymod.core.MarketStockService;
import weaponinventorymod.core.StockReviewConfig;
import weaponinventorymod.core.SubmarketWeaponStock;
import weaponinventorymod.core.GlobalWeaponMarketService;

public final class StockReviewHotkeyScript implements EveryFrameScript {
    private static final Logger LOG = Logger.getLogger(StockReviewHotkeyScript.class);
    private static final int HOTKEY = Keyboard.KEY_F8;
    private static final WimGuiDialogTracker<MarketAPI, StockReviewState> DIALOG_TRACKER =
            new WimGuiDialogTracker<MarketAPI, StockReviewState>();

    private final WimGuiHotkeyLatch hotkey = new WimGuiHotkeyLatch(HOTKEY);

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
        if (DIALOG_TRACKER.hasPending()) {
            WimGuiPendingDialog<MarketAPI, StockReviewState> pending = DIALOG_TRACKER.consumePending();
            openDialog(pending.getContext(), pending.getState());
            return;
        }
        if (hotkey.consumePress()) {
            openDialog();
        }
    }

    static void markDialogClosed() {
        DIALOG_TRACKER.markClosed();
    }

    static void requestReopen(MarketAPI market, StockReviewState state) {
        DIALOG_TRACKER.requestReopen(market, new StockReviewState(state));
    }

    private void openDialog() {
        if (DIALOG_TRACKER.isOpen()) {
            return;
        }
        WimGuiCampaignDialogHost host = WimGuiCampaignDialogHost.current();
        if (!host.hasDialog()) {
            host.addMessage("Weapon Stock Review requires an active market/storage dialog.");
            return;
        }

        MarketAPI market = host.getCurrentMarket();
        if (!canOpenAtCurrentMarket(market)) {
            host.addMessage("Weapon Stock Review is only available while shopping at a market or carrying weapons to sell.");
            return;
        }
        try {
            openDialog(market, null);
        } catch (Throwable t) {
            DIALOG_TRACKER.markClosed();
            LOG.error("WIM_STOCK_REVIEW open failed", t);
            host.addMessage("Weapon Stock Review failed to open. Check starsector.log.");
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
            MarketStockService.MarketStock globalStock =
                    new GlobalWeaponMarketService().collectGlobalWeaponStock(Global.getSector(), config.isIncludeBlackMarket());
            for (String ignored : globalStock.weaponIds()) {
                return true;
            }
            return playerHasWeaponCargo();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean playerHasWeaponCargo() {
        if (Global.getSector() == null || Global.getSector().getPlayerFleet() == null) {
            return false;
        }
        CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();
        if (cargo == null || cargo.getStacksCopy() == null) {
            return false;
        }
        for (CargoStackAPI stack : cargo.getStacksCopy()) {
            if (MarketStockService.isVisibleWeaponStack(stack)) {
                return true;
            }
        }
        return false;
    }

    private void openDialog(MarketAPI market, StockReviewState initialState) {
        WimGuiCampaignDialogHost host = WimGuiCampaignDialogHost.current();
        if (!host.hasDialog()) {
            DIALOG_TRACKER.markClosed();
            return;
        }
        try {
            StockReviewPanelPlugin panelPlugin = new StockReviewPanelPlugin(market, initialState);
            WimGuiDialogOpener.show(host.getDialog(), StockReviewStyle.WIDTH, StockReviewStyle.HEIGHT, panelPlugin);
            DIALOG_TRACKER.markOpen();
            LOG.info("WIM_STOCK_REVIEW opened hotkey=F8 market=" + (market == null ? "null" : market.getId()));
        } catch (Throwable t) {
            DIALOG_TRACKER.markClosed();
            LOG.error("WIM_STOCK_REVIEW open failed", t);
            host.addMessage("Weapon Stock Review failed to open. Check starsector.log.");
        }
    }
}
