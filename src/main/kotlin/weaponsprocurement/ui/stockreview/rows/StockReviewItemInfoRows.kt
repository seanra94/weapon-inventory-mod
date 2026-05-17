package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.WimGuiToggleHeading
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.actions.StockReviewAction.Type
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import weaponsprocurement.ui.stockreview.state.StockReviewState
import weaponsprocurement.stock.item.WeaponStockRecord
import java.util.Locale

object StockReviewItemInfoRows {
    @JvmStatic
    fun add(rows: MutableList<WimGuiListRow<StockReviewAction>>, record: WeaponStockRecord, state: StockReviewState?) {
        add(
            rows,
            record,
            state,
            StockReviewStyle.TRADE_ROW_RIGHT_BLOCK_WIDTH,
            StockReviewStyle.LIST_WIDTH,
            StockReviewStyle.DETAIL_INDENT,
            StockReviewStyle.DATA_INDENT,
        )
    }

    @JvmStatic
    fun add(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        record: WeaponStockRecord,
        state: StockReviewState?,
        rightReserveWidth: Float,
    ) {
        add(
            rows,
            record,
            state,
            rightReserveWidth,
            StockReviewStyle.LIST_WIDTH,
            StockReviewStyle.SECTION_INDENT,
            StockReviewStyle.DETAIL_INDENT,
        )
    }

    @JvmStatic
    fun add(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        record: WeaponStockRecord,
        state: StockReviewState?,
        rightReserveWidth: Float,
        listWidth: Float,
    ) {
        add(
            rows,
            record,
            state,
            rightReserveWidth,
            listWidth,
            StockReviewStyle.SECTION_INDENT,
            StockReviewStyle.DETAIL_INDENT,
        )
    }

    @JvmStatic
    fun add(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        record: WeaponStockRecord,
        state: StockReviewState?,
        rightReserveWidth: Float,
        listWidth: Float,
        infoIndent: Float,
        dataIndent: Float,
    ) {
        val basicExpanded = isInfoSectionExpanded(state, record, "basic")
        rows.add(infoHeading("Basic Info", record, "basic", basicExpanded, infoIndent, rightReserveWidth))
        if (basicExpanded) {
            addBasicInfo(rows, record, rightReserveWidth, listWidth, dataIndent)
        }
        if (record.isWing()) {
            return
        }
        val advancedExpanded = isInfoSectionExpanded(state, record, "advanced")
        rows.add(infoHeading("Advanced Info", record, "advanced", advancedExpanded, infoIndent, rightReserveWidth))
        if (advancedExpanded) {
            addAdvancedInfo(rows, record, rightReserveWidth, listWidth, dataIndent)
        }
    }

    private fun addBasicInfo(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        record: WeaponStockRecord,
        rightReserveWidth: Float,
        listWidth: Float,
        dataIndent: Float,
    ) {
        addRequiredDataRow(rows, "Desired", record.desiredCount.toString(), rightReserveWidth, listWidth, dataIndent)
        addDataRow(
            rows,
            "Availability",
            record.fixerAvailabilityLabel,
            rightReserveWidth,
            listWidth,
            dataIndent,
            record.fixerAvailabilityDetails,
        )
        addDataRow(rows, "Rarity", record.fixerRarityLabel, rightReserveWidth, listWidth, dataIndent)
        if (record.isWing()) {
            addDataRow(rows, "Primary Role", record.typeLabel, rightReserveWidth, listWidth, dataIndent)
            addRequiredDataRow(rows, "Size", "WING", rightReserveWidth, listWidth, dataIndent)
            addDataRow(rows, "Fighters", record.wingFighterCountLabel, rightReserveWidth, listWidth, dataIndent)
            addDataRow(rows, "OP", record.wingOpCostLabel, rightReserveWidth, listWidth, dataIndent)
            addDataRow(rows, "Range", record.rangeLabel, rightReserveWidth, listWidth, dataIndent)
            addDataRow(rows, "Refit(Sec)", record.wingRefitTimeLabel, rightReserveWidth, listWidth, dataIndent)
            return
        }
        addDataRow(rows, "Primary Role", record.primaryRoleLabel, rightReserveWidth, listWidth, dataIndent)
        addDataRow(rows, "Size", record.sizeLabel, rightReserveWidth, listWidth, dataIndent)
        addDataRow(rows, "Type", record.typeLabel, rightReserveWidth, listWidth, dataIndent)
        addDataRow(rows, "OP", record.opCostLabel, rightReserveWidth, listWidth, dataIndent)
        addDataRow(rows, "Range", record.rangeLabel, rightReserveWidth, listWidth, dataIndent)
        addDataRow(rows, "Refire(Sec)", record.refireSecondsLabel, rightReserveWidth, listWidth, dataIndent)
        addDataRow(rows, "Damage", record.damageLabel, rightReserveWidth, listWidth, dataIndent)
        addDataRow(
            rows,
            if (record.hasDifferentSustainedDamagePerSecond()) "Damage/Sec (sustained)" else "Damage/Sec",
            record.sustainedDamagePerSecondLabel,
            rightReserveWidth,
            listWidth,
            dataIndent,
        )
        addDataRow(
            rows,
            if (record.hasDifferentSustainedFluxPerSecond()) "Flux/Sec (sustained)" else "Flux/Sec",
            record.sustainedFluxPerSecondLabel,
            rightReserveWidth,
            listWidth,
            dataIndent,
        )
        addDataRow(rows, "Flux/Damage", record.fluxPerDamageLabel, rightReserveWidth, listWidth, dataIndent)
        addPositiveDataRow(rows, "EMP", record.empLabel, rightReserveWidth, listWidth, dataIndent)
        addDataRow(rows, "Max Ammo", record.maxAmmoLabel, rightReserveWidth, listWidth, dataIndent)
        addDataRow(rows, "Sec / Reload", record.secPerReloadLabel, rightReserveWidth, listWidth, dataIndent)
        addDataRow(rows, "Ammo Gain", record.ammoGainLabel, rightReserveWidth, listWidth, dataIndent)
        addDataRow(rows, "Accuracy", record.accuracyLabel, rightReserveWidth, listWidth, dataIndent)
    }

