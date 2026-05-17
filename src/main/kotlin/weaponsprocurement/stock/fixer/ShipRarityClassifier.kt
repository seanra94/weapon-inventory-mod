package weaponsprocurement.stock.fixer

import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.ShipHullSpecAPI
import kotlin.math.max

object ShipRarityClassifier {
    private const val HIGH_HULL_FREQUENCY = 1f
    private const val LOW_HULL_FREQUENCY = 0.35f
    private const val VERY_LOW_HULL_FREQUENCY = 0.15f

    @JvmStatic
    fun classify(
        spec: ShipHullSpecAPI?,
        faction: FactionAPI?,
        market: MarketAPI?,
        submarketId: String?,
        hullFrequency: Float?,
        priority: Boolean,
        maxShipSizeRank: Int,
    ): ShipCatalogRarity {
        if (spec == null) return ShipCatalogRarity.VERY_RARE
        if (!ShipCatalogPolicy.isWithinSizeCap(spec, maxShipSizeRank)) return ShipCatalogRarity.VERY_RARE

        val fleetPoints = max(0, spec.fleetPoints)
        val budget = combatBudgetEnvelope(market, submarketId)
        val frequency = hullFrequency ?: spec.rarity
        val doctrine = faction?.doctrine
        var score = 0

        if (priority) score += 2
        if (frequency >= HIGH_HULL_FREQUENCY) score += 2
        if (frequency <= VERY_LOW_HULL_FREQUENCY) score -= 2 else if (frequency <= LOW_HULL_FREQUENCY) score -= 1

        if (fleetPoints <= budget * 0.45f) score += 2
        else if (fleetPoints <= budget * 0.7f) score += 1
        else if (fleetPoints > budget) score -= 2
        else score -= 1

        val sizeRank = ShipCatalogPolicy.hullSizeRank(spec.hullSize)
        val doctrineSize = doctrine?.shipSize ?: 3
        if (sizeRank <= doctrineSize) score += 1 else score -= 1

        if (spec.isCarrier && (doctrine?.carriers ?: 3) <= 1) score -= 1
        if (spec.isPhase && (doctrine?.phaseShips ?: 3) <= 1) score -= 1
        if (spec.isCivilianNonCarrier) score += 1

        return when {
            score >= 4 -> ShipCatalogRarity.COMMON
            score >= 2 -> ShipCatalogRarity.UNCOMMON
            score >= 0 -> ShipCatalogRarity.RARE
            else -> ShipCatalogRarity.VERY_RARE
        }
    }

    @JvmStatic
    fun combatBudgetEnvelope(market: MarketAPI?, submarketId: String?): Float {
        if (com.fs.starfarer.api.impl.campaign.ids.Submarkets.GENERIC_MILITARY == submarketId) {
            return 200f * normalizedStability(market)
        }
        if (com.fs.starfarer.api.impl.campaign.ids.Submarkets.SUBMARKET_BLACK == submarketId) {
            val stability = normalizedStability(market)
            return 70f * (0.5f + (1f - stability) * 0.5f)
        }
        return 40f
    }

    @JvmStatic
    fun observedOnly(item: ObservedShipStockIndex.ObservedShip?): ShipCatalogRarity? {
        return if (item != null && item.isOnlyUnsupportedCustomSubmarket) {
            ShipCatalogRarity.UNKNOWN_CUSTOM_SUBMARKET
        } else {
            null
        }
    }

    private fun normalizedStability(market: MarketAPI?): Float {
        val value = market?.stabilityValue ?: 5f
        return (value.coerceIn(0f, 10f)) / 10f
    }
}
