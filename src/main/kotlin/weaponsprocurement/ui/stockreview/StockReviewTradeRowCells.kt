package weaponsprocurement.ui.stockreview

import weaponsprocurement.ui.*

import com.fs.starfarer.api.ui.Alignment
import weaponsprocurement.trade.CreditFormat
import java.awt.Color
import kotlin.math.abs

class StockReviewTradeRowCells private constructor() {
    companion object {
        @JvmStatic
        fun storage(ownedCount: Int, planQuantity: Int, width: Float): WimGuiRowCell<StockReviewAction> =
            WimGuiRowCell.info(
                storageLabel(ownedCount, planQuantity),
                width,
                StockReviewStyle.CELL_BACKGROUND,
                StockReviewStyle.TEXT,
                Alignment.LMID,
                StockReviewTooltips.STORAGE,
            )

        @JvmStatic
        fun plan(planQuantity: Int, transactionCost: Long): WimGuiRowCell<StockReviewAction> {
            val quantity = cappedCount(abs(planQuantity))
            val total = cappedCredits(transactionCost, 999999)
            val label = if (planQuantity > 0) {
                "Buying: $quantity [$total]"
            } else if (planQuantity < 0) {
                "Selling: $quantity [$total]"
            } else {
                "Buying: 0 [${StockReviewFormat.credits(0)}]"
            }
            val fill = if (planQuantity > 0) {
                StockReviewStyle.PLAN_POSITIVE
            } else if (planQuantity < 0) {
                StockReviewStyle.PLAN_NEGATIVE
            } else {
                StockReviewStyle.CELL_BACKGROUND
            }
            return WimGuiRowCell.info(label, StockReviewStyle.PLAN_CELL_WIDTH, fill, StockReviewStyle.TEXT, Alignment.LMID, StockReviewTooltips.PLAN)
        }

        @JvmStatic
        fun unitPrice(unitPrice: Int): WimGuiRowCell<StockReviewAction> {
            if (unitPrice == StockReviewQuoteBook.PRICE_UNAVAILABLE) {
                return WimGuiRowCell.info("Price: ?", StockReviewStyle.PRICE_CELL_WIDTH, StockReviewStyle.CELL_BACKGROUND, StockReviewStyle.TEXT, Alignment.LMID, StockReviewTooltips.PRICE)
            }
            return WimGuiRowCell.info("Price: ${cappedCredits(unitPrice.toLong(), 99999)}", StockReviewStyle.PRICE_CELL_WIDTH, StockReviewStyle.CELL_BACKGROUND, StockReviewStyle.TEXT, Alignment.LMID, StockReviewTooltips.PRICE)
        }

        @JvmStatic
        fun step(sign: String, quantity: Int, fill: Color, action: StockReviewAction, tooltip: String): WimGuiRowCell<StockReviewAction> {
            val enabled = quantity > 1
            val label = if (enabled) sign + quantity else sign + "10"
            return WimGuiRowCell.standardAction(label, StockReviewStyle.TRADE_STEP_BUTTON_WIDTH, fill, action, enabled, tooltip)
        }

        @JvmStatic
        fun addWorstCaseTradeRow(rows: MutableList<WimGuiListRow<StockReviewAction>>) {
            val cells = WimGuiRowCell.of(
                WimGuiRowCell.info("Storage: 99+", StockReviewStyle.STOCK_CELL_WIDTH, StockReviewStyle.CELL_BACKGROUND, StockReviewStyle.TEXT, Alignment.LMID, StockReviewTooltips.STORAGE),
                WimGuiRowCell.info("Price: 99,999+${CreditFormat.CREDIT_SYMBOL}", StockReviewStyle.PRICE_CELL_WIDTH, StockReviewStyle.CELL_BACKGROUND, StockReviewStyle.TEXT, Alignment.LMID, StockReviewTooltips.PRICE),
                WimGuiRowCell.info("Selling: 99+ [999,999+${CreditFormat.CREDIT_SYMBOL}]", StockReviewStyle.PLAN_CELL_WIDTH, StockReviewStyle.PLAN_NEGATIVE, StockReviewStyle.TEXT, Alignment.LMID, StockReviewTooltips.PLAN),
                WimGuiRowCell.standardAction("-10", StockReviewStyle.TRADE_STEP_BUTTON_WIDTH, StockReviewStyle.SELL_BUTTON, StockReviewAction.debugNoop(), true, StockReviewTooltips.decreasePlan(10)),
                WimGuiRowCell.standardAction("-1", StockReviewStyle.TRADE_STEP_BUTTON_WIDTH, StockReviewStyle.SELL_BUTTON, StockReviewAction.debugNoop(), true, StockReviewTooltips.decreasePlan(1)),
                WimGuiRowCell.standardAction("+1", StockReviewStyle.TRADE_STEP_BUTTON_WIDTH, StockReviewStyle.BUY_BUTTON, StockReviewAction.debugNoop(), true, StockReviewTooltips.increasePlan(1)),
                WimGuiRowCell.standardAction("+10", StockReviewStyle.TRADE_STEP_BUTTON_WIDTH, StockReviewStyle.BUY_BUTTON, StockReviewAction.debugNoop(), true, StockReviewTooltips.increasePlan(10)),
                WimGuiRowCell.standardAction("Sufficient", StockReviewStyle.SUFFICIENT_BUTTON_WIDTH, StockReviewStyle.SELL_BUTTON, StockReviewAction.debugNoop(), true, "Adjust the queued trade quantity so that your stock of this item just meets the sufficiency threshold (99)."),
                WimGuiRowCell.standardAction("Reset", StockReviewStyle.RESET_BUTTON_WIDTH, StockReviewStyle.ACTION_BACKGROUND, StockReviewAction.debugNoop(), true, StockReviewTooltips.resetPlan()),
            )
            rows.add(
                StockReviewListRow.item(
                    "Suzuki-Clapteryon Thermal Prokector... (+)",
                    cells,
                    StockReviewAction.debugNoop(),
                    "Worst-case row-width test sample. It does not affect trades.",
                    StockReviewStyle.SECTION_INDENT,
                ),
            )
        }

        private fun storageLabel(ownedCount: Int, planQuantity: Int): String {
            if (planQuantity == 0) {
                return "Storage: ${cappedCount(ownedCount)}"
            }
            return "Storage: ${cappedCount(ownedCount)} [${signedCappedCount(planQuantity)}]"
        }

        private fun cappedCount(value: Int): String = if (value >= 99) "99+" else Math.max(0, value).toString()

        private fun signedCappedCount(value: Int): String {
            val sign = if (value > 0) "+" else if (value < 0) "-" else ""
            val absolute = abs(value)
            return sign + if (absolute >= 99) "99+" else absolute.toString()
        }

        private fun cappedCredits(credits: Long, cap: Int): String {
            if (credits == StockReviewQuoteBook.PRICE_UNAVAILABLE.toLong()) {
                return "?"
            }
            val absolute = abs(credits)
            if (absolute >= cap) {
                return CreditFormat.grouped(cap.toLong()) + "+" + CreditFormat.CREDIT_SYMBOL
            }
            return StockReviewFormat.credits(absolute)
        }
    }
}
