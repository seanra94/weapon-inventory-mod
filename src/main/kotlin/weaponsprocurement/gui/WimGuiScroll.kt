package weaponsprocurement.gui

import java.util.ArrayList

class WimGuiScroll private constructor() {
    fun interface ExtraGapProvider<T> {
        fun extraGapBefore(item: T): Float
    }

    companion object {
        const val DEFAULT_WHEEL_STEP = 3

        @JvmStatic
        fun wheelDelta(eventValue: Int): Int {
            if (eventValue == 0) {
                return 0
            }
            return if (eventValue > 0) -DEFAULT_WHEEL_STEP else DEFAULT_WHEEL_STEP
        }

        @JvmStatic
        fun verticalSlotCount(containerHeight: Float, itemHeight: Float, verticalGap: Float): Int {
            val perRow = itemHeight + verticalGap
            return Math.max(1, Math.floor(((containerHeight + verticalGap) / perRow).toDouble()).toInt())
        }

        @JvmStatic
        fun usefulOffset(requestedOffset: Int, maxOffset: Int): Int = usefulOffset(requestedOffset, maxOffset, 0)

        @JvmStatic
        fun usefulOffset(requestedOffset: Int, maxOffset: Int, scrollDelta: Int): Int =
            Math.max(0, Math.min(requestedOffset, Math.max(0, maxOffset)))

        @JvmStatic
        fun usefulOffsetByDelta(currentOffset: Int, scrollDelta: Int, maxOffset: Int): Int =
            usefulOffset(currentOffset + scrollDelta, maxOffset, scrollDelta)

        @JvmStatic
        fun <T> verticalSlice(
            rows: List<T>?,
            requestedOffset: Int,
            containerHeight: Float,
            rowHeight: Float,
            rowGap: Float,
        ): WimGuiScrollSlice<T> = verticalSlice(rows, requestedOffset, containerHeight, rowHeight, rowGap, null)

        @JvmStatic
        fun <T> verticalSlice(
            rows: List<T>?,
            requestedOffset: Int,
            containerHeight: Float,
            rowHeight: Float,
            rowGap: Float,
            extraGapProvider: ExtraGapProvider<T>?,
        ): WimGuiScrollSlice<T> {
            val safeRows = rows ?: ArrayList<T>()
            val itemCount = safeRows.size
            if (itemCount <= 0) {
                return WimGuiScrollSlice(0, safeRows, false, false, 0)
            }
            if (fits(safeRows, 0, itemCount, false, false, containerHeight, rowHeight, rowGap, extraGapProvider)) {
                return WimGuiScrollSlice(0, safeRows, false, false, 0)
            }

            val maxOffset = bottomOffset(safeRows, itemCount, containerHeight, rowHeight, rowGap, extraGapProvider)
            val offset = usefulOffset(requestedOffset, maxOffset)
            val hasAbove = offset > 0
            var visibleSlots = visibleCount(
                safeRows,
                offset,
                itemCount,
                hasAbove,
                false,
                containerHeight,
                rowHeight,
                rowGap,
                extraGapProvider,
            )
            var hasBelow = offset + visibleSlots < itemCount
            if (hasBelow) {
                visibleSlots = visibleCount(
                    safeRows,
                    offset,
                    itemCount,
                    hasAbove,
                    true,
                    containerHeight,
                    rowHeight,
                    rowGap,
                    extraGapProvider,
                )
            }
            visibleSlots = Math.max(1, visibleSlots)

            val end = Math.min(itemCount, offset + visibleSlots)
            hasBelow = end < itemCount
            return WimGuiScrollSlice(offset, ArrayList(safeRows.subList(offset, end)), hasAbove, hasBelow, maxOffset)
        }

        private fun <T> fits(
            rows: List<T>,
            start: Int,
            end: Int,
            hasAbove: Boolean,
            hasBelow: Boolean,
            containerHeight: Float,
            rowHeight: Float,
            rowGap: Float,
            extraGapProvider: ExtraGapProvider<T>?,
        ): Boolean {
            return visibleCount(
                rows,
                start,
                end,
                hasAbove,
                hasBelow,
                containerHeight,
                rowHeight,
                rowGap,
                extraGapProvider,
            ) >= end - start
        }

        private fun <T> visibleCount(
            rows: List<T>,
            start: Int,
            end: Int,
            hasAbove: Boolean,
            reserveBelow: Boolean,
            containerHeight: Float,
            rowHeight: Float,
            rowGap: Float,
            extraGapProvider: ExtraGapProvider<T>?,
        ): Int {
            var y = 0f
            if (hasAbove) {
                if (rowHeight > containerHeight) {
                    return 0
                }
                y += rowHeight + rowGap
            }
            var visible = 0
            for (i in start until end) {
                val extraGap = if (extraGapProvider == null) 0f else Math.max(0f, extraGapProvider.extraGapBefore(rows[i]))
                val rowTop = y + extraGap
                var requiredBottom = rowTop + rowHeight
                if (reserveBelow) {
                    requiredBottom += rowGap + rowHeight
                }
                if (requiredBottom > containerHeight + 0.01f) {
                    break
                }
                visible++
                y = rowTop + rowHeight + rowGap
            }
            return visible
        }

        private fun <T> bottomOffset(
            rows: List<T>,
            itemCount: Int,
            containerHeight: Float,
            rowHeight: Float,
            rowGap: Float,
            extraGapProvider: ExtraGapProvider<T>?,
        ): Int {
            var bottomOffset = Math.max(0, itemCount - 1)
            for (candidate in itemCount - 1 downTo 0) {
                if (!fits(rows, candidate, itemCount, candidate > 0, false, containerHeight, rowHeight, rowGap, extraGapProvider)) {
                    break
                }
                bottomOffset = candidate
            }
            return bottomOffset
        }
    }
}
