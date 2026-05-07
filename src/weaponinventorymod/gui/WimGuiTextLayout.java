package weaponinventorymod.gui;

final class WimGuiTextLayout {
    final String wrappedText;
    final int lineCount;
    final float rowHeight;
    final float textWidth;
    final float renderHeight;
    final boolean truncated;

    WimGuiTextLayout(String wrappedText,
                     int lineCount,
                     float rowHeight,
                     float textWidth,
                     float renderHeight,
                     boolean truncated) {
        this.wrappedText = wrappedText;
        this.lineCount = lineCount;
        this.rowHeight = rowHeight;
        this.textWidth = textWidth;
        this.renderHeight = renderHeight;
        this.truncated = truncated;
    }
}
