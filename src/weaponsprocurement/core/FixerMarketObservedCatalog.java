package weaponsprocurement.core;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class FixerMarketObservedCatalog {
    private static final Logger LOG = Logger.getLogger(FixerMarketObservedCatalog.class);
    private static final String PERSISTENT_KEY = "weaponsProcurement.fixerObservedCatalog.v1";
    private static final String VALUE_SEPARATOR = "|";

    private static final Set<String> SPOILER_TAGS = tags(
            "restricted",
            "no_dealer",
            "no_drop",
            "no_bp_drop",
            "omega",
            "remnant",
            "dweller",
            "threat",
            "hide_in_codex",
            "invisible_in_codex",
            "codex_unlockable");

    private final MarketStockService marketStockService = new MarketStockService();
    private static boolean migrationLogged;

    public int observeSectorStock(SectorAPI sector, WeaponMarketBlacklist blacklist) {
        Map<String, String> catalog = rawCatalog(sector);
        if (catalog == null) {
            return 0;
        }
        EconomyAPI economy = sector == null ? null : sector.getEconomy();
        List<MarketAPI> markets = economy == null ? null : economy.getMarketsCopy();
        if (markets == null) {
            return 0;
        }

        int added = 0;
        for (int i = 0; i < markets.size(); i++) {
            MarketStockService.MarketStock stock = marketStockService.collectCurrentMarketItemStock(markets.get(i), true);
            for (String itemKey : stock.itemKeys()) {
                if (!isSafeFixerItem(itemKey) || isBanned(blacklist, itemKey)) {
                    continue;
                }
                SubmarketWeaponStock source = cheapestReferenceSource(stock.getSubmarketStocks(itemKey));
                if (source == null) {
                    continue;
                }
                if (!catalog.containsKey(itemKey)) {
                    added++;
                }
                catalog.put(itemKey, encode(source.getBaseUnitPrice(), source.getUnitCargoSpace()));
            }
        }
        return added;
    }

    public Map<String, ObservedItem> observedItems(SectorAPI sector, WeaponMarketBlacklist blacklist) {
        Map<String, String> catalog = rawCatalog(sector);
        if (catalog == null || catalog.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, ObservedItem> result = new HashMap<String, ObservedItem>();
        for (Map.Entry<String, String> entry : catalog.entrySet()) {
            String itemKey = entry.getKey();
            if (!isSafeFixerItem(itemKey) || isBanned(blacklist, itemKey)) {
                continue;
            }
            ObservedItem item = decode(entry.getValue());
            if (item != null) {
                result.put(itemKey, item);
            }
        }
        return result;
    }

    public String cacheKey(SectorAPI sector) {
        Map<String, String> catalog = rawCatalog(sector);
        return catalog == null ? "observed=none" : "observed=" + catalog.size() + ":" + catalog.hashCode();
    }

    static boolean isSafeFixerItem(String itemKey) {
        StockItemType itemType = StockItemType.fromKey(itemKey);
        String itemId = StockItemType.rawId(itemKey);
        if (StockItemType.WING.equals(itemType)) {
            return isSafeWing(itemId);
        }
        return isSafeWeapon(itemId);
    }

    private static boolean isSafeWeapon(String weaponId) {
        WeaponSpecAPI spec = safeWeaponSpec(weaponId);
        if (spec == null) {
            return false;
        }
        try {
            if (spec.getAIHints() != null && spec.getAIHints().contains(WeaponAPI.AIHints.SYSTEM)) {
                return false;
            }
        } catch (Throwable ignored) {
        }
        Set<String> tags = lowerTags(spec.getTags());
        return !tags.contains("no_sell")
                && !tags.contains("weapon_no_sell")
                && !intersects(tags, SPOILER_TAGS);
    }

    private static boolean isSafeWing(String wingId) {
        FighterWingSpecAPI spec = safeWingSpec(wingId);
        if (spec == null) {
            return false;
        }
        Set<String> tags = lowerTags(spec.getTags());
        return !tags.contains("no_sell")
                && !tags.contains("wing_no_sell")
                && !intersects(tags, SPOILER_TAGS);
    }

    private static boolean isBanned(WeaponMarketBlacklist blacklist, String itemKey) {
        return blacklist != null && blacklist.isBannedFromFixers(itemKey);
    }

    private static SubmarketWeaponStock cheapestReferenceSource(List<SubmarketWeaponStock> sources) {
        SubmarketWeaponStock best = null;
        if (sources == null) {
            return null;
        }
        for (int i = 0; i < sources.size(); i++) {
            SubmarketWeaponStock source = sources.get(i);
            if (source == null || source.getCount() <= 0 || !source.isPurchasable()) {
                continue;
            }
            if (best == null || compareReferenceSource(source, best) < 0) {
                best = source;
            }
        }
        return best;
    }

    private static Map<String, String> rawCatalog(SectorAPI sector) {
        if (sector == null || sector.getPersistentData() == null) {
            return null;
        }
        Object existing = sector.getPersistentData().get(PERSISTENT_KEY);
        if (existing instanceof Map) {
            return sanitizedCatalog(sector, (Map<?, ?>) existing);
        }
        Map<String, String> catalog = new HashMap<String, String>();
        sector.getPersistentData().put(PERSISTENT_KEY, catalog);
        return catalog;
    }

    private static Map<String, String> sanitizedCatalog(SectorAPI sector, Map<?, ?> existing) {
        Map<String, String> catalog = new HashMap<String, String>();
        int discarded = 0;
        for (Map.Entry<?, ?> entry : existing.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            if (key instanceof String && value instanceof String) {
                catalog.put((String) key, (String) value);
            } else {
                discarded++;
            }
        }
        if (discarded > 0 && !migrationLogged) {
            migrationLogged = true;
            LOG.warn("WP_FIXER_CATALOG discarded " + discarded + " malformed persistent entries.");
        }
        sector.getPersistentData().put(PERSISTENT_KEY, catalog);
        return catalog;
    }

    private static String encode(int baseUnitPrice, float unitCargoSpace) {
        return Math.max(0, baseUnitPrice) + VALUE_SEPARATOR + Math.max(0.01f, unitCargoSpace);
    }

    private static ObservedItem decode(String value) {
        if (value == null) {
            return null;
        }
        String[] parts = value.split("\\|", 2);
        try {
            int baseUnitPrice = parts.length > 0 ? Integer.parseInt(parts[0]) : 0;
            float unitCargoSpace = parts.length > 1 ? Float.parseFloat(parts[1]) : 1f;
            return new ObservedItem(Math.max(0, baseUnitPrice), Math.max(0.01f, unitCargoSpace));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Set<String> lowerTags(Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> result = new HashSet<String>();
        for (String tag : tags) {
            if (tag != null) {
                result.add(tag.toLowerCase(Locale.US));
            }
        }
        return result;
    }

    private static boolean intersects(Set<String> left, Set<String> right) {
        if (left == null || right == null) {
            return false;
        }
        for (String value : left) {
            if (right.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> tags(String... tags) {
        Set<String> result = new HashSet<String>();
        for (String tag : tags) {
            result.add(tag);
        }
        return Collections.unmodifiableSet(result);
    }

    private static int compareReferenceSource(SubmarketWeaponStock left, SubmarketWeaponStock right) {
        int result = Integer.compare(left.getBaseUnitPrice(), right.getBaseUnitPrice());
        if (result != 0) {
            return result;
        }
        return left.getDisplaySourceName().compareToIgnoreCase(right.getDisplaySourceName());
    }

    private static WeaponSpecAPI safeWeaponSpec(String weaponId) {
        try {
            return Global.getSettings().getWeaponSpec(weaponId);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static FighterWingSpecAPI safeWingSpec(String wingId) {
        try {
            return Global.getSettings().getFighterWingSpec(wingId);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static final class ObservedItem {
        private final int baseUnitPrice;
        private final float unitCargoSpace;

        private ObservedItem(int baseUnitPrice, float unitCargoSpace) {
            this.baseUnitPrice = baseUnitPrice;
            this.unitCargoSpace = unitCargoSpace;
        }

        public int getBaseUnitPrice() {
            return baseUnitPrice;
        }

        public float getUnitCargoSpace() {
            return unitCargoSpace;
        }
    }
}
