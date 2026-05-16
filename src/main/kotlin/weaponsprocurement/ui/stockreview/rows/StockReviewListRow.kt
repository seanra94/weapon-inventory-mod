package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.WimGuiRowCell
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import java.awt.Color

object StockReviewListRow {
    @JvmStatic
    fun category(label: String?, textColor: Color?, action: StockReviewAction?, topGap: Boolean): WimGuiListRow<StockReviewAction> =
        category(label, textColor, action, topGap, null)

    @JvmStatic
    fun category(
        label: String?,
        textColor: Color?,
        action: StockReviewAction?,
        topGap: Boolean,
        tooltip: String?,
    ): WimGuiListRow<StockReviewAction> = category(label, textColor, action, topGap, tooltip, 0f)

    @JvmStatic
    fun categoryIndented(
        label: String?,
        textColor: Color?,
        action: StockReviewAction?,
        topGap: Boolean,
        tooltip: String?,
        indent: Float,
    ): WimGuiListRow<StockReviewAction> = category(label, textColor, action, topGap, tooltip, indent)

    private fun category(
        label: String?,
        textColor: Color?,
        action: StockReviewAction?,
        topGap: Boolean,
        tooltip: String?,
        indent: Float,
    ): WimGuiListRow<StockReviewAction> = row(
        label,
        StockReviewStyle.TEXT,
        null,
        textColor,
        null,
        indent,
        action,
        Alignment.LMID,
        null,
        topGap,
        tooltip,
    )

    @JvmStatic
    fun filterHeading(label: String?, action: StockReviewAction?, topGap: Boolean): WimGuiListRow<StockReviewAction> =
        filterHeading(label, action, topGap, null)

    @JvmStatic
    fun filterHeading(
        label: String?,
        action: StockReviewAction?,
        topGap: Boolean,
        tooltip: String?,
    ): WimGuiListRow<StockReviewAction> = row(
        label,
        StockReviewStyle.TEXT,
        StockReviewStyle.HEADING_BACKGROUND,
        StockReviewStyle.HEADING_BACKGROUND,
        null,
        0f,
        action,
        Alignment.LMID,
        null,
        topGap,
        tooltip,
    )

    @JvmStatic
    fun nestedHeading(
        label: String?,
        indent: Float,
        rightReserveWidth: Float,
        action: StockReviewAction?,
        topGap: Boolean,
        tooltip: String?,
    ): WimGuiListRow<StockReviewAction> = row(
        label,
        StockReviewStyle.TEXT,
        null,
        StockReviewStyle.HEADING_BACKGROUND,
        null,
        indent,
        action,
        Alignment.LMID,
        null,
        topGap,
        null,
        rightReserveWidth,
        tooltip,
    )

    @JvmStatic
    fun filter(label: String?, active: Boolean, action: StockReviewAction?, topGap: Boolean): WimGuiListRow<StockReviewAction> =
        filter(label, active, action, topGap, null)

    @JvmStatic
    fun filter(
        label: String?,
        active: Boolean,
        action: StockReviewAction?,
        topGap: Boolean,
        tooltip: String?,
    ): WimGuiListRow<StockReviewAction> {
        val fill = if (active) StockReviewStyle.FILTER_ACTIVE else StockReviewStyle.ROW_BACKGROUND
        return row(
            label,
            StockReviewStyle.TEXT,
            fill,
            fill,
            StockReviewStyle.ROW_BORDER,
            if (active) 0f else StockReviewStyle.WEAPON_INDENT,
            action,
            Alignment.LMID,
            null,
            topGap,
            tooltip,
        )
    }

    @JvmStatic
    fun item(
        label: String?,
        cells: List<WimGuiRowCell<StockReviewAction>>?,
        action: StockReviewAction?,
    ): WimGuiListRow<StockReviewAction> = item(label, cells, action, null)

    @JvmStatic
    fun item(
        label: String?,
        cells: List<WimGuiRowCell<StockReviewAction>>?,
        action: StockReviewAction?,
        tooltip: String?,
    ): WimGuiListRow<StockReviewAction> = item(label, cells, action, tooltip, StockReviewStyle.WEAPON_INDENT)

    @JvmStatic
    fun item(
        label: String?,
        cells: List<WimGuiRowCell<StockReviewAction>>?,
        action: StockReviewAction?,
        tooltip: String?,
        indent: Float,
    ): WimGuiListRow<StockReviewAction> = item(label, cells, action, tooltip, null, indent)

