package weaponsprocurement.trade

import weaponsprocurement.stock.item.*
import weaponsprocurement.stock.market.*
import weaponsprocurement.stock.fixer.*

object CreditFormat {
    const val CREDIT_SYMBOL: String = "\u00a2"

    @JvmStatic
    fun credits(credits: Int): String = credits(credits.toLong())

    @JvmStatic
    fun credits(credits: Long): String = grouped(credits) + CREDIT_SYMBOL

    @JvmStatic
    fun creditsLong(credits: Int): String = creditsLong(credits.toLong())

    @JvmStatic
    fun creditsLong(credits: Long): String = grouped(credits) + " credits"

    @JvmStatic
    fun grouped(value: Int): String = grouped(value.toLong())

    @JvmStatic
    fun grouped(value: Long): String {
        val digits = kotlin.math.abs(value).toString()
        val result = StringBuilder()
        var firstGroup = digits.length % 3
        if (firstGroup == 0) firstGroup = 3
        result.append(digits.substring(0, firstGroup))
        var index = firstGroup
        while (index < digits.length) {
            result.append(',').append(digits.substring(index, index + 3))
            index += 3
        }
        return if (value < 0) "-$result" else result.toString()
    }
}