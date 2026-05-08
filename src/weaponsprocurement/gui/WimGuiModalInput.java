package weaponsprocurement.gui;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import org.lwjgl.input.Keyboard;

import java.util.List;

final class WimGuiModalInput<A> {
    private final WimGuiButtonPoller<A> buttonPoller;
    private CustomPanelAPI root;
    private WimGuiListBounds listBounds;

    WimGuiModalInput(int buttonPollFramesAfterMouseEvent) {
        this.buttonPoller = new WimGuiButtonPoller<A>(buttonPollFramesAfterMouseEvent);
    }

    void setRoot(CustomPanelAPI root) {
        this.root = root;
    }

    void setListBounds(WimGuiListBounds listBounds) {
        this.listBounds = listBounds;
    }

    List<WimGuiButtonBinding<A>> buttonBindings() {
        return buttonPoller.bindings();
    }

    void clearButtonBindings() {
        buttonPoller.clearBindings();
    }

    void clearCheckedButtons() {
        buttonPoller.clearCheckedButtons();
    }

    void suppressNextMouseUpPoll() {
        buttonPoller.suppressNextMouseUp();
    }

    WimGuiInputResult processInput(List<InputEventAPI> events) {
        if (events == null) {
            return WimGuiInputResult.none();
        }
        for (int i = 0; i < events.size(); i++) {
            InputEventAPI event = events.get(i);
            if (event == null || event.isConsumed()) {
                continue;
            }
            if (event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE) {
                event.consume();
                return WimGuiInputResult.closeRequested();
            }
            if (event.isMouseScrollEvent() && event.getEventValue() != 0 && isMouseInList(event)) {
                int delta = WimGuiScroll.wheelDelta(event.getEventValue());
                event.consume();
                return WimGuiInputResult.scroll(delta);
            }
        }
        buttonPoller.requestPollFromInput(events);
        return WimGuiInputResult.none();
    }

    boolean processButtonsIfRequested(WimGuiButtonPoller.ActionHandler<A> handler) {
        return buttonPoller.processIfRequested(handler);
    }

    private boolean isMouseInList(InputEventAPI event) {
        return listBounds != null && listBounds.contains(root, event);
    }
}
