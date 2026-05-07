package weaponinventorymod.gui;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;

final class WimGuiListBounds {
    private final int maxScrollOffset;
    private final float left;
    private final float top;
    private final float width;
    private final float height;

    WimGuiListBounds(int maxScrollOffset, float left, float top, float width, float height) {
        this.maxScrollOffset = maxScrollOffset;
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
    }

    int getMaxScrollOffset() {
        return maxScrollOffset;
    }

    boolean contains(CustomPanelAPI root, InputEventAPI event) {
        if (root == null || root.getPosition() == null || event == null || maxScrollOffset <= 0) {
            return false;
        }
        float screenLeft = root.getPosition().getX() + left;
        float screenRight = screenLeft + width;
        float screenTop = root.getPosition().getY() + root.getPosition().getHeight() - top;
        float screenBottom = screenTop - height;
        return event.getX() >= screenLeft
                && event.getX() <= screenRight
                && event.getY() >= screenBottom
                && event.getY() <= screenTop;
    }
}
