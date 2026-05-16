package weaponsprocurement.ui

class WimGuiModalListLayout<T> private constructor(
    @JvmField val scrollSlice: WimGuiScrollSlice<T>,
    @JvmField val panelTop: Float,
    @JvmField val panelHeight: Float,
    @JvmField val innerWidth: Float,
    @JvmField val maxOffset: Int,
) {
    fun pageDelta(): Int = Math.max(1, scrollSlice.items.size)

    companion object {
        @JvmStatic
        fun <T> compute(
            rows: List<T>?,
            requestedOffset: Int,
            bodyTop: Float,
            bodyHeight: Float,
            panelWidth: Float,
            modal: WimGuiModalLayout,
        ): WimGuiModalListLayout<T> = compute(rows, requestedOffset, bodyTop, bodyHeight, panelWidth, modal, null)

        @JvmStatic
        fun <T> compute(
            rows: List<T>?,
            requestedOffset: Int,
            bodyTop: Float,
            bodyHeight: Float,
            panelWidth: Float,
            modal: WimGuiModalLayout,
            extraGapProvider: WimGuiScroll.ExtraGapProvider<T>?,
        ): WimGuiModalListLayout<T> {
            val availablePanelHeight = Math.max(modal.rowHeight, bodyHeight)
            val availableInnerHeight = Math.max(modal.rowHeight, availablePanelHeight - 2f * modal.listInset)
            val slice = WimGuiScroll.verticalSlice(
                rows,
                requestedOffset,
                availableInnerHeight,
                modal.rowHeight,
                modal.rowGap,
                extraGapProvider,
            )
            val renderedPanelHeight = availablePanelHeight
            val panelTop = bodyTop
            val innerWidth = Math.max(1f, panelWidth - 2f * modal.listInset)
            return WimGuiModalListLayout(slice, panelTop, renderedPanelHeight, innerWidth, slice.maxOffset)
        }
    }
}
