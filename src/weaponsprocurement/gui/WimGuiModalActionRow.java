package weaponsprocurement.gui;

import com.fs.starfarer.api.ui.CustomPanelAPI;

import java.util.List;

final class WimGuiModalActionRow {
    private WimGuiModalActionRow() {
    }

    static <A> void add(CustomPanelAPI parent,
                        WimGuiModalLayout modal,
                        float headerHeight,
                        float headerToActionGap,
                        float buttonHeight,
                        float buttonGap,
                        List<WimGuiButtonSpec<A>> specs,
                        List<WimGuiButtonBinding<A>> bindings) {
        float y = modal.actionRowY(headerHeight, headerToActionGap);
        WimGuiControls.addButtonRow(parent, modal.padding, y, buttonHeight, buttonGap, specs, bindings);
    }
}
