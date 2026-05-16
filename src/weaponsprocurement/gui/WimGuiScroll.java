package weaponsprocurement.gui;

import java.util.ArrayList;
import java.util.List;

public final class WimGuiScroll {
    public interface ExtraGapProvider<T> {
        float extraGapBefore(T item);
    }

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
        return Math.max(0, Math.min(requestedOffset, Math.max(0, maxOffset)));
    }

    static int usefulOffsetByDelta(int currentOffset, int scrollDelta, int maxOffset) {
        return usefulOffset(currentOffset + scrollDelta, maxOffset, scrollDelta);
    }

    static <T> WimGuiScrollSlice<T> verticalSlice(List<T> rows,
                                                 int requestedOffset,
                                                 float containerHeight,
                                                 float rowHeight,
                                                 float rowGap) {
        return verticalSlice(rows, requestedOffset, containerHeight, rowHeight, rowGap, null);
    }

    static <T> WimGuiScrollSlice<T> verticalSlice(List<T> rows,
                                                 int requestedOffset,
                                                 float containerHeight,
                                                 float rowHeight,
                                                 float rowGap,
                                                 ExtraGapProvider<T> extraGapProvider) {
        int itemCount = rows == null ? 0 : rows.size();
        if (itemCount <= 0) {
            return new WimGuiScrollSlice<T>(0, rows == null ? new ArrayList<T>() : rows, false, false, 0);
        }
        if (fits(rows, 0, itemCount, false, false, containerHeight, rowHeight, rowGap, extraGapProvider)) {
            return new WimGuiScrollSlice<T>(0, rows == null ? new ArrayList<T>() : rows, false, false, 0);
        }

        int maxOffset = bottomOffset(rows, itemCount, containerHeight, rowHeight, rowGap, extraGapProvider);
        int offset = usefulOffset(requestedOffset, maxOffset);
        boolean hasAbove;
        boolean hasBelow;
        int visibleSlots;
        hasAbove = offset > 0;
        visibleSlots = visibleCount(rows, offset, itemCount, hasAbove, false,
                containerHeight, rowHeight, rowGap, extraGapProvider);
        hasBelow = offset + visibleSlots < itemCount;
        if (hasBelow) {
            visibleSlots = visibleCount(rows, offset, itemCount, hasAbove, true,
                    containerHeight, rowHeight, rowGap, extraGapProvider);
        }
        visibleSlots = Math.max(1, visibleSlots);

        int end = Math.min(itemCount, offset + visibleSlots);
        hasBelow = end < itemCount;
        return new WimGuiScrollSlice<T>(
                offset,
                new ArrayList<T>(rows.subList(offset, end)),
                hasAbove,
                hasBelow,
                maxOffset);
    }

    private static <T> boolean fits(List<T> rows,
                                    int start,
                                    int end,
                                    boolean hasAbove,
                                    boolean hasBelow,
                                    float containerHeight,
                                    float rowHeight,
                                    float rowGap,
                                    ExtraGapProvider<T> extraGapProvider) {
        return visibleCount(rows, start, end, hasAbove, hasBelow,
                containerHeight, rowHeight, rowGap, extraGapProvider) >= end - start;
    }

    private static <T> int visibleCount(List<T> rows,
                                        int start,
                                        int end,
                                        boolean hasAbove,
                                        boolean reserveBelow,
                                        float containerHeight,
                                        float rowHeight,
                                        float rowGap,
                                        ExtraGapProvider<T> extraGapProvider) {
        float y = 0f;
        if (hasAbove) {
            if (rowHeight > containerHeight) {
                return 0;
            }
            y += rowHeight + rowGap;
        }
        int visible = 0;
        for (int i = start; i < end; i++) {
            float extraGap = extraGapProvider == null ? 0f : Math.max(0f, extraGapProvider.extraGapBefore(rows.get(i)));
            float rowTop = y + extraGap;
            float requiredBottom = rowTop + rowHeight;
            if (reserveBelow) {
                requiredBottom += rowGap + rowHeight;
            }
            if (requiredBottom > containerHeight + 0.01f) {
                break;
            }
            visible++;
            y = rowTop + rowHeight + rowGap;
        }
        return visible;
    }

    private static <T> int bottomOffset(List<T> rows,
                                        int itemCount,
                                        float containerHeight,
                                        float rowHeight,
                                        float rowGap,
                                        ExtraGapProvider<T> extraGapProvider) {
        int bottomOffset = Math.max(0, itemCount - 1);
        for (int candidate = itemCount - 1; candidate >= 0; candidate--) {
            if (!fits(rows, candidate, itemCount, candidate > 0, false,
                    containerHeight, rowHeight, rowGap, extraGapProvider)) {
                break;
            }
            bottomOffset = candidate;
        }
        return bottomOffset;
    }
}
