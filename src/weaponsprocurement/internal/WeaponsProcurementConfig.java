package weaponsprocurement.internal;

import lunalib.lunaSettings.LunaSettings;
import org.apache.log4j.Logger;

public final class WeaponsProcurementConfig {
    private static final Logger LOG = Logger.getLogger(WeaponsProcurementConfig.class);

    private static final String MOD_ID = "weapons_procurement";
    private static final String SETTING_UPDATE_INTERVAL = "wp_update_interval_seconds";
    private static final String SETTING_ENABLE_DIALOG_OPTION = "wp_enable_dialog_option";
    private static final String SETTING_ENABLE_SECTOR_MARKET = "wp_enable_sector_market";
    private static final String SETTING_ENABLE_FIXERS_MARKET = "wp_enable_fixers_market";
    private static final String SETTING_ENABLE_FIXERS_MARKET_TAG_INFERENCE = "wp_enable_fixers_market_tag_inference";
    private static final String SETTING_SECTOR_MARKET_PRICE_MULTIPLIER = "wp_sector_market_price_multiplier";
    private static final String SETTING_FIXERS_MARKET_PRICE_MULTIPLIER = "wp_fixers_market_price_multiplier";
    private static final String SETTING_DESIRED_SMALL_WEAPON_COUNT = "wp_desired_small_weapon_count";
    private static final String SETTING_DESIRED_MEDIUM_WEAPON_COUNT = "wp_desired_medium_weapon_count";
    private static final String SETTING_DESIRED_LARGE_WEAPON_COUNT = "wp_desired_large_weapon_count";
    private static final String SETTING_DESIRED_FIGHTER_WING_COUNT = "wp_desired_fighter_wing_count";
    private static final String KEY_UPDATE_INTERVAL = "wp.config.updateIntervalSeconds";
    private static final String KEY_DIALOG_OPTION_ENABLED = "wp.config.dialogOptionEnabled";
    private static final String KEY_SECTOR_MARKET_ENABLED = "wp.config.sectorMarketEnabled";
    private static final String KEY_FIXERS_MARKET_ENABLED = "wp.config.fixersMarketEnabled";
    private static final String KEY_FIXERS_MARKET_TAG_INFERENCE_ENABLED = "wp.config.fixersMarketTagInferenceEnabled";
    private static final String KEY_SECTOR_MARKET_PRICE_MULTIPLIER = "wp.config.sectorMarketPriceMultiplier";
    private static final String KEY_FIXERS_MARKET_PRICE_MULTIPLIER = "wp.config.fixersMarketPriceMultiplier";
    private static final String KEY_DESIRED_SMALL_WEAPON_COUNT = "wp.config.desiredSmallWeaponCount";
    private static final String KEY_DESIRED_MEDIUM_WEAPON_COUNT = "wp.config.desiredMediumWeaponCount";
    private static final String KEY_DESIRED_LARGE_WEAPON_COUNT = "wp.config.desiredLargeWeaponCount";
    private static final String KEY_DESIRED_FIGHTER_WING_COUNT = "wp.config.desiredFighterWingCount";
    public static final String KEY_DEBUG_TRADE_FAILURE_STEP = "wp.debug.failTradeStep";

    private static final float DEFAULT_UPDATE_INTERVAL_SEC = 0.20f;
    private static final float MIN_UPDATE_INTERVAL_SEC = 0.05f;
    private static final float MAX_UPDATE_INTERVAL_SEC = 2.00f;
    private static final float DEFAULT_SECTOR_MARKET_PRICE_MULTIPLIER = 3.00f;
    private static final float DEFAULT_FIXERS_MARKET_PRICE_MULTIPLIER = 5.00f;
    private static final float MIN_REMOTE_MARKET_PRICE_MULTIPLIER = 1.00f;
    private static final float MAX_REMOTE_MARKET_PRICE_MULTIPLIER = 20.00f;
    private static final int DEFAULT_DESIRED_SMALL_WEAPON_COUNT = 16;
    private static final int DEFAULT_DESIRED_MEDIUM_WEAPON_COUNT = 8;
    private static final int DEFAULT_DESIRED_LARGE_WEAPON_COUNT = 4;
    private static final int DEFAULT_DESIRED_FIGHTER_WING_COUNT = 4;
    private static final int MIN_DESIRED_WEAPON_COUNT = 0;
    private static final int MAX_DESIRED_WEAPON_COUNT = 999;
    private static final int MAX_CONFIG_LOGS = 10;
    private static final String INITIAL_DEBUG_TRADE_FAILURE_STEP = System.getProperty(KEY_DEBUG_TRADE_FAILURE_STEP, "");

    private static int configLogs = 0;
    private static boolean configErrorLogged = false;

    private WeaponsProcurementConfig() {
    }

    public static float refreshAndPublishUpdateIntervalSeconds() {
        return refreshAndPublishSettings();
    }

