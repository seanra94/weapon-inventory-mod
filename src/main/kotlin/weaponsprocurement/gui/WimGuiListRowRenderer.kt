package weaponsprocurement.gui

import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.CustomPanelAPI
import java.awt.Color

class WimGuiListRowRenderer private constructor() {
    companion object {
        @JvmStatic
        fun <A> renderRow(
            parent: CustomPanelAPI,
            row: WimGuiListRow<A>,
            y: Float,
            rowHeight: Float,
            actionHeight: Float,
            horizontalPad: Float,
            buttonGap: Float,
            textLeftPad: Float,
            minLabelWidth: Float,
            defaultBorder: Color,
            buttons: MutableList<WimGuiButtonBinding<A>>,
        ) {
            val width = parent.position.width - 2f * horizontalPad
            val rowBorder = if (row.getIndent() > 0f) null else row.getBorderColor()
            val rowPanel = parent.createCustomPanel(
                width,
                rowHeight,
                WimGuiPanelPlugin(row.getFillColor(), rowBorder),
            )
            parent.addComponent(rowPanel).inTL(horizontalPad, y)

            val cellGap = row.cellGap(buttonGap)
            val cellBlockWidth = WimGuiRowCell.totalWidth(row.getCells(), cellGap)
            val reservedBlockWidth = Math.max(cellBlockWidth, row.rightReserveWidth())
            val labelLeft = row.getIndent()
            val labelWidth = Math.max(minLabelWidth, width - labelLeft - reservedBlockWidth - textLeftPad)
            if (row.getMainAction() != null) {
                addMainAction(rowPanel, row, labelLeft, labelWidth, actionHeight, buttonGap, defaultBorder, buttons)
            } else {
                addLabel(rowPanel, row.getLabel(), row.getTextColor(), labelLeft, labelWidth, rowHeight)
            }

            if (row.getCells().isNotEmpty()) {
                var x = width - row.rightReserveWidth() - cellBlockWidth
                for (cell in row.getCells()) {
                    WimGuiControls.addRowCell(rowPanel, x, 0f, actionHeight, cell, buttons, defaultBorder)
                    x += cell.getWidth() + cellGap
                }
            }
        }

        private fun <A> addMainAction(
            rowPanel: CustomPanelAPI,
            row: WimGuiListRow<A>,
            labelLeft: Float,
            labelWidth: Float,
            actionHeight: Float,
            buttonGap: Float,
            defaultBorder: Color,
            buttons: MutableList<WimGuiButtonBinding<A>>,
        ) {
            var buttonLeft = labelLeft
            var buttonWidth = labelWidth
            val icon = row.getIcon()
            if (icon != null) {
                val iconSize = actionHeight
                addRowIcon(rowPanel, icon, labelLeft, 0f, iconSize)
                buttonLeft += iconSize + buttonGap
                buttonWidth = Math.max(8f, labelWidth - iconSize - buttonGap)
            }
            WimGuiControls.addBoundButton(
                rowPanel,
                buttonLeft,
                0f,
                actionHeight,
                WimGuiButtonSpec.toggle(
                    buttonWidth,
                    row.getLabel(),
                    row.getTextColor() ?: WimGuiStyle.DEFAULT_TEXT,
                    row.getMainAction(),
                    row.getMainAlignment(),
                    row.getButtonFillColor() ?: WimGuiStyle.UNCOLOURED_BUTTON,
                    defaultBorder,
                    row.getTooltip(),
                    row.getTooltipCreator(),
                ),
                buttons,
            )
        }

        private fun addRowIcon(parent: CustomPanelAPI, icon: StockReviewRowIcon?, x: Float, y: Float, size: Float) {
            if (icon == null || size <= 0f) {
                return
            }
            val panel = parent.createCustomPanel(
                size,
                size,
                StockReviewWeaponIconPlugin(icon.spriteName, icon.motifType),
            )
            parent.addComponent(panel).inTL(x, y)
        }

        private fun addLabel(parent: CustomPanelAPI, text: String?, color: Color?, x: Float, width: Float, rowHeight: Float) {
            WimGuiControls.addLabel(parent, text, color, x, 0f, width, rowHeight, Alignment.LMID)
        }
    }
}
