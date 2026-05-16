package weaponsprocurement.internal

import lunalib.lunaSettings.LunaSettings
import org.apache.log4j.Logger

class WeaponsProcurementBadgeConfig private constructor() {
    companion object {
        private val LOG: Logger = Logger.getLogger(WeaponsProcurementBadgeConfig::class.java)

        private const val MOD_ID = "weapons_procurement"
        private const val SETTING_UPDATE_INTERVAL = "wp_update_interval_seconds"
        private const val SETTING_ENABLE_PATCHED_BADGES = "wp_enable_patched_badges"
        private const val KEY_UPDATE_INTERVAL = "wp.config.updateIntervalSeconds"
        private const val KEY_PATCHED_BADGES_ENABLED = "wp.config.patchedBadgesEnabled"
        private const val KEY_PATCHED_BADGES_OVERRIDE = "wp.private.patchedBadgesEnabled"
        private const val DEFAULT_UPDATE_INTERVAL_SEC = 0.20f
        private const val MIN_UPDATE_INTERVAL_SEC = 0.05f
        private const val MAX_UPDATE_INTERVAL_SEC = 2.00f

        private var configErrorLogged = false

        @JvmStatic
        fun refreshAndPublishBadgeSettings(): Float {
            var effective = DEFAULT_UPDATE_INTERVAL_SEC
            val override = System.getProperty(KEY_PATCHED_BADGES_OVERRIDE)
            val overrideSet = !override.isNullOrBlank()
            var enabled = if (overrideSet) java.lang.Boolean.parseBoolean(override) else true
            try {
                val value = LunaSettings.getDouble(MOD_ID, SETTING_UPDATE_INTERVAL)
                if (value != null) {
                    effective = value.toFloat()
                }
                val badgesEnabled = if (overrideSet) null else LunaSettings.getBoolean(MOD_ID, SETTING_ENABLE_PATCHED_BADGES)
                if (!overrideSet && badgesEnabled != null) {
                    enabled = badgesEnabled
                }
            } catch (t: Throwable) {
                if (!configErrorLogged) {
                    configErrorLogged = true
                    LOG.error("WP_BADGE_CONFIG luna settings read error", t)
                }
            }
            effective = clamp(effective, MIN_UPDATE_INTERVAL_SEC, MAX_UPDATE_INTERVAL_SEC)
            System.setProperty(KEY_UPDATE_INTERVAL, effective.toString())
            System.setProperty(KEY_PATCHED_BADGES_ENABLED, enabled.toString())
            return effective
        }

        @JvmStatic
        fun isEnabled(): Boolean = java.lang.Boolean.parseBoolean(
            System.getProperty(KEY_PATCHED_BADGES_ENABLED, "false"),
        )

        private fun clamp(value: Float, min: Float, max: Float): Float = Math.max(min, Math.min(max, value))
    }
}
