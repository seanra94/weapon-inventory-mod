package weaponinventorymod.gui;

import com.fs.starfarer.api.ui.ButtonAPI;

final class StockReviewButtonBinding {
    private final ButtonAPI button;
    private final StockReviewAction action;

    StockReviewButtonBinding(ButtonAPI button, StockReviewAction action) {
        this.button = button;
        this.action = action;
    }

    boolean consumeIfPressed() {
        if (button == null || !button.isChecked()) {
            return false;
        }
        button.setChecked(false);
        return true;
    }

    StockReviewAction getAction() {
        return action;
    }
}
