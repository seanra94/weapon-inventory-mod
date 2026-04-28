package weaponinventorymod.internal;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Misc;
import org.apache.log4j.Logger;

import java.util.List;

public class WeaponInventoryCountUpdater implements EveryFrameScript {
    private static final Logger LOG = Logger.getLogger(WeaponInventoryCountUpdater.class);

    private static final float UPDATE_INTERVAL_SEC = 0.20f;
    private static final int MAX_UPDATE_LOGS = 30;
    private static final int MAX_PRICKLER_LOGS = 30;
    private static final int MAX_FIGHTER_LOGS = 30;

    private static final String KEY_READY = "wim.counts.ready";
    private static final String KEY_UPDATED_AT = "wim.counts.updatedAt";
    private static final String KEY_WEAPON_PREFIX = "wim.weapon.";
    private static final String KEY_FIGHTER_PREFIX = "wim.fighter.";
    private static final String KEY_PLAYER_SUFFIX = ".player";
    private static final String KEY_STORAGE_SUFFIX = ".storage";
    private static final String PRICKLER_ID = "hhe_prickler";

    private float elapsedSinceUpdate = 0f;
    private int updateLogs = 0;
    private int pricklerLogs = 0;
    private int fighterLogs = 0;
    private boolean updaterErrorLogged = false;

    public WeaponInventoryCountUpdater() {
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
        elapsedSinceUpdate += amount;
        if (elapsedSinceUpdate < UPDATE_INTERVAL_SEC) {
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

            int pricklerPlayer = -1;
            int pricklerStorage = -1;
            int sampledFighterPlayer = -1;
            int sampledFighterStorage = -1;
            String sampledFighterId = null;
            int totalKeysUpdated = 0;

            if (weaponIds != null) {
                for (String weaponId : weaponIds) {
                    if (weaponId == null || weaponId.isEmpty()) {
                        continue;
                    }
                    int playerCount = 0;
                    if (playerCargo != null) {
                        playerCount = playerCargo.getNumWeapons(weaponId);
                    }

                    int storageCount = 0;
                    if (markets != null) {
                        for (MarketAPI market : markets) {
                            if (market == null || !Misc.playerHasStorageAccess(market)) {
                                continue;
                            }
                            CargoAPI storageCargo = Misc.getStorageCargo(market);
                            if (storageCargo != null) {
                                storageCount += storageCargo.getNumWeapons(weaponId);
                            }
                        }
                    }

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
                    int playerCount = 0;
                    if (playerCargo != null) {
                        playerCount = playerCargo.getNumFighters(fighterId);
                    }

                    int storageCount = 0;
                    if (markets != null) {
                        for (MarketAPI market : markets) {
                            if (market == null || !Misc.playerHasStorageAccess(market)) {
                                continue;
                            }
                            CargoAPI storageCargo = Misc.getStorageCargo(market);
                            if (storageCargo != null) {
                                storageCount += storageCargo.getNumFighters(fighterId);
                            }
                        }
                    }

                    System.setProperty(KEY_FIGHTER_PREFIX + fighterId + KEY_PLAYER_SUFFIX, Integer.toString(playerCount));
                    System.setProperty(KEY_FIGHTER_PREFIX + fighterId + KEY_STORAGE_SUFFIX, Integer.toString(storageCount));
                    totalKeysUpdated++;

                    if (sampledFighterId == null) {
                        sampledFighterId = fighterId;
                        sampledFighterPlayer = playerCount;
                        sampledFighterStorage = storageCount;
                    }
                }
            }

            System.setProperty(KEY_READY, "true");
            System.setProperty(KEY_UPDATED_AT, Long.toString(System.currentTimeMillis()));

            long elapsedMs = System.currentTimeMillis() - start;
            logUpdateCapped(totalKeysUpdated, elapsedMs);
            if (pricklerPlayer >= 0 && pricklerLogs < MAX_PRICKLER_LOGS) {
                pricklerLogs++;
                LOG.info("WIM_COUNT_UPDATER hhe_prickler player=" + pricklerPlayer + " storage=" + pricklerStorage);
            }
            if (sampledFighterId != null && fighterLogs < MAX_FIGHTER_LOGS) {
                fighterLogs++;
                LOG.info("WIM_COUNT_UPDATER fighter sample id=" + sampledFighterId + " player=" + sampledFighterPlayer + " storage=" + sampledFighterStorage);
            }
        } catch (Throwable t) {
            System.setProperty(KEY_READY, "false");
            if (!updaterErrorLogged) {
                updaterErrorLogged = true;
                LOG.error("WIM_COUNT_UPDATER error", t);
            }
        }
    }

    private void logUpdateCapped(int weaponCount, long elapsedMs) {
        if (updateLogs >= MAX_UPDATE_LOGS) {
            return;
        }
        updateLogs++;
        LOG.info("WIM_COUNT_UPDATER updated weaponCount=" + weaponCount + " elapsedMs=" + elapsedMs);
    }
}
