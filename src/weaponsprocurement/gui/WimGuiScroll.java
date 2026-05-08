package weaponsprocurement.gui;

import java.util.ArrayList;
import java.util.List;

final class WimGuiScroll {
    static final int DEFAULT_WHEEL_STEP = 3;

    private WimGuiScroll() {
    }

    static int wheelDelta(int eventValue) {
        if (eventValue == 0) {
            return 0;
        }
        return eventValue > 0 ? -DEFAULT_WHEEL_STEP : DEFAULT_WHEEL_STEP;
    }

    static int verticalSlotCount(float containerHeight, float itemHeight, float verticalGap) {
        float perRow = itemHeight + verticalGap;
        return Math.max(1, (int) Math.floor((containerHeight + verticalGap) / perRow));
    }

    static int usefulOffset(int requestedOffset, int maxOffset) {
        return usefulOffset(requestedOffset, maxOffset, 0);
    }

    static int usefulOffset(int requestedOffset, int maxOffset, int scrollDelta) {
        int clamped = Math.max(0, Math.min(requestedOffset, Math.max(0, maxOffset)));
        if (clamped != 1) {
            return clamped;
        }
        return scrollDelta > 0 && maxOffset >= 2 ? 2 : 0;
    }

    static int usefulOffsetByDelta(int currentOffset, int scrollDelta, int maxOffset) {
        return usefulOffset(currentOffset + scrollDelta, maxOffset, scrollDelta);
    }

    static <T> WimGuiScrollSlice<T> verticalSlice(List<T> rows,
                                                 int requestedOffset,
                                                 float containerHeight,
                                                 float rowHeight,
                                                 float rowGap) {
        int itemCount = rows == null ? 0 : rows.size();
        int totalSlots = verticalSlotCount(containerHeight, rowHeight, rowGap);
        if (itemCount <= totalSlots) {
            return new WimGuiScrollSlice<T>(0, rows == null ? new ArrayList<T>() : rows, false, false, 0);
        }

        int maxOffset = Math.max(0, itemCount - 1);
        int offset = usefulOffset(requestedOffset, maxOffset);
        boolean hasAbove;
        boolean hasBelow;
        int visibleSlots;
        int finalMaxOffset;
        while (true) {
            hasAbove = offset > 0;
            visibleSlots = totalSlots - (hasAbove ? 1 : 0);
            hasBelow = offset + visibleSlots < itemCount;
            if (hasBelow) {
                visibleSlots -= 1;
            }
            visibleSlots = Math.max(1, visibleSlots);

            finalMaxOffset = Math.max(0, itemCount - visibleSlots);
            int useful = usefulOffset(offset, finalMaxOffset);
            if (useful == offset) {
                break;
            }
            offset = useful;
        }

        int end = Math.min(itemCount, offset + visibleSlots);
        hasBelow = end < itemCount;
        return new WimGuiScrollSlice<T>(
                offset,
                new ArrayList<T>(rows.subList(offset, end)),
                hasAbove,
                hasBelow,
                finalMaxOffset);
    }
}
