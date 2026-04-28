package weaponinventorymod.internal;

import org.apache.log4j.Logger;

public class WeaponInventoryBadgeHelper {
    private static final Logger LOG = Logger.getLogger(WeaponInventoryBadgeHelper.class);
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

    private static final String KEY_READY = "wim.counts.ready";
    private static final String KEY_PREFIX = "wim.weapon.";
    private static final String KEY_PLAYER_SUFFIX = ".player";
    private static final String KEY_STORAGE_SUFFIX = ".storage";

    private static boolean helperReachedLogged = false;
    private static boolean parseErrorLogged = false;
    private static int loggedCalls = 0;

    private WeaponInventoryBadgeHelper() {
    }

    public static String getAnchorSpritePath() {
        logHelperReachedOnce();
        return ANCHOR;
    }

    public static String getPlayerStatusSpritePath(String weaponId) {
        logHelperReachedOnce();
        if (!isReady() || weaponId == null || weaponId.isEmpty()) {
            return PLAYER_ERR;
        }
        Integer playerCount = readCount(weaponId, true);
        if (playerCount == null) {
            return PLAYER_ERR;
        }
        return toPlayerSprite(playerCount.intValue());
    }

    public static String getStorageStatusSpritePath(String weaponId) {
        logHelperReachedOnce();

        boolean ready = isReady();
        Integer playerCount = null;
        Integer storageCount = null;
        boolean playerError = true;
        boolean storageError = true;
        String playerSprite = PLAYER_ERR;
        String storageSprite = STORAGE_ERR;

        if (!ready || weaponId == null || weaponId.isEmpty()) {
            logCallCapped(weaponId, playerCount, playerError, playerSprite, storageCount, storageError, storageSprite, ready);
            return STORAGE_ERR;
        }

        playerCount = readCount(weaponId, true);
        if (playerCount != null) {
            playerError = false;
            playerSprite = toPlayerSprite(playerCount.intValue());
        }

        storageCount = readCount(weaponId, false);
        if (storageCount != null) {
            storageError = false;
            storageSprite = toStorageSprite(storageCount.intValue());
        }

        logCallCapped(weaponId, playerCount, playerError, playerSprite, storageCount, storageError, storageSprite, true);
        return storageSprite;
    }

    private static boolean isReady() {
        return "true".equalsIgnoreCase(System.getProperty(KEY_READY));
    }

    private static Integer readCount(String weaponId, boolean player) {
        String key = KEY_PREFIX + weaponId + (player ? KEY_PLAYER_SUFFIX : KEY_STORAGE_SUFFIX);
        String raw = System.getProperty(key);
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(raw);
        } catch (Throwable t) {
            logParseErrorOnce(key, raw, t);
            return null;
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

    private static void logCallCapped(String weaponId, Integer playerCount, boolean playerError, String playerSprite,
                                      Integer storageCount, boolean storageError, String storageSprite, boolean ready) {
        if (loggedCalls >= MAX_CALL_LOGS) {
            return;
        }
        loggedCalls++;
        LOG.info("WIM_DIAG_BADGE call weaponId=" + weaponId
                + " ready=" + ready
                + " playerCount=" + playerCount
                + " playerError=" + playerError
                + " playerSprite=" + playerSprite
                + " storageCount=" + storageCount
                + " storageError=" + storageError
                + " storageSprite=" + storageSprite
        );
    }

    private static void logParseErrorOnce(String key, String value, Throwable t) {
        if (parseErrorLogged) {
            return;
        }
        parseErrorLogged = true;
        LOG.error("WIM_DIAG_BADGE parse error key=" + key + " value=" + value, t);
    }
}
