package weaponsprocurement.config

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.loading.FighterWingSpecAPI
import com.fs.starfarer.api.loading.WeaponSpecAPI
import org.apache.log4j.Logger
import org.json.JSONObject
import weaponsprocurement.core.StockItemType
import java.util.Collections
import java.util.HashSet
import java.util.Locale

class WeaponMarketBlacklist private constructor(
    sector: Set<String>,
    fixers: Set<String>,
) {
    private val sector: Set<String> = Collections.unmodifiableSet(HashSet(sector))
    private val fixers: Set<String> = Collections.unmodifiableSet(HashSet(fixers))

    fun isBannedFromSector(itemKey: String?): Boolean = contains(sector, itemKey)

    fun isBannedFromFixers(itemKey: String?): Boolean = contains(fixers, itemKey)

    fun cacheKey(): String = "sector=${sector.hashCode()}|fixers=${fixers.hashCode()}"

    companion object {
        private val LOG: Logger = Logger.getLogger(WeaponMarketBlacklist::class.java)
        private const val CONFIG_PATH = "data/config/weapons_procurement_market_blacklist.json"
        private const val SECTOR_KEY = "BANNED_FROM_SECTOR_MARKET"
        private const val FIXERS_KEY = "BANNED_FROM_FIXERS_MARKET"

        private var cached: WeaponMarketBlacklist? = null
        private var errorLogged = false

        @JvmStatic
        fun load(): WeaponMarketBlacklist {
            val current = cached
            if (current != null) return current
            return try {
                val root = Global.getSettings().loadJSON(CONFIG_PATH)
                val loaded = WeaponMarketBlacklist(readSet(root, SECTOR_KEY), readSet(root, FIXERS_KEY))
                cached = loaded
                loaded
            } catch (t: Throwable) {
                if (!errorLogged) {
                    errorLogged = true
                    LOG.warn("WP_MARKET_BLACKLIST load failed; using empty blacklist from $CONFIG_PATH", t)
                }
                val empty = WeaponMarketBlacklist(emptySet(), emptySet())
                cached = empty
                empty
            }
        }

        private fun readSet(root: JSONObject?, key: String): Set<String> {
            if (root == null) return emptySet()
            val array = root.optJSONArray(key) ?: return emptySet()
            val result = HashSet<String>()
            var index = 0
            while (index < array.length()) {
                val value = normalize(array.optString(index, null))
                if (value != null) result.add(value)
                index++
            }
            return result
        }

        private fun contains(set: Set<String>, itemKey: String?): Boolean {
            val rawId = StockItemType.rawId(itemKey)
            if (set.contains(normalize(itemKey)) || set.contains(normalize(rawId))) {
                return true
            }
            val displayName = displayName(itemKey, rawId)
            return displayName != null && set.contains(normalize(displayName))
        }

        private fun displayName(itemKey: String?, rawId: String?): String? {
            val itemType = StockItemType.fromKey(itemKey)
            if (StockItemType.WING == itemType) {
                val spec = safeWingSpec(rawId)
                return spec?.wingName
            }
            val spec = safeWeaponSpec(rawId)
            return spec?.weaponName
        }

        private fun safeWeaponSpec(weaponId: String?): WeaponSpecAPI? {
            return try {
                Global.getSettings().getWeaponSpec(weaponId)
            } catch (ignored: Throwable) {
                null
            }
        }

        private fun safeWingSpec(wingId: String?): FighterWingSpecAPI? {
            return try {
                Global.getSettings().getFighterWingSpec(wingId)
            } catch (ignored: Throwable) {
                null
            }
        }

        private fun normalize(value: String?): String? {
            if (value == null) return null
            val trimmed = value.trim()
            return if (trimmed.isEmpty()) null else trimmed.lowercase(Locale.US)
        }
    }
}
