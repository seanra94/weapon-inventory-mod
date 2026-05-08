package weaponinventorymod.internal;

import lunalib.lunaSettings.LunaSettings;
import org.apache.log4j.Logger;

public final class WeaponInventoryConfig {
    private static final Logger LOG = Logger.getLogger(WeaponInventoryConfig.class);

    private static final String MOD_ID = "weapon_inventory_mod";
    private static final String SETTING_UPDATE_INTERVAL = "wim_update_interval_seconds";
    private static final String SETTING_ENABLE_PATCHED_BADGES = "wim_enable_patched_badges";
    private static final String SETTING_ENABLE_GLOBAL_MARKET_TAG_INFERENCE = "wim_enable_global_market_tag_inference";
    private static final String SETTING_GLOBAL_MARKET_PRICE_MULTIPLIER = "wim_global_market_price_multiplier";
    private static final String SETTING_SECTOR_MARKET_PRICE_MULTIPLIER = "wim_sector_market_price_multiplier";
    private static final String SETTING_SECRET_MARKET_PRICE_MULTIPLIER = "wim_secret_market_price_multiplier";
    private static final String SETTING_DESIRED_SMALL_WEAPON_COUNT = "wim_desired_small_weapon_count";
    private static final String SETTING_DESIRED_MEDIUM_WEAPON_COUNT = "wim_desired_medium_weapon_count";
    private static final String SETTING_DESIRED_LARGE_WEAPON_COUNT = "wim_desired_large_weapon_count";
    private static final String SETTING_DESIRED_FIGHTER_WING_COUNT = "wim_desired_fighter_wing_count";
    private static final String KEY_UPDATE_INTERVAL = "wim.config.updateIntervalSeconds";
    private static final String KEY_PATCHED_BADGES_ENABLED = "wim.config.patchedBadgesEnabled";
    private static final String KEY_GLOBAL_MARKET_TAG_INFERENCE_ENABLED = "wim.config.globalMarketTagInferenceEnabled";
    private static final String KEY_GLOBAL_MARKET_PRICE_MULTIPLIER = "wim.config.globalMarketPriceMultiplier";
    private static final String KEY_SECTOR_MARKET_PRICE_MULTIPLIER = "wim.config.sectorMarketPriceMultiplier";
    private static final String KEY_SECRET_MARKET_PRICE_MULTIPLIER = "wim.config.secretMarketPriceMultiplier";
    private static final String KEY_DESIRED_SMALL_WEAPON_COUNT = "wim.config.desiredSmallWeaponCount";
    private static final String KEY_DESIRED_MEDIUM_WEAPON_COUNT = "wim.config.desiredMediumWeaponCount";
    private static final String KEY_DESIRED_LARGE_WEAPON_COUNT = "wim.config.desiredLargeWeaponCount";
    private static final String KEY_DESIRED_FIGHTER_WING_COUNT = "wim.config.desiredFighterWingCount";

    private static final float DEFAULT_UPDATE_INTERVAL_SEC = 0.20f;
    private static final float MIN_UPDATE_INTERVAL_SEC = 0.05f;
    private static final float MAX_UPDATE_INTERVAL_SEC = 2.00f;
    private static final float DEFAULT_GLOBAL_MARKET_PRICE_MULTIPLIER = 4.00f;
    private static final float DEFAULT_SECTOR_MARKET_PRICE_MULTIPLIER = 3.00f;
    private static final float DEFAULT_SECRET_MARKET_PRICE_MULTIPLIER = 5.00f;
    private static final float MIN_GLOBAL_MARKET_PRICE_MULTIPLIER = 1.00f;
    private static final float MAX_GLOBAL_MARKET_PRICE_MULTIPLIER = 20.00f;
    private static final int DEFAULT_DESIRED_SMALL_WEAPON_COUNT = 16;
    private static final int DEFAULT_DESIRED_MEDIUM_WEAPON_COUNT = 8;
    private static final int DEFAULT_DESIRED_LARGE_WEAPON_COUNT = 4;
    private static final int DEFAULT_DESIRED_FIGHTER_WING_COUNT = 4;
    private static final int MIN_DESIRED_WEAPON_COUNT = 0;
    private static final int MAX_DESIRED_WEAPON_COUNT = 999;
    private static final int MAX_CONFIG_LOGS = 10;

    private static int configLogs = 0;
    private static boolean configErrorLogged = false;

    private WeaponInventoryConfig() {
    }

    public static float refreshAndPublishUpdateIntervalSeconds() {
        return refreshAndPublishSettings();
    }

