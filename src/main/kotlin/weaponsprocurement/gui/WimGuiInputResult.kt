package weaponsprocurement.gui

class WimGuiInputResult private constructor(
    private val closeRequested: Boolean,
    private val scrollDelta: Int,
) {
    fun isCloseRequested(): Boolean = closeRequested

    fun hasScrollDelta(): Boolean = scrollDelta != 0

    fun getScrollDelta(): Int = scrollDelta

    companion object {
        private val NONE = WimGuiInputResult(false, 0)
        private val CLOSE = WimGuiInputResult(true, 0)

        @JvmStatic
        fun none(): WimGuiInputResult = NONE

        @JvmStatic
        fun closeRequested(): WimGuiInputResult = CLOSE

        @JvmStatic
        fun scroll(delta: Int): WimGuiInputResult = if (delta == 0) NONE else WimGuiInputResult(false, delta)
    }
}
