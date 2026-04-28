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

    private static final Map<String, CountsSnapshot> CACHE = new HashMap<String, CountsSnapshot>();
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
        CountsSnapshot snapshot = getCountsSnapshot(weaponId);
        if (snapshot.error) {
            return PLAYER_ERR;
        }
        return toPlayerSprite(snapshot.playerCount);
    }

    public static String getStorageStatusSpritePath(String weaponId) {
        logHelperReachedOnce();
        CountsSnapshot snapshot = getCountsSnapshot(weaponId);
        String playerSprite = snapshot.error ? PLAYER_ERR : toPlayerSprite(snapshot.playerCount);
        String storageSprite = snapshot.error ? STORAGE_ERR : toStorageSprite(snapshot.storageCount);
        logCallCapped(weaponId, snapshot.playerCount, snapshot.storageCount, playerSprite, storageSprite, snapshot.error);
        return storageSprite;
    }

    private static CountsSnapshot getCountsSnapshot(String weaponId) {
        if (weaponId == null || weaponId.isEmpty()) {
            return CountsSnapshot.error();
        }

        long now = System.currentTimeMillis();
        CountsSnapshot cached = CACHE.get(weaponId);
        if (cached != null && now - cached.timestampMs <= CACHE_TTL_MS) {
            return cached;
        }

        CountsSnapshot computed = computeCounts(weaponId, now);
        CACHE.put(weaponId, computed);
        return computed;
    }

    private static CountsSnapshot computeCounts(String weaponId, long now) {
        try {
            SectorAPI sector = Global.getSector();
            if (sector == null) {
                return CountsSnapshot.error(now);
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

            return new CountsSnapshot(playerCount, storageCount, false, now);
        } catch (Throwable t) {
            logErrorOnce(t);
            return CountsSnapshot.error(now);
        }
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

    private static final class CountsSnapshot {
        final int playerCount;
        final int storageCount;
        final boolean error;
        final long timestampMs;

        private CountsSnapshot(int playerCount, int storageCount, boolean error, long timestampMs) {
            this.playerCount = playerCount;
            this.storageCount = storageCount;
            this.error = error;
            this.timestampMs = timestampMs;
        }

        static CountsSnapshot error() {
            return error(System.currentTimeMillis());
        }

        static CountsSnapshot error(long now) {
            return new CountsSnapshot(0, 0, true, now);
        }
    }
}