    public static float refreshAndPublishSettings() {
        float effective = DEFAULT_UPDATE_INTERVAL_SEC;
        boolean dialogOptionEnabled = false;
        boolean sectorMarketEnabled = true;
        boolean fixersMarketEnabled = true;
        boolean fixersMarketTagInferenceEnabled = false;
        float sectorMarketPriceMultiplier = DEFAULT_SECTOR_MARKET_PRICE_MULTIPLIER;
        float fixersMarketPriceMultiplier = DEFAULT_FIXERS_MARKET_PRICE_MULTIPLIER;
        int desiredSmallWeaponCount = DEFAULT_DESIRED_SMALL_WEAPON_COUNT;
        int desiredMediumWeaponCount = DEFAULT_DESIRED_MEDIUM_WEAPON_COUNT;
        int desiredLargeWeaponCount = DEFAULT_DESIRED_LARGE_WEAPON_COUNT;
        int desiredFighterWingCount = DEFAULT_DESIRED_FIGHTER_WING_COUNT;
        String debugTradeFailureStep = "";

        Double updateInterval = readDoubleSetting(SETTING_UPDATE_INTERVAL);
        if (updateInterval != null) {
            effective = (float) updateInterval.doubleValue();
        }
        Boolean dialogOption = readBooleanSetting(SETTING_ENABLE_DIALOG_OPTION);
        if (dialogOption != null) {
            dialogOptionEnabled = dialogOption.booleanValue();
        }
        Boolean sectorEnabled = readBooleanSetting(SETTING_ENABLE_SECTOR_MARKET);
        if (sectorEnabled != null) {
            sectorMarketEnabled = sectorEnabled.booleanValue();
        }
        Boolean fixersEnabled = readBooleanSetting(SETTING_ENABLE_FIXERS_MARKET);
        if (fixersEnabled != null) {
            fixersMarketEnabled = fixersEnabled.booleanValue();
        }
        Boolean inferenceEnabled = readBooleanSetting(SETTING_ENABLE_FIXERS_MARKET_TAG_INFERENCE);
        if (inferenceEnabled != null) {
            fixersMarketTagInferenceEnabled = inferenceEnabled.booleanValue();
        }
        Double sectorMultiplier = readDoubleSetting(SETTING_SECTOR_MARKET_PRICE_MULTIPLIER);
        if (sectorMultiplier != null) {
            sectorMarketPriceMultiplier = (float) sectorMultiplier.doubleValue();
        }
        Double fixersMultiplier = readDoubleSetting(SETTING_FIXERS_MARKET_PRICE_MULTIPLIER);
        if (fixersMultiplier != null) {
            fixersMarketPriceMultiplier = (float) fixersMultiplier.doubleValue();
        }
        desiredSmallWeaponCount = readDesiredWeaponCount(SETTING_DESIRED_SMALL_WEAPON_COUNT, DEFAULT_DESIRED_SMALL_WEAPON_COUNT);
        desiredMediumWeaponCount = readDesiredWeaponCount(SETTING_DESIRED_MEDIUM_WEAPON_COUNT, DEFAULT_DESIRED_MEDIUM_WEAPON_COUNT);
        desiredLargeWeaponCount = readDesiredWeaponCount(SETTING_DESIRED_LARGE_WEAPON_COUNT, DEFAULT_DESIRED_LARGE_WEAPON_COUNT);
        desiredFighterWingCount = readDesiredWeaponCount(SETTING_DESIRED_FIGHTER_WING_COUNT, DEFAULT_DESIRED_FIGHTER_WING_COUNT);
        debugTradeFailureStep = readDebugTradeFailureStep();

        effective = clamp(effective, MIN_UPDATE_INTERVAL_SEC, MAX_UPDATE_INTERVAL_SEC);
        sectorMarketPriceMultiplier = clamp(sectorMarketPriceMultiplier, MIN_REMOTE_MARKET_PRICE_MULTIPLIER, MAX_REMOTE_MARKET_PRICE_MULTIPLIER);
        fixersMarketPriceMultiplier = clamp(fixersMarketPriceMultiplier, MIN_REMOTE_MARKET_PRICE_MULTIPLIER, MAX_REMOTE_MARKET_PRICE_MULTIPLIER);
        System.setProperty(KEY_UPDATE_INTERVAL, Float.toString(effective));
        System.setProperty(KEY_DIALOG_OPTION_ENABLED, Boolean.toString(dialogOptionEnabled));
        System.setProperty(KEY_SECTOR_MARKET_ENABLED, Boolean.toString(sectorMarketEnabled));
        System.setProperty(KEY_FIXERS_MARKET_ENABLED, Boolean.toString(fixersMarketEnabled));
        System.setProperty(KEY_FIXERS_MARKET_TAG_INFERENCE_ENABLED, Boolean.toString(fixersMarketTagInferenceEnabled));
        System.setProperty(KEY_SECTOR_MARKET_PRICE_MULTIPLIER, Float.toString(sectorMarketPriceMultiplier));
        System.setProperty(KEY_FIXERS_MARKET_PRICE_MULTIPLIER, Float.toString(fixersMarketPriceMultiplier));
        System.setProperty(KEY_DESIRED_SMALL_WEAPON_COUNT, Integer.toString(desiredSmallWeaponCount));
        System.setProperty(KEY_DESIRED_MEDIUM_WEAPON_COUNT, Integer.toString(desiredMediumWeaponCount));
        System.setProperty(KEY_DESIRED_LARGE_WEAPON_COUNT, Integer.toString(desiredLargeWeaponCount));
        System.setProperty(KEY_DESIRED_FIGHTER_WING_COUNT, Integer.toString(desiredFighterWingCount));
        System.setProperty(KEY_DEBUG_TRADE_FAILURE_STEP, debugTradeFailureStep);
        if (configLogs < MAX_CONFIG_LOGS) {
            configLogs++;
            LOG.info("WP_CONFIG updateIntervalSeconds=" + effective
                    + " dialogOptionEnabled=" + dialogOptionEnabled
                    + " sectorMarketEnabled=" + sectorMarketEnabled
                    + " fixersMarketEnabled=" + fixersMarketEnabled
                    + " fixersMarketTagInferenceEnabled=" + fixersMarketTagInferenceEnabled
                    + " sectorMarketPriceMultiplier=" + sectorMarketPriceMultiplier
                    + " fixersMarketPriceMultiplier=" + fixersMarketPriceMultiplier
                    + " desiredSmallWeaponCount=" + desiredSmallWeaponCount
                    + " desiredMediumWeaponCount=" + desiredMediumWeaponCount
                    + " desiredLargeWeaponCount=" + desiredLargeWeaponCount
                    + " desiredFighterWingCount=" + desiredFighterWingCount
                    + " debugTradeFailureStep=" + debugTradeFailureStep);
        }
        return effective;
    }

