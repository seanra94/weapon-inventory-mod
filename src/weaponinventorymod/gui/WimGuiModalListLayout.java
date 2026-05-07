package weaponinventorymod.gui;

import java.util.List;

final class WimGuiModalListLayout<T> {
    interface ExtraGapProvider<T> {
        float extraGapBefore(T item);
    }

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
                                                WimGuiModalLayout modal,
                                                ExtraGapProvider<T> extraGapProvider) {
        float availablePanelHeight = Math.max(modal.rowHeight, bodyHeight);
        float availableInnerHeight = Math.max(modal.rowHeight, availablePanelHeight - 2f * modal.listInset);
        WimGuiScrollSlice<T> slice = WimGuiScroll.verticalSlice(
                rows,
                requestedOffset,
                availableInnerHeight,
                modal.rowHeight,
                modal.rowGap);
        float extraGapHeight = visibleExtraGapHeight(slice.items, extraGapProvider);
        float renderedPanelHeight = Math.min(
                availablePanelHeight,
                modal.listPanelHeight(slice.items.size(), slice.hasAbove, slice.hasBelow, extraGapHeight));
        float panelTop = bodyTop + Math.max(0f, availablePanelHeight - renderedPanelHeight);
        float innerWidth = Math.max(1f, panelWidth - 2f * modal.listInset);
        return new WimGuiModalListLayout<T>(slice, panelTop, renderedPanelHeight, innerWidth, slice.maxOffset);
    }

    private static <T> float visibleExtraGapHeight(List<T> visibleRows, ExtraGapProvider<T> extraGapProvider) {
        if (visibleRows == null || extraGapProvider == null) {
            return 0f;
        }
        float result = 0f;
        for (int i = 0; i < visibleRows.size(); i++) {
            result += Math.max(0f, extraGapProvider.extraGapBefore(visibleRows.get(i)));
        }
        return result;
    }
}
