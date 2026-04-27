package weaponinventorymod.listener;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.GenericPluginManagerAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.listeners.CommodityIconProvider;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import org.apache.log4j.Logger;

public class WeaponCargoTestIconProvider implements CommodityIconProvider {
    private static final Logger LOG = Global.getLogger(WeaponCargoTestIconProvider.class);
    private static final int ACTIVE_PRIORITY = GenericPluginManagerAPI.MOD_GENERAL;
    private static final int NO_PRIORITY = -1;
    private static final String SPRITE_CATEGORY = "ui";
    private static final String SPRITE_KEY = "weapon_inventory_test_marker";
    private static final int MAX_DIAGNOSTIC_CALL_LOGS = 6;
    private static boolean registrationLogged = false;
    private static boolean firstPriorityParamLogged = false;
    private static int iconCallLogsWritten = 0;
    private static int rankCallLogsWritten = 0;
    private static boolean spriteLookupFailureLogged = false;
    private static boolean spriteLookupSuccessLogged = false;
    private static boolean spriteResolved = false;
    private static String resolvedSpriteName = null;

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
                LOG.info("WIM_DIAG register: provider added to GenericPluginManagerAPI");
            }
            return;
        }

        if (!registrationLogged) {
            registrationLogged = true;
            LOG.info("WIM_DIAG register: provider already present in GenericPluginManagerAPI");
        }
    }

    @Override
    public int getHandlingPriority(Object params) {
        if (!firstPriorityParamLogged) {
            firstPriorityParamLogged = true;
            String className = params == null ? "null" : params.getClass().getName();
            LOG.info("WIM_DIAG priority: first params class = " + className);
        }

        if (params instanceof CargoStackAPI) {
            CargoStackAPI stack = (CargoStackAPI) params;
            return stack.isWeaponStack() ? ACTIVE_PRIORITY : NO_PRIORITY;
        }

        // Diagnostic mode: unknown params are not inspected and are treated as active candidates.
        return ACTIVE_PRIORITY;
    }

    @Override
    public String getRankIconName(CargoStackAPI stack) {
        logRankCall(stack);
        if (stack == null) {
            return null;
        }

        // Diagnostic-only behavior: force marker here to determine whether rank-icon path is active.
        return getTestMarkerSpriteName();
    }

    @Override
    public String getIconName(CargoStackAPI stack) {
        logIconCall(stack);
        if (stack == null) {
            return null;
        }

        // Diagnostic-only behavior: force marker here to determine whether icon path is active.
        return getTestMarkerSpriteName();
    }

    private void logIconCall(CargoStackAPI stack) {
        if (iconCallLogsWritten >= MAX_DIAGNOSTIC_CALL_LOGS) {
            return;
        }
        iconCallLogsWritten++;
        LOG.info("WIM_DIAG getIconName call#" + iconCallLogsWritten + " " + describeStackSafe(stack));
    }

    private void logRankCall(CargoStackAPI stack) {
        if (rankCallLogsWritten >= MAX_DIAGNOSTIC_CALL_LOGS) {
            return;
        }
        rankCallLogsWritten++;
        LOG.info("WIM_DIAG getRankIconName call#" + rankCallLogsWritten + " " + describeStackSafe(stack));
    }

    private String describeStackSafe(CargoStackAPI stack) {
        if (stack == null) {
            return "stack=null";
        }

        String commodityId = nullToText(safeCommodityId(stack));
        String weaponId = nullToText(safeWeaponId(stack));
        return "stack=nonNull"
                + " isWeapon=" + safeBoolean(stack.isWeaponStack())
                + " isCommodity=" + safeBoolean(stack.isCommodityStack())
                + " isNullStack=" + safeBoolean(stack.isNull())
                + " inPlayerCargo=" + safeBoolean(stack.isInPlayerCargo())
                + " pickedUp=" + safeBoolean(stack.isPickedUp())
                + " type=" + nullToText(stack.getType())
                + " commodityId=" + commodityId
                + " weaponId=" + weaponId
                + " size=" + stack.getSize();
    }

    private String safeCommodityId(CargoStackAPI stack) {
        try {
            return stack.getCommodityId();
        } catch (RuntimeException ex) {
            return "<err>";
        }
    }

    private String safeWeaponId(CargoStackAPI stack) {
        try {
            WeaponSpecAPI spec = stack.getWeaponSpecIfWeapon();
            return spec == null ? null : spec.getWeaponId();
        } catch (RuntimeException ex) {
            return "<err>";
        }
    }

    private String getTestMarkerSpriteName() {
        if (!spriteResolved) {
            spriteResolved = true;
            resolveSpriteNameOnce();
        }

        return resolvedSpriteName;
    }

    private void resolveSpriteNameOnce() {
        try {
            String spriteName = Global.getSettings().getSpriteName(SPRITE_CATEGORY, SPRITE_KEY);
            if (spriteName == null || spriteName.isEmpty()) {
                logSpriteLookupFailureOnce("WIM_DIAG sprite lookup returned empty name for ui.weapon_inventory_test_marker", null);
                return;
            }
            resolvedSpriteName = spriteName;
            if (!spriteLookupSuccessLogged) {
                spriteLookupSuccessLogged = true;
                LOG.info("WIM_DIAG sprite resolved: " + spriteName);
            }
        } catch (RuntimeException ex) {
            logSpriteLookupFailureOnce("WIM_DIAG sprite lookup failed for ui.weapon_inventory_test_marker", ex);
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

    private String nullToText(Object value) {
        return value == null ? "null" : String.valueOf(value);
    }

    private String safeBoolean(boolean value) {
        return value ? "true" : "false";
    }
}
