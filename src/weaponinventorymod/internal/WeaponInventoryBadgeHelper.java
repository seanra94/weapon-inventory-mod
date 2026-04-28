package weaponinventorymod.internal;

import org.apache.log4j.Logger;

public class WeaponInventoryBadgeHelper {
    private static final Logger LOG = Logger.getLogger(WeaponInventoryBadgeHelper.class);
    private static final int MAX_CALL_LOGS = 20;

    private static final String TOTAL_ERR = "graphics/ui/wim_total_err.png";
    private static final String TOTAL_99PLUS = "graphics/ui/wim_total_green_99plus.png";
    private static final String TOTAL_RED_PREFIX = "graphics/ui/wim_total_red_";
    private static final String TOTAL_YELLOW_PREFIX = "graphics/ui/wim_total_yellow_";
    private static final String TOTAL_GREEN_PREFIX = "graphics/ui/wim_total_green_";
    private static final String TOTAL_SUFFIX = ".png";

    private static final String KEY_READY = "wim.counts.ready";
    private static final String KEY_PREFIX = "wim.weapon.";
    private static final String KEY_PLAYER_SUFFIX = ".player";
    private static final String KEY_STORAGE_SUFFIX = ".storage";

    private static boolean helperReachedLogged = false;
    private static boolean parseErrorLogged = false;
    private static int loggedCalls = 0;

    private WeaponInventoryBadgeHelper() {
    }

    public static String getTotalStatusSpritePath(String weaponId) {
        logHelperReachedOnce();

        boolean ready = isReady();
        Integer playerCount = null;
        Integer storageCount = null;
        boolean playerError = false;
        boolean storageError = false;
        Integer totalCount = null;
        String totalSprite = TOTAL_ERR;

        if (!ready || weaponId == null || weaponId.isEmpty()) {
            logCallCapped(weaponId, playerCount, storageCount, totalCount, totalSprite, ready, true, true);
            return TOTAL_ERR;
        }

        playerCount = readCount(weaponId, true);
        if (playerCount == null) {
            playerError = true;
        }
        storageCount = readCount(weaponId, false);
        if (storageCount == null) {
            storageError = true;
        }

        if (!playerError && !storageError) {
            int total = playerCount.intValue() + storageCount.intValue();
            if (total >= 99) {
                totalCount = Integer.valueOf(99);
                totalSprite = TOTAL_99PLUS;
            } else if (total >= 0) {
                totalCount = Integer.valueOf(total);
                totalSprite = toTotalSprite(total);
            } else {
                totalSprite = TOTAL_ERR;
            }
        }

        logCallCapped(weaponId, playerCount, storageCount, totalCount, totalSprite, true, playerError, storageError);
        return totalSprite;
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

    private static String toTotalSprite(int total) {
        if (total == 0) {
            return TOTAL_RED_PREFIX + "0" + TOTAL_SUFFIX;
        }
        if (total >= 1 && total <= 9) {
            return TOTAL_YELLOW_PREFIX + total + TOTAL_SUFFIX;
        }
        return TOTAL_GREEN_PREFIX + total + TOTAL_SUFFIX;
    }

    private static void logHelperReachedOnce() {
        if (helperReachedLogged) {
            return;
        }
        helperReachedLogged = true;
        LOG.info("WIM_DIAG_BADGE helper reached");
    }

    private static void logCallCapped(String weaponId, Integer playerCount, Integer storageCount, Integer totalCount,
                                      String totalSprite, boolean ready, boolean playerError, boolean storageError) {
        if (loggedCalls >= MAX_CALL_LOGS) {
            return;
        }
        loggedCalls++;
        LOG.info("WIM_DIAG_BADGE call weaponId=" + weaponId
                + " ready=" + ready
                + " playerCount=" + playerCount
                + " storageCount=" + storageCount
                + " totalCount=" + totalCount
                + " totalSprite=" + totalSprite
                + " playerError=" + playerError
                + " storageError=" + storageError
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
