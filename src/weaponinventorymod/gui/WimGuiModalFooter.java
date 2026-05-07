package weaponinventorymod.gui;

import com.fs.starfarer.api.ui.CustomPanelAPI;

import java.util.List;

final class WimGuiModalFooter {
    private WimGuiModalFooter() {
    }

    static <A> void addLeftButtonRow(CustomPanelAPI parent,
                                     WimGuiModalLayout modal,
                                     float buttonHeight,
                                     float gap,
                                     List<WimGuiButtonSpec<A>> specs,
                                     List<WimGuiButtonBinding<A>> bindings) {
        WimGuiControls.addButtonRow(
                parent,
                modal.padding,
                modal.footerButtonY(buttonHeight),
                buttonHeight,
                gap,
                specs,
                bindings);
    }

    static <A> void addRightButton(CustomPanelAPI parent,
                                   WimGuiModalLayout modal,
                                   float buttonHeight,
                                   WimGuiButtonSpec<A> spec,
                                   List<WimGuiButtonBinding<A>> bindings) {
        WimGuiControls.addBoundButton(
                parent,
                modal.width - modal.padding - spec.width,
                modal.footerButtonY(buttonHeight),
                buttonHeight,
                spec,
                bindings);
    }

    static <A> void addEdgeButtons(CustomPanelAPI parent,
                                   WimGuiModalLayout modal,
                                   float buttonHeight,
                                   WimGuiButtonSpec<A> left,
                                   WimGuiButtonSpec<A> right,
                                   List<WimGuiButtonBinding<A>> bindings) {
        addLeftButtonRow(parent, modal, buttonHeight, modal.rowGap, WimGuiButtonSpecs.of(left), bindings);
        addRightButton(parent, modal, buttonHeight, right, bindings);
    }

    static <A> void addLeftRowAndRightButton(CustomPanelAPI parent,
                                             WimGuiModalLayout modal,
                                             float buttonHeight,
                                             float gap,
                                             List<WimGuiButtonSpec<A>> leftSpecs,
                                             WimGuiButtonSpec<A> right,
                                             List<WimGuiButtonBinding<A>> bindings) {
        addLeftButtonRow(parent, modal, buttonHeight, gap, leftSpecs, bindings);
        addRightButton(parent, modal, buttonHeight, right, bindings);
    }
}
