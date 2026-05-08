package weaponsprocurement.core;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;

final class StockItemCargo {
    private StockItemCargo() {
    }

    static CargoStackAPI itemStack(CargoAPI cargo, StockItemType itemType, String itemId) {
        if (cargo == null || cargo.getStacksCopy() == null) {
            return null;
        }
        for (CargoStackAPI stack : cargo.getStacksCopy()) {
            if (StockItemStacks.isVisibleItemStack(stack, itemType) && itemId.equals(StockItemStacks.itemId(stack, itemType))) {
                return stack;
            }
        }
        return null;
    }

    static int itemCount(CargoAPI cargo, StockItemType itemType, String itemId) {
        int count = 0;
        if (cargo == null || cargo.getStacksCopy() == null) {
            return count;
        }
        for (CargoStackAPI stack : cargo.getStacksCopy()) {
            if (StockItemStacks.isVisibleItemStack(stack, itemType) && itemId.equals(StockItemStacks.itemId(stack, itemType))) {
                count += Math.round(stack.getSize());
            }
        }
        return count;
    }

    static String itemDisplayName(StockItemType itemType, String itemId) {
        try {
            if (StockItemType.WING.equals(itemType)) {
                return Global.getSettings().getFighterWingSpec(itemId).getWingName();
            }
            return Global.getSettings().getWeaponSpec(itemId).getWeaponName();
        } catch (Throwable ignored) {
            return itemId;
        }
    }

    static void addItem(CargoAPI cargo, StockItemType itemType, String itemId, int quantity) {
        if (StockItemType.WING.equals(itemType)) {
            cargo.addFighters(itemId, quantity);
        } else {
            cargo.addWeapons(itemId, quantity);
        }
    }

    static void removeItem(CargoAPI cargo, StockItemType itemType, String itemId, int quantity) {
        if (StockItemType.WING.equals(itemType)) {
            cargo.removeFighters(itemId, quantity);
        } else {
            cargo.removeWeapons(itemId, quantity);
        }
    }

    static void reconcileItemCount(CargoAPI cargo, StockItemType itemType, String itemId, int expectedCount) {
        if (cargo == null || expectedCount < 0) {
            return;
        }
        int currentCount = itemCount(cargo, itemType, itemId);
        if (currentCount < expectedCount) {
            addItem(cargo, itemType, itemId, expectedCount - currentCount);
        } else if (currentCount > expectedCount) {
            removeItem(cargo, itemType, itemId, currentCount - expectedCount);
        }
        tidyCargo(cargo);
    }

    static void tidyCargo(CargoAPI cargo) {
        if (cargo == null) {
            return;
        }
        cargo.removeEmptyStacks();
        cargo.sort();
        cargo.updateSpaceUsed();
    }
}
