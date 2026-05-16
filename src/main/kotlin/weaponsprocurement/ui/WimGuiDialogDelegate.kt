package weaponsprocurement.ui

import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate
import com.fs.starfarer.api.ui.CustomPanelAPI

class WimGuiDialogDelegate(private val panel: WimGuiDialogPanel) : CustomVisualDialogDelegate {
    override fun init(panel: CustomPanelAPI, callbacks: CustomVisualDialogDelegate.DialogCallbacks) {
        this.panel.init(panel, callbacks)
    }

    override fun getCustomPanelPlugin(): WimGuiDialogPanel = panel

    override fun getNoiseAlpha(): Float = 0f

    override fun advance(amount: Float) {
    }

    override fun reportDismissed(option: Int) {
        panel.reportDialogDismissed(option)
    }
}