    @JvmStatic
    fun item(
        label: String?,
        cells: List<WimGuiRowCell<StockReviewAction>>?,
        action: StockReviewAction?,
        tooltip: String?,
        tooltipCreator: TooltipMakerAPI.TooltipCreator?,
        indent: Float,
    ): WimGuiListRow<StockReviewAction> = item(label, cells, action, tooltip, tooltipCreator, indent, null)

    @JvmStatic
    fun item(
        label: String?,
        cells: List<WimGuiRowCell<StockReviewAction>>?,
        action: StockReviewAction?,
        tooltip: String?,
        tooltipCreator: TooltipMakerAPI.TooltipCreator?,
        indent: Float,
        icon: StockReviewRowIcon?,
    ): WimGuiListRow<StockReviewAction> = row(
        label,
        StockReviewStyle.TEXT,
        StockReviewStyle.ROW_BACKGROUND,
        StockReviewStyle.CELL_BACKGROUND,
        StockReviewStyle.ROW_BORDER,
        indent,
        action,
        Alignment.LMID,
        cells,
        false,
        tooltip,
        tooltipCreator,
        icon,
    )

    @JvmStatic
    fun labelTextIndented(label: String?, value: String?, indent: Float): WimGuiListRow<StockReviewAction> =
        labelTextIndented(label, value, indent, false)

    @JvmStatic
    fun labelTextIndented(
        label: String?,
        value: String?,
        indent: Float,
        topGap: Boolean,
    ): WimGuiListRow<StockReviewAction> =
        labelTextIndented(label, value, indent, topGap, StockReviewStyle.TRADE_ROW_RIGHT_BLOCK_WIDTH)

    @JvmStatic
    fun labelTextIndented(
        label: String?,
        value: String?,
        indent: Float,
        topGap: Boolean,
        rightReserveWidth: Float,
    ): WimGuiListRow<StockReviewAction> =
        labelTextIndented(label, value, indent, topGap, rightReserveWidth, StockReviewStyle.LIST_WIDTH)

    @JvmStatic
    fun labelTextIndented(
        label: String?,
        value: String?,
        indent: Float,
        topGap: Boolean,
        rightReserveWidth: Float,
        listWidth: Float,
    ): WimGuiListRow<StockReviewAction> {
        val componentWidth = maxOf(
            40f,
            listWidth - indent - rightReserveWidth - 2f * StockReviewStyle.SMALL_PAD,
        )
        return labelTextRow(label, value, indent, topGap, componentWidth, rightReserveWidth, StockReviewStyle.TEXT)
    }

    private fun labelTextRow(
        label: String?,
        value: String?,
        indent: Float,
        topGap: Boolean,
        componentWidth: Float,
        rightReserveWidth: Float,
        valueColor: Color?,
    ): WimGuiListRow<StockReviewAction> {
        val labelWidth = componentWidth * 0.65f
        val valueWidth = componentWidth - labelWidth
        val cells = WimGuiRowCell.of(
            WimGuiRowCell.infoWithBorder<StockReviewAction>(
                label,
                labelWidth,
                null,
                StockReviewStyle.TEXT,
                Alignment.LMID,
                StockReviewStyle.ROW_BORDER,
            ),
            WimGuiRowCell.infoWithBorder<StockReviewAction>(
                value,
                valueWidth,
                StockReviewStyle.CELL_BACKGROUND,
                valueColor,
                Alignment.MID,
                StockReviewStyle.ROW_BORDER,
            ),
        )
        return row(
            "",
            StockReviewStyle.TEXT,
            null,
            null,
            null,
            indent,
            null,
            Alignment.LMID,
            cells,
            topGap,
            0f,
            rightReserveWidth,
        )
    }

    @JvmStatic
    fun form(label: String?, cells: List<WimGuiRowCell<StockReviewAction>>?): WimGuiListRow<StockReviewAction> = row(
        label,
        StockReviewStyle.TEXT,
        StockReviewStyle.ROW_BACKGROUND,
        StockReviewStyle.ROW_BACKGROUND,
        StockReviewStyle.ROW_BORDER,
        0f,
        null,
        Alignment.LMID,
        cells,
        false,
    )

    @JvmStatic
    fun empty(label: String?): WimGuiListRow<StockReviewAction> = row(
        label,
        StockReviewStyle.MUTED,
        null,
        null,
        null,
        0f,
        null,
        Alignment.LMID,
        null,
        false,
    )

    @JvmStatic
    fun scroll(label: String?, action: StockReviewAction?): WimGuiListRow<StockReviewAction> = row(
        label,
        StockReviewStyle.SCROLL,
        StockReviewStyle.HEADING_BACKGROUND,
        StockReviewStyle.HEADING_BACKGROUND,
        null,
        0f,
        action,
        Alignment.MID,
        null,
        false,
        "Move the list by one visible page.",
    )

