package weaponsprocurement.ui

import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.CustomPanelAPI
import java.awt.Color

class WimGuiModalHeader private constructor() {
    companion object {
        @JvmStatic
        fun addTitleStatusHeader(
            parent: CustomPanelAPI,
            modal: WimGuiModalLayout,
            headerHeight: Float,
            title: String?,
            status: String?,
            fill: Color,
            border: Color,
            textColor: Color,
        ): CustomPanelAPI {
            val header = parent.createCustomPanel(
                modal.contentWidth(),
                headerHeight,
                WimGuiPanelPlugin(fill, border),
            )
            parent.addComponent(header).inTL(modal.padding, modal.headingTop())

            val textWidth = Math.max(1f, header.position.width - 2f * modal.padding)
            WimGuiControls.addLabel(header, title, textColor, modal.padding, 2f, textWidth, 22f, Alignment.LMID)
            if (!status.isNullOrEmpty()) {
                WimGuiControls.addLabel(
                    header,
                    status,
                    textColor,
                    modal.padding,
                    28f,
                    textWidth,
                    Math.max(18f, headerHeight - 30f),
                    Alignment.LMID,
                )
            }
            return header
        }
    }
}
