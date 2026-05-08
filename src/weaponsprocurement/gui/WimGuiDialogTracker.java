package weaponsprocurement.gui;

final class WimGuiDialogTracker<C, S> {
    private boolean open = false;
    private C pendingContext;
    private S pendingState;

    boolean isOpen() {
        return open;
    }

    void markOpen() {
        open = true;
    }

    void markClosed() {
        open = false;
    }

    void requestReopen(C context, S state) {
        pendingContext = context;
        pendingState = state;
        open = false;
    }

    boolean hasPending() {
        return !open && pendingState != null;
    }

    WimGuiPendingDialog<C, S> consumePending() {
        if (!hasPending()) {
            return null;
        }
        WimGuiPendingDialog<C, S> pending = new WimGuiPendingDialog<C, S>(pendingContext, pendingState);
        pendingContext = null;
        pendingState = null;
        return pending;
    }
}
