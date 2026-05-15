package weaponsprocurement.gui;

import java.util.List;

final class WimGuiModalListLayout<T> {
    final WimGuiScrollSlice<T> scrollSlice;
    final float panelTop;
    final float panelHeight;
    final float innerWidth;
    final int maxOffset;

    private WimGuiModalListLayout(WimGuiScrollSlice<T> scrollSlice,
                                  float panelTop,
                                  float panelHeight,
                                  float innerWidth,
                                  int maxOffset) {
        this.scrollSlice = scrollSlice;
        this.panelTop = panelTop;
        this.panelHeight = panelHeight;
        this.innerWidth = innerWidth;
        this.maxOffset = maxOffset;
    }

    int pageDelta() {
        return Math.max(1, scrollSlice.items.size());
    }

    static <T> WimGuiModalListLayout<T> compute(List<T> rows,
                                                int requestedOffset,
                                                float bodyTop,
                                                float bodyHeight,
                                                float panelWidth,
                                                WimGuiModalLayout modal) {
        return compute(rows, requestedOffset, bodyTop, bodyHeight, panelWidth, modal, null);
    }

    static <T> WimGuiModalListLayout<T> compute(List<T> rows,
                                                int requestedOffset,
                                                float bodyTop,
                                                float bodyHeight,
                                                float panelWidth,
                                                WimGuiModalLayout modal,
                                                WimGuiScroll.ExtraGapProvider<T> extraGapProvider) {
        float availablePanelHeight = Math.max(modal.rowHeight, bodyHeight);
        float availableInnerHeight = Math.max(modal.rowHeight, availablePanelHeight - 2f * modal.listInset);
        WimGuiScrollSlice<T> slice = WimGuiScroll.verticalSlice(
                rows,
                requestedOffset,
                availableInnerHeight,
                modal.rowHeight,
                modal.rowGap,
                extraGapProvider);
        float renderedPanelHeight = availablePanelHeight;
        float panelTop = bodyTop;
        float innerWidth = Math.max(1f, panelWidth - 2f * modal.listInset);
        return new WimGuiModalListLayout<T>(slice, panelTop, renderedPanelHeight, innerWidth, slice.maxOffset);
    }
}
