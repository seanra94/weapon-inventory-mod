package weaponinventorymod.listener;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.GenericPluginManagerAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.listeners.CommodityIconProvider;
import com.fs.starfarer.api.impl.PlayerFleetPersonnelTracker;
import org.apache.log4j.Logger;

public class WeaponCargoTestIconProvider implements CommodityIconProvider {
    private static final Logger LOG = Global.getLogger(WeaponCargoTestIconProvider.class);
    private static final int ACTIVE_PRIORITY = GenericPluginManagerAPI.MOD_GENERAL;
    private static final int NO_PRIORITY = -1;
    private static final String SPRITE_CATEGORY = "ui";
    private static final String SPRITE_KEY = "weapon_inventory_test_marker";
    private static boolean registrationLogged = false;
    private static boolean spriteLookupFailureLogged = false;
    private static boolean unknownParamsLogged = false;

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
            if (!registrationLogged) {
                registrationLogged = true;
                LOG.info("Registered WeaponCargoTestIconProvider in GenericPluginManagerAPI");
            }
            return;
        }

        if (!registrationLogged) {
            registrationLogged = true;
            LOG.info("WeaponCargoTestIconProvider already registered in GenericPluginManagerAPI");
        }
    }

    @Override
    public int getHandlingPriority(Object params) {
        if (params instanceof CargoStackAPI) {
            CargoStackAPI stack = (CargoStackAPI) params;
            return stack.isWeaponStack() ? ACTIVE_PRIORITY : NO_PRIORITY;
        }

        // Unknown params are intentionally not inspected for members because the game sandbox blocks such probing.
        if (!unknownParamsLogged && params != null) {
            unknownParamsLogged = true;
            LOG.info("getHandlingPriority received unknown params type: " + params.getClass().getName());
        }
        return ACTIVE_PRIORITY;
    }

    @Override
    public String getRankIconName(CargoStackAPI stack) {
        return getDefaultRankIconName(stack);
    }

    @Override
    public String getIconName(CargoStackAPI stack) {
        if (stack == null || !stack.isWeaponStack()) {
            return null;
        }
        return getTestMarkerSpriteName();
    }

    private String getDefaultRankIconName(CargoStackAPI stack) {
        if (stack == null) {
            return null;
        }
        return PlayerFleetPersonnelTracker.getInstance().getRankIconName(stack);
    }

    private String getTestMarkerSpriteName() {
        try {
            String spriteName = Global.getSettings().getSpriteName(SPRITE_CATEGORY, SPRITE_KEY);
            if (spriteName == null || spriteName.isEmpty()) {
                logSpriteLookupFailureOnce("Sprite lookup returned empty name for ui.weapon_inventory_test_marker", null);
                return null;
            }
            return spriteName;
        } catch (RuntimeException ex) {
            logSpriteLookupFailureOnce("Failed to resolve sprite ui.weapon_inventory_test_marker", ex);
            return null;
        }
    }

    private void logSpriteLookupFailureOnce(String message, RuntimeException ex) {
        if (spriteLookupFailureLogged) {
            return;
        }
        spriteLookupFailureLogged = true;
        if (ex == null) {
            LOG.error(message);
            return;
        }
        LOG.error(message, ex);
    }
}
