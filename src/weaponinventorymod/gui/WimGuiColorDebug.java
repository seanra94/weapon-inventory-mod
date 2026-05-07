package weaponinventorymod.gui;

import com.fs.starfarer.api.Global;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class WimGuiColorDebug {
    static final class Target {
        private final String key;
        private final String label;
        private final Color defaultColor;

        Target(String key, String label, Color defaultColor) {
            this.key = key;
            this.label = label;
            this.defaultColor = defaultColor;
        }

        String getKey() {
            return key;
        }

        String getLabel() {
            return label;
        }

        Color getDefaultColor() {
            return defaultColor;
        }

        void apply(Color color) {
            Color normalized = normalize(color, defaultColor.getAlpha());
            setColor(key, normalized);
            StockReviewStyle.refreshColors();
        }
    }

    private static final Logger LOG = Logger.getLogger(WimGuiColorDebug.class);
    private static final String COMMON_FILE = "WIM_debugGuiColors.json";
    private static final List<Target> TARGETS = buildTargets();
    private static boolean loaded = false;

    private WimGuiColorDebug() {
    }

    static List<Target> targets() {
        ensureLoaded();
        return TARGETS;
    }

    static Target targetAt(int index) {
        List<Target> targets = targets();
        if (targets.isEmpty()) {
            return null;
        }
        int clamped = Math.max(0, Math.min(index, targets.size() - 1));
        return targets.get(clamped);
    }

    static Color currentColor(Target target) {
        ensureLoaded();
        if (target == null) {
            return WimGuiStyle.UNCOLOURED_BUTTON;
        }
        if ("SAVE_PURPLE".equals(target.getKey())) {
            return WimGuiStyle.SAVE_PURPLE;
        }
        if ("LOAD_YELLOW".equals(target.getKey())) {
            return WimGuiStyle.LOAD_YELLOW;
        }
        if ("CONFIRM_GREEN".equals(target.getKey())) {
            return WimGuiStyle.CONFIRM_GREEN;
        }
        if ("CANCEL_RED".equals(target.getKey())) {
            return WimGuiStyle.CANCEL_RED;
        }
        if ("UNCOLOURED_BUTTON".equals(target.getKey())) {
            return WimGuiStyle.UNCOLOURED_BUTTON;
        }
        if ("PRESET_SCOPE_ORANGE".equals(target.getKey())) {
            return WimGuiStyle.PRESET_SCOPE_ORANGE;
        }
        if ("PANEL_HEADING".equals(target.getKey())) {
            return WimGuiStyle.PANEL_HEADING;
        }
        if ("STALE_HEADING".equals(target.getKey())) {
            return WimGuiStyle.STALE_HEADING;
        }
        if ("COLLAPSIBLE_HEADING".equals(target.getKey())) {
            return WimGuiStyle.COLLAPSIBLE_HEADING;
        }
        if ("UNSAVED_HEADING".equals(target.getKey())) {
            return WimGuiStyle.UNSAVED_HEADING;
        }
        if ("TAG_CATEGORY_HEADING".equals(target.getKey())) {
            return WimGuiStyle.TAG_CATEGORY_HEADING;
        }
        if ("ALERT_RED".equals(target.getKey())) {
            return WimGuiStyle.ALERT_RED;
        }
        if ("DISABLED_BACKGROUND".equals(target.getKey())) {
            return WimGuiStyle.DISABLED_BACKGROUND;
        }
        if ("DISABLED_DARK".equals(target.getKey())) {
            return WimGuiStyle.DISABLED_DARK;
        }
        if ("ROW_BORDER".equals(target.getKey())) {
            return WimGuiStyle.ROW_BORDER;
        }
        return target.getDefaultColor();
    }

    static void save(Target target, Color color) {
        if (target == null || color == null) {
            return;
        }
        try {
            JSONObject root = readRoot();
            Color normalized = normalize(color, target.getDefaultColor().getAlpha());
            root.put(target.getKey(), new JSONObject()
                    .put("r", normalized.getRed())
                    .put("g", normalized.getGreen())
                    .put("b", normalized.getBlue()));
            Global.getSettings().writeJSONToCommon(COMMON_FILE, root, true);
        } catch (Throwable t) {
            LOG.warn("Unable to save " + COMMON_FILE, t);
        }
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        try {
            JSONObject root = readRoot();
            for (int i = 0; i < TARGETS.size(); i++) {
                Target target = TARGETS.get(i);
                JSONObject entry = root.optJSONObject(target.getKey());
                if (entry == null) {
                    continue;
                }
                target.apply(new Color(
                        clamp(entry.optInt("r", target.getDefaultColor().getRed())),
                        clamp(entry.optInt("g", target.getDefaultColor().getGreen())),
                        clamp(entry.optInt("b", target.getDefaultColor().getBlue())),
                        target.getDefaultColor().getAlpha()));
            }
        } catch (Throwable t) {
            LOG.warn("Unable to load " + COMMON_FILE, t);
        }
    }

    private static JSONObject readRoot() {
        try {
            if (Global.getSettings().fileExistsInCommon(COMMON_FILE)) {
                return Global.getSettings().readJSONFromCommon(COMMON_FILE, true);
            }
        } catch (Throwable t) {
            LOG.warn("Unable to read " + COMMON_FILE, t);
        }
        return new JSONObject();
    }

    private static List<Target> buildTargets() {
        List<Target> targets = new ArrayList<Target>();
        targets.add(new Target("SAVE_PURPLE", "Save / Apply Buttons", new Color(146, 126, 160, 225)));
        targets.add(new Target("LOAD_YELLOW", "Load / Delete Buttons", new Color(188, 178, 86, 225)));
        targets.add(new Target("CONFIRM_GREEN", "Confirm Buttons", new Color(144, 180, 148, 225)));
        targets.add(new Target("CANCEL_RED", "Cancel Buttons", new Color(218, 142, 140, 225)));
        targets.add(new Target("UNCOLOURED_BUTTON", "Neutral Buttons", new Color(0, 0, 0, 225)));
        targets.add(new Target("PRESET_SCOPE_ORANGE", "Preset / Filter Buttons", new Color(202, 146, 112, 225)));
        targets.add(new Target("PANEL_HEADING", "Panel Headings", new Color(40, 40, 40, 225)));
        targets.add(new Target("STALE_HEADING", "Stale Headings", new Color(120, 120, 120, 225)));
        targets.add(new Target("COLLAPSIBLE_HEADING", "Collapsible Headings", new Color(80, 80, 80, 225)));
        targets.add(new Target("UNSAVED_HEADING", "Unsaved Headings", new Color(220, 220, 220, 225)));
        targets.add(new Target("TAG_CATEGORY_HEADING", "Tag Category Headings", new Color(210, 165, 185, 225)));
        targets.add(new Target("ALERT_RED", "Alert Red", new Color(245, 95, 85, 240)));
        targets.add(new Target("DISABLED_BACKGROUND", "Disabled Background", new Color(28, 28, 28, 235)));
        targets.add(new Target("DISABLED_DARK", "Disabled Inner Fill", new Color(18, 18, 18, 235)));
        targets.add(new Target("ROW_BORDER", "Row / Modal Borders", new Color(210, 210, 210, 220)));
        return Collections.unmodifiableList(targets);
    }

    private static void setColor(String key, Color color) {
        if ("SAVE_PURPLE".equals(key)) {
            WimGuiStyle.SAVE_PURPLE = color;
        } else if ("LOAD_YELLOW".equals(key)) {
            WimGuiStyle.LOAD_YELLOW = color;
        } else if ("CONFIRM_GREEN".equals(key)) {
            WimGuiStyle.CONFIRM_GREEN = color;
        } else if ("CANCEL_RED".equals(key)) {
            WimGuiStyle.CANCEL_RED = color;
        } else if ("UNCOLOURED_BUTTON".equals(key)) {
            WimGuiStyle.UNCOLOURED_BUTTON = color;
        } else if ("PRESET_SCOPE_ORANGE".equals(key)) {
            WimGuiStyle.PRESET_SCOPE_ORANGE = color;
        } else if ("PANEL_HEADING".equals(key)) {
            WimGuiStyle.PANEL_HEADING = color;
        } else if ("STALE_HEADING".equals(key)) {
            WimGuiStyle.STALE_HEADING = color;
        } else if ("COLLAPSIBLE_HEADING".equals(key)) {
            WimGuiStyle.COLLAPSIBLE_HEADING = color;
        } else if ("UNSAVED_HEADING".equals(key)) {
            WimGuiStyle.UNSAVED_HEADING = color;
        } else if ("TAG_CATEGORY_HEADING".equals(key)) {
            WimGuiStyle.TAG_CATEGORY_HEADING = color;
        } else if ("ALERT_RED".equals(key)) {
            WimGuiStyle.ALERT_RED = color;
        } else if ("DISABLED_BACKGROUND".equals(key)) {
            WimGuiStyle.DISABLED_BACKGROUND = color;
        } else if ("DISABLED_DARK".equals(key)) {
            WimGuiStyle.DISABLED_DARK = color;
        } else if ("ROW_BORDER".equals(key)) {
            WimGuiStyle.ROW_BORDER = color;
            WimGuiStyle.MODAL_PANEL_BORDER = color;
        }
    }

    private static Color normalize(Color color, int alpha) {
        if (color == null) {
            return new Color(0, 0, 0, alpha);
        }
        return new Color(clamp(color.getRed()), clamp(color.getGreen()), clamp(color.getBlue()), alpha);
    }

    static Color adjust(Color color, int redDelta, int greenDelta, int blueDelta) {
        if (color == null) {
            color = WimGuiStyle.UNCOLOURED_BUTTON;
        }
        return new Color(
                clamp(color.getRed() + redDelta),
                clamp(color.getGreen() + greenDelta),
                clamp(color.getBlue() + blueDelta),
                color.getAlpha());
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
