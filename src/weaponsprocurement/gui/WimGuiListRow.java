package weaponsprocurement.gui;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class WimGuiListRow<A> {
    private final String label;
    private final Color textColor;
    private final Color fillColor;
    private final Color buttonFillColor;
    private final Color borderColor;
    private final float indent;
    private final A mainAction;
    private final Alignment mainAlignment;
    private final List<WimGuiRowCell<A>> cells;
    private final boolean topGap;
    private final Float cellGapOverride;
    private final float rightReserveWidth;
    private final String tooltip;
    private final TooltipMakerAPI.TooltipCreator tooltipCreator;
    private final StockReviewRowIcon icon;

    WimGuiListRow(String label,
                  Color textColor,
                  Color fillColor,
                  Color buttonFillColor,
                  Color borderColor,
                  float indent,
                  A mainAction,
                  Alignment mainAlignment,
                  List<WimGuiRowCell<A>> cells,
                  boolean topGap,
                  Float cellGapOverride,
                  float rightReserveWidth,
                  String tooltip) {
        this(label, textColor, fillColor, buttonFillColor, borderColor, indent, mainAction,
                mainAlignment, cells, topGap, cellGapOverride, rightReserveWidth, tooltip, null, null);
    }

    WimGuiListRow(String label,
                  Color textColor,
                  Color fillColor,
                  Color buttonFillColor,
                  Color borderColor,
                  float indent,
                  A mainAction,
                  Alignment mainAlignment,
                  List<WimGuiRowCell<A>> cells,
                  boolean topGap,
                  Float cellGapOverride,
                  float rightReserveWidth,
                  String tooltip,
                  TooltipMakerAPI.TooltipCreator tooltipCreator) {
        this(label, textColor, fillColor, buttonFillColor, borderColor, indent, mainAction, mainAlignment,
                cells, topGap, cellGapOverride, rightReserveWidth, tooltip, tooltipCreator, null);
    }

    WimGuiListRow(String label,
                  Color textColor,
                  Color fillColor,
                  Color buttonFillColor,
                  Color borderColor,
                  float indent,
                  A mainAction,
                  Alignment mainAlignment,
                  List<WimGuiRowCell<A>> cells,
                  boolean topGap,
                  Float cellGapOverride,
                  float rightReserveWidth,
                  String tooltip,
                  TooltipMakerAPI.TooltipCreator tooltipCreator,
                  StockReviewRowIcon icon) {
        this.label = label;
        this.textColor = textColor;
        this.fillColor = fillColor;
        this.buttonFillColor = buttonFillColor;
        this.borderColor = borderColor;
        this.indent = indent;
        this.mainAction = mainAction;
        this.mainAlignment = mainAlignment == null ? Alignment.LMID : mainAlignment;
        this.cells = cells == null
                ? Collections.<WimGuiRowCell<A>>emptyList()
                : Collections.unmodifiableList(new ArrayList<WimGuiRowCell<A>>(cells));
        this.topGap = topGap;
        this.cellGapOverride = cellGapOverride;
        this.rightReserveWidth = Math.max(0f, rightReserveWidth);
        this.tooltip = tooltip;
        this.tooltipCreator = tooltipCreator;
        this.icon = icon;
    }

    String getLabel() {
        return label;
    }

    Color getTextColor() {
        return textColor;
    }

    Color getFillColor() {
        return fillColor;
    }

    Color getButtonFillColor() {
        return buttonFillColor == null ? fillColor : buttonFillColor;
    }

    Color getBorderColor() {
        return borderColor;
    }

    float getIndent() {
        return indent;
    }

    A getMainAction() {
        return mainAction;
    }

    Alignment getMainAlignment() {
        return mainAlignment;
    }

    List<WimGuiRowCell<A>> getCells() {
        return cells;
    }

    boolean hasTopGap() {
        return topGap;
    }

    float cellGap(float defaultGap) {
        return cellGapOverride == null ? defaultGap : cellGapOverride;
    }

    float rightReserveWidth() {
        return rightReserveWidth;
    }

    String getTooltip() {
        return tooltip;
    }

    TooltipMakerAPI.TooltipCreator getTooltipCreator() {
        return tooltipCreator;
    }

    StockReviewRowIcon getIcon() {
        return icon;
    }
}
