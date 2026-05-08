package weaponinventorymod.gui;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;

import java.awt.Color;
import java.util.List;

final class WimGuiListRowRenderer {
    private WimGuiListRowRenderer() {
    }

    static <A> void renderRow(CustomPanelAPI parent,
                              WimGuiListRow<A> row,
                              float y,
                              float rowHeight,
                              float actionHeight,
                              float horizontalPad,
                              float buttonGap,
                              float textLeftPad,
                              float minLabelWidth,
                              java.awt.Color defaultBorder,
                              List<WimGuiButtonBinding<A>> buttons) {
        float width = parent.getPosition().getWidth() - 2f * horizontalPad;
        Color rowBorder = row.getIndent() > 0f ? null : row.getBorderColor();
        CustomPanelAPI rowPanel = parent.createCustomPanel(
                width,
                rowHeight,
                new WimGuiPanelPlugin(row.getFillColor(), rowBorder));
        parent.addComponent(rowPanel).inTL(horizontalPad, y);

        float cellGap = row.cellGap(buttonGap);
        float cellBlockWidth = WimGuiRowCell.totalWidth(row.getCells(), cellGap);
        float reservedBlockWidth = Math.max(cellBlockWidth, row.rightReserveWidth());
        float labelLeft = row.getIndent();
        float labelWidth = Math.max(minLabelWidth, width - labelLeft - reservedBlockWidth - textLeftPad);
        if (row.getMainAction() != null) {
            addMainAction(rowPanel, row, labelLeft, labelWidth, actionHeight, defaultBorder, buttons);
        } else {
            addLabel(rowPanel, row.getLabel(), row.getTextColor(), labelLeft, labelWidth, rowHeight);
        }

        if (!row.getCells().isEmpty()) {
            float x = width - row.rightReserveWidth() - cellBlockWidth;
            for (int i = 0; i < row.getCells().size(); i++) {
                WimGuiRowCell<A> cell = row.getCells().get(i);
                WimGuiControls.addRowCell(rowPanel, x, 0f, actionHeight, cell, buttons, defaultBorder);
                x += cell.getWidth() + cellGap;
            }
        }
    }

    private static <A> void addMainAction(CustomPanelAPI rowPanel,
                                          WimGuiListRow<A> row,
                                          float labelLeft,
                                          float labelWidth,
                                          float actionHeight,
                                          java.awt.Color defaultBorder,
                                          List<WimGuiButtonBinding<A>> buttons) {
        WimGuiControls.addBoundButton(
                rowPanel,
                labelLeft,
                0f,
                actionHeight,
                WimGuiButtonSpec.toggle(
                        labelWidth,
                        row.getLabel(),
                        row.getTextColor(),
                        row.getMainAction(),
                        row.getMainAlignment(),
                        row.getButtonFillColor(),
                        defaultBorder,
                        row.getTooltip()),
                buttons);
    }

    private static void addLabel(CustomPanelAPI parent,
                                 String text,
                                 java.awt.Color color,
                                 float x,
                                 float width,
                                 float rowHeight) {
        WimGuiControls.addLabel(parent, text, color, x, 0f, width, rowHeight, Alignment.LMID);
    }
}
