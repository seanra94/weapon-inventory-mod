package weaponsprocurement.gui;

enum StockReviewFilterGroup {
    SIZE("Size"),
    TYPE("Type"),
    DAMAGE("Damage");

    private final String label;

    StockReviewFilterGroup(String label) {
        this.label = label;
    }

    String getLabel() {
        return label;
    }
}
