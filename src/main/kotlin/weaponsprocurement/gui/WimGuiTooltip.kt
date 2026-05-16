package weaponsprocurement.gui

import com.fs.starfarer.api.ui.TooltipMakerAPI

class WimGuiTooltip(text: String?) : TooltipMakerAPI.TooltipCreator {
    private val text: String = text ?: ""

    override fun isTooltipExpandable(tooltipParam: Any?): Boolean = false

    override fun getTooltipWidth(tooltipParam: Any?): Float = WIDTH

    override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any?) {
        tooltip.setParaFontDefault()
        tooltip.setParaFontColor(StockReviewStyle.TEXT)
        tooltip.addPara(text, 0f)
    }

    companion object {
        private const val WIDTH = 320f

        @JvmStatic
        fun hasText(tooltip: String?): Boolean = tooltip != null && tooltip.trim().isNotEmpty()
    }
}
