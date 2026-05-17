package weaponsprocurement.stock.fixer

import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.SectorAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Submarkets
import weaponsprocurement.config.WeaponMarketBlacklist
import weaponsprocurement.stock.item.StockItemSpecs
import weaponsprocurement.stock.item.StockItemStacks
import weaponsprocurement.stock.item.StockItemType
import java.util.Collections
import java.util.HashMap
import java.util.HashSet
import kotlin.math.max

class TheoreticalSaleIndex {
    fun collect(sector: SectorAPI?, blacklist: WeaponMarketBlacklist?): Map<String, Candidate> {
        val economy = sector?.economy
        val markets = economy?.marketsCopy
        if (markets == null || markets.isEmpty()) return emptyMap()

        val result = HashMap<String, Candidate>()
        val processed = HashSet<String>()
        val factionsById = HashMap<String, FactionAPI?>()
        for (market in markets) {
            if (market == null || market.submarketsCopy == null) continue
            for (submarket in market.submarketsCopy) {
                val tierCap = tierCap(submarket)
                if (tierCap < 0) continue
                val militarySubmarket = Submarkets.GENERIC_MILITARY == submarket.specId
                val factionIds = relevantFactionIds(market, submarket)
                for (factionId in factionIds) {
                    val scanKey = "$factionId|$tierCap|$militarySubmarket"
                    if (!processed.add(scanKey)) continue
                    val faction = faction(sector, factionId, factionsById)
                    addFactionWeapons(result, faction, tierCap, militarySubmarket, blacklist)
                    addFactionWings(result, faction, tierCap, militarySubmarket, blacklist)
                }
            }
        }
        return Collections.unmodifiableMap(result)
    }

    class Candidate private constructor(
        val itemKey: String,
        private val tier: Int,
        private val sellFrequency: Float?,
        val baseUnitPrice: Int,
        val unitCargoSpace: Float,
        val rarity: FixerRarity,
    ) : Comparable<Candidate> {
        override fun compareTo(other: Candidate): Int {
            var result = tier.compareTo(other.tier)
            if (result != 0) return result
            val leftFrequency = sellFrequency ?: 0f
            val rightFrequency = other.sellFrequency ?: 0f
            result = -leftFrequency.compareTo(rightFrequency)
            if (result != 0) return result
            return itemKey.compareTo(other.itemKey)
        }

        companion object {
            fun create(
                itemKey: String,
                tier: Int,
                sellFrequency: Float?,
                baseUnitPrice: Int,
                unitCargoSpace: Float,
                rarity: FixerRarity,
            ): Candidate {
                return Candidate(itemKey, tier, sellFrequency, baseUnitPrice, unitCargoSpace, rarity)
            }
        }
    }

