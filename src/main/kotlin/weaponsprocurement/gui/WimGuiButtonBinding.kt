package weaponsprocurement.gui

import com.fs.starfarer.api.ui.ButtonAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.PositionAPI

class WimGuiButtonBinding<A>(
    private val panel: CustomPanelAPI?,
    private val button: ButtonAPI?,
    private val action: A,
) {
    fun consumeIfPressed(): Boolean {
        if (button == null || !button.isChecked) {
            return false
        }
        clear()
        return true
    }

    fun consumeIfClicked(x: Float, y: Float): Boolean {
        if (panel == null || panel.position == null || !contains(panel.position, x, y)) {
            return false
        }
        clear()
        return true
    }

    fun clear() {
        button?.isChecked = false
    }

    fun getAction(): A = action

    private fun contains(position: PositionAPI, x: Float, y: Float): Boolean {
        return x >= position.x &&
            x <= position.x + position.width &&
            y >= position.y &&
            y <= position.y + position.height
    }
}
