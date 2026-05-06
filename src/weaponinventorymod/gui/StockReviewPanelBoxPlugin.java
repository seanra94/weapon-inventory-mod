package weaponinventorymod.gui;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.ui.PositionAPI;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

final class StockReviewPanelBoxPlugin extends BaseCustomUIPanelPlugin {
    private final Color fillColor;
    private final Color borderColor;
    private PositionAPI position;

    StockReviewPanelBoxPlugin(Color fillColor, Color borderColor) {
        this.fillColor = fillColor;
        this.borderColor = borderColor;
    }

    @Override
    public void positionChanged(PositionAPI position) {
        this.position = position;
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (fillColor == null || position == null) {
            return;
        }
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(
                fillColor.getRed() / 255f,
                fillColor.getGreen() / 255f,
                fillColor.getBlue() / 255f,
                fillColor.getAlpha() / 255f * alphaMult);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(position.getX(), position.getY());
        GL11.glVertex2f(position.getX() + position.getWidth(), position.getY());
        GL11.glVertex2f(position.getX() + position.getWidth(), position.getY() + position.getHeight());
        GL11.glVertex2f(position.getX(), position.getY() + position.getHeight());
        GL11.glEnd();
        GL11.glPopMatrix();
    }

    @Override
    public void render(float alphaMult) {
        if (borderColor == null || position == null) {
            return;
        }
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(
                borderColor.getRed() / 255f,
                borderColor.getGreen() / 255f,
                borderColor.getBlue() / 255f,
                borderColor.getAlpha() / 255f * alphaMult);
        GL11.glLineWidth(1f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(position.getX(), position.getY());
        GL11.glVertex2f(position.getX() + position.getWidth(), position.getY());
        GL11.glVertex2f(position.getX() + position.getWidth(), position.getY() + position.getHeight());
        GL11.glVertex2f(position.getX(), position.getY() + position.getHeight());
        GL11.glEnd();
        GL11.glPopMatrix();
    }
}