    companion object {
        private const val OPEN_TIER_CAP = 0
        private const val MILITARY_TIER_CAP = 3
        private const val BLACK_TIER_CAP = 3

        private fun addFactionWeapons(
            result: MutableMap<String, Candidate>,
            faction: FactionAPI?,
            tierCap: Int,
            militarySubmarket: Boolean,
            blacklist: WeaponMarketBlacklist?,
        ) {
            if (faction == null) return
            val sellFrequency = faction.weaponSellFrequency
            val ids = candidateIds(faction.knownWeapons, sellFrequency)
            for (weaponId in ids) {
                val spec = StockItemSpecs.weaponSpec(weaponId)
                if (spec == null || spec.tier > tierCap) continue
                val itemKey = StockItemType.WEAPON.key(weaponId)
                if (!FixerCatalogPolicy.isEligibleTheoreticalItem(itemKey, blacklist, militarySubmarket)) continue
                val frequency = frequency(sellFrequency, weaponId)
                if (frequency != null && frequency <= 0f) continue
                addCandidate(
                    result,
                    itemKey,
                    spec.tier,
                    frequency,
                    referenceBaseUnitPrice(StockItemType.WEAPON, weaponId, max(0, Math.round(spec.baseValue.toFloat()))),
                    StockItemStacks.referenceUnitCargoSpace(StockItemType.WEAPON, weaponId),
                )
            }
        }

        private fun addFactionWings(
            result: MutableMap<String, Candidate>,
            faction: FactionAPI?,
            tierCap: Int,
            militarySubmarket: Boolean,
            blacklist: WeaponMarketBlacklist?,
        ) {
            if (faction == null) return
            val sellFrequency = faction.fighterSellFrequency
            val ids = candidateIds(faction.knownFighters, sellFrequency)
            for (wingId in ids) {
                val spec = StockItemSpecs.wingSpec(wingId)
                if (spec == null || spec.tier > tierCap) continue
                val itemKey = StockItemType.WING.key(wingId)
                if (!FixerCatalogPolicy.isEligibleTheoreticalItem(itemKey, blacklist, militarySubmarket)) continue
                val frequency = frequency(sellFrequency, wingId)
                if (frequency != null && frequency <= 0f) continue
                addCandidate(
                    result,
                    itemKey,
                    spec.tier,
                    frequency,
                    referenceBaseUnitPrice(StockItemType.WING, wingId, max(0, Math.round(spec.baseValue.toFloat()))),
                    StockItemStacks.referenceUnitCargoSpace(StockItemType.WING, wingId),
                )
            }
        }

        private fun addCandidate(
            result: MutableMap<String, Candidate>,
            itemKey: String,
            tier: Int,
            sellFrequency: Float?,
            baseUnitPrice: Int,
            unitCargoSpace: Float,
        ) {
            val candidate = Candidate.create(
                itemKey,
                tier,
                sellFrequency,
                baseUnitPrice,
                unitCargoSpace,
                RarityClassifier.classify(tier, sellFrequency),
            )
            val current = result[itemKey]
            if (current == null || candidate.compareTo(current) < 0) {
                result[itemKey] = candidate
            }
        }

        private fun relevantFactionIds(market: MarketAPI?, submarket: SubmarketAPI?): Set<String> {
            val result = HashSet<String>()
            val id = submarket?.specId
            if (Submarkets.SUBMARKET_OPEN == id ||
                Submarkets.GENERIC_MILITARY == id ||
                Submarkets.SUBMARKET_BLACK == id
            ) {
                addFactionId(result, market?.factionId)
            }
            return result
        }

        private fun addFactionId(result: MutableSet<String>, factionId: String?) {
            if (factionId != null && factionId.trim().isNotEmpty()) {
                result.add(factionId)
            }
        }

        private fun tierCap(submarket: SubmarketAPI?): Int {
            if (submarket == null) return -1
            val id = submarket.specId
            if (Submarkets.SUBMARKET_OPEN == id) return OPEN_TIER_CAP
            if (Submarkets.GENERIC_MILITARY == id) return MILITARY_TIER_CAP
            if (Submarkets.SUBMARKET_BLACK == id) return BLACK_TIER_CAP
            return -1
        }

        private fun safeFaction(sector: SectorAPI?, factionId: String?): FactionAPI? {
            return try {
                if (sector == null || factionId == null) null else sector.getFaction(factionId)
            } catch (_: RuntimeException) {
                null
            }
        }

        private fun faction(
            sector: SectorAPI?,
            factionId: String?,
            factionsById: MutableMap<String, FactionAPI?>,
        ): FactionAPI? {
            if (factionId.isNullOrBlank()) return null
            if (factionsById.containsKey(factionId)) return factionsById[factionId]
            val faction = safeFaction(sector, factionId)
            factionsById[factionId] = faction
            return faction
        }

        private fun frequency(frequencies: Map<String, Float>?, itemId: String): Float? {
            return frequencies?.get(itemId)
        }

        private fun referenceBaseUnitPrice(itemType: StockItemType, itemId: String, fallback: Int): Int {
            val stackPrice = StockItemStacks.referenceBaseUnitPrice(itemType, itemId)
            return if (stackPrice > 0) stackPrice else fallback
        }

        private fun candidateIds(knownIds: Set<String>?, sellFrequency: Map<String, Float>?): Set<String> {
            if (sellFrequency != null && sellFrequency.isNotEmpty()) {
                return sellFrequency.keys
            }
            if (knownIds == null || knownIds.isEmpty()) return Collections.emptySet()
            return knownIds
        }

    }
}
