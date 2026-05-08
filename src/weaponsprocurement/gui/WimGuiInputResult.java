package weaponsprocurement.gui;

final class WimGuiInputResult {
    private static final WimGuiInputResult NONE = new WimGuiInputResult(false, 0);
    private static final WimGuiInputResult CLOSE = new WimGuiInputResult(true, 0);

    private final boolean closeRequested;
    private final int scrollDelta;

    private WimGuiInputResult(boolean closeRequested, int scrollDelta) {
        this.closeRequested = closeRequested;
        this.scrollDelta = scrollDelta;
    }

    static WimGuiInputResult none() {
        return NONE;
    }

    static WimGuiInputResult closeRequested() {
        return CLOSE;
    }

    static WimGuiInputResult scroll(int delta) {
        return delta == 0 ? NONE : new WimGuiInputResult(false, delta);
    }

    boolean isCloseRequested() {
        return closeRequested;
    }

    boolean hasScrollDelta() {
        return scrollDelta != 0;
    }

    int getScrollDelta() {
        return scrollDelta;
    }
}
