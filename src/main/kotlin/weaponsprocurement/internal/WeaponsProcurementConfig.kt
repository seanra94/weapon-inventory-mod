package weaponsprocurement.internal

import lunalib.lunaSettings.LunaSettings
import org.apache.log4j.Logger
import java.util.Locale

object WeaponsProcurementConfig {
    private val LOG: Logger = Logger.getLogger(WeaponsProcurementConfig::class.java)

    private const val MOD_ID = "weapons_procurement"
    private const val SETTING_UPDATE_INTERVAL = "wp_update_interval_seconds"
    private const val SETTING_ENABLE_DIALOG_OPTION = "wp_enable_dialog_option"
    private const val SETTING_ENABLE_SECTOR_MARKET = "wp_enable_sector_market"
    private const val SETTING_ENABLE_FIXERS_MARKET = "wp_enable_fixers_market"
    private const val SETTING_ENABLE_FIXERS_MARKET_TAG_INFERENCE = "wp_enable_fixers_market_tag_inference"
    private const val SETTING_SECTOR_MARKET_PRICE_MULTIPLIER = "wp_sector_market_price_multiplier"
    private const val SETTING_FIXERS_MARKET_PRICE_MULTIPLIER = "wp_fixers_market_price_multiplier"
    private const val SETTING_DESIRED_SMALL_WEAPON_COUNT = "wp_desired_small_weapon_count"
    private const val SETTING_DESIRED_MEDIUM_WEAPON_COUNT = "wp_desired_medium_weapon_count"
    private const val SETTING_DESIRED_LARGE_WEAPON_COUNT = "wp_desired_large_weapon_count"
    private const val SETTING_DESIRED_FIGHTER_WING_COUNT = "wp_desired_fighter_wing_count"
    private const val KEY_UPDATE_INTERVAL = "wp.config.updateIntervalSeconds"
    private const val KEY_DIALOG_OPTION_ENABLED = "wp.config.dialogOptionEnabled"
    private const val KEY_SECTOR_MARKET_ENABLED = "wp.config.sectorMarketEnabled"
    private const val KEY_FIXERS_MARKET_ENABLED = "wp.config.fixersMarketEnabled"
    private const val KEY_FIXERS_MARKET_TAG_INFERENCE_ENABLED = "wp.config.fixersMarketTagInferenceEnabled"
    private const val KEY_SECTOR_MARKET_PRICE_MULTIPLIER = "wp.config.sectorMarketPriceMultiplier"
    private const val KEY_FIXERS_MARKET_PRICE_MULTIPLIER = "wp.config.fixersMarketPriceMultiplier"
    private const val KEY_DESIRED_SMALL_WEAPON_COUNT = "wp.config.desiredSmallWeaponCount"
    private const val KEY_DESIRED_MEDIUM_WEAPON_COUNT = "wp.config.desiredMediumWeaponCount"
    private const val KEY_DESIRED_LARGE_WEAPON_COUNT = "wp.config.desiredLargeWeaponCount"
    private const val KEY_DESIRED_FIGHTER_WING_COUNT = "wp.config.desiredFighterWingCount"
    const val KEY_DEBUG_TRADE_FAILURE_STEP: String = "wp.debug.failTradeStep"

    private const val DEFAULT_UPDATE_INTERVAL_SEC = 0.20f
    private const val MIN_UPDATE_INTERVAL_SEC = 0.05f
    private const val MAX_UPDATE_INTERVAL_SEC = 2.00f
    private const val DEFAULT_SECTOR_MARKET_PRICE_MULTIPLIER = 3.00f
    private const val DEFAULT_FIXERS_MARKET_PRICE_MULTIPLIER = 5.00f
    private const val MIN_REMOTE_MARKET_PRICE_MULTIPLIER = 1.00f
    private const val MAX_REMOTE_MARKET_PRICE_MULTIPLIER = 20.00f
    private const val DEFAULT_DESIRED_SMALL_WEAPON_COUNT = 16
    private const val DEFAULT_DESIRED_MEDIUM_WEAPON_COUNT = 8
    private const val DEFAULT_DESIRED_LARGE_WEAPON_COUNT = 4
    private const val DEFAULT_DESIRED_FIGHTER_WING_COUNT = 4
    private const val MIN_DESIRED_WEAPON_COUNT = 0
    private const val MAX_DESIRED_WEAPON_COUNT = 999
    private const val MAX_CONFIG_LOGS = 10
    private val INITIAL_DEBUG_TRADE_FAILURE_STEP = System.getProperty(KEY_DEBUG_TRADE_FAILURE_STEP, "")

