package weaponsprocurement.gui;

import java.awt.Color;

final class WimGuiModalListSpec {
    final WimGuiModalLayout modal;
    final float panelLeft;
    final float panelTop;
    final float panelWidth;
    final float panelHeight;
    final float rowHeight;
    final float actionHeight;
    final float rowGap;
    final float rowHorizontalPad;
    final float buttonGap;
    final float textLeftPad;
    final float minLabelWidth;
    final Color panelFill;
    final Color panelBorder;
    final Color rowBorder;

    WimGuiModalListSpec(WimGuiModalLayout modal,
                        float panelLeft,
                        float panelTop,
                        float panelWidth,
                        float panelHeight,
                        float rowHeight,
                        float actionHeight,
                        float rowGap,
                        float rowHorizontalPad,
                        float buttonGap,
                        float textLeftPad,
                        float minLabelWidth,
                        Color panelFill,
                        Color panelBorder,
                        Color rowBorder) {
        this.modal = modal;
        this.panelLeft = panelLeft;
        this.panelTop = panelTop;
        this.panelWidth = panelWidth;
        this.panelHeight = panelHeight;
        this.rowHeight = rowHeight;
        this.actionHeight = actionHeight;
        this.rowGap = rowGap;
        this.rowHorizontalPad = rowHorizontalPad;
        this.buttonGap = buttonGap;
        this.textLeftPad = textLeftPad;
        this.minLabelWidth = minLabelWidth;
        this.panelFill = panelFill;
        this.panelBorder = panelBorder;
        this.rowBorder = rowBorder;
    }
}
