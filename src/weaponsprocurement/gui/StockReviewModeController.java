package weaponsprocurement.gui;

import java.awt.Color;
import java.util.List;

final class StockReviewModeController {
    private boolean reviewMode;
    private boolean filterMode;
    private boolean colorDebugMode;
    private boolean colorDebugReturnToReview;
    private int colorDebugReturnScrollOffset;
    private boolean colorDebugPersistent;
    private int colorDebugTargetIndex;
    private Color colorDebugDraft;

    StockReviewModeController(boolean reviewMode) {
        this.reviewMode = reviewMode;
    }

    boolean isReviewMode() {
        return reviewMode;
    }

    boolean isFilterMode() {
        return filterMode;
    }

    boolean isColorDebugMode() {
        return colorDebugMode;
    }

    boolean isColorDebugPersistent() {
        return colorDebugPersistent;
    }

    int getColorDebugTargetIndex() {
        return colorDebugTargetIndex;
    }

    void enterFilters(StockReviewState state) {
        filterMode = true;
        reviewMode = false;
        colorDebugMode = false;
        state.setListScrollOffset(0);
    }

    void leaveFilters(StockReviewState state) {
        filterMode = false;
        state.setListScrollOffset(0);
    }

    void enterColorDebug(StockReviewState state) {
        colorDebugReturnToReview = reviewMode;
        colorDebugReturnScrollOffset = state.getListScrollOffset();
        colorDebugMode = true;
        reviewMode = false;
        filterMode = false;
        state.setListScrollOffset(0);
        ensureColorDebugDraft();
    }

    void leaveColorDebug(StockReviewState state) {
        colorDebugMode = false;
        reviewMode = colorDebugReturnToReview;
        state.setListScrollOffset(colorDebugReturnScrollOffset);
    }

    void exitReview(StockReviewState state) {
        reviewMode = false;
        state.setListScrollOffset(0);
    }

    void setReviewMode(boolean reviewMode) {
        this.reviewMode = reviewMode;
    }

    void toggleColorDebugPersistence() {
        colorDebugPersistent = !colorDebugPersistent;
    }

    void cycleColorDebugTarget(int delta) {
        List<WimGuiColorDebug.Target> targets = WimGuiColorDebug.targets();
        if (targets.isEmpty()) {
            colorDebugTargetIndex = 0;
            colorDebugDraft = null;
            return;
        }
        int size = targets.size();
        colorDebugTargetIndex = ((colorDebugTargetIndex + delta) % size + size) % size;
        colorDebugDraft = WimGuiColorDebug.currentColor(WimGuiColorDebug.targetAt(colorDebugTargetIndex));
    }

    Color currentColorDebugDraft() {
        ensureColorDebugDraft();
        return colorDebugDraft;
    }

    void adjustColorDebugDraft(int redDelta, int greenDelta, int blueDelta) {
        colorDebugDraft = WimGuiColorDebug.adjust(currentColorDebugDraft(), redDelta, greenDelta, blueDelta);
    }

    void restoreColorDebugDraft() {
        WimGuiColorDebug.Target target = WimGuiColorDebug.targetAt(colorDebugTargetIndex);
        colorDebugDraft = target == null ? null : target.getDefaultColor();
    }

    void applyColorDebugDraft() {
        WimGuiColorDebug.Target target = WimGuiColorDebug.targetAt(colorDebugTargetIndex);
        if (target == null) {
            return;
        }
        target.apply(currentColorDebugDraft());
        if (colorDebugPersistent) {
            WimGuiColorDebug.save(target, currentColorDebugDraft());
        }
    }

    private void ensureColorDebugDraft() {
        if (colorDebugDraft != null) {
            return;
        }
        colorDebugDraft = WimGuiColorDebug.currentColor(WimGuiColorDebug.targetAt(colorDebugTargetIndex));
    }
}
