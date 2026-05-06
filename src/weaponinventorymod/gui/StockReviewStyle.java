package weaponinventorymod.gui;

import com.fs.starfarer.api.util.Misc;

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

    static final Color NO_STOCK = new Color(242, 90, 90);
    static final Color INSUFFICIENT = new Color(255, 216, 61);
    static final Color SUFFICIENT = new Color(75, 203, 99);
    static final Color ROW_BACKGROUND = new Color(38, 38, 38, 230);
    static final Color ROW_BACKGROUND_DARK = new Color(24, 24, 24, 230);
    static final Color HEADING_BACKGROUND = new Color(18, 18, 18, 245);
    static final Color PANEL_BACKGROUND = new Color(0, 0, 0, 220);
    static final Color PANEL_BORDER = new Color(210, 210, 210, 220);
    static final Color ACTION_BACKGROUND = new Color(10, 10, 10, 230);
    static final Color ACTION_HOVER = new Color(95, 95, 95, 255);
    static final Color DISABLED_TEXT = new Color(105, 105, 105);
    static final Color DISABLED_BACKGROUND = new Color(0, 0, 0, 235);
    static final Color SCROLL = new Color(150, 150, 150);
    static final Color TEXT = Misc.getTextColor();
    static final Color MUTED = Misc.getGrayColor();

    private StockReviewStyle() {
    }
}
