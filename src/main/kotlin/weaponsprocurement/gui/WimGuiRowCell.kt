package weaponsprocurement.gui

import com.fs.starfarer.api.ui.Alignment
import java.awt.Color
import java.util.ArrayList

class WimGuiRowCell<A> private constructor(
    private val label: String?,
    private val width: Float,
    private val fillColor: Color?,
    private val textColor: Color?,
    private val action: A?,
    private val enabled: Boolean,
    private val alignment: Alignment?,
    private val borderOverride: Color?,
    private val hasBorderOverride: Boolean,
    private val tooltip: String?,
) {
    fun getLabel(): String? = label
    fun getWidth(): Float = width
    fun getFillColor(): Color? = fillColor
    fun getTextColor(): Color? = textColor
    fun getAction(): A? = action
    fun isEnabled(): Boolean = enabled
    fun isAction(): Boolean = action != null
    fun getAlignment(): Alignment = alignment ?: Alignment.MID
    fun borderColor(defaultBorder: Color?): Color? = if (hasBorderOverride) borderOverride else defaultBorder
    fun getTooltip(): String? = tooltip

    companion object {
        @JvmStatic
        fun <A> info(label: String?, width: Float, fillColor: Color?, textColor: Color?): WimGuiRowCell<A> =
            info(label, width, fillColor, textColor, Alignment.MID)

        @JvmStatic
        fun <A> info(
            label: String?,
            width: Float,
            fillColor: Color?,
            textColor: Color?,
            alignment: Alignment?,
        ): WimGuiRowCell<A> = info(label, width, fillColor, textColor, alignment, null)

        @JvmStatic
        fun <A> info(
            label: String?,
            width: Float,
            fillColor: Color?,
            textColor: Color?,
            alignment: Alignment?,
            tooltip: String?,
        ): WimGuiRowCell<A> = WimGuiRowCell(label, width, fillColor, textColor, null, true, alignment, null, false, tooltip)

        @JvmStatic
        fun <A> infoWithBorder(
            label: String?,
            width: Float,
            fillColor: Color?,
            textColor: Color?,
            alignment: Alignment?,
            borderColor: Color?,
        ): WimGuiRowCell<A> = infoWithBorder(label, width, fillColor, textColor, alignment, borderColor, null)

        @JvmStatic
        fun <A> infoWithBorder(
            label: String?,
            width: Float,
            fillColor: Color?,
            textColor: Color?,
            alignment: Alignment?,
            borderColor: Color?,
            tooltip: String?,
        ): WimGuiRowCell<A> =
            WimGuiRowCell(label, width, fillColor, textColor, null, true, alignment, borderColor, true, tooltip)

        @JvmStatic
        fun <A> action(
            label: String?,
            width: Float,
            fillColor: Color?,
            enabledTextColor: Color?,
            disabledTextColor: Color?,
            action: A,
            enabled: Boolean,
        ): WimGuiRowCell<A> = action(label, width, fillColor, enabledTextColor, disabledTextColor, action, enabled, null)

        @JvmStatic
        fun <A> action(
            label: String?,
            width: Float,
            fillColor: Color?,
            enabledTextColor: Color?,
            disabledTextColor: Color?,
            action: A,
            enabled: Boolean,
            tooltip: String?,
        ): WimGuiRowCell<A> =
            WimGuiRowCell(label, width, fillColor, if (enabled) enabledTextColor else disabledTextColor, action, enabled, Alignment.MID, null, false, tooltip)

        @JvmStatic
        fun <A> standardAction(label: String?, width: Float, fillColor: Color?, action: A, enabled: Boolean): WimGuiRowCell<A> =
            standardAction(label, width, fillColor, action, enabled, null)

        @JvmStatic
        fun <A> standardAction(
            label: String?,
            width: Float,
            fillColor: Color?,
            action: A,
            enabled: Boolean,
            tooltip: String?,
        ): WimGuiRowCell<A> = action(
            label,
            width,
            fillColor,
            WimGuiStyle.WHITE_TEXT,
            WimGuiStyle.DISABLED_TEXT,
            action,
            enabled,
            tooltip,
        )

        @JvmStatic
        @SafeVarargs
        fun <A> of(vararg cells: WimGuiRowCell<A>?): List<WimGuiRowCell<A>> {
            val result = ArrayList<WimGuiRowCell<A>>()
            for (cell in cells) {
                if (cell != null) {
                    result.add(cell)
                }
            }
            return result
        }

        @JvmStatic
        fun totalWidth(cells: List<WimGuiRowCell<*>>?, gap: Float): Float {
            if (cells.isNullOrEmpty()) {
                return 0f
            }
            var result = 0f
            for (i in cells.indices) {
                if (i > 0) {
                    result += gap
                }
                result += cells[i].getWidth()
            }
            return result
        }
    }
}
