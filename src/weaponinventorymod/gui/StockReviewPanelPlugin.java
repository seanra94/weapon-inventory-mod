package weaponinventorymod.gui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreInteractionListener;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import org.apache.log4j.Logger;
import org.lwjgl.input.Keyboard;
import weaponinventorymod.core.StockReviewConfig;
import weaponinventorymod.core.StockPurchaseService;
import weaponinventorymod.core.WeaponStockSnapshot;
import weaponinventorymod.core.WeaponStockSnapshotBuilder;

import java.util.ArrayList;
import java.util.List;

public final class StockReviewPanelPlugin extends BaseCustomUIPanelPlugin {
    private static final Logger LOG = Logger.getLogger(StockReviewPanelPlugin.class);

    private final StockReviewConfig config = StockReviewConfig.load();
    private final StockReviewState state;
    private final StockReviewRenderer renderer = new StockReviewRenderer();
    private final WeaponStockSnapshotBuilder snapshotBuilder = new WeaponStockSnapshotBuilder();
    private final StockPurchaseService purchaseService = new StockPurchaseService();
    private final List<StockReviewButtonBinding> buttons = new ArrayList<StockReviewButtonBinding>();
    private final MarketAPI initialMarket;

    private CustomPanelAPI root;
    private CustomVisualDialogDelegate.DialogCallbacks callbacks;
    private CustomPanelAPI content;
    private WeaponStockSnapshot snapshot;
    private int renderedMaxScrollOffset = 0;

    public StockReviewPanelPlugin(MarketAPI initialMarket, StockReviewState initialState) {
        this.initialMarket = initialMarket;
        this.state = initialState == null ? new StockReviewState(config) : new StockReviewState(initialState);
    }

    void init(CustomPanelAPI panel, CustomVisualDialogDelegate.DialogCallbacks callbacks) {
        this.root = panel;
        this.callbacks = callbacks;
        rebuildSnapshot();
        rebuildContent();
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (events == null) {
            return;
        }
        for (InputEventAPI event : events) {
            if (event == null || event.isConsumed()) {
                continue;
            }
            if (event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE) {
                event.consume();
                close();
                return;
            }
            if (event.isMouseScrollEvent() && event.getEventValue() != 0 && isMouseInList(event)) {
                int delta = event.getEventValue() > 0 ? -StockReviewStyle.SCROLL_STEP : StockReviewStyle.SCROLL_STEP;
                state.adjustListScrollOffset(delta, renderedMaxScrollOffset);
                event.consume();
                rebuildContent();
                return;
            }
        }
    }

    @Override
    public void advance(float amount) {
        if (callbacks == null) {
            return;
        }
        for (int i = 0; i < buttons.size(); i++) {
            StockReviewButtonBinding binding = buttons.get(i);
            if (!binding.consumeIfPressed()) {
                continue;
            }
            handleAction(binding.getAction());
            return;
        }
    }

    @Override
    public void buttonPressed(Object buttonId) {
        if (buttonId instanceof StockReviewAction) {
            handleAction((StockReviewAction) buttonId);
        }
    }

    private void handleAction(StockReviewAction action) {
        if (action == null) {
            return;
        }
        StockReviewAction.Type type = action.getType();
        if (StockReviewAction.Type.TOGGLE_CATEGORY.equals(type)) {
            state.toggle(action.getCategory());
            rebuildContent();
            return;
        }
        if (StockReviewAction.Type.TOGGLE_WEAPON.equals(type)) {
            state.toggleWeapon(action.getWeaponId());
            rebuildContent();
            return;
        }
        if (StockReviewAction.Type.TOGGLE_WEAPON_SECTION.equals(type)) {
            state.toggleWeaponSection(action.getWeaponId(), action.getSection());
            rebuildContent();
            return;
        }
        if (StockReviewAction.Type.BUY_BEST.equals(type)) {
            handlePurchase(buyBest(action));
            return;
        }
        if (StockReviewAction.Type.BUY_FROM_SUBMARKET.equals(type)) {
            handlePurchase(buyFromSubmarket(action));
            return;
        }
        if (StockReviewAction.Type.CYCLE_DISPLAY_MODE.equals(type)) {
            state.cycleDisplayMode();
            rebuildSnapshot();
            rebuildContent();
            return;
        }
        if (StockReviewAction.Type.CYCLE_SORT_MODE.equals(type)) {
            state.cycleSortMode();
            rebuildSnapshot();
            rebuildContent();
            return;
        }
        if (StockReviewAction.Type.TOGGLE_CURRENT_MARKET_STORAGE.equals(type)) {
            state.toggleCurrentMarketStorage();
            rebuildSnapshot();
            rebuildContent();
            return;
        }
        if (StockReviewAction.Type.TOGGLE_BLACK_MARKET.equals(type)) {
            state.toggleBlackMarket();
            rebuildSnapshot();
            rebuildContent();
            return;
        }
        if (StockReviewAction.Type.SCROLL_LIST.equals(type)) {
            state.adjustListScrollOffset(action.getQuantity(), renderedMaxScrollOffset);
            rebuildContent();
            return;
        }
        if (StockReviewAction.Type.REFRESH.equals(type)) {
            rebuildSnapshot();
            rebuildContent();
            return;
        }
        if (StockReviewAction.Type.CLOSE.equals(type)) {
            close();
        }
    }