    public static float refreshAndPublishSettings() {
        float effective = DEFAULT_UPDATE_INTERVAL_SEC;
        boolean badgesEnabled = true;
        boolean globalMarketTagInferenceEnabled = true;
        float globalMarketPriceMultiplier = DEFAULT_GLOBAL_MARKET_PRICE_MULTIPLIER;
        float sectorMarketPriceMultiplier = DEFAULT_SECTOR_MARKET_PRICE_MULTIPLIER;
        float secretMarketPriceMultiplier = DEFAULT_SECRET_MARKET_PRICE_MULTIPLIER;
        int desiredSmallWeaponCount = DEFAULT_DESIRED_SMALL_WEAPON_COUNT;
        int desiredMediumWeaponCount = DEFAULT_DESIRED_MEDIUM_WEAPON_COUNT;
        int desiredLargeWeaponCount = DEFAULT_DESIRED_LARGE_WEAPON_COUNT;
        int desiredFighterWingCount = DEFAULT_DESIRED_FIGHTER_WING_COUNT;
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
            Double multiplier = LunaSettings.getDouble(MOD_ID, SETTING_GLOBAL_MARKET_PRICE_MULTIPLIER);
            if (multiplier != null) {
                globalMarketPriceMultiplier = (float) multiplier.doubleValue();
            }
            Double sectorMultiplier = LunaSettings.getDouble(MOD_ID, SETTING_SECTOR_MARKET_PRICE_MULTIPLIER);
            if (sectorMultiplier != null) {
                sectorMarketPriceMultiplier = (float) sectorMultiplier.doubleValue();
            }
            Double secretMultiplier = LunaSettings.getDouble(MOD_ID, SETTING_SECRET_MARKET_PRICE_MULTIPLIER);
            if (secretMultiplier != null) {
                secretMarketPriceMultiplier = (float) secretMultiplier.doubleValue();
            }
            desiredSmallWeaponCount = readDesiredWeaponCount(SETTING_DESIRED_SMALL_WEAPON_COUNT, DEFAULT_DESIRED_SMALL_WEAPON_COUNT);
            desiredMediumWeaponCount = readDesiredWeaponCount(SETTING_DESIRED_MEDIUM_WEAPON_COUNT, DEFAULT_DESIRED_MEDIUM_WEAPON_COUNT);
            desiredLargeWeaponCount = readDesiredWeaponCount(SETTING_DESIRED_LARGE_WEAPON_COUNT, DEFAULT_DESIRED_LARGE_WEAPON_COUNT);
            desiredFighterWingCount = readDesiredWeaponCount(SETTING_DESIRED_FIGHTER_WING_COUNT, DEFAULT_DESIRED_FIGHTER_WING_COUNT);
        } catch (Throwable t) {
            if (!configErrorLogged) {
                configErrorLogged = true;
                LOG.error("WIM_CONFIG luna settings read error", t);
            }
        }

