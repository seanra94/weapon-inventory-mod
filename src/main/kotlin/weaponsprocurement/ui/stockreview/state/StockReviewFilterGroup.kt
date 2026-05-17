package weaponsprocurement.ui.stockreview.state

enum class StockReviewFilterGroup(
    val label: String,
    val weaponOnly: Boolean,
) {
    SIZE("Size", true),
    TYPE("Type", true),
    DAMAGE("Damage", true),
    AVAILABILITY("Availability", false),
    RARITY("Rarity", false),
}
