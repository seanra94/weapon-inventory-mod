package weaponsprocurement.stock.fixer

import com.fs.starfarer.api.campaign.SectorAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import weaponsprocurement.stock.market.MarketStockService
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap

class ObservedShipStockIndex {
    fun collect(sector: SectorAPI?): Map<String, ObservedShip> {
        val markets = sector?.economy?.marketsCopy
        if (markets == null || markets.isEmpty()) return emptyMap()

        val result = HashMap<String, ObservedShip>()
        for (market in markets) {
            val submarkets = market?.submarketsCopy ?: continue
            for (submarket in submarkets) {
                if (!MarketStockService.isTradeSubmarket(submarket, true)) continue
                addObservedShips(result, market, submarket)
            }
        }
        return Collections.unmodifiableMap(result)
    }

    private fun addObservedShips(
        result: MutableMap<String, ObservedShip>,
        market: MarketAPI,
        submarket: SubmarketAPI,
    ) {
        val members = submarket.cargoNullOk?.mothballedShips?.membersListCopy ?: return
        for (member in members) {
            val source = sourceFor(market, submarket, member) ?: continue
            val current = result[source.hullId]
            if (current == null) {
                result[source.hullId] = ObservedShip(source, ShipCatalogPolicy.isVanillaSupportedSubmarket(submarket.specId))
            } else {
                current.observe(source, ShipCatalogPolicy.isVanillaSupportedSubmarket(submarket.specId))
            }
        }
    }

    private fun sourceFor(
        market: MarketAPI,
        submarket: SubmarketAPI,
        member: FleetMemberAPI?,
    ): ObservedShipSource? {
        val spec = member?.hullSpec ?: ShipCatalogPolicy.hullSpec(member?.hullId) ?: return null
        if (!ShipCatalogPolicy.isSafeHull(spec)) return null
        val hullId = spec.hullId ?: return null
        val displayName = spec.nameWithDesignationWithDashClass ?: spec.hullName ?: hullId
        return ObservedShipSource(
            hullId,
            displayName,
            market.id,
            market.name,
            submarket.specId,
            submarket.nameOneLine,
            referencePrice(member, spec.baseValue),
            Math.max(0, member?.fleetPointCost ?: spec.fleetPoints),
            ShipCatalogPolicy.hullSizeLabel(spec.hullSize),
        )
    }

    class ObservedShip internal constructor(
        initialSource: ObservedShipSource,
        vanillaSupported: Boolean,
    ) {
        private val sources = ArrayList<ObservedShipSource>()
        private var cheapest: ObservedShipSource? = null
        private var vanillaSupportedSeen = false
        private var unsupportedSeen = false

        init {
            observe(initialSource, vanillaSupported)
        }

        fun observe(source: ObservedShipSource, vanillaSupported: Boolean) {
            sources.add(source)
            if (vanillaSupported) {
                vanillaSupportedSeen = true
            } else {
                unsupportedSeen = true
            }
            val currentCheapest = cheapest
            if (currentCheapest == null || source.baseUnitPrice < currentCheapest.baseUnitPrice) {
                cheapest = source
            }
        }

        val cheapestReferenceSource: ObservedShipSource?
            get() = cheapest

        val observedSources: List<ObservedShipSource>
            get() = Collections.unmodifiableList(sources)

        val totalObservedCount: Int
            get() = sources.size

        val isOnlyUnsupportedCustomSubmarket: Boolean
            get() = unsupportedSeen && !vanillaSupportedSeen
    }

    class ObservedShipSource internal constructor(
        val hullId: String,
        val displayName: String,
        val marketId: String?,
        val marketName: String?,
        val submarketId: String?,
        val submarketName: String?,
        val baseUnitPrice: Int,
        val fleetPoints: Int,
        val hullSizeLabel: String,
    )

    companion object {
        private fun referencePrice(member: FleetMemberAPI?, fallback: Float): Int {
            val price = member?.baseBuyValue ?: fallback
            if (price.isNaN() || price.isInfinite()) return 0
            return Math.max(0, Math.round(price))
        }
    }
}
