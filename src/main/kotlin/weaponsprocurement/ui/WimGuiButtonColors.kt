package weaponsprocurement.ui

import java.awt.Color

class WimGuiButtonColors(
    @JvmField val idle: Color,
    hover: Color?,
) {
    @JvmField
    val hover: Color = hover ?: idle

    companion object {
        @JvmStatic
        fun dimmedInner(color: Color): WimGuiButtonColors = WimGuiButtonColors(color, color)

        @JvmStatic
        fun same(color: Color): WimGuiButtonColors = dimmedInner(color)
    }
}
