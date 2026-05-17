package weaponsprocurement.ui.stockreview.state

import weaponsprocurement.ui.WimGuiColorDebug
import java.awt.Color

class StockReviewModeController(private var reviewMode: Boolean) {
    private var filterMode = false
    private var colorDebugMode = false
    private var colorDebugReturnToReview = false
    private var colorDebugReturnScrollOffset = 0
    private var colorDebugPersistent = false
    private var colorDebugTargetIndex = 0
    private var colorDebugDraft: Color? = null
    private var revision = 0

    fun isReviewMode(): Boolean = reviewMode

    fun isFilterMode(): Boolean = filterMode

    fun isColorDebugMode(): Boolean = colorDebugMode

    fun isColorDebugPersistent(): Boolean = colorDebugPersistent

    fun getColorDebugTargetIndex(): Int = colorDebugTargetIndex

    fun getRevision(): Int = revision

    fun enterFilters(state: StockReviewState) {
        val changed = !filterMode || reviewMode || colorDebugMode || state.getListScrollOffset() != 0
        filterMode = true
        reviewMode = false
        colorDebugMode = false
        state.setListScrollOffset(0)
        markChangedIf(changed)
    }

    fun leaveFilters(state: StockReviewState) {
        val changed = filterMode || state.getListScrollOffset() != 0
        filterMode = false
        state.setListScrollOffset(0)
        markChangedIf(changed)
    }

    fun enterColorDebug(state: StockReviewState) {
        val previousDraft = colorDebugDraft?.rgb
        val changed = !colorDebugMode || reviewMode || filterMode || state.getListScrollOffset() != 0 || previousDraft == null
        colorDebugReturnToReview = reviewMode
        colorDebugReturnScrollOffset = state.getListScrollOffset()
        colorDebugMode = true
        reviewMode = false
        filterMode = false
        state.setListScrollOffset(0)
        ensureColorDebugDraft()
        markChangedIf(changed || previousDraft != colorDebugDraft?.rgb)
    }

    fun leaveColorDebug(state: StockReviewState) {
        val changed = colorDebugMode ||
            reviewMode != colorDebugReturnToReview ||
            state.getListScrollOffset() != colorDebugReturnScrollOffset
        colorDebugMode = false
        reviewMode = colorDebugReturnToReview
        state.setListScrollOffset(colorDebugReturnScrollOffset)
        markChangedIf(changed)
    }

    fun exitReview(state: StockReviewState) {
        val changed = reviewMode || state.getListScrollOffset() != 0
        reviewMode = false
        state.setListScrollOffset(0)
        markChangedIf(changed)
    }

    fun setReviewMode(reviewMode: Boolean) {
        if (this.reviewMode == reviewMode) {
            return
        }
        this.reviewMode = reviewMode
        markChanged()
    }

    fun toggleColorDebugPersistence() {
        colorDebugPersistent = !colorDebugPersistent
        markChanged()
    }

    fun cycleColorDebugTarget(delta: Int) {
        val targets = WimGuiColorDebug.targets()
        if (targets.isEmpty()) {
            val changed = colorDebugTargetIndex != 0 || colorDebugDraft != null
            colorDebugTargetIndex = 0
            colorDebugDraft = null
            markChangedIf(changed)
            return
        }
        val size = targets.size
        val previousIndex = colorDebugTargetIndex
        val previousDraft = colorDebugDraft?.rgb
        colorDebugTargetIndex = ((colorDebugTargetIndex + delta) % size + size) % size
        colorDebugDraft = WimGuiColorDebug.currentColor(WimGuiColorDebug.targetAt(colorDebugTargetIndex))
        markChangedIf(previousIndex != colorDebugTargetIndex || previousDraft != colorDebugDraft?.rgb)
    }

    fun currentColorDebugDraft(): Color? {
        ensureColorDebugDraft()
        return colorDebugDraft
    }

    fun adjustColorDebugDraft(redDelta: Int, greenDelta: Int, blueDelta: Int) {
        val previous = currentColorDebugDraft()?.rgb
        colorDebugDraft = WimGuiColorDebug.adjust(currentColorDebugDraft(), redDelta, greenDelta, blueDelta)
        markChangedIf(previous != colorDebugDraft?.rgb)
    }

    fun restoreColorDebugDraft() {
        val target = WimGuiColorDebug.targetAt(colorDebugTargetIndex)
        val previous = colorDebugDraft?.rgb
        colorDebugDraft = target?.defaultColor
        markChangedIf(previous != colorDebugDraft?.rgb)
    }

    fun applyColorDebugDraft() {
        val target = WimGuiColorDebug.targetAt(colorDebugTargetIndex) ?: return
        target.apply(currentColorDebugDraft())
        if (colorDebugPersistent) {
            WimGuiColorDebug.save(target, currentColorDebugDraft())
        }
        markChanged()
    }

    private fun ensureColorDebugDraft() {
        if (colorDebugDraft != null) {
            return
        }
        colorDebugDraft = WimGuiColorDebug.currentColor(WimGuiColorDebug.targetAt(colorDebugTargetIndex))
    }

    private fun markChanged() {
        revision++
    }

    private fun markChangedIf(changed: Boolean) {
        if (changed) markChanged()
    }
}
