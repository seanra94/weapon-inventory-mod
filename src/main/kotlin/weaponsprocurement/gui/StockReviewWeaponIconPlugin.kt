package weaponsprocurement.gui

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.api.loading.WeaponSpecAPI
import com.fs.starfarer.api.ui.PositionAPI
import com.fs.starfarer.api.util.Misc
import org.lwjgl.opengl.GL11
import java.awt.Color

class StockReviewWeaponIconPlugin(
    private val spriteName: String?,
    private val motifType: WeaponAPI.WeaponType?,
) : BaseCustomUIPanelPlugin() {
    private var position: PositionAPI? = null

    override fun positionChanged(position: PositionAPI?) {
        this.position = position
    }

    override fun render(alphaMult: Float) {
        val currentPosition = position ?: return
        val x = currentPosition.x
        val y = currentPosition.y
        val width = currentPosition.width
        val height = currentPosition.height
        Misc.renderQuadAlpha(x, y, width, height, ICON_BACKING, alphaMult)
        renderWeaponMotif(x, y, width, height, alphaMult)
        renderWeaponSprite(x, y, width, height, alphaMult)
    }

    private fun renderWeaponMotif(x: Float, y: Float, width: Float, height: Float, alphaMult: Float) {
        when (motifType) {
            WeaponAPI.WeaponType.ENERGY -> renderCircleOutline(x, y, width, height, 0.88f, ENERGY_MOTIF, alphaMult)
            WeaponAPI.WeaponType.BALLISTIC -> renderSquareOutline(x, y, width, height, 0.84f, BALLISTIC_MOTIF, alphaMult)
            WeaponAPI.WeaponType.MISSILE -> renderDiamondOutline(x, y, width, height, 0.86f, MISSILE_MOTIF, alphaMult)
            WeaponAPI.WeaponType.HYBRID -> {
                renderCircleOutline(x, y, width, height, 0.88f, HYBRID_MOTIF, alphaMult)
                renderSquareOutline(x, y, width, height, 0.62f, HYBRID_MOTIF, alphaMult)
            }
            WeaponAPI.WeaponType.COMPOSITE -> {
                renderSquareOutline(x, y, width, height, 0.84f, COMPOSITE_MOTIF, alphaMult)
                renderDiamondOutline(x, y, width, height, 0.60f, COMPOSITE_MOTIF, alphaMult)
            }
            WeaponAPI.WeaponType.SYNERGY -> {
                renderCircleOutline(x, y, width, height, 0.88f, SYNERGY_MOTIF, alphaMult)
                renderDiamondOutline(x, y, width, height, 0.60f, SYNERGY_MOTIF, alphaMult)
            }
            else -> {
                renderCircleOutline(x, y, width, height, 0.90f, UNIVERSAL_MOTIF, alphaMult)
                renderSquareOutline(x, y, width, height, 0.70f, UNIVERSAL_MOTIF, alphaMult)
                renderDiamondOutline(x, y, width, height, 0.50f, UNIVERSAL_MOTIF, alphaMult)
            }
        }
    }

    private fun renderWeaponSprite(x: Float, y: Float, width: Float, height: Float, alphaMult: Float) {
        val inset = maxOf(2f, minOf(width, height) * 0.24f)
        renderFittedSprite(
            spriteName,
            Color.WHITE,
            x + width * 0.5f,
            y + height * 0.5f,
            maxOf(1f, width - 2f * inset),
            maxOf(1f, height - 2f * inset),
            alphaMult,
        )
    }

    private fun renderSquareOutline(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        ratio: Float,
        color: Color,
        alphaMult: Float,
    ) {
        val size = minOf(width, height) * ratio
        val thickness = motifStroke(width, height)
        val left = x + width * 0.5f - size * 0.5f
        val bottom = y + height * 0.5f - size * 0.5f
        Misc.renderQuadAlpha(left, bottom, size, thickness, color, alphaMult)
        Misc.renderQuadAlpha(left, bottom + size - thickness, size, thickness, color, alphaMult)
        Misc.renderQuadAlpha(left, bottom, thickness, size, color, alphaMult)
        Misc.renderQuadAlpha(left + size - thickness, bottom, thickness, size, color, alphaMult)
    }

    private fun renderDiamondOutline(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        ratio: Float,
        color: Color,
        alphaMult: Float,
    ) {
        val cx = x + width * 0.5f
        val cy = y + height * 0.5f
        val radius = minOf(width, height) * ratio * 0.5f
        renderLineLoop(
            color,
            alphaMult,
            motifStroke(width, height),
            floatArrayOf(cx, cx + radius, cx, cx - radius),
            floatArrayOf(cy + radius, cy, cy - radius, cy),
        )
    }

    private fun renderCircleOutline(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        ratio: Float,
        color: Color,
        alphaMult: Float,
    ) {
        val size = minOf(width, height) * ratio
        renderFittedSprite(CIRCLE_SPRITE, color, x + width * 0.5f, y + height * 0.5f, size, size, alphaMult)
    }

    private fun motifStroke(width: Float, height: Float): Float = maxOf(2.25f, minOf(width, height) * 0.07f)

    private fun renderLineLoop(color: Color, alphaMult: Float, strokeWidth: Float, xs: FloatArray?, ys: FloatArray?) {
        if (xs == null || ys == null || xs.size != ys.size || xs.size < 2) {
            return
        }
        GL11.glPushMatrix()
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glColor4f(
            color.red / 255f,
            color.green / 255f,
            color.blue / 255f,
            color.alpha / 255f * alphaMult,
        )
        GL11.glLineWidth(strokeWidth)
        GL11.glBegin(GL11.GL_LINE_LOOP)
        for (i in xs.indices) {
            GL11.glVertex2f(xs[i], ys[i])
        }
        GL11.glEnd()
        GL11.glPopMatrix()
    }

    private fun renderFittedSprite(
        path: String?,
        color: Color?,
        centerX: Float,
        centerY: Float,
        maxWidth: Float,
        maxHeight: Float,
        alphaMult: Float,
    ): Boolean {
        if (!WimGuiTooltip.hasText(path)) {
            return false
        }
        val sprite: SpriteAPI = try {
            Global.getSettings().getSprite(path)
        } catch (ex: RuntimeException) {
            return false
        } ?: return false
        if (sprite.width <= 0f || sprite.height <= 0f) {
            return false
        }
        val oldWidth = sprite.width
        val oldHeight = sprite.height
        val oldAlpha = sprite.alphaMult
        val oldColor = sprite.color
        val oldAngle = sprite.angle
        val scale = minOf(maxOf(1f, maxWidth) / oldWidth, maxOf(1f, maxHeight) / oldHeight)
        sprite.setSize(oldWidth * scale, oldHeight * scale)
        sprite.alphaMult = oldAlpha * alphaMult
        sprite.color = color ?: Color.WHITE
        sprite.angle = 0f
        sprite.renderAtCenter(centerX, centerY)
        sprite.setSize(oldWidth, oldHeight)
        sprite.alphaMult = oldAlpha
        sprite.color = oldColor
        sprite.angle = oldAngle
        return true
    }

    companion object {
        private val ICON_BACKING = Color(0, 0, 0, 230)
        private val BALLISTIC_MOTIF = Color(255, 226, 45, 230)
        private val ENERGY_MOTIF = Color(120, 185, 255, 230)
        private val MISSILE_MOTIF = Color(170, 255, 80, 230)
        private val HYBRID_MOTIF = Color(255, 177, 45, 230)
        private val COMPOSITE_MOTIF = Color(215, 255, 55, 230)
        private val SYNERGY_MOTIF = Color(110, 255, 215, 230)
        private val UNIVERSAL_MOTIF = Color(220, 220, 220, 220)
        private const val CIRCLE_SPRITE = "graphics/ui/icons/64x_circle.png"

        @JvmStatic
        fun spriteName(spec: WeaponSpecAPI?): String? {
            if (spec == null) {
                return null
            }
            if (WimGuiTooltip.hasText(spec.turretSpriteName)) {
                return spec.turretSpriteName
            }
            return spec.hardpointSpriteName
        }

        @JvmStatic
        fun motifType(spec: WeaponSpecAPI?): WeaponAPI.WeaponType? {
            if (spec == null) {
                return null
            }
            return spec.mountType ?: spec.type
        }
    }
}
