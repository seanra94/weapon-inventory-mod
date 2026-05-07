package weaponinventorymod.gui;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.awt.Color;
import java.util.List;

final class WimGuiControls {
    private WimGuiControls() {
    }

    static WimGuiButtonShell addButton(CustomPanelAPI parent,
                                       float x,
                                       float y,
                                       float width,
                                       float height,
                                       String label,
                                       Color textColor,
                                       Object action,
                                       boolean enabled,
                                       Alignment alignment,
                                       WimGuiButtonColors colors,
                                       Color borderColor) {
        Color idle = colors == null ? WimGuiStyle.UNCOLOURED_BUTTON : colors.idle;
        Color hover = colors == null ? idle : colors.hover;
        Color shellFill = enabled ? idle : WimGuiStyle.DISABLED_BACKGROUND;
        Color buttonFill = enabled ? WimGuiStyle.dimForIdle(idle) : WimGuiStyle.DISABLED_DARK;
        Color resolvedText = enabled ? textColor : WimGuiStyle.DISABLED_TEXT;

        CustomPanelAPI shell = parent.createCustomPanel(
                width,
                height,
                new WimGuiPanelPlugin(shellFill, borderColor));
        parent.addComponent(shell).inTL(x, y);
        if (!enabled) {
            addLabel(shell, label, resolvedText, 0f, 0f, width, height, alignment);
            return new WimGuiButtonShell(shell, null);
        }

        TooltipMakerAPI element = shell.createUIElement(width, height, false);
        element.setButtonFontDefault();
        ButtonAPI button = element.addButton(
                "",
                action,
                hover,
                buttonFill,
                alignment,
                CutStyle.NONE,
                width,
                height,
                0f);
        button.setEnabled(enabled);
        button.setQuickMode(true);
        shell.addUIElement(element).inTL(0f, 0f);
        addLabel(shell, label, resolvedText, 0f, 0f, width, height, alignment);
        return new WimGuiButtonShell(shell, button);
    }

    static <A> WimGuiButtonShell addBoundButton(CustomPanelAPI parent,
                                                float x,
                                                float y,
                                                float height,
                                                WimGuiButtonSpec<A> spec,
                                                List<WimGuiButtonBinding<A>> bindings) {
        WimGuiButtonShell shell = addButton(
                parent,
                x,
                y,
                spec.width,
                height,
                spec.label,
                spec.textColor,
                spec.action,
                spec.enabled,
                spec.alignment,
                spec.colors,
                spec.borderColor);
        if (spec.enabled && bindings != null) {
            bindings.add(new WimGuiButtonBinding<A>(shell.panel, shell.button, spec.action));
        }
        return shell;
    }

    static <A> float addButtonRow(CustomPanelAPI parent,
                                  float x,
                                  float y,
                                  float height,
                                  float gap,
                                  List<WimGuiButtonSpec<A>> specs,
                                  List<WimGuiButtonBinding<A>> bindings) {
        float cursor = x;
        if (specs == null) {
            return cursor;
        }
        for (int i = 0; i < specs.size(); i++) {
            WimGuiButtonSpec<A> spec = specs.get(i);
            addBoundButton(parent, cursor, y, height, spec, bindings);
            cursor += spec.width + gap;
        }
        return cursor;
    }

    static void addInfoCell(CustomPanelAPI parent,
                            float x,
                            float y,
                            float width,
                            float height,
                            String label,
                            Color background,
                            Color textColor,
                            Color borderColor) {
        addInfoCell(parent, x, y, width, height, label, background, textColor, borderColor, Alignment.MID);
    }

    static void addInfoCell(CustomPanelAPI parent,
                            float x,
                            float y,
                            float width,
                            float height,
                            String label,
                            Color background,
                            Color textColor,
                            Color borderColor,
                            Alignment alignment) {
        CustomPanelAPI cell = parent.createCustomPanel(width, height,
                new WimGuiPanelPlugin(background, borderColor));
        parent.addComponent(cell).inTL(x, y);
        addLabel(cell, label, textColor, 0f, 0f, width, height, alignment);
    }

    static void addLabelTextRow(CustomPanelAPI parent,
                                float x,
                                float y,
                                float width,
                                float height,
                                String label,
                                String value,
                                Color valueFillColor,
                                Color borderColor,
                                Color textColor) {
        float labelWidth = width / 2f;
        float valueWidth = width - labelWidth;
        addInfoCell(parent, x, y, labelWidth, height, label, null, textColor, borderColor, Alignment.LMID);
        addInfoCell(parent, x + labelWidth, y, valueWidth, height, value, valueFillColor, textColor, borderColor, Alignment.MID);
    }

    static <A> void addRowCell(CustomPanelAPI parent,
                               float x,
                               float y,
                               float height,
                               WimGuiRowCell<A> cell,
                               List<WimGuiButtonBinding<A>> bindings,
                               Color borderColor) {
        if (cell == null) {
            return;
        }
        if (cell.isAction()) {
            Color fill = cell.isEnabled() ? cell.getFillColor() : WimGuiStyle.DISABLED_BACKGROUND;
            addBoundButton(
                    parent,
                    x,
                    y,
                    height,
                    WimGuiButtonSpec.dimmedInner(
                            cell.getWidth(),
                            cell.getLabel(),
                            cell.getTextColor(),
                            cell.getAction(),
                            cell.isEnabled(),
                            Alignment.MID,
                            fill,
                            borderColor),
                    bindings);
            return;
        }
        addInfoCell(parent, x, y, cell.getWidth(), height, cell.getLabel(), cell.getFillColor(), cell.getTextColor(), cell.borderColor(borderColor), cell.getAlignment());
    }

    static void addLabel(CustomPanelAPI parent,
                         String text,
                         Color color,
                         float x,
                         float y,
                         float width,
                         float height,
                         Alignment alignment) {
        float labelX = x;
        float labelWidth = width;
        if (Alignment.LMID.equals(alignment)) {
            labelX += WimGuiStyle.TEXT_LEFT_PAD;
            labelWidth = Math.max(8f, width - WimGuiStyle.TEXT_LEFT_PAD);
        }
        TooltipMakerAPI label = parent.createUIElement(labelWidth, height, false);
        label.setParaFontDefault();
        label.setParaFontColor(color);
        int maxChars = WimGuiText.estimatedChars(labelWidth);
        LabelAPI line = label.addPara(WimGuiText.fit(text, maxChars), 0f, color);
        line.setAlignment(alignment);
        parent.addUIElement(label).inTL(labelX, y + WimGuiStyle.TEXT_TOP_PAD);
    }

    static WimGuiTextLayout addWrappedLabel(CustomPanelAPI parent,
                                            String text,
                                            Color color,
                                            float x,
                                            float y,
                                            float width,
                                            float minHeight,
                                            int maxLines,
                                            Alignment alignment) {
        float labelX = x;
        float labelWidth = width;
        if (Alignment.LMID.equals(alignment)) {
            labelX += WimGuiStyle.TEXT_LEFT_PAD;
            labelWidth = Math.max(8f, width - WimGuiStyle.TEXT_LEFT_PAD);
        }
        WimGuiTextLayout layout = WimGuiText.fitLayout(text, labelWidth, minHeight, maxLines);
        TooltipMakerAPI label = parent.createUIElement(labelWidth, layout.rowHeight, false);
        label.setParaFontDefault();
        label.setParaFontColor(color);
        LabelAPI line = label.addPara(layout.wrappedText, 0f, color);
        line.setAlignment(alignment);
        parent.addUIElement(label).inTL(labelX, y + WimGuiStyle.TEXT_TOP_PAD);
        return layout;
    }
}
