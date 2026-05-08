package weaponsprocurement.internal;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Misc;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeaponsProcurementCountUpdater implements EveryFrameScript {
    private static final Logger LOG = Logger.getLogger(WeaponsProcurementCountUpdater.class);

    private static final float UPDATE_INTERVAL_SEC = 0.20f;
    private static final int MAX_UPDATE_LOGS = 30;
    private static final int MAX_PRICKLER_LOGS = 30;
    private static final int MAX_FIGHTER_LOGS = 30;
    private static final int MAX_FIGHTER_SUMMARY_LOGS = 30;

    private static final String KEY_READY = "wp.counts.ready";
    private static final String KEY_UPDATED_AT = "wp.counts.updatedAt";
    private static final String KEY_WEAPON_PREFIX = "wp.weapon.";
    private static final String KEY_FIGHTER_PREFIX = "wp.fighter.";
    private static final String KEY_PLAYER_SUFFIX = ".player";
    private static final String KEY_STORAGE_SUFFIX = ".storage";
    private static final String PRICKLER_ID = "hhe_prickler";

    private float elapsedSinceUpdate = 0f;
    private float updateIntervalSec = UPDATE_INTERVAL_SEC;
    private int updateLogs = 0;
    private int pricklerLogs = 0;
    private int fighterLogs = 0;
    private int fighterSummaryLogs = 0;
    private boolean updaterErrorLogged = false;

    public WeaponsProcurementCountUpdater() {
        System.setProperty(KEY_READY, "false");
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public void advance(float amount) {
        updateIntervalSec = WeaponsProcurementConfig.refreshAndPublishSettings();
        elapsedSinceUpdate += amount;
        if (elapsedSinceUpdate < updateIntervalSec) {
            return;
        }
        elapsedSinceUpdate = 0f;
        updateCounts();
    }

    private void updateCounts() {
        long start = System.currentTimeMillis();
        try {
            SectorAPI sector = Global.getSector();
            if (sector == null) {
                System.setProperty(KEY_READY, "false");
                return;
            }

            List<String> weaponIds = sector.getAllWeaponIds();
            List<String> fighterIds = sector.getAllFighterWingIds();

            CampaignFleetAPI fleet = sector.getPlayerFleet();
            CargoAPI playerCargo = fleet == null ? null : fleet.getCargo();
            EconomyAPI economy = sector.getEconomy();
            List<MarketAPI> markets = economy == null ? null : economy.getMarketsCopy();
            Map<String, Integer> playerWeapons = collectWeaponCounts(playerCargo);
            Map<String, Integer> playerFighters = collectFighterCounts(playerCargo);
            Map<String, Integer> storageWeapons = new HashMap<String, Integer>();
            Map<String, Integer> storageFighters = new HashMap<String, Integer>();
            collectAccessibleStorageCounts(markets, storageWeapons, storageFighters);

            int pricklerPlayer = -1;
            int pricklerStorage = -1;
            int fighterWingCount = 0;
            int fighterNonzeroPlayer = 0;
            int fighterNonzeroStorage = 0;
            int totalKeysUpdated = 0;

            if (weaponIds != null) {
                for (String weaponId : weaponIds) {
                    if (weaponId == null || weaponId.isEmpty()) {
                        continue;
                    }
                    int playerCount = getCount(playerWeapons, weaponId);
                    int storageCount = getCount(storageWeapons, weaponId);
                    System.setProperty(KEY_WEAPON_PREFIX + weaponId + KEY_PLAYER_SUFFIX, Integer.toString(playerCount));
                    System.setProperty(KEY_WEAPON_PREFIX + weaponId + KEY_STORAGE_SUFFIX, Integer.toString(storageCount));
                    totalKeysUpdated++;

                    if (PRICKLER_ID.equals(weaponId)) {
                        pricklerPlayer = playerCount;
                        pricklerStorage = storageCount;
                    }
                }
            }

            if (fighterIds != null) {
                for (String fighterId : fighterIds) {
                    if (fighterId == null || fighterId.isEmpty()) {
                        continue;
                    }
                    fighterWingCount++;
                    int playerCount = getCount(playerFighters, fighterId);
                    int storageCount = getCount(storageFighters, fighterId);
                    System.setProperty(KEY_FIGHTER_PREFIX + fighterId + KEY_PLAYER_SUFFIX, Integer.toString(playerCount));
                    System.setProperty(KEY_FIGHTER_PREFIX + fighterId + KEY_STORAGE_SUFFIX, Integer.toString(storageCount));
                    totalKeysUpdated++;

                    if (playerCount > 0) {
                        fighterNonzeroPlayer++;
                    }
                    if (storageCount > 0) {
                        fighterNonzeroStorage++;
                    }

                    if ((playerCount > 0 || storageCount > 0) && fighterLogs < MAX_FIGHTER_LOGS) {
                        fighterLogs++;
                        LOG.info("WP_COUNT_UPDATER fighter wingId=" + fighterId + " player=" + playerCount + " storage=" + storageCount);
                    }
                }
            }

            System.setProperty(KEY_READY, "true");
            System.setProperty(KEY_UPDATED_AT, Long.toString(System.currentTimeMillis()));

            long elapsedMs = System.currentTimeMillis() - start;
            logUpdateCapped(totalKeysUpdated, elapsedMs);
            if (pricklerPlayer >= 0 && pricklerLogs < MAX_PRICKLER_LOGS) {
                pricklerLogs++;
                LOG.info("WP_COUNT_UPDATER hhe_prickler player=" + pricklerPlayer + " storage=" + pricklerStorage);
            }
            if (fighterSummaryLogs < MAX_FIGHTER_SUMMARY_LOGS) {
                fighterSummaryLogs++;
                LOG.info("WP_COUNT_UPDATER fighters wingCount=" + fighterWingCount
                        + " nonzeroPlayer=" + fighterNonzeroPlayer
                        + " nonzeroStorage=" + fighterNonzeroStorage);
            }
        } catch (Throwable t) {
            System.setProperty(KEY_READY, "false");
            if (!updaterErrorLogged) {
                updaterErrorLogged = true;
                LOG.error("WP_COUNT_UPDATER error", t);
            }
        }
    }

    private void logUpdateCapped(int weaponCount, long elapsedMs) {
        if (updateLogs >= MAX_UPDATE_LOGS) {
            return;
        }
        updateLogs++;
            LOG.info("WP_COUNT_UPDATER updated weaponCount=" + weaponCount + " elapsedMs=" + elapsedMs);
    }

    private static Map<String, Integer> collectWeaponCounts(CargoAPI cargo) {
        Map<String, Integer> counts = new HashMap<String, Integer>();
        if (cargo == null || cargo.getWeapons() == null) {
            return counts;
        }
        for (CargoAPI.CargoItemQuantity<String> quantity : cargo.getWeapons()) {
            if (quantity != null) {
                addCount(counts, quantity.getItem(), quantity.getCount());
            }
        }
        return counts;
    }

    private static Map<String, Integer> collectFighterCounts(CargoAPI cargo) {
        Map<String, Integer> counts = new HashMap<String, Integer>();
        if (cargo == null || cargo.getFighters() == null) {
            return counts;
        }
        for (CargoAPI.CargoItemQuantity<String> quantity : cargo.getFighters()) {
            if (quantity != null) {
                addCount(counts, quantity.getItem(), quantity.getCount());
            }
        }
        return counts;
    }

    private static void collectAccessibleStorageCounts(List<MarketAPI> markets,
                                                       Map<String, Integer> weaponCounts,
                                                       Map<String, Integer> fighterCounts) {
        if (markets == null) {
            return;
        }
        for (MarketAPI market : markets) {
            if (market == null || !Misc.playerHasStorageAccess(market)) {
                continue;
            }
            CargoAPI storageCargo = Misc.getStorageCargo(market);
            mergeCounts(weaponCounts, collectWeaponCounts(storageCargo));
            mergeCounts(fighterCounts, collectFighterCounts(storageCargo));
        }
    }

    private static void mergeCounts(Map<String, Integer> target, Map<String, Integer> source) {
        for (Map.Entry<String, Integer> entry : source.entrySet()) {
            addCount(target, entry.getKey(), entry.getValue().intValue());
        }
    }

    private static void addCount(Map<String, Integer> counts, String id, int count) {
        if (id == null || id.isEmpty() || count == 0) {
            return;
        }
        Integer existing = counts.get(id);
        counts.put(id, Integer.valueOf((existing == null ? 0 : existing.intValue()) + count));
    }

    private static int getCount(Map<String, Integer> counts, String id) {
        Integer count = counts.get(id);
        return count == null ? 0 : count.intValue();
    }
}
