package weaponsprocurement.core;

import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SubmarketPlugin;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;

public final class StockItemStacks {
    private StockItemStacks() {
    }

    public static boolean isVisibleWeaponStack(CargoStackAPI stack) {
        return stack != null && stack.isWeaponStack() && stack.getWeaponSpecIfWeapon() != null && stack.getSize() > 0f;
    }

    public static boolean isVisibleWingStack(CargoStackAPI stack) {
        return stack != null && stack.isFighterWingStack() && stack.getFighterWingSpecIfWing() != null && stack.getSize() > 0f;
    }

    public static boolean isVisibleItemStack(CargoStackAPI stack, StockItemType itemType) {
        return StockItemType.WING.equals(itemType) ? isVisibleWingStack(stack) : isVisibleWeaponStack(stack);
    }

    public static boolean isPurchasableWeaponStack(SubmarketAPI submarket, CargoStackAPI stack) {
        if (submarket == null || stack == null || !stack.isWeaponStack() || stack.getWeaponSpecIfWeapon() == null) {
            return false;
        }
        SubmarketPlugin plugin = submarket.getPlugin();
        if (plugin != null && plugin.isIllegalOnSubmarket(stack, SubmarketPlugin.TransferAction.PLAYER_BUY)) {
            return false;
        }
        return stack.getSize() > 0f;
    }

    public static boolean isPurchasableWingStack(SubmarketAPI submarket, CargoStackAPI stack) {
        if (submarket == null || stack == null || !stack.isFighterWingStack() || stack.getFighterWingSpecIfWing() == null) {
            return false;
        }
        SubmarketPlugin plugin = submarket.getPlugin();
        if (plugin != null && plugin.isIllegalOnSubmarket(stack, SubmarketPlugin.TransferAction.PLAYER_BUY)) {
            return false;
        }
        return stack.getSize() > 0f;
    }

    public static boolean isPurchasableItemStack(SubmarketAPI submarket, CargoStackAPI stack, StockItemType itemType) {
        return StockItemType.WING.equals(itemType)
                ? isPurchasableWingStack(submarket, stack)
                : isPurchasableWeaponStack(submarket, stack);
    }

    public static String itemId(CargoStackAPI stack, StockItemType itemType) {
        if (stack == null) {
            return null;
        }
        if (StockItemType.WING.equals(itemType)) {
            return stack.getFighterWingSpecIfWing() == null ? null : stack.getFighterWingSpecIfWing().getId();
        }
        return stack.getWeaponSpecIfWeapon() == null ? null : stack.getWeaponSpecIfWeapon().getWeaponId();
    }

    public static int unitPrice(SubmarketAPI submarket, CargoStackAPI stack) {
        if (stack == null) {
            return 0;
        }
        float tariff = tariff(submarket);
        return Math.max(0, Math.round(stack.getBaseValuePerUnit() * (1f + Math.max(0f, tariff))));
    }

    public static int baseUnitPrice(CargoStackAPI stack) {
        if (stack == null) {
            return 0;
        }
        return Math.max(0, Math.round(stack.getBaseValuePerUnit()));
    }

    public static int sellUnitPrice(SubmarketAPI submarket, CargoStackAPI stack) {
        if (stack == null) {
            return 0;
        }
        float tariff = tariff(submarket);
        return Math.max(0, Math.round(stack.getBaseValuePerUnit() * (1f - Math.max(0f, tariff))));
    }

    public static float unitCargoSpace(CargoStackAPI stack) {
        if (stack == null) {
            return 1f;
        }
        float value = stack.getCargoSpacePerUnit();
        return value <= 0f ? 1f : value;
    }

    private static float tariff(SubmarketAPI submarket) {
        if (submarket == null) {
            return 0f;
        }
        SubmarketPlugin plugin = submarket.getPlugin();
        return plugin == null ? submarket.getTariff() : plugin.getTariff();
    }
}
