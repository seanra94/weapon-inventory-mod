package weaponinventorymod.internal;

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

public class WeaponInventoryBadgeHelper {
    private static final Logger LOG = Logger.getLogger(WeaponInventoryBadgeHelper.class);
    private static final long CACHE_TTL_MS = 750L;
    private static final int MAX_CALL_LOGS = 20;

    private static final String ANCHOR = "graphics/ui/wim_diag_anchor.png";
    private static final String PLAYER_0 = "graphics/ui/wim_diag_player_0.png";
    private static final String PLAYER_1 = "graphics/ui/wim_diag_player_1.png";
    private static final String PLAYER_2PLUS = "graphics/ui/wim_diag_player_2plus.png";
    private static final String PLAYER_ERR = "graphics/ui/wim_diag_player_err.png";
    private static final String STORAGE_0 = "graphics/ui/wim_diag_storage_0.png";
    private static final String STORAGE_1 = "graphics/ui/wim_diag_storage_1.png";
    private static final String STORAGE_2PLUS = "graphics/ui/wim_diag_storage_2plus.png";
    private static final String STORAGE_ERR = "graphics/ui/wim_diag_storage_err.png";

    private static final Map<String, Integer> PLAYER_COUNT_CACHE = new HashMap<String, Integer>();
    private static final Map<String, Integer> STORAGE_COUNT_CACHE = new HashMap<String, Integer>();
    private static final Map<String, Boolean> ERROR_CACHE = new HashMap<String, Boolean>();
    private static final Map<String, Long> CACHE_TIME_MS = new HashMap<String, Long>();
    private static boolean helperReachedLogged = false;
    private static boolean errorLogged = false;
    private static int loggedCalls = 0;

    private WeaponInventoryBadgeHelper() {
    }

    public static String getAnchorSpritePath() {
        logHelperReachedOnce();
        return ANCHOR;
    }

    public static String getPlayerStatusSpritePath(String weaponId) {
        logHelperReachedOnce();
        if (weaponId == null || weaponId.isEmpty()) {
            return PLAYER_ERR;
        }
        ensureCounts(weaponId);
        if (isError(weaponId)) {
            return PLAYER_ERR;
        }
        return toPlayerSprite(getPlayerCount(weaponId));
    }

    public static String getStorageStatusSpritePath(String weaponId) {
        logHelperReachedOnce();
        if (weaponId == null || weaponId.isEmpty()) {
            logCallCapped(weaponId, 0, 0, PLAYER_ERR, STORAGE_ERR, true);
            return STORAGE_ERR;
        }
        ensureCounts(weaponId);
        boolean error = isError(weaponId);
        int playerCount = getPlayerCount(weaponId);
        int storageCount = getStorageCount(weaponId);
        String playerSprite = error ? PLAYER_ERR : toPlayerSprite(playerCount);
        String storageSprite = error ? STORAGE_ERR : toStorageSprite(storageCount);
        logCallCapped(weaponId, playerCount, storageCount, playerSprite, storageSprite, error);
        return storageSprite;
    }

    private static void ensureCounts(String weaponId) {
        long now = System.currentTimeMillis();
        Long cachedAt = CACHE_TIME_MS.get(weaponId);
        if (cachedAt != null && now - cachedAt <= CACHE_TTL_MS) {
            return;
        }
        refreshCounts(weaponId, now);
    }

    private static void refreshCounts(String weaponId, long now) {
        try {
            SectorAPI sector = Global.getSector();
            if (sector == null) {
                setCounts(weaponId, 0, 0, true, now);
                return;
            }

            int playerCount = 0;
            CampaignFleetAPI fleet = sector.getPlayerFleet();
            if (fleet != null) {
                CargoAPI fleetCargo = fleet.getCargo();
                if (fleetCargo != null) {
                    playerCount = fleetCargo.getNumWeapons(weaponId);
                }
            }

            int storageCount = 0;
            EconomyAPI economy = sector.getEconomy();
            if (economy != null) {
                List<MarketAPI> markets = economy.getMarketsCopy();
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
            }

            setCounts(weaponId, playerCount, storageCount, false, now);
        } catch (Throwable t) {
            logErrorOnce(t);
            setCounts(weaponId, 0, 0, true, now);
        }
    }

    private static void setCounts(String weaponId, int playerCount, int storageCount, boolean error, long now) {
        PLAYER_COUNT_CACHE.put(weaponId, playerCount);
        STORAGE_COUNT_CACHE.put(weaponId, storageCount);
        ERROR_CACHE.put(weaponId, error);
        CACHE_TIME_MS.put(weaponId, now);
    }

    private static int getPlayerCount(String weaponId) {
        Integer value = PLAYER_COUNT_CACHE.get(weaponId);
        return value == null ? 0 : value.intValue();
    }

    private static int getStorageCount(String weaponId) {
        Integer value = STORAGE_COUNT_CACHE.get(weaponId);
        return value == null ? 0 : value.intValue();
    }

    private static boolean isError(String weaponId) {
        Boolean value = ERROR_CACHE.get(weaponId);
        return value != null && value.booleanValue();
    }

    private static String toPlayerSprite(int count) {
        if (count <= 0) {
            return PLAYER_0;
        }
        if (count == 1) {
            return PLAYER_1;
        }
        return PLAYER_2PLUS;
    }

    private static String toStorageSprite(int count) {
        if (count <= 0) {
            return STORAGE_0;
        }
        if (count == 1) {
            return STORAGE_1;
        }
        return STORAGE_2PLUS;
    }

    private static void logHelperReachedOnce() {
        if (helperReachedLogged) {
            return;
        }
        helperReachedLogged = true;
        LOG.info("WIM_DIAG_BADGE helper reached");
    }

    private static void logCallCapped(String weaponId, int playerCount, int storageCount, String playerSprite, String storageSprite, boolean error) {
        if (loggedCalls >= MAX_CALL_LOGS) {
            return;
        }
        loggedCalls++;
        LOG.info("WIM_DIAG_BADGE call weaponId=" + weaponId
                + " playerCount=" + playerCount
                + " storageCount=" + storageCount
                + " playerSprite=" + playerSprite
                + " storageSprite=" + storageSprite
                + " error=" + error);
    }

    private static void logErrorOnce(Throwable t) {
        if (errorLogged) {
            return;
        }
        errorLogged = true;
        LOG.error("WIM_DIAG_BADGE helper error", t);
    }
}
