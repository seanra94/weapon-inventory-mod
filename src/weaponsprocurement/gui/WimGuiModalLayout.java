package weaponsprocurement.gui;

final class WimGuiModalLayout {
    final float width;
    final float height;
    final float padding;
    final float sectionGap;
    final float headingHeight;
    final float footerHeight;
    final float rowHeight;
    final float rowGap;
    final float listInset;

    WimGuiModalLayout(float width,
                      float height,
                      float padding,
                      float sectionGap,
                      float headingHeight,
                      float footerHeight,
                      float rowHeight,
                      float rowGap,
                      float listInset) {
        this.width = width;
        this.height = height;
        this.padding = padding;
        this.sectionGap = sectionGap;
        this.headingHeight = headingHeight;
        this.footerHeight = footerHeight;
        this.rowHeight = rowHeight;
        this.rowGap = rowGap;
        this.listInset = listInset;
    }

    float bodyTop() {
        return padding + headingHeight + sectionGap;
    }

    float headingTop() {
        return padding;
    }

    float actionRowY(float headerContentHeight, float gapAfterHeader) {
        return headingTop() + headerContentHeight + gapAfterHeader;
    }

    float footerButtonY(float buttonHeight) {
        return height - padding - buttonHeight;
    }

    float bodyHeight() {
        return Math.max(rowHeight, height - bodyTop() - sectionGap - footerHeight - padding);
    }

    float contentWidth() {
        return width - 2f * padding;
    }

    float listPanelHeight(int rowCount, boolean hasAbove, boolean hasBelow, float extraGapHeight) {
        int controlCount = Math.max(1, rowCount + (hasAbove ? 1 : 0) + (hasBelow ? 1 : 0));
        return verticalItemsHeight(controlCount, rowHeight, rowGap) + Math.max(0f, extraGapHeight) + 2f * listInset;
    }

    float verticalItemsHeight(int count, float itemHeight, float gap) {
        int safeCount = Math.max(1, count);
        return safeCount * itemHeight + Math.max(0, safeCount - 1) * gap;
    }
}
