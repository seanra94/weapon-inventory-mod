package weaponsprocurement.config

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.WeaponAPI
import org.apache.log4j.Logger
import org.json.JSONObject
import weaponsprocurement.stock.OwnedSourcePolicy
import weaponsprocurement.stock.StockItemType
import weaponsprocurement.stock.StockSortMode
import java.util.Collections
import java.util.HashMap

class StockReviewConfig private constructor(
    private val smallWeaponDesired: Int,
    private val mediumWeaponDesired: Int,
    private val largeWeaponDesired: Int,
    private val fighterWingDesired: Int,
    private val includeCurrentMarketStorage: Boolean,
    private val includeBlackMarket: Boolean,
    private val sortMode: StockSortMode,
    desiredOverrides: Map<String, Int>,
    ignoredItems: Map<String, Boolean>,
) {
    private val desiredOverrides: Map<String, Int> = Collections.unmodifiableMap(HashMap(desiredOverrides))
    private val ignoredItems: Map<String, Boolean> = Collections.unmodifiableMap(HashMap(ignoredItems))

    fun desiredCount(weaponId: String?, size: WeaponAPI.WeaponSize?): Int {
        var override = desiredOverrides[StockItemType.WEAPON.key(weaponId)]
        if (override == null) override = desiredOverrides[weaponId]
        if (override != null) return override

        if (WeaponAPI.WeaponSize.SMALL == size) return smallWeaponDesired
        if (WeaponAPI.WeaponSize.MEDIUM == size) return mediumWeaponDesired
        if (WeaponAPI.WeaponSize.LARGE == size) return largeWeaponDesired
        return mediumWeaponDesired
    }

    fun isIgnored(itemKeyOrId: String?): Boolean {
        val ignored = ignoredItems[itemKeyOrId]
        return ignored == true
    }

    fun desiredFighterWingCount(wingId: String?): Int {
        var override = desiredOverrides[StockItemType.WING.key(wingId)]
        if (override == null) override = desiredOverrides[wingId]
        return override ?: fighterWingDesired
    }

    fun isIncludeCurrentMarketStorage(): Boolean = includeCurrentMarketStorage

    fun isIncludeBlackMarket(): Boolean = includeBlackMarket

    fun getSortMode(): StockSortMode = sortMode

    fun ownedSourcePolicy(includeStorage: Boolean): OwnedSourcePolicy {
        return if (includeStorage) {
            OwnedSourcePolicy.FLEET_AND_ACCESSIBLE_STORAGE
        } else {
            OwnedSourcePolicy.FLEET_ONLY
        }
    }

    companion object {
        private val LOG: Logger = Logger.getLogger(StockReviewConfig::class.java)
        private const val CONFIG_PATH = "data/config/weapons_procurement_stock.json"

        private const val DEFAULT_SMALL_WEAPON_COUNT = 16
        private const val DEFAULT_MEDIUM_WEAPON_COUNT = 8
        private const val DEFAULT_LARGE_WEAPON_COUNT = 4
        private const val DEFAULT_FIGHTER_WING_COUNT = 4

        @JvmStatic
        fun load(): StockReviewConfig {
            return try {
                val json = Global.getSettings().loadJSON(CONFIG_PATH)
                fromJson(json)
            } catch (t: Throwable) {
                LOG.warn("WP_STOCK_REVIEW config load failed; using defaults from $CONFIG_PATH", t)
                defaults()
            }
        }

        @JvmStatic
        fun defaults(): StockReviewConfig {
            return StockReviewConfig(
                WeaponsProcurementConfig.desiredSmallWeaponCount(DEFAULT_SMALL_WEAPON_COUNT),
                WeaponsProcurementConfig.desiredMediumWeaponCount(DEFAULT_MEDIUM_WEAPON_COUNT),
                WeaponsProcurementConfig.desiredLargeWeaponCount(DEFAULT_LARGE_WEAPON_COUNT),
                WeaponsProcurementConfig.desiredFighterWingCount(DEFAULT_FIGHTER_WING_COUNT),
                true,
                true,
                StockSortMode.NEED,
                Collections.emptyMap(),
                Collections.emptyMap(),
            )
        }

        private fun fromJson(json: JSONObject): StockReviewConfig {
            val desiredDefaults = json.optJSONObject("desiredDefaults")
            val small = WeaponsProcurementConfig.desiredSmallWeaponCount(
                clampDesired(optInt(desiredDefaults, "smallWeapon", DEFAULT_SMALL_WEAPON_COUNT))
            )
            val medium = WeaponsProcurementConfig.desiredMediumWeaponCount(
                clampDesired(optInt(desiredDefaults, "mediumWeapon", DEFAULT_MEDIUM_WEAPON_COUNT))
            )
            val large = WeaponsProcurementConfig.desiredLargeWeaponCount(
                clampDesired(optInt(desiredDefaults, "largeWeapon", DEFAULT_LARGE_WEAPON_COUNT))
            )
            val fighterWing = WeaponsProcurementConfig.desiredFighterWingCount(
                clampDesired(optInt(desiredDefaults, "fighterWing", DEFAULT_FIGHTER_WING_COUNT))
            )

            val sources = json.optJSONObject("sources")
            val includeStorage = optBoolean(sources, "includeCurrentMarketStorage", true)
            val includeBlackMarket = optBoolean(sources, "includeBlackMarket", true)

            val display = json.optJSONObject("display")
            val sortMode = StockSortMode.fromConfig(optString(display, "defaultSort", "NEED"))

            val overrides = HashMap<String, Int>()
            val ignored = HashMap<String, Boolean>()
            readPerItemOverrides(json.optJSONObject("perWeapon"), overrides, ignored, medium)
            readPerItemOverrides(json.optJSONObject("perItem"), overrides, ignored, medium)

            return StockReviewConfig(
                small,
                medium,
                large,
                fighterWing,
                includeStorage,
                includeBlackMarket,
                sortMode,
                overrides,
                ignored,
            )
        }

        private fun optInt(json: JSONObject?, key: String, defaultValue: Int): Int {
            return json?.optInt(key, defaultValue) ?: defaultValue
        }

        private fun optBoolean(json: JSONObject?, key: String, defaultValue: Boolean): Boolean {
            return json?.optBoolean(key, defaultValue) ?: defaultValue
        }

        private fun optString(json: JSONObject?, key: String, defaultValue: String): String {
            return json?.optString(key, defaultValue) ?: defaultValue
        }

        private fun readPerItemOverrides(
            json: JSONObject?,
            desired: MutableMap<String, Int>,
            ignored: MutableMap<String, Boolean>,
            defaultDesired: Int,
        ) {
            if (json == null) return
            val names = JSONObject.getNames(json) ?: return
            for (itemKey in names) {
                val itemConfig = json.optJSONObject(itemKey) ?: continue
                if (itemConfig.has("desired")) {
                    desired[itemKey] = clampDesired(itemConfig.optInt("desired", defaultDesired))
                }
                if (itemConfig.has("ignored")) {
                    ignored[itemKey] = itemConfig.optBoolean("ignored", false)
                }
            }
        }

        private fun clampDesired(value: Int): Int {
            if (value < 0) return 0
            if (value > 999) return 999
            return value
        }
    }
}