    private var configLogs = 0
    private var configErrorLogged = false

    @JvmStatic
    fun refreshAndPublishUpdateIntervalSeconds(): Float = refreshAndPublishSettings()

    @JvmStatic
    fun refreshAndPublishSettings(): Float {
        var effective = DEFAULT_UPDATE_INTERVAL_SEC
        var dialogOptionEnabled = false
        var sectorMarketEnabled = true
        var fixersMarketEnabled = true
        var fixersMarketTagInferenceEnabled = false
        var sectorMarketPriceMultiplier = DEFAULT_SECTOR_MARKET_PRICE_MULTIPLIER
        var fixersMarketPriceMultiplier = DEFAULT_FIXERS_MARKET_PRICE_MULTIPLIER
        val desiredSmallWeaponCount: Int
        val desiredMediumWeaponCount: Int
        val desiredLargeWeaponCount: Int
        val desiredFighterWingCount: Int

        val updateInterval = readDoubleSetting(SETTING_UPDATE_INTERVAL)
        if (updateInterval != null) {
            effective = updateInterval.toFloat()
        }
        val dialogOption = readBooleanSetting(SETTING_ENABLE_DIALOG_OPTION)
        if (dialogOption != null) {
            dialogOptionEnabled = dialogOption
        }
        val sectorEnabled = readBooleanSetting(SETTING_ENABLE_SECTOR_MARKET)
        if (sectorEnabled != null) {
            sectorMarketEnabled = sectorEnabled
        }
        val fixersEnabled = readBooleanSetting(SETTING_ENABLE_FIXERS_MARKET)
        if (fixersEnabled != null) {
            fixersMarketEnabled = fixersEnabled
        }
        val inferenceEnabled = readBooleanSetting(SETTING_ENABLE_FIXERS_MARKET_TAG_INFERENCE)
        if (inferenceEnabled != null) {
            fixersMarketTagInferenceEnabled = inferenceEnabled
        }
        val sectorMultiplier = readDoubleSetting(SETTING_SECTOR_MARKET_PRICE_MULTIPLIER)
        if (sectorMultiplier != null) {
            sectorMarketPriceMultiplier = sectorMultiplier.toFloat()
        }
        val fixersMultiplier = readDoubleSetting(SETTING_FIXERS_MARKET_PRICE_MULTIPLIER)
        if (fixersMultiplier != null) {
            fixersMarketPriceMultiplier = fixersMultiplier.toFloat()
        }
        desiredSmallWeaponCount = readDesiredWeaponCount(SETTING_DESIRED_SMALL_WEAPON_COUNT, DEFAULT_DESIRED_SMALL_WEAPON_COUNT)
        desiredMediumWeaponCount = readDesiredWeaponCount(SETTING_DESIRED_MEDIUM_WEAPON_COUNT, DEFAULT_DESIRED_MEDIUM_WEAPON_COUNT)
        desiredLargeWeaponCount = readDesiredWeaponCount(SETTING_DESIRED_LARGE_WEAPON_COUNT, DEFAULT_DESIRED_LARGE_WEAPON_COUNT)
        desiredFighterWingCount = readDesiredWeaponCount(SETTING_DESIRED_FIGHTER_WING_COUNT, DEFAULT_DESIRED_FIGHTER_WING_COUNT)
        val debugTradeFailureStep = readDebugTradeFailureStep()

        effective = clamp(effective, MIN_UPDATE_INTERVAL_SEC, MAX_UPDATE_INTERVAL_SEC)
        sectorMarketPriceMultiplier = clamp(sectorMarketPriceMultiplier, MIN_REMOTE_MARKET_PRICE_MULTIPLIER, MAX_REMOTE_MARKET_PRICE_MULTIPLIER)
        fixersMarketPriceMultiplier = clamp(fixersMarketPriceMultiplier, MIN_REMOTE_MARKET_PRICE_MULTIPLIER, MAX_REMOTE_MARKET_PRICE_MULTIPLIER)
        System.setProperty(KEY_UPDATE_INTERVAL, effective.toString())
        System.setProperty(KEY_DIALOG_OPTION_ENABLED, dialogOptionEnabled.toString())
        System.setProperty(KEY_SECTOR_MARKET_ENABLED, sectorMarketEnabled.toString())
        System.setProperty(KEY_FIXERS_MARKET_ENABLED, fixersMarketEnabled.toString())
        System.setProperty(KEY_FIXERS_MARKET_TAG_INFERENCE_ENABLED, fixersMarketTagInferenceEnabled.toString())
        System.setProperty(KEY_SECTOR_MARKET_PRICE_MULTIPLIER, sectorMarketPriceMultiplier.toString())
        System.setProperty(KEY_FIXERS_MARKET_PRICE_MULTIPLIER, fixersMarketPriceMultiplier.toString())
        System.setProperty(KEY_DESIRED_SMALL_WEAPON_COUNT, desiredSmallWeaponCount.toString())
        System.setProperty(KEY_DESIRED_MEDIUM_WEAPON_COUNT, desiredMediumWeaponCount.toString())
        System.setProperty(KEY_DESIRED_LARGE_WEAPON_COUNT, desiredLargeWeaponCount.toString())
        System.setProperty(KEY_DESIRED_FIGHTER_WING_COUNT, desiredFighterWingCount.toString())
        System.setProperty(KEY_DEBUG_TRADE_FAILURE_STEP, debugTradeFailureStep)
        if (configLogs < MAX_CONFIG_LOGS) {
            configLogs++
            LOG.info(
                "WP_CONFIG updateIntervalSeconds=$effective" +
                    " dialogOptionEnabled=$dialogOptionEnabled" +
                    " sectorMarketEnabled=$sectorMarketEnabled" +
                    " fixersMarketEnabled=$fixersMarketEnabled" +
                    " fixersMarketTagInferenceEnabled=$fixersMarketTagInferenceEnabled" +
                    " sectorMarketPriceMultiplier=$sectorMarketPriceMultiplier" +
                    " fixersMarketPriceMultiplier=$fixersMarketPriceMultiplier" +
                    " desiredSmallWeaponCount=$desiredSmallWeaponCount" +
                    " desiredMediumWeaponCount=$desiredMediumWeaponCount" +
                    " desiredLargeWeaponCount=$desiredLargeWeaponCount" +
                    " desiredFighterWingCount=$desiredFighterWingCount" +
                    " debugTradeFailureStep=$debugTradeFailureStep",
            )
        }
        return effective
    }

