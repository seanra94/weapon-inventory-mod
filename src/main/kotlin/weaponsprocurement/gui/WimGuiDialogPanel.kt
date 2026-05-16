package weaponsprocurement.gui

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin
import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate
import com.fs.starfarer.api.ui.CustomPanelAPI

interface WimGuiDialogPanel : CustomUIPanelPlugin {
    fun init(panel: CustomPanelAPI, callbacks: CustomVisualDialogDelegate.DialogCallbacks)

    fun reportDialogDismissed(option: Int)
}
