package weaponsprocurement.gui

import com.fs.starfarer.api.ui.CustomPanelAPI

class WimGuiModalListRenderer private constructor() {
    fun interface ScrollRowFactory<A> {
        fun createScrollRow(label: String, scrollDelta: Int): WimGuiListRow<A>
    }

    fun interface ExtraGapProvider<A> {
        fun extraGapBefore(row: WimGuiListRow<A>): Float
    }

    companion object {
        @JvmStatic
        fun <A> render(
            root: CustomPanelAPI,
            rows: List<WimGuiListRow<A>>,
            requestedOffset: Int,
            spec: WimGuiModalListSpec,
            scrollRowFactory: ScrollRowFactory<A>,
            extraGapProvider: ExtraGapProvider<A>?,
            buttons: MutableList<WimGuiButtonBinding<A>>,
        ): WimGuiModalListRenderResult {
            val layout = WimGuiModalListLayout.compute(
                rows,
                requestedOffset,
                spec.panelTop,
                spec.panelHeight,
                spec.panelWidth,
                spec.modal,
                gapProvider(extraGapProvider),
            )
            val slice = layout.scrollSlice
            val listPanel = root.createCustomPanel(
                spec.panelWidth,
                layout.panelHeight,
                WimGuiPanelPlugin(spec.panelFill, spec.panelBorder),
            )
            root.addComponent(listPanel).inTL(spec.panelLeft, layout.panelTop)

            var y = spec.rowHorizontalPad + finalPageTopOffset(slice, layout.panelHeight, spec, extraGapProvider)
            if (slice.hasAbove) {
                renderRow(listPanel, scrollRowFactory.createScrollRow(WimGuiScrollIndicator.ABOVE, -layout.pageDelta()), y, spec, buttons)
                y += spec.rowHeight + spec.rowGap
            }
            for (row in slice.items) {
                y += gapBefore(row, extraGapProvider)
                renderRow(listPanel, row, y, spec, buttons)
                y += spec.rowHeight + spec.rowGap
            }
            if (slice.hasBelow) {
                renderRow(listPanel, scrollRowFactory.createScrollRow(WimGuiScrollIndicator.BELOW, layout.pageDelta()), y, spec, buttons)
            }
            return WimGuiModalListRenderResult(
                WimGuiListBounds(layout.maxOffset, spec.panelLeft, layout.panelTop, spec.panelWidth, layout.panelHeight),
                slice.offset,
            )
        }

        @JvmStatic
        fun <A> renderAndStoreOffset(
            root: CustomPanelAPI,
            rows: List<WimGuiListRow<A>>,
            state: WimGuiScrollableListState?,
            spec: WimGuiModalListSpec,
            scrollRowFactory: ScrollRowFactory<A>,
            extraGapProvider: ExtraGapProvider<A>?,
            buttons: MutableList<WimGuiButtonBinding<A>>,
        ): WimGuiListBounds {
            val result = render(root, rows, state?.getListScrollOffset() ?: 0, spec, scrollRowFactory, extraGapProvider, buttons)
            state?.setListScrollOffset(result.offset)
            return result.bounds
        }

        private fun <A> renderRow(
            listPanel: CustomPanelAPI,
            row: WimGuiListRow<A>,
            y: Float,
            spec: WimGuiModalListSpec,
            buttons: MutableList<WimGuiButtonBinding<A>>,
        ) {
            WimGuiListRowRenderer.renderRow(
                listPanel,
                row,
                y,
                spec.rowHeight,
                spec.actionHeight,
                spec.rowHorizontalPad,
                spec.buttonGap,
                spec.textLeftPad,
                spec.minLabelWidth,
                spec.rowBorder,
                buttons,
            )
        }

        private fun <A> finalPageTopOffset(
            slice: WimGuiScrollSlice<WimGuiListRow<A>>?,
            panelHeight: Float,
            spec: WimGuiModalListSpec,
            extraGapProvider: ExtraGapProvider<A>?,
        ): Float {
            if (slice == null || !slice.hasAbove || slice.hasBelow || slice.items.isEmpty()) {
                return 0f
            }
            var contentBottom = spec.rowHeight
            for (row in slice.items) {
                contentBottom += spec.rowGap + gapBefore(row, extraGapProvider) + spec.rowHeight
            }
            val innerHeight = panelHeight - 2f * spec.rowHorizontalPad
            return Math.max(0f, innerHeight - contentBottom)
        }

        private fun <A> gapBefore(row: WimGuiListRow<A>, extraGapProvider: ExtraGapProvider<A>?): Float =
            if (extraGapProvider == null) 0f else Math.max(0f, extraGapProvider.extraGapBefore(row))

        private fun <A> gapProvider(extraGapProvider: ExtraGapProvider<A>?): WimGuiScroll.ExtraGapProvider<WimGuiListRow<A>>? {
            if (extraGapProvider == null) {
                return null
            }
            return WimGuiScroll.ExtraGapProvider { row -> extraGapProvider.extraGapBefore(row) }
        }
    }
}
