package weaponinventorymod.gui;

import java.awt.Color;

final class StockReviewStyle {
    static final float WIDTH = 1180f;
    static final float HEIGHT = 640f;
    static final float PAD = 10f;
    static final float SMALL_PAD = 4f;
    static final float HEADER_HEIGHT = 58f;
    static final float ACTION_ROW_HEIGHT = 28f;
    static final float FOOTER_HEIGHT = 34f;
    static final float MODE_BUTTON_WIDTH = 168f;
    static final float SORT_BUTTON_WIDTH = 140f;
    static final float STORAGE_BUTTON_WIDTH = 178f;
    static final float BLACK_MARKET_BUTTON_WIDTH = 160f;
    static final float CLOSE_BUTTON_WIDTH = 82f;
    static final float SELLER_BUY_BUTTON_WIDTH = 54f;
    static final float STOCK_CELL_WIDTH = 92f;
    static final float INVENTORY_CELL_WIDTH = 102f;
    static final float PLAN_CELL_WIDTH = 98f;
    static final float COST_CELL_WIDTH = 124f;
    static final float REVIEW_MARKET_CELL_WIDTH = 180f;
    static final float TRADE_STEP_BUTTON_WIDTH = 38f;
    static final float RESET_BUTTON_WIDTH = 58f;
    static final float ACTION_BUTTON_HEIGHT = 22f;
    static final float ROW_HEIGHT = 22f;
    static final float ROW_GAP = 3f;
    static final float CATEGORY_TOP_GAP = 8f;
    static final float TEXT_TOP_PAD = WimGuiStyle.TEXT_TOP_PAD;
    static final float TEXT_LEFT_PAD = WimGuiStyle.TEXT_LEFT_PAD;
    static final float WEAPON_INDENT = 18f;
    static final float SECTION_INDENT = 34f;
    static final float DETAIL_INDENT = 52f;
    static final float SELLER_INDENT = 52f;
    static final float BUTTON_GAP = 5f;
    static final float FOOTER_BUTTON_WIDTH = 180f;
    static final float BULK_BUTTON_WIDTH = 210f;
    static final float RESET_ALL_BUTTON_WIDTH = 150f;
    static final int BUTTON_POLL_FRAMES_AFTER_MOUSE_EVENT = 3;
    static final boolean REFRESH_VANILLA_CORE_AFTER_PURCHASE = false;
    static final WimGuiModalLayout MODAL = new WimGuiModalLayout(
            WIDTH,
            HEIGHT,
            PAD,
            PAD,
            HEADER_HEIGHT + SMALL_PAD + ACTION_ROW_HEIGHT,
            FOOTER_HEIGHT,
            ROW_HEIGHT,
            ROW_GAP,
            SMALL_PAD);
    static final float LIST_TOP = MODAL.bodyTop();
    static final float LIST_HEIGHT = MODAL.bodyHeight();
    static final float LIST_WIDTH = MODAL.contentWidth();
    // Ported from the accepted ACG GUI palette. Hover colors intentionally
    // equal idle colors because Starsector darkens idle buttons differently.
    static final Color DEFAULT_TEXT = WimGuiStyle.DEFAULT_TEXT;
    static final Color WHITE_TEXT = WimGuiStyle.WHITE_TEXT;
    static final Color ALERT_RED = WimGuiStyle.ALERT_RED;
    static final Color DISABLED_BACKGROUND = WimGuiStyle.DISABLED_BACKGROUND;
    static final Color DISABLED_DARK = WimGuiStyle.DISABLED_DARK;
    static final Color DISABLED_TEXT = WimGuiStyle.DISABLED_TEXT;
    static final Color DISABLED_BORDER = WimGuiStyle.DISABLED_BORDER;
    static final Color SAVE_BUTTON = WimGuiStyle.SAVE_PURPLE;
    static final Color LOAD_BUTTON = WimGuiStyle.LOAD_YELLOW;
    static final Color CONFIRM_BUTTON = WimGuiStyle.CONFIRM_GREEN;
    static final Color CANCEL_BUTTON = WimGuiStyle.CANCEL_RED;
    static final Color UNCOLOURED_BUTTON = WimGuiStyle.UNCOLOURED_BUTTON;
    static final Color PRESET_SCOPE_BUTTON = WimGuiStyle.PRESET_SCOPE_ORANGE;
    static final Color SELECTED_WEAPON_SHIP = WimGuiStyle.SELECTED_BLUE;
    static final Color PANEL_HEADING = WimGuiStyle.PANEL_HEADING;
    static final Color STALE_HEADING = WimGuiStyle.STALE_HEADING;
    static final Color COLLAPSIBLE_HEADING = WimGuiStyle.COLLAPSIBLE_HEADING;
    static final Color UNSAVED_HEADING = WimGuiStyle.UNSAVED_HEADING;
    static final Color TAG_CATEGORY_HEADING = WimGuiStyle.TAG_CATEGORY_HEADING;

    static final Color NO_STOCK = CANCEL_BUTTON;
    static final Color INSUFFICIENT = LOAD_BUTTON;
    static final Color SUFFICIENT = CONFIRM_BUTTON;
    static final Color ROW_BACKGROUND = UNCOLOURED_BUTTON;
    static final Color ROW_BACKGROUND_DARK = DISABLED_DARK;
    static final Color HEADING_BACKGROUND = COLLAPSIBLE_HEADING;
    static final Color CELL_BACKGROUND = COLLAPSIBLE_HEADING;
    static final Color PANEL_BACKGROUND = WimGuiStyle.MODAL_PANEL_BACKGROUND;
    static final Color PANEL_BORDER = WimGuiStyle.MODAL_PANEL_BORDER;
    static final Color ROW_BORDER = WimGuiStyle.ROW_BORDER;
    static final Color ACTION_BACKGROUND = UNCOLOURED_BUTTON;
    static final Color BUY_BUTTON = CONFIRM_BUTTON;
    static final Color SELL_BUTTON = CANCEL_BUTTON;
    static final Color BULK_BUTTON = SAVE_BUTTON;
    static final Color PLAN_POSITIVE = CONFIRM_BUTTON;
    static final Color PLAN_NEGATIVE = CANCEL_BUTTON;
    static final Color PLAN_ZERO = LOAD_BUTTON;
    static final Color COST_BUTTON = CANCEL_BUTTON;
    static final Color PROFIT_BUTTON = CONFIRM_BUTTON;
    static final Color SCROLL = DEFAULT_TEXT;
    static final Color TEXT = WHITE_TEXT;
    static final Color MUTED = WHITE_TEXT;
    static final WimGuiModalListSpec LIST = new WimGuiModalListSpec(
            MODAL,
            PAD,
            LIST_TOP,
            LIST_WIDTH,
            LIST_HEIGHT,
            ROW_HEIGHT,
            ACTION_BUTTON_HEIGHT,
            ROW_GAP,
            SMALL_PAD,
            BUTTON_GAP,
            TEXT_LEFT_PAD,
            80f,
            PANEL_BACKGROUND,
            PANEL_BORDER,
            ROW_BORDER);

    private StockReviewStyle() {
    }

    static WimGuiListBounds initialListBounds() {
        return new WimGuiListBounds(0, PAD, LIST_TOP, LIST_WIDTH, LIST_HEIGHT);
    }
}
