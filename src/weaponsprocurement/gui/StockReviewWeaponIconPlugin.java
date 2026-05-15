package weaponsprocurement.gui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

final class StockReviewWeaponIconPlugin extends BaseCustomUIPanelPlugin {
    private static final Color ICON_BACKING = new Color(0, 0, 0, 230);
    private static final Color BALLISTIC_MOTIF = new Color(255, 226, 45, 230);
    private static final Color ENERGY_MOTIF = new Color(120, 185, 255, 230);
    private static final Color MISSILE_MOTIF = new Color(170, 255, 80, 230);
    private static final Color HYBRID_MOTIF = new Color(255, 177, 45, 230);
    private static final Color COMPOSITE_MOTIF = new Color(215, 255, 55, 230);
    private static final Color SYNERGY_MOTIF = new Color(110, 255, 215, 230);
    private static final Color UNIVERSAL_MOTIF = new Color(220, 220, 220, 220);
    private static final String CIRCLE_SPRITE = "graphics/ui/icons/64x_circle.png";

    private final String spriteName;
    private final WeaponAPI.WeaponType motifType;
    private PositionAPI position;

    StockReviewWeaponIconPlugin(String spriteName, WeaponAPI.WeaponType motifType) {
        this.spriteName = spriteName;
        this.motifType = motifType;
    }

    static String spriteName(WeaponSpecAPI spec) {
        if (spec == null) {
            return null;
        }
        if (WimGuiTooltip.hasText(spec.getTurretSpriteName())) {
            return spec.getTurretSpriteName();
        }
        return spec.getHardpointSpriteName();
    }

    static WeaponAPI.WeaponType motifType(WeaponSpecAPI spec) {
        if (spec == null) {
            return null;
        }
        if (spec.getMountType() != null) {
            return spec.getMountType();
        }
        return spec.getType();
    }

    @Override
    public void positionChanged(PositionAPI position) {
        this.position = position;
    }

    @Override
    public void render(float alphaMult) {
        if (position == null) {
            return;
        }
        float x = position.getX();
        float y = position.getY();
        float width = position.getWidth();
        float height = position.getHeight();
        Misc.renderQuadAlpha(x, y, width, height, ICON_BACKING, alphaMult);
        renderWeaponMotif(x, y, width, height, alphaMult);
        renderWeaponSprite(x, y, width, height, alphaMult);
    }

    private void renderWeaponMotif(float x, float y, float width, float height, float alphaMult) {
        WeaponAPI.WeaponType type = motifType;
        if (WeaponAPI.WeaponType.ENERGY.equals(type)) {
            renderCircleOutline(x, y, width, height, 0.88f, ENERGY_MOTIF, alphaMult);
        } else if (WeaponAPI.WeaponType.BALLISTIC.equals(type)) {
            renderSquareOutline(x, y, width, height, 0.84f, BALLISTIC_MOTIF, alphaMult);
        } else if (WeaponAPI.WeaponType.MISSILE.equals(type)) {
            renderDiamondOutline(x, y, width, height, 0.86f, MISSILE_MOTIF, alphaMult);
        } else if (WeaponAPI.WeaponType.HYBRID.equals(type)) {
            renderCircleOutline(x, y, width, height, 0.88f, HYBRID_MOTIF, alphaMult);
            renderSquareOutline(x, y, width, height, 0.62f, HYBRID_MOTIF, alphaMult);
        } else if (WeaponAPI.WeaponType.COMPOSITE.equals(type)) {
            renderSquareOutline(x, y, width, height, 0.84f, COMPOSITE_MOTIF, alphaMult);
            renderDiamondOutline(x, y, width, height, 0.60f, COMPOSITE_MOTIF, alphaMult);
        } else if (WeaponAPI.WeaponType.SYNERGY.equals(type)) {
            renderCircleOutline(x, y, width, height, 0.88f, SYNERGY_MOTIF, alphaMult);
            renderDiamondOutline(x, y, width, height, 0.60f, SYNERGY_MOTIF, alphaMult);
        } else {
            renderCircleOutline(x, y, width, height, 0.90f, UNIVERSAL_MOTIF, alphaMult);
            renderSquareOutline(x, y, width, height, 0.70f, UNIVERSAL_MOTIF, alphaMult);
            renderDiamondOutline(x, y, width, height, 0.50f, UNIVERSAL_MOTIF, alphaMult);
        }
    }

