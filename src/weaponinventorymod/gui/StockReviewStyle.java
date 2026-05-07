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
    static final float BLACK_MARKET_BUTTON_WIDTH = 160f;
    static final float CLOSE_BUTTON_WIDTH = 82f;
    static final float BUTTON_GAP = 5f;
    static final float SELLER_BUY_BUTTON_WIDTH = 54f;
    static final float STOCK_CELL_WIDTH = 92f;
    static final float INVENTORY_CELL_WIDTH = 102f;
    static final float PLAN_CELL_WIDTH = 98f;
    static final float COST_CELL_WIDTH = 124f;
    static final float REVIEW_MARKET_CELL_WIDTH = 180f;
    static final float TRADE_STEP_BUTTON_WIDTH = 38f;
    static final float RESET_BUTTON_WIDTH = 58f;
    static final float TRADE_CONTROL_BLOCK_WIDTH = 6f * TRADE_STEP_BUTTON_WIDTH + RESET_BUTTON_WIDTH + 6f * BUTTON_GAP;
    static final float TRADE_ROW_RIGHT_BLOCK_WIDTH = STOCK_CELL_WIDTH + INVENTORY_CELL_WIDTH + STOCK_CELL_WIDTH
            + PLAN_CELL_WIDTH + COST_CELL_WIDTH + TRADE_CONTROL_BLOCK_WIDTH + 11f * BUTTON_GAP;
    static final float REVIEW_ROW_RIGHT_BLOCK_WIDTH = STOCK_CELL_WIDTH + INVENTORY_CELL_WIDTH
            + PLAN_CELL_WIDTH + COST_CELL_WIDTH + 3f * BUTTON_GAP;
    static final float DEBUG_VALUE_WIDTH = 430f;
    static final float DEBUG_SAMPLE_WIDTH = 130f;
    static final float DEBUG_DELTA_BUTTON_WIDTH = 48f;
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
    static final float REVIEW_LIST_WIDTH = LIST_WIDTH - TRADE_CONTROL_BLOCK_WIDTH;
    // Ported from the accepted ACG GUI palette. Hover colors intentionally
    // equal idle colors because Starsector darkens idle buttons differently.
    static Color DEFAULT_TEXT = WimGuiStyle.DEFAULT_TEXT;
    static Color WHITE_TEXT = WimGuiStyle.WHITE_TEXT;
    static Color ALERT_RED = WimGuiStyle.ALERT_RED;
    static Color DISABLED_BACKGROUND = WimGuiStyle.DISABLED_BACKGROUND;
    static Color DISABLED_DARK = WimGuiStyle.DISABLED_DARK;
    static Color DISABLED_TEXT = WimGuiStyle.DISABLED_TEXT;
    static Color DISABLED_BORDER = WimGuiStyle.DISABLED_BORDER;
    static Color SAVE_BUTTON = WimGuiStyle.SAVE_PURPLE;
    static Color LOAD_BUTTON = WimGuiStyle.LOAD_YELLOW;
    static Color CONFIRM_BUTTON = WimGuiStyle.CONFIRM_GREEN;
    static Color CANCEL_BUTTON = WimGuiStyle.CANCEL_RED;
    static Color UNCOLOURED_BUTTON = WimGuiStyle.UNCOLOURED_BUTTON;
    static Color PRESET_SCOPE_BUTTON = WimGuiStyle.PRESET_SCOPE_ORANGE;
    static Color SELECTED_WEAPON_SHIP = WimGuiStyle.SELECTED_BLUE;
    static Color PANEL_HEADING = WimGuiStyle.PANEL_HEADING;
    static Color STALE_HEADING = WimGuiStyle.STALE_HEADING;
    static Color COLLAPSIBLE_HEADING = WimGuiStyle.COLLAPSIBLE_HEADING;
    static Color UNSAVED_HEADING = WimGuiStyle.UNSAVED_HEADING;
    static Color TAG_CATEGORY_HEADING = WimGuiStyle.TAG_CATEGORY_HEADING;

    static Color NO_STOCK = CANCEL_BUTTON;
    static Color INSUFFICIENT = LOAD_BUTTON;
    static Color SUFFICIENT = CONFIRM_BUTTON;
    static Color ROW_BACKGROUND = UNCOLOURED_BUTTON;
    static Color ROW_BACKGROUND_DARK = DISABLED_DARK;
    static Color HEADING_BACKGROUND = COLLAPSIBLE_HEADING;
    static Color CELL_BACKGROUND = COLLAPSIBLE_HEADING;
    static Color PANEL_BACKGROUND = WimGuiStyle.MODAL_PANEL_BACKGROUND;
    static Color PANEL_BORDER = WimGuiStyle.MODAL_PANEL_BORDER;
    static Color ROW_BORDER = WimGuiStyle.ROW_BORDER;
    static Color ACTION_BACKGROUND = UNCOLOURED_BUTTON;
    static Color BUY_BUTTON = CONFIRM_BUTTON;
    static Color SELL_BUTTON = CANCEL_BUTTON;
    static Color BULK_BUTTON = SAVE_BUTTON;
    static Color PLAN_POSITIVE = CONFIRM_BUTTON;
    static Color PLAN_NEGATIVE = CANCEL_BUTTON;
    static Color PLAN_ZERO = LOAD_BUTTON;
    static Color COST_BUTTON = CANCEL_BUTTON;
    static Color PROFIT_BUTTON = CONFIRM_BUTTON;
    static Color SCROLL = DEFAULT_TEXT;
    static Color TEXT = WHITE_TEXT;
    static Color MUTED = WHITE_TEXT;
    static WimGuiModalListSpec LIST = new WimGuiModalListSpec(
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
    static WimGuiModalListSpec REVIEW_LIST = new WimGuiModalListSpec(
            MODAL,
            PAD,
            LIST_TOP,
            REVIEW_LIST_WIDTH,
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

    static void refreshColors() {
        DEFAULT_TEXT = WimGuiStyle.DEFAULT_TEXT;
        WHITE_TEXT = WimGuiStyle.WHITE_TEXT;
        ALERT_RED = WimGuiStyle.ALERT_RED;
        DISABLED_BACKGROUND = WimGuiStyle.DISABLED_BACKGROUND;
        DISABLED_DARK = WimGuiStyle.DISABLED_DARK;
        DISABLED_TEXT = WimGuiStyle.DISABLED_TEXT;
        DISABLED_BORDER = WimGuiStyle.DISABLED_BORDER;
        SAVE_BUTTON = WimGuiStyle.SAVE_PURPLE;
        LOAD_BUTTON = WimGuiStyle.LOAD_YELLOW;
        CONFIRM_BUTTON = WimGuiStyle.CONFIRM_GREEN;
        CANCEL_BUTTON = WimGuiStyle.CANCEL_RED;
        UNCOLOURED_BUTTON = WimGuiStyle.UNCOLOURED_BUTTON;
        PRESET_SCOPE_BUTTON = WimGuiStyle.PRESET_SCOPE_ORANGE;
        SELECTED_WEAPON_SHIP = WimGuiStyle.SELECTED_BLUE;
        PANEL_HEADING = WimGuiStyle.PANEL_HEADING;
        STALE_HEADING = WimGuiStyle.STALE_HEADING;
        COLLAPSIBLE_HEADING = WimGuiStyle.COLLAPSIBLE_HEADING;
        UNSAVED_HEADING = WimGuiStyle.UNSAVED_HEADING;
        TAG_CATEGORY_HEADING = WimGuiStyle.TAG_CATEGORY_HEADING;
        NO_STOCK = CANCEL_BUTTON;
        INSUFFICIENT = LOAD_BUTTON;
        SUFFICIENT = CONFIRM_BUTTON;
        ROW_BACKGROUND = UNCOLOURED_BUTTON;
        ROW_BACKGROUND_DARK = DISABLED_DARK;
        HEADING_BACKGROUND = COLLAPSIBLE_HEADING;
        CELL_BACKGROUND = COLLAPSIBLE_HEADING;
        PANEL_BACKGROUND = WimGuiStyle.MODAL_PANEL_BACKGROUND;
        PANEL_BORDER = WimGuiStyle.MODAL_PANEL_BORDER;
        ROW_BORDER = WimGuiStyle.ROW_BORDER;
        ACTION_BACKGROUND = UNCOLOURED_BUTTON;
        BUY_BUTTON = CONFIRM_BUTTON;
        SELL_BUTTON = CANCEL_BUTTON;
        BULK_BUTTON = SAVE_BUTTON;
        PLAN_POSITIVE = CONFIRM_BUTTON;
        PLAN_NEGATIVE = CANCEL_BUTTON;
        PLAN_ZERO = LOAD_BUTTON;
        COST_BUTTON = CANCEL_BUTTON;
        PROFIT_BUTTON = CONFIRM_BUTTON;
        SCROLL = DEFAULT_TEXT;
        TEXT = WHITE_TEXT;
        MUTED = WHITE_TEXT;
        LIST = new WimGuiModalListSpec(
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
        REVIEW_LIST = new WimGuiModalListSpec(
                MODAL,
                PAD,
                LIST_TOP,
                REVIEW_LIST_WIDTH,
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
    }

    static WimGuiListBounds initialListBounds() {
        return new WimGuiListBounds(0, PAD, LIST_TOP, LIST_WIDTH, LIST_HEIGHT);
    }
}
