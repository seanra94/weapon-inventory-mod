package weaponsprocurement.core

enum class StockSortMode(val label: String) {
    NEED("Stock"),
    NAME("Name"),
    PRICE("Price");

    fun next(): StockSortMode {
        val values = values()
        val nextOrdinal = ordinal + 1
        return values[if (nextOrdinal >= values.size) 0 else nextOrdinal]
    }

    companion object {
        @JvmStatic
        fun fromConfig(value: String?): StockSortMode {
            if (value == null) return NEED
            val normalized = value.trim().uppercase().replace('-', '_').replace(' ', '_')
            if (normalized == "PURCHASABLE" || normalized == "FOR_SALE" || normalized == "OWNED") {
                return NEED
            }
            if (normalized == "COST") return PRICE
            for (mode in values()) {
                if (mode.name == normalized) return mode
            }
            return NEED
        }
    }
}
