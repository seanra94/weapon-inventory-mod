package weaponsprocurement.gui;

import java.util.List;

final class WimGuiScrollSlice<T> {
    final int offset;
    final List<T> items;
    final boolean hasAbove;
    final boolean hasBelow;
    final int maxOffset;

    WimGuiScrollSlice(int offset, List<T> items, boolean hasAbove, boolean hasBelow, int maxOffset) {
        this.offset = offset;
        this.items = items;
        this.hasAbove = hasAbove;
        this.hasBelow = hasBelow;
        this.maxOffset = maxOffset;
    }

}
