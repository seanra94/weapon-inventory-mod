package weaponsprocurement.gui

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.ui.PositionAPI
import org.lwjgl.opengl.GL11
import java.awt.Color

class WimGuiPanelPlugin(private val fillColor: Color?, private val borderColor: Color?) : BaseCustomUIPanelPlugin() {
    private var position: PositionAPI? = null

    override fun positionChanged(position: PositionAPI?) {
        this.position = position
    }

    override fun renderBelow(alphaMult: Float) {
        val currentPosition = position
        if (fillColor == null || currentPosition == null) {
            return
        }
        GL11.glPushMatrix()
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glColor4f(
            fillColor.red / 255f,
            fillColor.green / 255f,
            fillColor.blue / 255f,
            fillColor.alpha / 255f * alphaMult,
        )
        GL11.glBegin(GL11.GL_QUADS)
        GL11.glVertex2f(currentPosition.x, currentPosition.y)
        GL11.glVertex2f(currentPosition.x + currentPosition.width, currentPosition.y)
        GL11.glVertex2f(currentPosition.x + currentPosition.width, currentPosition.y + currentPosition.height)
        GL11.glVertex2f(currentPosition.x, currentPosition.y + currentPosition.height)
        GL11.glEnd()
        GL11.glPopMatrix()
    }

    override fun render(alphaMult: Float) {
        val currentPosition = position
        if (borderColor == null || currentPosition == null) {
            return
        }
        GL11.glPushMatrix()
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glColor4f(
            borderColor.red / 255f,
            borderColor.green / 255f,
            borderColor.blue / 255f,
            borderColor.alpha / 255f * alphaMult,
        )
        GL11.glLineWidth(1f)
        GL11.glBegin(GL11.GL_LINE_LOOP)
        GL11.glVertex2f(currentPosition.x, currentPosition.y)
        GL11.glVertex2f(currentPosition.x + currentPosition.width, currentPosition.y)
        GL11.glVertex2f(currentPosition.x + currentPosition.width, currentPosition.y + currentPosition.height)
        GL11.glVertex2f(currentPosition.x, currentPosition.y + currentPosition.height)
        GL11.glEnd()
        GL11.glPopMatrix()
    }
}
