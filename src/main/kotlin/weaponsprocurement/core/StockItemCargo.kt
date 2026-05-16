package weaponsprocurement.core

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.CargoStackAPI

object StockItemCargo {
    @JvmStatic
    fun itemStack(cargo: CargoAPI?, itemType: StockItemType, itemId: String): CargoStackAPI? {
        val stacks = cargo?.stacksCopy ?: return null
        for (stack in stacks) {
            if (StockItemStacks.isVisibleItemStack(stack, itemType) && itemId == StockItemStacks.itemId(stack, itemType)) {
                return stack
            }
        }
        return null
    }

    @JvmStatic
    fun itemCount(cargo: CargoAPI?, itemType: StockItemType, itemId: String): Int {
        var count = 0
        val stacks = cargo?.stacksCopy ?: return count
        for (stack in stacks) {
            if (StockItemStacks.isVisibleItemStack(stack, itemType) && itemId == StockItemStacks.itemId(stack, itemType)) {
                count += Math.round(stack.size)
            }
        }
        return count
    }

    @JvmStatic
    fun itemDisplayName(itemType: StockItemType, itemId: String): String {
        return try {
            if (StockItemType.WING == itemType) {
                Global.getSettings().getFighterWingSpec(itemId).wingName
            } else {
                Global.getSettings().getWeaponSpec(itemId).weaponName
            }
        } catch (ignored: Throwable) {
            itemId
        }
    }

    @JvmStatic
    fun addItem(cargo: CargoAPI, itemType: StockItemType, itemId: String, quantity: Int) {
        if (StockItemType.WING == itemType) {
            cargo.addFighters(itemId, quantity)
        } else {
            cargo.addWeapons(itemId, quantity)
        }
    }

    @JvmStatic
    fun removeItem(cargo: CargoAPI, itemType: StockItemType, itemId: String, quantity: Int) {
        if (StockItemType.WING == itemType) {
            cargo.removeFighters(itemId, quantity)
        } else {
            cargo.removeWeapons(itemId, quantity)
        }
    }

    @JvmStatic
    fun reconcileItemCount(cargo: CargoAPI?, itemType: StockItemType, itemId: String, expectedCount: Int) {
        if (cargo == null || expectedCount < 0) return
        val currentCount = itemCount(cargo, itemType, itemId)
        if (currentCount < expectedCount) {
            addItem(cargo, itemType, itemId, expectedCount - currentCount)
        } else if (currentCount > expectedCount) {
            removeItem(cargo, itemType, itemId, currentCount - expectedCount)
        }
        tidyCargo(cargo)
    }

    @JvmStatic
    fun tidyCargo(cargo: CargoAPI?) {
        if (cargo == null) return
        cargo.removeEmptyStacks()
        cargo.sort()
        cargo.updateSpaceUsed()
    }
}
