package weaponsprocurement.gui

import com.fs.starfarer.api.Global
import java.awt.Color

class StockReviewStyle private constructor() {
    companion object {
        const val SHOW_WIDTH_TEST_ROWS = false

        private const val FALLBACK_WIDTH = 1180f
        private const val FALLBACK_HEIGHT = 640f
        private const val MIN_FULLSCREEN_WIDTH = 900f
        private const val MIN_FULLSCREEN_HEIGHT = 520f
        private const val SCREEN_EDGE_X_MARGIN = 24f
        private const val SCREEN_EDGE_Y_MARGIN = 34f

        @JvmField val WIDTH = fullscreenWidth()
        const val PAD = 10f
        const val SMALL_PAD = 4f
        const val SECTION_GAP = PAD
        const val HEADER_HEIGHT = 58f
        const val ACTION_ROW_HEIGHT = 28f
        const val FOOTER_HEIGHT = 34f
        const val SORT_BUTTON_WIDTH = 140f
        const val SOURCE_BUTTON_WIDTH = 170f
        const val BLACK_MARKET_BUTTON_WIDTH = 160f
        const val FILTER_BUTTON_WIDTH = 108f
        const val COLOR_BUTTON_WIDTH = 82f
        const val BUTTON_GAP = 5f
        const val STOCK_CELL_WIDTH = 104f
        const val INVENTORY_CELL_WIDTH = 102f
        const val PLAN_CELL_WIDTH = 184f
        const val PRICE_CELL_WIDTH = 120f
        const val TRADE_STEP_BUTTON_WIDTH = 38f
        const val SUFFICIENT_BUTTON_WIDTH = 82f
        const val RESET_BUTTON_WIDTH = 58f
        const val TRADE_CONTROL_BLOCK_WIDTH = 4f * TRADE_STEP_BUTTON_WIDTH + SUFFICIENT_BUTTON_WIDTH + RESET_BUTTON_WIDTH + 5f * BUTTON_GAP
        const val TRADE_ROW_RIGHT_BLOCK_WIDTH = STOCK_CELL_WIDTH + PRICE_CELL_WIDTH + PLAN_CELL_WIDTH + TRADE_CONTROL_BLOCK_WIDTH + 3f * BUTTON_GAP
        const val REVIEW_STOCK_CELL_WIDTH = STOCK_CELL_WIDTH
        const val REVIEW_ROW_RIGHT_BLOCK_WIDTH = REVIEW_STOCK_CELL_WIDTH + PLAN_CELL_WIDTH + BUTTON_GAP
        @JvmField val REVIEW_WIDTH = WIDTH
        const val DEBUG_VALUE_WIDTH = 430f
        const val DEBUG_SAMPLE_WIDTH = 130f
        const val DEBUG_DELTA_BUTTON_WIDTH = 48f
        const val ACTION_BUTTON_HEIGHT = 22f
        const val ROW_HEIGHT = 22f
        const val ROW_GAP = 4f
        const val CATEGORY_TOP_GAP = ROW_GAP
        const val SUMMARY_ROW_GAP = 4f
        const val SUMMARY_ROW_COUNT = 4
        const val SUMMARY_HEIGHT = SUMMARY_ROW_COUNT * ROW_HEIGHT + (SUMMARY_ROW_COUNT - 1) * SUMMARY_ROW_GAP
        const val TRADE_ACTION_ROW_TOP = PAD
        const val TRADE_LIST_TOP = TRADE_ACTION_ROW_TOP + ACTION_BUTTON_HEIGHT + SECTION_GAP
        const val REVIEW_LIST_TOP = TRADE_LIST_TOP
        @JvmField val HEIGHT = fullscreenHeight()
        @JvmField val SUMMARY_TOP = HEIGHT - PAD - ACTION_BUTTON_HEIGHT - SECTION_GAP - SUMMARY_HEIGHT
        @JvmField val TRADE_LIST_HEIGHT = Math.max(ROW_HEIGHT + 2f * SMALL_PAD, SUMMARY_TOP - SECTION_GAP - TRADE_LIST_TOP)
        @JvmField val REVIEW_LIST_HEIGHT = TRADE_LIST_HEIGHT
        const val TEXT_TOP_PAD = WimGuiStyle.TEXT_TOP_PAD
        const val TEXT_LEFT_PAD = WimGuiStyle.TEXT_LEFT_PAD
        const val WEAPON_INDENT = 18f
        const val SECTION_INDENT = 2f * WEAPON_INDENT
        const val DETAIL_INDENT = 3f * WEAPON_INDENT
        const val DATA_INDENT = 4f * WEAPON_INDENT
        const val FOOTER_BUTTON_WIDTH = 180f
        const val BULK_BUTTON_WIDTH = 210f
        const val RESET_ALL_BUTTON_WIDTH = 150f
        const val BUTTON_POLL_FRAMES_AFTER_MOUSE_EVENT = 3
        const val REFRESH_VANILLA_CORE_AFTER_PURCHASE = false

        @JvmField val MODAL = WimGuiModalLayout(WIDTH, HEIGHT, PAD, PAD, HEADER_HEIGHT + SMALL_PAD + ACTION_ROW_HEIGHT, FOOTER_HEIGHT, ROW_HEIGHT, ROW_GAP, SMALL_PAD)
        @JvmField val REVIEW_MODAL = WimGuiModalLayout(REVIEW_WIDTH, HEIGHT, PAD, PAD, 0f, FOOTER_HEIGHT, ROW_HEIGHT, ROW_GAP, SMALL_PAD)
        @JvmField val LIST_TOP = MODAL.bodyTop()
        @JvmField val LIST_HEIGHT = MODAL.bodyHeight()
        @JvmField val LIST_WIDTH = MODAL.contentWidth()
        @JvmField val FILTER_LIST_WIDTH = LIST_WIDTH / 4f
        @JvmField val REVIEW_LIST_WIDTH = REVIEW_MODAL.contentWidth()

        // Ported from the accepted ACG GUI palette. Hover colors intentionally
        // equal idle colors because Starsector darkens idle buttons differently.
        @JvmField var DEFAULT_TEXT: Color = WimGuiStyle.DEFAULT_TEXT
        @JvmField var WHITE_TEXT: Color = WimGuiStyle.WHITE_TEXT
        @JvmField var ALERT_RED: Color = WimGuiStyle.ALERT_RED
        @JvmField var DISABLED_BACKGROUND: Color = WimGuiStyle.DISABLED_BACKGROUND
        @JvmField var DISABLED_DARK: Color = WimGuiStyle.DISABLED_DARK
        @JvmField var DISABLED_TEXT: Color = WimGuiStyle.DISABLED_TEXT
        @JvmField var DISABLED_BORDER: Color = WimGuiStyle.DISABLED_BORDER
        @JvmField var SAVE_BUTTON: Color = WimGuiStyle.SAVE_PURPLE
        @JvmField var LOAD_BUTTON: Color = WimGuiStyle.LOAD_YELLOW
        @JvmField var CONFIRM_BUTTON: Color = WimGuiStyle.CONFIRM_GREEN
        @JvmField var CANCEL_BUTTON: Color = WimGuiStyle.CANCEL_RED
        @JvmField var UNCOLOURED_BUTTON: Color = WimGuiStyle.UNCOLOURED_BUTTON
        @JvmField var PRESET_SCOPE_BUTTON: Color = WimGuiStyle.PRESET_SCOPE_ORANGE
        @JvmField var SELECTED_WEAPON_SHIP: Color = WimGuiStyle.SELECTED_BLUE
        @JvmField var PANEL_HEADING: Color = WimGuiStyle.PANEL_HEADING
        @JvmField var STALE_HEADING: Color = WimGuiStyle.STALE_HEADING
        @JvmField var COLLAPSIBLE_HEADING: Color = WimGuiStyle.COLLAPSIBLE_HEADING
        @JvmField var UNSAVED_HEADING: Color = WimGuiStyle.UNSAVED_HEADING
        @JvmField var TAG_CATEGORY_HEADING: Color = WimGuiStyle.TAG_CATEGORY_HEADING
        @JvmField var NO_STOCK: Color = CANCEL_BUTTON
        @JvmField var INSUFFICIENT: Color = LOAD_BUTTON
        @JvmField var SUFFICIENT: Color = CONFIRM_BUTTON
        @JvmField var ROW_BACKGROUND: Color = UNCOLOURED_BUTTON
        @JvmField var ROW_BACKGROUND_DARK: Color = DISABLED_DARK
        @JvmField var HEADING_BACKGROUND: Color = COLLAPSIBLE_HEADING
        @JvmField var CELL_BACKGROUND: Color = COLLAPSIBLE_HEADING
        @JvmField var PANEL_BACKGROUND: Color = WimGuiStyle.MODAL_PANEL_BACKGROUND
        @JvmField var PANEL_BORDER: Color = WimGuiStyle.MODAL_PANEL_BORDER
        @JvmField var ROW_BORDER: Color = WimGuiStyle.ROW_BORDER
        @JvmField var ACTION_BACKGROUND: Color = HEADING_BACKGROUND
        @JvmField var BUY_BUTTON: Color = LOAD_BUTTON
        @JvmField var SELL_BUTTON: Color = SAVE_BUTTON
        @JvmField var BULK_BUTTON: Color = SAVE_BUTTON
        @JvmField var FILTER_ACTIVE: Color = PRESET_SCOPE_BUTTON
        @JvmField var PLAN_POSITIVE: Color = BUY_BUTTON
        @JvmField var PLAN_NEGATIVE: Color = SELL_BUTTON
        @JvmField var PLAN_ZERO: Color = CELL_BACKGROUND
        @JvmField var PROFIT_BUTTON: Color = CONFIRM_BUTTON
        @JvmField var SCROLL: Color = DEFAULT_TEXT
        @JvmField var TEXT: Color = WHITE_TEXT
        @JvmField var MUTED: Color = WHITE_TEXT

        @JvmField var LIST = listSpec(MODAL, PAD, LIST_TOP, LIST_WIDTH, LIST_HEIGHT)
        @JvmField var TRADE_LIST = listSpec(MODAL, PAD, TRADE_LIST_TOP, LIST_WIDTH, TRADE_LIST_HEIGHT)
        @JvmField var FILTER_LIST = listSpec(MODAL, PAD, LIST_TOP, FILTER_LIST_WIDTH, LIST_HEIGHT)
        @JvmField var REVIEW_LIST = listSpec(REVIEW_MODAL, PAD, REVIEW_LIST_TOP, REVIEW_LIST_WIDTH, REVIEW_LIST_HEIGHT)

        private fun fullscreenWidth(): Float = screenDimension(true, FALLBACK_WIDTH, MIN_FULLSCREEN_WIDTH, SCREEN_EDGE_X_MARGIN)

        private fun fullscreenHeight(): Float = screenDimension(false, FALLBACK_HEIGHT, MIN_FULLSCREEN_HEIGHT, SCREEN_EDGE_Y_MARGIN)

        private fun screenDimension(width: Boolean, fallback: Float, minimum: Float, edgeMargin: Float): Float {
            return try {
                val screen = if (width) Global.getSettings().screenWidth else Global.getSettings().screenHeight
                if (screen > 0f && !screen.isNaN() && !screen.isInfinite()) {
                    Math.max(minimum, screen - 2f * edgeMargin)
                } else {
                    fallback
                }
            } catch (_: RuntimeException) {
                fallback
            }
        }

        @JvmStatic
        fun refreshColors() {
            DEFAULT_TEXT = WimGuiStyle.DEFAULT_TEXT
            WHITE_TEXT = WimGuiStyle.WHITE_TEXT
            ALERT_RED = WimGuiStyle.ALERT_RED
            DISABLED_BACKGROUND = WimGuiStyle.DISABLED_BACKGROUND
            DISABLED_DARK = WimGuiStyle.DISABLED_DARK
            DISABLED_TEXT = WimGuiStyle.DISABLED_TEXT
            DISABLED_BORDER = WimGuiStyle.DISABLED_BORDER
            SAVE_BUTTON = WimGuiStyle.SAVE_PURPLE
            LOAD_BUTTON = WimGuiStyle.LOAD_YELLOW
            CONFIRM_BUTTON = WimGuiStyle.CONFIRM_GREEN
            CANCEL_BUTTON = WimGuiStyle.CANCEL_RED
            UNCOLOURED_BUTTON = WimGuiStyle.UNCOLOURED_BUTTON
            PRESET_SCOPE_BUTTON = WimGuiStyle.PRESET_SCOPE_ORANGE
            SELECTED_WEAPON_SHIP = WimGuiStyle.SELECTED_BLUE
            PANEL_HEADING = WimGuiStyle.PANEL_HEADING
            STALE_HEADING = WimGuiStyle.STALE_HEADING
            COLLAPSIBLE_HEADING = WimGuiStyle.COLLAPSIBLE_HEADING
            UNSAVED_HEADING = WimGuiStyle.UNSAVED_HEADING
            TAG_CATEGORY_HEADING = WimGuiStyle.TAG_CATEGORY_HEADING
            NO_STOCK = CANCEL_BUTTON
            INSUFFICIENT = LOAD_BUTTON
            SUFFICIENT = CONFIRM_BUTTON
            ROW_BACKGROUND = UNCOLOURED_BUTTON
            ROW_BACKGROUND_DARK = DISABLED_DARK
            HEADING_BACKGROUND = COLLAPSIBLE_HEADING
            CELL_BACKGROUND = COLLAPSIBLE_HEADING
            PANEL_BACKGROUND = WimGuiStyle.MODAL_PANEL_BACKGROUND
            PANEL_BORDER = WimGuiStyle.MODAL_PANEL_BORDER
            ROW_BORDER = WimGuiStyle.ROW_BORDER
            ACTION_BACKGROUND = HEADING_BACKGROUND
            BUY_BUTTON = LOAD_BUTTON
            SELL_BUTTON = SAVE_BUTTON
            BULK_BUTTON = SAVE_BUTTON
            FILTER_ACTIVE = PRESET_SCOPE_BUTTON
            PLAN_POSITIVE = BUY_BUTTON
            PLAN_NEGATIVE = SELL_BUTTON
            PLAN_ZERO = CELL_BACKGROUND
            PROFIT_BUTTON = CONFIRM_BUTTON
            SCROLL = DEFAULT_TEXT
            TEXT = WHITE_TEXT
            MUTED = WHITE_TEXT
            LIST = listSpec(MODAL, PAD, LIST_TOP, LIST_WIDTH, LIST_HEIGHT)
            TRADE_LIST = listSpec(MODAL, PAD, TRADE_LIST_TOP, LIST_WIDTH, TRADE_LIST_HEIGHT)
            FILTER_LIST = listSpec(MODAL, PAD, LIST_TOP, FILTER_LIST_WIDTH, LIST_HEIGHT)
            REVIEW_LIST = listSpec(REVIEW_MODAL, PAD, REVIEW_LIST_TOP, REVIEW_LIST_WIDTH, REVIEW_LIST_HEIGHT)
        }

        private fun listSpec(
            modal: WimGuiModalLayout,
            panelLeft: Float,
            panelTop: Float,
            panelWidth: Float,
            panelHeight: Float,
        ): WimGuiModalListSpec = WimGuiModalListSpec(
            modal,
            panelLeft,
            panelTop,
            panelWidth,
            panelHeight,
            ROW_HEIGHT,
            ACTION_BUTTON_HEIGHT,
            ROW_GAP,
            SMALL_PAD,
            BUTTON_GAP,
            TEXT_LEFT_PAD,
            80f,
            PANEL_BACKGROUND,
            PANEL_BORDER,
            ROW_BORDER,
        )

        @JvmStatic
        fun initialListBounds(): WimGuiListBounds = WimGuiListBounds(0, PAD, TRADE_LIST_TOP, LIST_WIDTH, TRADE_LIST_HEIGHT)

        @JvmStatic
        fun initialListBounds(reviewMode: Boolean): WimGuiListBounds =
            if (reviewMode) WimGuiListBounds(0, PAD, REVIEW_LIST_TOP, REVIEW_LIST_WIDTH, REVIEW_LIST_HEIGHT) else initialListBounds()

        @JvmStatic
        fun widthFor(reviewMode: Boolean): Float = if (reviewMode) REVIEW_WIDTH else WIDTH
    }
}