    private void renderWeaponSprite(float x, float y, float width, float height, float alphaMult) {
        float inset = Math.max(2f, Math.min(width, height) * 0.24f);
        renderFittedSprite(spriteName, Color.WHITE, x + width * 0.5f, y + height * 0.5f,
                Math.max(1f, width - 2f * inset), Math.max(1f, height - 2f * inset), alphaMult);
    }

    private void renderSquareOutline(float x,
                                     float y,
                                     float width,
                                     float height,
                                     float ratio,
                                     Color color,
                                     float alphaMult) {
        float size = Math.min(width, height) * ratio;
        float thickness = motifStroke(width, height);
        float left = x + width * 0.5f - size * 0.5f;
        float bottom = y + height * 0.5f - size * 0.5f;
        Misc.renderQuadAlpha(left, bottom, size, thickness, color, alphaMult);
        Misc.renderQuadAlpha(left, bottom + size - thickness, size, thickness, color, alphaMult);
        Misc.renderQuadAlpha(left, bottom, thickness, size, color, alphaMult);
        Misc.renderQuadAlpha(left + size - thickness, bottom, thickness, size, color, alphaMult);
    }

    private void renderDiamondOutline(float x,
                                      float y,
                                      float width,
                                      float height,
                                      float ratio,
                                      Color color,
                                      float alphaMult) {
        float cx = x + width * 0.5f;
        float cy = y + height * 0.5f;
        float radius = Math.min(width, height) * ratio * 0.5f;
        renderLineLoop(color, alphaMult, motifStroke(width, height),
                new float[]{cx, cx + radius, cx, cx - radius},
                new float[]{cy + radius, cy, cy - radius, cy});
    }

    private void renderCircleOutline(float x,
                                     float y,
                                     float width,
                                     float height,
                                     float ratio,
                                     Color color,
                                     float alphaMult) {
        float size = Math.min(width, height) * ratio;
        renderFittedSprite(CIRCLE_SPRITE, color, x + width * 0.5f, y + height * 0.5f,
                size, size, alphaMult);
    }

    private float motifStroke(float width, float height) {
        return Math.max(2.25f, Math.min(width, height) * 0.07f);
    }

    private void renderLineLoop(Color color, float alphaMult, float strokeWidth, float[] xs, float[] ys) {
        if (xs == null || ys == null || xs.length != ys.length || xs.length < 2) {
            return;
        }
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(
                color.getRed() / 255f,
                color.getGreen() / 255f,
                color.getBlue() / 255f,
                color.getAlpha() / 255f * alphaMult);
        GL11.glLineWidth(strokeWidth);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < xs.length; i++) {
            GL11.glVertex2f(xs[i], ys[i]);
        }
        GL11.glEnd();
        GL11.glPopMatrix();
    }

    private boolean renderFittedSprite(String path,
                                       Color color,
                                       float centerX,
                                       float centerY,
                                       float maxWidth,
                                       float maxHeight,
                                       float alphaMult) {
        if (!WimGuiTooltip.hasText(path)) {
            return false;
        }
        SpriteAPI sprite;
        try {
            sprite = Global.getSettings().getSprite(path);
        } catch (RuntimeException ex) {
            return false;
        }
        if (sprite == null || sprite.getWidth() <= 0f || sprite.getHeight() <= 0f) {
            return false;
        }
        float oldWidth = sprite.getWidth();
        float oldHeight = sprite.getHeight();
        float oldAlpha = sprite.getAlphaMult();
        Color oldColor = sprite.getColor();
        float oldAngle = sprite.getAngle();
        float scale = Math.min(Math.max(1f, maxWidth) / oldWidth, Math.max(1f, maxHeight) / oldHeight);
        sprite.setSize(oldWidth * scale, oldHeight * scale);
        sprite.setAlphaMult(oldAlpha * alphaMult);
        sprite.setColor(color == null ? Color.WHITE : color);
        sprite.setAngle(0f);
        sprite.renderAtCenter(centerX, centerY);
        sprite.setSize(oldWidth, oldHeight);
        sprite.setAlphaMult(oldAlpha);
        sprite.setColor(oldColor);
        sprite.setAngle(oldAngle);
        return true;
    }
}
