package weaponinventorymod.gui;

import weaponinventorymod.core.StockCategory;
import weaponinventorymod.core.StockDisplayMode;
import weaponinventorymod.core.StockReviewConfig;

import java.util.EnumMap;
import java.util.Map;

public final class StockReviewState {
    private final Map<StockCategory, Boolean> expanded = new EnumMap<StockCategory, Boolean>(StockCategory.class);
    private StockDisplayMode displayMode;
    private boolean includeCurrentMarketStorage;
    private boolean includeBlackMarket;

    public StockReviewState(StockReviewConfig config) {
        expanded.put(StockCategory.NO_STOCK, Boolean.TRUE);
        expanded.put(StockCategory.INSUFFICIENT, Boolean.TRUE);
        expanded.put(StockCategory.SUFFICIENT, Boolean.FALSE);
        this.displayMode = config.getDisplayMode();
        this.includeCurrentMarketStorage = config.isIncludeCurrentMarketStorage();
        this.includeBlackMarket = config.isIncludeBlackMarket();
    }

    public boolean isExpanded(StockCategory category) {
        Boolean value = expanded.get(category);
        return value != null && value.booleanValue();
    }

    public void toggle(StockCategory category) {
        expanded.put(category, Boolean.valueOf(!isExpanded(category)));
    }

    public StockDisplayMode getDisplayMode() {
        return displayMode;
    }

    public void cycleDisplayMode() {
        displayMode = displayMode.next();
    }

    public boolean isIncludeCurrentMarketStorage() {
        return includeCurrentMarketStorage;
    }

    public void toggleCurrentMarketStorage() {
        includeCurrentMarketStorage = !includeCurrentMarketStorage;
    }

    public boolean isIncludeBlackMarket() {
        return includeBlackMarket;
    }

    public void toggleBlackMarket() {
        includeBlackMarket = !includeBlackMarket;
    }
}
