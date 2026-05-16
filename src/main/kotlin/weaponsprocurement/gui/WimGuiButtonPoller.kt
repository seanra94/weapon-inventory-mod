package weaponsprocurement.gui

import com.fs.starfarer.api.input.InputEventAPI
import java.util.ArrayList

class WimGuiButtonPoller<A>(framesAfterMouseEvent: Int) {
    fun interface ActionHandler<A> {
        fun handle(action: A)
    }

    private val bindings: MutableList<WimGuiButtonBinding<A>> = ArrayList()
    private val framesAfterMouseEvent: Int = Math.max(1, framesAfterMouseEvent)
    private var framesRemaining = 0
    private var hasPendingMouseUp = false
    private var suppressNextMouseUp = false
    private var pendingMouseUpX = 0f
    private var pendingMouseUpY = 0f

    fun bindings(): MutableList<WimGuiButtonBinding<A>> = bindings

    fun clearBindings() {
        bindings.clear()
        framesRemaining = 0
        clearPendingMouseUp()
    }

    fun requestPollFromInput(events: List<InputEventAPI>?) {
        if (events == null) {
            return
        }
        for (event in events) {
            if (event.isMouseUpEvent) {
                if (suppressNextMouseUp) {
                    suppressNextMouseUp = false
                    continue
                }
                hasPendingMouseUp = true
                pendingMouseUpX = event.x.toFloat()
                pendingMouseUpY = event.y.toFloat()
                framesRemaining = framesAfterMouseEvent
                return
            }
            if (event.isMouseDownEvent) {
                framesRemaining = framesAfterMouseEvent
                return
            }
        }
    }

    fun processIfRequested(handler: ActionHandler<A>?): Boolean {
        if (framesRemaining <= 0 || handler == null) {
            return false
        }
        framesRemaining--
        if (hasPendingMouseUp) {
            val x = pendingMouseUpX
            val y = pendingMouseUpY
            clearPendingMouseUp()
            for (binding in bindings) {
                if (!binding.consumeIfClicked(x, y)) {
                    continue
                }
                handler.handle(binding.getAction())
                return true
            }
        }
        for (binding in bindings) {
            if (!binding.consumeIfPressed()) {
                continue
            }
            handler.handle(binding.getAction())
            return true
        }
        return false
    }

    fun clearCheckedButtons() {
        for (binding in bindings) {
            binding.clear()
        }
        framesRemaining = 0
        clearPendingMouseUp()
    }

    fun suppressNextMouseUp() {
        suppressNextMouseUp = true
        framesRemaining = 0
        clearPendingMouseUp()
    }

    private fun clearPendingMouseUp() {
        hasPendingMouseUp = false
        pendingMouseUpX = 0f
        pendingMouseUpY = 0f
    }
}
