package weaponsprocurement.ui

class WimGuiDialogTracker<C, S> {
    private var open = false
    private var pendingContext: C? = null
    private var pendingState: S? = null

    fun isOpen(): Boolean = open

    fun markOpen() {
        open = true
    }

    fun markClosed() {
        open = false
    }

    fun requestReopen(context: C, state: S) {
        pendingContext = context
        pendingState = state
        open = false
    }

    fun hasPending(): Boolean = !open && pendingState != null

    fun consumePending(): WimGuiPendingDialog<C?, S?>? {
        if (!hasPending()) return null
        val pending = WimGuiPendingDialog(pendingContext, pendingState)
        pendingContext = null
        pendingState = null
        return pending
    }
}