        effective = clamp(effective, MIN_UPDATE_INTERVAL_SEC, MAX_UPDATE_INTERVAL_SEC);
        globalMarketPriceMultiplier = clamp(globalMarketPriceMultiplier, MIN_GLOBAL_MARKET_PRICE_MULTIPLIER, MAX_GLOBAL_MARKET_PRICE_MULTIPLIER);
        sectorMarketPriceMultiplier = clamp(sectorMarketPriceMultiplier, MIN_GLOBAL_MARKET_PRICE_MULTIPLIER, MAX_GLOBAL_MARKET_PRICE_MULTIPLIER);
        secretMarketPriceMultiplier = clamp(secretMarketPriceMultiplier, MIN_GLOBAL_MARKET_PRICE_MULTIPLIER, MAX_GLOBAL_MARKET_PRICE_MULTIPLIER);
        System.setProperty(KEY_UPDATE_INTERVAL, Float.toString(effective));
        System.setProperty(KEY_PATCHED_BADGES_ENABLED, Boolean.toString(badgesEnabled));
        System.setProperty(KEY_GLOBAL_MARKET_TAG_INFERENCE_ENABLED, Boolean.toString(globalMarketTagInferenceEnabled));
        System.setProperty(KEY_GLOBAL_MARKET_PRICE_MULTIPLIER, Float.toString(globalMarketPriceMultiplier));
        System.setProperty(KEY_SECTOR_MARKET_PRICE_MULTIPLIER, Float.toString(sectorMarketPriceMultiplier));
        System.setProperty(KEY_SECRET_MARKET_PRICE_MULTIPLIER, Float.toString(secretMarketPriceMultiplier));
        System.setProperty(KEY_DESIRED_SMALL_WEAPON_COUNT, Integer.toString(desiredSmallWeaponCount));
        System.setProperty(KEY_DESIRED_MEDIUM_WEAPON_COUNT, Integer.toString(desiredMediumWeaponCount));
        System.setProperty(KEY_DESIRED_LARGE_WEAPON_COUNT, Integer.toString(desiredLargeWeaponCount));
        System.setProperty(KEY_DESIRED_FIGHTER_WING_COUNT, Integer.toString(desiredFighterWingCount));
        if (configLogs < MAX_CONFIG_LOGS) {
            configLogs++;
            LOG.info("WIM_CONFIG updateIntervalSeconds=" + effective
                    + " patchedBadgesEnabled=" + badgesEnabled
                    + " globalMarketTagInferenceEnabled=" + globalMarketTagInferenceEnabled
                    + " globalMarketPriceMultiplier=" + globalMarketPriceMultiplier
                    + " sectorMarketPriceMultiplier=" + sectorMarketPriceMultiplier
                    + " secretMarketPriceMultiplier=" + secretMarketPriceMultiplier
                    + " desiredSmallWeaponCount=" + desiredSmallWeaponCount
                    + " desiredMediumWeaponCount=" + desiredMediumWeaponCount
                    + " desiredLargeWeaponCount=" + desiredLargeWeaponCount
                    + " desiredFighterWingCount=" + desiredFighterWingCount);
        }
        return effective;
    }

    public static boolean isGlobalMarketTagInferenceEnabled() {
        return Boolean.parseBoolean(System.getProperty(KEY_GLOBAL_MARKET_TAG_INFERENCE_ENABLED, "true"));
    }

    public static float globalMarketPriceMultiplier() {
        return readMultiplier(KEY_GLOBAL_MARKET_PRICE_MULTIPLIER, DEFAULT_GLOBAL_MARKET_PRICE_MULTIPLIER);
    }

    public static float sectorMarketPriceMultiplier() {
        return readMultiplier(KEY_SECTOR_MARKET_PRICE_MULTIPLIER, DEFAULT_SECTOR_MARKET_PRICE_MULTIPLIER);
    }

    public static float secretMarketPriceMultiplier() {
        return readMultiplier(KEY_SECRET_MARKET_PRICE_MULTIPLIER, DEFAULT_SECRET_MARKET_PRICE_MULTIPLIER);
    }

    private static float readMultiplier(String propertyKey, float defaultValue) {
        try {
            return clamp(Float.parseFloat(System.getProperty(propertyKey,
                            Float.toString(defaultValue))),
                    MIN_GLOBAL_MARKET_PRICE_MULTIPLIER,
                    MAX_GLOBAL_MARKET_PRICE_MULTIPLIER);
        } catch (Throwable ignored) {
            return defaultValue;
        }
    }

    public static int desiredSmallWeaponCount(int fallback) {
        return readPublishedDesiredWeaponCount(KEY_DESIRED_SMALL_WEAPON_COUNT, fallback);
    }

    public static int desiredMediumWeaponCount(int fallback) {
        return readPublishedDesiredWeaponCount(KEY_DESIRED_MEDIUM_WEAPON_COUNT, fallback);
    }

    public static int desiredLargeWeaponCount(int fallback) {
        return readPublishedDesiredWeaponCount(KEY_DESIRED_LARGE_WEAPON_COUNT, fallback);
    }

    public static int desiredFighterWingCount(int fallback) {
        return readPublishedDesiredWeaponCount(KEY_DESIRED_FIGHTER_WING_COUNT, fallback);
    }

    private static int readDesiredWeaponCount(String settingId, int defaultValue) {
        Double value = LunaSettings.getDouble(MOD_ID, settingId);
        if (value == null) {
            return defaultValue;
        }
        return clamp(Math.round(value.floatValue()), MIN_DESIRED_WEAPON_COUNT, MAX_DESIRED_WEAPON_COUNT);
    }

    private static int readPublishedDesiredWeaponCount(String propertyKey, int fallback) {
        try {
            return clamp(Integer.parseInt(System.getProperty(propertyKey, Integer.toString(fallback))),
                    MIN_DESIRED_WEAPON_COUNT,
                    MAX_DESIRED_WEAPON_COUNT);
        } catch (Throwable ignored) {
            return clamp(fallback, MIN_DESIRED_WEAPON_COUNT, MAX_DESIRED_WEAPON_COUNT);
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
