package weaponsprocurement.core;

public final class TradeMoney {
    public static final long MAX_EXECUTABLE_CREDITS = Integer.MAX_VALUE;

    private TradeMoney() {
    }

    public static long lineTotal(int unitPrice, int quantity) {
        if (unitPrice < 0 || quantity < 0) {
            return -1L;
        }
        return (long) unitPrice * (long) quantity;
    }

    public static long safeAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        if (right < 0L && left < Long.MIN_VALUE - right) {
            return Long.MIN_VALUE;
        }
        return left + right;
    }

    public static boolean canExecuteCreditMutation(long credits) {
        return credits >= 0L && credits <= MAX_EXECUTABLE_CREDITS;
    }
}
