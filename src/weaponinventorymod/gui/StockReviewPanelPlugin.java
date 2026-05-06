package weaponinventorymod.gui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import org.apache.log4j.Logger;
import org.lwjgl.input.Keyboard;
import weaponinventorymod.core.StockReviewConfig;
import weaponinventorymod.core.WeaponStockSnapshot;
import weaponinventorymod.core.WeaponStockSnapshotBuilder;

import java.util.ArrayList;
import java.util.List;

public final class StockReviewPanelPlugin extends BaseCustomUIPanelPlugin {
    private static final Logger LOG = Logger.getLogger(StockReviewPanelPlugin.class);

    private final StockReviewConfig config = StockReviewConfig.load();
    private final StockReviewState state = new StockReviewState(config);
    private final StockReviewRenderer renderer = new StockReviewRenderer();
    private final WeaponStockSnapshotBuilder snapshotBuilder = new WeaponStockSnapshotBuilder();
    private final List<StockReviewButtonBinding> buttons = new ArrayList<StockReviewButtonBinding>();
    private final MarketAPI initialMarket;

    private CustomPanelAPI root;
    private CustomVisualDialogDelegate.DialogCallbacks callbacks;
    private TooltipMakerAPI content;
    private WeaponStockSnapshot snapshot;

    public StockReviewPanelPlugin(MarketAPI initialMarket) {
        this.initialMarket = initialMarket;
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
        }
    }

    @Override
    public void advance(float amount) {
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
        // Tooltip buttons are polled in advance() so one click cannot toggle twice
        // if Starsector also routes buttonPressed() for this custom panel.
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
        if (StockReviewAction.Type.CYCLE_DISPLAY_MODE.equals(type)) {
            state.cycleDisplayMode();
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
        if (StockReviewAction.Type.REFRESH.equals(type)) {
            rebuildSnapshot();
            rebuildContent();
            return;
        }
        if (StockReviewAction.Type.CLOSE.equals(type)) {
            close();
        }
    }

    private void rebuildSnapshot() {
        SectorAPI sector = Global.getSector();
        MarketAPI market = currentMarket(sector);
        snapshot = snapshotBuilder.build(sector, market, config, state.getDisplayMode(),
                state.isIncludeCurrentMarketStorage(), state.isIncludeBlackMarket());
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
            content = root.createUIElement(StockReviewStyle.WIDTH, StockReviewStyle.HEIGHT, true);
            renderer.render(content, snapshot, state, buttons);
            root.addUIElement(content).inTL(0f, 0f);
        } catch (Throwable t) {
            LOG.error("WIM_STOCK_REVIEW rebuild failed", t);
            close();
        }
    }

    private void close() {
        if (callbacks != null) {
            callbacks.dismissDialog();
        }
    }
}
