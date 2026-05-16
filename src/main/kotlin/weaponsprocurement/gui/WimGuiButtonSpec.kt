package weaponsprocurement.gui

import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import java.awt.Color

class WimGuiButtonSpec<A>(
    @JvmField val width: Float,
    @JvmField val label: String?,
    @JvmField val textColor: Color,
    @JvmField val action: A,
    @JvmField val enabled: Boolean,
    @JvmField val alignment: Alignment,
    @JvmField val colors: WimGuiButtonColors,
    @JvmField val borderColor: Color,
    @JvmField val tooltip: String?,
    @JvmField val tooltipCreator: TooltipMakerAPI.TooltipCreator? = null,
) {
    companion object {
        @JvmStatic
        fun <A> sameColor(
            width: Float,
            label: String?,
            textColor: Color,
            action: A,
            enabled: Boolean,
            alignment: Alignment,
            color: Color,
            borderColor: Color,
        ): WimGuiButtonSpec<A> = dimmedInner(width, label, textColor, action, enabled, alignment, color, borderColor)

        @JvmStatic
        fun <A> sameColor(
            width: Float,
            label: String?,
            textColor: Color,
            action: A,
            enabled: Boolean,
            alignment: Alignment,
            color: Color,
            borderColor: Color,
            tooltip: String?,
        ): WimGuiButtonSpec<A> = dimmedInner(width, label, textColor, action, enabled, alignment, color, borderColor, tooltip)

        @JvmStatic
        fun <A> dimmedInner(
            width: Float,
            label: String?,
            textColor: Color,
            action: A,
            enabled: Boolean,
            alignment: Alignment,
            color: Color,
            borderColor: Color,
        ): WimGuiButtonSpec<A> = dimmedInner(width, label, textColor, action, enabled, alignment, color, borderColor, null)

        @JvmStatic
        fun <A> dimmedInner(
            width: Float,
            label: String?,
            textColor: Color,
            action: A,
            enabled: Boolean,
            alignment: Alignment,
            color: Color,
            borderColor: Color,
            tooltip: String?,
        ): WimGuiButtonSpec<A> = WimGuiButtonSpec(
            width,
            label,
            textColor,
            action,
            enabled,
            alignment,
            WimGuiButtonColors.dimmedInner(color),
            borderColor,
            tooltip,
        )

        @JvmStatic
        fun <A> toggle(
            width: Float,
            label: String?,
            textColor: Color,
            action: A,
            alignment: Alignment,
            color: Color,
            borderColor: Color,
        ): WimGuiButtonSpec<A> = toggle(width, label, textColor, action, alignment, color, borderColor, null)

        @JvmStatic
        fun <A> toggle(
            width: Float,
            label: String?,
            textColor: Color,
            action: A,
            alignment: Alignment,
            color: Color,
            borderColor: Color,
            tooltip: String?,
        ): WimGuiButtonSpec<A> = dimmedInner(width, label, textColor, action, true, alignment, color, borderColor, tooltip)

        @JvmStatic
        fun <A> toggle(
            width: Float,
            label: String?,
            textColor: Color,
            action: A,
            alignment: Alignment,
            color: Color,
            borderColor: Color,
            tooltip: String?,
            tooltipCreator: TooltipMakerAPI.TooltipCreator?,
        ): WimGuiButtonSpec<A> = WimGuiButtonSpec(
            width,
            label,
            textColor,
            action,
            true,
            alignment,
            WimGuiButtonColors.dimmedInner(color),
            borderColor,
            tooltip,
            tooltipCreator,
        )

        @JvmStatic
        fun <A> semantic(
            width: Float,
            label: String?,
            action: A,
            enabled: Boolean,
            enabledFill: Color,
            borderColor: Color,
        ): WimGuiButtonSpec<A> = semantic(width, label, action, enabled, enabledFill, borderColor, null)

        @JvmStatic
        fun <A> semantic(
            width: Float,
            label: String?,
            action: A,
            enabled: Boolean,
            enabledFill: Color,
            borderColor: Color,
            tooltip: String?,
        ): WimGuiButtonSpec<A> = sameColor(
            width,
            label,
            if (enabled) WimGuiStyle.WHITE_TEXT else WimGuiStyle.DISABLED_TEXT,
            action,
            enabled,
            Alignment.MID,
            if (enabled) enabledFill else WimGuiStyle.DISABLED_BACKGROUND,
            borderColor,
            tooltip,
        )
    }
}
