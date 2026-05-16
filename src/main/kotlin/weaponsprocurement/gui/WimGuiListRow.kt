package weaponsprocurement.gui

import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import java.awt.Color
import java.util.ArrayList
import java.util.Collections

class WimGuiListRow<A> @JvmOverloads constructor(
    private val label: String?,
    private val textColor: Color?,
    private val fillColor: Color?,
    private val buttonFillColor: Color?,
    private val borderColor: Color?,
    private val indent: Float,
    private val mainAction: A?,
    private val mainAlignment: Alignment?,
    cells: List<WimGuiRowCell<A>>?,
    private val topGap: Boolean,
    private val cellGapOverride: Float?,
    rightReserveWidth: Float,
    private val tooltip: String?,
    private val tooltipCreator: TooltipMakerAPI.TooltipCreator? = null,
    private val icon: StockReviewRowIcon? = null,
) {
    private val cells: List<WimGuiRowCell<A>> =
        if (cells == null) emptyList() else Collections.unmodifiableList(ArrayList(cells))
    private val rightReserveWidth: Float = Math.max(0f, rightReserveWidth)

    fun getLabel(): String? = label
    fun getTextColor(): Color? = textColor
    fun getFillColor(): Color? = fillColor
    fun getButtonFillColor(): Color? = buttonFillColor ?: fillColor
    fun getBorderColor(): Color? = borderColor
    fun getIndent(): Float = indent
    fun getMainAction(): A? = mainAction
    fun getMainAlignment(): Alignment = mainAlignment ?: Alignment.LMID
    fun getCells(): List<WimGuiRowCell<A>> = cells
    fun hasTopGap(): Boolean = topGap
    fun cellGap(defaultGap: Float): Float = cellGapOverride ?: defaultGap
    fun rightReserveWidth(): Float = rightReserveWidth
    fun getTooltip(): String? = tooltip
    fun getTooltipCreator(): TooltipMakerAPI.TooltipCreator? = tooltipCreator
    fun getIcon(): StockReviewRowIcon? = icon
}
