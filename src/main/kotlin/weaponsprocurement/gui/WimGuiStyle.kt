package weaponsprocurement.gui

import java.awt.Color

class WimGuiStyle private constructor() {
    companion object {
        const val BUTTON_IDLE_DIM_MULT = 0.55f
        const val TEXT_TOP_PAD = 3f
        const val TEXT_LEFT_PAD = 8f
        const val TEXT_APPROX_CHAR_WIDTH = 6.8f
        const val TEXT_LINE_HEIGHT = 15f
        const val TEXT_VERTICAL_PADDING = 8f

        @JvmField var DEFAULT_TEXT: Color = Color(220, 220, 220)
        @JvmField var WHITE_TEXT: Color = Color.WHITE
        @JvmField var UNCOLOURED_BUTTON: Color = Color(0, 0, 0, 225)
        @JvmField var DISABLED_BACKGROUND: Color = Color(28, 28, 28, 235)
        @JvmField var DISABLED_DARK: Color = Color(18, 18, 18, 235)
        @JvmField var DISABLED_TEXT: Color = Color(120, 120, 120)
        @JvmField var DISABLED_BORDER: Color = Color(95, 95, 95)
        @JvmField var ALERT_RED: Color = Color(245, 95, 85, 240)
        @JvmField var SAVE_PURPLE: Color = Color(146, 126, 160, 225)
        @JvmField var LOAD_YELLOW: Color = Color(188, 178, 86, 225)
        @JvmField var CONFIRM_GREEN: Color = Color(144, 180, 148, 225)
        @JvmField var CANCEL_RED: Color = Color(218, 142, 140, 225)
        @JvmField var PRESET_SCOPE_ORANGE: Color = Color(202, 146, 112, 225)
        @JvmField var SELECTED_BLUE: Color = Color(52, 78, 88, 225)
        @JvmField var PANEL_HEADING: Color = Color(40, 40, 40, 225)
        @JvmField var STALE_HEADING: Color = Color(120, 120, 120, 225)
        @JvmField var COLLAPSIBLE_HEADING: Color = Color(80, 80, 80, 225)
        @JvmField var UNSAVED_HEADING: Color = Color(220, 220, 220, 225)
        @JvmField var TAG_CATEGORY_HEADING: Color = Color(210, 165, 185, 225)
        @JvmField var MODAL_PANEL_BACKGROUND: Color = Color(0, 0, 0, 220)
        @JvmField var MODAL_PANEL_BORDER: Color = Color(210, 210, 210, 220)
        @JvmField var ROW_BORDER: Color = Color(210, 210, 210, 220)

        @JvmStatic
        fun dimForIdle(color: Color?): Color {
            if (color == null) {
                return DISABLED_DARK
            }
            return Color(
                clampColor(Math.round(color.red * BUTTON_IDLE_DIM_MULT)),
                clampColor(Math.round(color.green * BUTTON_IDLE_DIM_MULT)),
                clampColor(Math.round(color.blue * BUTTON_IDLE_DIM_MULT)),
                color.alpha,
            )
        }

        private fun clampColor(value: Int): Int = Math.max(0, Math.min(255, value))
    }
}
