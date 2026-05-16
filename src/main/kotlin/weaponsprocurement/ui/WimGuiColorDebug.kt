package weaponsprocurement.ui

import com.fs.starfarer.api.Global
import org.apache.log4j.Logger
import org.json.JSONObject
import weaponsprocurement.ui.stockreview.StockReviewStyle
import java.awt.Color
import java.util.Collections

object WimGuiColorDebug {
    class Target(
        val key: String,
        val label: String,
        val defaultColor: Color,
    ) {
        fun apply(color: Color?) {
            val normalized = normalize(color, defaultColor.alpha)
            setColor(key, normalized)
            StockReviewStyle.refreshColors()
        }
    }

    private val LOG: Logger = Logger.getLogger(WimGuiColorDebug::class.java)
    private const val COMMON_FILE = "WP_debugGuiColors.json"
    private val TARGETS: List<Target> = buildTargets()
    private var loaded = false

    @JvmStatic
    fun targets(): List<Target> {
        ensureLoaded()
        return TARGETS
    }

    @JvmStatic
    fun targetAt(index: Int): Target? {
        val targets = targets()
        if (targets.isEmpty()) {
            return null
        }
        val clamped = maxOf(0, minOf(index, targets.size - 1))
        return targets[clamped]
    }

    @JvmStatic
    fun currentColor(target: Target?): Color {
        ensureLoaded()
        if (target == null) {
            return WimGuiStyle.UNCOLOURED_BUTTON
        }
        return when (target.key) {
            "SAVE_PURPLE" -> WimGuiStyle.SAVE_PURPLE
            "LOAD_YELLOW" -> WimGuiStyle.LOAD_YELLOW
            "CONFIRM_GREEN" -> WimGuiStyle.CONFIRM_GREEN
            "CANCEL_RED" -> WimGuiStyle.CANCEL_RED
            "UNCOLOURED_BUTTON" -> WimGuiStyle.UNCOLOURED_BUTTON
            "PRESET_SCOPE_ORANGE" -> WimGuiStyle.PRESET_SCOPE_ORANGE
            "PANEL_HEADING" -> WimGuiStyle.PANEL_HEADING
            "STALE_HEADING" -> WimGuiStyle.STALE_HEADING
            "COLLAPSIBLE_HEADING" -> WimGuiStyle.COLLAPSIBLE_HEADING
            "UNSAVED_HEADING" -> WimGuiStyle.UNSAVED_HEADING
            "TAG_CATEGORY_HEADING" -> WimGuiStyle.TAG_CATEGORY_HEADING
            "ALERT_RED" -> WimGuiStyle.ALERT_RED
            "DISABLED_BACKGROUND" -> WimGuiStyle.DISABLED_BACKGROUND
            "DISABLED_DARK" -> WimGuiStyle.DISABLED_DARK
            "ROW_BORDER" -> WimGuiStyle.ROW_BORDER
            else -> target.defaultColor
        }
    }

    @JvmStatic
    fun save(target: Target?, color: Color?) {
        if (target == null || color == null) {
            return
        }
        try {
            val root = readRoot()
            val normalized = normalize(color, target.defaultColor.alpha)
            root.put(
                target.key,
                JSONObject()
                    .put("r", normalized.red)
                    .put("g", normalized.green)
                    .put("b", normalized.blue),
            )
            Global.getSettings().writeJSONToCommon(COMMON_FILE, root, true)
        } catch (t: Throwable) {
            LOG.warn("Unable to save $COMMON_FILE", t)
        }
    }

    private fun ensureLoaded() {
        if (loaded) {
            return
        }
        loaded = true
        try {
            val root = readRoot()
            for (target in TARGETS) {
                val entry = root.optJSONObject(target.key) ?: continue
                target.apply(
                    Color(
                        clamp(entry.optInt("r", target.defaultColor.red)),
                        clamp(entry.optInt("g", target.defaultColor.green)),
                        clamp(entry.optInt("b", target.defaultColor.blue)),
                        target.defaultColor.alpha,
                    ),
                )
            }
        } catch (t: Throwable) {
            LOG.warn("Unable to load $COMMON_FILE", t)
        }
    }

    private fun readRoot(): JSONObject {
        try {
            if (Global.getSettings().fileExistsInCommon(COMMON_FILE)) {
                return Global.getSettings().readJSONFromCommon(COMMON_FILE, true)
            }
        } catch (t: Throwable) {
            LOG.warn("Unable to read $COMMON_FILE", t)
        }
        return JSONObject()
    }

