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
import weaponinventorymod.core.SubmarketWeaponStock;
import weaponinventorymod.core.WeaponStockRecord;
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
    private final List<StockReviewPendingPurchase> pendingPurchases = new ArrayList<StockReviewPendingPurchase>();
    private final MarketAPI initialMarket;

    private CustomPanelAPI root;
    private CustomVisualDialogDelegate.DialogCallbacks callbacks;
    private CustomPanelAPI content;
    private WeaponStockSnapshot snapshot;
    private int renderedMaxScrollOffset = 0;
    private boolean reviewMode = false;

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
        if (StockReviewAction.Type.ADJUST_PLAN.equals(type)) {
            addPendingTrade(action);
            return;
        }
        if (StockReviewAction.Type.BUY_FROM_SUBMARKET.equals(type)) {
            addPendingTrade(action);
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
        if (StockReviewAction.Type.PURCHASE_UNTIL_SUFFICIENT.equals(type)) {
            purchaseUntilSufficient();
            return;
        }
        if (StockReviewAction.Type.REVIEW_PURCHASE.equals(type)) {
            if (!pendingPurchases.isEmpty()) {
                reviewMode = true;
                state.setListScrollOffset(0);
                rebuildContent();
            }
            return;
        }
        if (StockReviewAction.Type.GO_BACK.equals(type)) {
            reviewMode = false;
            state.setListScrollOffset(0);
            rebuildContent();
            return;
        }
        if (StockReviewAction.Type.CONFIRM_PURCHASE.equals(type)) {
            confirmPendingPurchases();
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

    private void addPendingTrade(StockReviewAction action) {
        int available = availableFor(action);
        if (available <= 0) {
            reportMessage(action.getQuantity() < 0 ? "No more player-cargo stock is available to sell." : "No more buyable stock is available for that plan.");
            rebuildContent();
            return;
        }
        int requested = action.getQuantity();
        int quantity = requested > 0 ? Math.min(requested, available) : -Math.min(-requested, available);
        StockReviewPendingPurchase existing = findPending(action.getWeaponId(), action.getSubmarketId());
        if (existing == null) {
            pendingPurchases.add(new StockReviewPendingPurchase(action.getWeaponId(), action.getSubmarketId(), quantity));
        } else {
            existing.addQuantity(quantity);
            if (existing.isZero()) {
                pendingPurchases.remove(existing);
            }
        }
        if (Math.abs(quantity) < Math.abs(requested)) {
            reportMessage("Only " + Math.abs(quantity) + " more can be tallied for that weapon.");
        }
        rebuildContent();
    }

    private void purchaseUntilSufficient() {
        if (snapshot == null) {
            return;
        }
        int added = 0;
        for (int i = 0; i < snapshot.getAllRecords().size(); i++) {
            WeaponStockRecord record = snapshot.getAllRecords().get(i);
            int planQuantity = pendingNetQuantityForWeapon(record.getWeaponId());
            int needed = Math.max(0, record.getDesiredCount() - (record.getOwnedCount() + planQuantity));
            int buyRemaining = Math.max(0, record.getBuyableCount() - pendingBuyQuantityForWeapon(record.getWeaponId()));
            int quantity = Math.min(needed, buyRemaining);
            if (quantity <= 0) {
                continue;
            }
            StockReviewPendingPurchase existing = findPending(record.getWeaponId(), null);
            if (existing == null) {
                pendingPurchases.add(new StockReviewPendingPurchase(record.getWeaponId(), null, quantity));
            } else {
                existing.addQuantity(quantity);
            }
            added += quantity;
        }
        if (added <= 0) {
            reportMessage("No additional sufficient-stock purchases are available.");
            rebuildContent();
            return;
        }
        reviewMode = true;
        state.setListScrollOffset(0);
        rebuildContent();
    }

    private int availableFor(StockReviewAction action) {
        WeaponStockRecord record = StockReviewPurchasePreview.findRecord(snapshot, action.getWeaponId());
        if (record == null) {
            return 0;
        }
        if (action.getQuantity() < 0) {
            return Math.max(0, record.getPlayerCargoCount() - pendingSellQuantityForWeapon(action.getWeaponId()));
        }
        int totalRemaining = Math.max(0, record.getBuyableCount() - pendingBuyQuantityForWeapon(action.getWeaponId()));
        if (action.getSubmarketId() == null) {
            return totalRemaining;
        }
        int sourceRemaining = 0;
        for (int i = 0; i < record.getSubmarketStocks().size(); i++) {
            SubmarketWeaponStock stock = record.getSubmarketStocks().get(i);
            if (action.getSubmarketId().equals(stock.getSubmarketId()) && stock.isPurchasable()) {
                sourceRemaining += stock.getCount();
            }
        }
        return Math.max(0, Math.min(totalRemaining, sourceRemaining - pendingQuantityForSource(action.getWeaponId(), action.getSubmarketId())));
    }

    private int pendingBuyQuantityForWeapon(String weaponId) {
        int count = 0;
        for (int i = 0; i < pendingPurchases.size(); i++) {
            StockReviewPendingPurchase purchase = pendingPurchases.get(i);
            if (weaponId != null && weaponId.equals(purchase.getWeaponId()) && purchase.getQuantity() > 0) {
                count += purchase.getQuantity();
            }
        }
        return count;
    }

    private int pendingNetQuantityForWeapon(String weaponId) {
        int count = 0;
        for (int i = 0; i < pendingPurchases.size(); i++) {
            StockReviewPendingPurchase purchase = pendingPurchases.get(i);
            if (weaponId != null && weaponId.equals(purchase.getWeaponId())) {
                count += purchase.getQuantity();
            }
        }
        return count;
    }

    private int pendingSellQuantityForWeapon(String weaponId) {
        int count = 0;
        for (int i = 0; i < pendingPurchases.size(); i++) {
            StockReviewPendingPurchase purchase = pendingPurchases.get(i);
            if (weaponId != null && weaponId.equals(purchase.getWeaponId()) && purchase.getQuantity() < 0) {
                count += -purchase.getQuantity();
            }
        }
        return count;
    }

    private int pendingQuantityForSource(String weaponId, String submarketId) {
        int count = 0;
        for (int i = 0; i < pendingPurchases.size(); i++) {
            StockReviewPendingPurchase purchase = pendingPurchases.get(i);
            if (purchase.matches(weaponId, submarketId)) {
                count += purchase.getQuantity();
            }
        }
        return count;
    }

    private StockReviewPendingPurchase findPending(String weaponId, String submarketId) {
        for (int i = 0; i < pendingPurchases.size(); i++) {
            StockReviewPendingPurchase purchase = pendingPurchases.get(i);
            if (purchase.matches(weaponId, submarketId)) {
                return purchase;
            }
        }
        return null;
    }

    private void confirmPendingPurchases() {
        if (pendingPurchases.isEmpty()) {
            reviewMode = false;
            rebuildContent();
            return;
        }
        int estimatedCost = StockReviewPurchasePreview.totalCost(snapshot, pendingPurchases);
        if (estimatedCost == StockReviewPurchasePreview.PRICE_UNAVAILABLE) {
            reportMessage("Could not price every queued weapon. Refresh and try again.");
            rebuildContent();
            return;
        }
        float credits = StockReviewPurchasePreview.currentCredits();
        if (estimatedCost > 0 && credits + 0.01f < estimatedCost) {
            reportMessage("Need " + estimatedCost + " credits for this purchase.");
            rebuildContent();
            return;
        }

        while (!pendingPurchases.isEmpty()) {
            int index = firstSellerSpecificPurchaseIndex();
            StockReviewPendingPurchase purchase = pendingPurchases.remove(index);
            StockPurchaseService.PurchaseResult result = purchase.isSell()
                    ? purchaseService.sellToMarket(Global.getSector(), currentMarket(Global.getSector()), purchase.getWeaponId(), -purchase.getQuantity(), state.isIncludeBlackMarket())
                    : purchase.getSubmarketId() == null
                    ? purchaseService.buyCheapest(Global.getSector(), currentMarket(Global.getSector()), purchase.getWeaponId(), purchase.getQuantity(), state.isIncludeBlackMarket())
                    : purchaseService.buyFromSubmarket(Global.getSector(), currentMarket(Global.getSector()), purchase.getWeaponId(), purchase.getSubmarketId(), purchase.getQuantity(), state.isIncludeBlackMarket());
            if (result == null || !result.isSuccess()) {
                if (result != null) {
                    reportPurchaseFailure(result);
                }
                pendingPurchases.add(0, purchase);
                rebuildSnapshot();
                rebuildContent();
                return;
            }
        }
        reviewMode = false;
        state.setListScrollOffset(0);
        rebuildSnapshot();
        if (StockReviewStyle.REFRESH_VANILLA_CORE_AFTER_PURCHASE) {
            StockReviewHotkeyScript.requestReopen(currentMarket(Global.getSector()), state);
            refreshVanillaCargoScreen();
            close();
            return;
        }
        rebuildContent();
    }

    private int firstSellerSpecificPurchaseIndex() {
        for (int i = 0; i < pendingPurchases.size(); i++) {
            if (pendingPurchases.get(i).getSubmarketId() != null) {
                return i;
            }
        }
        return 0;
    }

    private void reportPurchaseFailure(StockPurchaseService.PurchaseResult result) {
        reportMessage(result.getMessage());
        LOG.info("WIM_STOCK_REVIEW purchase blocked: " + result.getMessage());
    }

    private void reportMessage(String message) {
        SectorAPI sector = Global.getSector();
        if (sector != null && sector.getCampaignUI() != null) {
            sector.getCampaignUI().addMessage(message);
        }
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
            StockReviewRenderer.RenderResult result = renderer.render(content, snapshot, state, pendingPurchases, reviewMode, buttons);
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
