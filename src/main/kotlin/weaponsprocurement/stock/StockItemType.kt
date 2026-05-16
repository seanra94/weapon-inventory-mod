package weaponsprocurement.stock

enum class StockItemType(
    val sectionLabel: String,
    val singularLabel: String,
    private val keyPrefix: String,
) {
    WEAPON("Weapons", "Weapon", "W:"),
    WING("Wings", "Wing", "F:");

    fun key(itemId: String?): String = keyPrefix + (itemId ?: "")

    companion object {
        @JvmStatic
        fun fromKey(itemKey: String?): StockItemType {
            return if (itemKey != null && itemKey.startsWith(WING.keyPrefix)) {
                WING
            } else {
                WEAPON
            }
        }

        @JvmStatic
        fun rawId(itemKey: String?): String? {
            if (itemKey == null) return null
            for (type in values()) {
                if (itemKey.startsWith(type.keyPrefix)) {
                    return itemKey.substring(type.keyPrefix.length)
                }
            }
            return itemKey
        }
    }
}
