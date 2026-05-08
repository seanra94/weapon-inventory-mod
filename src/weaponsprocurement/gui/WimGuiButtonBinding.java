package weaponsprocurement.gui;

import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;

final class WimGuiButtonBinding<A> {
    private final CustomPanelAPI panel;
    private final ButtonAPI button;
    private final A action;

    WimGuiButtonBinding(CustomPanelAPI panel, ButtonAPI button, A action) {
        this.panel = panel;
        this.button = button;
        this.action = action;
    }

    boolean consumeIfPressed() {
        if (button == null || !button.isChecked()) {
            return false;
        }
        clear();
        return true;
    }

    boolean consumeIfClicked(float x, float y) {
        if (panel == null || panel.getPosition() == null || !contains(panel.getPosition(), x, y)) {
            return false;
        }
        clear();
        return true;
    }

    void clear() {
        if (button != null) {
            button.setChecked(false);
        }
    }

    A getAction() {
        return action;
    }

    private static boolean contains(PositionAPI position, float x, float y) {
        return x >= position.getX()
                && x <= position.getX() + position.getWidth()
                && y >= position.getY()
                && y <= position.getY() + position.getHeight();
    }
}
