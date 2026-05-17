package weaponsprocurement.trade.execution

import weaponsprocurement.stock.item.StockItemCargo
import weaponsprocurement.stock.item.StockItemType
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import org.apache.log4j.Logger
import weaponsprocurement.config.WeaponsProcurementConfig
import weaponsprocurement.trade.plan.StockPurchasePlan
import weaponsprocurement.trade.plan.StockPurchaseSource
import weaponsprocurement.trade.plan.StockSellTarget
import weaponsprocurement.trade.plan.TradeMoney
import weaponsprocurement.trade.quote.CreditFormat
import java.util.ArrayList
import java.util.IdentityHashMap

class StockPurchaseExecutor private constructor() {
    companion object {
        private const val FAIL_AFTER_SOURCE_REMOVAL = "after-source-removal"
        private const val FAIL_AFTER_PLAYER_CARGO_REMOVE = "after-player-cargo-remove"
        private const val FAIL_AFTER_PLAYER_CARGO_ADD = "after-player-cargo-add"
        private const val FAIL_AFTER_TARGET_CARGO_ADD = "after-target-cargo-add"
        private const val FAIL_AFTER_CREDIT_MUTATION = "after-credit-mutation"

        @JvmStatic
        fun sellToMarket(
            log: Logger,
            market: MarketAPI?,
            playerCargo: CargoAPI,
            target: StockSellTarget,
            itemType: StockItemType,
            itemId: String,
            quantity: Int,
        ): StockPurchaseService.PurchaseResult {
            val credits = TradeMoney.lineTotal(target.unitPrice, quantity)
            val expectedMarketCount = StockItemCargo.itemCount(target.cargo, itemType, itemId) + quantity
            val journal = MutationJournal(playerCargo, itemType, itemId)
            journal.recordCargo(playerCargo, "player cargo")
            journal.recordCargo(target.cargo, "sell target " + marketLabel(market, target.submarket))
            try {
                val validation = StockPurchaseChecks.canMutateCredits(credits)
                if (validation != null) return validation
                if (StockItemCargo.itemCount(playerCargo, itemType, itemId) < quantity) {
                    return StockPurchaseService.PurchaseResult.failure("No player-cargo stock is available to sell.")
                }
                StockItemCargo.removeItem(playerCargo, itemType, itemId, quantity)
                maybeFail(FAIL_AFTER_PLAYER_CARGO_REMOVE)
                playerCargo.credits.add(credits.toFloat())
                maybeFail(FAIL_AFTER_CREDIT_MUTATION)
                StockItemCargo.tidyCargo(playerCargo)
                StockItemCargo.addItem(target.cargo, itemType, itemId, quantity)
                maybeFail(FAIL_AFTER_TARGET_CARGO_ADD)
                StockItemCargo.tidyCargo(target.cargo)
                StockMarketTransactionReporter.reportItemTransaction(
                    log,
                    market,
                    target.submarket,
                    itemType,
                    itemId,
                    quantity,
                    target.unitPrice,
                    false,
                )
                reconcileAfterTransactionReport(
                    log,
                    target.cargo,
                    itemType,
                    itemId,
                    expectedMarketCount,
                    "sell target " + marketLabel(market, target.submarket),
                )

                val message = "Sold " + quantity + " " + StockItemCargo.itemDisplayName(itemType, itemId) +
                    " for " + CreditFormat.creditsLong(credits) + "."
                StockPurchaseChecks.addCampaignMessage(message)
                return StockPurchaseService.PurchaseResult.success(message, quantity, -credits)
            } catch (t: Throwable) {
                return executionFailure(log, "sell to market", itemType, itemId, quantity, t, journal)
            }
        }

        @JvmStatic
        fun buyFromFixersMarket(
            log: Logger,
            playerCargo: CargoAPI,
            itemType: StockItemType,
            itemId: String,
            quantity: Int,
            totalCost: Long,
        ): StockPurchaseService.PurchaseResult {
            val journal = MutationJournal(playerCargo, itemType, itemId)
            journal.recordCargo(playerCargo, "player cargo")
            try {
                StockItemCargo.addItem(playerCargo, itemType, itemId, quantity)
                maybeFail(FAIL_AFTER_PLAYER_CARGO_ADD)
                playerCargo.credits.subtract(totalCost.toFloat())
                maybeFail(FAIL_AFTER_CREDIT_MUTATION)
                StockItemCargo.tidyCargo(playerCargo)

                val message = "Bought " + quantity + " " + StockItemCargo.itemDisplayName(itemType, itemId) +
                    " from the fixer's market for " + CreditFormat.creditsLong(totalCost) + "."
                StockPurchaseChecks.addCampaignMessage(message)
                return StockPurchaseService.PurchaseResult.success(message, quantity, totalCost)
            } catch (t: Throwable) {
                return executionFailure(log, "buy from fixer's market", itemType, itemId, quantity, t, journal)
            }
        }

        @JvmStatic
        fun buyPlan(
            log: Logger,
            playerCargo: CargoAPI,
            fallbackMarket: MarketAPI?,
            itemType: StockItemType,
            itemId: String,
            plan: StockPurchasePlan?,
            sourceLabel: String,
            operation: String,
        ): StockPurchaseService.PurchaseResult {
            val journal = MutationJournal(playerCargo, itemType, itemId)
            val reportLines = ArrayList<TransactionReportLine>()
            try {
                val validation = buyPlanStillAvailable(plan, itemType, itemId)
                if (validation != null) return validation
                val checkedPlan = plan ?: return StockPurchaseService.PurchaseResult.failure("No purchasable stock is available.")

                journal.recordCargo(playerCargo, "player cargo")
                for (line in checkedPlan.lines) {
                    journal.recordCargo(line.source.cargo, "buy source " + sourceLabel(line.source, fallbackMarket))
                }
                for (line in checkedPlan.lines) {
                    val sourceCargo = line.source.cargo
                        ?: return StockPurchaseService.PurchaseResult.failure("Trade source is no longer available.")
                    StockItemCargo.removeItem(sourceCargo, itemType, itemId, line.quantity)
                    maybeFail(FAIL_AFTER_SOURCE_REMOVAL)
                    StockItemCargo.tidyCargo(sourceCargo)
                    val reportMarket = line.source.market ?: fallbackMarket
                    reportLines.add(
                        TransactionReportLine(
                            reportMarket,
                            line.source.submarket,
                            itemType,
                            itemId,
                            line.quantity,
                            line.source.unitPrice,
                            true,
                        )
                    )
                }
                StockItemCargo.addItem(playerCargo, itemType, itemId, checkedPlan.totalQuantity)
                maybeFail(FAIL_AFTER_PLAYER_CARGO_ADD)
                playerCargo.credits.subtract(checkedPlan.totalCost.toFloat())
                maybeFail(FAIL_AFTER_CREDIT_MUTATION)
                StockItemCargo.tidyCargo(playerCargo)
                flushTransactionReports(log, reportLines)

                val message = "Bought " + checkedPlan.totalQuantity + " " + StockItemCargo.itemDisplayName(itemType, itemId) +
                    sourceLabel + " for " + CreditFormat.creditsLong(checkedPlan.totalCost) + "."
                StockPurchaseChecks.addCampaignMessage(message)
                return StockPurchaseService.PurchaseResult.success(message, checkedPlan.totalQuantity, checkedPlan.totalCost)
            } catch (t: Throwable) {
                val totalQuantity = plan?.totalQuantity ?: 0
                return executionFailure(log, operation, itemType, itemId, totalQuantity, t, journal)
            }
        }

        private fun buyPlanStillAvailable(
            plan: StockPurchasePlan?,
            itemType: StockItemType,
            itemId: String,
        ): StockPurchaseService.PurchaseResult? {
            if (plan == null || plan.lines.isEmpty()) {
                return StockPurchaseService.PurchaseResult.failure("No purchasable stock is available.")
            }
            for (line in plan.lines) {
                if (line.source.cargo == null || line.quantity <= 0) {
                    return StockPurchaseService.PurchaseResult.failure("Trade source is no longer available.")
                }
                val available = StockItemCargo.itemCount(line.source.cargo, itemType, itemId)
                if (available < line.quantity) {
                    return StockPurchaseService.PurchaseResult.failure(
                        "Market stock changed before confirmation. Reopen the review and try again."
                    )
                }
            }
            return null
        }

        private fun flushTransactionReports(log: Logger, reportLines: List<TransactionReportLine>) {
            for (reportLine in reportLines) reportLine.report(log)
        }

        private fun reconcileAfterTransactionReport(
            log: Logger,
            cargo: CargoAPI,
            itemType: StockItemType,
            itemId: String,
            expectedCount: Int,
            label: String,
        ) {
            try {
                StockItemCargo.reconcileItemCount(cargo, itemType, itemId, expectedCount)
            } catch (t: Throwable) {
                log.warn(
                    "WP_STOCK_REVIEW post-report cargo reconciliation failed item=" +
                        itemType.key(itemId) + " target=" + label + " expectedCount=" + expectedCount,
                    t,
                )
            }
        }

        private fun maybeFail(step: String) {
            val requested = System.getProperty(WeaponsProcurementConfig.KEY_DEBUG_TRADE_FAILURE_STEP, "")
            if (step.equals(requested, ignoreCase = true) || "*" == requested) {
                throw RuntimeException("WP debug forced trade failure at $step")
            }
        }

        private fun executionFailure(
            log: Logger,
            operation: String,
            itemType: StockItemType,
            itemId: String,
            quantity: Int,
            t: Throwable,
            journal: MutationJournal?,
        ): StockPurchaseService.PurchaseResult {
            val rollback = journal?.rollback(itemType, itemId) ?: "rollback=none"
            log.error(
                "WP_STOCK_REVIEW trade execution failed operation=" + operation +
                    " item=" + itemType.key(itemId) +
                    " quantity=" + quantity +
                    " " + rollback,
                t,
            )
            return StockPurchaseService.PurchaseResult.failure("Trade failed during execution. Check starsector.log.")
        }

        private fun sourceLabel(source: StockPurchaseSource?, fallbackMarket: MarketAPI?): String {
            if (source == null) return "unknown source"
            val market = source.market ?: fallbackMarket
            return marketLabel(market, source.submarket)
        }

        private fun marketLabel(market: MarketAPI?, submarket: SubmarketAPI?): String {
            val result = StringBuilder()
            if (market == null) {
                result.append("unknown market")
            } else {
                result.append(safe(market.name)).append("/").append(safe(market.id))
            }
            result.append(" ")
            if (submarket == null) {
                result.append("unknown submarket")
            } else {
                result.append(safe(submarket.nameOneLine)).append("/").append(safe(submarket.specId))
            }
            return result.toString()
        }

        private fun safe(value: String?): String {
            return if (value.isNullOrEmpty()) "?" else value
        }
    }

