package weaponsprocurement.gui

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.CustomPanelAPI

abstract class WimGuiModalPanelPlugin<A>(
    private val actionClass: Class<A>,
    private val width: Float,
    private val height: Float,
    buttonPollFramesAfterMouseEvent: Int,
    initialListBounds: WimGuiListBounds?,
) : BaseCustomUIPanelPlugin(), WimGuiDialogPanel, WimGuiButtonPoller.ActionHandler<A> {
    private val modalInput = WimGuiModalInput<A>(buttonPollFramesAfterMouseEvent)
    private val contentPanel = WimGuiContentPanel()
    private var root: CustomPanelAPI? = null
    private var callbacks: CustomVisualDialogDelegate.DialogCallbacks? = null
    private var listBounds: WimGuiListBounds? = initialListBounds

    init {
        modalInput.setListBounds(initialListBounds)
    }

    override fun init(panel: CustomPanelAPI, callbacks: CustomVisualDialogDelegate.DialogCallbacks) {
        root = panel
        this.callbacks = callbacks
        modalInput.setRoot(panel)
        onInit()
        rebuildContent()
    }

    override fun processInput(events: List<InputEventAPI>) {
        val result = modalInput.processInput(events)
        if (result.isCloseRequested()) {
            onCloseRequested()
            return
        }
        if (result.hasScrollDelta()) {
            onScroll(result.getScrollDelta(), listBounds?.getMaxScrollOffset() ?: 0)
            rebuildContent()
        }
    }

    override fun advance(amount: Float) {
        if (callbacks != null) {
            modalInput.processButtonsIfRequested(this)
        }
    }

    override fun buttonPressed(buttonId: Any?) {
        if (actionClass.isInstance(buttonId)) {
            modalInput.clearCheckedButtons()
            modalInput.suppressNextMouseUpPoll()
            handle(actionClass.cast(buttonId))
        }
    }

    protected fun rebuildContent() {
        val currentRoot = root
        if (currentRoot == null || !canRenderContent()) {
            return
        }
        try {
            modalInput.clearButtonBindings()
            val content = contentPanel.begin(currentRoot, width, height)
            val renderedBounds = renderContent(content!!, modalInput.buttonBindings())
            if (renderedBounds != null) {
                listBounds = renderedBounds
                modalInput.setListBounds(renderedBounds)
            }
            contentPanel.attach(currentRoot)
        } catch (t: Throwable) {
            onRebuildFailed(t)
            close()
        }
    }

    protected fun close() {
        val currentCallbacks = callbacks ?: return
        callbacks = null
        currentCallbacks.dismissDialog()
    }

    protected open fun onCloseRequested() {
        close()
    }

    protected fun maxScrollOffset(): Int = listBounds?.getMaxScrollOffset() ?: 0

    protected open fun onInit() {
    }

    protected open fun canRenderContent(): Boolean = true

    protected abstract fun renderContent(
        content: CustomPanelAPI,
        buttonBindings: MutableList<WimGuiButtonBinding<A>>,
    ): WimGuiListBounds?

    protected abstract fun onScroll(scrollDelta: Int, maxScrollOffset: Int)

    protected abstract fun onRebuildFailed(t: Throwable)
}