    private StockPurchaseService.PurchaseResult buyBest(StockReviewAction action) {
        return purchaseService.buyCheapest(
                Global.getSector(), currentMarket(Global.getSector()), action.getWeaponId(), action.getQuantity(), state.isIncludeBlackMarket());
    }

    private StockPurchaseService.PurchaseResult buyFromSubmarket(StockReviewAction action) {
        return purchaseService.buyFromSubmarket(
                Global.getSector(), currentMarket(Global.getSector()), action.getWeaponId(), action.getSubmarketId(), action.getQuantity(), state.isIncludeBlackMarket());
    }

    private void handlePurchase(StockPurchaseService.PurchaseResult result) {
        if (result == null) {
            reopen();
            return;
        }
        if (!result.isSuccess()) {
            reportPurchaseFailure(result);
            reopen();
            return;
        }
        StockReviewHotkeyScript.requestReopen(currentMarket(Global.getSector()), state);
        refreshVanillaCargoScreen();
        close();
    }

    private void reportPurchaseFailure(StockPurchaseService.PurchaseResult result) {
        SectorAPI sector = Global.getSector();
        if (sector != null && sector.getCampaignUI() != null) {
            sector.getCampaignUI().addMessage(result.getMessage());
        }
        LOG.info("WIM_STOCK_REVIEW purchase blocked: " + result.getMessage());
    }

    private void rebuildSnapshot() {
        SectorAPI sector = Global.getSector();
        MarketAPI market = currentMarket(sector);
        snapshot = snapshotBuilder.build(sector, market, config, state.getDisplayMode(),
                state.getSortMode(), state.isIncludeCurrentMarketStorage(), state.isIncludeBlackMarket());
    }

    private MarketAPI currentMarket(SectorAPI sector) {
        if (sector != null && sector.getCurrentlyOpenMarket() != null) {
            return sector.getCurrentlyOpenMarket();
        }
        return initialMarket;
    }

    private void rebuildContent() {
        if (root == null || snapshot == null) {
            return;
        }
        try {
            if (content != null) {
                root.removeComponent(content);
            }
            buttons.clear();
            content = root.createCustomPanel(StockReviewStyle.WIDTH, StockReviewStyle.HEIGHT, null);
            StockReviewRenderer.RenderResult result = renderer.render(content, snapshot, state, buttons);
            renderedMaxScrollOffset = result.getMaxScrollOffset();
            root.addComponent(content).inTL(0f, 0f);
        } catch (Throwable t) {
            LOG.error("WIM_STOCK_REVIEW rebuild failed", t);
            close();
        }
    }

    private boolean isMouseInList(InputEventAPI event) {
        if (root == null || root.getPosition() == null || renderedMaxScrollOffset <= 0) {
            return false;
        }
        float left = root.getPosition().getX() + StockReviewStyle.PAD;
        float right = left + StockReviewStyle.LIST_WIDTH;
        float top = root.getPosition().getY() + root.getPosition().getHeight() - StockReviewStyle.LIST_TOP;
        float bottom = top - StockReviewStyle.LIST_HEIGHT;
        return event.getX() >= left && event.getX() <= right && event.getY() >= bottom && event.getY() <= top;
    }

    private void close() {
        if (callbacks != null) {
            CustomVisualDialogDelegate.DialogCallbacks currentCallbacks = callbacks;
            callbacks = null;
            currentCallbacks.dismissDialog();
        }
    }

    private void reopen() {
        StockReviewHotkeyScript.requestReopen(currentMarket(Global.getSector()), state);
        close();
    }

    private void refreshVanillaCargoScreen() {
        SectorAPI sector = Global.getSector();
        CampaignUIAPI ui = sector == null ? null : sector.getCampaignUI();
        InteractionDialogAPI dialog = ui == null ? null : ui.getCurrentInteractionDialog();
        if (dialog == null || dialog.getVisualPanel() == null) {
            return;
        }
        MarketAPI market = currentMarket(sector);
        try {
            dialog.getVisualPanel().closeCoreUI();
            dialog.getVisualPanel().showCore(CoreUITabId.CARGO, dialog.getInteractionTarget(),
                    CampaignUIAPI.CoreUITradeMode.OPEN, new CoreInteractionListener() {
                        @Override
                        public void coreUIDismissed() {
                        }
                    });
            LOG.info("WIM_STOCK_REVIEW refreshed vanilla cargo screen market=" + (market == null ? "null" : market.getId()));
        } catch (Throwable t) {
            LOG.warn("WIM_STOCK_REVIEW could not refresh vanilla cargo screen after purchase; closing stale core UI", t);
            try {
                dialog.getVisualPanel().closeCoreUI();
            } catch (Throwable ignored) {
            }
        }
    }
}
