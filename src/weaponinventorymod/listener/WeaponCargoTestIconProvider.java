package weaponinventorymod.listener;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.GenericPluginManagerAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.listeners.CommodityIconProvider;
import com.fs.starfarer.api.impl.PlayerFleetPersonnelTracker;

public class WeaponCargoTestIconProvider implements CommodityIconProvider {
    private static final int PRIORITY = 100;
    private static final String SPRITE_CATEGORY = "ui";
    private static final String SPRITE_KEY = "weapon_inventory_test_marker";

    public static void register() {
        SectorAPI sector = Global.getSector();
        if (sector == null) {
            return;
        }

        GenericPluginManagerAPI plugins = sector.getGenericPlugins();
        if (plugins == null) {
            return;
        }

        java.util.List<?> existing = plugins.getPluginsOfClass(WeaponCargoTestIconProvider.class);
        if (existing.isEmpty()) {
            plugins.addPlugin(new WeaponCargoTestIconProvider(), true);
        }
    }

    @Override
    public int getHandlingPriority(Object params) {
        return PRIORITY;
    }

    @Override
    public String getRankIconName(CargoStackAPI stack) {
        if (stack == null || !stack.isWeaponStack()) {
            return getDefaultRankIconName(stack);
        }

        try {
            return Global.getSettings().getSpriteName(SPRITE_CATEGORY, SPRITE_KEY);
        } catch (RuntimeException ignored) {
            return getDefaultRankIconName(stack);
        }
    }

    @Override
    public String getIconName(CargoStackAPI stack) {
        return null;
    }

    private String getDefaultRankIconName(CargoStackAPI stack) {
        if (stack == null) {
            return null;
        }
        return PlayerFleetPersonnelTracker.getInstance().getRankIconName(stack);
    }
}
