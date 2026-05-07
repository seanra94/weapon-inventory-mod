package weaponinventorymod.gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

final class WimGuiRowCell<A> {
    private final String label;
    private final float width;
    private final Color fillColor;
    private final Color textColor;
    private final A action;
    private final boolean enabled;

    private WimGuiRowCell(String label,
                          float width,
                          Color fillColor,
                          Color textColor,
                          A action,
                          boolean enabled) {
        this.label = label;
        this.width = width;
        this.fillColor = fillColor;
        this.textColor = textColor;
        this.action = action;
        this.enabled = enabled;
    }

    static <A> WimGuiRowCell<A> info(String label, float width, Color fillColor, Color textColor) {
        return new WimGuiRowCell<A>(label, width, fillColor, textColor, null, true);
    }

    static <A> WimGuiRowCell<A> action(String label,
                                       float width,
                                       Color fillColor,
                                       Color enabledTextColor,
                                       Color disabledTextColor,
                                       A action,
                                       boolean enabled) {
        return new WimGuiRowCell<A>(label, width, fillColor, enabled ? enabledTextColor : disabledTextColor, action, enabled);
    }

    static <A> WimGuiRowCell<A> standardAction(String label,
                                               float width,
                                               Color fillColor,
                                               A action,
                                               boolean enabled) {
        return action(
                label,
                width,
                fillColor,
                WimGuiStyle.WHITE_TEXT,
                WimGuiStyle.DISABLED_TEXT,
                action,
                enabled);
    }

    @SafeVarargs
    static <A> List<WimGuiRowCell<A>> of(WimGuiRowCell<A>... cells) {
        List<WimGuiRowCell<A>> result = new ArrayList<WimGuiRowCell<A>>();
        if (cells == null) {
            return result;
        }
        for (int i = 0; i < cells.length; i++) {
            if (cells[i] != null) {
                result.add(cells[i]);
            }
        }
        return result;
    }

    String getLabel() {
        return label;
    }

    float getWidth() {
        return width;
    }

    Color getFillColor() {
        return fillColor;
    }

    Color getTextColor() {
        return textColor;
    }

    A getAction() {
        return action;
    }

    boolean isEnabled() {
        return enabled;
    }

    boolean isAction() {
        return action != null;
    }

    static float totalWidth(List<? extends WimGuiRowCell<?>> cells, float gap) {
        if (cells == null || cells.isEmpty()) {
            return 0f;
        }
        float result = 0f;
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                result += gap;
            }
            result += cells.get(i).getWidth();
        }
        return result;
    }
}