    @JvmStatic
    fun isSectorMarketEnabled(): Boolean = System.getProperty(KEY_SECTOR_MARKET_ENABLED, "true").toBoolean()

    @JvmStatic
    fun isDialogueOptionEnabled(): Boolean = System.getProperty(KEY_DIALOG_OPTION_ENABLED, "false").toBoolean()

    @JvmStatic
    fun isFixersMarketEnabled(): Boolean = System.getProperty(KEY_FIXERS_MARKET_ENABLED, "true").toBoolean()

    @JvmStatic
    fun isFixersMarketTagInferenceEnabled(): Boolean =
        System.getProperty(KEY_FIXERS_MARKET_TAG_INFERENCE_ENABLED, "false").toBoolean()

    @JvmStatic
    fun sectorMarketPriceMultiplier(): Float =
        readMultiplier(KEY_SECTOR_MARKET_PRICE_MULTIPLIER, DEFAULT_SECTOR_MARKET_PRICE_MULTIPLIER)

    @JvmStatic
    fun fixersMarketPriceMultiplier(): Float =
        readMultiplier(KEY_FIXERS_MARKET_PRICE_MULTIPLIER, DEFAULT_FIXERS_MARKET_PRICE_MULTIPLIER)

    private fun readMultiplier(propertyKey: String, defaultValue: Float): Float {
        return try {
            clamp(
                System.getProperty(propertyKey, defaultValue.toString()).toFloat(),
                MIN_REMOTE_MARKET_PRICE_MULTIPLIER,
                MAX_REMOTE_MARKET_PRICE_MULTIPLIER,
            )
        } catch (_: Throwable) {
            defaultValue
        }
    }

