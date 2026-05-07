package weaponinventorymod.gui;

enum StockReviewTradeGroup {
    BUYING("Buying"),
    SELLING("Selling");

    private final String label;

    StockReviewTradeGroup(String label) {
        this.label = label;
    }

    String getLabel() {
        return label;
    }
}
