package weaponinventorymod.core;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

final class WeaponMarketBlacklist {
    private static final Logger LOG = Logger.getLogger(WeaponMarketBlacklist.class);
    private static final String CONFIG_PATH = "data/config/weapon_inventory_market_blacklist.json";
    private static final String SECTOR_KEY = "BANNED_FROM_SECTOR_MARKET";
    private static final String FIXERS_KEY = "BANNED_FROM_FIXERS_MARKET";

    private static WeaponMarketBlacklist cached;
    private static boolean errorLogged;

    private final Set<String> sector;
    private final Set<String> fixers;

    private WeaponMarketBlacklist(Set<String> sector, Set<String> fixers) {
        this.sector = Collections.unmodifiableSet(new HashSet<String>(sector));
        this.fixers = Collections.unmodifiableSet(new HashSet<String>(fixers));
    }

    static WeaponMarketBlacklist load() {
        if (cached != null) {
            return cached;
        }
        try {
            JSONObject root = Global.getSettings().loadJSON(CONFIG_PATH);
            cached = new WeaponMarketBlacklist(readSet(root, SECTOR_KEY), readSet(root, FIXERS_KEY));
        } catch (Throwable t) {
            if (!errorLogged) {
                errorLogged = true;
                LOG.warn("WIM_MARKET_BLACKLIST load failed; using empty blacklist from " + CONFIG_PATH, t);
            }
            cached = new WeaponMarketBlacklist(Collections.<String>emptySet(), Collections.<String>emptySet());
        }
        return cached;
    }

    boolean isBannedFromSector(String itemKey) {
        return contains(sector, itemKey);
    }

    boolean isBannedFromFixers(String itemKey) {
        return contains(fixers, itemKey);
    }

    String cacheKey() {
        return "sector=" + sector.hashCode() + "|fixers=" + fixers.hashCode();
    }

    private static Set<String> readSet(JSONObject root, String key) {
        if (root == null) {
            return Collections.emptySet();
        }
        JSONArray array = root.optJSONArray(key);
        if (array == null) {
            return Collections.emptySet();
        }
        Set<String> result = new HashSet<String>();
        for (int i = 0; i < array.length(); i++) {
            String value = normalize(array.optString(i, null));
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }

    private static boolean contains(Set<String> set, String itemKey) {
        String rawId = StockItemType.rawId(itemKey);
        if (set.contains(normalize(itemKey)) || set.contains(normalize(rawId))) {
            return true;
        }
        WeaponSpecAPI spec = safeWeaponSpec(rawId);
        return spec != null && set.contains(normalize(spec.getWeaponName()));
    }

    private static WeaponSpecAPI safeWeaponSpec(String weaponId) {
        try {
            return Global.getSettings().getWeaponSpec(weaponId);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.US);
    }
}
