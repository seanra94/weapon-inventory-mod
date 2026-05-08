package weaponsprocurement.gui;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;

import java.util.List;

abstract class WimGuiModalPanelPlugin<A> extends BaseCustomUIPanelPlugin implements WimGuiDialogPanel,
        WimGuiButtonPoller.ActionHandler<A> {
    private final Class<A> actionClass;
    private final float width;
    private final float height;
    private final WimGuiModalInput<A> modalInput;
    private final WimGuiContentPanel contentPanel = new WimGuiContentPanel();

    private CustomPanelAPI root;
    private CustomVisualDialogDelegate.DialogCallbacks callbacks;
    private WimGuiListBounds listBounds;

    WimGuiModalPanelPlugin(Class<A> actionClass,
                           float width,
                           float height,
                           int buttonPollFramesAfterMouseEvent,
                           WimGuiListBounds initialListBounds) {
        this.actionClass = actionClass;
        this.width = width;
        this.height = height;
        this.modalInput = new WimGuiModalInput<A>(buttonPollFramesAfterMouseEvent);
        this.listBounds = initialListBounds;
        this.modalInput.setListBounds(initialListBounds);
    }

    @Override
    public final void init(CustomPanelAPI panel, CustomVisualDialogDelegate.DialogCallbacks callbacks) {
        this.root = panel;
        this.callbacks = callbacks;
        modalInput.setRoot(panel);
        onInit();
        rebuildContent();
    }

    @Override
    public final void processInput(List<InputEventAPI> events) {
        WimGuiInputResult result = modalInput.processInput(events);
        if (result.isCloseRequested()) {
            onCloseRequested();
            return;
        }
        if (result.hasScrollDelta()) {
            onScroll(result.getScrollDelta(), listBounds == null ? 0 : listBounds.getMaxScrollOffset());
            rebuildContent();
        }
    }

    @Override
    public final void advance(float amount) {
        if (callbacks != null) {
            modalInput.processButtonsIfRequested(this);
        }
    }

    @Override
    public final void buttonPressed(Object buttonId) {
        if (actionClass.isInstance(buttonId)) {
            modalInput.clearCheckedButtons();
            modalInput.suppressNextMouseUpPoll();
            handle(actionClass.cast(buttonId));
        }
    }

    protected final void rebuildContent() {
        if (root == null || !canRenderContent()) {
            return;
        }
        try {
            modalInput.clearButtonBindings();
            CustomPanelAPI content = contentPanel.begin(root, width, height);
            WimGuiListBounds renderedBounds = renderContent(content, modalInput.buttonBindings());
            if (renderedBounds != null) {
                listBounds = renderedBounds;
                modalInput.setListBounds(renderedBounds);
            }
            contentPanel.attach(root);
        } catch (Throwable t) {
            onRebuildFailed(t);
            close();
        }
    }

    protected final void close() {
        if (callbacks != null) {
            CustomVisualDialogDelegate.DialogCallbacks currentCallbacks = callbacks;
            callbacks = null;
            currentCallbacks.dismissDialog();
        }
    }

    protected void onCloseRequested() {
        close();
    }

    protected final int maxScrollOffset() {
        return listBounds == null ? 0 : listBounds.getMaxScrollOffset();
    }

    protected void onInit() {
    }

    protected boolean canRenderContent() {
        return true;
    }

    protected abstract WimGuiListBounds renderContent(CustomPanelAPI content,
                                                      List<WimGuiButtonBinding<A>> buttonBindings);

    protected abstract void onScroll(int scrollDelta, int maxScrollOffset);

    protected abstract void onRebuildFailed(Throwable t);
}
