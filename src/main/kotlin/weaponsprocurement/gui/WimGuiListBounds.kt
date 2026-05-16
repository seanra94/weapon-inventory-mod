package weaponsprocurement.gui

import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.CustomPanelAPI

class WimGuiListBounds(
    private val maxScrollOffset: Int,
    private val left: Float,
    private val top: Float,
    private val width: Float,
    private val height: Float,
) {
    fun getMaxScrollOffset(): Int = maxScrollOffset

    fun contains(root: CustomPanelAPI?, event: InputEventAPI?): Boolean {
        if (root == null || root.position == null || event == null || maxScrollOffset <= 0) {
            return false
        }
        val screenLeft = root.position.x + left
        val screenRight = screenLeft + width
        val screenTop = root.position.y + root.position.height - top
        val screenBottom = screenTop - height
        return event.x >= screenLeft &&
            event.x <= screenRight &&
            event.y >= screenBottom &&
            event.y <= screenTop
    }
}
