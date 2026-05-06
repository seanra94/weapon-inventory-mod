package weaponinventorymod.core;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class StockReviewConfig {
    private static final Logger LOG = Logger.getLogger(StockReviewConfig.class);
    private static final String CONFIG_PATH = "data/config/weapon_inventory_stock.json";

    private static final int DEFAULT_SMALL_WEAPON_COUNT = 8;
    private static final int DEFAULT_MEDIUM_WEAPON_COUNT = 4;
    private static final int DEFAULT_LARGE_WEAPON_COUNT = 2;

    private final int smallWeaponDesired;
    private final int mediumWeaponDesired;
    private final int largeWeaponDesired;
    private final boolean includeCurrentMarketStorage;
    private final boolean includeBlackMarket;
    private final StockDisplayMode displayMode;
    private final StockSortMode sortMode;
    private final Map<String, Integer> desiredOverrides;
    private final Map<String, Boolean> ignoredWeapons;

    private StockReviewConfig(int smallWeaponDesired,
                              int mediumWeaponDesired,
                              int largeWeaponDesired,
                              boolean includeCurrentMarketStorage,
                              boolean includeBlackMarket,
                              StockDisplayMode displayMode,
                              StockSortMode sortMode,
                              Map<String, Integer> desiredOverrides,
                              Map<String, Boolean> ignoredWeapons) {
        this.smallWeaponDesired = smallWeaponDesired;
        this.mediumWeaponDesired = mediumWeaponDesired;
        this.largeWeaponDesired = largeWeaponDesired;
        this.includeCurrentMarketStorage = includeCurrentMarketStorage;
        this.includeBlackMarket = includeBlackMarket;
        this.displayMode = displayMode;
        this.sortMode = sortMode;
        this.desiredOverrides = Collections.unmodifiableMap(new HashMap<String, Integer>(desiredOverrides));
        this.ignoredWeapons = Collections.unmodifiableMap(new HashMap<String, Boolean>(ignoredWeapons));
    }

    public static StockReviewConfig load() {
        try {
            JSONObject json = Global.getSettings().loadJSON(CONFIG_PATH);
            return fromJson(json);
        } catch (Throwable t) {
            LOG.warn("WIM_STOCK_REVIEW config load failed; using defaults from " + CONFIG_PATH, t);
            return defaults();
        }
    }

    public static StockReviewConfig defaults() {
        return new StockReviewConfig(
                DEFAULT_SMALL_WEAPON_COUNT,
                DEFAULT_MEDIUM_WEAPON_COUNT,
                DEFAULT_LARGE_WEAPON_COUNT,
                true,
                true,
                StockDisplayMode.ALL_TRACKED,
                StockSortMode.NEED,
                Collections.<String, Integer>emptyMap(),
                Collections.<String, Boolean>emptyMap());
    }

    private static StockReviewConfig fromJson(JSONObject json) {
        JSONObject desiredDefaults = json.optJSONObject("desiredDefaults");
        int small = clampDesired(optInt(desiredDefaults, "smallWeapon", DEFAULT_SMALL_WEAPON_COUNT));
        int medium = clampDesired(optInt(desiredDefaults, "mediumWeapon", DEFAULT_MEDIUM_WEAPON_COUNT));
        int large = clampDesired(optInt(desiredDefaults, "largeWeapon", DEFAULT_LARGE_WEAPON_COUNT));

        JSONObject sources = json.optJSONObject("sources");
        boolean includeStorage = optBoolean(sources, "includeCurrentMarketStorage", true);
        boolean includeBlackMarket = optBoolean(sources, "includeBlackMarket", true);

        JSONObject display = json.optJSONObject("display");
        StockDisplayMode displayMode = StockDisplayMode.fromConfig(optString(display, "defaultMode", "OWNED_OR_FOR_SALE"));
        StockSortMode sortMode = StockSortMode.fromConfig(optString(display, "defaultSort", "NEED"));

        Map<String, Integer> overrides = new HashMap<String, Integer>();
        Map<String, Boolean> ignored = new HashMap<String, Boolean>();
        JSONObject perWeapon = json.optJSONObject("perWeapon");
        if (perWeapon != null) {
            String[] names = JSONObject.getNames(perWeapon);
            if (names != null) {
                for (int i = 0; i < names.length; i++) {
                    String weaponId = names[i];
                    JSONObject weaponConfig = perWeapon.optJSONObject(weaponId);
                    if (weaponConfig == null) {
                        continue;
                    }
                    if (weaponConfig.has("desired")) {
                        overrides.put(weaponId, Integer.valueOf(clampDesired(weaponConfig.optInt("desired", medium))));
                    }
                    ignored.put(weaponId, Boolean.valueOf(weaponConfig.optBoolean("ignored", false)));
                }
            }
        }

        return new StockReviewConfig(small, medium, large, includeStorage, includeBlackMarket, displayMode, sortMode, overrides, ignored);
    }

    public int desiredCount(String weaponId, WeaponAPI.WeaponSize size) {
        Integer override = desiredOverrides.get(weaponId);
        if (override != null) {
            return override.intValue();
        }
        if (WeaponAPI.WeaponSize.SMALL.equals(size)) {
            return smallWeaponDesired;
        }
        if (WeaponAPI.WeaponSize.MEDIUM.equals(size)) {
            return mediumWeaponDesired;
        }
        if (WeaponAPI.WeaponSize.LARGE.equals(size)) {
            return largeWeaponDesired;
        }
        return mediumWeaponDesired;
    }

    public boolean isIgnored(String weaponId) {
        Boolean ignored = ignoredWeapons.get(weaponId);
        return ignored != null && ignored.booleanValue();
    }

    public boolean isIncludeCurrentMarketStorage() {
        return includeCurrentMarketStorage;
    }

    public boolean isIncludeBlackMarket() {
        return includeBlackMarket;
    }

    public StockDisplayMode getDisplayMode() {
        return displayMode;
    }

    public StockSortMode getSortMode() {
        return sortMode;
    }

    public OwnedSourcePolicy ownedSourcePolicy(boolean includeStorage) {
        return includeStorage ? OwnedSourcePolicy.FLEET_AND_ACCESSIBLE_STORAGE : OwnedSourcePolicy.FLEET_ONLY;
    }

    private static int optInt(JSONObject json, String key, int defaultValue) {
        return json == null ? defaultValue : json.optInt(key, defaultValue);
    }

    private static boolean optBoolean(JSONObject json, String key, boolean defaultValue) {
        return json == null ? defaultValue : json.optBoolean(key, defaultValue);
    }

    private static String optString(JSONObject json, String key, String defaultValue) {
        return json == null ? defaultValue : json.optString(key, defaultValue);
    }

    private static int clampDesired(int value) {
        if (value < 0) {
            return 0;
        }
        if (value > 999) {
            return 999;
        }
        return value;
    }
}
