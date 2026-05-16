package weaponsprocurement.core

import weaponsprocurement.internal.WeaponsProcurementConfig

enum class StockSourceMode(
    val label: String,
    private val remote: Boolean,
) {
    LOCAL("Local", false),
    SECTOR("Sector Market", true),
    FIXERS("Fixer's Market", true);

    fun isRemote(): Boolean = remote

    fun next(): StockSourceMode {
        val values = values()
        var current = this
        for (i in values.indices) {
            current = values[(current.ordinal + 1) % values.size]
            if (current.isEnabled) return current
        }
        return LOCAL
    }

    val isEnabled: Boolean
        get() {
            if (this == SECTOR) return WeaponsProcurementConfig.isSectorMarketEnabled()
            if (this == FIXERS) return WeaponsProcurementConfig.isFixersMarketEnabled()
            return true
        }
}
