package weaponsprocurement.gui;

final class WimGuiPendingDialog<C, S> {
    private final C context;
    private final S state;

    WimGuiPendingDialog(C context, S state) {
        this.context = context;
        this.state = state;
    }

    C getContext() {
        return context;
    }

    S getState() {
        return state;
    }
}
