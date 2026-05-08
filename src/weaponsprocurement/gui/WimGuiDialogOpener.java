package weaponsprocurement.gui;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;

final class WimGuiDialogOpener {
    private WimGuiDialogOpener() {
    }

    static void show(InteractionDialogAPI dialog, float width, float height, WimGuiDialogPanel panel) {
        if (dialog == null) {
            throw new IllegalArgumentException("Cannot show a WP GUI dialog without an active interaction dialog.");
        }
        dialog.showCustomVisualDialog(width, height, new WimGuiDialogDelegate(panel));
    }
}
