package weaponsprocurement.stock.fixer

import weaponsprocurement.config.WeaponMarketBlacklist
import weaponsprocurement.stock.item.StockItemSpecs
import weaponsprocurement.stock.item.StockItemType
import java.util.Collections
import java.util.HashSet
import java.util.Locale

object FixerCatalogPolicy {
    private const val MILITARY_MARKET_ONLY_TAG = "military_market_only"

    private val spoilerTags: Set<String> = tags(
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
        "codex_unlockable",
    )

    @JvmStatic
    fun isSafeItem(itemKey: String?): Boolean {
        val itemType = StockItemType.fromKey(itemKey)
        val itemId = StockItemType.rawId(itemKey)
        return if (StockItemType.WING == itemType) {
            isSafeWing(itemId)
        } else {
            isSafeWeapon(itemId)
        }
    }

    @JvmStatic
    fun isEligibleObservedItem(itemKey: String?, blacklist: WeaponMarketBlacklist?): Boolean {
        return isSafeItem(itemKey) && !isBanned(blacklist, itemKey)
    }

    @JvmStatic
    fun isEligibleTheoreticalItem(
        itemKey: String?,
        blacklist: WeaponMarketBlacklist?,
        militarySubmarket: Boolean,
    ): Boolean {
        if (!isEligibleObservedItem(itemKey, blacklist)) return false
        return militarySubmarket || !hasTag(itemKey, MILITARY_MARKET_ONLY_TAG)
    }

    @JvmStatic
    fun isBanned(blacklist: WeaponMarketBlacklist?, itemKey: String?): Boolean {
        return blacklist != null && blacklist.isBannedFromFixers(itemKey)
    }

    private fun isSafeWeapon(weaponId: String?): Boolean {
        val spec = StockItemSpecs.weaponSpec(weaponId) ?: return false
        if (StockItemSpecs.hasSystemHint(spec)) return false
        val tags = lowerTags(spec.tags)
        return !tags.contains("no_sell") &&
            !tags.contains("weapon_no_sell") &&
            !intersects(tags, spoilerTags)
    }

    private fun isSafeWing(wingId: String?): Boolean {
        val spec = StockItemSpecs.wingSpec(wingId) ?: return false
        val tags = lowerTags(spec.tags)
        return !tags.contains("no_sell") &&
            !tags.contains("wing_no_sell") &&
            !intersects(tags, spoilerTags)
    }

    private fun hasTag(itemKey: String?, target: String): Boolean {
        val itemType = StockItemType.fromKey(itemKey)
        val itemId = StockItemType.rawId(itemKey)
        val tags = if (StockItemType.WING == itemType) {
            StockItemSpecs.wingSpec(itemId)?.tags
        } else {
            StockItemSpecs.weaponSpec(itemId)?.tags
        }
        if (tags == null || tags.isEmpty()) return false
        val lowerTarget = target.lowercase(Locale.US)
        for (tag in tags) {
            if (lowerTarget == tag.lowercase(Locale.US)) {
                return true
            }
        }
        return false
    }

    private fun lowerTags(tags: Set<String>?): Set<String> {
        if (tags == null || tags.isEmpty()) return Collections.emptySet()
        val result = HashSet<String>()
        for (tag in tags) result.add(tag.lowercase(Locale.US))
        return result
    }

    private fun intersects(left: Set<String>?, right: Set<String>?): Boolean {
        if (left == null || right == null) return false
        for (value in left) {
            if (right.contains(value)) return true
        }
        return false
    }

    private fun tags(vararg tags: String): Set<String> {
        val result = HashSet<String>()
        for (tag in tags) {
            result.add(tag)
        }
        return Collections.unmodifiableSet(result)
    }
}
