package weaponinventorymod.gui;

import java.awt.Color;

final class StockReviewStyle {
    static final float WIDTH = 880f;
    static final float HEIGHT = 640f;
    static final float PAD = 10f;
    static final float SMALL_PAD = 4f;
    static final float HEADER_HEIGHT = 58f;
    static final float ACTION_ROW_HEIGHT = 28f;
    static final float LIST_TOP = PAD + HEADER_HEIGHT + SMALL_PAD + ACTION_ROW_HEIGHT + PAD;
    static final float LIST_HEIGHT = HEIGHT - LIST_TOP - PAD;
    static final float LIST_WIDTH = WIDTH - 2f * PAD;
    static final float REFRESH_BUTTON_WIDTH = 82f;
    static final float MODE_BUTTON_WIDTH = 168f;
    static final float SORT_BUTTON_WIDTH = 140f;
    static final float STORAGE_BUTTON_WIDTH = 178f;
    static final float BLACK_MARKET_BUTTON_WIDTH = 160f;
    static final float CLOSE_BUTTON_WIDTH = 82f;
    static final float BUY_BUTTON_WIDTH = 68f;
    static final float ACTION_BUTTON_HEIGHT = 22f;
    static final float ROW_HEIGHT = 22f;
    static final float ROW_GAP = 3f;
    static final float CATEGORY_TOP_GAP = 8f;
    static final float TEXT_TOP_PAD = 3f;
    static final float TEXT_LEFT_PAD = 8f;
    static final float WEAPON_INDENT = 18f;
    static final float SECTION_INDENT = 34f;
    static final float DETAIL_INDENT = 52f;
    static final float SELLER_INDENT = 52f;
    static final float BUTTON_GAP = 5f;
    static final int SCROLL_STEP = 3;
    static final boolean REFRESH_VANILLA_CORE_AFTER_PURCHASE = false;

    // Ported from the accepted ACG GUI palette. Hover colors intentionally
    // equal idle colors because Starsector darkens idle buttons differently.
    static final Color DEFAULT_TEXT = new Color(220, 220, 220);
    static final Color WHITE_TEXT = Color.WHITE;
    static final Color ALERT_RED = new Color(245, 95, 85, 240);
    static final Color DISABLED_BACKGROUND = new Color(28, 28, 28, 235);
    static final Color DISABLED_DARK = new Color(18, 18, 18, 235);
    static final Color DISABLED_TEXT = new Color(120, 120, 120);
    static final Color DISABLED_BORDER = new Color(95, 95, 95);
    static final Color SAVE_BUTTON = new Color(146, 126, 160, 225);
    static final Color LOAD_BUTTON = new Color(188, 178, 86, 225);
    static final Color CONFIRM_BUTTON = new Color(144, 180, 148, 225);
    static final Color CANCEL_BUTTON = new Color(218, 142, 140, 225);
    static final Color UNCOLOURED_BUTTON = new Color(0, 0, 0, 225);
    static final Color PRESET_SCOPE_BUTTON = new Color(202, 146, 112, 225);
    static final Color SELECTED_WEAPON_SHIP = new Color(52, 78, 88, 225);
    static final Color PANEL_HEADING = new Color(40, 40, 40, 225);
    static final Color STALE_HEADING = new Color(120, 120, 120, 225);
    static final Color COLLAPSIBLE_HEADING = new Color(80, 80, 80, 225);
    static final Color UNSAVED_HEADING = new Color(220, 220, 220, 225);
    static final Color TAG_CATEGORY_HEADING = new Color(210, 165, 185, 225);

    static final Color NO_STOCK = CANCEL_BUTTON;
    static final Color INSUFFICIENT = LOAD_BUTTON;
    static final Color SUFFICIENT = CONFIRM_BUTTON;
    static final Color ROW_BACKGROUND = UNCOLOURED_BUTTON;
    static final Color ROW_BACKGROUND_DARK = DISABLED_DARK;
    static final Color HEADING_BACKGROUND = COLLAPSIBLE_HEADING;
    static final Color PANEL_BACKGROUND = new Color(0, 0, 0, 220);
    static final Color PANEL_BORDER = new Color(210, 210, 210, 220);
    static final Color ACTION_BACKGROUND = UNCOLOURED_BUTTON;
    static final Color ACTION_HOVER = UNCOLOURED_BUTTON;
    static final Color BUY_BUTTON = LOAD_BUTTON;
    static final Color SCROLL = DEFAULT_TEXT;
    static final Color TEXT = WHITE_TEXT;
    static final Color MUTED = WHITE_TEXT;

    private StockReviewStyle() {
    }
}
