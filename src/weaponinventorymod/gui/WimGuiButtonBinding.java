package weaponinventorymod.gui;

import com.fs.starfarer.api.ui.ButtonAPI;

final class WimGuiButtonBinding<A> {
    private final ButtonAPI button;
    private final A action;

    WimGuiButtonBinding(ButtonAPI button, A action) {
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

    void clear() {
        if (button != null) {
            button.setChecked(false);
        }
    }

    A getAction() {
        return action;
    }
}
