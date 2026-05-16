package weaponsprocurement.trade.plan

import weaponsprocurement.stock.item.*
import weaponsprocurement.stock.market.*
import weaponsprocurement.stock.fixer.*

object TradeMoney {
    const val MAX_EXECUTABLE_CREDITS: Long = 2147483647L

    @JvmStatic
    fun lineTotal(unitPrice: Int, quantity: Int): Long {
        if (unitPrice < 0 || quantity < 0) return -1L
        return unitPrice.toLong() * quantity.toLong()
    }

    @JvmStatic
    fun safeAdd(left: Long, right: Long): Long {
        if (right > 0L && left > Long.MAX_VALUE - right) return Long.MAX_VALUE
        if (right < 0L && left < Long.MIN_VALUE - right) return Long.MIN_VALUE
        return left + right
    }

    @JvmStatic
    fun canExecuteCreditMutation(credits: Long): Boolean {
        return credits in 0L..MAX_EXECUTABLE_CREDITS
    }
}