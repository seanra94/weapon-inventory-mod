package weaponinventorymod.gui;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate;
import com.fs.starfarer.api.ui.CustomPanelAPI;

public final class StockReviewDialogDelegate implements CustomVisualDialogDelegate {
    private final StockReviewPanelPlugin panelPlugin;

    public StockReviewDialogDelegate(StockReviewPanelPlugin panelPlugin) {
        this.panelPlugin = panelPlugin;
    }

    @Override
    public void init(CustomPanelAPI panel, DialogCallbacks callbacks) {
        panelPlugin.init(panel, callbacks);
    }

    @Override
    public CustomUIPanelPlugin getCustomPanelPlugin() {
        return panelPlugin;
    }

    @Override
    public float getNoiseAlpha() {
        return 0f;
    }

    @Override
    public void advance(float amount) {
    }

    @Override
    public void reportDismissed(int option) {
        StockReviewHotkeyScript.markDialogClosed();
    }
}
