package weaponsprocurement.ui

class WimGuiModalLayout(
    @JvmField val width: Float,
    @JvmField val height: Float,
    @JvmField val padding: Float,
    @JvmField val sectionGap: Float,
    @JvmField val headingHeight: Float,
    @JvmField val footerHeight: Float,
    @JvmField val rowHeight: Float,
    @JvmField val rowGap: Float,
    @JvmField val listInset: Float,
) {
    fun bodyTop(): Float = padding + headingHeight + sectionGap

    fun headingTop(): Float = padding

    fun actionRowY(headerContentHeight: Float, gapAfterHeader: Float): Float =
        headingTop() + headerContentHeight + gapAfterHeader

    fun footerButtonY(buttonHeight: Float): Float = height - padding - buttonHeight

    fun bodyHeight(): Float = Math.max(rowHeight, height - bodyTop() - sectionGap - footerHeight - padding)

    fun contentWidth(): Float = width - 2f * padding

    fun listPanelHeight(rowCount: Int, hasAbove: Boolean, hasBelow: Boolean, extraGapHeight: Float): Float {
        val controlCount = Math.max(1, rowCount + (if (hasAbove) 1 else 0) + (if (hasBelow) 1 else 0))
        return verticalItemsHeight(controlCount, rowHeight, rowGap) + Math.max(0f, extraGapHeight) + 2f * listInset
    }

    fun verticalItemsHeight(count: Int, itemHeight: Float, gap: Float): Float {
        val safeCount = Math.max(1, count)
        return safeCount * itemHeight + Math.max(0, safeCount - 1) * gap
    }
}
