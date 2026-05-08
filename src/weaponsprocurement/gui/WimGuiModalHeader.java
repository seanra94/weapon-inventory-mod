package weaponsprocurement.gui;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;

import java.awt.Color;

final class WimGuiModalHeader {
    private WimGuiModalHeader() {
    }

    static CustomPanelAPI addTitleStatusHeader(CustomPanelAPI parent,
                                               WimGuiModalLayout modal,
                                               float headerHeight,
                                               String title,
                                               String status,
                                               Color fill,
                                               Color border,
                                               Color textColor) {
        CustomPanelAPI header = parent.createCustomPanel(
                modal.contentWidth(),
                headerHeight,
                new WimGuiPanelPlugin(fill, border));
        parent.addComponent(header).inTL(modal.padding, modal.headingTop());

        float textWidth = Math.max(1f, header.getPosition().getWidth() - 2f * modal.padding);
        WimGuiControls.addLabel(header, title, textColor,
                modal.padding, 2f, textWidth, 22f, Alignment.LMID);
        if (status != null && !status.isEmpty()) {
            WimGuiControls.addLabel(header, status, textColor,
                    modal.padding, 28f, textWidth, Math.max(18f, headerHeight - 30f), Alignment.LMID);
        }
        return header;
    }
}