    private fun buildTargets(): List<Target> {
        val targets = ArrayList<Target>()
        targets.add(Target("SAVE_PURPLE", "Save / Apply Buttons", Color(146, 126, 160, 225)))
        targets.add(Target("LOAD_YELLOW", "Load / Delete Buttons", Color(188, 178, 86, 225)))
        targets.add(Target("CONFIRM_GREEN", "Confirm Buttons", Color(144, 180, 148, 225)))
        targets.add(Target("CANCEL_RED", "Cancel Buttons", Color(218, 142, 140, 225)))
        targets.add(Target("UNCOLOURED_BUTTON", "Neutral Buttons", Color(0, 0, 0, 225)))
        targets.add(Target("PRESET_SCOPE_ORANGE", "Preset / Filter Buttons", Color(202, 146, 112, 225)))
        targets.add(Target("PANEL_HEADING", "Panel Headings", Color(40, 40, 40, 225)))
        targets.add(Target("STALE_HEADING", "Stale Headings", Color(120, 120, 120, 225)))
        targets.add(Target("COLLAPSIBLE_HEADING", "Collapsible Headings", Color(80, 80, 80, 225)))
        targets.add(Target("UNSAVED_HEADING", "Unsaved Headings", Color(220, 220, 220, 225)))
        targets.add(Target("TAG_CATEGORY_HEADING", "Tag Category Headings", Color(210, 165, 185, 225)))
        targets.add(Target("ALERT_RED", "Alert Red", Color(245, 95, 85, 240)))
        targets.add(Target("DISABLED_BACKGROUND", "Disabled Background", Color(28, 28, 28, 235)))
        targets.add(Target("DISABLED_DARK", "Disabled Inner Fill", Color(18, 18, 18, 235)))
        targets.add(Target("ROW_BORDER", "Row / Modal Borders", Color(210, 210, 210, 220)))
        return Collections.unmodifiableList(targets)
    }

    private fun setColor(key: String, color: Color) {
        when (key) {
            "SAVE_PURPLE" -> WimGuiStyle.SAVE_PURPLE = color
            "LOAD_YELLOW" -> WimGuiStyle.LOAD_YELLOW = color
            "CONFIRM_GREEN" -> WimGuiStyle.CONFIRM_GREEN = color
            "CANCEL_RED" -> WimGuiStyle.CANCEL_RED = color
            "UNCOLOURED_BUTTON" -> WimGuiStyle.UNCOLOURED_BUTTON = color
            "PRESET_SCOPE_ORANGE" -> WimGuiStyle.PRESET_SCOPE_ORANGE = color
            "PANEL_HEADING" -> WimGuiStyle.PANEL_HEADING = color
            "STALE_HEADING" -> WimGuiStyle.STALE_HEADING = color
            "COLLAPSIBLE_HEADING" -> WimGuiStyle.COLLAPSIBLE_HEADING = color
            "UNSAVED_HEADING" -> WimGuiStyle.UNSAVED_HEADING = color
            "TAG_CATEGORY_HEADING" -> WimGuiStyle.TAG_CATEGORY_HEADING = color
            "ALERT_RED" -> WimGuiStyle.ALERT_RED = color
            "DISABLED_BACKGROUND" -> WimGuiStyle.DISABLED_BACKGROUND = color
            "DISABLED_DARK" -> WimGuiStyle.DISABLED_DARK = color
            "ROW_BORDER" -> {
                WimGuiStyle.ROW_BORDER = color
                WimGuiStyle.MODAL_PANEL_BORDER = color
            }
        }
    }

    private fun normalize(color: Color?, alpha: Int): Color {
        if (color == null) {
            return Color(0, 0, 0, alpha)
        }
        return Color(clamp(color.red), clamp(color.green), clamp(color.blue), alpha)
    }

    @JvmStatic
    fun adjust(color: Color?, redDelta: Int, greenDelta: Int, blueDelta: Int): Color {
        val base = color ?: WimGuiStyle.UNCOLOURED_BUTTON
        return Color(
            clamp(base.red + redDelta),
            clamp(base.green + greenDelta),
            clamp(base.blue + blueDelta),
            base.alpha,
        )
    }

    private fun clamp(value: Int): Int = maxOf(0, minOf(255, value))
}
