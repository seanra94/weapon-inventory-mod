package weaponinventorymod.gui;

import com.fs.starfarer.api.util.Misc;

import java.awt.Color;

final class StockReviewStyle {
    static final float WIDTH = 880f;
    static final float HEIGHT = 640f;
    static final float PAD = 10f;
    static final float SMALL_PAD = 4f;
    static final float HEADING_HEIGHT = 24f;
    static final float ACTION_BUTTON_WIDTH = 110f;
    static final float WIDE_ACTION_BUTTON_WIDTH = 230f;
    static final float BUY_BUTTON_WIDTH = 68f;
    static final float ACTION_BUTTON_HEIGHT = 22f;
    static final float CATEGORY_BUTTON_WIDTH = 820f;
    static final float CATEGORY_BUTTON_HEIGHT = 24f;
    static final float CATEGORY_GAP = 12f;
    static final float WEAPON_ROW_WIDTH = 635f;
    static final float WEAPON_ROW_HEIGHT = 22f;
    static final float SECTION_ROW_WIDTH = 700f;
    static final float SECTION_ROW_HEIGHT = 20f;
    static final int MAX_VISIBLE_ROWS_PER_CATEGORY = 80;

    static final Color NO_STOCK = new Color(242, 90, 90);
    static final Color INSUFFICIENT = new Color(255, 216, 61);
    static final Color SUFFICIENT = new Color(75, 203, 99);
    static final Color ROW_BACKGROUND = new Color(38, 38, 38);
    static final Color ROW_BACKGROUND_DARK = new Color(24, 24, 24);
    static final Color HEADING_BACKGROUND = new Color(18, 18, 18);
    static final Color TEXT = Misc.getTextColor();
    static final Color MUTED = Misc.getGrayColor();

    private StockReviewStyle() {
    }
}
