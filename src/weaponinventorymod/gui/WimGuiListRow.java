package weaponinventorymod.gui;

import com.fs.starfarer.api.ui.Alignment;

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

    WimGuiListRow(String label,
                  Color textColor,
                  Color fillColor,
                  Color buttonFillColor,
                  Color borderColor,
                  float indent,
                  A mainAction,
                  Alignment mainAlignment,
                  List<WimGuiRowCell<A>> cells,
                  boolean topGap) {
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
}
