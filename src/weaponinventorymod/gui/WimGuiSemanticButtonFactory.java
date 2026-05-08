package weaponinventorymod.gui;

import java.awt.Color;

final class WimGuiSemanticButtonFactory<A> {
    private final Color borderColor;

    WimGuiSemanticButtonFactory(Color borderColor) {
        this.borderColor = borderColor;
    }

    WimGuiButtonSpec<A> button(float width,
                               String label,
                               A action,
                               boolean enabled,
                               Color fillColor) {
        return button(width, label, action, enabled, fillColor, null);
    }

    WimGuiButtonSpec<A> button(float width,
                               String label,
                               A action,
                               boolean enabled,
                               Color fillColor,
                               String tooltip) {
        return WimGuiButtonSpec.semantic(width, label, action, enabled, fillColor, borderColor, tooltip);
    }

    WimGuiButtonSpec<A> enabledButton(float width,
                                      String label,
                                      A action,
                                      Color fillColor) {
        return button(width, label, action, true, fillColor);
    }

    WimGuiButtonSpec<A> enabledButton(float width,
                                      String label,
                                      A action,
                                      Color fillColor,
                                      String tooltip) {
        return button(width, label, action, true, fillColor, tooltip);
    }
}
