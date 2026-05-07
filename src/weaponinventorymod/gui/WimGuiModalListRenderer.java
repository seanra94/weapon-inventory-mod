package weaponinventorymod.gui;

import com.fs.starfarer.api.ui.CustomPanelAPI;

import java.util.List;

final class WimGuiModalListRenderer {
    interface ScrollRowFactory<A> {
        WimGuiListRow<A> createScrollRow(String label, int scrollDelta);
    }

    interface ExtraGapProvider<A> {
        float extraGapBefore(WimGuiListRow<A> row);
    }

    private WimGuiModalListRenderer() {
    }

    static <A> WimGuiModalListRenderResult render(CustomPanelAPI root,
                                                  List<WimGuiListRow<A>> rows,
                                                  int requestedOffset,
                                                  WimGuiModalListSpec spec,
                                                  ScrollRowFactory<A> scrollRowFactory,
                                                  ExtraGapProvider<A> extraGapProvider,
                                                  List<WimGuiButtonBinding<A>> buttons) {
        WimGuiModalListLayout<WimGuiListRow<A>> layout = WimGuiModalListLayout.compute(
                rows,
                requestedOffset,
                spec.panelTop,
                spec.panelHeight,
                spec.panelWidth,
                spec.modal,
                new WimGuiModalListGapAdapter<A>(extraGapProvider));
        WimGuiScrollSlice<WimGuiListRow<A>> slice = layout.scrollSlice;
        CustomPanelAPI listPanel = root.createCustomPanel(
                spec.panelWidth,
                layout.panelHeight,
                new WimGuiPanelPlugin(spec.panelFill, spec.panelBorder));
        root.addComponent(listPanel).inTL(spec.panelLeft, layout.panelTop);

        float y = spec.rowHorizontalPad;
        if (slice.hasAbove) {
            renderRow(listPanel, scrollRowFactory.createScrollRow(WimGuiScrollIndicator.ABOVE,
                    -layout.pageDelta()), y, spec, buttons);
            y += spec.rowHeight + spec.rowGap;
        }
        for (int i = 0; i < slice.items.size(); i++) {
            WimGuiListRow<A> row = slice.items.get(i);
            y += extraGapProvider == null ? 0f : extraGapProvider.extraGapBefore(row);
            renderRow(listPanel, row, y, spec, buttons);
            y += spec.rowHeight + spec.rowGap;
        }
        if (slice.hasBelow) {
            renderRow(listPanel, scrollRowFactory.createScrollRow(WimGuiScrollIndicator.BELOW,
                    layout.pageDelta()), y, spec, buttons);
        }
        return new WimGuiModalListRenderResult(
                new WimGuiListBounds(
                        layout.maxOffset,
                        spec.panelLeft,
                        layout.panelTop,
                        spec.panelWidth,
                        layout.panelHeight),
                slice.offset);
    }

    static <A> WimGuiListBounds renderAndStoreOffset(CustomPanelAPI root,
                                                     List<WimGuiListRow<A>> rows,
                                                     WimGuiScrollableListState state,
                                                     WimGuiModalListSpec spec,
                                                     ScrollRowFactory<A> scrollRowFactory,
                                                     ExtraGapProvider<A> extraGapProvider,
                                                     List<WimGuiButtonBinding<A>> buttons) {
        WimGuiModalListRenderResult result = render(
                root,
                rows,
                state == null ? 0 : state.getListScrollOffset(),
                spec,
                scrollRowFactory,
                extraGapProvider,
                buttons);
        if (state != null) {
            state.setListScrollOffset(result.getOffset());
        }
        return result.getBounds();
    }

    private static <A> void renderRow(CustomPanelAPI listPanel,
                                      WimGuiListRow<A> row,
                                      float y,
                                      WimGuiModalListSpec spec,
                                      List<WimGuiButtonBinding<A>> buttons) {
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
                buttons);
    }

}
