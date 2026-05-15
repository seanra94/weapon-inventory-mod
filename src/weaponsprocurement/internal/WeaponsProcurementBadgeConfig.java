package weaponsprocurement.internal;

import lunalib.lunaSettings.LunaSettings;
import org.apache.log4j.Logger;

public final class WeaponsProcurementBadgeConfig {
    private static final Logger LOG = Logger.getLogger(WeaponsProcurementBadgeConfig.class);

    private static final String MOD_ID = "weapons_procurement";
    private static final String SETTING_UPDATE_INTERVAL = "wp_update_interval_seconds";
    private static final String SETTING_ENABLE_PATCHED_BADGES = "wp_enable_patched_badges";
    private static final String KEY_UPDATE_INTERVAL = "wp.config.updateIntervalSeconds";
    private static final String KEY_PATCHED_BADGES_ENABLED = "wp.config.patchedBadgesEnabled";
    private static final String KEY_PATCHED_BADGES_OVERRIDE = "wp.private.patchedBadgesEnabled";
    private static final float DEFAULT_UPDATE_INTERVAL_SEC = 0.20f;
    private static final float MIN_UPDATE_INTERVAL_SEC = 0.05f;
    private static final float MAX_UPDATE_INTERVAL_SEC = 2.00f;
    private static boolean configErrorLogged = false;

    private WeaponsProcurementBadgeConfig() {
    }

    public static float refreshAndPublishBadgeSettings() {
        float effective = DEFAULT_UPDATE_INTERVAL_SEC;
        String override = System.getProperty(KEY_PATCHED_BADGES_OVERRIDE);
        boolean overrideSet = override != null && !override.trim().isEmpty();
        boolean enabled = overrideSet ? Boolean.parseBoolean(override) : true;
        try {
            Double value = LunaSettings.getDouble(MOD_ID, SETTING_UPDATE_INTERVAL);
            if (value != null) {
                effective = (float) value.doubleValue();
            }
            Boolean badgesEnabled = overrideSet ? null : LunaSettings.getBoolean(MOD_ID, SETTING_ENABLE_PATCHED_BADGES);
            if (!overrideSet && badgesEnabled != null) {
                enabled = badgesEnabled.booleanValue();
            }
        } catch (Throwable t) {
            if (!configErrorLogged) {
                configErrorLogged = true;
                LOG.error("WP_BADGE_CONFIG luna settings read error", t);
            }
        }
        effective = clamp(effective, MIN_UPDATE_INTERVAL_SEC, MAX_UPDATE_INTERVAL_SEC);
        System.setProperty(KEY_UPDATE_INTERVAL, Float.toString(effective));
        System.setProperty(KEY_PATCHED_BADGES_ENABLED, Boolean.toString(enabled));
        return effective;
    }

    public static boolean isEnabled() {
        return Boolean.parseBoolean(System.getProperty(KEY_PATCHED_BADGES_ENABLED, "false"));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
