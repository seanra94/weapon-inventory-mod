package weaponsprocurement.internal

import org.apache.log4j.Logger

class WeaponsProcurementBadgeHelper private constructor() {
    companion object {
        private val LOG: Logger = Logger.getLogger(WeaponsProcurementBadgeHelper::class.java)

        private const val TOTAL_ERR = "graphics/ui/wp_total_err.png"
        private const val TOTAL_99PLUS = "graphics/ui/wp_total_green_99plus.png"
        private const val TOTAL_RED_PREFIX = "graphics/ui/wp_total_red_"
        private const val TOTAL_YELLOW_PREFIX = "graphics/ui/wp_total_yellow_"
        private const val TOTAL_GREEN_PREFIX = "graphics/ui/wp_total_green_"
        private const val TOTAL_SUFFIX = ".png"

        private const val KEY_READY = "wp.counts.ready"
        private const val KEY_PATCHED_BADGES_ENABLED = "wp.config.patchedBadgesEnabled"
        private const val KEY_WEAPON_PREFIX = "wp.weapon."
        private const val KEY_FIGHTER_PREFIX = "wp.fighter."
        private const val KEY_PLAYER_SUFFIX = ".player"
        private const val KEY_STORAGE_SUFFIX = ".storage"

        private var parseErrorLogged = false

        @JvmStatic
        fun getTotalStatusSpritePath(weaponId: String?): String? = getTotalStatusSpritePath("weapon", weaponId)

        @JvmStatic
        fun getTotalStatusSpritePath(kind: String?, id: String?): String? {
            if (!isPatchedBadgesEnabled()) {
                return null
            }

            if (!isReady() || id.isNullOrEmpty()) {
                return TOTAL_ERR
            }

            val playerCount = readCount(kind, id, true)
            val storageCount = readCount(kind, id, false)
            return if (playerCount != null && storageCount != null) {
                val total = playerCount + storageCount
                when {
                    total >= 99 -> TOTAL_99PLUS
                    total >= 0 -> toTotalSprite(total)
                    else -> TOTAL_ERR
                }
            } else {
                TOTAL_ERR
            }
        }

        private fun isReady(): Boolean = "true".equals(System.getProperty(KEY_READY), ignoreCase = true)

        private fun isPatchedBadgesEnabled(): Boolean =
            "true".equals(System.getProperty(KEY_PATCHED_BADGES_ENABLED), ignoreCase = true)

        private fun readCount(kind: String?, id: String, player: Boolean): Int? {
            val prefix = getPrefix(kind) ?: return null
            val key = prefix + id + if (player) KEY_PLAYER_SUFFIX else KEY_STORAGE_SUFFIX
            val raw = System.getProperty(key)
            if (raw.isNullOrEmpty()) {
                return null
            }
            return try {
                raw.toInt()
            } catch (t: NumberFormatException) {
                logParseErrorOnce(key, raw, t)
                null
            }
        }

        private fun getPrefix(kind: String?): String? = when (kind) {
            "weapon" -> KEY_WEAPON_PREFIX
            "fighter" -> KEY_FIGHTER_PREFIX
            else -> null
        }

        private fun toTotalSprite(total: Int): String = when {
            total == 0 -> TOTAL_RED_PREFIX + "0" + TOTAL_SUFFIX
            total in 1..9 -> TOTAL_YELLOW_PREFIX + total + TOTAL_SUFFIX
            else -> TOTAL_GREEN_PREFIX + total + TOTAL_SUFFIX
        }

        private fun logParseErrorOnce(key: String, value: String, t: NumberFormatException) {
            if (parseErrorLogged) {
                return
            }
            parseErrorLogged = true
            LOG.error("WP_BADGE_HELPER parse error key=$key value=$value", t)
        }
    }
}
