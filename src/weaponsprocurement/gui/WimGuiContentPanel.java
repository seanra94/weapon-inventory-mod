package weaponsprocurement.gui;

import com.fs.starfarer.api.ui.CustomPanelAPI;

final class WimGuiContentPanel {
    private CustomPanelAPI content;

    CustomPanelAPI begin(CustomPanelAPI root, float width, float height) {
        if (root == null) {
            return null;
        }
        if (content != null) {
            root.removeComponent(content);
        }
        content = root.createCustomPanel(width, height, null);
        return content;
    }

    void attach(CustomPanelAPI root) {
        if (root != null && content != null) {
            root.addComponent(content).inTL(0f, 0f);
        }
    }

}