    private class MutationJournal(
        private val playerCargo: CargoAPI?,
        private val itemType: StockItemType,
        private val itemId: String,
    ) {
        private val creditsBefore: Float = playerCargo?.credits?.get() ?: 0f
        private val snapshots = ArrayList<CargoSnapshot>()
        private val snapshotsByCargo = IdentityHashMap<CargoAPI, CargoSnapshot>()

        fun recordCargo(cargo: CargoAPI?, label: String) {
            if (cargo == null || snapshotsByCargo.containsKey(cargo)) return
            val snapshot = CargoSnapshot(cargo, itemType, itemId, label)
            snapshotsByCargo[cargo] = snapshot
            snapshots.add(snapshot)
        }

        fun rollback(itemType: StockItemType, itemId: String): String {
            var restored = 0
            var failed = 0
            for (snapshot in snapshots) {
                try {
                    snapshot.itemCountAtFailure = StockItemCargo.itemCount(snapshot.cargo, itemType, itemId)
                    StockItemCargo.reconcileItemCount(snapshot.cargo, itemType, itemId, snapshot.itemCountBefore)
                    snapshot.itemCountAfterRollback = StockItemCargo.itemCount(snapshot.cargo, itemType, itemId)
                    restored++
                } catch (_: Throwable) {
                    failed++
                }
            }
            var creditsRestored = false
            try {
                if (playerCargo != null) {
                    playerCargo.credits.set(creditsBefore)
                    creditsRestored = true
                }
            } catch (_: Throwable) {
            }
            return "rollback=attempted restoredCargos=" + restored +
                " failedCargos=" + failed +
                " creditsRestored=" + creditsRestored +
                " touched=" + describe()
        }

        private fun describe(): String {
            val result = StringBuilder()
            for (i in snapshots.indices) {
                if (i > 0) result.append(",")
                val snapshot = snapshots[i]
                result.append(snapshot.itemCountBefore)
                    .append("[")
                    .append(snapshot.label)
                    .append("]")
                    .append("->")
                    .append(if (snapshot.itemCountAtFailure < 0) "?" else snapshot.itemCountAtFailure.toString())
                    .append("->")
                    .append(if (snapshot.itemCountAfterRollback < 0) "?" else snapshot.itemCountAfterRollback.toString())
            }
            return result.toString()
        }
    }

    private class CargoSnapshot(
        val cargo: CargoAPI,
        itemType: StockItemType,
        itemId: String,
        label: String?,
    ) {
        val label: String = if (label == null) "unknown cargo" else label
        val itemCountBefore: Int = StockItemCargo.itemCount(cargo, itemType, itemId)
        var itemCountAtFailure: Int = -1
        var itemCountAfterRollback: Int = -1
    }

    private class TransactionReportLine(
        private val market: MarketAPI?,
        private val submarket: SubmarketAPI?,
        private val itemType: StockItemType,
        private val itemId: String,
        private val quantity: Int,
        private val unitPrice: Int,
        private val buy: Boolean,
    ) {
        fun report(log: Logger) {
            StockMarketTransactionReporter.reportItemTransaction(
                log,
                market,
                submarket,
                itemType,
                itemId,
                quantity,
                unitPrice,
                buy,
            )
        }
    }
}
