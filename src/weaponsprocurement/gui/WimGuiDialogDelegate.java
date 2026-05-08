package weaponsprocurement.gui;

import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate;
import com.fs.starfarer.api.ui.CustomPanelAPI;

final class WimGuiDialogDelegate implements CustomVisualDialogDelegate {
    private final WimGuiDialogPanel panel;

    WimGuiDialogDelegate(WimGuiDialogPanel panel) {
        this.panel = panel;
    }

    @Override
    public void init(CustomPanelAPI panel, DialogCallbacks callbacks) {
        this.panel.init(panel, callbacks);
    }

    @Override
    public WimGuiDialogPanel getCustomPanelPlugin() {
        return panel;
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
        panel.reportDialogDismissed(option);
    }
}