    public static boolean isSectorMarketEnabled() {
        return Boolean.parseBoolean(System.getProperty(KEY_SECTOR_MARKET_ENABLED, "true"));
    }

    public static boolean isDialogueOptionEnabled() {
        return Boolean.parseBoolean(System.getProperty(KEY_DIALOG_OPTION_ENABLED, "false"));
    }

    public static boolean isFixersMarketEnabled() {
        return Boolean.parseBoolean(System.getProperty(KEY_FIXERS_MARKET_ENABLED, "true"));
    }

    public static boolean isFixersMarketTagInferenceEnabled() {
        return Boolean.parseBoolean(System.getProperty(KEY_FIXERS_MARKET_TAG_INFERENCE_ENABLED, "false"));
    }

    public static float sectorMarketPriceMultiplier() {
        return readMultiplier(KEY_SECTOR_MARKET_PRICE_MULTIPLIER, DEFAULT_SECTOR_MARKET_PRICE_MULTIPLIER);
    }

    public static float fixersMarketPriceMultiplier() {
        return readMultiplier(KEY_FIXERS_MARKET_PRICE_MULTIPLIER, DEFAULT_FIXERS_MARKET_PRICE_MULTIPLIER);
    }

    private static float readMultiplier(String propertyKey, float defaultValue) {
        try {
            return clamp(Float.parseFloat(System.getProperty(propertyKey,
                            Float.toString(defaultValue))),
                    MIN_REMOTE_MARKET_PRICE_MULTIPLIER,
                    MAX_REMOTE_MARKET_PRICE_MULTIPLIER);
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
        Double value = readDoubleSetting(settingId);
        if (value == null) {
            return defaultValue;
        }
        return clamp(Math.round(value.floatValue()), MIN_DESIRED_WEAPON_COUNT, MAX_DESIRED_WEAPON_COUNT);
    }

    private static String readDebugTradeFailureStep() {
        String value = INITIAL_DEBUG_TRADE_FAILURE_STEP == null ? "" : INITIAL_DEBUG_TRADE_FAILURE_STEP.trim();
        if (value.length() == 0 || "none".equalsIgnoreCase(value)) {
            return "";
        }
        if ("after-source-removal".equalsIgnoreCase(value)
                || "after-player-cargo-remove".equalsIgnoreCase(value)
                || "after-player-cargo-add".equalsIgnoreCase(value)
                || "after-target-cargo-add".equalsIgnoreCase(value)
                || "after-credit-mutation".equalsIgnoreCase(value)) {
            return value.toLowerCase(java.util.Locale.US);
        }
        LOG.warn("WP_CONFIG ignored unknown debug trade failure step: " + value);
        return "";
    }

    private static Double readDoubleSetting(String settingId) {
        try {
            return LunaSettings.getDouble(MOD_ID, settingId);
        } catch (Throwable t) {
            logConfigReadError(settingId, t);
            return null;
        }
    }

    private static Boolean readBooleanSetting(String settingId) {
        try {
            return LunaSettings.getBoolean(MOD_ID, settingId);
        } catch (Throwable t) {
            logConfigReadError(settingId, t);
            return null;
        }
    }

    private static void logConfigReadError(String settingId, Throwable t) {
        if (!configErrorLogged) {
            configErrorLogged = true;
            LOG.error("WP_CONFIG luna settings read error settingId=" + settingId, t);
        }
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
