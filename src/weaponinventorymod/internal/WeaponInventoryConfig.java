package weaponinventorymod.internal;

import lunalib.lunaSettings.LunaSettings;
import org.apache.log4j.Logger;

public final class WeaponInventoryConfig {
    private static final Logger LOG = Logger.getLogger(WeaponInventoryConfig.class);

    private static final String MOD_ID = "weapon_inventory_mod";
    private static final String SETTING_UPDATE_INTERVAL = "wim_update_interval_seconds";
    private static final String SETTING_ENABLE_PATCHED_BADGES = "wim_enable_patched_badges";
    private static final String SETTING_ENABLE_GLOBAL_MARKET_TAG_INFERENCE = "wim_enable_global_market_tag_inference";
    private static final String KEY_UPDATE_INTERVAL = "wim.config.updateIntervalSeconds";
    private static final String KEY_PATCHED_BADGES_ENABLED = "wim.config.patchedBadgesEnabled";
    private static final String KEY_GLOBAL_MARKET_TAG_INFERENCE_ENABLED = "wim.config.globalMarketTagInferenceEnabled";

    private static final float DEFAULT_UPDATE_INTERVAL_SEC = 0.20f;
    private static final float MIN_UPDATE_INTERVAL_SEC = 0.05f;
    private static final float MAX_UPDATE_INTERVAL_SEC = 2.00f;
    private static final int MAX_CONFIG_LOGS = 10;

    private static int configLogs = 0;
    private static boolean configErrorLogged = false;

    private WeaponInventoryConfig() {
    }

    public static float refreshAndPublishUpdateIntervalSeconds() {
        float effective = DEFAULT_UPDATE_INTERVAL_SEC;
        boolean badgesEnabled = true;
        boolean globalMarketTagInferenceEnabled = true;
        try {
            Double value = LunaSettings.getDouble(MOD_ID, SETTING_UPDATE_INTERVAL);
            if (value != null) {
                effective = (float) value.doubleValue();
            }
            Boolean enabled = LunaSettings.getBoolean(MOD_ID, SETTING_ENABLE_PATCHED_BADGES);
            if (enabled != null) {
                badgesEnabled = enabled.booleanValue();
            }
            Boolean inferenceEnabled = LunaSettings.getBoolean(MOD_ID, SETTING_ENABLE_GLOBAL_MARKET_TAG_INFERENCE);
            if (inferenceEnabled != null) {
                globalMarketTagInferenceEnabled = inferenceEnabled.booleanValue();
            }
        } catch (Throwable t) {
            if (!configErrorLogged) {
                configErrorLogged = true;
                LOG.error("WIM_CONFIG luna settings read error", t);
            }
        }

        effective = clamp(effective, MIN_UPDATE_INTERVAL_SEC, MAX_UPDATE_INTERVAL_SEC);
        System.setProperty(KEY_UPDATE_INTERVAL, Float.toString(effective));
        System.setProperty(KEY_PATCHED_BADGES_ENABLED, Boolean.toString(badgesEnabled));
        System.setProperty(KEY_GLOBAL_MARKET_TAG_INFERENCE_ENABLED, Boolean.toString(globalMarketTagInferenceEnabled));
        if (configLogs < MAX_CONFIG_LOGS) {
            configLogs++;
            LOG.info("WIM_CONFIG updateIntervalSeconds=" + effective
                    + " patchedBadgesEnabled=" + badgesEnabled
                    + " globalMarketTagInferenceEnabled=" + globalMarketTagInferenceEnabled);
        }
        return effective;
    }

    public static boolean isGlobalMarketTagInferenceEnabled() {
        return Boolean.parseBoolean(System.getProperty(KEY_GLOBAL_MARKET_TAG_INFERENCE_ENABLED, "true"));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
