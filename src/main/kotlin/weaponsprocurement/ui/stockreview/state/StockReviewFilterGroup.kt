package weaponsprocurement.ui.stockreview.state

import weaponsprocurement.ui.stockreview.actions.StockReviewAction.Type
enum class StockReviewFilterGroup(val label: String) {
    SIZE("Size"),
    TYPE("Type"),
    DAMAGE("Damage"),
}