    @JvmStatic
    fun desiredSmallWeaponCount(fallback: Int): Int = readPublishedDesiredWeaponCount(KEY_DESIRED_SMALL_WEAPON_COUNT, fallback)

    @JvmStatic
    fun desiredMediumWeaponCount(fallback: Int): Int = readPublishedDesiredWeaponCount(KEY_DESIRED_MEDIUM_WEAPON_COUNT, fallback)

    @JvmStatic
    fun desiredLargeWeaponCount(fallback: Int): Int = readPublishedDesiredWeaponCount(KEY_DESIRED_LARGE_WEAPON_COUNT, fallback)

    @JvmStatic
    fun desiredFighterWingCount(fallback: Int): Int = readPublishedDesiredWeaponCount(KEY_DESIRED_FIGHTER_WING_COUNT, fallback)

    private fun readDesiredWeaponCount(settingId: String, defaultValue: Int): Int {
        val value = readDoubleSetting(settingId) ?: return defaultValue
        return clamp(Math.round(value.toFloat()), MIN_DESIRED_WEAPON_COUNT, MAX_DESIRED_WEAPON_COUNT)
    }

    private fun readDebugTradeFailureStep(): String {
        val value = INITIAL_DEBUG_TRADE_FAILURE_STEP?.trim() ?: ""
        if (value.isEmpty() || value.equals("none", ignoreCase = true)) {
            return ""
        }
        if (value.equals("after-source-removal", ignoreCase = true) ||
            value.equals("after-player-cargo-remove", ignoreCase = true) ||
            value.equals("after-player-cargo-add", ignoreCase = true) ||
            value.equals("after-target-cargo-add", ignoreCase = true) ||
            value.equals("after-credit-mutation", ignoreCase = true)
        ) {
            return value.lowercase(Locale.US)
        }
        LOG.warn("WP_CONFIG ignored unknown debug trade failure step: $value")
        return ""
    }

    private fun readDoubleSetting(settingId: String): Double? {
        return try {
            LunaSettings.getDouble(MOD_ID, settingId)
        } catch (t: Throwable) {
            logConfigReadError(settingId, t)
            null
        }
    }

    private fun readBooleanSetting(settingId: String): Boolean? {
        return try {
            LunaSettings.getBoolean(MOD_ID, settingId)
        } catch (t: Throwable) {
            logConfigReadError(settingId, t)
            null
        }
    }

    private fun logConfigReadError(settingId: String, t: Throwable) {
        if (!configErrorLogged) {
            configErrorLogged = true
            LOG.error("WP_CONFIG luna settings read error settingId=$settingId", t)
        }
    }

    private fun readPublishedDesiredWeaponCount(propertyKey: String, fallback: Int): Int {
        return try {
            clamp(
                System.getProperty(propertyKey, fallback.toString()).toInt(),
                MIN_DESIRED_WEAPON_COUNT,
                MAX_DESIRED_WEAPON_COUNT,
            )
        } catch (_: Throwable) {
            clamp(fallback, MIN_DESIRED_WEAPON_COUNT, MAX_DESIRED_WEAPON_COUNT)
        }
    }

    private fun clamp(value: Float, min: Float, max: Float): Float = maxOf(min, minOf(max, value))

    private fun clamp(value: Int, min: Int, max: Int): Int = maxOf(min, minOf(max, value))
}
