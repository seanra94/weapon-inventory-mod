package weaponinventorymod.gui;

import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;

final class WimGuiButtonShell {
    final CustomPanelAPI panel;
    final ButtonAPI button;

    WimGuiButtonShell(CustomPanelAPI panel, ButtonAPI button) {
        this.panel = panel;
        this.button = button;
    }
}
