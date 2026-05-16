package weaponsprocurement.core;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class TheoreticalSaleIndex {
    private static final int OPEN_TIER_CAP = 0;
    private static final int MILITARY_TIER_CAP = 3;
    private static final int BLACK_TIER_CAP = 3;

    public Map<String, Candidate> collect(SectorAPI sector, WeaponMarketBlacklist blacklist) {
        EconomyAPI economy = sector == null ? null : sector.getEconomy();
        List<MarketAPI> markets = economy == null ? null : economy.getMarketsCopy();
        if (markets == null || markets.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Candidate> result = new HashMap<String, Candidate>();
        for (int i = 0; i < markets.size(); i++) {
            MarketAPI market = markets.get(i);
            if (market == null || market.getSubmarketsCopy() == null) {
                continue;
            }
            for (SubmarketAPI submarket : market.getSubmarketsCopy()) {
                int tierCap = tierCap(submarket);
                if (tierCap < 0) {
                    continue;
                }
                boolean militarySubmarket = Submarkets.GENERIC_MILITARY.equals(submarket.getSpecId());
                Set<String> factionIds = relevantFactionIds(market, submarket);
                for (String factionId : factionIds) {
                    FactionAPI faction = safeFaction(sector, factionId);
                    addFactionWeapons(result, faction, tierCap, militarySubmarket, blacklist);
                    addFactionWings(result, faction, tierCap, militarySubmarket, blacklist);
                }
            }
        }
        return result;
    }

    private static void addFactionWeapons(Map<String, Candidate> result,
                                          FactionAPI faction,
                                          int tierCap,
                                          boolean militarySubmarket,
                                          WeaponMarketBlacklist blacklist) {
        if (faction == null) {
            return;
        }
        Map<String, Float> sellFrequency = faction.getWeaponSellFrequency();
        Set<String> ids = candidateIds(faction.getKnownWeapons(), sellFrequency);
        for (String weaponId : ids) {
            WeaponSpecAPI spec = safeWeaponSpec(weaponId);
            if (spec == null || spec.getTier() > tierCap) {
                continue;
            }
            String itemKey = StockItemType.WEAPON.key(weaponId);
            if (blacklist != null && blacklist.isBannedFromFixers(itemKey)) {
                continue;
            }
            if (!FixerMarketObservedCatalog.isSafeFixerItem(itemKey)) {
                continue;
            }
            if (!militarySubmarket && hasTag(spec.getTags(), "military_market_only")) {
                continue;
            }
            try {
                if (spec.getAIHints() != null && spec.getAIHints().contains(WeaponAPI.AIHints.SYSTEM)) {
                    continue;
                }
            } catch (Throwable ignored) {
            }
            Float frequency = frequency(sellFrequency, weaponId);
            if (frequency != null && frequency.floatValue() <= 0f) {
                continue;
            }
            addCandidate(result, itemKey, spec.getTier(), frequency, Math.max(0, Math.round(spec.getBaseValue())), 1f);
        }
    }

    private static void addFactionWings(Map<String, Candidate> result,
                                        FactionAPI faction,
                                        int tierCap,
                                        boolean militarySubmarket,
                                        WeaponMarketBlacklist blacklist) {
        if (faction == null) {
            return;
        }
        Map<String, Float> sellFrequency = faction.getFighterSellFrequency();
        Set<String> ids = candidateIds(faction.getKnownFighters(), sellFrequency);
        for (String wingId : ids) {
            FighterWingSpecAPI spec = safeWingSpec(wingId);
            if (spec == null || spec.getTier() > tierCap) {
                continue;
            }
            String itemKey = StockItemType.WING.key(wingId);
            if (blacklist != null && blacklist.isBannedFromFixers(itemKey)) {
                continue;
            }
            if (!FixerMarketObservedCatalog.isSafeFixerItem(itemKey)) {
                continue;
            }
            if (!militarySubmarket && hasTag(spec.getTags(), "military_market_only")) {
                continue;
            }
            Float frequency = frequency(sellFrequency, wingId);
            if (frequency != null && frequency.floatValue() <= 0f) {
                continue;
            }
            addCandidate(result, itemKey, spec.getTier(), frequency, Math.max(0, Math.round(spec.getBaseValue())), 1f);
        }
    }

    private static void addCandidate(Map<String, Candidate> result,
                                     String itemKey,
                                     int tier,
                                     Float sellFrequency,
                                     int baseUnitPrice,
                                     float unitCargoSpace) {
        Candidate candidate = new Candidate(
                itemKey,
                tier,
                sellFrequency,
                baseUnitPrice,
                unitCargoSpace,
                RarityClassifier.classify(tier, sellFrequency));
        Candidate current = result.get(itemKey);
        if (current == null || candidate.compareTo(current) < 0) {
            result.put(itemKey, candidate);
        }
    }

    private static Set<String> relevantFactionIds(MarketAPI market, SubmarketAPI submarket) {
        Set<String> result = new HashSet<String>();
        String id = submarket == null ? null : submarket.getSpecId();
        if (Submarkets.SUBMARKET_OPEN.equals(id)) {
            addFactionId(result, market == null ? null : market.getFactionId());
        } else if (Submarkets.GENERIC_MILITARY.equals(id)) {
            addFactionId(result, market == null ? null : market.getFactionId());
            addFactionId(result, submarket.getFaction() == null ? null : submarket.getFaction().getId());
        } else if (Submarkets.SUBMARKET_BLACK.equals(id)) {
            addFactionId(result, market == null ? null : market.getFactionId());
            addFactionId(result, submarket.getFaction() == null ? null : submarket.getFaction().getId());
        }
        return result;
    }

    private static void addFactionId(Set<String> result, String factionId) {
        if (factionId != null && factionId.trim().length() > 0) {
            result.add(factionId);
        }
    }

    private static int tierCap(SubmarketAPI submarket) {
        if (submarket == null) {
            return -1;
        }
        String id = submarket.getSpecId();
        if (Submarkets.SUBMARKET_OPEN.equals(id)) {
            return OPEN_TIER_CAP;
        }
        if (Submarkets.GENERIC_MILITARY.equals(id)) {
            return MILITARY_TIER_CAP;
        }
        if (Submarkets.SUBMARKET_BLACK.equals(id)) {
            return BLACK_TIER_CAP;
        }
        return -1;
    }

    private static FactionAPI safeFaction(SectorAPI sector, String factionId) {
        try {
            return sector == null || factionId == null ? null : sector.getFaction(factionId);
        } catch (Throwable ignored) {
            return null;
        }
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

    private static Float frequency(Map<String, Float> frequencies, String itemId) {
        return frequencies == null ? null : frequencies.get(itemId);
    }

    private static Set<String> candidateIds(Set<String> knownIds, Map<String, Float> sellFrequency) {
        if (sellFrequency != null && !sellFrequency.isEmpty()) {
            return new HashSet<String>(sellFrequency.keySet());
        }
        if (knownIds == null || knownIds.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<String>(knownIds);
    }

    private static boolean hasTag(Set<String> tags, String target) {
        if (tags == null || target == null) {
            return false;
        }
        String lowerTarget = target.toLowerCase(Locale.US);
        for (String tag : tags) {
            if (tag != null && lowerTarget.equals(tag.toLowerCase(Locale.US))) {
                return true;
            }
        }
        return false;
    }

    public static final class Candidate implements Comparable<Candidate> {
        private final String itemKey;
        private final int tier;
        private final Float sellFrequency;
        private final int baseUnitPrice;
        private final float unitCargoSpace;
        private final FixerRarity rarity;

        private Candidate(String itemKey,
                          int tier,
                          Float sellFrequency,
                          int baseUnitPrice,
                          float unitCargoSpace,
                          FixerRarity rarity) {
            this.itemKey = itemKey;
            this.tier = tier;
            this.sellFrequency = sellFrequency;
            this.baseUnitPrice = baseUnitPrice;
            this.unitCargoSpace = unitCargoSpace;
            this.rarity = rarity;
        }

        public String getItemKey() {
            return itemKey;
        }

        public int getBaseUnitPrice() {
            return baseUnitPrice;
        }

        public float getUnitCargoSpace() {
            return unitCargoSpace;
        }

        public FixerRarity getRarity() {
            return rarity;
        }

        @Override
        public int compareTo(Candidate other) {
            int result = Integer.compare(tier, other.tier);
            if (result != 0) {
                return result;
            }
            float leftFrequency = sellFrequency == null ? 0f : sellFrequency.floatValue();
            float rightFrequency = other.sellFrequency == null ? 0f : other.sellFrequency.floatValue();
            result = -Float.compare(leftFrequency, rightFrequency);
            if (result != 0) {
                return result;
            }
            return itemKey.compareTo(other.itemKey);
        }
    }
}
