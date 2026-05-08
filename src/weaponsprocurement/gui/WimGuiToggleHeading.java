package weaponsprocurement.gui;

final class WimGuiToggleHeading {
    private WimGuiToggleHeading() {
    }

    static String label(String title, boolean expanded) {
        return safeTitle(title) + " " + suffix(expanded);
    }

    static String countedLabel(String title, int count, boolean expanded) {
        return safeTitle(title) + " [" + Math.max(0, count) + "] " + suffix(expanded);
    }

    private static String suffix(boolean expanded) {
        return expanded ? "(-)" : "(+)";
    }

    private static String safeTitle(String title) {
        return title == null ? "" : title;
    }
}
