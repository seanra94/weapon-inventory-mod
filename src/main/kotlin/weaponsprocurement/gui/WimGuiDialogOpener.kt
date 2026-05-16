package weaponsprocurement.gui

import com.fs.starfarer.api.campaign.InteractionDialogAPI

class WimGuiDialogOpener private constructor() {
    companion object {
        @JvmStatic
        fun show(dialog: InteractionDialogAPI?, width: Float, height: Float, panel: WimGuiDialogPanel) {
            if (dialog == null) {
                throw IllegalArgumentException("Cannot show a WP GUI dialog without an active interaction dialog.")
            }
            dialog.showCustomVisualDialog(width, height, WimGuiDialogDelegate(panel))
        }
    }
}