    @JvmStatic
    fun review(label: String?, fillColor: Color?): WimGuiListRow<StockReviewAction> = row(
        label,
        StockReviewStyle.TEXT,
        fillColor,
        fillColor,
        StockReviewStyle.ROW_BORDER,
        StockReviewStyle.WEAPON_INDENT,
        null,
        Alignment.LMID,
        null,
        false,
    )

    @JvmStatic
    fun review(label: String?): WimGuiListRow<StockReviewAction> = review(label, StockReviewStyle.ROW_BACKGROUND)

    private fun row(
        label: String?,
        textColor: Color?,
        fillColor: Color?,
        buttonFillColor: Color?,
        borderColor: Color?,
        indent: Float,
        action: StockReviewAction?,
        alignment: Alignment?,
        cells: List<WimGuiRowCell<StockReviewAction>>?,
        topGap: Boolean,
    ): WimGuiListRow<StockReviewAction> = WimGuiListRow(
        label,
        textColor,
        fillColor,
        buttonFillColor,
        borderColor,
        indent,
        action,
        alignment,
        cells,
        topGap,
        null,
        0f,
        null,
    )

    private fun row(
        label: String?,
        textColor: Color?,
        fillColor: Color?,
        buttonFillColor: Color?,
        borderColor: Color?,
        indent: Float,
        action: StockReviewAction?,
        alignment: Alignment?,
        cells: List<WimGuiRowCell<StockReviewAction>>?,
        topGap: Boolean,
        tooltip: String?,
    ): WimGuiListRow<StockReviewAction> = row(
        label,
        textColor,
        fillColor,
        buttonFillColor,
        borderColor,
        indent,
        action,
        alignment,
        cells,
        topGap,
        tooltip,
        null,
    )

    private fun row(
        label: String?,
        textColor: Color?,
        fillColor: Color?,
        buttonFillColor: Color?,
        borderColor: Color?,
        indent: Float,
        action: StockReviewAction?,
        alignment: Alignment?,
        cells: List<WimGuiRowCell<StockReviewAction>>?,
        topGap: Boolean,
        tooltip: String?,
        tooltipCreator: TooltipMakerAPI.TooltipCreator?,
    ): WimGuiListRow<StockReviewAction> = row(
        label,
        textColor,
        fillColor,
        buttonFillColor,
        borderColor,
        indent,
        action,
        alignment,
        cells,
        topGap,
        tooltip,
        tooltipCreator,
        null,
    )

    private fun row(
        label: String?,
        textColor: Color?,
        fillColor: Color?,
        buttonFillColor: Color?,
        borderColor: Color?,
        indent: Float,
        action: StockReviewAction?,
        alignment: Alignment?,
        cells: List<WimGuiRowCell<StockReviewAction>>?,
        topGap: Boolean,
        tooltip: String?,
        tooltipCreator: TooltipMakerAPI.TooltipCreator?,
        icon: StockReviewRowIcon?,
    ): WimGuiListRow<StockReviewAction> = WimGuiListRow(
        label,
        textColor,
        fillColor,
        buttonFillColor,
        borderColor,
        indent,
        action,
        alignment,
        cells,
        topGap,
        null,
        0f,
        tooltip,
        tooltipCreator,
        icon,
    )

    private fun row(
        label: String?,
        textColor: Color?,
        fillColor: Color?,
        buttonFillColor: Color?,
        borderColor: Color?,
        indent: Float,
        action: StockReviewAction?,
        alignment: Alignment?,
        cells: List<WimGuiRowCell<StockReviewAction>>?,
        topGap: Boolean,
        cellGapOverride: Float?,
        rightReserveWidth: Float,
    ): WimGuiListRow<StockReviewAction> =
        row(label, textColor, fillColor, buttonFillColor, borderColor, indent, action, alignment, cells, topGap, cellGapOverride, rightReserveWidth, null)

    private fun row(
        label: String?,
        textColor: Color?,
        fillColor: Color?,
        buttonFillColor: Color?,
        borderColor: Color?,
        indent: Float,
        action: StockReviewAction?,
        alignment: Alignment?,
        cells: List<WimGuiRowCell<StockReviewAction>>?,
        topGap: Boolean,
        cellGapOverride: Float?,
        rightReserveWidth: Float,
        tooltip: String?,
    ): WimGuiListRow<StockReviewAction> = WimGuiListRow(
        label,
        textColor,
        fillColor,
        buttonFillColor,
        borderColor,
        indent,
        action,
        alignment,
        cells,
        topGap,
        cellGapOverride,
        rightReserveWidth,
        tooltip,
    )
}