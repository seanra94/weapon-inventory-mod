package weaponsprocurement.internal;

import org.apache.log4j.Logger;

public class WeaponsProcurementBadgeHelper {
    private static final Logger LOG = Logger.getLogger(WeaponsProcurementBadgeHelper.class);
    private static final int MAX_CALL_LOGS = 20;
    private static final int MAX_FIGHTER_CALL_LOGS = 40;

    private static final String TOTAL_ERR = "graphics/ui/wp_total_err.png";
    private static final String TOTAL_99PLUS = "graphics/ui/wp_total_green_99plus.png";
    private static final String TOTAL_RED_PREFIX = "graphics/ui/wp_total_red_";
    private static final String TOTAL_YELLOW_PREFIX = "graphics/ui/wp_total_yellow_";
    private static final String TOTAL_GREEN_PREFIX = "graphics/ui/wp_total_green_";
    private static final String TOTAL_SUFFIX = ".png";

    private static final String KEY_READY = "wp.counts.ready";
    private static final String KEY_PATCHED_BADGES_ENABLED = "wp.config.patchedBadgesEnabled";
    private static final String KEY_WEAPON_PREFIX = "wp.weapon.";
    private static final String KEY_FIGHTER_PREFIX = "wp.fighter.";
    private static final String KEY_PLAYER_SUFFIX = ".player";
    private static final String KEY_STORAGE_SUFFIX = ".storage";

    private static boolean helperReachedLogged = false;
    private static boolean parseErrorLogged = false;
    private static int loggedCalls = 0;
    private static int fighterLoggedCalls = 0;

    private WeaponsProcurementBadgeHelper() {
    }

    public static String getTotalStatusSpritePath(String weaponId) {
        return getTotalStatusSpritePath("weapon", weaponId);
    }

    public static String getTotalStatusSpritePath(String kind, String id) {
        logHelperReachedOnce();

        if (!isPatchedBadgesEnabled()) {
            return null;
        }

        boolean ready = isReady();
        Integer playerCount = null;
        Integer storageCount = null;
        boolean playerError = false;
        boolean storageError = false;
        Integer totalCount = null;
        String totalSprite = TOTAL_ERR;

        if (!ready || id == null || id.isEmpty()) {
            logCallCapped(kind, id, playerCount, storageCount, totalCount, totalSprite, ready, true, true);
            logFighterCallCapped(kind, id, ready, playerCount, storageCount, totalCount, totalSprite, true, true);
            return TOTAL_ERR;
        }

        playerCount = readCount(kind, id, true);
        if (playerCount == null) {
            playerError = true;
        }
        storageCount = readCount(kind, id, false);
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

        logCallCapped(kind, id, playerCount, storageCount, totalCount, totalSprite, true, playerError, storageError);
        logFighterCallCapped(kind, id, ready, playerCount, storageCount, totalCount, totalSprite, playerError, storageError);
        return totalSprite;
    }

    private static boolean isReady() {
        return "true".equalsIgnoreCase(System.getProperty(KEY_READY));
    }

    private static boolean isPatchedBadgesEnabled() {
        String raw = System.getProperty(KEY_PATCHED_BADGES_ENABLED);
        return raw == null || "true".equalsIgnoreCase(raw);
    }

    private static Integer readCount(String kind, String id, boolean player) {
        String prefix = getPrefix(kind);
        if (prefix == null) {
            return null;
        }
        String key = prefix + id + (player ? KEY_PLAYER_SUFFIX : KEY_STORAGE_SUFFIX);
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

    private static String getPrefix(String kind) {
        if ("weapon".equals(kind)) {
            return KEY_WEAPON_PREFIX;
        }
        if ("fighter".equals(kind)) {
            return KEY_FIGHTER_PREFIX;
        }
        return null;
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
        LOG.info("WP_DIAG_BADGE helper reached");
    }

    private static void logCallCapped(String kind, String id, Integer playerCount, Integer storageCount, Integer totalCount,
                                      String totalSprite, boolean ready, boolean playerError, boolean storageError) {
        if (loggedCalls >= MAX_CALL_LOGS) {
            return;
        }
        loggedCalls++;
        LOG.info("WP_DIAG_BADGE call kind=" + kind
                + " id=" + id
                + " ready=" + ready
                + " playerCount=" + playerCount
                + " storageCount=" + storageCount
                + " totalCount=" + totalCount
                + " totalSprite=" + totalSprite
                + " playerError=" + playerError
                + " storageError=" + storageError
        );
    }

    private static void logFighterCallCapped(String kind, String id, boolean ready, Integer playerCount, Integer storageCount,
                                             Integer totalCount, String totalSprite, boolean playerError, boolean storageError) {
        if (!"fighter".equals(kind) || fighterLoggedCalls >= MAX_FIGHTER_CALL_LOGS) {
            return;
        }
        fighterLoggedCalls++;
        LOG.info("WP_BADGE_HELPER kind=fighter"
                + " id=" + id
                + " ready=" + ready
                + " player=" + playerCount
                + " storage=" + storageCount
                + " total=" + totalCount
                + " sprite=" + totalSprite
                + " playerError=" + playerError
                + " storageError=" + storageError);
    }

    private static void logParseErrorOnce(String key, String value, Throwable t) {
        if (parseErrorLogged) {
            return;
        }
        parseErrorLogged = true;
        LOG.error("WP_DIAG_BADGE parse error key=" + key + " value=" + value, t);
    }
}
