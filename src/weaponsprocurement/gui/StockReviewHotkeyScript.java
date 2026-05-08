package weaponsprocurement.gui;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import org.apache.log4j.Logger;
import org.lwjgl.input.Keyboard;
import weaponsprocurement.core.StockReviewConfig;
import weaponsprocurement.core.SubmarketWeaponStock;
import weaponsprocurement.core.MarketStockService;
import weaponsprocurement.core.StockItemStacks;
import weaponsprocurement.internal.WeaponsProcurementConfig;

public final class StockReviewHotkeyScript implements EveryFrameScript {
    private static final Logger LOG = Logger.getLogger(StockReviewHotkeyScript.class);
    private static final int HOTKEY = Keyboard.KEY_F8;
    private static final WimGuiDialogTracker<MarketAPI, StockReviewLaunchState> DIALOG_TRACKER =
            new WimGuiDialogTracker<MarketAPI, StockReviewLaunchState>();

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
            WimGuiPendingDialog<MarketAPI, StockReviewLaunchState> pending = DIALOG_TRACKER.consumePending();
            openDialog(pending.getContext(), pending.getState(), "pending-reopen");
            return;
        }
        if (hotkey.consumePress()) {
            openFromCurrentDialog("hotkey=F8");
        }
    }

    static void markDialogClosed() {
        DIALOG_TRACKER.markClosed();
    }

    static void requestReopen(MarketAPI market, StockReviewState state) {
        requestReopen(market, new StockReviewLaunchState(state, null, false));
    }

    static void requestReopen(MarketAPI market, StockReviewLaunchState launchState) {
        DIALOG_TRACKER.requestReopen(market, launchState);
    }

    public static boolean canOpenFromCurrentDialog() {
        WimGuiCampaignDialogHost host = WimGuiCampaignDialogHost.current();
        return host.hasDialog() && canOpenAtCurrentMarket(host.getCurrentMarket());
    }

    public static void openFromCurrentDialog(String source) {
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
            host.addMessage("Weapon Stock Review requires an active market or storage dialog.");
            return;
        }
        try {
            openDialog(market, null, source);
        } catch (Throwable t) {
            DIALOG_TRACKER.markClosed();
            LOG.error("WP_STOCK_REVIEW open failed", t);
            host.addMessage("Weapon Stock Review failed to open. Check starsector.log.");
        }
    }

    public static boolean canOpenAtCurrentMarket(MarketAPI market) {
        if (market == null) {
            return false;
        }
        try {
            StockReviewConfig config = StockReviewConfig.load();
            MarketStockService.MarketStock stock = new MarketStockService().collectCurrentMarketItemStock(market, config.isIncludeBlackMarket());
            for (String itemKey : stock.itemKeys()) {
                for (SubmarketWeaponStock submarketStock : stock.getSubmarketStocks(itemKey)) {
                    if (submarketStock.isPurchasable() && submarketStock.getCount() > 0) {
                        return true;
                    }
                }
            }
            if (playerHasTradeableCargo()) {
                return true;
            }
            return hasEnabledRemoteSource();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean hasEnabledRemoteSource() {
        return WeaponsProcurementConfig.isSectorMarketEnabled() || WeaponsProcurementConfig.isFixersMarketEnabled();
    }

    private static boolean playerHasTradeableCargo() {
        if (Global.getSector() == null || Global.getSector().getPlayerFleet() == null) {
            return false;
        }
        CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();
        if (cargo == null || cargo.getStacksCopy() == null) {
            return false;
        }
        for (CargoStackAPI stack : cargo.getStacksCopy()) {
            if (StockItemStacks.isVisibleWeaponStack(stack) || StockItemStacks.isVisibleWingStack(stack)) {
                return true;
            }
        }
        return false;
    }

    private static void openDialog(MarketAPI market, StockReviewLaunchState launchState, String source) {
        WimGuiCampaignDialogHost host = WimGuiCampaignDialogHost.current();
        if (!host.hasDialog()) {
            DIALOG_TRACKER.markClosed();
            return;
        }
        try {
            StockReviewPanelPlugin panelPlugin = new StockReviewPanelPlugin(market, launchState);
            WimGuiDialogOpener.show(host.getDialog(), StockReviewStyle.widthFor(panelPlugin.isReviewMode()), StockReviewStyle.HEIGHT, panelPlugin);
            DIALOG_TRACKER.markOpen();
            LOG.info("WP_STOCK_REVIEW opened source=" + source + " market=" + (market == null ? "null" : market.getId()));
        } catch (Throwable t) {
            DIALOG_TRACKER.markClosed();
            LOG.error("WP_STOCK_REVIEW open failed", t);
            host.addMessage("Weapon Stock Review failed to open. Check starsector.log.");
        }
    }
}
