package weaponsprocurement.stock.fixer

import com.fs.starfarer.api.campaign.SectorAPI
import org.apache.log4j.Logger
import java.util.ArrayList
import java.util.EnumMap
import java.util.HashSet
import java.util.Locale

class ShipCatalogDiagnostics {
    private val observedIndex = ObservedShipStockIndex()
    private val theoreticalIndex = TheoreticalShipSaleIndex()

    fun dump(sector: SectorAPI?, rawSpec: String?, log: Logger) {
        val request = DiagnosticRequest.parse(rawSpec) ?: return
        if (sector?.economy == null) {
            log.info("WP_SHIP_CATALOG_DIAG SKIP reason=no_sector_or_economy")
            return
        }

        val observed = observedIndex.collect(sector)
        val theoretical = theoreticalIndex.collect(sector)
        logSummary(log, observed, theoretical)
        logObservedOnly(log, observed, theoretical)

        if (request.logTopCandidates) {
            logTopCandidates(log, theoretical)
        }
        if (request.targets.isNotEmpty()) {
            logTargets(log, request.targets, observed, theoretical)
        }
    }

    private fun logSummary(
        log: Logger,
        observed: Map<String, ObservedShipStockIndex.ObservedShip>,
        theoretical: Map<String, TheoreticalShipSaleIndex.Candidate>,
    ) {
        val counts = EnumMap<ShipCatalogRarity, Int>(ShipCatalogRarity::class.java)
        for (candidate in theoretical.values) {
            counts[candidate.rarity] = (counts[candidate.rarity] ?: 0) + 1
        }
        log.info(
            "WP_SHIP_CATALOG_DIAG PASS summary" +
                " observedHullTypes=${observed.size}" +
                " theoreticalHullTypes=${theoretical.size}" +
                " common=${counts[ShipCatalogRarity.COMMON] ?: 0}" +
                " uncommon=${counts[ShipCatalogRarity.UNCOMMON] ?: 0}" +
                " rare=${counts[ShipCatalogRarity.RARE] ?: 0}" +
                " veryRare=${counts[ShipCatalogRarity.VERY_RARE] ?: 0}",
        )
    }

    private fun logObservedOnly(
        log: Logger,
        observed: Map<String, ObservedShipStockIndex.ObservedShip>,
        theoretical: Map<String, TheoreticalShipSaleIndex.Candidate>,
    ) {
        var unsupportedOnly = 0
        val missingTheoretical = ArrayList<String>()
        for ((hullId, item) in observed) {
            if (item.isOnlyUnsupportedCustomSubmarket) unsupportedOnly++
            if (!theoretical.containsKey(hullId) && missingTheoretical.size < MAX_OBSERVED_ONLY_LOGS) {
                val source = item.cheapestReferenceSource
                missingTheoretical.add(
                    "$hullId@${source?.marketId ?: "?"}/${source?.submarketId ?: "?"}",
                )
            }
        }
        log.info(
            "WP_SHIP_CATALOG_DIAG observedComparison" +
                " observedOnlySample=${missingTheoretical.joinToString(",").ifEmpty { "none" }}" +
                " unsupportedOnly=$unsupportedOnly",
        )
    }

    private fun logTopCandidates(
        log: Logger,
        theoretical: Map<String, TheoreticalShipSaleIndex.Candidate>,
    ) {
        val sorted = TheoreticalShipSaleIndex.sortedCandidates(theoretical)
        val limit = Math.min(MAX_TOP_CANDIDATES, sorted.size)
        for (i in 0 until limit) {
            logCandidate(log, "top[$i]", sorted[i])
        }
        if (sorted.size > limit) {
            log.info("WP_SHIP_CATALOG_DIAG top truncated remaining=${sorted.size - limit}")
        }
    }

    private fun logTargets(
        log: Logger,
        targets: Set<String>,
        observed: Map<String, ObservedShipStockIndex.ObservedShip>,
        theoretical: Map<String, TheoreticalShipSaleIndex.Candidate>,
    ) {
        for (target in targets) {
            val candidate = theoretical[target]
            val observedShip = observed[target]
            if (candidate == null && observedShip == null) {
                log.info("WP_SHIP_CATALOG_DIAG target=$target status=missing")
                continue
            }
            if (candidate != null) {
                logCandidate(log, "target=$target theoretical", candidate)
            }
            if (observedShip != null) {
                val source = observedShip.cheapestReferenceSource
                log.info(
                    "WP_SHIP_CATALOG_DIAG target=$target observed" +
                        " count=${observedShip.totalObservedCount}" +
                        " reference=${source?.marketId ?: "?"}/${source?.submarketId ?: "?"}" +
                        " price=${source?.baseUnitPrice ?: 0}" +
                        " unsupportedOnly=${observedShip.isOnlyUnsupportedCustomSubmarket}",
                )
            }
        }
    }

    private fun logCandidate(log: Logger, prefix: String, candidate: TheoreticalShipSaleIndex.Candidate) {
        log.info(
            "WP_SHIP_CATALOG_DIAG $prefix" +
                " hull=${candidate.hullId}" +
                " name=\"${candidate.displayName}\"" +
                " rarity=${candidate.rarity.label}" +
                " faction=${candidate.factionId ?: "?"}" +
                " market=${candidate.marketId ?: "?"}" +
                " submarket=${candidate.submarketId ?: "?"}" +
                " fp=${candidate.fleetPoints}" +
                " size=${candidate.hullSizeLabel}" +
                " frequency=${candidate.hullFrequency ?: "unknown"}" +
                " priority=${candidate.priority}" +
                " budget=${String.format(Locale.US, "%.1f", candidate.combatBudgetEstimate)}" +
                " price=${candidate.baseUnitPrice}",
        )
    }

    private class DiagnosticRequest private constructor(
        val logTopCandidates: Boolean,
        val targets: Set<String>,
    ) {
        companion object {
            fun parse(rawSpec: String?): DiagnosticRequest? {
                val spec = rawSpec?.trim() ?: ""
                if (spec.isEmpty() || spec.equals("false", ignoreCase = true) || spec.equals("none", ignoreCase = true)) {
                    return null
                }
                if (spec.equals("true", ignoreCase = true) || spec.equals("summary", ignoreCase = true)) {
                    return DiagnosticRequest(false, emptySet())
                }
                if (spec.equals("all", ignoreCase = true) || spec.equals("top", ignoreCase = true)) {
                    return DiagnosticRequest(true, emptySet())
                }
                val targets = HashSet<String>()
                for (token in spec.split(',', ';', ' ')) {
                    val target = token.trim()
                    if (target.isNotEmpty()) targets.add(target)
                }
                return DiagnosticRequest(false, targets)
            }
        }
    }

    companion object {
        private const val MAX_TOP_CANDIDATES = 25
        private const val MAX_OBSERVED_ONLY_LOGS = 12
    }
}
