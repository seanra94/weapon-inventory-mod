package weaponsprocurement.ui

import java.awt.Color

class WimGuiSemanticButtonFactory<A>(private val borderColor: Color) {
    fun button(width: Float, label: String?, action: A, enabled: Boolean, fillColor: Color): WimGuiButtonSpec<A> =
        button(width, label, action, enabled, fillColor, null)

    fun button(
        width: Float,
        label: String?,
        action: A,
        enabled: Boolean,
        fillColor: Color,
        tooltip: String?,
    ): WimGuiButtonSpec<A> = WimGuiButtonSpec.semantic(width, label, action, enabled, fillColor, borderColor, tooltip)

    fun enabledButton(width: Float, label: String?, action: A, fillColor: Color): WimGuiButtonSpec<A> =
        button(width, label, action, true, fillColor)

    fun enabledButton(
        width: Float,
        label: String?,
        action: A,
        fillColor: Color,
        tooltip: String?,
    ): WimGuiButtonSpec<A> = button(width, label, action, true, fillColor, tooltip)
}
