package weaponsprocurement.gui

import java.awt.Color

class StockReviewModeController(private var reviewMode: Boolean) {
    private var filterMode = false
    private var colorDebugMode = false
    private var colorDebugReturnToReview = false
    private var colorDebugReturnScrollOffset = 0
    private var colorDebugPersistent = false
    private var colorDebugTargetIndex = 0
    private var colorDebugDraft: Color? = null

    fun isReviewMode(): Boolean = reviewMode

    fun isFilterMode(): Boolean = filterMode

    fun isColorDebugMode(): Boolean = colorDebugMode

    fun isColorDebugPersistent(): Boolean = colorDebugPersistent

    fun getColorDebugTargetIndex(): Int = colorDebugTargetIndex

    fun enterFilters(state: StockReviewState) {
        filterMode = true
        reviewMode = false
        colorDebugMode = false
        state.setListScrollOffset(0)
    }

    fun leaveFilters(state: StockReviewState) {
        filterMode = false
        state.setListScrollOffset(0)
    }

    fun enterColorDebug(state: StockReviewState) {
        colorDebugReturnToReview = reviewMode
        colorDebugReturnScrollOffset = state.getListScrollOffset()
        colorDebugMode = true
        reviewMode = false
        filterMode = false
        state.setListScrollOffset(0)
        ensureColorDebugDraft()
    }

    fun leaveColorDebug(state: StockReviewState) {
        colorDebugMode = false
        reviewMode = colorDebugReturnToReview
        state.setListScrollOffset(colorDebugReturnScrollOffset)
    }

    fun exitReview(state: StockReviewState) {
        reviewMode = false
        state.setListScrollOffset(0)
    }

    fun setReviewMode(reviewMode: Boolean) {
        this.reviewMode = reviewMode
    }

    fun toggleColorDebugPersistence() {
        colorDebugPersistent = !colorDebugPersistent
    }

    fun cycleColorDebugTarget(delta: Int) {
        val targets = WimGuiColorDebug.targets()
        if (targets.isEmpty()) {
            colorDebugTargetIndex = 0
            colorDebugDraft = null
            return
        }
        val size = targets.size
        colorDebugTargetIndex = ((colorDebugTargetIndex + delta) % size + size) % size
        colorDebugDraft = WimGuiColorDebug.currentColor(WimGuiColorDebug.targetAt(colorDebugTargetIndex))
    }

    fun currentColorDebugDraft(): Color? {
        ensureColorDebugDraft()
        return colorDebugDraft
    }

    fun adjustColorDebugDraft(redDelta: Int, greenDelta: Int, blueDelta: Int) {
        colorDebugDraft = WimGuiColorDebug.adjust(currentColorDebugDraft(), redDelta, greenDelta, blueDelta)
    }

    fun restoreColorDebugDraft() {
        val target = WimGuiColorDebug.targetAt(colorDebugTargetIndex)
        colorDebugDraft = target?.defaultColor
    }

    fun applyColorDebugDraft() {
        val target = WimGuiColorDebug.targetAt(colorDebugTargetIndex) ?: return
        target.apply(currentColorDebugDraft())
        if (colorDebugPersistent) {
            WimGuiColorDebug.save(target, currentColorDebugDraft())
        }
    }

    private fun ensureColorDebugDraft() {
        if (colorDebugDraft != null) {
            return
        }
        colorDebugDraft = WimGuiColorDebug.currentColor(WimGuiColorDebug.targetAt(colorDebugTargetIndex))
    }
}
