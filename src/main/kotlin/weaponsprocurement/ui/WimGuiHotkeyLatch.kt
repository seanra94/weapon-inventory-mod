package weaponsprocurement.ui

import org.lwjgl.input.Keyboard

class WimGuiHotkeyLatch(private val keyCode: Int) {
    private var wasDown = false

    fun consumePress(): Boolean {
        val down = Keyboard.isKeyDown(keyCode)
        if (!down) {
            wasDown = false
            return false
        }
        if (wasDown) return false
        wasDown = true
        return true
    }
}
