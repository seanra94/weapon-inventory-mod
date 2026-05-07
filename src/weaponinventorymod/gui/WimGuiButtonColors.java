package weaponinventorymod.gui;

import java.awt.Color;

final class WimGuiButtonColors {
    final Color idle;
    final Color hover;

    WimGuiButtonColors(Color idle, Color hover) {
        this.idle = idle;
        this.hover = hover == null ? idle : hover;
    }

    static WimGuiButtonColors dimmedInner(Color color) {
        return new WimGuiButtonColors(color, color);
    }

    static WimGuiButtonColors same(Color color) {
        return dimmedInner(color);
    }
}
