package weaponsprocurement.gui

class WimGuiToggleHeading private constructor() {
    companion object {
        @JvmStatic
        fun label(title: String?, expanded: Boolean): String = safeTitle(title) + " " + suffix(expanded)

        @JvmStatic
        fun countedLabel(title: String?, count: Int, expanded: Boolean): String {
            return safeTitle(title) + " [" + Math.max(0, count) + "] " + suffix(expanded)
        }

        private fun suffix(expanded: Boolean): String = if (expanded) "(-)" else "(+)"

        private fun safeTitle(title: String?): String = title ?: ""
    }
}
