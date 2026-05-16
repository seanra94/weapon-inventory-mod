package weaponsprocurement.ui

import com.fs.starfarer.api.ui.CustomPanelAPI

class WimGuiContentPanel {
    private var content: CustomPanelAPI? = null

    fun begin(root: CustomPanelAPI?, width: Float, height: Float): CustomPanelAPI? {
        if (root == null) return null
        if (content != null) root.removeComponent(content)
        content = root.createCustomPanel(width, height, null)
        return content
    }

    fun attach(root: CustomPanelAPI?) {
        if (root != null && content != null) {
            root.addComponent(content).inTL(0f, 0f)
        }
    }
}
