package weaponinventorymod.core;

public final class CreditFormat {
    private CreditFormat() {
    }

    public static String credits(int credits) {
        return grouped(credits) + "cr";
    }

    public static String creditsLong(int credits) {
        return grouped(credits) + " credits";
    }

    public static String grouped(int value) {
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
