package weaponsprocurement.gui

import com.fs.starfarer.api.ui.CustomPanelAPI

class WimGuiModalActionRow private constructor() {
    companion object {
        @JvmStatic
        fun <A> add(
            parent: CustomPanelAPI,
            modal: WimGuiModalLayout,
            headerHeight: Float,
            headerToActionGap: Float,
            buttonHeight: Float,
            buttonGap: Float,
            specs: List<WimGuiButtonSpec<A>>,
            bindings: List<WimGuiButtonBinding<A>>,
        ) {
            val y = modal.actionRowY(headerHeight, headerToActionGap)
            WimGuiControls.addButtonRow(parent, modal.padding, y, buttonHeight, buttonGap, specs, bindings)
        }
    }
}
