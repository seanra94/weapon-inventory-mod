package weaponsprocurement.ui

import com.fs.starfarer.api.ui.CustomPanelAPI

class WimGuiModalFooter private constructor() {
    companion object {
        @JvmStatic
        fun <A> addLeftButtonRow(
            parent: CustomPanelAPI,
            modal: WimGuiModalLayout,
            buttonHeight: Float,
            gap: Float,
            specs: List<WimGuiButtonSpec<A>>,
            bindings: MutableList<WimGuiButtonBinding<A>>,
        ) {
            WimGuiControls.addButtonRow(
                parent,
                modal.padding,
                modal.footerButtonY(buttonHeight),
                buttonHeight,
                gap,
                specs,
                bindings,
            )
        }

        @JvmStatic
        fun <A> addRightButton(
            parent: CustomPanelAPI,
            modal: WimGuiModalLayout,
            buttonHeight: Float,
            spec: WimGuiButtonSpec<A>,
            bindings: MutableList<WimGuiButtonBinding<A>>,
        ) {
            WimGuiControls.addBoundButton(
                parent,
                modal.width - modal.padding - spec.width,
                modal.footerButtonY(buttonHeight),
                buttonHeight,
                spec,
                bindings,
            )
        }

        @JvmStatic
        fun <A> addEdgeButtons(
            parent: CustomPanelAPI,
            modal: WimGuiModalLayout,
            buttonHeight: Float,
            left: WimGuiButtonSpec<A>,
            right: WimGuiButtonSpec<A>,
            bindings: MutableList<WimGuiButtonBinding<A>>,
        ) {
            addLeftButtonRow(parent, modal, buttonHeight, modal.rowGap, WimGuiButtonSpecs.of(left), bindings)
            addRightButton(parent, modal, buttonHeight, right, bindings)
        }

        @JvmStatic
        fun <A> addLeftRowAndRightButton(
            parent: CustomPanelAPI,
            modal: WimGuiModalLayout,
            buttonHeight: Float,
            gap: Float,
            leftSpecs: List<WimGuiButtonSpec<A>>,
            right: WimGuiButtonSpec<A>,
            bindings: MutableList<WimGuiButtonBinding<A>>,
        ) {
            addLeftButtonRow(parent, modal, buttonHeight, gap, leftSpecs, bindings)
            addRightButton(parent, modal, buttonHeight, right, bindings)
        }
    }
}
