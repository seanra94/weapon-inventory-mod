package weaponsprocurement.internal

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.util.Misc
import org.apache.log4j.Logger
import java.util.HashMap

class WeaponsProcurementCountUpdater : EveryFrameScript {
    private var elapsedSinceUpdate = 0f
    private var elapsedSinceSettingsRefresh = SETTINGS_REFRESH_INTERVAL_SEC
    private var updateIntervalSec = UPDATE_INTERVAL_SEC
    private var updaterErrorLogged = false

    init {
        System.setProperty(KEY_READY, "false")
    }

    override fun isDone(): Boolean = false

    override fun runWhilePaused(): Boolean = true

    override fun advance(amount: Float) {
        elapsedSinceSettingsRefresh += amount
        if (elapsedSinceSettingsRefresh >= SETTINGS_REFRESH_INTERVAL_SEC) {
            elapsedSinceSettingsRefresh = 0f
            updateIntervalSec = WeaponsProcurementBadgeConfig.refreshAndPublishBadgeSettings()
        }
        if (!WeaponsProcurementBadgeConfig.isEnabled()) {
            System.setProperty(KEY_READY, "false")
            elapsedSinceUpdate = 0f
            return
        }
        elapsedSinceUpdate += amount
        if (elapsedSinceUpdate < updateIntervalSec) {
            return
        }
        elapsedSinceUpdate = 0f
        updateCounts()
    }

    private fun updateCounts() {
        try {
            val sector: SectorAPI = Global.getSector() ?: run {
                System.setProperty(KEY_READY, "false")
                return
            }

            val weaponIds = sector.allWeaponIds
            val fighterIds = sector.allFighterWingIds

            val fleet: CampaignFleetAPI? = sector.playerFleet
            val playerCargo = fleet?.cargo
            val markets: List<MarketAPI>? = sector.economy?.marketsCopy
            val playerWeapons = collectWeaponCounts(playerCargo)
            val playerFighters = collectFighterCounts(playerCargo)
            val storageWeapons = HashMap<String, Int>()
            val storageFighters = HashMap<String, Int>()
            collectAccessibleStorageCounts(markets, storageWeapons, storageFighters)

            if (weaponIds != null) {
                for (weaponId in weaponIds) {
                    if (weaponId.isNullOrEmpty()) {
                        continue
                    }
                    val playerCount = getCount(playerWeapons, weaponId)
                    val storageCount = getCount(storageWeapons, weaponId)
                    System.setProperty(KEY_WEAPON_PREFIX + weaponId + KEY_PLAYER_SUFFIX, playerCount.toString())
                    System.setProperty(KEY_WEAPON_PREFIX + weaponId + KEY_STORAGE_SUFFIX, storageCount.toString())
                }
            }

            if (fighterIds != null) {
                for (fighterId in fighterIds) {
                    if (fighterId.isNullOrEmpty()) {
                        continue
                    }
                    val playerCount = getCount(playerFighters, fighterId)
                    val storageCount = getCount(storageFighters, fighterId)
                    System.setProperty(KEY_FIGHTER_PREFIX + fighterId + KEY_PLAYER_SUFFIX, playerCount.toString())
                    System.setProperty(KEY_FIGHTER_PREFIX + fighterId + KEY_STORAGE_SUFFIX, storageCount.toString())
                }
            }

            System.setProperty(KEY_READY, "true")
            System.setProperty(KEY_UPDATED_AT, System.currentTimeMillis().toString())
        } catch (t: Throwable) {
            System.setProperty(KEY_READY, "false")
            if (!updaterErrorLogged) {
                updaterErrorLogged = true
                LOG.error("WP_COUNT_UPDATER error", t)
            }
        }
    }

    companion object {
        private val LOG: Logger = Logger.getLogger(WeaponsProcurementCountUpdater::class.java)

        private const val UPDATE_INTERVAL_SEC = 0.20f
        private const val SETTINGS_REFRESH_INTERVAL_SEC = 1.00f

        private const val KEY_READY = "wp.counts.ready"
        private const val KEY_UPDATED_AT = "wp.counts.updatedAt"
        private const val KEY_WEAPON_PREFIX = "wp.weapon."
        private const val KEY_FIGHTER_PREFIX = "wp.fighter."
        private const val KEY_PLAYER_SUFFIX = ".player"
        private const val KEY_STORAGE_SUFFIX = ".storage"

        private var cargoCountErrorLogged = false
        private var storageCountErrorLogged = false

        private fun collectWeaponCounts(cargo: CargoAPI?): MutableMap<String, Int> {
            val counts = HashMap<String, Int>()
            if (cargo == null) {
                return counts
            }
            try {
                val weapons = cargo.weapons ?: return counts
                for (quantity in weapons) {
                    if (quantity != null) {
                        addCount(counts, quantity.item, quantity.count)
                    }
                }
            } catch (t: Throwable) {
                logCargoCountErrorOnce("weapons", t)
            }
            return counts
        }

        private fun collectFighterCounts(cargo: CargoAPI?): MutableMap<String, Int> {
            val counts = HashMap<String, Int>()
            if (cargo == null) {
                return counts
            }
            try {
                val fighters = cargo.fighters ?: return counts
                for (quantity in fighters) {
                    if (quantity != null) {
                        addCount(counts, quantity.item, quantity.count)
                    }
                }
            } catch (t: Throwable) {
                logCargoCountErrorOnce("fighters", t)
            }
            return counts
        }

        private fun collectAccessibleStorageCounts(
            markets: List<MarketAPI>?,
            weaponCounts: MutableMap<String, Int>,
            fighterCounts: MutableMap<String, Int>,
        ) {
            if (markets == null) {
                return
            }
            for (market in markets) {
                try {
                    if (!Misc.playerHasStorageAccess(market)) {
                        continue
                    }
                    val storageCargo = Misc.getStorageCargo(market)
                    mergeCounts(weaponCounts, collectWeaponCounts(storageCargo))
                    mergeCounts(fighterCounts, collectFighterCounts(storageCargo))
                } catch (t: Throwable) {
                    logStorageCountErrorOnce(market, t)
                }
            }
        }

        private fun mergeCounts(target: MutableMap<String, Int>, source: Map<String, Int>) {
            for ((key, value) in source) {
                addCount(target, key, value)
            }
        }

        private fun addCount(counts: MutableMap<String, Int>, id: String?, count: Int) {
            if (id.isNullOrEmpty() || count == 0) {
                return
            }
            counts[id] = (counts[id] ?: 0) + count
        }

        private fun getCount(counts: Map<String, Int>, id: String): Int = counts[id] ?: 0

        private fun logCargoCountErrorOnce(kind: String, t: Throwable) {
            if (cargoCountErrorLogged) {
                return
            }
            cargoCountErrorLogged = true
            LOG.warn("WP_COUNT_UPDATER cargo count failed for $kind; continuing with partial counts", t)
        }

        private fun logStorageCountErrorOnce(market: MarketAPI?, t: Throwable) {
            if (storageCountErrorLogged) {
                return
            }
            storageCountErrorLogged = true
            LOG.warn("WP_COUNT_UPDATER storage count failed for market=${marketLabel(market)}; continuing with partial counts", t)
        }

        private fun marketLabel(market: MarketAPI?): String {
            if (market == null) {
                return "null"
            }
            return try {
                market.id + "/" + market.name
            } catch (_: Throwable) {
                market.toString()
            }
        }
    }
}
