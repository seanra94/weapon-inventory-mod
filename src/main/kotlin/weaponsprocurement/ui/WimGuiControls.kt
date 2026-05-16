package weaponsprocurement.ui

import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.ButtonAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.CutStyle
import com.fs.starfarer.api.ui.TooltipMakerAPI
import weaponsprocurement.ui.stockreview.tooltips.StockReviewItemTooltip
import java.awt.Color

object WimGuiControls {
    @JvmStatic
    fun addButton(
        parent: CustomPanelAPI,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        label: String?,
        textColor: Color?,
        action: Any?,
        enabled: Boolean,
        alignment: Alignment,
        colors: WimGuiButtonColors?,
        borderColor: Color?,
        tooltip: String?,
    ): WimGuiButtonShell = addButton(
        parent,
        x,
        y,
        width,
        height,
        label,
        textColor,
        action,
        enabled,
        alignment,
        colors,
        borderColor,
        tooltip,
        null,
    )

    @JvmStatic
    fun addButton(
        parent: CustomPanelAPI,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        label: String?,
        textColor: Color?,
        action: Any?,
        enabled: Boolean,
        alignment: Alignment,
        colors: WimGuiButtonColors?,
        borderColor: Color?,
        tooltip: String?,
        tooltipCreator: TooltipMakerAPI.TooltipCreator?,
    ): WimGuiButtonShell {
        val idle = colors?.idle ?: WimGuiStyle.UNCOLOURED_BUTTON
        val hover = colors?.hover ?: idle
        val shellFill = if (enabled) idle else WimGuiStyle.DISABLED_BACKGROUND
        val buttonFill = if (enabled) WimGuiStyle.dimForIdle(idle) else WimGuiStyle.DISABLED_DARK
        val resolvedText = if (enabled) textColor else WimGuiStyle.DISABLED_TEXT

        val shell = parent.createCustomPanel(width, height, WimGuiPanelPlugin(shellFill, borderColor))
        parent.addComponent(shell).inTL(x, y)
        if (!enabled) {
            addLabel(shell, label, resolvedText, 0f, 0f, width, height, alignment)
            addTooltipHost(shell, width, height, tooltip, tooltipCreator)
            return WimGuiButtonShell(shell, null)
        }

        val element = shell.createUIElement(width, height, false)
        element.setButtonFontDefault()
        val button = element.addButton(
            "",
            action,
            hover,
            buttonFill,
            alignment,
            CutStyle.NONE,
            width,
            height,
            0f,
        )
        button.isEnabled = enabled
        button.setQuickMode(true)
        addTooltipTo(element, shell, tooltip, tooltipCreator)
        shell.addUIElement(element).inTL(0f, 0f)
        addLabel(shell, label, resolvedText, 0f, 0f, width, height, alignment)
        return WimGuiButtonShell(shell, button)
    }

    @JvmStatic
    fun <A> addBoundButton(
        parent: CustomPanelAPI,
        x: Float,
        y: Float,
        height: Float,
        spec: WimGuiButtonSpec<A>,
        bindings: MutableList<WimGuiButtonBinding<A>>?,
    ): WimGuiButtonShell {
        val shell = addButton(
            parent,
            x,
            y,
            spec.width,
            height,
            spec.label,
            spec.textColor,
            spec.action,
            spec.enabled,
            spec.alignment,
            spec.colors,
            spec.borderColor,
            spec.tooltip,
            spec.tooltipCreator,
        )
        if (spec.enabled && bindings != null) {
            bindings.add(WimGuiButtonBinding(shell.panel, shell.button, spec.action))
        }
        return shell
    }

    @JvmStatic
    fun <A> addButtonRow(
        parent: CustomPanelAPI,
        x: Float,
        y: Float,
        height: Float,
        gap: Float,
        specs: List<WimGuiButtonSpec<A>>?,
        bindings: MutableList<WimGuiButtonBinding<A>>?,
    ): Float {
        var cursor = x
        if (specs == null) {
            return cursor
        }
        for (spec in specs) {
            addBoundButton(parent, cursor, y, height, spec, bindings)
            cursor += spec.width + gap
        }
        return cursor
    }

    @JvmStatic
    fun addInfoCell(
        parent: CustomPanelAPI,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        label: String?,
        background: Color?,
        textColor: Color?,
        borderColor: Color?,
    ) {
        addInfoCell(parent, x, y, width, height, label, background, textColor, borderColor, Alignment.MID)
    }

    @JvmStatic
    fun addInfoCell(
        parent: CustomPanelAPI,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        label: String?,
        background: Color?,
        textColor: Color?,
        borderColor: Color?,
        alignment: Alignment,
    ) {
        addInfoCell(parent, x, y, width, height, label, background, textColor, borderColor, alignment, null)
    }

    @JvmStatic
    fun addInfoCell(
        parent: CustomPanelAPI,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        label: String?,
        background: Color?,
        textColor: Color?,
        borderColor: Color?,
        alignment: Alignment,
        tooltip: String?,
    ) {
        val cell = parent.createCustomPanel(width, height, WimGuiPanelPlugin(background, borderColor))
        parent.addComponent(cell).inTL(x, y)
        addTooltipHost(cell, width, height, tooltip)
        addLabel(cell, label, textColor, 0f, 0f, width, height, alignment)
    }

    @JvmStatic
    fun addLabelTextRow(
        parent: CustomPanelAPI,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        label: String?,
        value: String?,
        valueFillColor: Color?,
        borderColor: Color?,
        textColor: Color?,
    ) {
        addLabelTextRow(parent, x, y, width, height, label, value, valueFillColor, borderColor, textColor, null)
    }

