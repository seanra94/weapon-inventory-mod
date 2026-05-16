package weaponsprocurement.gui

class WimGuiScrollSlice<T>(
    @JvmField val offset: Int,
    @JvmField val items: List<T>,
    @JvmField val hasAbove: Boolean,
    @JvmField val hasBelow: Boolean,
    @JvmField val maxOffset: Int,
)
