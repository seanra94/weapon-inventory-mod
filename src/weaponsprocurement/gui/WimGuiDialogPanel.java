package weaponsprocurement.gui;

import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.ui.CustomPanelAPI;

interface WimGuiDialogPanel extends CustomUIPanelPlugin {
    void init(CustomPanelAPI panel, CustomVisualDialogDelegate.DialogCallbacks callbacks);

    void reportDialogDismissed(int option);
}
