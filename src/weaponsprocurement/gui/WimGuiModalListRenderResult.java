package weaponsprocurement.gui;

final class WimGuiModalListRenderResult {
    private final WimGuiListBounds bounds;
    private final int offset;

    WimGuiModalListRenderResult(WimGuiListBounds bounds, int offset) {
        this.bounds = bounds;
        this.offset = offset;
    }

    WimGuiListBounds getBounds() {
        return bounds;
    }

    int getOffset() {
        return offset;
    }
}
