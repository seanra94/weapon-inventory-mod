package weaponinventorymod.gui;

import com.fs.starfarer.api.input.InputEventAPI;

import java.util.ArrayList;
import java.util.List;

final class WimGuiButtonPoller<A> {
    interface ActionHandler<A> {
        void handle(A action);
    }

    private final List<WimGuiButtonBinding<A>> bindings = new ArrayList<WimGuiButtonBinding<A>>();
    private final int framesAfterMouseEvent;
    private int framesRemaining = 0;
    private boolean hasPendingMouseUp = false;
    private boolean suppressNextMouseUp = false;
    private float pendingMouseUpX = 0f;
    private float pendingMouseUpY = 0f;

    WimGuiButtonPoller(int framesAfterMouseEvent) {
        this.framesAfterMouseEvent = Math.max(1, framesAfterMouseEvent);
    }

    List<WimGuiButtonBinding<A>> bindings() {
        return bindings;
    }

    void clearBindings() {
        bindings.clear();
        framesRemaining = 0;
        clearPendingMouseUp();
    }

    void requestPollFromInput(List<InputEventAPI> events) {
        if (events == null) {
            return;
        }
        for (int i = 0; i < events.size(); i++) {
            InputEventAPI event = events.get(i);
            if (event == null) {
                continue;
            }
            if (event.isMouseUpEvent()) {
                if (suppressNextMouseUp) {
                    suppressNextMouseUp = false;
                    continue;
                }
                hasPendingMouseUp = true;
                pendingMouseUpX = event.getX();
                pendingMouseUpY = event.getY();
                framesRemaining = framesAfterMouseEvent;
                return;
            }
            if (event.isMouseDownEvent()) {
                framesRemaining = framesAfterMouseEvent;
                return;
            }
        }
    }

    boolean processIfRequested(ActionHandler<A> handler) {
        if (framesRemaining <= 0 || handler == null) {
            return false;
        }
        framesRemaining--;
        if (hasPendingMouseUp) {
            float x = pendingMouseUpX;
            float y = pendingMouseUpY;
            clearPendingMouseUp();
            for (int i = 0; i < bindings.size(); i++) {
                WimGuiButtonBinding<A> binding = bindings.get(i);
                if (!binding.consumeIfClicked(x, y)) {
                    continue;
                }
                handler.handle(binding.getAction());
                return true;
            }
        }
        for (int i = 0; i < bindings.size(); i++) {
            WimGuiButtonBinding<A> binding = bindings.get(i);
            if (!binding.consumeIfPressed()) {
                continue;
            }
            handler.handle(binding.getAction());
            return true;
        }
        return false;
    }

    void clearCheckedButtons() {
        for (int i = 0; i < bindings.size(); i++) {
            bindings.get(i).clear();
        }
        framesRemaining = 0;
        clearPendingMouseUp();
    }

    void suppressNextMouseUp() {
        suppressNextMouseUp = true;
        framesRemaining = 0;
        clearPendingMouseUp();
    }

    private void clearPendingMouseUp() {
        hasPendingMouseUp = false;
        pendingMouseUpX = 0f;
        pendingMouseUpY = 0f;
    }
}
