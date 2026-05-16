package weaponsprocurement.stock.item

enum class StockCategory(val label: String) {
    NO_STOCK("No Stock"),
    INSUFFICIENT("Insufficient Stock"),
    SUFFICIENT("Sufficient Stock"),
}
