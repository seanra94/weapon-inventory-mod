package weaponinventorymod.gui;

import org.lwjgl.input.Keyboard;

final class WimGuiHotkeyLatch {
    private final int keyCode;
    private boolean wasDown = false;

    WimGuiHotkeyLatch(int keyCode) {
        this.keyCode = keyCode;
    }

    boolean consumePress() {
        boolean down = Keyboard.isKeyDown(keyCode);
        if (!down) {
            wasDown = false;
            return false;
        }
        if (wasDown) {
            return false;
        }
        wasDown = true;
        return true;
    }
}
