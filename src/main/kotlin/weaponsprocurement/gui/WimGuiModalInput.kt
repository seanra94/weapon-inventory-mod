package weaponsprocurement.gui

import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import org.lwjgl.input.Keyboard

class WimGuiModalInput<A>(buttonPollFramesAfterMouseEvent: Int) {
    private val buttonPoller = WimGuiButtonPoller<A>(buttonPollFramesAfterMouseEvent)
    private var root: CustomPanelAPI? = null
    private var listBounds: WimGuiListBounds? = null

    fun setRoot(root: CustomPanelAPI?) {
        this.root = root
    }

    fun setListBounds(listBounds: WimGuiListBounds?) {
        this.listBounds = listBounds
    }

    fun buttonBindings(): MutableList<WimGuiButtonBinding<A>> = buttonPoller.bindings()

    fun clearButtonBindings() {
        buttonPoller.clearBindings()
    }

    fun clearCheckedButtons() {
        buttonPoller.clearCheckedButtons()
    }

    fun suppressNextMouseUpPoll() {
        buttonPoller.suppressNextMouseUp()
    }

    fun processInput(events: List<InputEventAPI>?): WimGuiInputResult {
        if (events == null) {
            return WimGuiInputResult.none()
        }
        for (event in events) {
            if (event.isConsumed) {
                continue
            }
            if (event.isKeyDownEvent && event.eventValue == Keyboard.KEY_ESCAPE) {
                event.consume()
                return WimGuiInputResult.closeRequested()
            }
            if (event.isMouseScrollEvent && event.eventValue != 0 && isMouseInList(event)) {
                val delta = WimGuiScroll.wheelDelta(event.eventValue)
                event.consume()
                return WimGuiInputResult.scroll(delta)
            }
        }
        buttonPoller.requestPollFromInput(events)
        return WimGuiInputResult.none()
    }

    fun processButtonsIfRequested(handler: WimGuiButtonPoller.ActionHandler<A>?): Boolean =
        buttonPoller.processIfRequested(handler)

    private fun isMouseInList(event: InputEventAPI): Boolean = listBounds?.contains(root, event) == true
}
