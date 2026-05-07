package weaponinventorymod.gui;

import java.awt.Color;

final class WimGuiStyle {
    static final float BUTTON_IDLE_DIM_MULT = 0.55f;
    static final float TEXT_TOP_PAD = 3f;
    static final float TEXT_LEFT_PAD = 8f;
    static final float TEXT_APPROX_CHAR_WIDTH = 6.8f;
    static final float TEXT_LINE_HEIGHT = 15f;
    static final float TEXT_VERTICAL_PADDING = 8f;

    static final Color DEFAULT_TEXT = new Color(220, 220, 220);
    static final Color WHITE_TEXT = Color.WHITE;
    static final Color UNCOLOURED_BUTTON = new Color(0, 0, 0, 225);
    static final Color DISABLED_BACKGROUND = new Color(28, 28, 28, 235);
    static final Color DISABLED_DARK = new Color(18, 18, 18, 235);
    static final Color DISABLED_TEXT = new Color(120, 120, 120);
    static final Color DISABLED_BORDER = new Color(95, 95, 95);
    static final Color ALERT_RED = new Color(245, 95, 85, 240);
    static final Color SAVE_PURPLE = new Color(146, 126, 160, 225);
    static final Color LOAD_YELLOW = new Color(188, 178, 86, 225);
    static final Color CONFIRM_GREEN = new Color(144, 180, 148, 225);
    static final Color CANCEL_RED = new Color(218, 142, 140, 225);
    static final Color PRESET_SCOPE_ORANGE = new Color(202, 146, 112, 225);
    static final Color SELECTED_BLUE = new Color(52, 78, 88, 225);
    static final Color PANEL_HEADING = new Color(40, 40, 40, 225);
    static final Color STALE_HEADING = new Color(120, 120, 120, 225);
    static final Color COLLAPSIBLE_HEADING = new Color(80, 80, 80, 225);
    static final Color UNSAVED_HEADING = new Color(220, 220, 220, 225);
    static final Color TAG_CATEGORY_HEADING = new Color(210, 165, 185, 225);
    static final Color MODAL_PANEL_BACKGROUND = new Color(0, 0, 0, 220);
    static final Color MODAL_PANEL_BORDER = new Color(210, 210, 210, 220);
    static final Color ROW_BORDER = new Color(210, 210, 210, 220);

    private WimGuiStyle() {
    }

    static Color dimForIdle(Color color) {
        if (color == null) {
            return DISABLED_DARK;
        }
        return new Color(
                clampColor(Math.round(color.getRed() * BUTTON_IDLE_DIM_MULT)),
                clampColor(Math.round(color.getGreen() * BUTTON_IDLE_DIM_MULT)),
                clampColor(Math.round(color.getBlue() * BUTTON_IDLE_DIM_MULT)),
                color.getAlpha());
    }

    private static int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
