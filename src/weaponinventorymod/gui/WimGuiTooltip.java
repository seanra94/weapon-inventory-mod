package weaponinventorymod.gui;

import com.fs.starfarer.api.ui.TooltipMakerAPI;

final class WimGuiTooltip implements TooltipMakerAPI.TooltipCreator {
    private static final float WIDTH = 320f;

    private final String text;

    WimGuiTooltip(String text) {
        this.text = text == null ? "" : text;
    }

    @Override
    public boolean isTooltipExpandable(Object tooltipParam) {
        return false;
    }

    @Override
    public float getTooltipWidth(Object tooltipParam) {
        return WIDTH;
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
        tooltip.setParaFontDefault();
        tooltip.setParaFontColor(StockReviewStyle.TEXT);
        tooltip.addPara(text, 0f);
    }

    static boolean hasText(String tooltip) {
        return tooltip != null && tooltip.trim().length() > 0;
    }
}
