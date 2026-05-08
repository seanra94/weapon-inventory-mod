package weaponinventorymod.gui;

import com.fs.starfarer.api.ui.Alignment;

import java.awt.Color;

final class WimGuiButtonSpec<A> {
    final float width;
    final String label;
    final Color textColor;
    final A action;
    final boolean enabled;
    final Alignment alignment;
    final WimGuiButtonColors colors;
    final Color borderColor;
    final String tooltip;

    WimGuiButtonSpec(float width,
                     String label,
                     Color textColor,
                     A action,
                     boolean enabled,
                     Alignment alignment,
                     WimGuiButtonColors colors,
                     Color borderColor,
                     String tooltip) {
        this.width = width;
        this.label = label;
        this.textColor = textColor;
        this.action = action;
        this.enabled = enabled;
        this.alignment = alignment;
        this.colors = colors;
        this.borderColor = borderColor;
        this.tooltip = tooltip;
    }

    static <A> WimGuiButtonSpec<A> sameColor(float width,
                                             String label,
                                             Color textColor,
                                             A action,
                                             boolean enabled,
                                             Alignment alignment,
                                             Color color,
                                             Color borderColor) {
        return dimmedInner(width, label, textColor, action, enabled, alignment, color, borderColor);
    }

    static <A> WimGuiButtonSpec<A> sameColor(float width,
                                             String label,
                                             Color textColor,
                                             A action,
                                             boolean enabled,
                                             Alignment alignment,
                                             Color color,
                                             Color borderColor,
                                             String tooltip) {
        return dimmedInner(width, label, textColor, action, enabled, alignment, color, borderColor, tooltip);
    }

    static <A> WimGuiButtonSpec<A> dimmedInner(float width,
                                               String label,
                                               Color textColor,
                                               A action,
                                               boolean enabled,
                                               Alignment alignment,
                                               Color color,
                                               Color borderColor) {
        return dimmedInner(width, label, textColor, action, enabled, alignment, color, borderColor, null);
    }

    static <A> WimGuiButtonSpec<A> dimmedInner(float width,
                                               String label,
                                               Color textColor,
                                               A action,
                                               boolean enabled,
                                               Alignment alignment,
                                               Color color,
                                               Color borderColor,
                                               String tooltip) {
        return new WimGuiButtonSpec<A>(
                width,
                label,
                textColor,
                action,
                enabled,
                alignment,
                WimGuiButtonColors.dimmedInner(color),
                borderColor,
                tooltip);
    }

    static <A> WimGuiButtonSpec<A> toggle(float width,
                                          String label,
                                          Color textColor,
                                          A action,
                                          Alignment alignment,
                                          Color color,
                                          Color borderColor) {
        return dimmedInner(width, label, textColor, action, true, alignment, color, borderColor);
    }

    static <A> WimGuiButtonSpec<A> semantic(float width,
                                            String label,
                                            A action,
                                            boolean enabled,
                                            Color enabledFill,
                                            Color borderColor) {
        return semantic(width, label, action, enabled, enabledFill, borderColor, null);
    }

    static <A> WimGuiButtonSpec<A> semantic(float width,
                                            String label,
                                            A action,
                                            boolean enabled,
                                            Color enabledFill,
                                            Color borderColor,
                                            String tooltip) {
        return sameColor(
                width,
                label,
                enabled ? WimGuiStyle.WHITE_TEXT : WimGuiStyle.DISABLED_TEXT,
                action,
                enabled,
                Alignment.MID,
                enabled ? enabledFill : WimGuiStyle.DISABLED_BACKGROUND,
                borderColor,
                tooltip);
    }
}