    @JvmStatic
    fun addLabelTextRow(
        parent: CustomPanelAPI,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        label: String?,
        value: String?,
        valueFillColor: Color?,
        borderColor: Color?,
        textColor: Color?,
        tooltip: String?,
    ) {
        val labelWidth = width / 2f
        val valueWidth = width - labelWidth
        addInfoCell(parent, x, y, labelWidth, height, label, null, textColor, borderColor, Alignment.LMID, tooltip)
        addInfoCell(parent, x + labelWidth, y, valueWidth, height, value, valueFillColor, textColor, borderColor, Alignment.MID, tooltip)
    }

    @JvmStatic
    fun <A> addRowCell(
        parent: CustomPanelAPI,
        x: Float,
        y: Float,
        height: Float,
        cell: WimGuiRowCell<A>?,
        bindings: MutableList<WimGuiButtonBinding<A>>?,
        borderColor: Color?,
    ) {
        if (cell == null) {
            return
        }
        if (cell.isAction()) {
            val fill = if (cell.isEnabled()) cell.getFillColor() else WimGuiStyle.DISABLED_BACKGROUND
            addBoundButton(
                parent,
                x,
                y,
                height,
                WimGuiButtonSpec.dimmedInner(
                    cell.getWidth(),
                    cell.getLabel(),
                    cell.getTextColor() ?: WimGuiStyle.DEFAULT_TEXT,
                    cell.getAction()!!,
                    cell.isEnabled(),
                    Alignment.MID,
                    fill ?: WimGuiStyle.UNCOLOURED_BUTTON,
                    borderColor ?: WimGuiStyle.ROW_BORDER,
                    cell.getTooltip(),
                ),
                bindings,
            )
            return
        }
        addInfoCell(
            parent,
            x,
            y,
            cell.getWidth(),
            height,
            cell.getLabel(),
            cell.getFillColor(),
            cell.getTextColor(),
            cell.borderColor(borderColor),
            cell.getAlignment(),
            cell.getTooltip(),
        )
    }

    @JvmStatic
    fun addLabel(
        parent: CustomPanelAPI,
        text: String?,
        color: Color?,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        alignment: Alignment,
    ) {
        var labelX = x
        var labelWidth = width
        if (Alignment.LMID == alignment) {
            labelX += WimGuiStyle.TEXT_LEFT_PAD
            labelWidth = maxOf(8f, width - WimGuiStyle.TEXT_LEFT_PAD)
        }
        val label = parent.createUIElement(labelWidth, height, false)
        label.setParaFontDefault()
        label.setParaFontColor(color)
        val maxChars = WimGuiText.estimatedChars(labelWidth)
        val line = label.addPara(WimGuiText.fit(text, maxChars), 0f, color)
        line.setAlignment(alignment)
        parent.addUIElement(label).inTL(labelX, y + WimGuiStyle.TEXT_TOP_PAD)
    }

    private fun addTooltipHost(parent: CustomPanelAPI, width: Float, height: Float, tooltip: String?) {
        if (!WimGuiTooltip.hasText(tooltip)) {
            return
        }
        addTooltipHost(parent, width, height, tooltip, null)
    }

    private fun addTooltipHost(
        parent: CustomPanelAPI,
        width: Float,
        height: Float,
        tooltip: String?,
        tooltipCreator: TooltipMakerAPI.TooltipCreator?,
    ) {
        if (!hasTooltip(tooltip, tooltipCreator)) {
            return
        }
        val element = parent.createUIElement(width, height, false)
        addTooltipTo(element, parent, tooltip, tooltipCreator)
        parent.addUIElement(element).inTL(0f, 0f)
    }

    private fun addTooltipTo(
        element: TooltipMakerAPI,
        target: CustomPanelAPI,
        tooltip: String?,
        tooltipCreator: TooltipMakerAPI.TooltipCreator?,
    ) {
        val creator = tooltipCreator ?: textTooltip(tooltip) ?: return
        val location = if (tooltipCreator is StockReviewItemTooltip) {
            TooltipMakerAPI.TooltipLocation.RIGHT
        } else {
            TooltipMakerAPI.TooltipLocation.BELOW
        }
        element.addTooltipTo(creator, target, location)
    }

    private fun textTooltip(tooltip: String?): TooltipMakerAPI.TooltipCreator? =
        if (WimGuiTooltip.hasText(tooltip)) WimGuiTooltip(tooltip) else null

    private fun hasTooltip(tooltip: String?, tooltipCreator: TooltipMakerAPI.TooltipCreator?): Boolean =
        tooltipCreator != null || WimGuiTooltip.hasText(tooltip)

    @JvmStatic
    fun addWrappedLabel(
        parent: CustomPanelAPI,
        text: String?,
        color: Color?,
        x: Float,
        y: Float,
        width: Float,
        minHeight: Float,
        maxLines: Int,
        alignment: Alignment,
    ): WimGuiTextLayout {
        var labelX = x
        var labelWidth = width
        if (Alignment.LMID == alignment) {
            labelX += WimGuiStyle.TEXT_LEFT_PAD
            labelWidth = maxOf(8f, width - WimGuiStyle.TEXT_LEFT_PAD)
        }
        val layout = WimGuiText.fitLayout(text, labelWidth, minHeight, maxLines)
        val label = parent.createUIElement(labelWidth, layout.rowHeight, false)
        label.setParaFontDefault()
        label.setParaFontColor(color)
        val line = label.addPara(layout.wrappedText, 0f, color)
        line.setAlignment(alignment)
        parent.addUIElement(label).inTL(labelX, y + WimGuiStyle.TEXT_TOP_PAD)
        return layout
    }
}
