package weaponsprocurement.core

import com.fs.starfarer.api.campaign.CargoStackAPI
import com.fs.starfarer.api.campaign.SubmarketPlugin
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import kotlin.math.max

object StockItemStacks {
    @JvmStatic
    fun isVisibleWeaponStack(stack: CargoStackAPI?): Boolean {
        return stack != null && stack.isWeaponStack && stack.weaponSpecIfWeapon != null && stack.size > 0f
    }

    @JvmStatic
    fun isVisibleWingStack(stack: CargoStackAPI?): Boolean {
        return stack != null && stack.isFighterWingStack && stack.fighterWingSpecIfWing != null && stack.size > 0f
    }

    @JvmStatic
    fun isVisibleItemStack(stack: CargoStackAPI?, itemType: StockItemType?): Boolean {
        return if (StockItemType.WING == itemType) {
            isVisibleWingStack(stack)
        } else {
            isVisibleWeaponStack(stack)
        }
    }

    @JvmStatic
    fun isPurchasableWeaponStack(submarket: SubmarketAPI?, stack: CargoStackAPI?): Boolean {
        if (submarket == null || stack == null || !stack.isWeaponStack || stack.weaponSpecIfWeapon == null) {
            return false
        }
        val plugin = submarket.plugin
        if (plugin != null && plugin.isIllegalOnSubmarket(stack, SubmarketPlugin.TransferAction.PLAYER_BUY)) {
            return false
        }
        return stack.size > 0f
    }

    @JvmStatic
    fun isPurchasableWingStack(submarket: SubmarketAPI?, stack: CargoStackAPI?): Boolean {
        if (submarket == null || stack == null || !stack.isFighterWingStack || stack.fighterWingSpecIfWing == null) {
            return false
        }
        val plugin = submarket.plugin
        if (plugin != null && plugin.isIllegalOnSubmarket(stack, SubmarketPlugin.TransferAction.PLAYER_BUY)) {
            return false
        }
        return stack.size > 0f
    }

    @JvmStatic
    fun isPurchasableItemStack(submarket: SubmarketAPI?, stack: CargoStackAPI?, itemType: StockItemType?): Boolean {
        return if (StockItemType.WING == itemType) {
            isPurchasableWingStack(submarket, stack)
        } else {
            isPurchasableWeaponStack(submarket, stack)
        }
    }

    @JvmStatic
    fun itemId(stack: CargoStackAPI?, itemType: StockItemType?): String? {
        if (stack == null) return null
        if (StockItemType.WING == itemType) {
            return stack.fighterWingSpecIfWing?.id
        }
        return stack.weaponSpecIfWeapon?.weaponId
    }

    @JvmStatic
    fun unitPrice(submarket: SubmarketAPI?, stack: CargoStackAPI?): Int {
        if (stack == null) return 0
        val tariff = tariff(submarket)
        return max(0, Math.round((stack.baseValuePerUnit * (1f + max(0f, tariff))).toFloat()))
    }

    @JvmStatic
    fun baseUnitPrice(stack: CargoStackAPI?): Int {
        if (stack == null) return 0
        return max(0, Math.round(stack.baseValuePerUnit.toFloat()))
    }

    @JvmStatic
    fun sellUnitPrice(submarket: SubmarketAPI?, stack: CargoStackAPI?): Int {
        if (stack == null) return 0
        val tariff = tariff(submarket)
        return max(0, Math.round((stack.baseValuePerUnit * (1f - max(0f, tariff))).toFloat()))
    }

    @JvmStatic
    fun unitCargoSpace(stack: CargoStackAPI?): Float {
        if (stack == null) return 1f
        val value = stack.cargoSpacePerUnit
        return if (value <= 0f) 1f else value
    }

    private fun tariff(submarket: SubmarketAPI?): Float {
        if (submarket == null) return 0f
        val plugin = submarket.plugin
        return plugin?.tariff ?: submarket.tariff
    }
}