    private fun addAdvancedInfo(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        record: WeaponStockRecord,
        rightReserveWidth: Float,
        listWidth: Float,
        dataIndent: Float,
    ) {
        addPositiveDataRow(
            rows,
            if (record.hasDifferentSustainedEmpPerSecond()) "EMP/Second (sustained)" else "EMP/Second",
            record.sustainedEmpPerSecondLabel,
            rightReserveWidth,
            listWidth,
            dataIndent,
        )
        addPositiveDataRow(rows, "Flux/EMP", record.fluxPerEmpLabel, rightReserveWidth, listWidth, dataIndent)
        addDataRow(rows, "Beam DPS", record.beamDpsLabel, rightReserveWidth, listWidth, dataIndent)
        addDataRow(rows, "Charge Up", record.beamChargeUpLabel, rightReserveWidth, listWidth, dataIndent)
        addDataRow(rows, "Charge Down", record.beamChargeDownLabel, rightReserveWidth, listWidth, dataIndent)
        addDataRow(rows, "Burst Delay", record.burstDelayLabel, rightReserveWidth, listWidth, dataIndent)
        addDataRow(rows, "Turn Rate/Second", record.turnRateLabel, rightReserveWidth, listWidth, dataIndent)
        addDataRow(rows, "Min Spread", record.minSpreadLabel, rightReserveWidth, listWidth, dataIndent)
        addDataRow(rows, "Max Spread", record.maxSpreadLabel, rightReserveWidth, listWidth, dataIndent)
        addDataRow(rows, "Spread / Shot", record.spreadPerShotLabel, rightReserveWidth, listWidth, dataIndent)
        addDataRow(rows, "Spread Decay", record.spreadDecayLabel, rightReserveWidth, listWidth, dataIndent)
        addDataRow(rows, "Proj. Speed", record.projectileSpeedLabel, rightReserveWidth, listWidth, dataIndent)
        addDataRow(rows, "Launch Speed", record.launchSpeedLabel, rightReserveWidth, listWidth, dataIndent)
        addDataRow(rows, "Flight Time", record.flightTimeLabel, rightReserveWidth, listWidth, dataIndent)
        addDataRow(rows, "Guided", record.guidedLabel, rightReserveWidth, listWidth, dataIndent)
    }

    private fun infoHeading(
        label: String,
        record: WeaponStockRecord,
        section: String,
        expanded: Boolean,
        indent: Float,
        rightReserveWidth: Float,
    ): WimGuiListRow<StockReviewAction> = StockReviewListRow.nestedHeading(
        WimGuiToggleHeading.label(label, expanded),
        indent,
        rightReserveWidth,
        StockReviewAction.toggleItem(infoSectionKey(record, section)),
        false,
        "Show or hide ${label.lowercase(Locale.US)} rows.",
    )

    private fun isInfoSectionExpanded(state: StockReviewState?, record: WeaponStockRecord, section: String): Boolean =
        state == null || !state.isItemExpanded(infoSectionKey(record, section))

    private fun infoSectionKey(record: WeaponStockRecord, section: String): String = record.itemKey + "::info::" + section

    private fun addRequiredDataRow(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        label: String,
        value: String?,
        rightReserveWidth: Float,
        listWidth: Float,
        indent: Float,
    ) {
        rows.add(dataRow(label, value, rightReserveWidth, listWidth, indent))
    }

    private fun addDataRow(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        label: String,
        value: String?,
        rightReserveWidth: Float,
        listWidth: Float,
        indent: Float,
    ) = addDataRow(rows, label, value, rightReserveWidth, listWidth, indent, null)

    private fun addDataRow(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        label: String,
        value: String?,
        rightReserveWidth: Float,
        listWidth: Float,
        indent: Float,
        tooltip: String?,
    ) {
        if (isMeaningful(value)) {
            rows.add(dataRow(label, value, rightReserveWidth, listWidth, indent, tooltip))
        }
    }

    private fun addPositiveDataRow(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        label: String,
        value: String?,
        rightReserveWidth: Float,
        listWidth: Float,
        indent: Float,
    ) {
        if (isPositiveValue(value)) {
            rows.add(dataRow(label, value, rightReserveWidth, listWidth, indent))
        }
    }

    private fun dataRow(
        label: String,
        value: String?,
        rightReserveWidth: Float,
        listWidth: Float,
        indent: Float,
    ): WimGuiListRow<StockReviewAction> = dataRow(label, value, rightReserveWidth, listWidth, indent, null)

    private fun dataRow(
        label: String,
        value: String?,
        rightReserveWidth: Float,
        listWidth: Float,
        indent: Float,
        tooltip: String?,
    ): WimGuiListRow<StockReviewAction> = StockReviewListRow.labelTextIndented(
        label,
        value,
        indent,
        false,
        rightReserveWidth + StockReviewStyle.TEXT_LEFT_PAD,
        listWidth,
        tooltip,
    )

    private fun isMeaningful(value: String?): Boolean = value != null && value.trim().isNotEmpty() && value.trim() != "?"

    private fun isPositiveValue(value: String?): Boolean {
        if (!isMeaningful(value)) {
            return false
        }
        val normalized = value?.replace("\u00b0/s", "")?.trim() ?: return false
        return try {
            normalized.toFloat() > 0f
        } catch (ex: NumberFormatException) {
            true
        }
    }
}
