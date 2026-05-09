package weaponsprocurement.core;

public final class CreditFormat {
    public static final String CREDIT_SYMBOL = "\u00a2";

    private CreditFormat() {
    }

    public static String credits(int credits) {
        return credits((long) credits);
    }

    public static String credits(long credits) {
        return grouped(credits) + CREDIT_SYMBOL;
    }

    public static String creditsLong(int credits) {
        return creditsLong((long) credits);
    }

    public static String creditsLong(long credits) {
        return grouped(credits) + " credits";
    }

    public static String grouped(int value) {
        return grouped((long) value);
    }

    public static String grouped(long value) {
        String digits = String.valueOf(Math.abs(value));
        StringBuilder result = new StringBuilder();
        int firstGroup = digits.length() % 3;
        if (firstGroup == 0) {
            firstGroup = 3;
        }
        result.append(digits.substring(0, firstGroup));
        for (int i = firstGroup; i < digits.length(); i += 3) {
            result.append(',').append(digits.substring(i, i + 3));
        }
        return value < 0 ? "-" + result.toString() : result.toString();
    }
}
