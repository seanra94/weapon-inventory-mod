package weaponinventorymod.gui;

import weaponinventorymod.core.CreditFormat;

final class StockReviewFormat {
    private StockReviewFormat() {
    }

    static String credits(int credits) {
        if (credits == StockReviewQuoteBook.PRICE_UNAVAILABLE) {
            return "?";
        }
        return CreditFormat.credits(Math.abs(credits));
    }
}
