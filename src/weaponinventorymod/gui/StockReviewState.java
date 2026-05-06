package weaponinventorymod.gui;

import weaponinventorymod.core.StockCategory;

import java.util.EnumMap;
import java.util.Map;

public final class StockReviewState {
    private final Map<StockCategory, Boolean> expanded = new EnumMap<StockCategory, Boolean>(StockCategory.class);

    public StockReviewState() {
        expanded.put(StockCategory.NO_STOCK, Boolean.TRUE);
        expanded.put(StockCategory.INSUFFICIENT, Boolean.TRUE);
        expanded.put(StockCategory.SUFFICIENT, Boolean.FALSE);
    }

    public boolean isExpanded(StockCategory category) {
        Boolean value = expanded.get(category);
        return value != null && value.booleanValue();
    }

    public void toggle(StockCategory category) {
        expanded.put(category, Boolean.valueOf(!isExpanded(category)));
    }
}
