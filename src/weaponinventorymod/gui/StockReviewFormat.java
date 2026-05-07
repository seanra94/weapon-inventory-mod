package weaponinventorymod.gui;

final class StockReviewFormat {
    private StockReviewFormat() {
    }

    static String credits(int credits) {
        if (credits == StockReviewQuoteBook.PRICE_UNAVAILABLE) {
            return "?";
        }
        return grouped(Math.abs(credits)) + "cr";
    }

    static String grouped(int value) {
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
