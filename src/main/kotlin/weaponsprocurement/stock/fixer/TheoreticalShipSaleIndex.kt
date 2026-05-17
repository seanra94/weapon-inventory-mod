package weaponsprocurement.stock.fixer

import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.SectorAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap

class TheoreticalShipSaleIndex {
    fun collect(sector: SectorAPI?): Map<String, Candidate> {
        val markets = sector?.economy?.marketsCopy
        if (markets == null || markets.isEmpty()) return emptyMap()

        val result = HashMap<String, Candidate>()
        val factionsById = HashMap<String, FactionAPI?>()
        for (market in markets) {
            val submarkets = market?.submarketsCopy ?: continue
            for (submarket in submarkets) {
                if (!ShipCatalogPolicy.isVanillaSupportedSubmarket(submarket.specId)) continue
                addMarketFactionCandidates(result, sector, market, submarket, factionsById)
            }
        }
        return Collections.unmodifiableMap(result)
    }

    private fun addMarketFactionCandidates(
        result: MutableMap<String, Candidate>,
        sector: SectorAPI?,
        market: MarketAPI,
        submarket: SubmarketAPI,
        factionsById: MutableMap<String, FactionAPI?>,
    ) {
        val faction = faction(sector, market.factionId, factionsById) ?: return
        val hullIds = faction.knownShips
        if (hullIds == null || hullIds.isEmpty()) return
        val maxSizeRank = ShipCatalogPolicy.maxShipSizeRank(submarket.specId, market.factionId, submarket.faction?.id)
        val hullFrequency = faction.hullFrequency
        val priorityShips = faction.priorityShips
        for (hullId in hullIds) {
            val spec = ShipCatalogPolicy.hullSpec(hullId) ?: continue
            if (!ShipCatalogPolicy.isSafeHull(spec)) continue
            if (!ShipCatalogPolicy.isWithinSizeCap(spec, maxSizeRank)) continue
            val candidate = Candidate.create(
                spec.hullId,
                spec.nameWithDesignationWithDashClass ?: spec.hullName ?: spec.hullId,
                market.id,
                market.name,
                market.factionId,
                submarket.specId,
                submarket.nameOneLine,
                Math.max(0, Math.round(spec.baseValue)),
                Math.max(0, spec.fleetPoints),
                ShipCatalogPolicy.hullSizeLabel(spec.hullSize),
                hullFrequency?.get(spec.hullId) ?: hullFrequency?.get(spec.baseHullId),
                priorityShips?.contains(spec.hullId) == true || priorityShips?.contains(spec.baseHullId) == true,
                maxSizeRank,
                ShipRarityClassifier.combatBudgetEnvelope(market, submarket.specId),
                ShipRarityClassifier.classify(
                    spec,
                    faction,
                    market,
                    submarket.specId,
                    hullFrequency?.get(spec.hullId) ?: hullFrequency?.get(spec.baseHullId),
                    priorityShips?.contains(spec.hullId) == true || priorityShips?.contains(spec.baseHullId) == true,
                    maxSizeRank,
                ),
            )
            val current = result[candidate.hullId]
            if (current == null || candidate.compareTo(current) < 0) {
                result[candidate.hullId] = candidate
            }
        }
    }

    class Candidate private constructor(
        val hullId: String,
        val displayName: String,
        val marketId: String?,
        val marketName: String?,
        val factionId: String?,
        val submarketId: String?,
        val submarketName: String?,
        val baseUnitPrice: Int,
        val fleetPoints: Int,
        val hullSizeLabel: String,
        val hullFrequency: Float?,
        val priority: Boolean,
        val maxShipSizeRank: Int,
        val combatBudgetEstimate: Float,
        val rarity: ShipCatalogRarity,
    ) : Comparable<Candidate> {
        override fun compareTo(other: Candidate): Int {
            var result = rarityRank(rarity).compareTo(rarityRank(other.rarity))
            if (result != 0) return result
            result = -priority.compareTo(other.priority)
            if (result != 0) return result
            result = fleetPoints.compareTo(other.fleetPoints)
            if (result != 0) return result
            result = -(hullFrequency ?: 0f).compareTo(other.hullFrequency ?: 0f)
            if (result != 0) return result
            return displayName.compareTo(other.displayName, ignoreCase = true)
        }

        companion object {
            fun create(
                hullId: String?,
                displayName: String?,
                marketId: String?,
                marketName: String?,
                factionId: String?,
                submarketId: String?,
                submarketName: String?,
                baseUnitPrice: Int,
                fleetPoints: Int,
                hullSizeLabel: String,
                hullFrequency: Float?,
                priority: Boolean,
                maxShipSizeRank: Int,
                combatBudgetEstimate: Float,
                rarity: ShipCatalogRarity,
            ): Candidate {
                return Candidate(
                    hullId ?: "",
                    displayName ?: hullId ?: "",
                    marketId,
                    marketName,
                    factionId,
                    submarketId,
                    submarketName,
                    Math.max(0, baseUnitPrice),
                    Math.max(0, fleetPoints),
                    hullSizeLabel,
                    hullFrequency,
                    priority,
                    maxShipSizeRank,
                    combatBudgetEstimate,
                    rarity,
                )
            }

            private fun rarityRank(rarity: ShipCatalogRarity?): Int {
                return when (rarity) {
                    ShipCatalogRarity.COMMON -> 0
                    ShipCatalogRarity.UNCOMMON -> 1
                    ShipCatalogRarity.RARE -> 2
                    ShipCatalogRarity.VERY_RARE -> 3
                    ShipCatalogRarity.UNKNOWN_CUSTOM_SUBMARKET -> 4
                    null -> 5
                }
            }
        }
    }

    companion object {
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

        private fun safeFaction(sector: SectorAPI?, factionId: String?): FactionAPI? {
            return try {
                if (sector == null || factionId == null) null else sector.getFaction(factionId)
            } catch (_: RuntimeException) {
                null
            }
        }

        @JvmStatic
        fun sortedCandidates(candidates: Map<String, Candidate>?): List<Candidate> {
            if (candidates == null || candidates.isEmpty()) return Collections.emptyList()
            val result = ArrayList(candidates.values)
            Collections.sort(result)
            return result
        }
    }
}
